package dev.darknet

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import dev.darknet.data.Bytes
import dev.darknet.data.Prefs
import dev.darknet.data.ShareStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONObject

/**
 * Keeps sharing alive while the app is in the background: starts and stops the
 * privileged service, follows its status every second and shows it in a
 * notification.
 */
class ShareService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var follow: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, notification(mutableStatus.value), ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        when (intent?.action) {
            ACTION_STOP -> stopSharing()
            else -> startSharing()
        }
        return START_NOT_STICKY
    }

    private fun startSharing() {
        if (mutableStatus.value.isActive || mutableStatus.value.isBusy) return
        mutableStatus.value = ShareStatus(ShareStatus.State.STARTING)
        scope.launch {
            try {
                val settings = Prefs(this@ShareService).settings.first()
                val options = JSONObject().put("fastEngine", settings.fastEngine).put("usb", settings.usb)
                val service = ShizukuLink.service()
                publish(ShareStatus.parse(service.start(options.toString())))
                if (mutableStatus.value.isActive) followStatus() else stopSelf()
            } catch (failure: Throwable) {
                publish(ShareStatus(ShareStatus.State.ERROR, failure.message ?: failure.javaClass.simpleName))
                stopSelf()
            }
        }
    }

    private fun followStatus() {
        follow?.cancel()
        follow = scope.launch {
            var last = System.currentTimeMillis()
            while (isActive) {
                delay(1000)
                val now = System.currentTimeMillis()
                val next = runCatching { ShizukuLink.service().status() }.getOrNull() ?: continue
                publish(ShareStatus.parse(next, mutableStatus.value, now - last))
                last = now
                if (!mutableStatus.value.isActive) {
                    stopSelf()
                    return@launch
                }
            }
        }
    }

    private fun stopSharing() {
        follow?.cancel()
        mutableStatus.value = mutableStatus.value.copy(state = ShareStatus.State.STOPPING)
        scope.launch {
            runCatching { ShizukuLink.service().stop() }
            publish(ShareStatus())
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    private fun publish(status: ShareStatus) {
        mutableStatus.value = status
        getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, notification(status))
    }

    private fun notification(status: ShareStatus): Notification {
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL, "Compartir internet", NotificationManager.IMPORTANCE_LOW),
        )
        val open = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE,
        )
        val stop = PendingIntent.getService(
            this, 1, Intent(this, ShareService::class.java).setAction(ACTION_STOP), PendingIntent.FLAG_IMMUTABLE,
        )
        val (title, text) = when (status.state) {
            ShareStatus.State.ACTIVE -> "Compartiendo" to
                "${status.devices.size} dispositivos · ${Bytes.format(status.total)} · ${Bytes.rate(status.speed)}" +
                if (status.fastPath) " · motor rápido" else ""
            ShareStatus.State.STARTING -> "Iniciando…" to "Preparando el hotspot"
            ShareStatus.State.STOPPING -> "Deteniendo…" to "Restaurando el hotspot"
            ShareStatus.State.ERROR -> "No se pudo compartir" to status.detail
            ShareStatus.State.IDLE -> "DarkNet" to "Sin compartir"
        }
        return Notification.Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_darknet)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(open)
            .setOngoing(status.isActive || status.isBusy)
            .addAction(Notification.Action.Builder(null, "Detener", stop).build())
            .build()
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        private const val CHANNEL = "sharing"
        private const val NOTIFICATION_ID = 1
        private const val ACTION_STOP = "dev.darknet.STOP"

        private val mutableStatus = MutableStateFlow(ShareStatus())
        val status: StateFlow<ShareStatus> = mutableStatus.asStateFlow()

        fun start(context: Context) {
            context.startForegroundService(Intent(context, ShareService::class.java))
        }

        fun stop(context: Context) {
            context.startForegroundService(Intent(context, ShareService::class.java).setAction(ACTION_STOP))
        }

        /** Asks the privileged service for its log (JSON array of lines). */
        suspend fun log(): List<String> = runCatching {
            val array = org.json.JSONArray(ShizukuLink.service().log())
            List(array.length()) { array.getString(it) }
        }.getOrElse { listOf("No se pudo leer el registro: ${it.message}") }
    }
}
