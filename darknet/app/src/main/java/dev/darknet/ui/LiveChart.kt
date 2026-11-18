package dev.darknet.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.darknet.ShareService
import dev.darknet.data.Bytes
import dev.darknet.data.Health
import dev.darknet.data.ShareStatus

/** Live download/upload chart of the last minute, plus totals. */
@Composable
fun LiveChartCard(status: ShareStatus) {
    val history by ShareService.history.collectAsState()
    val peak = history.maxOfOrNull { maxOf(it.first, it.second) } ?: 0L
    Panel {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SectionLabel("Velocidad en vivo", Modifier.weight(1f))
            Text("máx ${mbps(peak)}", style = Dn.Small)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
            Legend("Bajada", mbps(status.speedDown), Dn.Cyan)
            Legend("Subida", mbps(status.speedUp), Dn.Violet)
        }
        Chart(history, Modifier.fillMaxWidth().height(130.dp))
        Text("último minuto", style = Dn.Small)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Stat("Bajado", Bytes.format(status.bytesDown), Modifier.weight(1f))
            Stat("Subido", Bytes.format(status.bytesUp), Modifier.weight(1f))
            Stat("Equipos", status.devices.size.toString(), Modifier.weight(1f))
        }
    }
}

/** Speeds in megabits per second, as speed tests show them. */
fun mbps(bytesPerSecond: Long): String {
    val mbps = bytesPerSecond * 8 / 1_000_000.0
    return when {
        mbps >= 100 -> "%.0f Mbps".format(mbps)
        mbps >= 0.1 -> "%.1f Mbps".format(mbps)
        bytesPerSecond > 0 -> "%.0f kbps".format(bytesPerSecond * 8 / 1000.0)
        else -> "0 Mbps"
    }
}

@Composable
private fun Legend(label: String, value: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(Modifier.size(10.dp).clip(CircleShape).background(color))
        Column {
            Text(label, style = Dn.Small)
            Text(value, style = Dn.Value.copy(color = color))
        }
    }
}

@Composable
private fun Chart(points: List<Pair<Long, Long>>, modifier: Modifier) {
    val grid = Dn.Border
    val down = Dn.Cyan
    val up = Dn.Violet
    Canvas(modifier) {
        val top = (points.maxOfOrNull { maxOf(it.first, it.second) } ?: 0L).coerceAtLeast(125_000).toFloat() * 1.15f
        for (i in 1..3) {
            val y = size.height * i / 4f
            drawLine(grid, Offset(0f, y), Offset(size.width, y), 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 8f)))
        }
        if (points.size < 2) return@Canvas
        series(points.map { it.second }, top, up, fill = false)
        series(points.map { it.first }, top, down, fill = true)
    }
}

private fun DrawScope.series(values: List<Long>, top: Float, color: Color, fill: Boolean) {
    val step = size.width / (ShareService.HISTORY - 1)
    val start = size.width - step * (values.size - 1)
    fun at(i: Int) = Offset(start + step * i, size.height - values[i] / top * size.height)
    val line = Path().apply {
        moveTo(at(0).x, at(0).y)
        for (i in 1 until values.size) {
            val a = at(i - 1)
            val b = at(i)
            val mid = (a.x + b.x) / 2
            cubicTo(mid, a.y, mid, b.y, b.x, b.y)
        }
    }
    if (fill) {
        val area = Path().apply {
            addPath(line)
            lineTo(at(values.lastIndex).x, size.height)
            lineTo(start, size.height)
            close()
        }
        drawPath(area, Brush.verticalGradient(listOf(color.copy(alpha = 0.35f), Color.Transparent)))
    }
    drawPath(line, color, style = Stroke(2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
    drawCircle(color, 4.dp.toPx(), at(values.lastIndex))
}

/** Ping, stability and cuts of the phone's own connection. */
@Composable
fun StabilityCard() {
    val h by ShareService.connection.collectAsState()
    val q = h.quality
    val color = when (q) {
        Health.Quality.GREAT, Health.Quality.GOOD -> Dn.Green
        Health.Quality.UNSTABLE, Health.Quality.BAD -> Dn.Amber
        Health.Quality.OFFLINE -> Dn.Red
        Health.Quality.UNKNOWN -> Dn.Muted
    }
    Panel {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SectionLabel("Estabilidad", Modifier.weight(1f))
            Text(
                q.label,
                style = Dn.Small.copy(color = color, fontWeight = FontWeight.SemiBold),
                modifier = Modifier.clip(RoundedCornerShape(50)).background(color.copy(alpha = 0.12f)).padding(horizontal = 10.dp, vertical = 4.dp),
            )
        }
        PingBars(h.samples, Modifier.fillMaxWidth().height(36.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Stat("Ping", h.last?.let { "$it ms" } ?: "–", Modifier.weight(1f))
            Stat("Variación", h.jitter?.let { "$it ms" } ?: "–", Modifier.weight(1f))
            Stat("Cortes", h.drops.toString(), Modifier.weight(1f))
        }
        Text("Mide la conexión de tus datos cada 5 segundos.", style = Dn.Small)
    }
}

@Composable
private fun PingBars(samples: List<Long?>, modifier: Modifier) {
    val good = Dn.Green
    val warn = Dn.Amber
    val bad = Dn.Red
    val empty = Dn.Border
    Canvas(modifier) {
        val n = Health.WINDOW
        val gap = 3.dp.toPx()
        val w = (size.width - gap * (n - 1)) / n
        val offset = n - samples.size
        for (i in 0 until n) {
            val s = samples.getOrNull(i - offset)
            val x = i * (w + gap)
            val (h, c) = when {
                i < offset -> 0.15f to empty
                s == null -> 1f to bad
                s > 300 -> 0.9f to warn
                s > 100 -> 0.6f to warn
                else -> (0.25f + s / 100f * 0.35f) to good
            }
            drawLine(c, Offset(x + w / 2, size.height), Offset(x + w / 2, size.height * (1 - h)), w, cap = StrokeCap.Butt)
        }
    }
}
