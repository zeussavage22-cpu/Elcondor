package com.elcondor.app.animation

import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.provider.Settings
import android.service.quicksettings.TileService
import android.util.Log
import com.elcondor.app.shell.ShizukuShellManager
import com.elcondor.app.tile.AnimationSpeedTileService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AnimationUiState(
    val scales: AnimationScales = AnimationScales(),
    val immersive: ImmersiveMode = ImmersiveMode.OFF,
    val isBusy: Boolean = false,
    val message: String? = null,
) {
    /** policy_control solo tiene efecto hasta Android 10 (salvo algunas ROMs). */
    val immersiveSupported: Boolean get() = Build.VERSION.SDK_INT <= ImmersiveMode.LAST_SUPPORTED_SDK
}

/**
 * Escalas de animación y modo inmersivo.
 *
 * - LECTURA de escalas: vía Settings.Global (claves públicas, sin privilegios),
 *   así el mosaico puede pintar su estado aunque Shizuku esté apagado.
 * - ESCRITURA: `settings put global ...` vía Shizuku (requiere WRITE_SECURE_SETTINGS,
 *   que tiene la shell pero no una app normal).
 */
class AnimationController(
    context: Context,
    private val shell: ShizukuShellManager,
    private val appScope: CoroutineScope,
) {
    private val appContext = context.applicationContext

    private val _state = MutableStateFlow(AnimationUiState(scales = readScales()))
    val state: StateFlow<AnimationUiState> = _state.asStateFlow()

    /** Lectura local e instantánea (no usa Shizuku). */
    fun readScales(): AnimationScales {
        val cr = appContext.contentResolver
        fun read(key: AnimationKey) = Settings.Global.getFloat(cr, key.setting, AnimationScales.DEFAULT)
        return AnimationScales(
            window = read(AnimationKey.WINDOW),
            transition = read(AnimationKey.TRANSITION),
            animator = read(AnimationKey.ANIMATOR),
        )
    }

    fun refresh() {
        appScope.launch {
            val immersive = shell.executeCommand("settings get global policy_control")
                .map(ImmersiveMode::parse)
                .getOrDefault(_state.value.immersive)
            _state.update { it.copy(scales = readScales(), immersive = immersive) }
        }
    }

    fun setAll(value: Float) = runChange("Animaciones a ${AnimationScale.format(value)}x") {
        AnimationScale.commandForAll(value)
    }

    fun set(key: AnimationKey, value: Float) =
        runChange("${key.label}: ${AnimationScale.format(value)}x") { AnimationScale.commandFor(key, value) }

    fun setImmersive(mode: ImmersiveMode) = runChange("Modo inmersivo: ${mode.label}") { mode.command }

    /**
     * Alterna rápido (0.25x) ↔ normal (1x). Lo usa el mosaico de Ajustes rápidos.
     * Devuelve true si se aplicó.
     */
    suspend fun toggleFast(): Boolean {
        val target = if (readScales().isFast) AnimationScales.DEFAULT else AnimationScale.FAST
        val result = shell.executeCommand(AnimationScale.commandForAll(target))
        result.onFailure { Log.w(TAG, "toggleFast falló", it) }
        afterChange()
        return result.isSuccess
    }

    fun consumeMessage() = _state.update { it.copy(message = null) }

    // --- Internos --------------------------------------------------------------

    private fun runChange(successMessage: String, command: () -> String) {
        if (_state.value.isBusy) return
        val cmd = try {
            command()
        } catch (e: IllegalArgumentException) {
            _state.update { it.copy(message = e.message) }
            return
        }
        appScope.launch {
            _state.update { it.copy(isBusy = true) }
            val result = shell.executeCommand(cmd)
            _state.update {
                it.copy(
                    isBusy = false,
                    message = if (result.isSuccess) successMessage
                    else "No se pudo aplicar: ${result.exceptionOrNull()?.message}",
                )
            }
            afterChange()
            refresh()
        }
    }

    private fun afterChange() {
        _state.update { it.copy(scales = readScales()) }
        // Pide al sistema que vuelva a pintar el mosaico con el nuevo estado.
        runCatching {
            TileService.requestListeningState(
                appContext,
                ComponentName(appContext, AnimationSpeedTileService::class.java),
            )
        }
    }

    private companion object {
        const val TAG = "AnimationController"
    }
}
