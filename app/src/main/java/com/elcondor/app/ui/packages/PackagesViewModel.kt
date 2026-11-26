package com.elcondor.app.ui.packages

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.elcondor.app.packages.PackageRepository
import com.elcondor.app.packages.SystemPackage
import com.elcondor.app.packages.TrashItem
import com.elcondor.app.shell.ShizukuShellManager
import com.elcondor.app.shell.ShizukuState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PackagesUiState(
    val installed: List<SystemPackage> = emptyList(),
    val trash: List<TrashItem> = emptyList(),
    val isLoading: Boolean = false,
    /** true tras la primera carga correcta (para no mostrar "vacío" mientras carga). */
    val hasLoaded: Boolean = false,
    val query: String = "",
    /** Paquetes con una operación en curso (se deshabilita su botón). */
    val inFlight: Set<String> = emptySet(),
    val message: String? = null,
)

/**
 * Si el usuario sale a mitad de una desinstalación, la corrutina se cancela pero
 * el comando ya enviado termina igual en el proceso remoto. No se pierde nada:
 * la siguiente carga detecta el paquete desinstalado vía `pm list packages -u`.
 */
class PackagesViewModel(
    private val repository: PackageRepository,
    shell: ShizukuShellManager,
) : ViewModel() {

    private val _state = MutableStateFlow(PackagesUiState())
    val state: StateFlow<PackagesUiState> = _state.asStateFlow()

    init {
        // Carga automática cada vez que Shizuku pasa a estar listo.
        viewModelScope.launch {
            shell.state
                .map { it is ShizukuState.Ready }
                .distinctUntilChanged()
                .filter { it }
                .collect { refresh() }
        }
    }

    fun refresh() {
        if (_state.value.isLoading) return
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            repository.loadSnapshot()
                .onSuccess { snap ->
                    _state.update {
                        it.copy(installed = snap.installed, trash = snap.trash, hasLoaded = true)
                    }
                }
                .onFailure { e -> postMessage("No se pudo listar paquetes: ${e.message}") }
            _state.update { it.copy(isLoading = false) }
        }
    }

    fun setQuery(query: String) = _state.update { it.copy(query = query) }

    fun uninstall(pkg: SystemPackage) = runOperation(pkg.packageName) {
        repository.uninstall(pkg.packageName)
            .onSuccess { postMessage("${pkg.label} desinstalado · disponible en Papelera") }
            .onFailure { postMessage("No se pudo desinstalar ${pkg.label}: ${it.message}") }
    }

    fun restore(item: TrashItem) = runOperation(item.packageName) {
        repository.restore(item.packageName)
            .onSuccess { postMessage("${item.label} restaurado") }
            .onFailure { postMessage("No se pudo restaurar ${item.label}: ${it.message}") }
    }

    fun consumeMessage() = _state.update { it.copy(message = null) }

    private fun runOperation(packageName: String, block: suspend () -> Unit) {
        if (packageName in _state.value.inFlight) return
        viewModelScope.launch {
            _state.update { it.copy(inFlight = it.inFlight + packageName) }
            try {
                block()
            } finally {
                _state.update { it.copy(inFlight = it.inFlight - packageName) }
            }
            refresh() // Recalcula ambas listas desde la fuente de verdad (pm).
        }
    }

    private fun postMessage(msg: String) = _state.update { it.copy(message = msg) }
}
