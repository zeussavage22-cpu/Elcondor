package com.elcondor.optimizer.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.elcondor.optimizer.data.AppsRepo
import com.elcondor.optimizer.data.Severity
import com.elcondor.optimizer.data.Tip
import com.elcondor.optimizer.ui.theme.Bad
import com.elcondor.optimizer.ui.theme.Good
import com.elcondor.optimizer.ui.theme.Info
import com.elcondor.optimizer.ui.theme.Warn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

fun levelColor(fraction: Float): Color = when {
    fraction < 0.7f -> Good
    fraction < 0.88f -> Warn
    else -> Bad
}

fun scoreColor(score: Int) = when { score >= 80 -> Good; score >= 60 -> Warn; else -> Bad }

@Composable
fun SectionCard(modifier: Modifier = Modifier, title: String? = null, icon: ImageVector? = null,
                trailing: @Composable (() -> Unit)? = null, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(Modifier.padding(16.dp)) {
            if (title != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (icon != null) {
                        Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                    trailing?.invoke()
                }
                Spacer(Modifier.height(12.dp))
            }
            content()
        }
    }
}

@Composable
fun ScoreRing(score: Int, size: Dp = 150.dp, label: String = "Salud") {
    val anim by animateFloatAsState(score / 100f, tween(1200), label = "score")
    val color = scoreColor(score)
    val track = MaterialTheme.colorScheme.surfaceContainerHigh
    Box(Modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val w = 14.dp.toPx()
            drawArc(track, 135f, 270f, false, style = Stroke(w, cap = StrokeCap.Round))
            drawArc(color, 135f, 270f * anim, false, style = Stroke(w, cap = StrokeCap.Round))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("${(anim * 100).toInt()}", fontSize = 44.sp, fontWeight = FontWeight.Bold, color = color)
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun MetricBar(label: String, value: String, fraction: Float, color: Color = levelColor(fraction)) {
    Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row {
            Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { fraction.coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
            color = color, trackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            drawStopIndicator = {},
        )
    }
}

@Composable
fun StatTile(modifier: Modifier, icon: ImageVector, label: String, value: String, sub: String? = null, tint: Color = MaterialTheme.colorScheme.primary) {
    Card(modifier, shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
        Column(Modifier.padding(14.dp)) {
            Icon(icon, null, tint = tint, modifier = Modifier.size(22.dp))
            Spacer(Modifier.height(8.dp))
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, maxLines = 1)
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (sub != null) Text(sub, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        }
    }
}

@Composable
fun KeyValue(k: String, v: String, color: Color = Color.Unspecified) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Text(k, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Text(v, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = color)
    }
}

@Composable
fun AppIcon(pkg: String?, size: Dp = 36.dp) {
    val ctx = LocalContext.current
    val bmp by produceState<android.graphics.Bitmap?>(null, pkg) {
        value = if (pkg == null) null else withContext(Dispatchers.IO) { AppsRepo.icon(ctx, pkg) }
    }
    val b = bmp
    if (b != null) Image(b.asImageBitmap(), null, Modifier.size(size).clip(RoundedCornerShape(10.dp)))
    else Box(Modifier.size(size).clip(RoundedCornerShape(10.dp)).background(MaterialTheme.colorScheme.surfaceContainerHigh),
        contentAlignment = Alignment.Center) { Icon(Icons.Filled.Android, null, Modifier.size(size * 0.6f)) }
}

@Composable
fun TipCard(tip: Tip, onAction: (() -> Unit)?) {
    val (icon, color) = when (tip.severity) {
        Severity.CRITICAL -> Icons.Filled.Error to Bad
        Severity.WARNING -> Icons.Filled.Warning to Warn
        Severity.TIP -> Icons.Filled.Lightbulb to Info
        Severity.OK -> Icons.Filled.CheckCircle to Good
    }
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.Top) {
        Box(Modifier.size(34.dp).clip(CircleShape).background(color.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = color, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(tip.title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            Text(tip.detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (onAction != null && tip.actionLabel != null) {
            Spacer(Modifier.width(8.dp))
            FilledTonalButton(onClick = onAction, contentPadding = PaddingValues(horizontal = 12.dp)) { Text(tip.actionLabel) }
        }
    }
}

@Composable
fun Sparkline(values: List<Float>, modifier: Modifier, color: Color = MaterialTheme.colorScheme.primary) {
    Canvas(modifier) {
        if (values.size < 2) return@Canvas
        val max = values.max().coerceAtLeast(1f)
        val min = 0f
        val stepX = size.width / (values.size - 1)
        val path = Path()
        values.forEachIndexed { i, v ->
            val p = Offset(i * stepX, size.height - (v - min) / (max - min) * size.height)
            if (i == 0) path.moveTo(p.x, p.y) else path.lineTo(p.x, p.y)
        }
        drawPath(path, color, style = Stroke(2.5.dp.toPx(), cap = StrokeCap.Round))
        val fill = Path().apply { addPath(path); lineTo(size.width, size.height); lineTo(0f, size.height); close() }
        drawPath(fill, color.copy(alpha = 0.15f))
    }
}

@Composable
fun PermissionGate(title: String, text: String, button: String, onGrant: () -> Unit) {
    SectionCard {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(6.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(12.dp))
        Button(onClick = onGrant) { Text(button) }
    }
}

@Composable
fun Loading(text: String = "Analizando…") {
    Column(Modifier.fillMaxWidth().padding(40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        CircularProgressIndicator()
        Spacer(Modifier.height(12.dp))
        Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
