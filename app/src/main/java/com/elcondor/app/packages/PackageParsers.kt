package com.elcondor.app.packages

/** Parsers PUROS de la salida de `pm` / `cmd package` (testeables en la JVM). */
object PackageParsers {

    /**
     * Nombre de paquete Java/Android. Solo letras, dígitos, `_` y `.`, así que
     * es imposible colar metacaracteres de shell (`;`, `&`, `$`, espacios...).
     * Se permite un solo segmento porque existe el paquete `android`.
     */
    private val PACKAGE_NAME = Regex("""^[A-Za-z][A-Za-z0-9_]*(\.[A-Za-z0-9_]+)*$""")

    fun isValidPackageName(name: String): Boolean =
        name.length in 1..255 && PACKAGE_NAME.matches(name)

    /** `package:com.foo\npackage:com.bar` → [com.bar, com.foo] (ordenados, sin duplicados). */
    fun parsePackageList(output: String): List<String> =
        output.lineSequence()
            .map(String::trim)
            .filter { it.startsWith("package:") }
            .map { it.removePrefix("package:").trim() }
            .filter(::isValidPackageName)
            .distinct()
            .sorted()
            .toList()

    /**
     * `pm uninstall` imprime "Success" o "Failure [MOTIVO]". En varias versiones de
     * Android el exit code es 0 incluso al fallar, así que hay que leer el texto.
     */
    fun isUninstallSuccess(stdout: String): Boolean =
        stdout.lineSequence().any { it.trim() == "Success" }

    /** `cmd package install-existing` imprime "Package X installed for user: 0". */
    fun isInstallExistingSuccess(stdout: String): Boolean =
        stdout.contains("installed for user")

    /** Extrae el motivo de "Failure [DELETE_FAILED_INTERNAL_ERROR]" para mostrarlo. */
    fun failureReason(stdout: String, stderr: String): String {
        val text = (stdout + "\n" + stderr).trim()
        return Regex("""Failure \[(.+?)]""").find(text)?.groupValues?.get(1)
            ?: text.lineSequence().firstOrNull { it.isNotBlank() }?.trim()
            ?: "error desconocido"
    }
}
