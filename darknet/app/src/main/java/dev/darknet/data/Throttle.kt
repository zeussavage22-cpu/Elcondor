package dev.darknet.data

import kotlin.math.abs

/** Reads the result of the throttling test: a normal download against a video file. */
object Throttle {
    /** Speeds carriers commonly cut plans down to, in Mbps. */
    val CAPS = listOf(0.25, 0.6, 1.0, 1.5, 3.0, 5.0)

    data class Verdict(val warn: Boolean, val text: String)

    fun nearCap(mbps: Double): Double? = CAPS.firstOrNull { abs(mbps - it) <= it * 0.2 }

    private fun quality(mbps: Double) = when {
        mbps <= 2.2 -> "480p"
        mbps <= 5.5 -> "720p"
        else -> "1080p o más"
    }

    fun verdict(normal: Double?, video: Double?): Verdict {
        if (normal == null || normal <= 0) return Verdict(true, "No se pudo completar la prueba. Revisa tu señal y repite.")
        nearCap(normal)?.let { cap ->
            return Verdict(
                true,
                "Todo va a ~%.1f Mbps, una velocidad típica de plan reducido: puede que hayas pasado tu límite de datos rápidos o de hotspot.".format(cap),
            )
        }
        if (video == null || video <= 0) {
            return Verdict(false, "Descarga normal a %.0f Mbps. La prueba de video no se pudo hacer.".format(normal))
        }
        val ratio = video / normal
        return if (ratio < 0.5) {
            Verdict(
                true,
                "El video va a %.1f Mbps y lo normal a %.0f Mbps: tu operadora probablemente limita el video (calidad ≈ %s).".format(video, normal, quality(video)),
            )
        } else {
            Verdict(false, "No se ve limitación: normal %.0f Mbps, video %.0f Mbps.".format(normal, video))
        }
    }
}
