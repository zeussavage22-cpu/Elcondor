package dev.darknet.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
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
    /** Accent color name (see ui.Accent). */
    val accent: String = "VIOLET",
    /** "dark", "light" or "system". */
    val theme: String = "dark",
    /** The first-run guide was finished. */
    val onboarded: Boolean = false,
    /** DNS the hotspot's devices use: "normal", "noads" or "family". */
    val dns: String = "normal",
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
        val accent = stringPreferencesKey("accent")
        val theme = stringPreferencesKey("theme")
        val onboarded = booleanPreferencesKey("onboarded")
        val dns = stringPreferencesKey("dns")
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
        accent = p[K.accent] ?: "VIOLET",
        theme = p[K.theme] ?: "dark",
        onboarded = p[K.onboarded] ?: false,
        dns = p[K.dns] ?: "normal",
    )

    suspend fun setFastEngine(value: Boolean) = context.store.edit { it[K.fast] = value }
    suspend fun setUsb(value: Boolean) = context.store.edit { it[K.usb] = value }
    suspend fun setQuota(gb: Int) = context.store.edit { it[K.quota] = gb.coerceAtLeast(0) }
    suspend fun setQuotaResetDay(day: Int) = context.store.edit { it[K.reset] = day.coerceIn(1, 28) }
    suspend fun setQuotaStop(value: Boolean) = context.store.edit { it[K.quotaStop] = value }
    suspend fun setIdleMinutes(minutes: Int) = context.store.edit { it[K.idle] = minutes.coerceAtLeast(0) }
    suspend fun setDeviceAlerts(value: Boolean) = context.store.edit { it[K.alerts] = value }
    suspend fun setAccent(value: String) = context.store.edit { it[K.accent] = value }
    suspend fun setTheme(value: String) = context.store.edit { it[K.theme] = value }
    suspend fun setOnboarded(value: Boolean) = context.store.edit { it[K.onboarded] = value }
    suspend fun setDns(value: String) = context.store.edit { it[K.dns] = value }
}

/** DNS servers for each choice, IPv4 and IPv6. */
object DnsChoice {
    val servers = mapOf(
        "normal" to listOf("1.1.1.1", "8.8.8.8", "2606:4700:4700::1111", "2001:4860:4860::8888"),
        // AdGuard DNS: blocks ads and trackers.
        "noads" to listOf("94.140.14.14", "94.140.15.15", "2a10:50c0::ad1:ff", "2a10:50c0::ad2:ff"),
        // AdGuard DNS Family: also blocks adult content and enforces safe search.
        "family" to listOf("94.140.14.15", "94.140.15.16", "2a10:50c0::bad1:ff", "2a10:50c0::bad2:ff"),
    )
    val labels = mapOf("normal" to "Normal", "noads" to "Sin anuncios", "family" to "Familiar")
}
