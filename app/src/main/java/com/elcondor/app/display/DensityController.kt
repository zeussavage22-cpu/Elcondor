package com.elcondor.app.display

import com.elcondor.app.shell.ShizukuShellManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class DensityInfo(
    val physical: Int,
    /** null si no hay override (se usa el valor físico). */
    val override: Int?,
) {
    val effective: Int get() = override ?: physical
}

data class DensityUiState(
    val info: DensityInfo? = null,
    val isBusy: Boolean = false,
    /** Segundos restantes del temporizador de seguridad, o null si no hay cambio pendiente. */
    val pendingConfirmationSeconds: Int? = null,
    val pendingDpi: Int? = null,
    val message: String? = null,
)

/**
 * Lógica de DPI con temporizador de seguridad.
 *
 * Vive en un scope de APLICACIÓN (no en un ViewModel) a propósito: cambiar el DPI
 * recrea la Activity, y si el usuario sale de la pantalla durante la cuenta atrás
 * el ViewModel se destruiría y cancelaría la reversión → el DPI malo quedaría fijo.
 *
 * Limitación conocida: si el sistema mata el proceso durante los 10 s, la
 * reversión no ocurre. Para blindarlo del todo, mover la cuenta atrás a un
 * Foreground Service (siguiente iteración).
 */
class DensityController(
    private val shell: ShizukuShellManager,
    private val appScope: CoroutineScope,
) {
    private val _state = MutableStateFlow(DensityUiState())
    val state: StateFlow<DensityUiState> = _state.asStateFlow()

    private var countdownJob: Job? = null

    /** Valor a restaurar si el usuario no confirma (null = `wm density reset`). */
    private var rollbackOverride: Int? = null

    fun refresh() {
        appScope.launch { load() }
    }

    /** Aplica [dpi] y arranca la cuenta atrás de 10 s. */
    fun applyDensity(dpi: Int) {
        if (_state.value.isBusy || _state.value.pendingConfirmationSeconds != null) return
        // Validación estricta: el valor se interpola en un comando de shell,
        // así que SOLO aceptamos un Int dentro de un rango razonable.
        if (dpi !in MIN_DPI..MAX_DPI) {
            postMessage("El DPI debe estar entre $MIN_DPI y $MAX_DPI")
            return
        }

        appScope.launch {
            setBusy(true)
            val before = load() ?: run { setBusy(false); return@launch }
            if (dpi == before.effective) {
                setBusy(false)
                postMessage("Ese ya es el DPI actual")
                return@launch
            }

            shell.executeCommand("wm density $dpi")
                .onSuccess {
                    rollbackOverride = before.override
                    load()
                    startCountdown(dpi)
                }
                .onFailure { postMessage("No se pudo aplicar: ${it.message}") }
            setBusy(false)
        }
    }

    /** El usuario ve bien la pantalla: se cancela la reversión. */
    fun confirm() {
        countdownJob?.cancel()
        countdownJob = null
        rollbackOverride = null
        _state.update { it.copy(pendingConfirmationSeconds = null, pendingDpi = null, message = "DPI guardado") }
    }

    /** Revertir manualmente antes de que acabe la cuenta atrás. */
    fun revertNow() {
        countdownJob?.cancel()
        countdownJob = null
        appScope.launch { rollback() }
    }

    /** `wm density reset`: vuelve al valor de fábrica del panel. */
    fun resetToFactory() {
        if (_state.value.pendingConfirmationSeconds != null) return
        appScope.launch {
            setBusy(true)
            shell.executeCommand("wm density reset")
                .onSuccess { postMessage("DPI restaurado a fábrica") }
                .onFailure { postMessage("Error al restaurar: ${it.message}") }
            load()
            setBusy(false)
        }
    }

    fun consumeMessage() = _state.update { it.copy(message = null) }

    // --- Internos --------------------------------------------------------------

    private fun startCountdown(dpi: Int) {
        countdownJob?.cancel()
        countdownJob = appScope.launch {
            for (remaining in CONFIRM_TIMEOUT_SECONDS downTo 1) {
                _state.update { it.copy(pendingConfirmationSeconds = remaining, pendingDpi = dpi) }
                delay(1_000)
            }
            rollback()
        }
    }

    private suspend fun rollback() = withContext(NonCancellable) {
        // NonCancellable: una vez decidido revertir, nada debe interrumpirlo.
        val target = rollbackOverride
        val command = if (target == null) "wm density reset" else "wm density $target"
        val result = shell.executeCommand(command)
        rollbackOverride = null
        _state.update {
            it.copy(
                pendingConfirmationSeconds = null,
                pendingDpi = null,
                message = if (result.isSuccess) "Revertido al DPI anterior"
                else "¡No se pudo revertir! Usa \"Restaurar fábrica\" o `adb shell wm density reset`",
            )
        }
        load()
    }

    /** Lee `wm density` y actualiza el estado. Devuelve null si falla. */
    private suspend fun load(): DensityInfo? {
        val output = shell.executeCommand("wm density").getOrElse {
            postMessage("No se pudo leer el DPI: ${it.message}")
            return null
        }
        val info = parseDensity(output) ?: run {
            postMessage("Salida inesperada de wm density: $output")
            return null
        }
        _state.update { it.copy(info = info) }
        return info
    }

    private fun setBusy(busy: Boolean) = _state.update { it.copy(isBusy = busy) }
    private fun postMessage(msg: String) = _state.update { it.copy(message = msg) }

    companion object {
        const val MIN_DPI = 120
        const val MAX_DPI = 800
        const val CONFIRM_TIMEOUT_SECONDS = 10

        private val PHYSICAL = Regex("""Physical density:\s*(\d+)""")
        private val OVERRIDE = Regex("""Override density:\s*(\d+)""")

        /**
         * Formato típico:
         *   Physical density: 420
         *   Override density: 380   ← solo si hay override
         */
        fun parseDensity(output: String): DensityInfo? {
            val physical = PHYSICAL.find(output)?.groupValues?.get(1)?.toIntOrNull() ?: return null
            val override = OVERRIDE.find(output)?.groupValues?.get(1)?.toIntOrNull()
            return DensityInfo(physical, override)
        }
    }
}
