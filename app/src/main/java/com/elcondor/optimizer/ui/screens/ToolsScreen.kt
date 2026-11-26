package com.elcondor.optimizer.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.elcondor.optimizer.data.*
import com.elcondor.optimizer.ui.components.*
import com.elcondor.optimizer.ui.theme.Good
import com.elcondor.optimizer.ui.theme.Warn
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@Composable
fun ToolsScreen(modifier: Modifier, refresh: Int, onBack: () -> Unit, onPerms: () -> Unit) {
    val ctx = LocalContext.current
    var id by remember { mutableStateOf(DeviceInfo.identity(ctx)) }
    var freqs by remember { mutableStateOf(DeviceInfo.cpuFreqsMhz()) }
    var mem by remember { mutableStateOf(DeviceInfo.memory(ctx)) }
    val usage = remember(refresh) { Perms.hasUsageAccess(ctx) }
    val media = remember(refresh) { Perms.hasMedia(ctx) }
    val files = remember(refresh) { Perms.hasAllFiles() }
    LaunchedEffect(refresh) {
        while (isActive) { id = DeviceInfo.identity(ctx); freqs = DeviceInfo.cpuFreqsMhz(); mem = DeviceInfo.memory(ctx); delay(2000) }
    }

    LazyColumn(modifier, contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 40.dp)) {
        item {
            Row(Modifier.statusBarsPadding().padding(top = 8.dp)) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Atrás") }
            }
            Header("Herramientas", id.model)
        }
        item {
            SectionLabel("Permisos")
            Block(padding = 12.dp) {
                val n = listOf(usage, media, files).count { it }
                NavRow(Icons.Outlined.Shield, "Configurar permisos", "$n de 3 activos", if (n == 3) "Listo" else "Revisar",
                    if (n == 3) Good else Warn, onPerms)
            }
        }
        item {
            SectionLabel("Dispositivo")
            Block(padding = 18.dp) {
                KeyValue("Android", id.android)
                id.oneUi?.let { KeyValue("One UI", it) }
                id.soc?.let { KeyValue("Procesador", it) }
                KeyValue("RAM", "${Fmt.bytes(mem.usedBytes)} / ${Fmt.bytes(mem.totalBytes)}")
                KeyValue("Encendido hace", Fmt.duration(id.uptimeMs))
                KeyValue("Temperatura interna", DeviceInfo.thermalLabel(id.thermalStatus))
                id.thermalHeadroom?.let { KeyValue("Margen térmico", "${((1 - it).coerceIn(0f, 1f) * 100).toInt()} %") }
            }
        }
        if (freqs.any { it != null }) item {
            SectionLabel("CPU en vivo")
            Block(padding = 18.dp) {
                freqs.forEachIndexed { i, f -> if (f != null) BarRow("Núcleo $i", "$f MHz", f / 4500f, MaterialTheme.colorScheme.primary) }
            }
        }
        item {
            SectionLabel("Atajos Samsung")
            Block(padding = 12.dp) {
                NavRow(Icons.Outlined.HealthAndSafety, "Device Care", "Optimización oficial") { Nav.deviceCare(ctx) }
                NavRow(Icons.Outlined.BatteryChargingFull, "Batería", "Protección y ahorro") { Nav.batterySettings(ctx) }
                NavRow(Icons.Outlined.Bedtime, "Apps en suspensión", "Límites en segundo plano") { Nav.backgroundLimits(ctx) }
                NavRow(Icons.Outlined.DataUsage, "Uso de datos", null) { Nav.dataUsage(ctx) }
                NavRow(Icons.Outlined.Dns, "DNS privado", "Más ajustes de conexión") { Nav.privateDns(ctx) }
                NavRow(Icons.Outlined.DisplaySettings, "Pantalla", null) { Nav.display(ctx) }
                NavRow(Icons.Outlined.SystemUpdate, "Actualizar software", null) { Nav.softwareUpdate(ctx) }
                NavRow(Icons.Outlined.Security, "Seguridad y privacidad", "Auto Blocker, permisos") { Nav.security(ctx) }
                NavRow(Icons.Outlined.DeveloperMode, "Opciones de desarrollador", "Animaciones 0.5x") { Nav.developer(ctx) }
            }
        }
        item {
            SectionLabel("Trucos")
            Block(padding = 18.dp) {
                listOf(
                    "Opciones de desarrollador › escalas de animación a 0.5x: se siente el doble de rápido.",
                    "Device Care › Optimización automática con reinicio semanal.",
                    "Agrega el botón Optimizar a tus Ajustes rápidos: desliza la cortina › lápiz › arrástralo.",
                ).forEach { Text(it, style = MaterialTheme.typography.bodySmall, color = muted(), modifier = Modifier.padding(vertical = 5.dp)) }
            }
        }
        item {
            Text("Cóndor Care 2.0 · Todo se procesa en tu teléfono.", style = MaterialTheme.typography.labelMedium, color = muted(),
                modifier = Modifier.padding(top = 20.dp, start = 4.dp))
        }
    }
}
