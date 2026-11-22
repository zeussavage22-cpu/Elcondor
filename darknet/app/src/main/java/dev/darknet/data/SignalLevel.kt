package dev.darknet.data

/**
 * Signal bars from the received power, with thresholds close to what phone makers
 * show in the status bar. Android's own 5G thresholds are much stricter, so a strong
 * 5G signal used to show as one bar.
 */
object SignalLevel {
    /** LTE RSRP in dBm to 0..4. */
    fun lte(rsrp: Int): Int = when {
        rsrp >= -90 -> 4
        rsrp >= -100 -> 3
        rsrp >= -110 -> 2
        rsrp >= -120 -> 1
        else -> 0
    }

    /** 5G SS-RSRP in dBm to 0..4. */
    fun nr(ssRsrp: Int): Int = when {
        ssRsrp >= -95 -> 4
        ssRsrp >= -105 -> 3
        ssRsrp >= -115 -> 2
        ssRsrp >= -125 -> 1
        else -> 0
    }

    private fun valid(dbm: Int?) = dbm != null && dbm in -160..-30

    /** Bars and the dBm they came from (the stronger of LTE and 5G); null if neither is known. */
    fun of(lteRsrp: Int?, nrRsrp: Int?): Pair<Int, Int>? {
        val options = buildList {
            if (valid(lteRsrp)) add(lte(lteRsrp!!) to lteRsrp)
            if (valid(nrRsrp)) add(nr(nrRsrp!!) to nrRsrp)
        }
        return options.maxWithOrNull(compareBy({ it.first }, { it.second }))
    }
}
