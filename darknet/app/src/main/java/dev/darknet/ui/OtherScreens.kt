package dev.darknet.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.darknet.BuildConfig
import dev.darknet.ShizukuState
import dev.darknet.SpeedTest
import dev.darknet.data.Settings
import dev.darknet.data.ShareStatus
import kotlin.math.ln

@Composable
private fun Screen(title: String, content: @Composable () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(title, style = Dn.Title)
        content()
    }
}

// ---- devices ------------------------------------------------------------------

@Composable
fun DevicesScreen(status: ShareStatus, shizukuReady: Boolean) {
    Screen("Dispositivos") {
        if (shizukuReady) QrPanel()
        if (!status.isActive) {
            Text("Empieza a compartir para ver quién se conecta.", style = Dn.Small)
            return@Screen
        }
        if (status.devices.isEmpty()) {
            Text("Nadie conectado todavía.", style = Dn.Small)
            return@Screen
        }
        status.devices.forEach { d ->
            Panel {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(40.dp).clip(CircleShape).background(Dn.Violet.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            when {
                                d.isUsb -> Icons.Filled.Usb
                                d.name.contains("pc", true) || d.name.contains("laptop", true) -> Icons.Filled.Computer
                                else -> Icons.Filled.PhoneAndroid
                            },
                            null, tint = Dn.VioletSoft,
                        )
                    }
                    Column(Modifier.weight(1f)) {
                        Text(d.name.ifBlank { "Dispositivo sin nombre" }, style = Dn.Body.copy(fontWeight = FontWeight.SemiBold))
                        Text(listOf(d.ip, d.mac).filter { it.isNotBlank() }.joinToString(" · "), style = Dn.Small)
                    }
                }
            }
        }
    }
}

// ---- speed test ---------------------------------------------------------------

@Composable
fun SpeedScreen(onRun: () -> Unit) {
    val r by SpeedTest.state.collectAsState()
    val shown = when (r.phase) {
        SpeedTest.Phase.DOWNLOAD, SpeedTest.Phase.UPLOAD -> r.liveMbps
        SpeedTest.Phase.DONE -> r.downMbps ?: 0.0
        else -> 0.0
    }
    Screen("Velocidad") {
        Text("Mide la conexión de este teléfono, que es la que comparten los dispositivos.", style = Dn.Small)
        Box(Modifier.fillMaxWidth().padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
            Box(Modifier.size(240.dp), contentAlignment = Alignment.Center) {
                Canvas(Modifier.fillMaxSize()) {
                    val stroke = 14.dp.toPx()
                    val tl = Offset(stroke / 2, stroke / 2)
                    val sz = androidx.compose.ui.geometry.Size(size.width - stroke, size.height - stroke)
                    drawArc(Dn.Border, 140f, 260f, false, tl, sz, style = Stroke(stroke, cap = StrokeCap.Round))
                    val f = if (shown <= 0) 0f else (ln(1 + shown) / ln(1001.0)).toFloat().coerceIn(0f, 1f)
                    if (f > 0f) drawArc(
                        Brush.sweepGradient(listOf(Dn.Cyan, Dn.Violet, Dn.Cyan)), 140f, 260f * f, false, tl, sz,
                        style = Stroke(stroke, cap = StrokeCap.Round),
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(if (shown >= 100) "%.0f".format(shown) else "%.1f".format(shown), style = Dn.Title.copy(fontSize = 48.sp))
                    Text("Mbps", style = Dn.Small)
                    Text(
                        when (r.phase) {
                            SpeedTest.Phase.IDLE -> "LISTO"
                            SpeedTest.Phase.PING -> "PING"
                            SpeedTest.Phase.DOWNLOAD -> "BAJADA"
                            SpeedTest.Phase.UPLOAD -> "SUBIDA"
                            SpeedTest.Phase.DONE -> "RESULTADO"
                            SpeedTest.Phase.FAILED -> "FALLÓ"
                        },
                        style = Dn.Label.copy(color = Dn.Cyan),
                    )
                }
            }
        }
        Button(
            onClick = onRun,
            enabled = !r.running,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Dn.Violet),
        ) { Text(if (r.running) "Midiendo…" else "Iniciar prueba", color = Dn.Background) }
        Panel {
            Row {
                Stat("Ping", r.pingMs?.let { "$it ms" } ?: "–", Modifier.weight(1f))
                Stat("Bajada", r.downMbps?.let { "%.0f Mbps".format(it) } ?: "–", Modifier.weight(1f))
                Stat("Subida", r.upMbps?.let { "%.0f Mbps".format(it) } ?: "–", Modifier.weight(1f))
            }
        }
        if (r.phase == SpeedTest.Phase.FAILED) Text("La prueba falló: ${r.error}", style = Dn.Small)
    }
}

// ---- settings -----------------------------------------------------------------

@Composable
fun SettingsScreen(
    settings: Settings,
    shizuku: ShizukuState,
    log: List<String>?,
    onFastEngine: (Boolean) -> Unit,
    onUsb: (Boolean) -> Unit,
    onQuota: (Int) -> Unit,
    onResetDay: (Int) -> Unit,
    onQuotaStop: (Boolean) -> Unit,
    onIdle: (Int) -> Unit,
    onAlerts: (Boolean) -> Unit,
    onAccent: (String) -> Unit,
    onTheme: (String) -> Unit,
    onDns: (String) -> Unit,
    onBand: (String) -> Unit,
    onStability: (Boolean) -> Unit,
    onLock: (Boolean) -> Unit,
    onShizuku: () -> Unit,
    onShowLog: () -> Unit,
    onHideLog: () -> Unit,
) {
    Screen("Ajustes") {
        SectionLabel("Motor")
        Panel {
            Toggle(
                "Motor rápido",
                "Deja que el TCP de Android lleve las conexiones: más velocidad y menos batería. Si tu teléfono no lo permite, DarkNet usa solo el motor de respaldo.",
                settings.fastEngine, onFastEngine,
            )
            Toggle("Compartir también por USB", "Incluye un cable USB además del hotspot Wi-Fi.", settings.usb, onUsb)
            Text("Los cambios se aplican la próxima vez que empiezas a compartir.", style = Dn.Small)
        }

        SectionLabel("Apariencia")
        Panel {
            Choice("Tema", "", settings.theme, listOf("dark", "light", "system"),
                { mapOf("dark" to "Oscuro", "light" to "Claro", "system" to "Como el sistema")[it] ?: it }, onTheme)
            Text("Color", style = Dn.Body.copy(fontWeight = FontWeight.SemiBold))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Accent.entries.forEach { a ->
                    Box(
                        Modifier.size(36.dp).clip(CircleShape).background(a.main)
                            .border(3.dp, if (settings.accent == a.name) Dn.Text else Dn.Surface, CircleShape)
                            .clickable { onAccent(a.name) },
                    )
                }
            }
        }

        SectionLabel("Red de los dispositivos")
        Panel {
            Choice(
                "DNS",
                "Traduce los nombres de las páginas. “Automático” mide cuál responde más rápido en tu red cada vez que empiezas a compartir.",
                settings.dns, dev.darknet.data.DnsChoice.labels.keys.toList(), { dev.darknet.data.DnsChoice.labels[it] ?: it }, onDns,
            )
            if (settings.dns == "auto" && settings.dnsAutoResult.isNotEmpty()) {
                Text("Última elección: ${settings.dnsAutoResult}", style = Dn.Small.copy(color = Dn.Cyan))
            }
            Text("Se aplica la próxima vez que empiezas a compartir. Apps con su propio DNS seguro pueden ignorarlo.", style = Dn.Small)
        }

        SectionLabel("Hotspot")
        Panel {
            Choice(
                "Banda Wi‑Fi",
                "5 GHz es la más rápida y compatible con iPhone. 6 GHz necesita WPA3 y equipos recientes; 2.4 GHz llega más lejos pero es más lenta.",
                settings.band, listOf("auto", "5", "6", "2"),
                { mapOf("auto" to "La del teléfono", "2" to "2.4 GHz", "5" to "5 GHz", "6" to "6 GHz")[it] ?: it }, onBand,
            )
            Text("Se aplica la próxima vez que empiezas a compartir y también cambia la banda en los ajustes del teléfono.", style = Dn.Small)
        }

        SectionLabel("Plan de datos")
        Panel {
            Choice("Plan mensual", "Tus GB del mes; aviso al 80 %.", settings.quotaGb, listOf(0, 5, 10, 15, 20, 30, 50, 100), { if (it == 0) "Desactivado" else "$it GB" }, onQuota)
            if (settings.quotaGb > 0) {
                Choice("Se renueva el día", "", settings.quotaResetDay, (1..28).toList(), { it.toString() }, onResetDay)
                Toggle("Detener al llegar al límite", "Deja de compartir cuando se acabe el plan.", settings.quotaStop, onQuotaStop)
            }
        }

        SectionLabel("Automático")
        Panel {
            Choice("Apagar si no hay nadie", "Tras este tiempo sin dispositivos conectados.", settings.idleMinutes, listOf(0, 3, 5, 10, 15, 30, 60), { if (it == 0) "Nunca" else "$it min" }, onIdle)
            Toggle("Avisos de conexión", "Notificación y vibración cuando alguien entra o sale.", settings.deviceAlerts, onAlerts)
            Toggle("Avisos de estabilidad", "Te avisa si se corta el internet, cuando vuelve, o si el ping sube mucho.", settings.stabilityAlerts, onStability)
        }

        SectionLabel("Seguridad")
        Panel {
            Toggle("Bloqueo con huella o PIN", "Pide tu huella, cara o PIN para abrir DarkNet.", settings.lock, onLock)
        }

        SectionLabel("Shizuku")
        Panel {
            Text(
                when (shizuku) {
                    ShizukuState.READY -> "Conectado y con permiso."
                    ShizukuState.NO_PERMISSION -> "Falta el permiso para DarkNet."
                    ShizukuState.NOT_RUNNING -> "Shizuku no está activo."
                    ShizukuState.NOT_INSTALLED -> "Shizuku no está instalado."
                },
                style = Dn.Body,
            )
            if (shizuku == ShizukuState.NO_PERMISSION) TextButton(onClick = onShizuku) { Text("Conceder permiso", color = Dn.VioletSoft) }
        }

        SectionLabel("Diagnóstico")
        Panel {
            if (log == null) {
                TextButton(onClick = onShowLog) { Text("Ver registro", color = Dn.VioletSoft) }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Registro", style = Dn.Heading, modifier = Modifier.weight(1f))
                    TextButton(onClick = onHideLog) { Text("Ocultar", color = Dn.VioletSoft) }
                }
                Column(Modifier.horizontalScroll(rememberScrollState())) {
                    log.takeLast(120).forEach {
                        Text(it, style = Dn.Small.copy(fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = Dn.Text))
                    }
                }
            }
        }

        SectionLabel("Acerca de")
        Panel {
            Text("DarkNet ${BuildConfig.VERSION_NAME}", style = Dn.Heading)
            Text("Creada por zeussavage22.", style = Dn.Small)
            Text("Usa Shizuku (Apache-2.0) y gVisor (Apache-2.0).", style = Dn.Small)
            Text("Compartir datos puede ir contra el contrato de tu operadora.", style = Dn.Small)
        }
    }
}

@Composable
private fun Toggle(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = Dn.Body.copy(fontWeight = FontWeight.SemiBold))
            Text(subtitle, style = Dn.Small)
        }
        Switch(
            checked = checked, onCheckedChange = onChange,
            colors = SwitchDefaults.colors(checkedTrackColor = Dn.Violet, checkedThumbColor = Dn.Background),
        )
    }
}

/** A setting with a few choices, cycled with a tap and listed in a menu. */
@Composable
private fun <T> Choice(
    title: String,
    subtitle: String,
    value: T,
    options: List<T>,
    label: (T) -> String,
    onChange: (T) -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = Dn.Body.copy(fontWeight = FontWeight.SemiBold))
            if (subtitle.isNotEmpty()) Text(subtitle, style = Dn.Small)
        }
        Box {
            TextButton(onClick = { open = true }) { Text(label(value), color = Dn.VioletSoft) }
            DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                options.forEach { option ->
                    DropdownMenuItem(text = { Text(label(option)) }, onClick = { onChange(option); open = false })
                }
            }
        }
    }
}
