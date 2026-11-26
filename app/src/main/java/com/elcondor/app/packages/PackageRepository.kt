package com.elcondor.app.packages

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import com.elcondor.app.shell.ShellCommandException
import com.elcondor.app.shell.ShizukuShellManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

data class SystemPackage(
    val packageName: String,
    val label: String,
    val risk: RemovalRisk,
)

data class TrashItem(
    val packageName: String,
    val label: String,
    /** null si se desinstaló fuera de la app (ADB, otra herramienta) y no hay registro. */
    val removedAt: Long?,
)

data class PackageSnapshot(
    val installed: List<SystemPackage>,
    val trash: List<TrashItem>,
)

/**
 * Acceso a los paquetes del sistema del usuario principal (user 0).
 *
 * `pm uninstall -k --user 0` NO borra el APK de /system: solo lo "quita" para este
 * usuario (conservando datos con -k). Por eso `install-existing` lo restaura al
 * instante sin descargar nada, y un reset de fábrica lo devuelve todo.
 */
class PackageRepository(
    context: Context,
    private val shell: ShizukuShellManager,
    private val trashStore: TrashStore,
) {
    private val appContext = context.applicationContext
    private val pm: PackageManager = appContext.packageManager
    private val labelCache = ConcurrentHashMap<String, String>()

    /**
     * Lista instalados y "en papelera". La papelera se calcula como
     * (todos los de sistema, incluidos los desinstalados) − (instalados), y se
     * enriquece con el historial guardado (nombre y fecha).
     */
    suspend fun loadSnapshot(): Result<PackageSnapshot> = withContext(Dispatchers.IO) {
        val installedOut = shell.executeCommand("pm list packages -s --user $USER_ID")
            .getOrElse { return@withContext Result.failure(it) }
        val allOut = shell.executeCommand("pm list packages -s -u --user $USER_ID")
            .getOrElse { return@withContext Result.failure(it) }

        val installed = PackageParsers.parsePackageList(installedOut)
        val uninstalled = PackageParsers.parsePackageList(allOut).toSet() - installed.toSet()

        // Lo restaurado por fuera de la app deja de aparecer en el historial.
        trashStore.retainOnly(uninstalled)
        val history = trashStore.entries()

        val policy = RemovalPolicy(dynamicProtectedPackages())
        Result.success(
            PackageSnapshot(
                installed = installed
                    .map { SystemPackage(it, labelOf(it), policy.riskOf(it)) }
                    .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.label }),
                trash = uninstalled
                    .map { pkg ->
                        val entry = history[pkg]
                        TrashItem(pkg, entry?.label ?: labelOf(pkg), entry?.removedAt)
                    }
                    // Lo más reciente primero; lo desconocido al final.
                    .sortedWith(compareByDescending<TrashItem> { it.removedAt ?: Long.MIN_VALUE }.thenBy { it.label }),
            ),
        )
    }

    /** `pm uninstall -k --user 0 <pkg>` + registro en la papelera. */
    suspend fun uninstall(packageName: String): Result<Unit> = withContext(Dispatchers.IO) {
        if (!PackageParsers.isValidPackageName(packageName)) {
            return@withContext Result.failure(IllegalArgumentException("Nombre de paquete inválido"))
        }

        // Se vuelve a comprobar aquí (no solo en la UI) por si el launcher/teclado cambió.
        val risk = RemovalPolicy(dynamicProtectedPackages()).riskOf(packageName)
        if (risk == RemovalRisk.BLOCKED) {
            return@withContext Result.failure(IllegalStateException("$packageName está protegido"))
        }

        val label = labelOf(packageName) // Antes de borrar: luego puede no resolverse.
        val command = "pm uninstall -k --user $USER_ID $packageName"
        shell.executeRaw(command).mapCatching { result ->
            if (!PackageParsers.isUninstallSuccess(result.stdout)) {
                throw ShellCommandException(command, result.copy(
                    stderr = PackageParsers.failureReason(result.stdout, result.stderr),
                ))
            }
            trashStore.add(TrashEntry(packageName, label, System.currentTimeMillis()))
        }
    }

    /** `cmd package install-existing --user 0 <pkg>` + se quita de la papelera. */
    suspend fun restore(packageName: String): Result<Unit> = withContext(Dispatchers.IO) {
        if (!PackageParsers.isValidPackageName(packageName)) {
            return@withContext Result.failure(IllegalArgumentException("Nombre de paquete inválido"))
        }

        val command = "cmd package install-existing --user $USER_ID $packageName"
        shell.executeRaw(command).mapCatching { result ->
            if (!PackageParsers.isInstallExistingSuccess(result.stdout)) {
                throw ShellCommandException(command, result.copy(
                    stderr = PackageParsers.failureReason(result.stdout, result.stderr),
                ))
            }
            trashStore.remove(packageName)
        }
    }

    // --- Internos --------------------------------------------------------------

    /** Paquetes que, si se quitan, dejan ESTE dispositivo sin forma de usarse/recuperarse. */
    private fun dynamicProtectedPackages(): Set<String> = buildSet {
        add(appContext.packageName)
        add(ShizukuShellManager.SHIZUKU_PACKAGE)

        // Launcher por defecto: sin él, pantalla negra tras pulsar Inicio.
        runCatching {
            val home = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
            pm.resolveActivity(home, PackageManager.MATCH_DEFAULT_ONLY)
                ?.activityInfo?.packageName
                ?.takeIf { it != "android" } // "android" = selector, no hay launcher fijado.
        }.getOrNull()?.let(::add)

        // Teclado activo: sin él no se puede ni escribir el PIN.
        runCatching {
            Settings.Secure.getString(appContext.contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD)
                ?.substringBefore('/')
        }.getOrNull()?.takeIf { it.isNotBlank() }?.let(::add)
    }

    /**
     * Nombre visible. Requiere QUERY_ALL_PACKAGES (Android 11+) y
     * MATCH_UNINSTALLED_PACKAGES para resolver los que están en la papelera.
     */
    private fun labelOf(packageName: String): String = labelCache.getOrPut(packageName) {
        try {
            pm.getApplicationLabel(applicationInfo(packageName)).toString()
                .takeIf { it.isNotBlank() } ?: packageName
        } catch (_: PackageManager.NameNotFoundException) {
            packageName
        }
    }

    private fun applicationInfo(packageName: String): ApplicationInfo {
        val flags = PackageManager.MATCH_UNINSTALLED_PACKAGES
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.getApplicationInfo(packageName, PackageManager.ApplicationInfoFlags.of(flags.toLong()))
        } else {
            @Suppress("DEPRECATION")
            pm.getApplicationInfo(packageName, flags)
        }
    }

    private companion object {
        /** Usuario principal. Perfiles de trabajo / multiusuario: fuera de alcance por ahora. */
        const val USER_ID = 0
    }
}
