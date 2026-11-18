package dev.darknet.data

import org.json.JSONObject

/** One device connected to the hotspot. */
data class Device(val name: String, val ip: String, val mac: String, val isUsb: Boolean)

/** What the privileged service reports, parsed for the UI. */
data class ShareStatus(
    val state: State = State.IDLE,
    val detail: String = "",
    val fastPath: Boolean = false,
    val upstreamOk: Boolean = false,
    val bytesUp: Long = 0,
    val bytesDown: Long = 0,
    val since: Long = 0,
    val devices: List<Device> = emptyList(),
    /** Bytes per second, both directions, from the last two reports. */
    val speed: Long = 0,
) {
    enum class State { IDLE, STARTING, ACTIVE, STOPPING, ERROR }

    val isActive: Boolean get() = state == State.ACTIVE
    val isBusy: Boolean get() = state == State.STARTING || state == State.STOPPING
    val total: Long get() = bytesUp + bytesDown

    companion object {
        fun parse(json: String, previous: ShareStatus? = null, elapsedMs: Long = 0): ShareStatus {
            val o = runCatching { JSONObject(json) }.getOrElse { return ShareStatus(State.ERROR, "bad status") }
            val state = when (o.optString("state")) {
                "active" -> State.ACTIVE
                "starting" -> State.STARTING
                "error" -> State.ERROR
                else -> State.IDLE
            }
            val devices = buildList {
                val array = o.optJSONArray("clients")
                for (i in 0 until (array?.length() ?: 0)) {
                    val d = array!!.getJSONObject(i)
                    add(Device(d.optString("name"), d.optString("ip"), d.optString("mac"), d.optBoolean("usb")))
                }
            }
            val up = o.optLong("up")
            val down = o.optLong("down")
            val speed = if (previous != null && previous.isActive && elapsedMs > 0) {
                val delta = (up + down) - previous.total
                if (delta >= 0) delta * 1000 / elapsedMs else 0
            } else {
                0
            }
            return ShareStatus(
                state = state,
                detail = o.optString("detail"),
                fastPath = o.optBoolean("fastPath"),
                upstreamOk = o.optBoolean("upstreamOk"),
                bytesUp = up,
                bytesDown = down,
                since = o.optLong("since"),
                devices = devices,
                speed = speed,
            )
        }
    }
}

/** Human-readable byte counts (SI units, like carriers use). */
object Bytes {
    private val units = listOf("B", "KB", "MB", "GB", "TB")

    fun format(bytes: Long): String {
        if (bytes < 1000) return "$bytes B"
        var value = bytes.toDouble()
        var unit = 0
        while (value >= 1000 && unit < units.lastIndex) {
            value /= 1000
            unit++
        }
        return if (value < 10) "%.1f %s".format(value, units[unit]) else "%.0f %s".format(value, units[unit])
    }

    fun rate(bytesPerSecond: Long): String = "${format(bytesPerSecond)}/s"

    fun duration(ms: Long): String {
        val s = (ms / 1000).coerceAtLeast(0)
        val h = s / 3600
        val m = (s % 3600) / 60
        val sec = s % 60
        return if (h > 0) "%d:%02d:%02d".format(h, m, sec) else "%d:%02d".format(m, sec)
    }
}
