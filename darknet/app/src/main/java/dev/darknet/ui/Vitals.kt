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
    /** Received power of the strongest cell, in dBm; null if unknown. */
    val signalDbm: Int? = null,
    val batteryPercent: Int = -1,
    val charging: Boolean = false,
    val temperatureC: Float = Float.NaN,
)

/** signalBars when DarkNet may not read the phone state. */
const val NO_PERMISSION = -2

@Composable
fun rememberVitals(): Vitals {
    val context = LocalContext.current
    var vitals by remember { mutableStateOf(Vitals()) }

    // Re-run when the app comes back, so a permission granted meanwhile takes effect.
    val lifecycle = androidx.lifecycle.compose.LocalLifecycleOwner.current.lifecycle
    var resumed by remember { mutableStateOf(0) }
    DisposableEffect(lifecycle) {
        val obs = androidx.lifecycle.LifecycleEventObserver { _, e ->
            if (e == androidx.lifecycle.Lifecycle.Event.ON_RESUME) resumed++
        }
        lifecycle.addObserver(obs)
        onDispose { lifecycle.removeObserver(obs) }
    }

    // While the permission is missing, check again every time the app comes back.
    DisposableEffect(if (vitals.signalBars == NO_PERMISSION) resumed else -1) {
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

        // Dual SIM or eSIM: follow the SIM that carries mobile data.
        val tm = context.getSystemService(TelephonyManager::class.java)?.let { base ->
            val dataSub = android.telephony.SubscriptionManager.getDefaultDataSubscriptionId()
            if (dataSub != android.telephony.SubscriptionManager.INVALID_SUBSCRIPTION_ID) base.createForSubscriptionId(dataSub) else base
        }
        val canReadPhone = context.checkSelfPermission(Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED
        fun applySignal(s: SignalStrength) {
            val lte = s.getCellSignalStrengths(android.telephony.CellSignalStrengthLte::class.java).firstOrNull()?.rsrp
            val nr = s.getCellSignalStrengths(android.telephony.CellSignalStrengthNr::class.java).firstOrNull()?.ssRsrp
            val measured = dev.darknet.data.SignalLevel.of(lte, nr)
            vitals = if (measured != null) {
                vitals.copy(signalBars = measured.first, signalDbm = measured.second)
            } else {
                vitals.copy(signalBars = maxOf(s.level, s.cellSignalStrengths.maxOfOrNull { it.level } ?: 0), signalDbm = null)
            }
        }
        if (tm != null && canReadPhone) {
            runCatching { tm.signalStrength?.let(::applySignal) }
        }
        val callback = if (tm != null && canReadPhone) {
            object : TelephonyCallback(), TelephonyCallback.SignalStrengthsListener, TelephonyCallback.DisplayInfoListener {
                override fun onSignalStrengthsChanged(s: SignalStrength) {
                    applySignal(s)
                }

                override fun onDisplayInfoChanged(info: TelephonyDisplayInfo) {
                    vitals = vitals.copy(network = networkName(info))
                }
            }.also { runCatching { tm.registerTelephonyCallback(context.mainExecutor, it) } }
        } else {
            null
        }
        if (tm != null && !canReadPhone) {
            vitals = vitals.copy(signalBars = NO_PERMISSION)
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
