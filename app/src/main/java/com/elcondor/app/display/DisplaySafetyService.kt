package com.elcondor.app.display

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.elcondor.app.ElCondorApp
import com.elcondor.app.MainActivity
import com.elcondor.app.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Servicio en primer plano mientras hay un cambio de DPI/resolución sin confirmar.
 *
 * Para qué sirve:
 *  1. Sube la prioridad del proceso: Android casi nunca mata un proceso con un
 *     servicio en primer plano, así que la reversión de 10 s se ejecuta aunque
 *     el usuario salga de la app o la memoria esté justa.
 *  2. Notificación con "Mantener" / "Revertir": si el nuevo DPI deja la app
 *     ilegible, se puede decidir desde el panel de notificaciones.
 *
 * La cuenta atrás en sí sigue en [DisplayController]; este servicio solo la
 * refleja y se detiene cuando ya no hay cambio pendiente.
 */
class DisplaySafetyService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val controller get() = (application as ElCondorApp).display
    private var started = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_KEEP -> controller.confirm()
            ACTION_REVERT -> controller.revertNow()
        }

        val pending = controller.state.value.pending
        if (!started) {
            // startForeground es obligatorio poco después de startForegroundService,
            // incluso si ya no hay nada pendiente (si no, el sistema cierra la app).
            startAsForeground(buildNotification(pending))
            started = true
            observe()
        }
        if (pending == null && intent?.action == null) stop()
        return START_NOT_STICKY
    }

    private fun observe() {
        scope.launch {
            controller.state.collect { state ->
                val pending = state.pending
                if (pending == null) {
                    stop()
                } else {
                    getSystemService(NotificationManager::class.java)
                        .notify(NOTIFICATION_ID, buildNotification(pending))
                }
            }
        }
    }

    private fun startAsForeground(notification: Notification) {
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        } else {
            0
        }
        ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, type)
    }

    private fun buildNotification(pending: PendingChange?): Notification {
        ensureChannel(this)
        val open = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_display)
            .setContentTitle(pending?.change?.description ?: "Cambio de pantalla")
            .setContentText(pending?.let { "Se revertirá en ${it.secondsLeft} s si no confirmas" } ?: "Finalizando…")
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setContentIntent(open)
            .apply {
                if (pending != null) {
                    addAction(0, "Mantener", actionIntent(ACTION_KEEP, 1))
                    addAction(0, "Revertir", actionIntent(ACTION_REVERT, 2))
                }
            }
            .build()
    }

    private fun actionIntent(action: String, requestCode: Int): PendingIntent =
        PendingIntent.getService(
            this, requestCode,
            Intent(this, DisplaySafetyService::class.java).setAction(action),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

    private fun stop() {
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        private const val TAG = "DisplaySafetyService"
        private const val CHANNEL_ID = "display_safety"
        private const val NOTIFICATION_ID = 1001
        private const val ACTION_KEEP = "com.elcondor.app.action.KEEP_DISPLAY"
        private const val ACTION_REVERT = "com.elcondor.app.action.REVERT_DISPLAY"

        /** Llamar al empezar una cuenta atrás. Nunca lanza: la cuenta atrás funciona igual sin él. */
        fun start(context: Context) {
            try {
                ContextCompat.startForegroundService(context, Intent(context, DisplaySafetyService::class.java))
            } catch (e: Exception) {
                // p. ej. ForegroundServiceStartNotAllowedException si la app ya está en segundo plano.
                Log.w(TAG, "No se pudo iniciar el servicio de seguridad", e)
            }
        }

        fun ensureChannel(context: Context) {
            val nm = context.getSystemService(NotificationManager::class.java)
            if (nm.getNotificationChannel(CHANNEL_ID) == null) {
                nm.createNotificationChannel(
                    NotificationChannel(CHANNEL_ID, "Seguridad de pantalla", NotificationManager.IMPORTANCE_LOW).apply {
                        description = "Cuenta atrás al cambiar DPI o resolución"
                    },
                )
            }
        }
    }
}
