package dev.darknet.ui

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.BatteryManager
import android.telephony.SignalStrength
import android.telephony.TelephonyCallback
import android.telephony.TelephonyDisplayInfo
import android.telephony.TelephonyManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext

/** The phone's own condition while it shares: mobile signal, battery and temperature. */
data class Vitals(
    val network: String = "–",
    val signalBars: Int = -1, // 0..4, -1 unknown
    val batteryPercent: Int = -1,
    val charging: Boolean = false,
    val temperatureC: Float = Float.NaN,
)

@Composable
fun rememberVitals(): Vitals {
    val context = LocalContext.current
    var vitals by remember { mutableStateOf(Vitals()) }

    DisposableEffect(Unit) {
        val battery = object : BroadcastReceiver() {
            override fun onReceive(c: Context, i: Intent) {
                val level = i.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale = i.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
                val status = i.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                vitals = vitals.copy(
                    batteryPercent = if (level >= 0) level * 100 / scale.coerceAtLeast(1) else -1,
                    charging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL,
                    temperatureC = i.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, Int.MIN_VALUE)
                        .takeIf { it != Int.MIN_VALUE }?.div(10f) ?: Float.NaN,
                )
            }
        }
        context.registerReceiver(battery, IntentFilter(Intent.ACTION_BATTERY_CHANGED), Context.RECEIVER_NOT_EXPORTED)

        val tm = context.getSystemService(TelephonyManager::class.java)
        val canReadPhone = context.checkSelfPermission(Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED
        val callback = if (tm != null && canReadPhone) {
            object : TelephonyCallback(), TelephonyCallback.SignalStrengthsListener, TelephonyCallback.DisplayInfoListener {
                override fun onSignalStrengthsChanged(s: SignalStrength) {
                    vitals = vitals.copy(signalBars = s.level)
                }

                override fun onDisplayInfoChanged(info: TelephonyDisplayInfo) {
                    vitals = vitals.copy(network = networkName(info))
                }
            }.also { runCatching { tm.registerTelephonyCallback(context.mainExecutor, it) } }
        } else {
            null
        }
        if (tm != null && !canReadPhone) {
            runCatching { vitals = vitals.copy(signalBars = tm.signalStrength?.level ?: -1) }
        }

        onDispose {
            runCatching { context.unregisterReceiver(battery) }
            if (callback != null) runCatching { tm?.unregisterTelephonyCallback(callback) }
        }
    }
    return vitals
}

private fun networkName(info: TelephonyDisplayInfo): String = when (info.overrideNetworkType) {
    TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_NR_ADVANCED -> "5G+"
    TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_NR_NSA -> "5G"
    TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_LTE_ADVANCED_PRO -> "4G+"
    TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_LTE_CA -> "4G+"
    else -> when (info.networkType) {
        TelephonyManager.NETWORK_TYPE_NR -> "5G"
        TelephonyManager.NETWORK_TYPE_LTE -> "4G"
        TelephonyManager.NETWORK_TYPE_UNKNOWN -> "–"
        else -> "3G"
    }
}
