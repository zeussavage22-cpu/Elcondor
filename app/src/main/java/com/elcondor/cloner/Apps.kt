package com.elcondor.cloner

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap

data class InstalledApp(
    val packageName: String,
    val label: String,
    val icon: ImageBitmap?,
    val isSystem: Boolean,
    val apkFiles: List<String>,
    val requestedPermissions: List<String>,
)

/** A group of permissions the user can block in a clone. */
data class PermissionGroup(val title: String, val permissions: List<String>)

val PERMISSION_GROUPS = listOf(
    PermissionGroup("Internet (sin conexión)", listOf("android.permission.INTERNET")),
    PermissionGroup("Ubicación", listOf(
        "android.permission.ACCESS_FINE_LOCATION",
        "android.permission.ACCESS_COARSE_LOCATION",
        "android.permission.ACCESS_BACKGROUND_LOCATION",
    )),
    PermissionGroup("Cámara", listOf("android.permission.CAMERA")),
    PermissionGroup("Micrófono", listOf("android.permission.RECORD_AUDIO")),
    PermissionGroup("Contactos", listOf(
        "android.permission.READ_CONTACTS",
        "android.permission.WRITE_CONTACTS",
        "android.permission.GET_ACCOUNTS",
    )),
    PermissionGroup("Teléfono y llamadas", listOf(
        "android.permission.READ_PHONE_STATE",
        "android.permission.READ_PHONE_NUMBERS",
        "android.permission.CALL_PHONE",
        "android.permission.READ_CALL_LOG",
        "android.permission.WRITE_CALL_LOG",
    )),
    PermissionGroup("SMS", listOf(
        "android.permission.SEND_SMS",
        "android.permission.READ_SMS",
        "android.permission.RECEIVE_SMS",
    )),
    PermissionGroup("Fotos, videos y archivos", listOf(
        "android.permission.READ_EXTERNAL_STORAGE",
        "android.permission.WRITE_EXTERNAL_STORAGE",
        "android.permission.READ_MEDIA_IMAGES",
        "android.permission.READ_MEDIA_VIDEO",
        "android.permission.READ_MEDIA_AUDIO",
    )),
    PermissionGroup("Calendario", listOf(
        "android.permission.READ_CALENDAR",
        "android.permission.WRITE_CALENDAR",
    )),
    PermissionGroup("Notificaciones", listOf("android.permission.POST_NOTIFICATIONS")),
)

object AppScanner {

    fun load(context: Context): List<InstalledApp> {
        val pm = context.packageManager
        val iconSize = (48 * context.resources.displayMetrics.density).toInt()
        @Suppress("DEPRECATION")
        val packages = pm.getInstalledPackages(PackageManager.GET_PERMISSIONS)
        return packages.mapNotNull { info ->
            val ai = info.applicationInfo ?: return@mapNotNull null
            if (info.packageName == context.packageName) return@mapNotNull null
            if (pm.getLaunchIntentForPackage(info.packageName) == null) return@mapNotNull null
            InstalledApp(
                packageName = info.packageName,
                label = ai.loadLabel(pm).toString(),
                icon = runCatching { ai.loadIcon(pm).toBitmap(iconSize, iconSize).asImageBitmap() }.getOrNull(),
                isSystem = ai.flags and (ApplicationInfo.FLAG_SYSTEM or ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0,
                apkFiles = listOf(ai.sourceDir) + (ai.splitSourceDirs?.toList() ?: emptyList()),
                requestedPermissions = info.requestedPermissions?.toList() ?: emptyList(),
            )
        }.sortedBy { it.label.lowercase() }
    }

    fun isInstalled(context: Context, pkg: String): Boolean =
        runCatching { context.packageManager.getPackageInfo(pkg, 0) }.isSuccess

    /** Suggests `<original>.clon<N>` with the first N that isn't installed yet. */
    fun suggestPackage(context: Context, original: String): String {
        var n = 1
        while (isInstalled(context, "$original.clon$n")) n++
        return "$original.clon$n"
    }

    fun launch(context: Context, pkg: String): Boolean {
        val intent = context.packageManager.getLaunchIntentForPackage(pkg) ?: return false
        context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        return true
    }
}
