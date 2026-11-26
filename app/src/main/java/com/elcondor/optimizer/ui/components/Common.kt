package com.elcondor.optimizer.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.elcondor.optimizer.data.AppsRepo
import com.elcondor.optimizer.data.Severity
import com.elcondor.optimizer.data.Tip
import com.elcondor.optimizer.ui.theme.Bad
import com.elcondor.optimizer.ui.theme.Good
import com.elcondor.optimizer.ui.theme.Info
import com.elcondor.optimizer.ui.theme.Warn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Espacio inferior para que el contenido no quede bajo la barra flotante. */
val BottomSpace = 132.dp

fun levelColor(fraction: Float): Color = when {
    fraction < 0.7f -> Good
    fraction < 0.88f -> Warn
    else -> Bad
}

fun scoreColor(score: Int) = when { score >= 80 -> Good; score >= 60 -> Warn; else -> Bad }

fun severityColor(s: Severity) = when (s) {
    Severity.CRITICAL -> Bad; Severity.WARNING -> Warn; Severity.TIP -> Info; Severity.OK -> Good
}

@Composable
fun muted() = MaterialTheme.colorScheme.onSurfaceVariant

/** Encabezado grande de pantalla. */
@Composable
fun Header(title: String, subtitle: String? = null, actions: @Composable RowScope.() -> Unit = {}) {
    Row(Modifier.fillMaxWidth().statusBarsPadding().padding(start = 24.dp, end = 12.dp, top = 20.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.headlineLarge)
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = muted())
        }
        actions()
    }
}

/** Etiqueta de sección pequeña en mayúsculas. */
@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier, trailing: (@Composable () -> Unit)? = null) {
    Row(modifier.fillMaxWidth().padding(start = 4.dp, top = 12.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(text.uppercase(), style = MaterialTheme.typography.labelSmall, color = muted(), modifier = Modifier.weight(1f))
        trailing?.invoke()
    }
}

/** Bloque plano redondeado: el contenedor base de toda la app. */
@Composable
fun Block(modifier: Modifier = Modifier, onClick: (() -> Unit)? = null, padding: Dp = 20.dp, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .animateContentSize()
            .padding(padding),
        content = content,
    )
}

@Composable
fun ThinBar(fraction: Float, color: Color, modifier: Modifier = Modifier) {
    val f by animateFloatAsState(fraction.coerceIn(0f, 1f), tween(900, easing = FastOutSlowInEasing), label = "bar")
    val track = MaterialTheme.colorScheme.surfaceContainerHighest
    Canvas(modifier.fillMaxWidth().height(6.dp)) {
        val h = size.height
        drawLine(track, Offset(h / 2, h / 2), Offset(size.width - h / 2, h / 2), h, StrokeCap.Round)
        if (f > 0f) drawLine(color, Offset(h / 2, h / 2), Offset(h / 2 + (size.width - h) * f, h / 2), h, StrokeCap.Round)
    }
}

@Composable
fun BarRow(label: String, value: String, fraction: Float, color: Color = levelColor(fraction), sub: String? = null) {
    Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(value, style = MaterialTheme.typography.bodyMedium, color = muted())
        }
        Spacer(Modifier.height(8.dp))
        ThinBar(fraction, color)
        if (sub != null) { Spacer(Modifier.height(6.dp)); Text(sub, style = MaterialTheme.typography.labelMedium, color = muted(), maxLines = 1, overflow = TextOverflow.Ellipsis) }
    }
}

@Composable
fun KeyValue(k: String, v: String, color: Color = Color.Unspecified) {
    Row(Modifier.fillMaxWidth().padding(vertical = 7.dp)) {
        Text(k, color = muted(), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Text(v, style = MaterialTheme.typography.bodyMedium, color = color)
    }
}

/** Anillo de puntuación fino con degradado; gira mientras trabaja. */
@Composable
fun ScoreRing(score: Int, busy: Boolean, size: Dp = 236.dp, center: @Composable () -> Unit) {
    val anim by animateFloatAsState(score / 100f, tween(1400, easing = FastOutSlowInEasing), label = "score")
    val inf = rememberInfiniteTransition(label = "spin")
    val rot by inf.animateFloat(0f, 360f, infiniteRepeatable(tween(1400, easing = LinearEasing)), label = "rot")
    val pulse by inf.animateFloat(0.35f, 1f, infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "pulse")
    val color = scoreColor(score)
    val track = MaterialTheme.colorScheme.surfaceContainerHigh
    Box(Modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize().rotate(if (busy) rot else 0f)) {
            val w = 5.dp.toPx()
            val inset = w
            val arc = androidx.compose.ui.geometry.Size(this.size.width - inset * 2, this.size.height - inset * 2)
            val tl = Offset(inset, inset)
            drawArc(track, 0f, 360f, false, tl, arc, style = Stroke(w))
            if (busy) {
                drawArc(Brush.sweepGradient(listOf(Color.Transparent, Info.copy(alpha = pulse), Info)), 0f, 300f, false, tl, arc,
                    style = Stroke(w, cap = StrokeCap.Round))
            } else {
                drawArc(Brush.sweepGradient(listOf(color.copy(alpha = 0.25f), color, color.copy(alpha = 0.25f))),
                    -90f, 360f * anim, false, tl, arc, style = Stroke(w, cap = StrokeCap.Round))
            }
        }
        center()
    }
}

@Composable
fun AppIcon(pkg: String?, size: Dp = 40.dp) {
    val ctx = LocalContext.current
    val bmp by produceState<android.graphics.Bitmap?>(null, pkg) {
        value = if (pkg == null) null else withContext(Dispatchers.IO) { AppsRepo.icon(ctx, pkg) }
    }
    val b = bmp
    if (b != null) Image(b.asImageBitmap(), null, Modifier.size(size).clip(RoundedCornerShape(size * 0.3f)))
    else Box(Modifier.size(size).clip(RoundedCornerShape(size * 0.3f)).background(MaterialTheme.colorScheme.surfaceContainerHigh),
        contentAlignment = Alignment.Center) { Icon(Icons.Filled.Android, null, Modifier.size(size * 0.5f), tint = muted()) }
}

@Composable
fun Dot(color: Color, size: Dp = 8.dp) = Box(Modifier.size(size).clip(CircleShape).background(color))

/** Sugerencia minimalista: punto de color, texto y acción. */
@Composable
fun TipRow(tip: Tip, onClick: (() -> Unit)?) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp))
        .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
        .padding(horizontal = 4.dp, vertical = 12.dp), verticalAlignment = Alignment.Top) {
        Box(Modifier.padding(top = 7.dp)) { Dot(severityColor(tip.severity)) }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(tip.title, style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(2.dp))
            Text(tip.detail, style = MaterialTheme.typography.bodySmall, color = muted())
        }
        if (onClick != null && tip.actionLabel != null) {
            Spacer(Modifier.width(8.dp))
            Text(tip.actionLabel, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 2.dp))
        }
    }
}

@Composable
fun NavRow(icon: ImageVector?, title: String, sub: String? = null, value: String? = null, valueColor: Color = Color.Unspecified, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).clickable(onClick = onClick).padding(vertical = 12.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically) {
        if (icon != null) {
            Box(Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceContainerHigh), contentAlignment = Alignment.Center) {
                Icon(icon, null, Modifier.size(20.dp))
            }
            Spacer(Modifier.width(14.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (sub != null) Text(sub, style = MaterialTheme.typography.bodySmall, color = muted())
        }
        if (value != null) Text(value, style = MaterialTheme.typography.bodyMedium, color = if (valueColor == Color.Unspecified) muted() else valueColor)
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = muted())
    }
}

@Composable
fun Sparkline(values: List<Float>, modifier: Modifier, color: Color = MaterialTheme.colorScheme.primary) {
    Canvas(modifier) {
        if (values.size < 2) return@Canvas
        val max = values.max().coerceAtLeast(0.001f)
        val stepX = size.width / (values.size - 1)
        val path = Path()
        values.forEachIndexed { i, v ->
            val x = i * stepX; val y = size.height - v / max * size.height * 0.92f
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        val fill = Path().apply { addPath(path); lineTo(size.width, size.height); lineTo(0f, size.height); close() }
        drawPath(fill, Brush.verticalGradient(listOf(color.copy(alpha = 0.28f), Color.Transparent)))
        drawPath(path, color, style = Stroke(2.dp.toPx(), cap = StrokeCap.Round))
    }
}

/** Estado bloqueado por permisos, con un solo botón para configurarlo. */
@Composable
fun Locked(text: String, onSetup: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Filled.Lock, null, tint = muted(), modifier = Modifier.size(22.dp))
        Spacer(Modifier.height(8.dp))
        Text(text, style = MaterialTheme.typography.bodySmall, color = muted(), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        Spacer(Modifier.height(12.dp))
        FilledTonalButton(onClick = onSetup) { Text("Activar") }
    }
}

@Composable
fun Loading(text: String = "Analizando…") {
    Column(Modifier.fillMaxWidth().padding(40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(28.dp))
        Spacer(Modifier.height(14.dp))
        Text(text, style = MaterialTheme.typography.bodySmall, color = muted())
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> Segmented(options: List<Pair<T, String>>, selected: T, onSelect: (T) -> Unit) {
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        options.forEachIndexed { i, (v, l) ->
            SegmentedButton(
                selected = v == selected, onClick = { onSelect(v) },
                shape = SegmentedButtonDefaults.itemShape(i, options.size),
                icon = {},
                colors = SegmentedButtonDefaults.colors(
                    activeContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    inactiveContainerColor = Color.Transparent,
                    activeBorderColor = MaterialTheme.colorScheme.outline, inactiveBorderColor = MaterialTheme.colorScheme.outline,
                ),
            ) { Text(l, style = MaterialTheme.typography.labelMedium) }
        }
    }
}

/** Botón principal tipo píldora. */
@Composable
fun PillButton(text: String, modifier: Modifier = Modifier, enabled: Boolean = true, icon: ImageVector? = null, onClick: () -> Unit) {
    Button(onClick = onClick, enabled = enabled, modifier = modifier.height(58.dp), shape = CircleShape,
        contentPadding = PaddingValues(horizontal = 28.dp)) {
        if (icon != null) { Icon(icon, null, Modifier.size(20.dp)); Spacer(Modifier.width(10.dp)) }
        Text(text, style = MaterialTheme.typography.titleMedium)
    }
}
