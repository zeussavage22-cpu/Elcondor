package com.elcondor.optimizer.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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

    LazyColumn(modifier, contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = BottomSpace)) {
        item { Header("Red", buildString {
            append(conn.type)
            conn.wifiBand?.let { append(" · $it") }
            conn.wifiStandard?.let { append(" · $it") }
        }) }

        // ---------- Prueba de velocidad ----------
        item {
            Block {
                val s = speed
                Row {
                    SpeedValue(Modifier.weight(1f), "Descarga", s?.downMbps, testing)
                    SpeedValue(Modifier.weight(1f), "Subida", s?.upMbps, testing)
                }
                if (s != null && s.error == null) {
                    Spacer(Modifier.height(14.dp))
                    Row {
                        Mini(Modifier.weight(1f), "Ping", s.latencyMs?.let { "$it ms" },
                            when { (s.latencyMs ?: 999) < 40 -> Good; (s.latencyMs ?: 999) < 100 -> Warn; else -> Bad })
                        Mini(Modifier.weight(1f), "Jitter", s.jitterMs?.let { "$it ms" })
                        Mini(Modifier.weight(1f), "DNS", s.dnsMs?.let { "$it ms" })
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(verdict(s, conn), style = MaterialTheme.typography.bodySmall, color = muted())
                }
                s?.error?.let { Text("No se pudo medir: $it", color = Bad, style = MaterialTheme.typography.bodySmall) }
                Spacer(Modifier.height(18.dp))
                if (testing) {
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                    Text(testStep, style = MaterialTheme.typography.labelMedium, color = muted(), modifier = Modifier.padding(top = 8.dp))
                } else PillButton(if (s == null) "Medir velocidad" else "Repetir", Modifier.fillMaxWidth(), icon = Icons.Outlined.Speed) {
                    testing = true
                    scope.launch { speed = withContext(Dispatchers.IO) { NetworkRepo.speedTest { testStep = it } }; testing = false }
                }
                if (conn.metered && !testing) Text("Usa unos 60 MB de datos.", style = MaterialTheme.typography.labelMedium, color = muted(),
                    modifier = Modifier.padding(top = 8.dp).align(Alignment.CenterHorizontally))
            }
        }

        // ---------- En vivo ----------
        item {
            SectionLabel("Tráfico en vivo", Modifier.padding(top = 12.dp))
            Block(padding = 18.dp) {
                Row {
                    Column(Modifier.weight(1f)) {
                        Text("${Fmt.bytes(downRate)}/s", style = MaterialTheme.typography.titleLarge, color = Info)
                        Text("Bajando", style = MaterialTheme.typography.labelMedium, color = muted())
                    }
                    Column(Modifier.weight(1f)) {
                        Text("${Fmt.bytes(upRate)}/s", style = MaterialTheme.typography.titleLarge, color = Good)
                        Text("Subiendo", style = MaterialTheme.typography.labelMedium, color = muted())
                    }
                }
                Spacer(Modifier.height(12.dp))
                Sparkline(downSamples.toList(), Modifier.fillMaxWidth().height(56.dp), Info)
                if (downRate > 1_000_000) Text("Hay una descarga activa: puede ser una actualización o una app sincronizando.",
                    style = MaterialTheme.typography.bodySmall, color = Warn, modifier = Modifier.padding(top = 8.dp))
            }
        }

        // ---------- Conexión ----------
        item {
            SectionLabel("Conexión")
            Block(padding = 18.dp) {
                KeyValue("Internet", if (conn.validated) "Funcionando" else "Sin acceso", if (conn.validated) Good else Bad)
                conn.wifiRssi?.let { KeyValue("Señal", "${conn.signalQuality} · $it dBm", when { it >= -67 -> Good; it >= -75 -> Warn; else -> Bad }) }
                conn.wifiLinkMbps?.let { KeyValue("Enlace Wi-Fi", "$it Mbps") }
                if (conn.vpn) KeyValue("VPN", "Activa", Warn)
                if (conn.dataSaver) KeyValue("Ahorro de datos", "Activado", Warn)
                KeyValue("Red medida", if (conn.metered) "Sí" else "No")
            }
        }

        // ---------- Diagnóstico ----------
        item {
            SectionLabel("¿Por qué va lento?")
            Block(padding = 14.dp) {
                diagnosis(conn, rows.orEmpty(), speed).forEach { t -> TipRow(t, if (t.action != Action.NONE) ({ onAction(t.action) }) else null) }
            }
        }

        // ---------- Por app ----------
        item {
            SectionLabel("Datos por app", trailing = { TextButton(onClick = { Nav.dataUsage(ctx) }) { Text("Ajustes") } })
            Block(padding = 16.dp) {
                if (!usage) Locked("Necesito el permiso de datos de uso para ver qué apps usan tu internet.") { onAction(Action.USAGE_PERM) }
                else {
                    Segmented(listOf(1 to "1 h", 24 to "24 h", 168 to "7 d", 720 to "30 d"), hours) { hours = it }
                    Spacer(Modifier.height(12.dp))
                    val list = rows
                    when {
                        list == null -> Loading()
                        list.isEmpty() -> Text("Sin consumo registrado.", color = muted())
                        else -> {
                            Row {
                                Mini(Modifier.weight(1f), "Wi-Fi", Fmt.bytes(list.sumOf { it.net.wifiFg + it.net.wifiBg }))
                                Mini(Modifier.weight(1f), "Móvil", Fmt.bytes(list.sumOf { it.net.mobileBytes }))
                                Mini(Modifier.weight(1f), "2º plano", Fmt.bytes(list.sumOf { it.net.bgBytes }))
                            }
                            Spacer(Modifier.height(12.dp))
                            val max = list.first().net.totalBytes.coerceAtLeast(1)
                            list.take(20).forEach { r ->
                                val heavyBg = r.net.bgBytes > r.net.totalBytes / 2 && r.net.bgBytes > 100L * 1_048_576
                                AppBar(r.pkg, r.label, Fmt.bytes(r.net.totalBytes), r.net.totalBytes.toFloat() / max,
                                    if (heavyBg) Warn else MaterialTheme.colorScheme.primary,
                                    "Wi-Fi ${Fmt.bytes(r.net.wifiFg + r.net.wifiBg)} · Móvil ${Fmt.bytes(r.net.mobileBytes)} · 2º plano ${Fmt.bytes(r.net.bgBytes)}",
                                    onClick = r.pkg?.takeIf { it != "android" }?.let { p -> { Nav.appDetails(ctx, p) } })
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SpeedValue(m: Modifier, label: String, mbps: Double?, testing: Boolean) {
    val inf = rememberInfiniteTransition(label = "t")
    val alpha by inf.animateFloat(0.3f, 1f, infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "a")
    Column(m) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = muted())
        Row(verticalAlignment = Alignment.Bottom) {
            Text(mbps?.let { String.format(Locale.US, if (it >= 100) "%.0f" else "%.1f", it) } ?: "—",
                style = MaterialTheme.typography.displayMedium,
                color = (when { mbps == null -> MaterialTheme.colorScheme.onSurface; mbps >= 50 -> Good; mbps >= 10 -> Warn; else -> Bad })
                    .copy(alpha = if (testing) alpha else 1f))
            Text(" Mbps", style = MaterialTheme.typography.labelMedium, color = muted(), modifier = Modifier.padding(bottom = 10.dp))
        }
    }
}

@Composable
private fun Mini(m: Modifier, label: String, value: String?, color: Color = Color.Unspecified) {
    Column(m) {
        Text(value ?: "—", style = MaterialTheme.typography.titleMedium, color = color)
        Text(label, style = MaterialTheme.typography.labelMedium, color = muted())
    }
}

private fun verdict(s: SpeedResult, c: ConnectionInfo): String {
    val d = s.downMbps ?: return ""
    val base = when {
        d >= 100 -> "Excelente para 4K, juegos online y videollamadas."
        d >= 25 -> "Buena para streaming HD y videollamadas."
        d >= 8 -> "Aceptable; el video en alta calidad puede cortarse."
        else -> "Lenta; navegar y ver video será difícil."
    }
    val link = c.wifiLinkMbps
    return if (link != null && d < link * 0.25 && d < 50)
        "$base Tu Wi-Fi da $link Mbps pero llegan ${String.format(Locale.US, "%.0f", d)}: el límite está en tu proveedor, el router u otros dispositivos."
    else base
}

private fun diagnosis(c: ConnectionInfo, rows: List<NetUsageRow>, s: SpeedResult?): List<Tip> {
    val t = mutableListOf<Tip>()
    if (!c.validated && c.type != "Sin conexión") t += Tip(Severity.CRITICAL, "Conectado sin internet", "Reinicia el router o apaga y enciende el Wi-Fi.", Action.WIFI, "Wi-Fi")
    c.wifiRssi?.let { if (it < -75) t += Tip(Severity.WARNING, "Señal débil", "Acércate al router o usa un repetidor.", Action.WIFI, "Wi-Fi") }
    if (c.wifiBand == "2.4 GHz") t += Tip(Severity.TIP, "Estás en 2.4 GHz", "Conéctate a la red de 5 o 6 GHz de tu router: es mucho más rápida.", Action.WIFI, "Wi-Fi")
    if (c.vpn) t += Tip(Severity.TIP, "VPN activa", "Desactívala para ver si mejora la velocidad.")
    if (c.dataSaver) t += Tip(Severity.TIP, "Ahorro de datos activado", "Puede ralentizar algunas apps.", Action.DATA_USAGE, "Ajustes")
    rows.filter { it.net.bgBytes > 300L * 1_048_576 }.take(3).forEach {
        t += Tip(Severity.WARNING, "${it.label} usa mucho en 2º plano", "${Fmt.bytes(it.net.bgBytes)} sin que la abrieras.", Action.BG_LIMITS, "Limitar")
    }
    s?.latencyMs?.let { if (it > 100) t += Tip(Severity.WARNING, "Ping alto ($it ms)", "Juegos y videollamadas irán con retraso.") }
    s?.dnsMs?.let { if (it > 150) t += Tip(Severity.TIP, "DNS lento", "Usa DNS privado: one.one.one.one", Action.NONE) }
    if (t.isEmpty()) t += Tip(Severity.OK, "Todo en orden", "Si notas lentitud, mide la velocidad arriba.")
    return t
}
