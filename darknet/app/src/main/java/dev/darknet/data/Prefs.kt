package dev.darknet.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class Settings(
    /** Kernel fast path for IPv4 TCP (falls back by itself if the phone blocks it). */
    val fastEngine: Boolean = true,
    /** Also share over a USB cable. */
    val usb: Boolean = false,
)

private val Context.store by preferencesDataStore("darknet")

class Prefs(private val context: Context) {
    private val fast = booleanPreferencesKey("fast_engine")
    private val usb = booleanPreferencesKey("usb")

    val settings: Flow<Settings> = context.store.data.map {
        Settings(fastEngine = it[fast] ?: true, usb = it[usb] ?: false)
    }

    suspend fun setFastEngine(value: Boolean) = context.store.edit { it[fast] = value }
    suspend fun setUsb(value: Boolean) = context.store.edit { it[usb] = value }
}
