package com.elcondor.optimizer.data

import android.content.Context
import android.os.PowerManager

enum class Severity { CRITICAL, WARNING, TIP, OK }

enum class Action { NONE, BATTERY, NETWORK, CLEANER, APPS, USAGE_PERM, FILES_PERM, BOOST,
    DISPLAY, LOCATION, BLUETOOTH, NFC, WIFI, BG_LIMITS, DEVICE_CARE, DATA_USAGE, UPDATE }

data class Tip(val severity: Severity, val title: String, val detail: String, val action: Action = Action.NONE, val actionLabel: String? = null)

data class Analysis(
    val battery: BatterySnapshot,
    val memory: MemorySnapshot,
    val storage: StorageSnapshot,
    val identity: DeviceIdentity,
    val drainers: SystemDrainers,
    val topDrain: List<DrainEntry>,
    val topData: List<NetUsageRow>,
    val connection: ConnectionInfo,
    val unusedApps: Int,
    val screenOnMs: Long,
    val usageAccess: Boolean,
    val filesAccess: Boolean,
    val junk: JunkResult?,
    val score: Int,
    val tips: List<Tip>,
)

/** "Asistente inteligente": motor de reglas que interpreta todas las métricas del teléfono. */
object Advisor {

    fun analyze(ctx: Context): Analysis {
        val battery = DeviceInfo.battery(ctx)
        val mem = DeviceInfo.memory(ctx)
        val sto = DeviceInfo.storage(ctx)
        val id = DeviceInfo.identity(ctx)
        val dr = DeviceInfo.drainers(ctx)
        val usage = Perms.hasUsageAccess(ctx)
        val files = Perms.hasAllFiles()
        val drain = if (usage) AppsRepo.batteryDrain(ctx, 24) else emptyList()
        val data = if (usage) NetworkRepo.usageRows(ctx, 24) else emptyList()
        val conn = NetworkRepo.connection(ctx)
        val unused = if (usage) {
            val apps = AppsRepo.installed(ctx)
            AppsRepo.fillUsage(ctx, apps, System.currentTimeMillis() - 86_400_000L)
            apps.count { !it.system && it.lastUsed in 1 until System.currentTimeMillis() - 30L * 86_400_000 }
        } else 0
        val sot = if (usage) AppsRepo.screenOnTime(ctx, AppsRepo.startOfToday()) else 0
        val junk = JunkScanner.lastResult

        val tips = mutableListOf<Tip>()
        var score = 100

        if (!usage) {
            score -= 5
            tips += Tip(Severity.WARNING, "Activa el acceso de uso",
                "Sin este permiso no puedo ver qué apps gastan batería ni datos. Es un permiso de solo lectura.",
                Action.USAGE_PERM, "Conceder")
        }
        if (!files) tips += Tip(Severity.TIP, "Permite el acceso a archivos",
            "Necesario para encontrar y borrar archivos basura, duplicados y APKs.", Action.FILES_PERM, "Conceder")

        // ---------- Batería ----------
        if (battery.temperatureC >= 42) {
            score -= 15
            tips += Tip(Severity.CRITICAL, "Batería muy caliente (${battery.temperatureC} °C)",
                "El calor degrada la batería de forma permanente. Quita la funda, deja de jugar o cargar y evita el sol.", Action.BATTERY, "Ver batería")
        } else if (battery.temperatureC >= 38) {
            score -= 6
            tips += Tip(Severity.WARNING, "Batería tibia (${battery.temperatureC} °C)",
                "Algo exige mucho al teléfono. Revisa las apps que más consumen ahora.", Action.BATTERY, "Ver batería")
        }
        if (!battery.healthGood && battery.health != "Desconocida") {
            score -= 10
            tips += Tip(Severity.CRITICAL, "Salud de batería: ${battery.health}", "Considera revisarla en un centro de servicio Samsung.", Action.DEVICE_CARE, "Device Care")
        }
        battery.cycleCount?.let {
            if (it > 800) tips += Tip(Severity.WARNING, "Batería con $it ciclos",
                "A partir de ~800 ciclos la capacidad suele bajar del 80 %. Activa 'Proteger batería' (carga al 80 %).", Action.BATTERY, "Ajustes")
            else if (it > 300) tips += Tip(Severity.TIP, "Cuida tu batería ($it ciclos)",
                "Activa Ajustes › Batería › Protección de batería › 'Básica' o 'Máxima' para alargar su vida útil.", Action.BATTERY, "Ajustes")
        }
        drain.firstOrNull()?.let { d ->
            if (d.sharePercent > 25 && d.bgServiceMs > 60 * 60_000) {
                score -= 8
                tips += Tip(Severity.WARNING, "${d.app.label} consume mucho en segundo plano",
                    "Estuvo activa ${Fmt.duration(d.bgServiceMs)} sin que la usaras. Ponla en 'Apps en suspensión profunda'.",
                    Action.BG_LIMITS, "Limitar")
            }
        }
        drain.filter { it.bgBytes > 200L * 1_048_576 && it.fgTimeMs < 10 * 60_000 }.take(2).forEach { d ->
            score -= 4
            tips += Tip(Severity.WARNING, "${d.app.label} gasta datos y batería a escondidas",
                "Usó ${Fmt.bytes(d.bgBytes)} en segundo plano en 24 h casi sin abrirla.", Action.BG_LIMITS, "Limitar")
        }

        // ---------- Drenadores del sistema ----------
        dr.brightnessPercent?.let { if (it > 80 && dr.autoBrightness == false) {
            score -= 4
            tips += Tip(Severity.TIP, "Brillo muy alto ($it %)", "La pantalla es el mayor consumidor. Activa el brillo adaptable.", Action.DISPLAY, "Pantalla")
        } }
        dr.screenTimeoutSec?.let { if (it >= 300) tips += Tip(Severity.TIP, "Pantalla se apaga tarde (${it / 60} min)",
            "Bájalo a 30 s–1 min para ahorrar batería.", Action.DISPLAY, "Pantalla") }
        if (battery.level in 0..20 && !dr.powerSave && !battery.charging)
            tips += Tip(Severity.WARNING, "Batería baja (${battery.level} %)", "Activa el Ahorro de energía para llegar al final del día.", Action.BATTERY, "Activar")
        if (dr.aodOn == true) tips += Tip(Severity.TIP, "Always On Display activo",
            "Gasta ~1 % por hora. Configúralo en 'Tocar para mostrar' o por horario.", Action.DISPLAY, "Ajustar")

        // ---------- Memoria / almacenamiento ----------
        if (mem.lowMemory || mem.usedFraction > 0.9f) {
            score -= 8
            tips += Tip(Severity.WARNING, "Memoria RAM casi llena (${Fmt.pct(mem.usedFraction)})",
                "Cierra procesos en segundo plano para que el teléfono vuelva a ir fluido.", Action.BOOST, "Optimizar")
        }
        if (sto.usedFraction > 0.9f) {
            score -= 15
            tips += Tip(Severity.CRITICAL, "Almacenamiento casi lleno (${Fmt.pct(sto.usedFraction)})",
                "Con poco espacio el teléfono se vuelve lento. Libera al menos un 15 %.", Action.CLEANER, "Limpiar")
        } else if (sto.usedFraction > 0.8f) {
            score -= 6
            tips += Tip(Severity.WARNING, "Almacenamiento al ${Fmt.pct(sto.usedFraction)}", "Libera espacio con el limpiador.", Action.CLEANER, "Limpiar")
        }
        junk?.let { j -> if (j.safeBytes > 300L * 1_048_576) {
            score -= 4
            tips += Tip(Severity.TIP, "${Fmt.bytes(j.safeBytes)} de basura segura", "Se puede borrar sin perder nada importante.", Action.CLEANER, "Limpiar")
        } }
        if (unused >= 5) {
            score -= 3
            tips += Tip(Severity.TIP, "$unused apps sin usar en 30+ días", "Desinstálalas para liberar espacio y procesos en segundo plano.", Action.APPS, "Ver apps")
        }

        // ---------- Rendimiento ----------
        val days = id.uptimeMs / 86_400_000
        if (days >= 7) {
            score -= 5
            tips += Tip(Severity.TIP, "Sin reiniciar desde hace $days días",
                "Reiniciar limpia la memoria y procesos atascados. Activa Device Care › Optimización automática › Reinicio automático.", Action.DEVICE_CARE, "Device Care")
        }
        if (id.thermalStatus >= PowerManager.THERMAL_STATUS_MODERATE) {
            score -= 10
            tips += Tip(Severity.CRITICAL, "El procesador se está limitando por calor",
                "Estado térmico: ${DeviceInfo.thermalLabel(id.thermalStatus)}. El rendimiento baja hasta que se enfríe.", Action.BATTERY, "Ver")
        }

        // ---------- Red ----------
        if (conn.type == "Wi-Fi") {
            conn.wifiRssi?.let { if (it < -75) {
                score -= 5
                tips += Tip(Severity.WARNING, "Señal Wi-Fi débil ($it dBm)", "Acércate al router; una señal débil hace lento el internet y gasta más batería.", Action.NETWORK, "Ver red")
            } }
            if (conn.wifiBand == "2.4 GHz" && (conn.wifiRssi ?: -100) > -65)
                tips += Tip(Severity.TIP, "Conectado a 2.4 GHz", "Tu S25 Ultra soporta Wi-Fi 7. Si tu router tiene red 5 GHz o 6 GHz, conéctate a ella para ir mucho más rápido.", Action.WIFI, "Wi-Fi")
        }
        if (conn.vpn) tips += Tip(Severity.TIP, "VPN activa", "Una VPN puede reducir la velocidad y añadir latencia.", Action.NETWORK, "Ver red")
        data.firstOrNull { it.net.bgBytes > 500L * 1_048_576 }?.let {
            tips += Tip(Severity.WARNING, "${it.label} satura tu conexión",
                "Usó ${Fmt.bytes(it.net.bgBytes)} en segundo plano (24 h). Esto puede volver lento tu internet.", Action.NETWORK, "Ver red")
        }

        if (tips.none { it.severity != Severity.TIP && it.severity != Severity.OK })
            tips += Tip(Severity.OK, "Todo se ve bien", "Tu teléfono está en buen estado. Sigue así.")

        return Analysis(battery, mem, sto, id, dr, drain.take(5), data.take(5), conn, unused, sot, usage, files, junk,
            score.coerceIn(0, 100), tips.sortedBy { it.severity.ordinal })
    }
}
