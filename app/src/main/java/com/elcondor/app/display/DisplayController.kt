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

/** Un cambio de pantalla que requiere confirmación del usuario. */
sealed interface DisplayChange {
    val description: String
    val command: String

    data class Density(val dpi: Int) : DisplayChange {
        override val description get() = "DPI $dpi"
        override val command get() = "wm density $dpi"
    }

    data class Size(val resolution: Resolution) : DisplayChange {
        override val description get() = "Resolución $resolution"
        override val command get() = "wm size $resolution"
    }
}

data class PendingChange(val change: DisplayChange, val secondsLeft: Int)

data class DisplayUiState(
    val info: DisplayInfo? = null,
    val isBusy: Boolean = false,
    /** Cambio aplicado pero sin confirmar; null si no hay cuenta atrás activa. */
    val pending: PendingChange? = null,
    val message: String? = null,
) {
    /** Mientras hay operación o cuenta atrás no se aceptan cambios nuevos. */
    val isLocked: Boolean get() = isBusy || pending != null
}

/**
 * DPI y resolución con temporizador de seguridad compartido.
 *
 * Vive en un scope de APLICACIÓN (no en un ViewModel) a propósito: estos cambios
 * recrean la Activity, y si el usuario sale de la pantalla durante la cuenta
 * atrás un ViewModel se destruiría y cancelaría la reversión.
 *
 * Limitación conocida: si el sistema mata el proceso durante los 10 s, la
 * reversión no ocurre. Solución futura: mover la cuenta atrás a un Foreground Service.
 */
class DisplayController(
    private val shell: ShizukuShellManager,
    private val appScope: CoroutineScope,
) {
    private val _state = MutableStateFlow(DisplayUiState())
    val state: StateFlow<DisplayUiState> = _state.asStateFlow()

    private var countdownJob: Job? = null

    /** Comando que deshace el cambio pendiente. */
    private var rollbackCommand: String? = null

    fun refresh() {
        appScope.launch { load() }
    }

    fun applyDensity(dpi: Int) {
        // Validación estricta: el valor se interpola en un comando de shell.
        DisplayLimits.validateDensity(dpi)?.let { return postMessage(it) }
        submit(
            change = DisplayChange.Density(dpi),
            precheck = { if (it.density.effective == dpi) "Ese ya es el DPI actual" else null },
            rollbackFor = { before ->
                before.density.override?.let { "wm density $it" } ?: "wm density reset"
            },
        )
    }

    fun applySize(target: Resolution) {
        submit(
            change = DisplayChange.Size(target),
            precheck = { info ->
                DisplayLimits.validateSize(target, info.size.physical)
                    ?: if (info.size.effective == target) "Esa ya es la resolución actual" else null
            },
            rollbackFor = { before ->
                before.size.override?.let { "wm size $it" } ?: "wm size reset"
            },
        )
    }

    /** Volver a fábrica es siempre seguro: no necesita temporizador. */
    fun resetDensity() = runReset("wm density reset", "DPI restaurado a fábrica")

    fun resetSize() = runReset("wm size reset", "Resolución restaurada a fábrica")

    /** El usuario ve bien la pantalla: se cancela la reversión. */
    fun confirm() {
        val pending = _state.value.pending ?: return
        countdownJob?.cancel()
        countdownJob = null
        rollbackCommand = null
        _state.update { it.copy(pending = null, message = "${pending.change.description} guardado") }
    }

    /** Revertir manualmente antes de que acabe la cuenta atrás. */
    fun revertNow() {
        if (_state.value.pending == null) return
        countdownJob?.cancel()
        countdownJob = null
        appScope.launch { rollback() }
    }

    fun consumeMessage() = _state.update { it.copy(message = null) }

    // --- Internos --------------------------------------------------------------

    private fun submit(
        change: DisplayChange,
        precheck: (DisplayInfo) -> String?,
        rollbackFor: (DisplayInfo) -> String,
    ) {
        if (_state.value.isLocked) return
        appScope.launch {
            setBusy(true)
            try {
                // Leemos el estado real justo antes: es la base de la reversión.
                val before = load() ?: return@launch
                precheck(before)?.let { postMessage(it); return@launch }

                shell.executeCommand(change.command)
                    .onSuccess {
                        rollbackCommand = rollbackFor(before)
                        load()
                        startCountdown(change)
                    }
                    .onFailure { postMessage("No se pudo aplicar: ${it.message}") }
            } finally {
                setBusy(false)
            }
        }
    }

    private fun runReset(command: String, successMessage: String) {
        if (_state.value.isLocked) return
        appScope.launch {
            setBusy(true)
            try {
                shell.executeCommand(command)
                    .onSuccess { postMessage(successMessage) }
                    .onFailure { postMessage("Error al restaurar: ${it.message}") }
                load()
            } finally {
                setBusy(false)
            }
        }
    }

    private fun startCountdown(change: DisplayChange) {
        countdownJob?.cancel()
        countdownJob = appScope.launch {
            for (remaining in CONFIRM_TIMEOUT_SECONDS downTo 1) {
                _state.update { it.copy(pending = PendingChange(change, remaining)) }
                delay(1_000)
            }
            rollback()
        }
    }

    private suspend fun rollback() = withContext(NonCancellable) {
        // NonCancellable: una vez decidido revertir, nada debe interrumpirlo.
        val command = rollbackCommand ?: return@withContext
        val result = shell.executeCommand(command)
        rollbackCommand = null
        _state.update {
            it.copy(
                pending = null,
                message = if (result.isSuccess) "Revertido al valor anterior"
                else "¡No se pudo revertir! Usa \"Restaurar fábrica\" o `adb shell $command`",
            )
        }
        load()
    }

    /** Lee `wm density` y `wm size`. Devuelve null (y avisa) si algo falla. */
    private suspend fun load(): DisplayInfo? {
        val densityOut = shell.executeCommand("wm density").getOrElse {
            postMessage("No se pudo leer el DPI: ${it.message}")
            return null
        }
        val sizeOut = shell.executeCommand("wm size").getOrElse {
            postMessage("No se pudo leer la resolución: ${it.message}")
            return null
        }
        val density = DisplayParsers.parseDensity(densityOut)
        val size = DisplayParsers.parseSize(sizeOut)
        if (density == null || size == null) {
            postMessage("Salida inesperada de wm: $densityOut / $sizeOut")
            return null
        }
        return DisplayInfo(density, size).also { info -> _state.update { it.copy(info = info) } }
    }

    private fun setBusy(busy: Boolean) = _state.update { it.copy(isBusy = busy) }
    private fun postMessage(msg: String) = _state.update { it.copy(message = msg) }

    companion object {
        const val CONFIRM_TIMEOUT_SECONDS = 10
    }
}
