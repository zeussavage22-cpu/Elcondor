package com.elcondor.app.perapp

/**
 * Lógica PURA del DPI por app (testeable en la JVM).
 *
 * Android no tiene DPI por aplicación sin root. Lo emulamos así:
 *   - al pasar a primer plano una app con regla → `wm density <regla>`
 *   - al pasar a una app sin regla → se restaura el DPI "base" (el que había antes)
 */
sealed interface DpiDecision {
    /** Evento irrelevante (teclado, panel de notificaciones...): no tocar nada. */
    data object Ignore : DpiDecision

    data class ApplyRule(val dpi: Int) : DpiDecision

    data object RestoreBaseline : DpiDecision
}

object PerAppDpiLogic {

    /**
     * Ventanas que aparecen "encima" de la app actual sin sustituirla.
     * Si las tratáramos como cambio de app, abrir el teclado o bajar las
     * notificaciones restauraría el DPI en mitad de la app con regla.
     */
    val ALWAYS_IGNORED = setOf(
        "android",
        "com.android.systemui",
        "com.android.permissioncontroller",
        "com.google.android.permissioncontroller",
    )

    fun decide(
        foregroundPackage: String,
        rules: Map<String, Int>,
        ignored: Set<String>,
        ruleModeActive: Boolean,
    ): DpiDecision = when {
        foregroundPackage in ALWAYS_IGNORED || foregroundPackage in ignored -> DpiDecision.Ignore
        foregroundPackage in rules -> DpiDecision.ApplyRule(rules.getValue(foregroundPackage))
        ruleModeActive -> DpiDecision.RestoreBaseline
        else -> DpiDecision.Ignore // Ya estamos en el DPI base: nada que hacer.
    }

    /** Comando para volver al DPI base. null = no había override → reset. */
    fun baselineCommand(baseline: Int?): String =
        baseline?.let { "wm density $it" } ?: "wm density reset"

    // --- Lista de servicios de accesibilidad -------------------------------

    /** "paquete/clase" tal como lo guarda Settings.Secure. Sin metacaracteres de shell peligrosos. */
    private val COMPONENT = Regex("""^[A-Za-z0-9_.]+/[A-Za-z0-9_.$]+$""")

    /**
     * Devuelve la nueva lista `enabled_accessibility_services` con [component]
     * añadido o quitado, conservando los servicios que el usuario ya tuviera.
     *
     * @return null si la lista actual tiene entradas con formato inesperado: en ese
     *   caso NO la reescribimos para no romper otros servicios de accesibilidad.
     */
    fun editAccessibilityList(current: String, component: String, enable: Boolean): List<String>? {
        val raw = current.trim()
        val entries = if (raw.isEmpty() || raw == "null") emptyList() else raw.split(':').filter { it.isNotBlank() }
        if (entries.any { !COMPONENT.matches(it) }) return null

        // Android puede guardar el nombre corto (paquete/.Clase): lo tratamos como el mismo.
        val pkg = component.substringBefore('/')
        val shortForm = pkg + "/" + component.substringAfter('/').removePrefix(pkg)
        val others = entries.filter { it != component && it != shortForm }
        return if (enable) others + component else others
    }

    fun accessibilityListCommand(entries: List<String>, enableAccessibility: Boolean): String {
        val write = if (entries.isEmpty()) {
            "settings delete secure enabled_accessibility_services"
        } else {
            // Comillas simples: `$` en nombres de clases internas no debe expandirse.
            "settings put secure enabled_accessibility_services '${entries.joinToString(":")}'"
        }
        return if (enableAccessibility) "$write && settings put secure accessibility_enabled 1" else write
    }
}
