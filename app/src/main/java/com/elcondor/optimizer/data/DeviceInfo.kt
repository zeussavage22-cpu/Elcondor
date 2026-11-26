package com.elcondor.optimizer.data

import android.app.ActivityManager
import android.app.usage.StorageStatsManager
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.location.LocationManager
import android.nfc.NfcAdapter
import android.os.BatteryManager
import android.os.Build
import android.os.PowerManager
import android.os.SystemClock
import android.os.storage.StorageManager
import android.provider.Settings
import android.view.WindowManager
import java.io.File

data class BatterySnapshot(
    val level: Int,
    val temperatureC: Float,
    val voltageMv: Int,
    val health: String,
    val healthGood: Boolean,
    val status: String,
    val charging: Boolean,
    val plugged: String,
    val technology: String,
    val currentMa: Int?,
    val cycleCount: Int?,
    val chargeCounterMah: Int?,
) {
    /** Potencia instantánea en vatios (positivo). */
    val watts: Float?
        get() = currentMa?.let { kotlin.math.abs(it) * voltageMv / 1_000_000f }
}

data class MemorySnapshot(val totalBytes: Long, val availBytes: Long, val lowMemory: Boolean) {
    val usedBytes get() = totalBytes - availBytes
    val usedFraction get() = if (totalBytes == 0L) 0f else usedBytes.toFloat() / totalBytes
}

data class StorageSnapshot(val totalBytes: Long, val freeBytes: Long) {
    val usedBytes get() = totalBytes - freeBytes
    val usedFraction get() = if (totalBytes == 0L) 0f else usedBytes.toFloat() / totalBytes
}

data class SystemDrainers(
    val brightnessPercent: Int?,
    val autoBrightness: Boolean?,
    val screenTimeoutSec: Int?,
    val refreshRate: Float,
    val locationOn: Boolean?,
    val bluetoothOn: Boolean?,
    val nfcOn: Boolean?,
    val darkMode: Boolean,
    val powerSave: Boolean,
    val aodOn: Boolean?,
)

data class DeviceIdentity(
    val model: String,
    val manufacturer: String,
    val android: String,
    val oneUi: String?,
    val soc: String?,
    val cores: Int,
    val uptimeMs: Long,
    val thermalStatus: Int,
    val thermalHeadroom: Float?,
)

object DeviceInfo {

    fun battery(ctx: Context): BatterySnapshot {
        val i = ctx.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val bm = ctx.getSystemService(BatteryManager::class.java)
        val level = i?.let {
            val l = it.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val s = it.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
            if (l >= 0 && s > 0) l * 100 / s else -1
        } ?: bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        val healthCode = i?.getIntExtra(BatteryManager.EXTRA_HEALTH, 0) ?: 0
        val statusCode = i?.getIntExtra(BatteryManager.EXTRA_STATUS, 0) ?: 0
        val pluggedCode = i?.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) ?: 0
        val charging = statusCode == BatteryManager.BATTERY_STATUS_CHARGING ||
            statusCode == BatteryManager.BATTERY_STATUS_FULL
        val rawCurrent = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)
        val currentMa = if (rawCurrent == Int.MIN_VALUE || rawCurrent == 0) null
        else if (kotlin.math.abs(rawCurrent) > 20_000) rawCurrent / 1000 else rawCurrent
        val counter = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER)
        val cycles = if (Build.VERSION.SDK_INT >= 34)
            i?.getIntExtra(BatteryManager.EXTRA_CYCLE_COUNT, -1)?.takeIf { it >= 0 } else null
        return BatterySnapshot(
            level = level,
            temperatureC = (i?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0) / 10f,
            voltageMv = i?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0) ?: 0,
            health = when (healthCode) {
                BatteryManager.BATTERY_HEALTH_GOOD -> "Buena"
                BatteryManager.BATTERY_HEALTH_OVERHEAT -> "Sobrecalentada"
                BatteryManager.BATTERY_HEALTH_DEAD -> "Agotada"
                BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "Sobrevoltaje"
                BatteryManager.BATTERY_HEALTH_COLD -> "Fría"
                BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE -> "Falla"
                else -> "Desconocida"
            },
            healthGood = healthCode == BatteryManager.BATTERY_HEALTH_GOOD,
            status = when (statusCode) {
                BatteryManager.BATTERY_STATUS_CHARGING -> "Cargando"
                BatteryManager.BATTERY_STATUS_DISCHARGING -> "Descargando"
                BatteryManager.BATTERY_STATUS_FULL -> "Completa"
                BatteryManager.BATTERY_STATUS_NOT_CHARGING -> "No carga"
                else -> "Desconocido"
            },
            charging = charging,
            plugged = when (pluggedCode) {
                BatteryManager.BATTERY_PLUGGED_AC -> "Cargador"
                BatteryManager.BATTERY_PLUGGED_USB -> "USB"
                BatteryManager.BATTERY_PLUGGED_WIRELESS -> "Inalámbrica"
                else -> "Desconectado"
            },
            technology = i?.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY) ?: "Li-ion",
            currentMa = currentMa,
            cycleCount = cycles,
            chargeCounterMah = if (counter > 0) counter / 1000 else null,
        )
    }

    fun memory(ctx: Context): MemorySnapshot {
        val am = ctx.getSystemService(ActivityManager::class.java)
        val mi = ActivityManager.MemoryInfo()
        am.getMemoryInfo(mi)
        return MemorySnapshot(mi.totalMem, mi.availMem, mi.lowMemory)
    }

    fun storage(ctx: Context): StorageSnapshot = try {
        val ssm = ctx.getSystemService(StorageStatsManager::class.java)
        StorageSnapshot(
            ssm.getTotalBytes(StorageManager.UUID_DEFAULT),
            ssm.getFreeBytes(StorageManager.UUID_DEFAULT),
        )
    } catch (e: Exception) {
        val f = ctx.filesDir
        StorageSnapshot(f.totalSpace, f.usableSpace)
    }

    fun identity(ctx: Context): DeviceIdentity {
        val pm = ctx.getSystemService(PowerManager::class.java)
        val headroom = if (Build.VERSION.SDK_INT >= 30) try {
            pm.getThermalHeadroom(10).takeIf { !it.isNaN() }
        } catch (_: Exception) { null } else null
        return DeviceIdentity(
            model = marketingName(),
            manufacturer = Build.MANUFACTURER.replaceFirstChar { it.uppercase() },
            android = Build.VERSION.RELEASE,
            oneUi = oneUiVersion(),
            soc = if (Build.VERSION.SDK_INT >= 31) "${Build.SOC_MANUFACTURER} ${Build.SOC_MODEL}" else Build.HARDWARE,
            cores = Runtime.getRuntime().availableProcessors(),
            uptimeMs = SystemClock.elapsedRealtime(),
            thermalStatus = pm.currentThermalStatus,
            thermalHeadroom = headroom,
        )
    }

    fun drainers(ctx: Context): SystemDrainers {
        val cr = ctx.contentResolver
        fun sysInt(name: String): Int? = try { Settings.System.getInt(cr, name) } catch (_: Exception) { null }
        val brightness = sysInt(Settings.System.SCREEN_BRIGHTNESS)
        val pm = ctx.getSystemService(PowerManager::class.java)
        val lm = ctx.getSystemService(LocationManager::class.java)
        val bt = try { ctx.getSystemService(BluetoothManager::class.java)?.adapter?.isEnabled } catch (_: Exception) { null }
        val nfc = try { NfcAdapter.getDefaultAdapter(ctx)?.isEnabled } catch (_: Exception) { null }
        @Suppress("DEPRECATION")
        val rate = try { ctx.getSystemService(WindowManager::class.java).defaultDisplay.refreshRate } catch (_: Exception) { 60f }
        val night = (ctx.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        return SystemDrainers(
            // Samsung usa una escala 1..255 en la mayoría de modelos
            brightnessPercent = brightness?.let { (it * 100 / 255).coerceIn(0, 100) },
            autoBrightness = sysInt(Settings.System.SCREEN_BRIGHTNESS_MODE)?.let { it == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC },
            screenTimeoutSec = sysInt(Settings.System.SCREEN_OFF_TIMEOUT)?.div(1000),
            refreshRate = rate,
            locationOn = try { lm.isLocationEnabled } catch (_: Exception) { null },
            bluetoothOn = bt,
            nfcOn = nfc,
            darkMode = night,
            powerSave = pm.isPowerSaveMode,
            aodOn = sysInt("aod_mode")?.let { it == 1 },
        )
    }

    /** Frecuencias actuales de CPU por núcleo en MHz (si el kernel las expone). */
    fun cpuFreqsMhz(): List<Int?> = (0 until Runtime.getRuntime().availableProcessors()).map { n ->
        try {
            File("/sys/devices/system/cpu/cpu$n/cpufreq/scaling_cur_freq").readText().trim().toInt() / 1000
        } catch (_: Exception) { null }
    }

    private fun sysProp(key: String): String? = try {
        val c = Class.forName("android.os.SystemProperties")
        (c.getMethod("get", String::class.java).invoke(null, key) as String).ifBlank { null }
    } catch (_: Exception) { null }

    fun oneUiVersion(): String? {
        sysProp("ro.build.version.oneui")?.toIntOrNull()?.let { v ->
            val major = v / 10000; val minor = (v % 10000) / 100
            return "$major.$minor"
        }
        return try {
            val sem = Build.VERSION::class.java.getField("SEM_PLATFORM_INT").getInt(null)
            val v = sem - 90000
            "${v / 10000}.${(v % 10000) / 100}"
        } catch (_: Exception) { null }
    }

    private fun marketingName(): String {
        val m = Build.MODEL
        val known = mapOf(
            "SM-S938" to "Galaxy S25 Ultra", "SM-S936" to "Galaxy S25+", "SM-S931" to "Galaxy S25",
            "SM-S937" to "Galaxy S25 Edge", "SM-S928" to "Galaxy S24 Ultra",
        )
        return known.entries.firstOrNull { m.startsWith(it.key) }?.let { "${it.value} ($m)" } ?: m
    }

    fun thermalLabel(status: Int): String = when (status) {
        PowerManager.THERMAL_STATUS_NONE -> "Normal"
        PowerManager.THERMAL_STATUS_LIGHT -> "Tibio"
        PowerManager.THERMAL_STATUS_MODERATE -> "Caliente"
        PowerManager.THERMAL_STATUS_SEVERE -> "Muy caliente"
        PowerManager.THERMAL_STATUS_CRITICAL -> "Crítico"
        PowerManager.THERMAL_STATUS_EMERGENCY, PowerManager.THERMAL_STATUS_SHUTDOWN -> "Emergencia"
        else -> "Desconocido"
    }
}
