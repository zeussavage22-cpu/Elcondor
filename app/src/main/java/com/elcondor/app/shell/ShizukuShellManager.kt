package com.elcondor.app.shell

import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.DeadObjectException
import android.os.IBinder
import android.os.RemoteException
import android.util.Log
import com.elcondor.app.BuildConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import rikka.shizuku.Shizuku

/** Estado observable de la conexión con Shizuku, de "peor" a "mejor". */
sealed interface ShizukuState {
    /** La app de Shizuku no está instalada (y tampoco hay Sui). */
    data object NotInstalled : ShizukuState

    /** Instalada, pero el servicio no está corriendo (p. ej. tras reiniciar). */
    data object NotRunning : ShizukuState

    /** Versión anterior a la 11: API incompatible. */
    data object Unsupported : ShizukuState

    /** Corriendo, pero aún no hemos pedido permiso (o el usuario lo denegó una vez). */
    data object PermissionRequired : ShizukuState

    /** El usuario eligió "Denegar siempre": hay que concederlo desde la app Shizuku. */
    data object PermissionDeniedPermanently : ShizukuState

    /** Todo listo. uid = 0 (root) o 2000 (shell/ADB). */
    data class Ready(val uid: Int, val version: Int) : ShizukuState {
        val isRoot: Boolean get() = uid == 0
    }
}

/** Errores tipados para que la UI pueda mostrar mensajes útiles. */
sealed class ShizukuException(message: String, cause: Throwable? = null) : Exception(message, cause) {
    class NotAvailable(val state: ShizukuState) :
        ShizukuException("Shizuku no está listo: ${state::class.simpleName}")

    class ServiceUnavailable(cause: Throwable? = null) :
        ShizukuException("No se pudo conectar con el servicio privilegiado", cause)

    class Disconnected(cause: Throwable? = null) :
        ShizukuException("Shizuku se desconectó durante la operación", cause)
}

/** El comando se ejecutó pero falló (exit ≠ 0 o timeout). */
class ShellCommandException(
    val command: String,
    val result: ShellResult,
) : Exception(
    if (result.timedOut) "Timeout ejecutando: $command"
    else "exit=${result.exitCode}: ${result.stderr.ifBlank { result.stdout }.trim()}",
)

/**
 * Punto único de acceso a comandos privilegiados vía Shizuku.
 *
 * Ciclo de vida: debe crearse UNA vez (en [android.app.Application.onCreate]) para
 * que los listeners de Shizuku se registren antes de que llegue el binder.
 *
 * Arquitectura:
 *   App ──(Shizuku binder)──▶ Shizuku ──lanza──▶ proceso remoto [ShellService]
 *   App ──(IShellService AIDL)──────────────────▶ ShellService.execute("wm density")
 */
class ShizukuShellManager(context: Context) {

    private val appContext = context.applicationContext

    private val _state = MutableStateFlow<ShizukuState>(ShizukuState.NotRunning)
    val state: StateFlow<ShizukuState> = _state.asStateFlow()

    /** Servicio remoto conectado, o null si no hay/está muerto. */
    private val service = MutableStateFlow<IShellService?>(null)
    private val bindMutex = Mutex()
    @Volatile private var bindRequested = false

    private val userServiceArgs = Shizuku.UserServiceArgs(
        ComponentName(appContext.packageName, ShellService::class.java.name),
    )
        .daemon(false) // Muere cuando se cierra la app: no queremos procesos shell huérfanos.
        .processNameSuffix("shell")
        .debuggable(BuildConfig.DEBUG)
        // Si cambia versionCode, Shizuku reemplaza el proceso remoto por el código nuevo.
        .version(BuildConfig.VERSION_CODE)

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            if (binder != null && binder.pingBinder()) {
                service.value = IShellService.Stub.asInterface(binder)
                Log.i(TAG, "ShellService conectado")
            } else {
                Log.w(TAG, "ShellService devolvió un binder inválido")
                service.value = null
                bindRequested = false
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            Log.w(TAG, "ShellService desconectado")
            service.value = null
            bindRequested = false
        }
    }

    // --- Listeners de Shizuku (se invocan en el hilo principal) ----------------

    private val binderReceived = Shizuku.OnBinderReceivedListener { refreshState() }

    private val binderDead = Shizuku.OnBinderDeadListener {
        Log.w(TAG, "Binder de Shizuku muerto (servicio detenido o app forzada a cerrar)")
        service.value = null
        bindRequested = false
        refreshState()
    }

    private val permissionResult =
        Shizuku.OnRequestPermissionResultListener { requestCode, _ ->
            if (requestCode == PERMISSION_REQUEST_CODE) refreshState()
        }

    init {
        // "Sticky": si el binder ya había llegado, el listener se dispara al instante.
        Shizuku.addBinderReceivedListenerSticky(binderReceived)
        Shizuku.addBinderDeadListener(binderDead)
        Shizuku.addRequestPermissionResultListener(permissionResult)
        refreshState()
    }

    // --- API pública -----------------------------------------------------------

    /** ¿Está la app de Shizuku instalada? (Sui no la necesita: ver [refreshState]). */
    fun isShizukuInstalled(): Boolean = try {
        appContext.packageManager.getPackageInfo(SHIZUKU_PACKAGE, 0)
        true
    } catch (_: PackageManager.NameNotFoundException) {
        false
    }

    /** Recalcula el estado. Seguro de llamar en cualquier momento (p. ej. en onResume). */
    fun refreshState() {
        _state.value = computeState()
    }

    /**
     * Pide el permiso de Shizuku. El resultado llega por listener y actualiza [state].
     * Devuelve false si no se puede pedir ahora (no está corriendo, ya concedido, etc.).
     */
    fun requestPermission(): Boolean {
        return when (computeState()) {
            ShizukuState.PermissionRequired -> try {
                Shizuku.requestPermission(PERMISSION_REQUEST_CODE)
                true
            } catch (t: Throwable) {
                Log.e(TAG, "requestPermission falló", t)
                refreshState()
                false
            }
            else -> false
        }
    }

    /**
     * Ejecuta [command] con privilegios de shell y devuelve su stdout recortado.
     *
     * - `Result.success(stdout)` si exit == 0.
     * - `Result.failure(ShellCommandException)` si el comando falla o hay timeout.
     * - `Result.failure(ShizukuException)` si Shizuku no está listo o se desconecta.
     *
     * Nunca lanza (salvo [CancellationException], que se propaga para respetar
     * la cancelación estructurada de corrutinas).
     */
    suspend fun executeCommand(
        command: String,
        timeoutMs: Long = DEFAULT_TIMEOUT_MS,
    ): Result<String> = executeRaw(command, timeoutMs).mapCatching { result ->
        if (!result.isSuccess) throw ShellCommandException(command, result)
        result.stdout.trim()
    }

    /** Variante que devuelve el [ShellResult] completo, aunque exit ≠ 0. */
    suspend fun executeRaw(
        command: String,
        timeoutMs: Long = DEFAULT_TIMEOUT_MS,
    ): Result<ShellResult> = withContext(Dispatchers.IO) {
        try {
            val remote = awaitService()
            if (BuildConfig.DEBUG) Log.d(TAG, "$ $command")
            Result.success(ShellResult.fromBundle(remote.execute(command, timeoutMs)))
        } catch (e: CancellationException) {
            throw e
        } catch (e: DeadObjectException) {
            // El proceso remoto murió a mitad de la llamada.
            onRemoteDied()
            Result.failure(ShizukuException.Disconnected(e))
        } catch (e: RemoteException) {
            onRemoteDied()
            Result.failure(ShizukuException.Disconnected(e))
        } catch (e: SecurityException) {
            // Permiso revocado desde la app Shizuku mientras estábamos conectados.
            refreshState()
            Result.failure(ShizukuException.NotAvailable(state.value))
        } catch (e: ShizukuException) {
            Result.failure(e)
        } catch (e: Exception) {
            // IllegalStateException del ShellService u otros errores inesperados.
            Result.failure(e)
        }
    }

    /** Libera el proceso remoto. Llamar solo si la app ya no necesita la shell. */
    fun release() {
        if (bindRequested && Shizuku.pingBinder()) {
            runCatching { Shizuku.unbindUserService(userServiceArgs, serviceConnection, true) }
        }
        service.value = null
        bindRequested = false
    }

    // --- Internos --------------------------------------------------------------

    private fun computeState(): ShizukuState {
        // Si hay binder, Shizuku (o Sui, vía Magisk) está corriendo: no hace falta la app.
        if (!Shizuku.pingBinder()) {
            return if (isShizukuInstalled()) ShizukuState.NotRunning else ShizukuState.NotInstalled
        }
        return try {
            when {
                Shizuku.isPreV11() -> ShizukuState.Unsupported
                Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED ->
                    ShizukuState.Ready(uid = Shizuku.getUid(), version = Shizuku.getVersion())
                // true = el usuario marcó "Denegar siempre".
                Shizuku.shouldShowRequestPermissionRationale() ->
                    ShizukuState.PermissionDeniedPermanently
                else -> ShizukuState.PermissionRequired
            }
        } catch (t: Throwable) {
            // El binder puede morir entre pingBinder() y la siguiente llamada.
            Log.w(TAG, "Error consultando Shizuku", t)
            ShizukuState.NotRunning
        }
    }

    /** Devuelve el servicio remoto, enlazándolo si hace falta. */
    private suspend fun awaitService(): IShellService {
        service.value?.takeIf { it.asBinder().pingBinder() }?.let { return it }

        refreshState()
        val current = state.value
        if (current !is ShizukuState.Ready) throw ShizukuException.NotAvailable(current)

        bindMutex.withLock {
            if (!bindRequested) {
                withContext(Dispatchers.Main) {
                    try {
                        Shizuku.bindUserService(userServiceArgs, serviceConnection)
                        bindRequested = true
                    } catch (t: Throwable) {
                        throw ShizukuException.ServiceUnavailable(t)
                    }
                }
            }
        }

        // La primera vez Shizuku tiene que arrancar app_process: puede tardar ~1-2 s.
        return withTimeoutOrNull(BIND_TIMEOUT_MS) { service.filterNotNull().first() }
            ?: run {
                bindRequested = false
                throw ShizukuException.ServiceUnavailable()
            }
    }

    private fun onRemoteDied() {
        service.value = null
        bindRequested = false
        refreshState()
    }

    companion object {
        private const val TAG = "ShizukuShellManager"
        const val SHIZUKU_PACKAGE = "moe.shizuku.privileged.api"
        private const val PERMISSION_REQUEST_CODE = 0x5A1
        private const val DEFAULT_TIMEOUT_MS = 15_000L
        private const val BIND_TIMEOUT_MS = 10_000L
    }
}
