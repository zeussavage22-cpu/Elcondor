package dev.darknet.service

import android.content.Context
import android.net.ConnectivityManager
import android.net.LinkProperties
import android.net.Network
import android.net.NetworkRequest
import android.os.Binder
import android.os.ParcelFileDescriptor
import java.net.InetAddress
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/** The TUN that hotspot traffic is routed into, and its Android network. */
internal class TestNetwork(
    val interfaceName: String,
    val descriptor: ParcelFileDescriptor,
    val network: Network,
    private val token: Binder,
    private val request: ConnectivityManager.NetworkCallback,
)  {
    fun release(context: Context) {
        val cm = context.getSystemService(ConnectivityManager::class.java)
        runCatching { cm.unregisterNetworkCallback(request) }
        runCatching {
            val tnm = context.getSystemService(TEST_NETWORK_SERVICE)
            if (tnm != null) Reflect.call(tnm, "teardownTestNetwork", network)
        }.onFailure { ServiceLog.warn("teardownTestNetwork: ${it.message}") }
        runCatching { descriptor.close() }
        // The token is the network's death signal; dropping it also tears it down.
        token.hashCode()
    }

    companion object {
        const val TEST_NETWORK_SERVICE = "test_network"
    }
}

/** Addresses of DarkNet's TUN. 192.0.2.0/24 and 2001:db8::/64 are reserved for documentation. */
internal object TunAddresses {
    const val IPV4 = "192.0.2.2"
    const val IPV4_PREFIX = 24
    const val FAKE_IPV4 = "192.0.2.3" // unused peer for the engine's fast path
    const val IPV6 = "2001:db8::2"
    const val IPV6_PREFIX = 64
    const val MTU = 1500
    val DNS = listOf("1.1.1.1", "8.8.8.8", "2606:4700:4700::1111", "2001:4860:4860::8888")
}

/** Android networking and tethering operations, done with the shell's privileges. */
internal class SystemNet(private val context: Context) {

    private val cm = context.getSystemService(ConnectivityManager::class.java)
    private val executor = Executors.newSingleThreadExecutor()

    // ---- test network -------------------------------------------------------

    fun createTestNetwork(dns: List<String>): TestNetwork {
        val tnm = context.getSystemService(TestNetwork.TEST_NETWORK_SERVICE)
            ?: error("this phone has no test network service")

        val v4 = linkAddress(TunAddresses.IPV4, TunAddresses.IPV4_PREFIX)
        val v6 = linkAddress(TunAddresses.IPV6, TunAddresses.IPV6_PREFIX)
        val tun = runCatching { Reflect.call(tnm, "createTunInterface", listOf(v4, v6)) }
            .recoverCatching {
                val array = java.lang.reflect.Array.newInstance(v4.javaClass, 2)
                java.lang.reflect.Array.set(array, 0, v4)
                java.lang.reflect.Array.set(array, 1, v6)
                Reflect.call(tnm, "createTunInterface", array)
            }
            .getOrThrow()!!
        val name = Reflect.call(tun, "getInterfaceName") as String
        val fd = Reflect.call(tun, "getFileDescriptor") as ParcelFileDescriptor
        ServiceLog.info("TUN created: $name")

        val token = Binder()
        try {
            setUpNetwork(tnm, name, v4, v6, token, dns)
            val (network, callback) = awaitNetwork(name)
            ServiceLog.info("test network available on $name")
            return TestNetwork(name, fd, network, token, callback)
        } catch (failure: Throwable) {
            runCatching { fd.close() }
            throw failure
        }
    }

    private fun setUpNetwork(tnm: Any, iface: String, v4: Any, v6: Any, token: Binder, dns: List<String>) {
        if (Reflect.has(tnm, "setupTestNetwork", 3)) {
            val lp = LinkProperties()
            Reflect.call(lp, "setInterfaceName", iface)
            Reflect.call(lp, "addLinkAddress", v4)
            Reflect.call(lp, "addLinkAddress", v6)
            for (server in dns.ifEmpty { TunAddresses.DNS }) Reflect.call(lp, "addDnsServer", InetAddress.getByName(server))
            Reflect.call(lp, "addRoute", route("0.0.0.0", 0, iface))
            Reflect.call(lp, "addRoute", route("::", 0, iface))
            Reflect.call(lp, "setMtu", TunAddresses.MTU)
            Reflect.call(tnm, "setupTestNetwork", lp, false, token)
        } else {
            Reflect.call(tnm, "setupTestNetwork", iface, token)
        }
    }

    private fun awaitNetwork(iface: String): Pair<Network, ConnectivityManager.NetworkCallback> {
        val found = CountDownLatch(1)
        var network: Network? = null
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(n: Network) {
                network = n
                found.countDown()
            }
        }
        val request = NetworkRequest.Builder()
            .clearCapabilities()
            .addTransportType(TRANSPORT_TEST)
            .setNetworkSpecifier(iface)
            .build()
        cm.requestNetwork(request, callback)
        if (!found.await(10, TimeUnit.SECONDS)) {
            runCatching { cm.unregisterNetworkCallback(callback) }
            error("the test network on $iface never became available")
        }
        return network!! to callback
    }

    private fun linkAddress(address: String, prefix: Int): Any =
        Reflect.newInstance("android.net.LinkAddress", InetAddress.getByName(address), prefix)

    private fun route(destination: String, prefix: Int, iface: String): Any {
        val ipPrefix = Reflect.newInstance("android.net.IpPrefix", InetAddress.getByName(destination), prefix)
        return Reflect.newInstance("android.net.RouteInfo", ipPrefix, null, iface)
    }

    // ---- hotspot settings -----------------------------------------------------

    /** The saved hotspot name, password and security type (needs the shell's Wi-Fi rights). */
    fun hotspotInfo(): Triple<String, String, Int> {
        val wifi = context.getSystemService(android.net.wifi.WifiManager::class.java)
        val config = Reflect.call(wifi, "getSoftApConfiguration")!!
        val ssid = runCatching { Reflect.call(config, "getWifiSsid")?.toString()?.trim('"') }.getOrNull()
            ?: (Reflect.call(config, "getSsid") as? String).orEmpty()
        val password = (Reflect.call(config, "getPassphrase") as? String).orEmpty()
        val security = (Reflect.call(config, "getSecurityType") as? Int) ?: 1
        return Triple(ssid, password, security)
    }

    // ---- tethering ----------------------------------------------------------

    private val tethering: Any
        get() = context.getSystemService("tethering") ?: error("this phone has no tethering service")

    /** Makes tethering use test networks (ours) as its upstream. */
    fun preferTestNetworks(prefer: Boolean) {
        Reflect.call(tethering, "setPreferTestNetworks", prefer)
        ServiceLog.info("prefer test networks: $prefer")
    }

    /** Starts tethering of [type]; returns null on success or the failure reason. */
    fun startTethering(type: Int): String? {
        val done = CountDownLatch(1)
        var failure: String? = null
        val request = Reflect.call(
            Reflect.newInstance("android.net.TetheringManager\$TetheringRequest\$Builder", type),
            "build",
        )!!
        val callback = Reflect.proxy("android.net.TetheringManager\$StartTetheringCallback") { name, args ->
            if (name == "onTetheringFailed") failure = "tethering error ${args.firstOrNull()}"
            done.countDown()
        }
        Reflect.call(tethering, "startTethering", request, executor, callback)
        if (!done.await(15, TimeUnit.SECONDS)) return "tethering did not answer"
        return failure
    }

    fun stopTethering(type: Int) {
        runCatching { Reflect.call(tethering, "stopTethering", type) }
            .onFailure { ServiceLog.warn("stopTethering($type): ${it.message}") }
    }

    /** Follows tethering's upstream and connected clients. */
    fun watchTethering(onUpstream: (Network?) -> Unit, onClients: (List<Client>) -> Unit): Any? =
        runCatching {
            val callback = Reflect.proxy("android.net.TetheringManager\$TetheringEventCallback") { name, args ->
                when (name) {
                    "onUpstreamChanged" -> onUpstream(args.firstOrNull() as? Network)
                    "onClientsChanged" -> onClients(parseClients(args.firstOrNull() as? Collection<*>))
                }
            }
            Reflect.call(tethering, "registerTetheringEventCallback", executor, callback)
            callback
        }.onFailure { ServiceLog.warn("tethering events unavailable: ${it.message}") }.getOrNull()

    fun unwatchTethering(callback: Any?) {
        if (callback == null) return
        runCatching { Reflect.call(tethering, "unregisterTetheringEventCallback", callback) }
    }

    private fun parseClients(raw: Collection<*>?): List<Client> = raw.orEmpty().mapNotNull { client ->
        runCatching {
            val mac = Reflect.call(client!!, "getMacAddress")?.toString().orEmpty()
            val type = Reflect.call(client, "getTetheringType") as Int
            val infos = Reflect.call(client, "getAddresses") as List<*>
            val ips = infos.mapNotNull { info ->
                (Reflect.call(info!!, "getAddress")?.toString())?.substringBefore('/')
            }
            val name = infos.firstNotNullOfOrNull { info ->
                (Reflect.call(info!!, "getHostname") as? String)?.takeIf { it.isNotBlank() }
            }
            Client(name = name.orEmpty(), mac = mac, addresses = ips, type = type)
        }.getOrNull()
    }

    companion object {
        /** NetworkCapabilities.TRANSPORT_TEST, hidden from the public SDK. */
        const val TRANSPORT_TEST = 7
        const val TETHERING_WIFI = 0
        const val TETHERING_USB = 1
    }
}

internal data class Client(val name: String, val mac: String, val addresses: List<String>, val type: Int)
