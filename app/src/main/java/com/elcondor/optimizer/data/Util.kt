package com.elcondor.optimizer.data

import android.app.AppOpsManager
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Process
import android.provider.Settings
import android.widget.Toast
import java.util.Locale

object Perms {
    fun hasUsageAccess(ctx: Context): Boolean {
        val ops = ctx.getSystemService(AppOpsManager::class.java)
        val mode = ops.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), ctx.packageName)
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun hasAllFiles(): Boolean = Build.VERSION.SDK_INT < 30 || Environment.isExternalStorageManager()

    fun openUsageAccess(ctx: Context) {
        val withPkg = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS, Uri.parse("package:${ctx.packageName}"))
        if (!Nav.tryStart(ctx, withPkg)) Nav.tryStart(ctx, Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
    }

    fun openAllFiles(ctx: Context) {
        if (Build.VERSION.SDK_INT < 30) return
        val withPkg = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, Uri.parse("package:${ctx.packageName}"))
        if (!Nav.tryStart(ctx, withPkg)) Nav.tryStart(ctx, Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION))
    }
}

object Nav {
    fun tryStart(ctx: Context, intent: Intent): Boolean = try {
        if (ctx !is android.app.Activity) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        ctx.startActivity(intent); true
    } catch (_: ActivityNotFoundException) { false } catch (_: SecurityException) { false }

    /** Intenta varias pantallas (Samsung primero) y cae a una genérica. */
    fun open(ctx: Context, vararg intents: Intent) {
        for (i in intents) if (tryStart(ctx, i)) return
        Toast.makeText(ctx, "No disponible en este dispositivo", Toast.LENGTH_SHORT).show()
    }

    fun component(pkg: String, cls: String) = Intent().setComponent(ComponentName(pkg, cls))

    fun appDetails(ctx: Context, pkg: String) =
        open(ctx, Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$pkg")))

    fun uninstall(ctx: Context, pkg: String) {
        @Suppress("DEPRECATION")
        open(ctx, Intent(Intent.ACTION_UNINSTALL_PACKAGE, Uri.parse("package:$pkg")),
            Intent(Intent.ACTION_DELETE, Uri.parse("package:$pkg")))
    }

    // --- Atajos Samsung / sistema ---
    fun deviceCare(ctx: Context) = open(ctx,
        component("com.samsung.android.lool", "com.samsung.android.sm.ui.SmartManagerDashBoardActivity"),
        component("com.samsung.android.sm_cn", "com.samsung.android.sm.ui.SmartManagerDashBoardActivity"),
        Intent(Settings.ACTION_SETTINGS))

    fun batterySettings(ctx: Context) = open(ctx,
        component("com.samsung.android.lool", "com.samsung.android.sm.battery.ui.BatteryActivity"),
        Intent(Intent.ACTION_POWER_USAGE_SUMMARY),
        Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS))

    fun batteryUsage(ctx: Context) = open(ctx,
        Intent(Intent.ACTION_POWER_USAGE_SUMMARY),
        component("com.samsung.android.lool", "com.samsung.android.sm.battery.ui.BatteryActivity"))

    fun backgroundLimits(ctx: Context) = open(ctx,
        component("com.samsung.android.lool", "com.samsung.android.sm.battery.ui.usage.CheckableAppListActivity"),
        component("com.samsung.android.lool", "com.samsung.android.sm.battery.ui.BatteryActivity"),
        Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))

    fun dataUsage(ctx: Context) = open(ctx,
        Intent(Settings.ACTION_DATA_USAGE_SETTINGS),
        Intent("android.settings.DATA_USAGE_SETTINGS"),
        Intent(Settings.ACTION_WIRELESS_SETTINGS))

    fun wifi(ctx: Context) = open(ctx, Intent(Settings.ACTION_WIFI_SETTINGS))
    fun display(ctx: Context) = open(ctx, Intent(Settings.ACTION_DISPLAY_SETTINGS))
    fun location(ctx: Context) = open(ctx, Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
    fun bluetooth(ctx: Context) = open(ctx, Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
    fun nfc(ctx: Context) = open(ctx, Intent(Settings.ACTION_NFC_SETTINGS), Intent(Settings.ACTION_WIRELESS_SETTINGS))
    fun storage(ctx: Context) = open(ctx, Intent(Settings.ACTION_INTERNAL_STORAGE_SETTINGS))
    fun developer(ctx: Context) = open(ctx, Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS), Intent(Settings.ACTION_DEVICE_INFO_SETTINGS))
    fun lockscreen(ctx: Context) = open(ctx, Intent("android.settings.LOCK_SCREEN_SETTINGS"), Intent(Settings.ACTION_SECURITY_SETTINGS))
    fun softwareUpdate(ctx: Context) = open(ctx,
        component("com.wssyncmldm", "com.idm.fotaagent.enabler.ui.admin.main.FotaMainActivity"),
        Intent("android.settings.SYSTEM_UPDATE_SETTINGS"),
        Intent(Settings.ACTION_DEVICE_INFO_SETTINGS))
    fun security(ctx: Context) = open(ctx, Intent(Settings.ACTION_SECURITY_SETTINGS))
    fun privateDns(ctx: Context) = open(ctx, Intent("android.settings.WIRELESS_SETTINGS"), Intent(Settings.ACTION_WIRELESS_SETTINGS))
}

object Fmt {
    fun bytes(b: Long): String {
        if (b < 1024) return "$b B"
        val units = arrayOf("KB", "MB", "GB", "TB")
        var v = b / 1024.0; var i = 0
        while (v >= 1024 && i < units.size - 1) { v /= 1024; i++ }
        return if (v >= 100) String.format(Locale.US, "%.0f %s", v, units[i])
        else String.format(Locale.US, "%.1f %s", v, units[i])
    }

    fun duration(ms: Long): String {
        val m = ms / 60_000
        return when {
            m < 1 -> "<1 min"
            m < 60 -> "$m min"
            else -> "${m / 60} h ${m % 60} min"
        }
    }

    fun ago(ts: Long): String {
        if (ts <= 0) return "Nunca / desconocido"
        val d = (System.currentTimeMillis() - ts) / 86_400_000
        return when {
            d < 1 -> "Hoy"
            d == 1L -> "Ayer"
            else -> "Hace $d días"
        }
    }

    fun pct(f: Float) = "${(f * 100).toInt()}%"
}
