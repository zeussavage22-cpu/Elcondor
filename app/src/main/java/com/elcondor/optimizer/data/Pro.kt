package com.elcondor.optimizer.data

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import rikka.shizuku.Shizuku
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

enum class ProStatus { NOT_INSTALLED, NOT_RUNNING, NO_PERMISSION, READY, UNSUPPORTED }

data class ShellResult(val code: Int, val out: String, val err: String) { val ok get() = code == 0 }

data class PowerComponent(val key: String, val label: String, val mah: Double)
data class AppPower(val uid: Int, val label: String, val pkg: String?, val mah: Double, val fgMah: Double?, val bgMah: Double?, val fgsMah: Double?)
data class PowerReport(val capacity: Double?, val computedDrain: Double?, val components: List<PowerComponent>, val apps: List<AppPower>)

/**
 * "Modo Pro" con Shizuku: ejecuta comandos con privilegios de ADB (usuario shell) sin root.
 * Permite autoconceder permisos, leer el consumo real en mAh y aplicar optimizaciones del sistema.
 */
object Pro {
    const val SHIZUKU_PKG = "moe.shizuku.privileged.api"
    private const val REQ = 4242

    var status by mutableStateOf(ProStatus.NOT_RUNNING)
        private set

    val ready get() = status == ProStatus.READY

    private var onGranted: (() -> Unit)? = null
    private val binderReceived = Shizuku.OnBinderReceivedListener { update() }
    private val binderDead = Shizuku.OnBinderDeadListener { update() }
    private val permResult = Shizuku.OnRequestPermissionResultListener { code, result ->
        update()
        if (code == REQ && result == PackageManager.PERMISSION_GRANTED) onGranted?.invoke()
    }
    private var appCtx: Context? = null

    fun init(ctx: Context) {
        appCtx = ctx.applicationContext
        Shizuku.addBinderReceivedListenerSticky(binderReceived)
        Shizuku.addBinderDeadListener(binderDead)
        Shizuku.addRequestPermissionResultListener(permResult)
        update()
    }

    fun dispose() {
        Shizuku.removeBinderReceivedListener(binderReceived)
        Shizuku.removeBinderDeadListener(binderDead)
        Shizuku.removeRequestPermissionResultListener(permResult)
    }

    fun update() {
        val ctx = appCtx
        status = try {
            when {
                !Shizuku.pingBinder() -> if (ctx != null && !installed(ctx)) ProStatus.NOT_INSTALLED else ProStatus.NOT_RUNNING
                Shizuku.isPreV11() -> ProStatus.UNSUPPORTED
                Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED -> ProStatus.READY
                else -> ProStatus.NO_PERMISSION
            }
        } catch (_: Throwable) { ProStatus.NOT_RUNNING }
    }

    fun installed(ctx: Context) = try { ctx.packageManager.getPackageInfo(SHIZUKU_PKG, 0); true } catch (_: Exception) { false }

    fun requestPermission(onOk: () -> Unit) {
        onGranted = onOk
        try {
            if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) { update(); onOk() }
            else Shizuku.requestPermission(REQ)
        } catch (_: Throwable) { update() }
    }

    fun openShizuku(ctx: Context) {
        val launch = ctx.packageManager.getLaunchIntentForPackage(SHIZUKU_PKG)
        if (launch != null) Nav.tryStart(ctx, launch) else installShizuku(ctx)
    }

    fun installShizuku(ctx: Context) = Nav.open(ctx,
        Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$SHIZUKU_PKG")),
        Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/RikkaApps/Shizuku/releases")))

    // ----------------------------------------------------------------------------- Ejecución

    private val newProcess by lazy {
        Shizuku::class.java.getDeclaredMethod("newProcess", Array<String>::class.java, Array<String>::class.java, String::class.java)
            .apply { isAccessible = true }
    }

    /** Ejecuta con Shizuku si está listo; si no, como la propia app (sirve para lecturas con permisos ya concedidos). */
    fun sh(cmd: String, timeoutSec: Long = 60): ShellResult = try {
        val p = if (ready) newProcess.invoke(null, arrayOf("sh", "-c", cmd), null, null) as Process
        else Runtime.getRuntime().exec(arrayOf("sh", "-c", cmd))
        var err = ""
        val t = thread { err = try { p.errorStream.bufferedReader().readText() } catch (_: Exception) { "" } }
        val out = p.inputStream.bufferedReader().readText()
        p.waitFor(timeoutSec, TimeUnit.SECONDS)
        t.join(2000)
        ShellResult(try { p.exitValue() } catch (_: Exception) { -1 }, out, err)
    } catch (e: Throwable) { ShellResult(-1, "", e.message ?: "error") }

    // ----------------------------------------------------------------------------- Permisos

    /** Concede a Cóndor todos sus permisos especiales. Algunos (DUMP, WRITE_SECURE_SETTINGS) persisten tras reiniciar. */
    fun grantAll(ctx: Context): Int {
        val p = ctx.packageName
        val cmds = mutableListOf(
            "appops set $p GET_USAGE_STATS allow",
            "appops set $p MANAGE_EXTERNAL_STORAGE allow",
            "appops set $p ACCESS_RESTRICTED_SETTINGS allow",
            "pm grant $p android.permission.DUMP",
            "pm grant $p android.permission.WRITE_SECURE_SETTINGS",
        )
        Perms.mediaPermissions().forEach { cmds += "pm grant $p $it" }
        return cmds.count { sh(it).ok }
    }

    val hasDump get() = appCtx?.checkSelfPermission(android.Manifest.permission.DUMP) == PackageManager.PERMISSION_GRANTED
    val hasSecureSettings get() = appCtx?.checkSelfPermission(android.Manifest.permission.WRITE_SECURE_SETTINGS) == PackageManager.PERMISSION_GRANTED

    /** ¿Se pueden leer las estadísticas reales de batería? (Shizuku activo o permiso DUMP ya concedido). */
    val canReadPower get() = ready || hasDump
    val canTweak get() = ready || hasSecureSettings

    // ----------------------------------------------------------------------------- Batería real

    private val componentNames = mapOf(
        "screen" to "Pantalla", "cpu" to "Procesador", "mobile_radio" to "Red móvil", "wifi" to "Wi-Fi",
        "gnss" to "GPS", "bluetooth" to "Bluetooth", "camera" to "Cámara", "flashlight" to "Linterna",
        "audio" to "Audio", "video" to "Video", "sensors" to "Sensores", "wakelock" to "Wakelocks",
        "idle" to "Reposo", "system_services" to "Servicios del sistema", "memory" to "Memoria",
        "phone" to "Llamadas", "ambient_display" to "Always On Display", "cell" to "Señal móvil",
        "custom" to "Otros", "any" to "Otros",
    )

    fun powerReport(ctx: Context): PowerReport? {
        val r = sh("dumpsys batterystats --charged", 90)
        if (r.out.isBlank()) return null
        val pm = ctx.packageManager
        return parsePower(r.out) { uid -> uidLabel(pm, uid) }
    }

    /** Analiza la sección "Estimated power use (mAh)" de dumpsys batterystats. Función pura (probada en tests). */
    fun parsePower(text: String, label: (Int) -> Pair<String, String?>): PowerReport? {
        val lines = text.lines()
        val start = lines.indexOfFirst { it.contains("Estimated power use (mAh)") }
        if (start < 0) return null
        val baseIndent = lines[start].indexOfFirst { !it.isWhitespace() }
        var capacity: Double? = null; var drain: Double? = null
        val comps = LinkedHashMap<String, Double>()
        val apps = LinkedHashMap<Int, AppPower>()
        val compRe = Regex("""^\s+([a-z_]+): ([\d.]+)(?: apps: [\d.]+)?(?: duration: .*)?$""")
        val uidRe = Regex("""^\s*(?:UID|Uid) (\S+?): ([\d.]+)(?: fg: ([\d.]+) bg: ([\d.]+)(?: fgs: ([\d.]+))?)?""")
        var inGlobal = false
        for (i in start + 1 until lines.size) {
            val l = lines[i]
            if (l.isBlank()) break
            if (l.indexOfFirst { !it.isWhitespace() } <= baseIndent) break
            val t = l.trim()
            if (t.startsWith("Capacity:")) {
                capacity = Regex("""Capacity: ([\d.]+)""").find(t)?.groupValues?.get(1)?.toDoubleOrNull()
                drain = Regex("""Computed drain: ([\d.]+)""").find(t)?.groupValues?.get(1)?.toDoubleOrNull()
                continue
            }
            if (t == "Global") { inGlobal = true; continue }
            val m = uidRe.find(l)
            if (m != null) {
                inGlobal = false
                val uid = parseUid(m.groupValues[1]) ?: continue
                if (apps.containsKey(uid)) continue
                val mah = m.groupValues[2].toDoubleOrNull() ?: continue
                if (mah < 0.05) continue
                val (name, pkg) = label(uid)
                apps[uid] = AppPower(uid, name, pkg, mah, m.groupValues[3].toDoubleOrNull(), m.groupValues[4].toDoubleOrNull(), m.groupValues[5].toDoubleOrNull())
                continue
            }
            if (inGlobal) compRe.find(l)?.let { c ->
                val v = c.groupValues[2].toDoubleOrNull() ?: 0.0
                if (v >= 0.5) comps[c.groupValues[1]] = (comps[c.groupValues[1]] ?: 0.0) + v
            }
        }
        if (apps.isEmpty() && comps.isEmpty()) return null
        return PowerReport(capacity, drain,
            comps.map { (k, v) -> PowerComponent(k, componentNames[k] ?: k.replace('_', ' ').replaceFirstChar { it.uppercase() }, v) }.sortedByDescending { it.mah },
            apps.values.sortedByDescending { it.mah })
    }

    private fun parseUid(s: String): Int? {
        Regex("""u(\d+)a(\d+)""").matchEntire(s)?.let { return it.groupValues[1].toInt() * 100_000 + 10_000 + it.groupValues[2].toInt() }
        return s.toIntOrNull()
    }

    private fun uidLabel(pm: PackageManager, uid: Int): Pair<String, String?> = when (uid) {
        0 -> "Kernel" to null
        1000 -> "Sistema Android" to "android"
        1001 -> "Teléfono / radio" to null
        1002 -> "Bluetooth" to null
        1010 -> "Wi-Fi" to null
        1013 -> "Multimedia" to null
        1041 -> "Audio" to null
        1047 -> "Cámara (servicio)" to null
        else -> {
            val pkgs = pm.getPackagesForUid(uid)
            if (pkgs.isNullOrEmpty()) (pm.getNameForUid(uid) ?: "UID $uid") to null
            else {
                val p = pkgs.first()
                (try { pm.getApplicationInfo(p, 0).loadLabel(pm).toString() } catch (_: Exception) { p }) to p
            }
        }
    }

    /** Salud de la batería en % (Samsung expone "ASOC" = capacidad actual frente a la original). */
    fun batteryHealthPercent(): Int? {
        for (path in listOf("/sys/class/power_supply/battery/fg_asoc", "/sys/class/power_supply/battery/batt_asoc")) {
            sh("cat $path").out.trim().toIntOrNull()?.takeIf { it in 40..110 }?.let { return it.coerceAtMost(100) }
        }
        val dump = sh("dumpsys battery").out
        Regex("""(?i)asoc\w*[:=]\s*(\d+)""").find(dump)?.groupValues?.get(1)?.toIntOrNull()?.takeIf { it in 40..110 }?.let { return it.coerceAtMost(100) }
        return null
    }

    // ----------------------------------------------------------------------------- Acciones

    fun forceStop(pkg: String) = sh("am force-stop $pkg").ok

    fun restrictBackground(pkg: String, restrict: Boolean): Boolean {
        val a = sh("cmd appops set $pkg RUN_ANY_IN_BACKGROUND ${if (restrict) "ignore" else "allow"}").ok
        sh("am set-standby-bucket $pkg ${if (restrict) "restricted" else "active"}")
        if (restrict) forceStop(pkg)
        return a
    }

    fun isRestricted(pkg: String) = sh("cmd appops get $pkg RUN_ANY_IN_BACKGROUND").out.contains("ignore")

    fun setEnabled(pkg: String, enabled: Boolean) =
        sh(if (enabled) "pm enable $pkg" else "pm disable-user --user 0 $pkg").ok

    /** Borra la caché de TODAS las apps. Devuelve bytes liberados. */
    fun trimCaches(ctx: Context): Long {
        val before = DeviceInfo.storage(ctx).freeBytes
        sh("pm trim-caches 9999G", 120)
        Thread.sleep(500)
        return (DeviceInfo.storage(ctx).freeBytes - before).coerceAtLeast(0)
    }

    /** Recompila las apps como hace el sistema por la noche (más fluidez). Puede tardar varios minutos. */
    fun optimizeApps() = sh("cmd package bg-dexopt-job", 900).ok

    fun reboot() = sh("svc power reboot")

    fun animationScale(ctx: Context): Float =
        try { Settings.Global.getFloat(ctx.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE) } catch (_: Exception) { 1f }

    fun setAnimationScale(ctx: Context, v: Float): Boolean {
        val keys = listOf(Settings.Global.WINDOW_ANIMATION_SCALE, Settings.Global.TRANSITION_ANIMATION_SCALE, Settings.Global.ANIMATOR_DURATION_SCALE)
        if (hasSecureSettings) return try { keys.forEach { Settings.Global.putFloat(ctx.contentResolver, it, v) }; true } catch (_: Exception) { false }
        return keys.all { sh("settings put global $it $v").ok }
    }

    /** Paquetes que nunca se deben desactivar. */
    val protectedPkgs = setOf(
        "android", "com.android.systemui", "com.android.settings", "com.android.phone", "com.android.vending",
        "com.google.android.gms", "com.sec.android.app.launcher", "com.samsung.android.honeyboard",
        "com.android.providers.settings", "com.android.shell", SHIZUKU_PKG, "com.elcondor.optimizer",
        "com.samsung.android.lool", "com.android.packageinstaller", "com.google.android.packageinstaller",
    )

    @Suppress("unused") private val sdk = Build.VERSION.SDK_INT
}
