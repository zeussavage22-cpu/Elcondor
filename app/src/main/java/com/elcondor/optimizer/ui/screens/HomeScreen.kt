package com.elcondor.optimizer.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.elcondor.optimizer.data.*
import com.elcondor.optimizer.ui.components.*
import com.elcondor.optimizer.ui.theme.Bad
import com.elcondor.optimizer.ui.theme.Good
import com.elcondor.optimizer.ui.theme.Warn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class BoostReport(val ramFreed: Long, val filesDeleted: Int, val bytesDeleted: Long, val ranJunk: Boolean)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(modifier: Modifier, refresh: Int, onAction: (Action) -> Unit, onTools: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var analysis by remember { mutableStateOf<Analysis?>(null) }
    var boosting by remember { mutableStateOf(false) }
    var boostStep by remember { mutableStateOf("") }
    var report by remember { mutableStateOf<BoostReport?>(null) }
    var localRefresh by remember { mutableIntStateOf(0) }

    LaunchedEffect(refresh, localRefresh) {
        analysis = withContext(Dispatchers.IO) { Advisor.analyze(ctx) }
    }

    Column(modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Column {
                    Text("Cóndor Care", fontWeight = FontWeight.Bold)
                    analysis?.let { Text(it.identity.model, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
            },
            actions = {
                IconButton(onClick = { analysis = null; localRefresh++ }) { Icon(Icons.Filled.Refresh, "Reanalizar") }
                IconButton(onClick = onTools) { Icon(Icons.Filled.Build, "Herramientas") }
            },
        )
        val a = analysis
        if (a == null) { Loading("Analizando todo tu teléfono…"); return@Column }

        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                SectionCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ScoreRing(a.score)
                        Spacer(Modifier.width(16.dp))
                        Column(Modifier.weight(1f)) {
                            Text(when {
                                a.score >= 85 -> "Excelente estado"
                                a.score >= 70 -> "Buen estado"
                                a.score >= 50 -> "Necesita atención"
                                else -> "Requiere optimización"
                            }, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            val issues = a.tips.count { it.severity == Severity.CRITICAL || it.severity == Severity.WARNING }
                            Text(if (issues == 0) "Sin problemas importantes" else "$issues problema(s) detectado(s)",
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(12.dp))
                            Button(
                                enabled = !boosting,
                                onClick = {
                                    boosting = true
                                    scope.launch {
                                        boostStep = "Cerrando procesos en segundo plano…"
                                        val ram = withContext(Dispatchers.IO) { AppsRepo.boost(ctx) }
                                        var n = 0; var b = 0L; var ran = false
                                        if (Perms.hasAllFiles()) {
                                            ran = true
                                            boostStep = "Buscando basura segura…"
                                            val r = withContext(Dispatchers.IO) { JunkScanner.scan() }
                                            boostStep = "Eliminando basura…"
                                            val safe = listOf(JunkType.TEMP, JunkType.THUMBS, JunkType.EMPTY).flatMap { r.items[it].orEmpty() }
                                            val res = withContext(Dispatchers.IO) { JunkScanner.delete(safe) }
                                            n = res.first; b = res.second
                                        }
                                        report = BoostReport(ram, n, b, ran)
                                        boosting = false
                                        localRefresh++
                                    }
                                },
                            ) {
                                Icon(Icons.Filled.RocketLaunch, null, Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(if (boosting) "Optimizando…" else "Optimizar ahora")
                            }
                            if (boosting) Text(boostStep, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatTile(Modifier.weight(1f), Icons.Filled.BatteryChargingFull, "Batería", "${a.battery.level}%",
                        a.battery.status, tint = if (a.battery.level > 20) Good else Bad)
                    StatTile(Modifier.weight(1f), Icons.Filled.Thermostat, "Temperatura", "${a.battery.temperatureC} °C",
                        DeviceInfo.thermalLabel(a.identity.thermalStatus),
                        tint = when { a.battery.temperatureC >= 42 -> Bad; a.battery.temperatureC >= 38 -> Warn; else -> Good })
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatTile(Modifier.weight(1f), Icons.Filled.Memory, "RAM en uso", Fmt.pct(a.memory.usedFraction),
                        "${Fmt.bytes(a.memory.availBytes)} libre", tint = levelColor(a.memory.usedFraction))
                    StatTile(Modifier.weight(1f), Icons.Filled.Storage, "Almacenamiento", Fmt.pct(a.storage.usedFraction),
                        "${Fmt.bytes(a.storage.freeBytes)} libre", tint = levelColor(a.storage.usedFraction))
                }
            }
            if (a.usageAccess) item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatTile(Modifier.weight(1f), Icons.Filled.Visibility, "Pantalla hoy", Fmt.duration(a.screenOnMs))
                    StatTile(Modifier.weight(1f), Icons.Filled.NetworkCheck, "Conexión", a.connection.type,
                        a.connection.signalQuality?.let { "Señal $it" } ?: if (a.connection.validated) "Con internet" else "Sin internet")
                }
            }
            item {
                SectionCard(title = "Asistente inteligente", icon = Icons.Filled.AutoAwesome) {
                    a.tips.forEach { t ->
                        TipCard(t, if (t.action != Action.NONE) ({ onAction(t.action) }) else null)
                    }
                }
            }
            if (a.topDrain.isNotEmpty()) item {
                SectionCard(title = "Lo que más gasta batería (24 h)", icon = Icons.Filled.BatteryAlert,
                    trailing = { TextButton(onClick = { onAction(Action.BATTERY) }) { Text("Ver todo") } }) {
                    a.topDrain.take(3).forEach { d ->
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
                            AppIcon(d.app.pkg, 32.dp); Spacer(Modifier.width(10.dp))
                            Box(Modifier.weight(1f)) { MetricBar(d.app.label, "${d.sharePercent.toInt()}%", d.sharePercent / 100f, Warn) }
                        }
                    }
                }
            }
            if (a.topData.isNotEmpty()) item {
                SectionCard(title = "Lo que más usa internet (24 h)", icon = Icons.Filled.DataUsage,
                    trailing = { TextButton(onClick = { onAction(Action.NETWORK) }) { Text("Ver todo") } }) {
                    val max = a.topData.first().net.totalBytes.coerceAtLeast(1)
                    a.topData.take(3).forEach { r ->
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
                            AppIcon(r.pkg, 32.dp); Spacer(Modifier.width(10.dp))
                            Box(Modifier.weight(1f)) { MetricBar(r.label, Fmt.bytes(r.net.totalBytes), r.net.totalBytes.toFloat() / max, MaterialTheme.colorScheme.primary) }
                        }
                    }
                }
            }
        }
    }

    report?.let { r ->
        AlertDialog(
            onDismissRequest = { report = null },
            confirmButton = { TextButton(onClick = { report = null }) { Text("Listo") } },
            icon = { Icon(Icons.Filled.CheckCircle, null, tint = Good) },
            title = { Text("Optimización completada") },
            text = {
                Column {
                    KeyValue("RAM liberada", Fmt.bytes(r.ramFreed))
                    if (r.ranJunk) {
                        KeyValue("Archivos basura borrados", "${r.filesDeleted}")
                        KeyValue("Espacio recuperado", Fmt.bytes(r.bytesDeleted))
                    } else Text("Concede acceso a archivos para limpiar también la basura.", style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(8.dp))
                    Text("Para limpiar la caché de todas las apps usa la pestaña Limpiar.", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
        )
    }
}
