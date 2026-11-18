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
import kotlinx.coroutines.withTimeout
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
    private var pending: CompletableDeferred<IDarkNetService>? = null
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
        Shizuku.addRequestPermissionResultListener { _, _ -> refresh() }
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

    /** The privileged service, binding it if needed. */
    suspend fun service(): IDarkNetService {
        service?.takeIf { it.asBinder().pingBinder() }?.let { return it }
        check(state.value == ShizukuState.READY) { "Shizuku is not ready" }
        val deferred = CompletableDeferred<IDarkNetService>()
        pending = deferred
        Shizuku.bindUserService(args, connection)
        val bound = withTimeout(15_000) { deferred.await() }
        if (runCatching { bound.version() }.getOrNull() != BuildConfig.VERSION_CODE) {
            // A service from an older install is still running: replace it.
            runCatching { Shizuku.unbindUserService(args, connection, true) }
            service = null
            val fresh = CompletableDeferred<IDarkNetService>()
            pending = fresh
            Shizuku.bindUserService(args, connection)
            return withTimeout(15_000) { fresh.await() }
        }
        return bound
    }
}
