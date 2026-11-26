package com.elcondor.app.packages

enum class RemovalRisk {
    /** Romperías el arranque/la UI: la app no permite desinstalarlo. */
    BLOCKED,

    /** Funciona, pero puede romper funciones importantes. Confirmación reforzada. */
    CAUTION,

    /** Sin información especial: confirmación normal. */
    NORMAL,
}

/**
 * Clasifica paquetes por riesgo de desinstalación.
 *
 * Aunque `pm uninstall -k --user 0` es reversible, quitar p. ej. SystemUI o el
 * launcher deja el teléfono sin interfaz y la reversión exige ADB desde un PC.
 * Por eso esos paquetes están bloqueados, no solo avisados.
 *
 * @param dynamicProtected paquetes protegidos en ESTE dispositivo: la propia app,
 *   Shizuku, el launcher por defecto y el teclado activo.
 */
class RemovalPolicy(private val dynamicProtected: Set<String>) {

    fun riskOf(packageName: String): RemovalRisk = when {
        packageName in dynamicProtected -> RemovalRisk.BLOCKED
        packageName in BLOCKED -> RemovalRisk.BLOCKED
        BLOCKED_PREFIXES.any(packageName::startsWith) -> RemovalRisk.BLOCKED
        packageName in CAUTION -> RemovalRisk.CAUTION
        CAUTION_PREFIXES.any(packageName::startsWith) -> RemovalRisk.CAUTION
        else -> RemovalRisk.NORMAL
    }

    companion object {
        val BLOCKED = setOf(
            "android",
            "com.android.systemui",
            "com.android.settings",
            "com.android.phone",
            "com.android.shell",
            "com.android.server.telecom",
            "com.android.packageinstaller",
            "com.google.android.packageinstaller",
            "com.android.permissioncontroller",
            "com.google.android.permissioncontroller",
            "com.android.inputdevices",
            "com.android.location.fused",
            "com.android.keychain",
            "com.android.externalstorage",
            "com.android.documentsui",
            "com.google.android.documentsui",
            "com.android.se",
        )

        val BLOCKED_PREFIXES = listOf(
            "com.android.providers.",   // settings, telephony, media, contacts...
            "com.android.networkstack",
            "com.google.android.networkstack",
            "com.android.wifi",
            "com.google.android.wifi",
        )

        val CAUTION = setOf(
            "com.google.android.gms",          // Servicios de Google: rompe la mayoría de apps
            "com.google.android.gsf",
            "com.android.vending",             // Play Store
            "com.google.android.webview",
            "com.android.webview",
            "com.google.android.trichromelibrary",
            "com.android.bluetooth",
            "com.android.nfc",
            "com.android.stk",
            "com.android.mms.service",
            "com.google.android.ext.services",
            "com.android.emergency",
            "com.google.android.apps.safetyhub",
        )

        val CAUTION_PREFIXES = listOf(
            "com.android.cellbroadcast",       // Alertas de emergencia
            "com.google.android.cellbroadcast",
            "com.android.launcher",
            "com.google.android.inputmethod",
            "com.android.inputmethod",
        )
    }
}
