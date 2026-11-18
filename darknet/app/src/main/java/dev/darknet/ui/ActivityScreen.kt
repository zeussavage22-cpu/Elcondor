package dev.darknet.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.darknet.data.Bytes
import dev.darknet.data.Session
import dev.darknet.data.UsageStore
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Data shared over time: totals, a two-week chart and the latest sessions. */
@Composable
fun ActivityScreen() {
    val context = LocalContext.current
    remember { UsageStore.load(context) }
    val usage by UsageStore.usage.collectAsState()
    val today = LocalDate.now()
    var confirmClear by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("Actividad", style = Dn.Title)

        Panel {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Stat("Hoy", Bytes.format(usage.on(today)), Modifier.weight(1f))
                Stat("7 días", Bytes.format(usage.lastDays(today, 7)), Modifier.weight(1f))
                Stat("Este mes", Bytes.format(usage.month(today)), Modifier.weight(1f))
            }
        }

        val series = usage.series(today, 14)
        val peak = series.maxOfOrNull { it.second } ?: 0L
        Panel {
            Row {
                SectionLabel("Últimos 14 días", Modifier.weight(1f))
                Text("máx ${Bytes.format(peak)}", style = Dn.Small)
            }
            Canvas(Modifier.fillMaxWidth().height(130.dp)) {
                val slot = size.width / series.size
                val w = slot * 0.6f
                val top = peak.coerceAtLeast(1L).toFloat()
                series.forEachIndexed { i, (day, bytes) ->
                    val h = if (bytes <= 0) 3.dp.toPx() else bytes / top * size.height
                    drawRoundRect(
                        brush = if (day == today) Brush.verticalGradient(listOf(Dn.Cyan, Dn.Violet))
                        else Brush.verticalGradient(listOf(Dn.Violet, Dn.Violet.copy(alpha = 0.55f))),
                        topLeft = Offset(slot * i + (slot - w) / 2, size.height - h),
                        size = Size(w, h),
                        cornerRadius = CornerRadius(w / 3, w / 3),
                        alpha = if (bytes <= 0) 0.25f else 1f,
                    )
                }
            }
            Row {
                Text(series.first().first.let { "${it.dayOfMonth}/${it.monthValue}" }, style = Dn.Small, modifier = Modifier.weight(1f))
                Text("Hoy", style = Dn.Small)
            }
        }

        SectionLabel("Sesiones")
        if (usage.sessions.isEmpty()) {
            Text("Aún no hay sesiones. Empieza a compartir y aparecerán aquí.", style = Dn.Small)
        } else {
            usage.sessions.take(20).forEach { SessionRow(it) }
        }

        Panel {
            if (confirmClear) {
                Text("¿Borrar todo el historial? No se puede deshacer.", style = Dn.Body)
                Row {
                    TextButton(onClick = { UsageStore.clear(context); confirmClear = false }) { Text("Borrar", color = Dn.Red) }
                    TextButton(onClick = { confirmClear = false }) { Text("Cancelar", color = Dn.VioletSoft) }
                }
            } else {
                TextButton(onClick = { confirmClear = true }) { Text("Borrar historial", color = Dn.Muted) }
            }
        }
    }
}

private val when_ = DateTimeFormatter.ofPattern("EEE d MMM · HH:mm", Locale("es"))

@Composable
private fun SessionRow(s: Session) {
    Panel {
        Row {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    Instant.ofEpochMilli(s.start).atZone(ZoneId.systemDefault()).format(when_),
                    style = Dn.Body.copy(fontWeight = FontWeight.SemiBold),
                )
                Text("${Bytes.duration(s.durationMs)} · ${s.devices} equipos máx.", style = Dn.Small)
            }
            Text(Bytes.format(s.bytes), style = Dn.Value.copy(color = Dn.VioletSoft))
        }
    }
}
