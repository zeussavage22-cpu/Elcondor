package com.elcondor.optimizer.ui.screens

import android.os.BatteryManager
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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

@Composable
fun BatteryScreen(modifier: Modifier, refresh: Int, onAction: (Action) -> Unit) {
    val ctx = LocalContext.current
    var battery by remember { mutableStateOf(DeviceInfo.battery(ctx)) }
    val samples = remember { mutableStateListOf<Float>() }
    var hours by remember { mutableIntStateOf(24) }
    var drain by remember { mutableStateOf<List<DrainEntry>?>(null) }
    var drainers by remember { mutableStateOf<SystemDrainers?>(null) }
    val usage = remember(refresh) { Perms.hasUsageAccess(ctx) }

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

    LazyColumn(modifier, contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = BottomSpace)) {
        item { Header("Batería", battery.status + if (battery.charging) " · ${battery.plugged}" else "") }
        item {
            Block {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text("${battery.level}", style = MaterialTheme.typography.displayLarge, color = if (battery.level > 20) Good else Bad)
                    Text("%", style = MaterialTheme.typography.headlineMedium, color = muted(), modifier = Modifier.padding(bottom = 12.dp, start = 2.dp))
                    Spacer(Modifier.weight(1f))
                    Column(horizontalAlignment = Alignment.End, modifier = Modifier.padding(bottom = 10.dp)) {
                        Text(battery.watts?.let { String.format(Locale.US, "%.2f W", it) } ?: "—", style = MaterialTheme.typography.titleLarge)
                        Text("${kotlin.math.abs(battery.currentMa ?: 0)} mA ahora", style = MaterialTheme.typography.labelMedium, color = muted())
                    }
                }
                Spacer(Modifier.height(12.dp))
                Sparkline(samples.toList(), Modifier.fillMaxWidth().height(64.dp), if (battery.charging) Good else MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(14.dp))
                val w = battery.watts ?: 0f
                Text(when {
                    battery.charging -> chargeHint(w)
                    w > 6 -> "Consumo muy alto: juego, cámara, GPS o brillo al máximo."
                    w > 3 -> "Consumo alto. Revisa la app que tienes abierta."
                    w > 0 -> "Consumo normal."
                    else -> "Leyendo sensor…"
                }, style = MaterialTheme.typography.bodySmall, color = muted())
                estimate(ctx, battery)?.let { Text(it, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 4.dp)) }
            }
        }
        item {
            SectionLabel("Salud", Modifier.padding(top = 12.dp))
            Block(padding = 18.dp) {
                KeyValue("Estado", battery.health, if (battery.healthGood) Good else Bad)
                KeyValue("Temperatura", "${battery.temperatureC} °C",
                    when { battery.temperatureC >= 42 -> Bad; battery.temperatureC >= 38 -> Warn; else -> Good })
                KeyValue("Voltaje", String.format(Locale.US, "%.2f V", battery.voltageMv / 1000f))
                battery.cycleCount?.let { KeyValue("Ciclos de carga", "$it") }
                battery.chargeCounterMah?.let { KeyValue("Carga restante", "$it mAh") }
            }
        }
        item {
            SectionLabel("Qué la está drenando")
            Block(padding = 16.dp) {
                if (!usage) Locked("Necesito el permiso de datos de uso para ver qué apps gastan tu batería.") { onAction(Action.USAGE_PERM) }
                else {
                    Segmented(listOf(24 to "24 h", 72 to "3 días", 168 to "7 días"), hours) { hours = it }
                    Spacer(Modifier.height(12.dp))
                    when {
                        drain == null -> Loading()
                        drain!!.isEmpty() -> Text("Aún no hay datos suficientes.", color = muted())
                        else -> drain!!.take(12).forEach { d ->
                            AppBar(d.app.pkg, d.app.label, String.format(Locale.US, "%.0f%%", d.sharePercent), d.sharePercent / 100f,
                                when { d.sharePercent > 25 -> Bad; d.sharePercent > 10 -> Warn; else -> Good }, d.reason) { Nav.appDetails(ctx, d.app.pkg) }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { Nav.batteryUsage(ctx) }, shape = CircleShape, modifier = Modifier.weight(1f)) { Text("Uso oficial") }
                        OutlinedButton(onClick = { Nav.backgroundLimits(ctx) }, shape = CircleShape, modifier = Modifier.weight(1f)) { Text("Suspender apps") }
                    }
                    Text("Estimación con tiempo en pantalla, trabajo en segundo plano y datos.",
                        style = MaterialTheme.typography.labelMedium, color = muted(), modifier = Modifier.padding(top = 10.dp))
                }
            }
        }
        drainers?.let { d ->
            item {
                SectionLabel("Ajustes que gastan")
                Block(padding = 12.dp) {
                    d.brightnessPercent?.let { NavRow(Icons.Outlined.LightMode, "Brillo", if (d.autoBrightness == true) "Adaptable" else null, "$it %",
                        if (it > 80 && d.autoBrightness != true) Warn else androidx.compose.ui.graphics.Color.Unspecified) { onAction(Action.DISPLAY) } }
                    NavRow(Icons.Outlined.Speed, "Fluidez", if (d.refreshRate > 100) "60 Hz ahorra ~10 %" else null, "${d.refreshRate.toInt()} Hz") { onAction(Action.DISPLAY) }
                    d.screenTimeoutSec?.let { NavRow(Icons.Outlined.Timer, "Apagar pantalla", null, if (it >= 60) "${it / 60} min" else "$it s",
                        if (it >= 300) Warn else androidx.compose.ui.graphics.Color.Unspecified) { onAction(Action.DISPLAY) } }
                    d.aodOn?.let { NavRow(Icons.Outlined.Visibility, "Always On Display", null, onOff(it), if (it) Warn else androidx.compose.ui.graphics.Color.Unspecified) { onAction(Action.DISPLAY) } }
                    NavRow(Icons.Outlined.DarkMode, "Modo oscuro", if (!d.darkMode) "Ahorra en pantallas AMOLED" else null, onOff(d.darkMode)) { onAction(Action.DISPLAY) }
                    d.locationOn?.let { NavRow(Icons.Outlined.LocationOn, "Ubicación", null, onOff(it)) { onAction(Action.LOCATION) } }
                    d.bluetoothOn?.let { NavRow(Icons.Outlined.Bluetooth, "Bluetooth", null, onOff(it)) { onAction(Action.BLUETOOTH) } }
                    d.nfcOn?.let { NavRow(Icons.Outlined.Nfc, "NFC", null, onOff(it)) { onAction(Action.NFC) } }
                    NavRow(Icons.Outlined.EnergySavingsLeaf, "Ahorro de energía", null, onOff(d.powerSave)) { Nav.batterySettings(ctx) }
                }
            }
        }
        item {
            SectionLabel("Consejos")
            Block(padding = 18.dp) {
                listOf(
                    "Protección de batería en 'Adaptable' o 'Máxima' (80 %) alarga su vida.",
                    "Usa 'Apps en suspensión profunda' para las apps que no necesitas.",
                    "Evita cargar con funda gruesa o jugando: el calor la desgasta.",
                ).forEach { Text(it, style = MaterialTheme.typography.bodySmall, color = muted(), modifier = Modifier.padding(vertical = 5.dp)) }
            }
        }
    }
}

private fun onOff(b: Boolean) = if (b) "Sí" else "No"

private fun chargeHint(w: Float) = when {
    w >= 35 -> "Carga súper rápida 2.0 (45 W)."
    w >= 18 -> "Carga súper rápida (25 W)."
    w >= 9 -> "Carga rápida."
    w > 0 -> "Carga lenta: revisa el cable o el cargador."
    else -> "Cargando…"
}

private fun estimate(ctx: android.content.Context, b: BatterySnapshot): String? {
    if (b.charging) {
        val ms = ctx.getSystemService(BatteryManager::class.java).computeChargeTimeRemaining()
        return if (ms > 0) "Llena en ~${Fmt.duration(ms)}" else null
    }
    val mah = b.chargeCounterMah ?: return null
    val ma = b.currentMa?.let { kotlin.math.abs(it) } ?: return null
    if (ma < 50) return null
    return "A este ritmo te queda ~${Fmt.duration((mah.toDouble() / ma * 3_600_000).toLong())}"
}
