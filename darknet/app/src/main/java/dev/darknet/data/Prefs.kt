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
    /** DNS the hotspot's devices use: a key of [DnsChoice.servers] or "auto". */
    val dns: String = "normal",
    /** What "auto" picked last time, e.g. "Cloudflare · 14 ms". */
    val dnsAutoResult: String = "",
    /** Hotspot band: "auto" (leave the phone's), "2", "5" or "6" GHz. */
    val band: String = "auto",
    /** Warn when the connection drops or gets slow. */
    val stabilityAlerts: Boolean = true,
    /** Ask for fingerprint, face or PIN to open DarkNet. */
    val lock: Boolean = false,
    /** BBR congestion control on the engine's TCP sockets, when the phone allows it. */
    val bbr: Boolean = true,
    /** "auto" (the carrier's) or a number. */
    val mtu: String = "auto",
    /** Spread connections over Wi-Fi and mobile data at once. */
    val combine: Boolean = false,
    /** Warn at this battery temperature in °C; 0 = off. */
    val heatLimit: Int = 43,
    /** Also stop sharing at the limit. */
    val heatStop: Boolean = false,
    /** Show the live speed as the status-bar icon. */
    val speedIcon: Boolean = true,
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
        val dnsAuto = stringPreferencesKey("dns_auto_result")
        val band = stringPreferencesKey("band")
        val stability = booleanPreferencesKey("stability_alerts")
        val lock = booleanPreferencesKey("lock")
        val bbr = booleanPreferencesKey("bbr")
        val mtu = stringPreferencesKey("mtu")
        val combine = booleanPreferencesKey("combine")
        val heatLimit = intPreferencesKey("heat_limit")
        val heatStop = booleanPreferencesKey("heat_stop")
        val speedIcon = booleanPreferencesKey("speed_icon")
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
        dns = p[K.dns]?.takeIf { it in DnsChoice.labels } ?: "normal",
        dnsAutoResult = p[K.dnsAuto] ?: "",
        band = p[K.band] ?: "auto",
        stabilityAlerts = p[K.stability] ?: true,
        lock = p[K.lock] ?: false,
        bbr = p[K.bbr] ?: true,
        mtu = p[K.mtu] ?: "auto",
        combine = p[K.combine] ?: false,
        heatLimit = p[K.heatLimit] ?: 43,
        heatStop = p[K.heatStop] ?: false,
        speedIcon = p[K.speedIcon] ?: true,
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
    suspend fun setDnsAutoResult(value: String) = context.store.edit { it[K.dnsAuto] = value }
    suspend fun setBand(value: String) = context.store.edit { it[K.band] = value }
    suspend fun setStabilityAlerts(value: Boolean) = context.store.edit { it[K.stability] = value }
    suspend fun setLock(value: Boolean) = context.store.edit { it[K.lock] = value }
    suspend fun setBbr(value: Boolean) = context.store.edit { it[K.bbr] = value }
    suspend fun setMtu(value: String) = context.store.edit { it[K.mtu] = value }
    suspend fun setCombine(value: Boolean) = context.store.edit { it[K.combine] = value }
    suspend fun setHeatLimit(value: Int) = context.store.edit { it[K.heatLimit] = value }
    suspend fun setHeatStop(value: Boolean) = context.store.edit { it[K.heatStop] = value }
    suspend fun setSpeedIcon(value: Boolean) = context.store.edit { it[K.speedIcon] = value }
}

/** DNS servers for each choice, IPv4 and IPv6. */
object DnsChoice {
    val servers = mapOf(
        "normal" to listOf("1.1.1.1", "8.8.8.8", "2606:4700:4700::1111", "2001:4860:4860::8888"),
        // Cloudflare 1.1.1.1.
        "cloudflare" to listOf("1.1.1.1", "1.0.0.1", "2606:4700:4700::1111", "2606:4700:4700::1001"),
        // Cloudflare for Families: also blocks malware.
        "secure" to listOf("1.1.1.2", "1.0.0.2", "2606:4700:4700::1112", "2606:4700:4700::1002"),
        "google" to listOf("8.8.8.8", "8.8.4.4", "2001:4860:4860::8888", "2001:4860:4860::8844"),
        "quad9" to listOf("9.9.9.9", "149.112.112.112", "2620:fe::fe", "2620:fe::9"),
        // AdGuard DNS: blocks ads and trackers.
        "noads" to listOf("94.140.14.14", "94.140.15.15", "2a10:50c0::ad1:ff", "2a10:50c0::ad2:ff"),
        // AdGuard DNS Family: also blocks adult content and enforces safe search.
        "family" to listOf("94.140.14.15", "94.140.15.16", "2a10:50c0::bad1:ff", "2a10:50c0::bad2:ff"),
    )

    /** In the order the settings menu shows them. */
    val labels = linkedMapOf(
        "auto" to "Automático (el más rápido)",
        "cloudflare" to "Cloudflare 1.1.1.1",
        "secure" to "Cloudflare sin virus (1.1.1.2)",
        "google" to "Google",
        "quad9" to "Quad9",
        "noads" to "Sin anuncios",
        "family" to "Familiar",
        "normal" to "Normal",
    )

    /** What the automatic choice compares. */
    val autoCandidates = listOf("cloudflare" to "Cloudflare", "google" to "Google", "quad9" to "Quad9")
}
