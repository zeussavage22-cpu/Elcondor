package dev.darknet.data

/** Compares the public IPs before and after a mobile data restart. */
object IpChange {
    /** The /64 network an IPv6 address belongs to: what the carrier assigns per session. */
    fun prefix6(ip: String): String = ip.split(':').take(4).joinToString(":")

    fun changed4(before: String, after: String) = after.isNotEmpty() && before.isNotEmpty() && after != before

    fun changed6(before: String, after: String) =
        after.isNotEmpty() && before.isNotEmpty() && prefix6(after) != prefix6(before)

    fun changed(before4: String, after4: String, before6: String, after6: String) =
        changed4(before4, after4) || changed6(before6, after6)

    /** Short form of an IPv6 address for the screen: its /64 network. */
    fun short6(ip: String): String = if (ip.isEmpty()) "–" else prefix6(ip) + "::"
}
