package com.elcondor.optimizer.data

import android.app.usage.NetworkStats
import android.app.usage.NetworkStatsManager
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.TrafficStats
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Build
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URL

data class UidNet(
    val uid: Int,
    var wifiFg: Long = 0, var wifiBg: Long = 0,
    var mobileFg: Long = 0, var mobileBg: Long = 0,
) {
    val bgBytes get() = wifiBg + mobileBg
    val totalBytes get() = wifiFg + wifiBg + mobileFg + mobileBg
    val mobileBytes get() = mobileFg + mobileBg
}

data class NetUsageRow(val label: String, val pkg: String?, val net: UidNet)

data class ConnectionInfo(
    val type: String,             // Wi-Fi, Datos móviles, Sin conexión...
    val validated: Boolean,
    val metered: Boolean,
    val vpn: Boolean,
    val downKbps: Int?, val upKbps: Int?,
    val wifiRssi: Int?, val wifiLinkMbps: Int?, val wifiFreqMhz: Int?, val wifiStandard: String?,
    val dataSaver: Boolean,
) {
    val wifiBand: String? get() = wifiFreqMhz?.let {
        when { it >= 5925 -> "6 GHz"; it >= 4900 -> "5 GHz"; else -> "2.4 GHz" }
    }
    val signalQuality: String? get() = wifiRssi?.let {
        when { it >= -55 -> "Excelente"; it >= -67 -> "Buena"; it >= -75 -> "Regular"; else -> "Débil" }
    }
}

data class SpeedResult(val latencyMs: Int?, val jitterMs: Int?, val dnsMs: Int?, val downMbps: Double?, val upMbps: Double?, val error: String?)

object NetworkRepo {

    fun perUid(ctx: Context, start: Long, end: Long): List<UidNet> {
        if (!Perms.hasUsageAccess(ctx)) return emptyList()
        val nsm = ctx.getSystemService(NetworkStatsManager::class.java)
        val map = HashMap<Int, UidNet>()
        @Suppress("DEPRECATION")
        for (type in intArrayOf(ConnectivityManager.TYPE_WIFI, ConnectivityManager.TYPE_MOBILE)) {
            val stats = try { nsm.querySummary(type, null, start, end) } catch (_: Exception) { null } ?: continue
            val b = NetworkStats.Bucket()
            while (stats.hasNextBucket()) {
                stats.getNextBucket(b)
                val bytes = b.rxBytes + b.txBytes
                if (bytes <= 0) continue
                val u = map.getOrPut(b.uid) { UidNet(b.uid) }
                val fg = b.state == NetworkStats.Bucket.STATE_FOREGROUND
                @Suppress("DEPRECATION")
                if (type == ConnectivityManager.TYPE_WIFI) { if (fg) u.wifiFg += bytes else u.wifiBg += bytes }
                else { if (fg) u.mobileFg += bytes else u.mobileBg += bytes }
            }
            stats.close()
        }
        return map.values.toList()
    }

    fun usageRows(ctx: Context, hours: Int): List<NetUsageRow> {
        val end = System.currentTimeMillis()
        val pm = ctx.packageManager
        return perUid(ctx, end - hours * 3_600_000L, end).map { u ->
            val pkgs = pm.getPackagesForUid(u.uid)
            val (label, pkg) = when {
                u.uid == -4 -> "Apps desinstaladas" to null
                u.uid == -5 -> "Zona Wi-Fi / anclaje" to null
                u.uid == 0 -> "Sistema (root)" to null
                u.uid == 1000 -> "Sistema Android" to "android"
                pkgs.isNullOrEmpty() -> "UID ${u.uid}" to null
                else -> {
                    val p = pkgs.first()
                    val l = try { pm.getApplicationInfo(p, 0).loadLabel(pm).toString() } catch (_: Exception) { p }
                    (if (pkgs.size > 1) "$l (+${pkgs.size - 1})" else l) to p
                }
            }
            NetUsageRow(label, pkg, u)
        }.sortedByDescending { it.net.totalBytes }
    }

    fun connection(ctx: Context): ConnectionInfo {
        val cm = ctx.getSystemService(ConnectivityManager::class.java)
        val net = cm.activeNetwork
        val caps = net?.let { cm.getNetworkCapabilities(it) }
        val type = when {
            caps == null -> "Sin conexión"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi-Fi"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Datos móviles"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> "VPN"
            else -> "Otra"
        }
        var rssi: Int? = null; var link: Int? = null; var freq: Int? = null; var std: String? = null
        if (type == "Wi-Fi") {
            val wi: WifiInfo? = if (Build.VERSION.SDK_INT >= 31) caps?.transportInfo as? WifiInfo else null
            @Suppress("DEPRECATION")
            val info = wi ?: ctx.applicationContext.getSystemService(WifiManager::class.java).connectionInfo
            info?.let {
                rssi = it.rssi.takeIf { r -> r in -127..0 }
                link = it.linkSpeed.takeIf { l -> l > 0 }
                freq = it.frequency.takeIf { f -> f > 0 }
                if (Build.VERSION.SDK_INT >= 30) std = when (it.wifiStandard) {
                    4 -> "Wi-Fi 4 (n)"; 5 -> "Wi-Fi 5 (ac)"; 6 -> "Wi-Fi 6/6E (ax)"; 8 -> "Wi-Fi 7 (be)"
                    1 -> "Legacy"; else -> null
                }
            }
        }
        return ConnectionInfo(
            type = type,
            validated = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) == true,
            metered = cm.isActiveNetworkMetered,
            vpn = caps?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) == true,
            downKbps = caps?.linkDownstreamBandwidthKbps, upKbps = caps?.linkUpstreamBandwidthKbps,
            wifiRssi = rssi, wifiLinkMbps = link, wifiFreqMhz = freq, wifiStandard = std,
            dataSaver = cm.restrictBackgroundStatus == ConnectivityManager.RESTRICT_BACKGROUND_STATUS_ENABLED,
        )
    }

    fun totalBytes(): Pair<Long, Long> = TrafficStats.getTotalRxBytes() to TrafficStats.getTotalTxBytes()

    private fun tcpLatency(host: String = "1.1.1.1", port: Int = 443): Int? = try {
        val t = System.nanoTime()
        Socket().use { it.connect(InetSocketAddress(host, port), 3000) }
        ((System.nanoTime() - t) / 1_000_000).toInt()
    } catch (_: Exception) { null }

    /** Prueba de velocidad contra Cloudflare (servidor cercano). Bloqueante: llamar desde IO. */
    fun speedTest(onProgress: (String) -> Unit): SpeedResult {
        return try {
            onProgress("Midiendo latencia…")
            val pings = (1..6).mapNotNull { tcpLatency() }
            val latency = pings.minOrNull()
            val jitter = if (pings.size > 1) pings.zipWithNext { a, b -> kotlin.math.abs(a - b) }.average().toInt() else null
            val dns = try {
                val t = System.nanoTime(); InetAddress.getByName("samsung.com")
                ((System.nanoTime() - t) / 1_000_000).toInt()
            } catch (_: Exception) { null }

            onProgress("Midiendo descarga…")
            val down = run {
                val c = URL("https://speed.cloudflare.com/__down?bytes=50000000").openConnection() as HttpURLConnection
                c.connectTimeout = 5000; c.readTimeout = 10000
                val buf = ByteArray(64 * 1024)
                var total = 0L
                val t0 = System.nanoTime()
                c.inputStream.use { s ->
                    while (true) {
                        val r = s.read(buf); if (r < 0) break
                        total += r
                        if (System.nanoTime() - t0 > 8_000_000_000L) break // máx 8 s
                    }
                }
                c.disconnect()
                val sec = (System.nanoTime() - t0) / 1e9
                total * 8 / sec / 1e6
            }

            onProgress("Midiendo subida…")
            val up = try {
                val payload = ByteArray(10_000_000)
                val c = URL("https://speed.cloudflare.com/__up").openConnection() as HttpURLConnection
                c.requestMethod = "POST"; c.doOutput = true
                c.connectTimeout = 5000; c.readTimeout = 15000
                c.setFixedLengthStreamingMode(payload.size)
                val t0 = System.nanoTime()
                c.outputStream.use { o ->
                    var off = 0
                    while (off < payload.size) { val n = minOf(64 * 1024, payload.size - off); o.write(payload, off, n); off += n }
                }
                c.responseCode
                val sec = (System.nanoTime() - t0) / 1e9
                c.disconnect()
                payload.size * 8 / sec / 1e6
            } catch (_: Exception) { null }
            SpeedResult(latency, jitter, dns, down, up, null)
        } catch (e: Exception) {
            SpeedResult(null, null, null, null, null, e.message ?: "Error de red")
        }
    }
}
