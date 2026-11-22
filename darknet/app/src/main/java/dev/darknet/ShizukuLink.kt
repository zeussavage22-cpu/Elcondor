package dev.darknet

import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.IBinder
import dev.darknet.service.PrivilegedService
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import rikka.shizuku.Shizuku

/** Shizuku's state as the user needs to see it. */
enum class ShizukuState { NOT_INSTALLED, NOT_RUNNING, NO_PERMISSION, READY }

/**
 * Talks to Shizuku: whether it can be used, asking for its permission, and
 * binding DarkNet's privileged service inside it.
 */
object ShizukuLink {
    private const val PERMISSION_REQUEST = 7
    private const val SHIZUKU_PACKAGE = "moe.shizuku.privileged.api"

    private val mutableState = MutableStateFlow(ShizukuState.NOT_RUNNING)
    val state: StateFlow<ShizukuState> = mutableState.asStateFlow()

    @Volatile private var service: IDarkNetService? = null
    @Volatile private var pending: CompletableDeferred<IDarkNetService>? = null
    /** One bind at a time: two callers binding at once left one waiting forever. */
    private val bindLock = Mutex()
    private lateinit var appContext: Context

    private val args by lazy {
        Shizuku.UserServiceArgs(ComponentName(appContext.packageName, PrivilegedService::class.java.name))
            .daemon(false)
            .processNameSuffix("engine")
            .tag("darknet")
            .debuggable(BuildConfig.DEBUG)
            .version(BuildConfig.VERSION_CODE)
    }

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            val bound = binder?.takeIf { it.pingBinder() }?.let { IDarkNetService.Stub.asInterface(it) } ?: return
            service = bound
            pending?.complete(bound)
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            service = null
        }
    }

    fun init(context: Context) {
        appContext = context.applicationContext
        Shizuku.addBinderReceivedListenerSticky { refresh() }
        Shizuku.addBinderDeadListener {
            service = null
            refresh()
        }
        Shizuku.addRequestPermissionResultListener { _, result ->
            // Trust the answer itself; asking Shizuku right away can still say "no".
            if (result == PackageManager.PERMISSION_GRANTED && Shizuku.pingBinder()) {
                mutableState.value = ShizukuState.READY
            } else {
                refresh()
            }
        }
        refresh()
    }

    fun refresh() {
        mutableState.value = when {
            !Shizuku.pingBinder() -> if (isInstalled()) ShizukuState.NOT_RUNNING else ShizukuState.NOT_INSTALLED
            Shizuku.isPreV11() -> ShizukuState.NOT_RUNNING
            Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED -> ShizukuState.NO_PERMISSION
            else -> ShizukuState.READY
        }
    }

    fun requestPermission() {
        runCatching { Shizuku.requestPermission(PERMISSION_REQUEST) }
    }

    private fun isInstalled(): Boolean =
        runCatching { appContext.packageManager.getPackageInfo(SHIZUKU_PACKAGE, 0); true }.getOrDefault(false)

    private fun current(): IDarkNetService? = service?.takeIf { it.asBinder().pingBinder() }

    /** The privileged service, binding it if needed. */
    suspend fun service(): IDarkNetService {
        current()?.let { return it }
        return bindLock.withLock {
            current()?.let { return@withLock it }
            if (state.value != ShizukuState.READY) refresh()
            check(state.value == ShizukuState.READY) { "Shizuku no está listo: ábrelo, actívalo y dale permiso a DarkNet." }
            // If the first try hangs, start the service over once.
            var bound = bindOnce() ?: run { reset(); bindOnce() } ?: error(NOT_STARTED)
            if (runCatching { bound.version() }.getOrNull() != BuildConfig.VERSION_CODE) {
                // A service from before an update is still running: stop it and start the new one.
                runCatching { bound.destroy() }
                reset()
                bound = bindOnce() ?: error(NOT_STARTED)
            }
            bound
        }
    }

    private const val NOT_STARTED = "DarkNet no pudo arrancar su servicio en Shizuku. Abre Shizuku, revisa que esté activo y vuelve a intentar."

    private suspend fun bindOnce(): IDarkNetService? {
        val deferred = CompletableDeferred<IDarkNetService>()
        pending = deferred
        runCatching { Shizuku.bindUserService(args, connection) }.onFailure { return null }
        return withTimeoutOrNull(20_000) { deferred.await() }
    }

    private suspend fun reset() {
        runCatching { Shizuku.unbindUserService(args, connection, true) }
        service = null
        delay(1000) // let the old process go away
    }
}
