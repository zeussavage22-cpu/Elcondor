package com.elcondor.app.animation

import java.util.Locale

/**
 * Modelos PUROS de animaciones e inmersivo (testeables en la JVM).
 */

/** Las tres escalas de "Opciones de desarrollador", por su clave en Settings.Global. */
enum class AnimationKey(val setting: String, val label: String) {
    WINDOW("window_animation_scale", "Ventanas"),
    TRANSITION("transition_animation_scale", "Transiciones"),
    ANIMATOR("animator_duration_scale", "Duración del animador"),
}

data class AnimationScales(
    val window: Float = DEFAULT,
    val transition: Float = DEFAULT,
    val animator: Float = DEFAULT,
) {
    operator fun get(key: AnimationKey): Float = when (key) {
        AnimationKey.WINDOW -> window
        AnimationKey.TRANSITION -> transition
        AnimationKey.ANIMATOR -> animator
    }

    /** "Rápido" = todas por debajo de 1x. Es lo que muestra el mosaico como activo. */
    val isFast: Boolean get() = AnimationKey.entries.all { this[it] < DEFAULT }

    /** Si las tres coinciden, ese valor; si no, null (configuración mixta). */
    val uniform: Float? get() = window.takeIf { it == transition && it == animator }

    companion object {
        const val DEFAULT = 1.0f
    }
}

object AnimationScale {
    const val MIN = 0f
    const val MAX = 10f
    const val FAST = 0.25f
    val PRESETS = listOf(0f, 0.25f, 0.5f, 0.75f, 1f, 1.5f)

    fun isValid(value: Float): Boolean = !value.isNaN() && value in MIN..MAX

    /**
     * Formato SIEMPRE con punto decimal (Locale.US): en español `"%.2f"` daría
     * "0,25", y `settings put` lo guardaría como texto inválido.
     */
    fun format(value: Float): String =
        String.format(Locale.US, "%.2f", value).trimEnd('0').trimEnd('.').ifEmpty { "0" }

    /** `settings get` devuelve "null" si nunca se tocó → equivale a 1x. */
    fun parse(output: String): Float {
        val value = output.trim().toFloatOrNull() ?: return AnimationScales.DEFAULT
        return if (isValid(value)) value else AnimationScales.DEFAULT
    }

    /** Un único comando con los tres `settings put` encadenados. */
    fun commandForAll(value: Float): String {
        require(isValid(value)) { "Escala fuera de rango" }
        val v = format(value)
        return AnimationKey.entries.joinToString(" && ") { "settings put global ${it.setting} $v" }
    }

    fun commandFor(key: AnimationKey, value: Float): String {
        require(isValid(value)) { "Escala fuera de rango" }
        return "settings put global ${key.setting} ${format(value)}"
    }
}

/**
 * Modos de `policy_control`. Esta clave se eliminó en Android 11 (API 30):
 * allí el comando "funciona" (se guarda) pero el sistema lo ignora.
 */
enum class ImmersiveMode(val policy: String?, val label: String) {
    OFF(null, "Desactivado"),
    FULL("immersive.full=*", "Completo"),
    STATUS("immersive.status=*", "Ocultar barra de estado"),
    NAVIGATION("immersive.navigation=*", "Ocultar navegación");

    /**
     * El valor va entre comillas simples: sin ellas, `*` es un comodín de la
     * shell y podría expandirse a nombres de archivo del directorio actual.
     */
    val command: String
        get() = policy?.let { "settings put global policy_control '$it'" }
            ?: "settings delete global policy_control"

    companion object {
        const val LAST_SUPPORTED_SDK = 29

        fun parse(output: String): ImmersiveMode {
            val value = output.trim()
            return entries.firstOrNull { it.policy != null && it.policy == value } ?: OFF
        }
    }
}
