package com.elcondor.optimizer.data

import android.app.ActivityManager
import android.app.usage.StorageStatsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Process
import android.os.storage.StorageManager
import android.util.LruCache
import androidx.core.graphics.drawable.toBitmap
import java.util.Calendar

data class AppEntry(
    val pkg: String,
    val label: String,
    val uid: Int,
    val system: Boolean,
    val installTime: Long,
    var lastUsed: Long = 0,
    var fgTimeMs: Long = 0,
    var fgServiceMs: Long = 0,
    var appBytes: Long = 0,
    var dataBytes: Long = 0,
    var cacheBytes: Long = 0,
) {
    val totalBytes get() = appBytes + dataBytes + cacheBytes
}

data class DrainEntry(
    val app: AppEntry,
    val score: Double,
    val sharePercent: Float,
    val fgTimeMs: Long,
    val bgServiceMs: Long,
    val bgBytes: Long,
    val totalBytes: Long,
    val reason: String,
)

object AppsRepo {
    private val iconCache = LruCache<String, Bitmap>(200)

    fun icon(ctx: Context, pkg: String): Bitmap? {
        iconCache.get(pkg)?.let { return it }
        return try {
            ctx.packageManager.getApplicationIcon(pkg).toBitmap(96, 96).also { iconCache.put(pkg, it) }
        } catch (_: Exception) { null }
    }

    /** Apps instaladas: de usuario + apps de sistema que tengan icono en el launcher. */
    fun installed(ctx: Context, includeSystem: Boolean = false): List<AppEntry> {
        val pm = ctx.packageManager
        val launchable = pm.queryIntentActivities(
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0
        ).map { it.activityInfo.packageName }.toSet()
        return pm.getInstalledPackages(0).mapNotNull { p ->
            val ai = p.applicationInfo ?: return@mapNotNull null
            val sys = (ai.flags and ApplicationInfo.FLAG_SYSTEM) != 0 &&
                (ai.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) == 0
            if (p.packageName == ctx.packageName) return@mapNotNull null
            if (sys && !includeSystem && p.packageName !in launchable) return@mapNotNull null
            AppEntry(
                pkg = p.packageName,
                label = ai.loadLabel(pm).toString(),
                uid = ai.uid,
                system = sys,
                installTime = p.firstInstallTime,
            )
        }
    }

    fun startOfToday(): Long = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    /** Rellena tiempo en primer plano, servicios en segundo plano y último uso. */
    fun fillUsage(ctx: Context, apps: List<AppEntry>, start: Long, end: Long = System.currentTimeMillis()) {
        if (!Perms.hasUsageAccess(ctx)) return
        val usm = ctx.getSystemService(UsageStatsManager::class.java)
        val agg = usm.queryAndAggregateUsageStats(start, end)
        // Último uso: ventana larga (1 año) para detectar apps abandonadas
        val longAgg = usm.queryAndAggregateUsageStats(end - 365L * 86_400_000, end)
        for (a in apps) {
            agg[a.pkg]?.let {
                a.fgTimeMs = it.totalTimeInForeground
                a.fgServiceMs = it.totalTimeForegroundServiceUsed
            }
            a.lastUsed = maxOf(longAgg[a.pkg]?.lastTimeUsed ?: 0L, agg[a.pkg]?.lastTimeUsed ?: 0L)
        }
    }

    /** Tiempo con pantalla encendida en la ventana dada. */
    fun screenOnTime(ctx: Context, start: Long, end: Long = System.currentTimeMillis()): Long {
        if (!Perms.hasUsageAccess(ctx)) return 0
        val usm = ctx.getSystemService(UsageStatsManager::class.java)
        val ev = usm.queryEvents(start, end)
        val e = UsageEvents.Event()
        var total = 0L
        var onSince = -1L
        var first = true
        while (ev.hasNextEvent()) {
            ev.getNextEvent(e)
            when (e.eventType) {
                UsageEvents.Event.SCREEN_INTERACTIVE -> { onSince = e.timeStamp; first = false }
                UsageEvents.Event.SCREEN_NON_INTERACTIVE -> {
                    if (onSince > 0) total += e.timeStamp - onSince
                    else if (first) total += e.timeStamp - start
                    onSince = -1; first = false
                }
            }
        }
        if (onSince > 0) total += end - onSince
        return total
    }

    fun fillStorage(ctx: Context, apps: List<AppEntry>) {
        if (!Perms.hasUsageAccess(ctx)) return
        val ssm = ctx.getSystemService(StorageStatsManager::class.java)
        val user = Process.myUserHandle()
        for (a in apps) try {
            val s = ssm.queryStatsForPackage(StorageManager.UUID_DEFAULT, a.pkg, user)
            a.appBytes = s.appBytes; a.dataBytes = s.dataBytes - s.cacheBytes; a.cacheBytes = s.cacheBytes
            if (a.dataBytes < 0) a.dataBytes = 0
        } catch (_: Exception) { }
    }

    fun totalCache(ctx: Context): Long {
        val apps = installed(ctx, includeSystem = true)
        fillStorage(ctx, apps)
        return apps.sumOf { it.cacheBytes }
    }

    /**
     * Estimación de consumo de batería por app. Android no permite a apps de terceros
     * leer los mAh por app, así que combinamos las señales que sí son medibles:
     * pantalla encendida con la app, servicios en primer plano (trabajo en segundo plano)
     * y tráfico de red en segundo plano (radio activa = gasto alto).
     */
    fun batteryDrain(ctx: Context, hours: Int = 24): List<DrainEntry> {
        val end = System.currentTimeMillis()
        val start = end - hours * 3_600_000L
        val apps = installed(ctx, includeSystem = true)
        fillUsage(ctx, apps, start, end)
        val net = NetworkRepo.perUid(ctx, start, end).associateBy { it.uid }
        val seenUids = HashSet<Int>()
        val list = apps.sortedByDescending { it.fgTimeMs }.map { a ->
            // Varias apps de sistema comparten UID: asignamos el tráfico solo a una
            val n = if (seenUids.add(a.uid)) net[a.uid] else null
            val bg = n?.bgBytes ?: 0L
            val tot = n?.totalBytes ?: 0L
            val score = a.fgTimeMs / 60_000.0 * 1.0 +            // minutos de pantalla
                a.fgServiceMs / 60_000.0 * 0.35 +                  // minutos de trabajo en 2º plano
                bg / 1_048_576.0 * 0.6 +                           // MB en 2º plano
                (tot - bg) / 1_048_576.0 * 0.05                    // MB en 1er plano (radio ya activa)
            Triple(a, score, n)
        }.filter { it.second > 0.5 }
        val sum = list.sumOf { it.second }.coerceAtLeast(1.0)
        return list.sortedByDescending { it.second }.map { (a, s, n) ->
            val reasons = buildList {
                if (a.fgTimeMs > 20 * 60_000) add("uso en pantalla ${Fmt.duration(a.fgTimeMs)}")
                if (a.fgServiceMs > 30 * 60_000) add("activa en 2º plano ${Fmt.duration(a.fgServiceMs)}")
                if ((n?.bgBytes ?: 0) > 50L * 1_048_576) add("datos en 2º plano ${Fmt.bytes(n!!.bgBytes)}")
            }
            DrainEntry(
                app = a, score = s, sharePercent = (s / sum * 100).toFloat(),
                fgTimeMs = a.fgTimeMs, bgServiceMs = a.fgServiceMs,
                bgBytes = n?.bgBytes ?: 0, totalBytes = n?.totalBytes ?: 0,
                reason = reasons.joinToString(" · ").ifEmpty { "uso moderado" },
            )
        }
    }

    /** Cierra procesos en segundo plano (los que Android permite) y devuelve RAM liberada. */
    fun boost(ctx: Context): Long {
        val am = ctx.getSystemService(ActivityManager::class.java)
        val before = DeviceInfo.memory(ctx).availBytes
        val pm = ctx.packageManager
        pm.getInstalledApplications(PackageManager.GET_META_DATA).forEach {
            if (it.packageName != ctx.packageName) try { am.killBackgroundProcesses(it.packageName) } catch (_: Exception) {}
        }
        ctx.cacheDir.listFiles()?.forEach { it.deleteRecursively() }
        System.gc()
        Thread.sleep(600)
        val after = DeviceInfo.memory(ctx).availBytes
        return (after - before).coerceAtLeast(0)
    }
}
