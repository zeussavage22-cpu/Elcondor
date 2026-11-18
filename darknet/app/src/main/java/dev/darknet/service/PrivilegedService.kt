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

    init {
        runCatching { HiddenApiBypass.addHiddenApiExemptions("L") }
            .onFailure { ServiceLog.error("hidden API exemption failed", it) }
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
        state = "starting"
        detail = ""
        ServiceLog.info("start: fast engine $fast, usb $usb")

        try {
            val network = net.createTestNetwork()
            test = network

            val session = engine.Engine.start(
                network.descriptor.fd.toLong(),
                TunAddresses.MTU.toLong(),
                TunAddresses.IPV4,
                TunAddresses.FAKE_IPV4,
                fast,
            )
            engineSession = session
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
        /** A context attributed to the shell package, so system services accept our uid. */
        fun shellContext(base: Context): Context =
            runCatching { base.createPackageContext("com.android.shell", 0) }
                .onFailure { Log.w("DarkNet", "no shell context: ${it.message}") }
                .getOrDefault(base)
    }
}
