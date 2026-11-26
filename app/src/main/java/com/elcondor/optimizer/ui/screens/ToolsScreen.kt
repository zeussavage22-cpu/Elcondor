package com.elcondor.optimizer.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToolsScreen(modifier: Modifier, refresh: Int, onBack: () -> Unit) {
    val ctx = LocalContext.current
    var id by remember { mutableStateOf(DeviceInfo.identity(ctx)) }
    var freqs by remember { mutableStateOf(DeviceInfo.cpuFreqsMhz()) }
    var mem by remember { mutableStateOf(DeviceInfo.memory(ctx)) }
    LaunchedEffect(refresh) {
        while (isActive) {
            id = DeviceInfo.identity(ctx); freqs = DeviceInfo.cpuFreqsMhz(); mem = DeviceInfo.memory(ctx)
            delay(2000)
        }
    }

    Column(modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Herramientas", fontWeight = FontWeight.Bold) },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Atrás") } },
        )
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                SectionCard(title = "Mi dispositivo", icon = Icons.Filled.PhoneAndroid) {
                    KeyValue("Modelo", id.model)
                    KeyValue("Android", id.android)
                    id.oneUi?.let { KeyValue("One UI", it) }
                    id.soc?.let { KeyValue("Procesador", it) }
                    KeyValue("Núcleos", "${id.cores}")
                    KeyValue("RAM", "${Fmt.bytes(mem.usedBytes)} / ${Fmt.bytes(mem.totalBytes)}")
                    KeyValue("Encendido desde", Fmt.duration(id.uptimeMs))
                    KeyValue("Estado térmico", DeviceInfo.thermalLabel(id.thermalStatus))
                    id.thermalHeadroom?.let { KeyValue("Margen térmico", "${((1 - it).coerceIn(0f, 1f) * 100).toInt()} % antes de limitar") }
                }
            }
            if (freqs.any { it != null }) item {
                SectionCard(title = "CPU en vivo", icon = Icons.Filled.Memory) {
                    freqs.forEachIndexed { i, f ->
                        if (f != null) MetricBar("Núcleo $i", "$f MHz", f / 4500f, MaterialTheme.colorScheme.primary)
                    }
                }
            }
            item {
                SectionCard(title = "Atajos Samsung", icon = Icons.Filled.Apps) {
                    ToolRow(Icons.Filled.HealthAndSafety, "Device Care", "Optimización oficial de Samsung") { Nav.deviceCare(ctx) }
                    ToolRow(Icons.Filled.BatteryChargingFull, "Batería y protección", "Protección de batería, ahorro de energía") { Nav.batterySettings(ctx) }
                    ToolRow(Icons.Filled.Bedtime, "Límites de uso en 2º plano", "Apps en suspensión / suspensión profunda") { Nav.backgroundLimits(ctx) }
                    ToolRow(Icons.Filled.DataUsage, "Uso de datos", "Límites y ahorro de datos") { Nav.dataUsage(ctx) }
                    ToolRow(Icons.Filled.Dns, "DNS privado", "Conexiones › Más ajustes › DNS privado") { Nav.privateDns(ctx) }
                    ToolRow(Icons.Filled.DisplaySettings, "Pantalla", "Fluidez, brillo, modo oscuro") { Nav.display(ctx) }
                    ToolRow(Icons.Filled.Storage, "Almacenamiento", "Análisis del sistema") { Nav.storage(ctx) }
                    ToolRow(Icons.Filled.SystemUpdate, "Actualización de software", "Instala parches de seguridad y mejoras") { Nav.softwareUpdate(ctx) }
                    ToolRow(Icons.Filled.Security, "Seguridad y privacidad", "Analiza permisos y amenazas") { Nav.security(ctx) }
                    ToolRow(Icons.Filled.DeveloperMode, "Opciones de desarrollador", "Escalas de animación a 0.5x = más fluido") { Nav.developer(ctx) }
                }
            }
            item {
                SectionCard(title = "Trucos de velocidad para S25 Ultra", icon = Icons.Filled.Bolt) {
                    listOf(
                        "Opciones de desarrollador › Escala de animación de ventana/transición/duración: 0.5x. El teléfono se siente el doble de rápido.",
                        "Device Care › Optimización automática: actívala con reinicio automático semanal.",
                        "Device Care › Rendimiento › 'Ligero' si priorizas batería sobre potencia en juegos.",
                        "Desactiva 'Galaxy AI' en funciones que no uses (Ajustes › Funciones avanzadas › Inteligencia avanzada).",
                        "Agrega el botón 'Optimizar' de Cóndor Care a tus Ajustes rápidos (desliza la cortina › lápiz › arrastra el botón).",
                    ).forEach { Text("• $it", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(vertical = 3.dp)) }
                }
            }
            item {
                SectionCard(title = "Permisos de Cóndor Care", icon = Icons.Filled.Shield) {
                    ToolRow(Icons.Filled.QueryStats, "Acceso a datos de uso", if (Perms.hasUsageAccess(ctx)) "Concedido ✓" else "Pendiente — necesario para batería y red") { Perms.openUsageAccess(ctx) }
                    ToolRow(Icons.Filled.Folder, "Acceso a todos los archivos", if (Perms.hasAllFiles()) "Concedido ✓" else "Pendiente — necesario para la limpieza") { Perms.openAllFiles(ctx) }
                    Text("Cóndor Care funciona 100 % en tu teléfono. No envía tus datos a ningún servidor (solo la prueba de velocidad contacta Cloudflare).",
                        style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun ToolRow(icon: ImageVector, title: String, sub: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(sub, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(Icons.Filled.ChevronRight, null)
    }
}
