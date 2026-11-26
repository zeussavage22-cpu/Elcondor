package com.elcondor.app.display

import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Modelos y parsers PUROS (sin dependencias de Android) para `wm density` / `wm size`.
 * Al no tocar el framework se pueden testear en la JVM (ver src/test).
 */

data class Resolution(val width: Int, val height: Int) {
    /** Formato que entiende `wm size`: 1080x2400. */
    override fun toString(): String = "${width}x$height"

    val aspectRatio: Float get() = width.toFloat() / height

    /** Escala manteniendo proporción y redondeando a par (evita líneas de 1 px en algunos GPU). */
    fun scaled(factor: Float): Resolution =
        Resolution(roundToEven(width * factor), roundToEven(height * factor))

    companion object {
        private val REGEX = Regex("""(\d{2,5})\s*x\s*(\d{2,5})""")

        fun parse(text: String): Resolution? = REGEX.find(text)?.let {
            Resolution(it.groupValues[1].toInt(), it.groupValues[2].toInt())
        }

        private fun roundToEven(value: Float): Int = (value / 2f).roundToInt() * 2
    }
}

/** Lectura de `wm density`. override == null → se usa el valor físico (fábrica). */
data class DensityReading(val physical: Int, val override: Int?) {
    val effective: Int get() = override ?: physical
}

/** Lectura de `wm size`. */
data class SizeReading(val physical: Resolution, val override: Resolution?) {
    val effective: Resolution get() = override ?: physical
}

data class DisplayInfo(val density: DensityReading, val size: SizeReading)

object DisplayParsers {
    private val PHYSICAL_DENSITY = Regex("""Physical density:\s*(\d+)""")
    private val OVERRIDE_DENSITY = Regex("""Override density:\s*(\d+)""")
    private val PHYSICAL_SIZE = Regex("""Physical size:\s*(\S+)""")
    private val OVERRIDE_SIZE = Regex("""Override size:\s*(\S+)""")

    /**
     * Physical density: 420
     * Override density: 380   ← solo si hay override
     */
    fun parseDensity(output: String): DensityReading? {
        val physical = PHYSICAL_DENSITY.find(output)?.groupValues?.get(1)?.toIntOrNull() ?: return null
        val override = OVERRIDE_DENSITY.find(output)?.groupValues?.get(1)?.toIntOrNull()
        return DensityReading(physical, override)
    }

    /**
     * Physical size: 1080x2400
     * Override size: 900x2000   ← solo si hay override
     */
    fun parseSize(output: String): SizeReading? {
        val physical = PHYSICAL_SIZE.find(output)?.groupValues?.get(1)?.let(Resolution::parse) ?: return null
        val override = OVERRIDE_SIZE.find(output)?.groupValues?.get(1)?.let(Resolution::parse)
        return SizeReading(physical, override)
    }
}

/** Límites de seguridad: lo que no pase por aquí nunca llega a la shell. */
object DisplayLimits {
    const val MIN_DPI = 120
    const val MAX_DPI = 800

    const val MIN_SIDE_PX = 320
    const val MIN_SCALE = 0.5f
    const val MAX_SCALE = 1.5f

    /** Diferencia máxima de proporción respecto al panel (evita imagen muy deformada). */
    const val MAX_ASPECT_DEVIATION = 0.20f

    fun validateDensity(dpi: Int): String? =
        if (dpi in MIN_DPI..MAX_DPI) null else "El DPI debe estar entre $MIN_DPI y $MAX_DPI"

    /** Devuelve un mensaje de error, o null si [target] es seguro para este panel. */
    fun validateSize(target: Resolution, physical: Resolution): String? {
        val (w, h) = target
        if (w < MIN_SIDE_PX || h < MIN_SIDE_PX) return "Cada lado debe medir al menos $MIN_SIDE_PX px"

        val wRange = (physical.width * MIN_SCALE).toInt()..(physical.width * MAX_SCALE).toInt()
        val hRange = (physical.height * MIN_SCALE).toInt()..(physical.height * MAX_SCALE).toInt()
        if (w !in wRange || h !in hRange) {
            return "Fuera de rango: ancho ${wRange.first}–${wRange.last}, alto ${hRange.first}–${hRange.last}"
        }

        // Ancho/alto invertidos respecto al panel → la UI quedaría girada y comprimida.
        if ((w <= h) != (physical.width <= physical.height)) {
            return "Orientación invertida: el panel es ${physical}"
        }

        val deviation = abs(target.aspectRatio - physical.aspectRatio) / physical.aspectRatio
        if (deviation > MAX_ASPECT_DEVIATION) {
            return "Proporción demasiado distinta a la del panel (${physical})"
        }
        return null
    }
}
