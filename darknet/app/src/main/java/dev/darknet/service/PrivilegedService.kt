package dev.darknet.service

import android.content.Context
import android.net.Network
import android.os.Build
import android.util.Log
import dev.darknet.BuildConfig
import dev.darknet.IDarkNetService
import org.json.JSONArray
import org.json.JSONObject
import org.lsposed.hiddenapibypass.HiddenApiBypass
import java.io.File
import kotlin.system.exitProcess

/**
 * Runs as the shell user inside the Shizuku process (a Shizuku "user service").
 *
 * Starting sharing: create a test network whose TUN the engine reads, tell
 * tethering to prefer test networks, then (re)start the hotspot so it picks our
 * network as its upstream. Stopping undoes each step.
 */
class PrivilegedService(base: Context) : IDarkNetService.Stub() {

    init {
        runCatching { HiddenApiBypass.addHiddenApiExemptions("L") }
            .onFailure { Log.e("DarkNet", "hidden API exemption failed", it) }
    }

    private val context: Context = shellContext(base)
    private val net by lazy { SystemNet(context) }
    private val lock = Any()

    @Volatile private var state = "idle"
    @Volatile private var detail = ""
    @Volatile private var since = 0L
    @Volatile private var upstream: Network? = null
    @Volatile private var clients: List<Client> = emptyList()

    private var test: TestNetwork? = null
    private var engineSession: engine.Session? = null
    private var events: Any? = null
    private var usb = false
    private var uplinks: List<android.net.ConnectivityManager.NetworkCallback> = emptyList()
    @Volatile private var combined = 0
    @Volatile private var mtu = TunAddresses.MTU
    @Volatile private var mtuSource = ""
    @Volatile private var dnsServers: List<String> = emptyList()

    init {
        ServiceLog.info(
            "service up: ${Build.MANUFACTURER} ${Build.MODEL}, Android ${Build.VERSION.RELEASE} " +
                "(SDK ${Build.VERSION.SDK_INT}), DarkNet ${BuildConfig.VERSION_NAME}, ${engine.Engine.Version}",
        )
    }

    override fun version(): Int = BuildConfig.VERSION_CODE

    override fun start(options: String?): String = synchronized(lock) {
        if (state == "active") return status()
        val opts = runCatching { JSONObject(options ?: "{}") }.getOrDefault(JSONObject())
        val fast = opts.optBoolean("fastEngine", true)
        usb = opts.optBoolean("usb", false)
        val band = opts.optInt("band", 0)
        val bbr = opts.optBoolean("bbr", true)
        val combine = opts.optBoolean("combine", false)
        val dns = opts.optJSONArray("dns")?.let { a -> List(a.length()) { a.getString(it) } }.orEmpty()
        state = "starting"
        detail = ""
        ServiceLog.info("start: fast engine $fast, usb $usb, band $band")

        try {
            net.pickMtu(opts.optString("mtu", "auto")).let { (value, source) -> mtu = value; mtuSource = source }
            ServiceLog.info("MTU $mtu ($mtuSource)")
            val network = net.createTestNetwork(dns, mtu)
            dnsServers = dns.ifEmpty { TunAddresses.DNS }
            ServiceLog.info("DNS for devices: $dnsServers")
            test = network

            val session = engine.Engine.start(
                network.descriptor.fd.toLong(),
                mtu.toLong(),
                TunAddresses.IPV4,
                TunAddresses.FAKE_IPV4,
                fast,
            )
            engineSession = session
            session.setCongestion(if (bbr) "bbr" else "")
            if (fast) {
                val v6 = session.enableIPv6(TunAddresses.IPV6, TunAddresses.FAKE_IPV6)
                ServiceLog.info("IPv6 fast path: ${if (v6) "ACTIVE" else "not available, IPv6 uses the userspace path"}")
                ServiceLog.info("UDP relay: ${if (session.udpRelayActive()) "ACTIVE" else "off"}")
            }
            if (combine) {
                uplinks = net.watchUplinks { nets ->
                    combined = nets.size
                    session.setNetworks(if (nets.size >= 2) nets.joinToString(",") { it.networkHandle.toString() } else "")
                    ServiceLog.info("combining networks: ${nets.size} available${if (nets.size >= 2) ", spreading connections" else ""}")
                }
            }
            ServiceLog.info(
                when {
                    !fast -> "engine started (userspace path only)"
                    session.fastPathActive() -> "engine started: fast path ACTIVE"
                    else -> "engine started: fast path probe failed, using the userspace path"
                },
            )

            events = net.watchTethering(
                onUpstream = { n ->
                    upstream = n
                    ServiceLog.info("tethering upstream: ${n ?: "none"}${if (n == network.network) " (DarkNet)" else ""}")
                },
                onClients = { list ->
                    if (list.size != clients.size) ServiceLog.info("clients: ${list.size}")
                    clients = list
                },
            )

            net.preferTestNetworks(true)
            // Restart the hotspot so it re-selects its upstream (ours).
            net.stopTethering(SystemNet.TETHERING_WIFI)
            Thread.sleep(600)
            if (band != 0) {
                runCatching { net.setHotspotBand(band) }
                    .onSuccess { ServiceLog.info("hotspot band: $it") }
                    .onFailure { ServiceLog.error("could not change the hotspot band", it) }
            }
            net.startTethering(SystemNet.TETHERING_WIFI)?.let { error("Wi-Fi hotspot: $it") }
            ServiceLog.info("Wi-Fi hotspot started")
            if (usb) {
                net.startTethering(SystemNet.TETHERING_USB)?.let { ServiceLog.warn("USB tethering: $it") }
            }

            state = "active"
            since = System.currentTimeMillis()
        } catch (failure: Throwable) {
            ServiceLog.error("start failed", failure)
            teardown()
            state = "error"
            detail = "${failure.javaClass.simpleName}: ${failure.message}"
        }
        status()
    }

    override fun stop(): String = synchronized(lock) {
        ServiceLog.info("stop requested")
        teardown()
        state = "idle"
        detail = ""
        status()
    }

    private fun teardown() {
        net.unwatchUplinks(uplinks)
        uplinks = emptyList()
        combined = 0
        net.unwatchTethering(events)
        events = null
        if (state == "active" || state == "starting") {
            net.stopTethering(SystemNet.TETHERING_WIFI)
            if (usb) net.stopTethering(SystemNet.TETHERING_USB)
        }
        runCatching { net.preferTestNetworks(false) }
        engineSession?.let { runCatching { it.stop() } }
        engineSession = null
        test?.release(context)
        test = null
        upstream = null
        clients = emptyList()
        since = 0L
    }

    override fun status(): String {
        val session = engineSession
        val iface = test?.interfaceName
        val raw = iface?.let(::readCounters) ?: (0L to 0L)
        val fastUp = session?.fastBytesUp() ?: 0L
        val fastDown = session?.fastBytesDown() ?: 0L
        // The TUN's tx is what clients sent; fast-path packets cross it twice.
        val up = (raw.first - fastDown).coerceAtLeast(0)
        val down = (raw.second - fastUp).coerceAtLeast(0)

        return JSONObject()
            .put("state", state)
            .put("detail", detail)
            .put("interface", iface ?: "")
            .put("since", since)
            .put("fastPath", session?.fastPathActive() == true)
            .put("fastPath6", session?.fastPath6Active() == true)
            .put("udpFast", session?.udpRelayActive() == true)
            .put("congestion", session?.congestion().orEmpty())
            .put("mtu", if (session != null) mtu else 0)
            .put("combined", combined)
            .put("upstreamOk", upstream != null && upstream == test?.network)
            .put("up", up)
            .put("down", down)
            .put(
                "clients",
                JSONArray(
                    clients.map {
                        JSONObject().put("name", it.name).put("mac", it.mac)
                            .put("ip", it.addresses.firstOrNull { a -> '.' in a } ?: it.addresses.firstOrNull().orEmpty())
                            .put("usb", it.type == SystemNet.TETHERING_USB)
                    },
                ),
            )
            .toString()
    }

    override fun log(): String = ServiceLog.dump()

    override fun hotspotInfo(): String = runCatching {
        val (ssid, password, security) = net.hotspotInfo()
        JSONObject().put("ssid", ssid).put("password", password).put("security", security).toString()
    }.getOrElse {
        ServiceLog.error("hotspot info", it)
        JSONObject().put("error", it.message ?: it.javaClass.simpleName).toString()
    }

    override fun diagnose(): String {
        val out = JSONArray()
        fun add(name: String, state: String, detail: String) {
            out.put(JSONObject().put("name", name).put("state", state).put("detail", detail))
        }
        val session = engineSession
        val tun = test
        if (session == null || tun == null || state != "active") {
            add("Compartiendo", "fail", "Empieza a compartir y vuelve a comprobar.")
            return out.toString()
        }
        add(
            "El hotspot sale por DarkNet",
            if (upstream == tun.network) "ok" else "fail",
            if (upstream == tun.network) "Android usa el túnel de DarkNet como salida." else "El hotspot está usando otra salida: ${upstream ?: "ninguna"}.",
        )
        add("Motor rápido TCP (IPv4)", if (session.fastPathActive()) "ok" else "warn",
            if (session.fastPathActive()) "El TCP de Android lleva las conexiones." else "Tu teléfono no lo permitió: va por el motor de respaldo.")
        add("Motor rápido TCP (IPv6)", if (session.fastPath6Active()) "ok" else "warn",
            if (session.fastPath6Active()) "Activo." else "No disponible aquí: IPv6 va por el motor de respaldo.")
        add("UDP directo (YouTube, juegos, llamadas)", if (session.udpRelayActive()) "ok" else "warn",
            if (session.udpRelayActive()) "Sin pasar por el motor de respaldo." else "Activa el motor rápido en Ajustes.")

        // A real request: the fast path accepts a connection inside the phone before
        // reaching the internet, so only a response proves the whole way works.
        val web = runCatching {
            val t0 = System.nanoTime()
            val c = tun.network.openConnection(java.net.URL("https://1.1.1.1/cdn-cgi/trace")) as java.net.HttpURLConnection
            c.connectTimeout = 5000
            c.readTimeout = 5000
            val body = c.inputStream.use { it.readBytes().decodeToString() }
            c.disconnect()
            val ms = (System.nanoTime() - t0) / 1_000_000
            val colo = body.lineSequence().firstOrNull { it.startsWith("colo=") }?.substringAfter('=')
            ms to colo
        }
        add("Internet a través del motor", if (web.isSuccess) "ok" else "fail",
            web.fold(
                { (ms, colo) -> "Respuesta real de internet por el túnel en $ms ms${colo?.let { " (servidor $it)" } ?: ""}." },
                { "La petición de prueba falló: ${it.message}" },
            ))

        val server = dnsServers.firstOrNull { '.' in it } ?: "1.1.1.1"
        val dns = runCatching {
            java.net.DatagramSocket().use { socket ->
                tun.network.bindSocket(socket)
                socket.soTimeout = 3000
                val id = 0x2a2a
                val q = dev.darknet.data.DnsProbe.query(id, "example.com")
                val t0 = System.nanoTime()
                socket.send(java.net.DatagramPacket(q, q.size, java.net.InetAddress.getByName(server), 53))
                val buf = ByteArray(512)
                val reply = java.net.DatagramPacket(buf, buf.size)
                socket.receive(reply)
                check(dev.darknet.data.DnsProbe.isAnswer(buf, reply.length, id)) { "respuesta inválida" }
                (System.nanoTime() - t0) / 1_000_000
            }
        }
        add("DNS elegido a través del motor", if (dns.isSuccess) "ok" else "fail",
            dns.fold({ "$server respondió en $it ms." }, { "$server no respondió: ${it.message}" }))

        val raw = test?.interfaceName?.let(::readCounters) ?: (0L to 0L)
        // Fast-path bytes cross the TUN twice, so the interface counts them twice;
        // the UDP relay's cross it once.
        val fastBytes = session.fastBytesUp() + session.fastBytesDown()
        val stats = runCatching { JSONObject(session.stats()) }.getOrDefault(JSONObject())
        val udpBytes = stats.optLong("udpUp") + stats.optLong("udpDown")
        val total = (raw.first + raw.second - fastBytes).coerceAtLeast(1)
        val tcpShare = (fastBytes * 100 / total).coerceIn(0, 100)
        val udpShare = (udpBytes * 100 / total).coerceIn(0, 100)
        val share = (tcpShare + udpShare).coerceAtMost(100)
        add("Tráfico en el motor rápido", if (share >= 50) "ok" else "info",
            "$share % de lo compartido en esta sesión (TCP $tcpShare %, UDP $udpShare %). El resto va por el motor de respaldo.")
        val cc = session.congestion()
        add("Control de congestión", if (cc.startsWith("bbr")) "ok" else "info",
            if (cc.isEmpty()) "Se verá cuando haya conexiones." else cc)
        add("MTU", "info", "$mtu ($mtuSource)")
        if (uplinks.isNotEmpty()) {
            add("Sumar Wi‑Fi y datos", if (combined >= 2) "ok" else "warn",
                if (combined >= 2) "Repartiendo conexiones entre $combined redes." else "Solo hay $combined red disponible ahora.")
        }
        return out.toString()
    }

    override fun speedTest(mode: String?): String = runCatching {
        val network = if (mode == "engine") test?.network ?: error("Empieza a compartir primero") else net.defaultNetwork() ?: error("Sin red")
        val bytes = java.util.concurrent.atomic.AtomicLong()
        val deadline = System.currentTimeMillis() + SPEED_MS
        val t0 = System.currentTimeMillis()
        val threads = List(4) {
            Thread {
                runCatching {
                    val c = network.openConnection(java.net.URL("https://speed.cloudflare.com/__down?bytes=$SPEED_BYTES_PER_STREAM")) as java.net.HttpURLConnection
                    c.connectTimeout = 5000
                    c.readTimeout = 5000
                    c.inputStream.use { input ->
                        val buf = ByteArray(64 shl 10)
                        while (System.currentTimeMillis() < deadline) {
                            val n = input.read(buf)
                            if (n < 0) break
                            bytes.addAndGet(n.toLong())
                        }
                    }
                    c.disconnect()
                }
            }.apply { start() }
        }
        threads.forEach { it.join(SPEED_MS + 6000) }
        val ms = (System.currentTimeMillis() - t0).coerceAtLeast(1)
        if (bytes.get() == 0L) error("No se pudo descargar")
        JSONObject().put("mbps", bytes.get() * 8.0 / 1000 / ms).put("bytes", bytes.get()).put("ms", ms).toString()
    }.getOrElse { JSONObject().put("error", it.message ?: it.javaClass.simpleName).toString() }

    override fun setNetwork(handle: Long) {
        engineSession?.setNetwork(handle)
    }

    override fun destroy() {
        runCatching { stop() }
        exitProcess(0)
    }

    /** tx and rx bytes of [iface] from /proc/net/dev. */
    private fun readCounters(iface: String): Pair<Long, Long> = runCatching {
        val line = File("/proc/net/dev").readLines().first { it.trimStart().startsWith("$iface:") }
        val f = line.substringAfter(':').trim().split(Regex("\\s+"))
        f[8].toLong() to f[0].toLong()
    }.getOrDefault(0L to 0L)

    private companion object {
        const val SHELL_UID = 2000
        const val SHELL_PACKAGE = "com.android.shell"
        const val SPEED_MS = 6000L
        const val SPEED_BYTES_PER_STREAM = 25_000_000

        /**
         * Shizuku gives this process a context named after DarkNet's package, but the
         * process runs as the shell user. System services check that the calling package
         * belongs to the calling uid, so present the shell package instead: rewrite the
         * attribution of the underlying ContextImpl before any manager is created.
         */
        fun shellContext(base: Context): Context {
            var impl: Context = base
            while (impl is android.content.ContextWrapper && impl.baseContext != null) impl = impl.baseContext
            runCatching {
                val source = android.content.AttributionSource.Builder(SHELL_UID)
                    .setPackageName(SHELL_PACKAGE)
                    .build()
                val field = findField(impl.javaClass, "mAttributionSource")
                field.isAccessible = true
                field.set(impl, source)
            }.onFailure { ServiceLog.error("could not set shell attribution", it) }
            runCatching {
                val field = findField(impl.javaClass, "mBasePackageName")
                field.isAccessible = true
                field.set(impl, SHELL_PACKAGE)
            }
            ServiceLog.info("acting as ${impl.opPackageName} (uid ${android.os.Process.myUid()})")
            return impl
        }

        private fun findField(cls: Class<*>, name: String): java.lang.reflect.Field {
            var c: Class<*>? = cls
            while (c != null) {
                runCatching { return c!!.getDeclaredField(name) }
                c = c.superclass
            }
            throw NoSuchFieldException(name)
        }
    }
}
