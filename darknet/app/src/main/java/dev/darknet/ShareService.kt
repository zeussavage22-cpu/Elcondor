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
import android.os.VibrationEffect
import android.os.Vibrator
import dev.darknet.data.Bytes
import dev.darknet.data.DnsChoice
import dev.darknet.data.DnsProbe
import dev.darknet.data.Health
import dev.darknet.data.Prefs
import dev.darknet.data.Session
import dev.darknet.data.Settings
import dev.darknet.data.ShareStatus
import dev.darknet.data.Usage
import dev.darknet.data.UsageStore
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
import java.time.LocalDate

/**
 * Keeps sharing alive in the background: starts and stops the privileged
 * service, follows its status every second, records usage history, enforces
 * the data plan and the "nobody connected" timer, and announces devices.
 */
class ShareService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var follow: Job? = null
    private var health: Job? = null
    private val prefs by lazy { Prefs(this) }

    // Per-session bookkeeping.
    private var sessionStart = 0L
    private var counted = 0L
    private var peakDevices = 0
    private var lastSeenSomeone = 0L
    private var knownDevices: Map<String, String> = emptyMap()
    private var lastSave = 0L

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, notification(mutableStatus.value), ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        when (intent?.action) {
            ACTION_STOP -> stopSharing(reason = null)
            else -> startSharing()
        }
        return START_NOT_STICKY
    }

    private fun startSharing() {
        if (mutableStatus.value.isActive || mutableStatus.value.isBusy) return
        mutableStatus.value = ShareStatus(ShareStatus.State.STARTING)
        scope.launch {
            try {
                val settings = prefs.settings.first()
                val options = JSONObject().put("fastEngine", settings.fastEngine).put("usb", settings.usb)
                    .put("dns", org.json.JSONArray(dnsServers(settings)))
                    .put("band", BANDS[settings.band] ?: 0)
                val service = ShizukuLink.service()
                publish(ShareStatus.parse(service.start(options.toString())))
                if (mutableStatus.value.isActive) followStatus() else stopSelf()
            } catch (failure: Throwable) {
                publish(ShareStatus(ShareStatus.State.ERROR, failure.message ?: failure.javaClass.simpleName))
                stopSelf()
            }
        }
    }

    /** The DNS servers for the devices; "auto" measures the candidates first. */
    private suspend fun dnsServers(settings: Settings): List<String> {
        if (settings.dns != "auto") return DnsChoice.servers[settings.dns].orEmpty()
        val candidates = DnsChoice.autoCandidates.map { (key, label) -> Triple(key, label, DnsChoice.servers.getValue(key).first()) }
        val best = DnsProbe.fastest(candidates)
        prefs.setDnsAutoResult(best?.let { "${it.second} · ${it.third} ms" } ?: "Sin respuesta; uso Cloudflare")
        return DnsChoice.servers.getValue(best?.first ?: "cloudflare")
    }

    /** Checks the phone's own connection every 5 s while sharing. */
    private fun watchHealth() {
        health?.cancel()
        mutableHealth.value = Health()
        health = scope.launch {
            while (isActive) {
                val before = mutableHealth.value
                val now = before.add(Health.ping())
                mutableHealth.value = now
                if (prefs.settings.first().stabilityAlerts) {
                    when {
                        now.offline && !before.offline -> alert("Se cortó el internet", "Tus datos móviles no responden; DarkNet sigue esperando.", vibrate = true)
                        !now.offline && before.offline -> alert("Volvió el internet", "La conexión respondió de nuevo.")
                        now.slow && !before.slow -> alert("Conexión lenta", "El ping lleva un rato por encima de 300 ms.")
                    }
                }
                delay(5000)
            }
        }
    }

    private fun followStatus() {
        follow?.cancel()
        sessionStart = System.currentTimeMillis()
        counted = 0
        peakDevices = 0
        lastSeenSomeone = sessionStart
        knownDevices = emptyMap()
        UsageStore.load(this)
        mutableHistory.value = emptyList()
        watchHealth()
        follow = scope.launch {
            var last = System.currentTimeMillis()
            while (isActive) {
                delay(1000)
                val now = System.currentTimeMillis()
                val next = runCatching { ShizukuLink.service().status() }.getOrNull() ?: continue
                val status = ShareStatus.parse(next, mutableStatus.value, now - last)
                last = now
                publish(status)
                if (!status.isActive) {
                    finishSession()
                    stopSelf()
                    return@launch
                }
                val settings = prefs.settings.first()
                record(status, now)
                announceDevices(status, settings)
                enforce(status, settings, now)?.let { reason ->
                    stopSharing(reason)
                    return@launch
                }
            }
        }
    }

    /** Adds new bytes to today's history; saves every half minute. */
    private fun record(status: ShareStatus, now: Long) {
        val total = status.total
        val delta = if (total >= counted) total - counted else total
        counted = total
        if (delta > 0) UsageStore.add(this, delta)
        peakDevices = maxOf(peakDevices, status.devices.size)
        if (now - lastSave > 30_000) {
            lastSave = now
            UsageStore.save(this)
        }
    }

    private fun finishSession() {
        if (sessionStart == 0L) return
        UsageStore.addSession(this, Session(sessionStart, System.currentTimeMillis(), counted, peakDevices))
        sessionStart = 0L
    }

    /** Returns why sharing should stop, or null. Also warns once at 80% of the plan. */
    private fun enforce(status: ShareStatus, settings: Settings, now: Long): String? {
        if (status.devices.isNotEmpty()) lastSeenSomeone = now
        if (settings.idleMinutes > 0 && now - lastSeenSomeone >= settings.idleMinutes * 60_000L) {
            return "Nadie se conectó en ${settings.idleMinutes} min"
        }
        if (settings.quotaGb > 0) {
            val today = LocalDate.now()
            val used = UsageStore.usage.value.cycle(today, settings.quotaResetDay)
            val limit = settings.quotaGb * 1_000_000_000L
            val cycle = Usage.cycleStart(today, settings.quotaResetDay).toString()
            if (used >= limit * 8 / 10 && warnOnce(cycle)) {
                alert(
                    "Plan de datos al ${used * 100 / limit}%",
                    "${Bytes.format(used)} de ${settings.quotaGb} GB este ciclo",
                )
            }
            if (settings.quotaStop && used >= limit) return "Se acabó tu plan de ${settings.quotaGb} GB"
        }
        return null
    }

    private fun warnOnce(cycle: String): Boolean {
        val p = getSharedPreferences("alerts", MODE_PRIVATE)
        if (p.getString("quota_cycle", null) == cycle) return false
        p.edit().putString("quota_cycle", cycle).apply()
        return true
    }

    private fun announceDevices(status: ShareStatus, settings: Settings) {
        val now = status.devices.associate { (it.mac.ifBlank { it.ip }) to it.name.ifBlank { "Un dispositivo" } }
        val joined = now.keys - knownDevices.keys
        val left = knownDevices.keys - now.keys
        knownDevices = now
        if (!settings.deviceAlerts) return
        joined.forEach { alert("${now[it]} se conectó", "${now.size} conectados", vibrate = true) }
        left.forEach { alert("Se desconectó un dispositivo", "${now.size} conectados") }
    }

    private fun alert(title: String, text: String, vibrate: Boolean = false) {
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel(ALERTS, "Avisos", NotificationManager.IMPORTANCE_DEFAULT))
        manager.notify(
            (System.nanoTime() % 100_000).toInt() + 10,
            Notification.Builder(this, ALERTS)
                .setSmallIcon(R.drawable.ic_stat_darknet)
                .setContentTitle(title)
                .setContentText(text)
                .setAutoCancel(true)
                .setTimeoutAfter(60_000)
                .build(),
        )
        if (vibrate) runCatching {
            getSystemService(Vibrator::class.java).vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_DOUBLE_CLICK))
        }
    }

    private fun stopSharing(reason: String?) {
        follow?.cancel()
        health?.cancel()
        mutableStatus.value = mutableStatus.value.copy(state = ShareStatus.State.STOPPING)
        scope.launch {
            runCatching { ShizukuLink.service().stop() }
            finishSession()
            UsageStore.save(this@ShareService)
            reason?.let { alert("DarkNet se detuvo", it) }
            publish(ShareStatus())
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    private fun publish(status: ShareStatus) {
        mutableStatus.value = status
        if (status.isActive) mutableHistory.value = (mutableHistory.value + (status.speedDown to status.speedUp)).takeLast(HISTORY)
        else health?.cancel()
        getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, notification(status))
        runCatching { DarkNetWidget.refresh(this, status) }
    }

    private fun notification(status: ShareStatus): Notification {
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel(CHANNEL, "Compartir internet", NotificationManager.IMPORTANCE_LOW))
        val open = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        val stop = PendingIntent.getService(
            this, 1, Intent(this, ShareService::class.java).setAction(ACTION_STOP), PendingIntent.FLAG_IMMUTABLE,
        )
        val (title, text) = when (status.state) {
            ShareStatus.State.ACTIVE -> "Compartiendo" to
                "${status.devices.size} conectados · ${Bytes.format(status.total)} · ${Bytes.rate(status.speed)}" +
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
        private const val ALERTS = "alerts"
        private const val NOTIFICATION_ID = 1
        const val ACTION_STOP = "dev.darknet.STOP"

        private val mutableStatus = MutableStateFlow(ShareStatus())
        val status: StateFlow<ShareStatus> = mutableStatus.asStateFlow()

        private val mutableHealth = MutableStateFlow(Health())
        /** Connection quality of the current session. */
        val connection: StateFlow<Health> = mutableHealth.asStateFlow()

        /** Seconds of history kept for the live chart. */
        const val HISTORY = 60
        private val mutableHistory = MutableStateFlow<List<Pair<Long, Long>>>(emptyList())
        /** Download and upload bytes per second, one entry per second, oldest first. */
        val history: StateFlow<List<Pair<Long, Long>>> = mutableHistory.asStateFlow()

        /** Setting value to SoftApConfiguration band. */
        private val BANDS = mapOf("2" to 1, "5" to 2, "6" to 4)

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
