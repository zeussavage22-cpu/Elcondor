package com.elcondor.cloner

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.os.Build
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import java.io.File

/** Outcome of an install session, reported by [InstallResultReceiver]. */
data class InstallResult(val packageName: String?, val success: Boolean, val message: String?)

object InstallEvents {
    private val _results = MutableSharedFlow<InstallResult>(extraBufferCapacity = 8)
    val results: SharedFlow<InstallResult> = _results
    fun emit(result: InstallResult) { _results.tryEmit(result) }
}

object CloneInstaller {
    const val EXTRA_PACKAGE = "com.elcondor.cloner.PACKAGE"

    /** Installs [apks] (base first, then splits) as one app through the system installer. */
    fun install(context: Context, apks: List<File>, packageName: String) {
        val installer = context.packageManager.packageInstaller
        val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL).apply {
            setAppPackageName(packageName)
            if (Build.VERSION.SDK_INT >= 34) setPackageSource(PackageInstaller.PACKAGE_SOURCE_LOCAL_FILE)
        }
        val sessionId = installer.createSession(params)
        try {
            installer.openSession(sessionId).use { session ->
                for (apk in apks) {
                    apk.inputStream().use { input ->
                        session.openWrite(apk.name, 0, apk.length()).use { out ->
                            input.copyTo(out, 1 shl 16)
                            session.fsync(out)
                        }
                    }
                }
                val intent = Intent(context, InstallResultReceiver::class.java)
                    .setPackage(context.packageName)
                    .putExtra(EXTRA_PACKAGE, packageName)
                val flags = PendingIntent.FLAG_UPDATE_CURRENT or
                    (if (Build.VERSION.SDK_INT >= 31) PendingIntent.FLAG_MUTABLE else 0)
                val pending = PendingIntent.getBroadcast(context, sessionId, intent, flags)
                session.commit(pending.intentSender)
            }
        } catch (e: Exception) {
            runCatching { installer.abandonSession(sessionId) }
            throw e
        }
    }
}

class InstallResultReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pkg = intent.getStringExtra(CloneInstaller.EXTRA_PACKAGE)
        when (val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)) {
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                @Suppress("DEPRECATION")
                val confirm = if (Build.VERSION.SDK_INT >= 33) {
                    intent.getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java)
                } else {
                    intent.getParcelableExtra(Intent.EXTRA_INTENT)
                }
                if (confirm != null) {
                    context.startActivity(confirm.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                } else {
                    InstallEvents.emit(InstallResult(pkg, false, "No se pudo abrir la confirmación de instalación"))
                }
            }
            PackageInstaller.STATUS_SUCCESS -> InstallEvents.emit(InstallResult(pkg, true, null))
            else -> {
                val msg = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)
                InstallEvents.emit(InstallResult(pkg, false, explain(status, msg)))
            }
        }
    }

    private fun explain(status: Int, msg: String?): String {
        val base = when (status) {
            PackageInstaller.STATUS_FAILURE_ABORTED -> "Instalación cancelada"
            PackageInstaller.STATUS_FAILURE_BLOCKED -> "La instalación fue bloqueada por el sistema"
            PackageInstaller.STATUS_FAILURE_CONFLICT -> "Ya existe una app con ese paquete firmada con otra clave"
            PackageInstaller.STATUS_FAILURE_INCOMPATIBLE -> "La app no es compatible con este dispositivo"
            PackageInstaller.STATUS_FAILURE_INVALID -> "El APK generado no es válido"
            PackageInstaller.STATUS_FAILURE_STORAGE -> "No hay espacio suficiente"
            else -> "Falló la instalación"
        }
        return if (msg.isNullOrBlank()) base else "$base\n\n$msg"
    }
}
