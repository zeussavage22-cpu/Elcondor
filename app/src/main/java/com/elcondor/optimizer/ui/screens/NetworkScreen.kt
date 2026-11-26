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
import com.elcondor.optimizer.ui.theme.Info
import com.elcondor.optimizer.ui.theme.Warn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NetworkScreen(modifier: Modifier, refresh: Int, onAction: (Action) -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var conn by remember { mutableStateOf(NetworkRepo.connection(ctx)) }
    var downRate by remember { mutableLongStateOf(0) }
    var upRate by remember { mutableLongStateOf(0) }
    val downSamples = remember { mutableStateListOf<Float>() }
    var hours by remember { mutableIntStateOf(24) }
    var rows by remember { mutableStateOf<List<NetUsageRow>?>(null) }
    var speed by remember { mutableStateOf<SpeedResult?>(null) }
    var testing by remember { mutableStateOf(false) }
    var testStep by remember { mutableStateOf("") }
    val usage = remember(refresh) { Perms.hasUsageAccess(ctx) }

    LaunchedEffect(Unit) {
        var (rx, tx) = NetworkRepo.totalBytes()
        var tick = 0
        while (isActive) {
            delay(1000)
            val (nrx, ntx) = NetworkRepo.totalBytes()
            downRate = (nrx - rx).coerceAtLeast(0); upRate = (ntx - tx).coerceAtLeast(0)
            rx = nrx; tx = ntx
            downSamples += downRate / 1024f; if (downSamples.size > 60) downSamples.removeAt(0)
            if (++tick % 5 == 0) conn = NetworkRepo.connection(ctx)
        }
    }
    LaunchedEffect(refresh, hours) {
        rows = null
        rows = withContext(Dispatchers.IO) { if (usage) NetworkRepo.usageRows(ctx, hours) else emptyList() }
    }

    Column(modifier.fillMaxSize()) {
        TopAppBar(title = { Text("Internet y red", fontWeight = FontWeight.Bold) })
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                SectionCard(title = "Conexión actual", icon = if (conn.type == "Wi-Fi") Icons.Filled.Wifi else Icons.Filled.SignalCellularAlt) {
                    KeyValue("Tipo", conn.type)
                    KeyValue("Internet", if (conn.validated) "Funcionando" else "Sin acceso", if (conn.validated) Good else Bad)
                    conn.wifiRssi?.let { KeyValue("Señal", "$it dBm · ${conn.signalQuality}",
                        when { it >= -67 -> Good; it >= -75 -> Warn; else -> Bad }) }
                    conn.wifiBand?.let { KeyValue("Banda", it, if (it == "2.4 GHz") Warn else Good) }
                    conn.wifiStandard?.let { KeyValue("Estándar", it) }
                    conn.wifiLinkMbps?.let { KeyValue("Velocidad de enlace", "$it Mbps") }
                    conn.downKbps?.let { KeyValue("Ancho de banda estimado", "${it / 1000} ↓ / ${(conn.upKbps ?: 0) / 1000} ↑ Mbps") }
                    KeyValue("Red medida (datos)", if (conn.metered) "Sí" else "No")
                    KeyValue("VPN", if (conn.vpn) "Activa" else "No", if (conn.vpn) Warn else Color0())
                    KeyValue("Ahorro de datos", if (conn.dataSaver) "Activado" else "Desactivado")
                }
            }
            item {
                SectionCard(title = "Tráfico en vivo", icon = Icons.Filled.SwapVert) {
                    Row {
                        Column(Modifier.weight(1f)) {
                            Text("↓ ${Fmt.bytes(downRate)}/s", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Info)
                            Text("Descarga", style = MaterialTheme.typography.labelMedium)
                        }
                        Column(Modifier.weight(1f)) {
                            Text("↑ ${Fmt.bytes(upRate)}/s", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Good)
                            Text("Subida", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Sparkline(downSamples.toList(), Modifier.fillMaxWidth().height(60.dp), Info)
                    if (downRate > 1_000_000) Text("Hay una descarga activa: si notas lentitud, puede ser una actualización o una app sincronizando.",
                        style = MaterialTheme.typography.bodySmall, color = Warn)
                }
            }
            item {
                SectionCard(title = "Prueba de velocidad", icon = Icons.Filled.Speed) {
                    speed?.let { s ->
                        if (s.error != null) Text("Error: ${s.error}", color = Bad)
                        else {
                            Row {
                                SpeedValue(Modifier.weight(1f), "Descarga", s.downMbps)
                                SpeedValue(Modifier.weight(1f), "Subida", s.upMbps)
                            }
                            Spacer(Modifier.height(8.dp))
                            KeyValue("Latencia (ping)", s.latencyMs?.let { "$it ms" } ?: "—",
                                when { (s.latencyMs ?: 999) < 40 -> Good; (s.latencyMs ?: 999) < 100 -> Warn; else -> Bad })
                            KeyValue("Variación (jitter)", s.jitterMs?.let { "$it ms" } ?: "—")
                            KeyValue("Resolución DNS", s.dnsMs?.let { "$it ms" } ?: "—")
                            Spacer(Modifier.height(6.dp))
                            Text(verdict(s, conn), style = MaterialTheme.typography.bodyMedium)
                        }
                        Spacer(Modifier.height(10.dp))
                    }
                    if (testing) { LinearProgressIndicator(Modifier.fillMaxWidth()); Text(testStep, style = MaterialTheme.typography.labelSmall) }
                    else Button(onClick = {
                        testing = true
                        scope.launch {
                            speed = withContext(Dispatchers.IO) { NetworkRepo.speedTest { testStep = it } }
                            testing = false
                        }
                    }) { Text(if (speed == null) "Iniciar prueba" else "Repetir prueba") }
                    if (conn.metered) Text("Usa ~60 MB de datos móviles.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            item {
                SectionCard(title = "¿Por qué va lento?", icon = Icons.Filled.Troubleshoot) {
                    diagnosis(conn, rows.orEmpty(), speed).forEach { t -> TipCard(t, if (t.action != Action.NONE) ({ onAction(t.action) }) else null) }
                }
            }
            item {
                SectionCard(title = "Consumo de datos por app", icon = Icons.Filled.DataUsage) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(1 to "1 h", 24 to "24 h", 168 to "7 días", 720 to "30 días").forEach { (h, l) ->
                            FilterChip(selected = hours == h, onClick = { hours = h }, label = { Text(l) })
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    when {
                        !usage -> PermissionGate("Permiso necesario", "Activa 'Acceso a datos de uso' para ver qué apps usan tu internet.", "Conceder") { Perms.openUsageAccess(ctx) }
                        rows == null -> Loading()
                        rows!!.isEmpty() -> Text("Sin consumo registrado.")
                        else -> {
                            val list = rows!!
                            val wifi = list.sumOf { it.net.wifiFg + it.net.wifiBg }
                            val mobile = list.sumOf { it.net.mobileBytes }
                            KeyValue("Total Wi-Fi", Fmt.bytes(wifi)); KeyValue("Total datos móviles", Fmt.bytes(mobile))
                            Spacer(Modifier.height(8.dp))
                            val max = list.first().net.totalBytes.coerceAtLeast(1)
                            list.take(20).forEach { r ->
                                Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                                    AppIcon(r.pkg); Spacer(Modifier.width(12.dp))
                                    Column(Modifier.weight(1f)) {
                                        MetricBar(r.label, Fmt.bytes(r.net.totalBytes), r.net.totalBytes.toFloat() / max, MaterialTheme.colorScheme.primary)
                                        Text("Wi-Fi ${Fmt.bytes(r.net.wifiFg + r.net.wifiBg)} · Móvil ${Fmt.bytes(r.net.mobileBytes)} · 2º plano ${Fmt.bytes(r.net.bgBytes)}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (r.net.bgBytes > r.net.totalBytes / 2 && r.net.bgBytes > 100L * 1_048_576) Warn else MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    if (r.pkg != null && r.pkg != "android") IconButton(onClick = { Nav.appDetails(ctx, r.pkg) }) { Icon(Icons.Filled.ChevronRight, null) }
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(onClick = { Nav.dataUsage(ctx) }) { Text("Ajustes de uso de datos") }
                }
            }
        }
    }
}

@Composable private fun Color0() = MaterialTheme.colorScheme.onSurface

@Composable
private fun SpeedValue(m: Modifier, label: String, mbps: Double?) {
    Column(m) {
        Text(mbps?.let { String.format(Locale.US, "%.1f", it) } ?: "—", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold,
            color = when { (mbps ?: 0.0) >= 50 -> Good; (mbps ?: 0.0) >= 10 -> Warn; else -> Bad })
        Text("$label · Mbps", style = MaterialTheme.typography.labelMedium)
    }
}

private fun verdict(s: SpeedResult, c: ConnectionInfo): String {
    val d = s.downMbps ?: return ""
    val base = when {
        d >= 100 -> "Excelente: 4K, juegos online y videollamadas sin problema."
        d >= 25 -> "Buena: suficiente para streaming HD y videollamadas."
        d >= 8 -> "Aceptable: puede haber cortes en video de alta calidad."
        else -> "Lenta: navegar y ver video será complicado."
    }
    val link = c.wifiLinkMbps
    return if (link != null && d < link * 0.25 && d < 50) "$base La velocidad real está muy por debajo del enlace Wi-Fi ($link Mbps): el cuello de botella es tu proveedor/router o hay otros dispositivos usando la red." else base
}

private fun diagnosis(c: ConnectionInfo, rows: List<NetUsageRow>, s: SpeedResult?): List<Tip> {
    val t = mutableListOf<Tip>()
    if (!c.validated && c.type != "Sin conexión") t += Tip(Severity.CRITICAL, "Conectado pero sin internet", "Reinicia el router o desactiva/activa el Wi-Fi.", Action.WIFI, "Wi-Fi")
    c.wifiRssi?.let { if (it < -75) t += Tip(Severity.WARNING, "Señal débil ($it dBm)", "Acércate al router o usa un repetidor/mesh.", Action.WIFI, "Wi-Fi") }
    if (c.wifiBand == "2.4 GHz") t += Tip(Severity.TIP, "Banda 2.4 GHz", "Es más lenta y saturada. Conéctate a la red 5 GHz o 6 GHz de tu router.", Action.WIFI, "Wi-Fi")
    if (c.vpn) t += Tip(Severity.TIP, "VPN activa", "Desactívala para comprobar si mejora la velocidad.")
    if (c.dataSaver) t += Tip(Severity.TIP, "Ahorro de datos activado", "Limita apps en segundo plano y puede ralentizar algunas.", Action.DATA_USAGE, "Ajustes")
    rows.filter { it.net.bgBytes > 300L * 1_048_576 }.take(3).forEach {
        t += Tip(Severity.WARNING, "${it.label} usa mucho en segundo plano", "${Fmt.bytes(it.net.bgBytes)} sin que la abrieras. Restríngela si no la necesitas.", Action.BG_LIMITS, "Limitar")
    }
    s?.latencyMs?.let { if (it > 100) t += Tip(Severity.WARNING, "Latencia alta ($it ms)", "Los juegos y videollamadas irán con retraso. Prueba DNS privado 'one.one.one.one' o 'dns.google'.") }
    s?.dnsMs?.let { if (it > 150) t += Tip(Severity.TIP, "DNS lento ($it ms)", "Configura DNS privado: Ajustes › Conexiones › Más ajustes › DNS privado › one.one.one.one") }
    if (t.isEmpty()) t += Tip(Severity.OK, "No encontré problemas", "Si sigue lento, haz la prueba de velocidad para medirlo.")
    return t
}
