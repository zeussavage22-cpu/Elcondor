package com.elcondor.optimizer.ui.screens

import android.os.BatteryManager
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.elcondor.optimizer.data.*
import com.elcondor.optimizer.ui.components.*
import com.elcondor.optimizer.ui.theme.Bad
import com.elcondor.optimizer.ui.theme.Good
import com.elcondor.optimizer.ui.theme.Warn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BatteryScreen(modifier: Modifier, refresh: Int, onAction: (Action) -> Unit) {
    val ctx = LocalContext.current
    var battery by remember { mutableStateOf(DeviceInfo.battery(ctx)) }
    val samples = remember { mutableStateListOf<Float>() }
    var hours by remember { mutableIntStateOf(24) }
    var drain by remember { mutableStateOf<List<DrainEntry>?>(null) }
    var drainers by remember { mutableStateOf<SystemDrainers?>(null) }
    val usage = remember(refresh) { Perms.hasUsageAccess(ctx) }

    // Monitor en vivo cada segundo
    LaunchedEffect(Unit) {
        while (isActive) {
            battery = DeviceInfo.battery(ctx)
            battery.watts?.let { samples += it; if (samples.size > 90) samples.removeAt(0) }
            delay(1000)
        }
    }
    LaunchedEffect(refresh, hours) {
        drain = null
        drainers = withContext(Dispatchers.IO) { DeviceInfo.drainers(ctx) }
        drain = withContext(Dispatchers.IO) { if (usage) AppsRepo.batteryDrain(ctx, hours) else emptyList() }
    }

    Column(modifier.fillMaxSize()) {
        TopAppBar(title = { Text("Batería", fontWeight = FontWeight.Bold) })
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                SectionCard(title = "Consumo en tiempo real", icon = Icons.Filled.Bolt) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text("${battery.level}%", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold,
                            color = if (battery.level > 20) Good else Bad)
                        Spacer(Modifier.width(16.dp))
                        Column {
                            Text(battery.status + if (battery.charging) " · ${battery.plugged}" else "", fontWeight = FontWeight.SemiBold)
                            battery.watts?.let {
                                Text(String.format(Locale.US, "%.2f W · %d mA", it, kotlin.math.abs(battery.currentMa ?: 0)),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Sparkline(samples.toList(), Modifier.fillMaxWidth().height(70.dp), if (battery.charging) Good else Warn)
                    Spacer(Modifier.height(8.dp))
                    val w = battery.watts ?: 0f
                    Text(
                        when {
                            battery.charging -> chargeHint(w)
                            w > 6 -> "Consumo MUY alto ahora mismo: un juego, la cámara, GPS o brillo máximo."
                            w > 3 -> "Consumo alto: revisa qué app tienes abierta."
                            w > 0 -> "Consumo normal."
                            else -> "Leyendo sensor…"
                        }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    estimate(ctx, battery)?.let { Text(it, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium) }
                }
            }
            item {
                SectionCard(title = "Estado de la batería", icon = Icons.Filled.HealthAndSafety) {
                    KeyValue("Salud", battery.health, if (battery.healthGood) Good else Bad)
                    KeyValue("Temperatura", "${battery.temperatureC} °C",
                        when { battery.temperatureC >= 42 -> Bad; battery.temperatureC >= 38 -> Warn; else -> Good })
                    KeyValue("Voltaje", String.format(Locale.US, "%.2f V", battery.voltageMv / 1000f))
                    battery.cycleCount?.let { KeyValue("Ciclos de carga", "$it") }
                    battery.chargeCounterMah?.let { KeyValue("Carga restante", "$it mAh") }
                    KeyValue("Tecnología", battery.technology)
                }
            }
            item {
                SectionCard(title = "Qué está drenando tu batería", icon = Icons.Filled.BatteryAlert) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(24 to "24 horas", 72 to "3 días", 168 to "7 días").forEach { (h, l) ->
                            FilterChip(selected = hours == h, onClick = { hours = h }, label = { Text(l) })
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    when {
                        !usage -> PermissionGate("Permiso necesario",
                            "Activa 'Acceso a datos de uso' para Cóndor Care para ver qué apps gastan batería.",
                            "Conceder") { Perms.openUsageAccess(ctx) }
                        drain == null -> Loading()
                        drain!!.isEmpty() -> Text("Sin datos suficientes todavía.")
                        else -> {
                            Text("Estimación basada en tiempo en pantalla, trabajo en segundo plano y uso de datos.",
                                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(8.dp))
                            drain!!.take(15).forEach { d -> DrainRow(d) { Nav.appDetails(ctx, d.app.pkg) } }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { Nav.batteryUsage(ctx) }) { Text("Uso oficial") }
                        OutlinedButton(onClick = { Nav.backgroundLimits(ctx) }) { Text("Limitar apps") }
                    }
                }
            }
            drainers?.let { d ->
                item {
                    SectionCard(title = "Ajustes que gastan batería", icon = Icons.Filled.Tune) {
                        d.brightnessPercent?.let {
                            DrainerRow(Icons.Filled.BrightnessHigh, "Brillo", "$it %" + if (d.autoBrightness == true) " (adaptable)" else "",
                                bad = it > 80 && d.autoBrightness != true) { onAction(Action.DISPLAY) }
                        }
                        DrainerRow(Icons.Filled.Speed, "Frecuencia de pantalla", "${d.refreshRate.toInt()} Hz",
                            bad = false, hint = if (d.refreshRate > 100) "Fluidez máxima; 'Estándar' (60 Hz) ahorra ~10 %" else null) { onAction(Action.DISPLAY) }
                        d.screenTimeoutSec?.let {
                            DrainerRow(Icons.Filled.Timer, "Tiempo de pantalla", if (it >= 60) "${it / 60} min" else "$it s", bad = it >= 300) { onAction(Action.DISPLAY) }
                        }
                        d.aodOn?.let { DrainerRow(Icons.Filled.Visibility, "Always On Display", onOff(it), bad = it) { onAction(Action.DISPLAY) } }
                        DrainerRow(Icons.Filled.DarkMode, "Modo oscuro", onOff(d.darkMode), bad = !d.darkMode,
                            hint = if (!d.darkMode) "En pantallas AMOLED ahorra batería" else null) { onAction(Action.DISPLAY) }
                        d.locationOn?.let { DrainerRow(Icons.Filled.LocationOn, "Ubicación", onOff(it), bad = false) { onAction(Action.LOCATION) } }
                        d.bluetoothOn?.let { DrainerRow(Icons.Filled.Bluetooth, "Bluetooth", onOff(it), bad = false) { onAction(Action.BLUETOOTH) } }
                        d.nfcOn?.let { DrainerRow(Icons.Filled.Nfc, "NFC", onOff(it), bad = false) { onAction(Action.NFC) } }
                        DrainerRow(Icons.Filled.EnergySavingsLeaf, "Ahorro de energía", onOff(d.powerSave), bad = false) { Nav.batterySettings(ctx) }
                    }
                }
            }
            item {
                SectionCard(title = "Consejos para tu S25 Ultra", icon = Icons.Filled.Lightbulb) {
                    listOf(
                        "Protección de batería › 'Adaptable' o 'Máxima' (carga al 80 %) alarga mucho su vida.",
                        "Usa 'Apps en suspensión profunda' para las apps que no necesitas que notifiquen.",
                        "Desactiva 'Wi-Fi siempre buscando' y 'Bluetooth buscando' en Ubicación › Servicios.",
                        "Evita cargar con fundas gruesas o jugando: el calor es el peor enemigo de la batería.",
                        "Carga súper rápida 2.0 calienta más; desactívala si cargas de noche.",
                    ).forEach { Text("• $it", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(vertical = 3.dp)) }
                }
            }
        }
    }
}

private fun onOff(b: Boolean) = if (b) "Activado" else "Desactivado"

private fun chargeHint(w: Float) = when {
    w >= 35 -> "Carga súper rápida 2.0 (45 W)."
    w >= 18 -> "Carga súper rápida (25 W)."
    w >= 9 -> "Carga rápida."
    w > 0 -> "Carga lenta: revisa el cable o el cargador."
    else -> "Cargando…"
}

private fun estimate(ctx: android.content.Context, b: BatterySnapshot): String? {
    if (b.charging) {
        val bm = ctx.getSystemService(BatteryManager::class.java)
        val ms = bm.computeChargeTimeRemaining()
        return if (ms > 0) "Carga completa en ~${Fmt.duration(ms)}" else null
    }
    val mah = b.chargeCounterMah ?: return null
    val ma = b.currentMa?.let { kotlin.math.abs(it) } ?: return null
    if (ma < 50) return null
    val h = mah.toDouble() / ma
    return "Al ritmo actual te queda ~${Fmt.duration((h * 3_600_000).toLong())}"
}

@Composable
private fun DrainRow(d: DrainEntry, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        AppIcon(d.app.pkg)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            MetricBar(d.app.label, "${String.format(Locale.US, "%.1f", d.sharePercent)}%", d.sharePercent / 100f,
                when { d.sharePercent > 25 -> Bad; d.sharePercent > 10 -> Warn; else -> Good })
            Text(d.reason, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        IconButton(onClick = onClick) { Icon(Icons.Filled.ChevronRight, "Detalles") }
    }
}

@Composable
private fun DrainerRow(icon: ImageVector, title: String, value: String, bad: Boolean, hint: String? = null, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = if (bad) Warn else MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium)
            if (hint != null) Text(hint, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(value, fontWeight = FontWeight.SemiBold, color = if (bad) Warn else MaterialTheme.colorScheme.onSurface)
        IconButton(onClick = onClick) { Icon(Icons.Filled.ChevronRight, null) }
    }
}
