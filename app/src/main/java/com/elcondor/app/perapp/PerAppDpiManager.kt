package com.elcondor.app.perapp

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import android.util.Log
import android.view.accessibility.AccessibilityManager
import com.elcondor.app.display.DisplayController
import com.elcondor.app.display.DisplayLimits
import com.elcondor.app.display.DisplayParsers
import com.elcondor.app.packages.PackageParsers
import com.elcondor.app.shell.ShizukuShellManager
import com.elcondor.app.shell.ShizukuState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

data class PerAppRule(val packageName: String, val label: String, val dpi: Int)

data class LaunchableApp(val packageName: String, val label: String)

data class PerAppUiState(
    val rules: List<PerAppRule> = emptyList(),
    /** El servicio de accesibilidad está conectado (detección activa). */
    val detectionActive: Boolean = false,
    /** App cuya regla está aplicada ahora mismo, si hay alguna. */
    val activePackage: String? = null,
    val isBusy: Boolean = false,
    val message: String? = null,
)

/**
 * "DPI por app" emulado: cambia el DPI global al entrar en una app con regla y
 * restaura el DPI base al salir. Recibe los cambios de app en primer plano desde
 * [ForegroundAppService].
 *
 * No hace falta temporizador de seguridad: si el DPI de una regla resulta
 * ilegible, basta con salir de la app (gesto de Inicio) para volver al DPI base.
 */
@OptIn(FlowPreview::class)
class PerAppDpiManager(
    context: Context,
    private val shell: ShizukuShellManager,
    private val display: DisplayController,
    private val appScope: CoroutineScope,
) {
    private val appContext = context.applicationContext
    private val pm = appContext.packageManager
    private val store = PerAppDpiStore(appContext)

    private val _state = MutableStateFlow(PerAppUiState())
    val state: StateFlow<PerAppUiState> = _state.asStateFlow()

    /** Último paquete en primer plano reportado por el servicio de accesibilidad. */
    private val foreground = MutableStateFlow<String?>(null)

    /** Serializa los cambios: nunca dos `wm density` cruzados. */
    private val switchMutex = Mutex()

    /** DPI aplicado por nosotros ahora mismo (evita repetir el mismo comando). */
    private var appliedDpi: Int? = null

    private val serviceComponent = ComponentName(appContext, ForegroundAppService::class.java)

    init {
        reloadRules()
        _state.update { it.copy(detectionActive = isServiceEnabledInSystem()) }

        appScope.launch {
            foreground
                .filterNotNull()
                // Al cambiar de app llegan ráfagas de eventos (splash, diálogos...):
                // esperamos a que se estabilice para no hacer `wm density` en bucle.
                .debounce(SWITCH_DEBOUNCE_MS)
                .collect { onForeground(it) }
        }

        // Si la app murió con un DPI de regla aplicado, lo deshacemos al volver.
        appScope.launch {
            shell.state.collect { s ->
                if (s is ShizukuState.Ready && store.ruleModeActive && !_state.value.detectionActive) {
                    restoreBaseline()
                }
            }
        }
    }

    // --- Llamadas desde ForegroundAppService ---------------------------------

    fun onForegroundActivity(packageName: String) {
        foreground.value = packageName
    }

    fun onServiceConnected() = _state.update { it.copy(detectionActive = true) }

    fun onServiceDisconnected() {
        _state.update { it.copy(detectionActive = false) }
        // Sin detección no sabremos cuándo salir de la app: volvemos ya al base.
        appScope.launch { restoreBaseline() }
    }

    // --- API para la UI -------------------------------------------------------

    fun addRule(app: LaunchableApp, dpi: Int) {
        DisplayLimits.validateDensity(dpi)?.let { msg -> return _state.update { it.copy(message = msg) } }
        if (!PackageParsers.isValidPackageName(app.packageName)) return
        store.putRule(app.packageName, dpi)
        reloadRules()
        _state.update { it.copy(message = "${app.label}: ${dpi} DPI") }
    }

    fun removeRule(packageName: String) {
        store.removeRule(packageName)
        reloadRules()
    }

    /** Activa/desactiva el servicio de accesibilidad escribiendo Settings.Secure vía Shizuku. */
    fun setDetectionEnabled(enable: Boolean) {
        if (_state.value.isBusy) return
        appScope.launch {
            _state.update { it.copy(isBusy = true) }
            val result = runCatching {
                val current = shell.executeCommand("settings get secure enabled_accessibility_services").getOrThrow()
                val entries = PerAppDpiLogic.editAccessibilityList(current, serviceComponent.flattenToString(), enable)
                    ?: error("La lista de servicios de accesibilidad tiene un formato inesperado; actívalo desde Ajustes > Accesibilidad")
                shell.executeCommand(PerAppDpiLogic.accessibilityListCommand(entries, enable)).getOrThrow()
            }
            if (!enable) restoreBaseline()
            _state.update {
                it.copy(
                    isBusy = false,
                    // Al activar, el sistema conecta el servicio de forma asíncrona y
                    // ForegroundAppService.onServiceConnected() pone el flag a true.
                    detectionActive = if (result.isSuccess && !enable) false else it.detectionActive,
                    message = result.exceptionOrNull()?.let { e -> "No se pudo cambiar: ${e.message}" }
                        ?: if (enable) "Detección de apps activada" else "Detección de apps desactivada",
                )
            }
        }
    }

    /** Apps con icono en el launcher, para el selector. */
    suspend fun loadLaunchableApps(): List<LaunchableApp> = withContext(Dispatchers.IO) {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        @Suppress("DEPRECATION")
        pm.queryIntentActivities(intent, 0)
            .map { it.activityInfo.packageName }
            .distinct()
            .filter { it != appContext.packageName }
            .map { LaunchableApp(it, labelOf(it)) }
            .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.label })
    }

    fun consumeMessage() = _state.update { it.copy(message = null) }

    // --- Motor ----------------------------------------------------------------

    private suspend fun onForeground(packageName: String) = switchMutex.withLock {
        // No pelear con la cuenta atrás de seguridad del DPI global.
        if (display.state.value.pending != null) return@withLock
        if (shell.state.value !is ShizukuState.Ready) return@withLock

        val decision = PerAppDpiLogic.decide(
            foregroundPackage = packageName,
            rules = store.rules(),
            ignored = setOfNotNull(currentImePackage()),
            ruleModeActive = store.ruleModeActive,
        )
        when (decision) {
            DpiDecision.Ignore -> Unit
            DpiDecision.RestoreBaseline -> restoreBaselineLocked()
            is DpiDecision.ApplyRule -> applyRuleLocked(packageName, decision.dpi)
        }
    }

    private suspend fun applyRuleLocked(packageName: String, dpi: Int) {
        if (!store.ruleModeActive) {
            // Primera app con regla: memorizamos el DPI actual como "base".
            val output = shell.executeCommand("wm density").getOrElse {
                Log.w(TAG, "No se pudo leer el DPI base", it)
                return
            }
            val reading = DisplayParsers.parseDensity(output) ?: return
            store.baseline = reading.override
            appliedDpi = reading.effective
        }
        if (appliedDpi != dpi) {
            shell.executeCommand("wm density $dpi")
                .onSuccess { appliedDpi = dpi }
                .onFailure { Log.w(TAG, "No se pudo aplicar $dpi a $packageName", it); return }
        }
        store.ruleModeActive = true
        _state.update { it.copy(activePackage = packageName) }
    }

    private suspend fun restoreBaseline() = switchMutex.withLock { restoreBaselineLocked() }

    private suspend fun restoreBaselineLocked() {
        if (!store.ruleModeActive) return
        if (shell.state.value !is ShizukuState.Ready) return
        shell.executeCommand(PerAppDpiLogic.baselineCommand(store.baseline))
            .onSuccess {
                store.ruleModeActive = false
                appliedDpi = null
                _state.update { it.copy(activePackage = null) }
                display.refresh()
            }
            .onFailure { Log.w(TAG, "No se pudo restaurar el DPI base", it) }
    }

    // --- Utilidades -----------------------------------------------------------

    private fun reloadRules() {
        val rules = store.rules().map { (pkg, dpi) -> PerAppRule(pkg, labelOf(pkg), dpi) }
            .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.label })
        _state.update { it.copy(rules = rules) }
    }

    private fun isServiceEnabledInSystem(): Boolean = runCatching {
        val am = appContext.getSystemService(AccessibilityManager::class.java)
        am.getEnabledAccessibilityServiceList(android.accessibilityservice.AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
            .any { it.resolveInfo.serviceInfo.let { s -> s.packageName == appContext.packageName && s.name == serviceComponent.className } }
    }.getOrDefault(false)

    /** El teclado se superpone a la app: abrirlo no debe contar como "cambio de app". */
    private fun currentImePackage(): String? = runCatching {
        Settings.Secure.getString(appContext.contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD)
            ?.substringBefore('/')
    }.getOrNull()

    private fun labelOf(packageName: String): String = try {
        pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString()
    } catch (_: PackageManager.NameNotFoundException) {
        packageName
    }

    private companion object {
        const val TAG = "PerAppDpiManager"
        const val SWITCH_DEBOUNCE_MS = 350L
    }
}
