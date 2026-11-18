package dev.darknet.data

import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket

/** Measures DNS servers so "Automático" can pick the fastest one. */
object DnsProbe {

    /** A DNS query for an A record of [name], with transaction id [id]. */
    fun query(id: Int, name: String): ByteArray {
        val out = java.io.ByteArrayOutputStream()
        out.write(id shr 8 and 0xff); out.write(id and 0xff)
        out.write(0x01); out.write(0x00) // recursion desired
        out.write(0); out.write(1) // one question
        repeat(6) { out.write(0) }
        for (label in name.trimEnd('.').split('.')) {
            out.write(label.length)
            out.write(label.toByteArray(Charsets.US_ASCII))
        }
        out.write(0)
        out.write(0); out.write(1) // type A
        out.write(0); out.write(1) // class IN
        return out.toByteArray()
    }

    /** True if [reply] answers query [id] without an error code. */
    fun isAnswer(reply: ByteArray, length: Int, id: Int): Boolean =
        length >= 12 &&
            (reply[0].toInt() and 0xff) == (id shr 8 and 0xff) &&
            (reply[1].toInt() and 0xff) == (id and 0xff) &&
            (reply[2].toInt() and 0x80) != 0 &&
            (reply[3].toInt() and 0x0f) == 0

    /** Round-trip time of one lookup in ms, or null if it failed. */
    fun lookup(server: String, name: String, timeoutMs: Int = 1500): Long? = runCatching {
        DatagramSocket().use { socket ->
            socket.soTimeout = timeoutMs
            val id = (Math.random() * 0xffff).toInt()
            val q = query(id, name)
            val start = System.nanoTime()
            socket.send(DatagramPacket(q, q.size, InetAddress.getByName(server), 53))
            val buf = ByteArray(512)
            var rtt: Long? = null
            while (rtt == null) {
                val p = DatagramPacket(buf, buf.size)
                socket.receive(p) // throws on timeout
                if (isAnswer(buf, p.length, id)) rtt = (System.nanoTime() - start) / 1_000_000
            }
            rtt
        }
    }.getOrNull()

    /** Typical time for [server]: the median of a few lookups; null if most failed. */
    fun measure(server: String): Long? {
        val names = listOf("google.com", "youtube.com", "whatsapp.net", "instagram.com", "netflix.com")
        val times = names.mapNotNull { lookup(server, it) }.sorted()
        return if (times.size >= 3) times[times.size / 2] else null
    }

    /** Picks the fastest of [candidates] (key, label, first server); null if none answered. */
    fun fastest(candidates: List<Triple<String, String, String>>): Triple<String, String, Long>? =
        candidates.mapNotNull { (key, label, server) -> measure(server)?.let { Triple(key, label, it) } }
            .minByOrNull { it.third }
}

/** Connection quality while sharing, from regular ping-like checks. */
data class Health(
    /** Last measurements in ms, newest last; null = failed. */
    val samples: List<Long?> = emptyList(),
    /** Times the connection went away this session. */
    val drops: Int = 0,
    val offline: Boolean = false,
    val slow: Boolean = false,
) {
    enum class Quality(val label: String) { UNKNOWN("Midiendo…"), GREAT("Excelente"), GOOD("Buena"), UNSTABLE("Inestable"), BAD("Lenta"), OFFLINE("Sin internet") }

    private val ok: List<Long> get() = samples.filterNotNull()
    val last: Long? get() = samples.lastOrNull()
    val average: Long? get() = ok.takeIf { it.isNotEmpty() }?.average()?.toLong()

    /** Average change between consecutive successful checks. */
    val jitter: Long? get() = ok.zipWithNext { a, b -> kotlin.math.abs(a - b) }.takeIf { it.isNotEmpty() }?.average()?.toLong()
    val loss: Int get() = if (samples.isEmpty()) 0 else samples.count { it == null } * 100 / samples.size

    val quality: Quality get() {
        val avg = average ?: return if (samples.isEmpty()) Quality.UNKNOWN else Quality.OFFLINE
        val j = jitter ?: 0
        return when {
            offline -> Quality.OFFLINE
            avg > 300 || loss >= 20 -> Quality.BAD
            j > 60 || loss >= 5 -> Quality.UNSTABLE
            avg <= 60 -> Quality.GREAT
            else -> Quality.GOOD
        }
    }

    /** Adds one check. Offline after 3 failures in a row; slow after 6 checks over 300 ms. */
    fun add(ms: Long?): Health {
        val next = (samples + ms).takeLast(WINDOW)
        val failedRun = next.takeLastWhile { it == null }.size
        val nowOffline = if (offline) ms == null else failedRun >= 3
        val recent = next.takeLast(6)
        val nowSlow = recent.size == 6 && recent.all { it != null && it > 300 }
        return copy(samples = next, offline = nowOffline, slow = nowSlow, drops = drops + if (nowOffline && !offline) 1 else 0)
    }

    companion object {
        /** About two minutes at one check every 5 s. */
        const val WINDOW = 24

        /** Time to open a TCP connection, a ping that works without root. */
        fun ping(host: String = "1.1.1.1", port: Int = 443, timeoutMs: Int = 2000): Long? = runCatching {
            Socket().use { s ->
                val start = System.nanoTime()
                s.connect(InetSocketAddress(host, port), timeoutMs)
                (System.nanoTime() - start) / 1_000_000
            }
        }.getOrNull()
    }
}
