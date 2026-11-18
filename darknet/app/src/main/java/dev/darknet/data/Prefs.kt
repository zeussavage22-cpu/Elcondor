package dev.darknet.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class Settings(
    /** Kernel fast path for IPv4 TCP (falls back by itself if the phone blocks it). */
    val fastEngine: Boolean = true,
    /** Also share over a USB cable. */
    val usb: Boolean = false,
    /** Monthly data plan in GB; 0 = off. */
    val quotaGb: Int = 0,
    /** Day of the month the plan renews (1–28). */
    val quotaResetDay: Int = 1,
    /** Stop sharing once the monthly plan is used up. */
    val quotaStop: Boolean = false,
    /** Stop sharing after this many minutes with nobody connected; 0 = off. */
    val idleMinutes: Int = 0,
    /** Notify when a device joins or leaves. */
    val deviceAlerts: Boolean = true,
)

private val Context.store by preferencesDataStore("darknet")

class Prefs(private val context: Context) {
    private object K {
        val fast = booleanPreferencesKey("fast_engine")
        val usb = booleanPreferencesKey("usb")
        val quota = intPreferencesKey("quota_gb")
        val reset = intPreferencesKey("quota_reset_day")
        val quotaStop = booleanPreferencesKey("quota_stop")
        val idle = intPreferencesKey("idle_minutes")
        val alerts = booleanPreferencesKey("device_alerts")
    }

    val settings: Flow<Settings> = context.store.data.map(::read)

    private fun read(p: Preferences) = Settings(
        fastEngine = p[K.fast] ?: true,
        usb = p[K.usb] ?: false,
        quotaGb = (p[K.quota] ?: 0).coerceAtLeast(0),
        quotaResetDay = (p[K.reset] ?: 1).coerceIn(1, 28),
        quotaStop = p[K.quotaStop] ?: false,
        idleMinutes = (p[K.idle] ?: 0).coerceAtLeast(0),
        deviceAlerts = p[K.alerts] ?: true,
    )

    suspend fun setFastEngine(value: Boolean) = context.store.edit { it[K.fast] = value }
    suspend fun setUsb(value: Boolean) = context.store.edit { it[K.usb] = value }
    suspend fun setQuota(gb: Int) = context.store.edit { it[K.quota] = gb.coerceAtLeast(0) }
    suspend fun setQuotaResetDay(day: Int) = context.store.edit { it[K.reset] = day.coerceIn(1, 28) }
    suspend fun setQuotaStop(value: Boolean) = context.store.edit { it[K.quotaStop] = value }
    suspend fun setIdleMinutes(minutes: Int) = context.store.edit { it[K.idle] = minutes.coerceAtLeast(0) }
    suspend fun setDeviceAlerts(value: Boolean) = context.store.edit { it[K.alerts] = value }
}
