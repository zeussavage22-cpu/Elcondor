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
            if (result == PackageManager.PERMISSION_GRANTED) {
                // Trust the answer itself: Shizuku can keep telling this process "no"
                // for a while, which is why the app used to need a restart.
                grantedHere = true
                mutableState.value = ShizukuState.READY
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(::restartIfStillRefused, 1200)
            } else {
                refresh()
            }
        }
        refresh()
    }

    /** The person allowed DarkNet in Shizuku's dialog during this run of the app. */
    @Volatile private var grantedHere = false

    private fun shizukuSaysGranted(): Boolean =
        runCatching { Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED }.getOrDefault(false)

    /**
     * If Shizuku still refuses this process after the person said yes, start the app
     * over (what closing and opening it did by hand). Never while sharing, so the
     * hotspot isn't touched.
     */
    private fun restartIfStillRefused() {
        if (shizukuSaysGranted() || ShareService.status.value.isActive || ShareService.status.value.isBusy) return
        restartApp()
    }

    private fun restartApp() {
        val launch = appContext.packageManager.getLaunchIntentForPackage(appContext.packageName) ?: return
        launch.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK)
        appContext.startActivity(launch)
        Runtime.getRuntime().exit(0)
    }

    fun refresh() {
        mutableState.value = when {
            !Shizuku.pingBinder() -> if (isInstalled()) ShizukuState.NOT_RUNNING else ShizukuState.NOT_INSTALLED
            Shizuku.isPreV11() -> ShizukuState.NOT_RUNNING
            !grantedHere && !shizukuSaysGranted() -> ShizukuState.NO_PERMISSION
            else -> ShizukuState.READY
        }
    }

    /** When the person last tapped "Conceder permiso" (elapsed realtime), 0 if not pending. */
    @Volatile private var askedAt = 0L

    fun requestPermission() {
        askedAt = android.os.SystemClock.elapsedRealtime()
        runCatching { Shizuku.requestPermission(PERMISSION_REQUEST) }
    }

    /**
     * Called when DarkNet comes back to the screen. A permission given inside the
     * Shizuku app reaches a running DarkNet only after a restart, so if the person
     * just went to give it and DarkNet still sees "no", restart once.
     */
    fun onAppResumed() {
        refresh()
        val asked = askedAt
        if (asked == 0L || state.value != ShizukuState.NO_PERMISSION) return
        if (android.os.SystemClock.elapsedRealtime() - asked > 5 * 60_000) { askedAt = 0; return }
        askedAt = 0
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            refresh()
            if (state.value == ShizukuState.NO_PERMISSION &&
                !ShareService.status.value.isActive && !ShareService.status.value.isBusy
            ) restartApp()
        }, 800)
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
