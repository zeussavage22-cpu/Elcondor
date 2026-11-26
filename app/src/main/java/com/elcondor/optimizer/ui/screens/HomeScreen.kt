package com.elcondor.optimizer.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.elcondor.optimizer.data.*
import com.elcondor.optimizer.ui.components.*
import com.elcondor.optimizer.ui.theme.Bad
import com.elcondor.optimizer.ui.theme.Good
import com.elcondor.optimizer.ui.theme.Warn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun HomeScreen(modifier: Modifier, refresh: Int, onAction: (Action) -> Unit, onTools: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    var analysis by remember { mutableStateOf(lastAnalysis) }
    var busy by remember { mutableStateOf(false) }
    var step by remember { mutableStateOf("") }
    var result by remember { mutableStateOf<String?>(null) }
    var reload by remember { mutableIntStateOf(0) }
    var showAllTips by remember { mutableStateOf(false) }

    LaunchedEffect(refresh, reload) {
        analysis = withContext(Dispatchers.IO) { Advisor.analyze(ctx) }.also { lastAnalysis = it }
    }

    fun optimize() {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        busy = true; result = null
        scope.launch {
            step = "Liberando memoria"
            val ram = withContext(Dispatchers.IO) { AppsRepo.boost(ctx) }
            var bytes = 0L; var n = 0
            if (Perms.hasAllFiles()) {
                step = "Buscando basura"
                val r = withContext(Dispatchers.IO) { JunkScanner.scan(ctx) }
                step = "Limpiando"
                val safe = listOf(JunkType.TEMP, JunkType.THUMBS, JunkType.EMPTY).flatMap { r.items[it].orEmpty() }
                val (c, b, _) = withContext(Dispatchers.IO) { JunkScanner.delete(safe) }
                n = c; bytes = b
            }
            step = "Reanalizando"
            analysis = withContext(Dispatchers.IO) { Advisor.analyze(ctx) }.also { lastAnalysis = it }
            result = buildString {
                append("${Fmt.bytes(ram)} de RAM liberada")
                if (n > 0) append(" · ${Fmt.bytes(bytes)} de basura eliminada")
            }
            busy = false
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    val a = analysis
    LazyColumn(modifier, contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = BottomSpace)) {
        item {
            Header("Cóndor", a?.identity?.model ?: "Analizando…") {
                IconButton(onClick = { reload++ }) { Icon(Icons.Outlined.Refresh, "Reanalizar") }
                IconButton(onClick = onTools) { Icon(Icons.Outlined.Build, "Herramientas") }
            }
        }
        if (a == null) { item { Loading("Analizando todo tu teléfono…") }; return@LazyColumn }

        item {
            Column(Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                ScoreRing(a.score, busy) {
                    AnimatedContent(busy, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "center") { b ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            if (b) {
                                Text(step, style = MaterialTheme.typography.titleMedium)
                                Text("un momento", style = MaterialTheme.typography.bodySmall, color = muted())
                            } else {
                                Text("${a.score}", style = MaterialTheme.typography.displayLarge, color = scoreColor(a.score))
                                Text(when {
                                    a.score >= 85 -> "Excelente"
                                    a.score >= 70 -> "Bien"
                                    a.score >= 50 -> "Mejorable"
                                    else -> "Necesita ayuda"
                                }, style = MaterialTheme.typography.bodyMedium, color = muted())
                            }
                        }
                    }
                }
                Spacer(Modifier.height(24.dp))
                PillButton(if (busy) "Optimizando…" else "Optimizar", Modifier.fillMaxWidth(0.72f), enabled = !busy, icon = Icons.Outlined.AutoAwesome) { optimize() }
                AnimatedVisibility(result != null, enter = fadeIn() + expandVertically()) {
                    Text(result.orEmpty(), style = MaterialTheme.typography.bodySmall, color = Good,
                        textAlign = TextAlign.Center, modifier = Modifier.padding(top = 12.dp))
                }
            }
        }

        if (!a.usageAccess) item {
            Spacer(Modifier.height(16.dp))
            Block(onClick = { onAction(Action.USAGE_PERM) }) {
                Text("Activa el análisis completo", style = MaterialTheme.typography.titleMedium)
                Text("Toca aquí para ver qué apps gastan tu batería e internet.", style = MaterialTheme.typography.bodySmall, color = muted())
            }
        }

        item {
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Tile(Modifier.weight(1f), "Batería", "${a.battery.level}%", a.battery.level / 100f,
                    if (a.battery.level > 20) Good else Bad, if (a.battery.charging) "Cargando" else "${a.battery.temperatureC}°C") { onAction(Action.BATTERY) }
                Tile(Modifier.weight(1f), "Almacenamiento", Fmt.pct(a.storage.usedFraction), a.storage.usedFraction,
                    levelColor(a.storage.usedFraction), "${Fmt.bytes(a.storage.freeBytes)} libres") { onAction(Action.CLEANER) }
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Tile(Modifier.weight(1f), "Memoria", Fmt.pct(a.memory.usedFraction), a.memory.usedFraction,
                    levelColor(a.memory.usedFraction), "${Fmt.bytes(a.memory.availBytes)} libres") { onAction(Action.BOOST) }
                val c = a.connection
                Tile(Modifier.weight(1f), c.type, c.signalQuality ?: if (c.validated) "En línea" else "Sin red",
                    c.wifiRssi?.let { ((it + 100) / 60f).coerceIn(0f, 1f) } ?: if (c.validated) 1f else 0f,
                    when (c.signalQuality) { "Excelente", "Buena" -> Good; "Regular" -> Warn; "Débil" -> Bad; else -> if (c.validated) Good else Bad },
                    c.wifiBand ?: if (a.usageAccess) "Pantalla ${Fmt.duration(a.screenOnMs)}" else "") { onAction(Action.NETWORK) }
            }
        }

        item {
            SectionLabel("Sugerencias", Modifier.padding(top = 12.dp))
            Block(padding = 14.dp) {
                val tips = if (showAllTips) a.tips else a.tips.take(4)
                tips.forEach { t -> TipRow(t, if (t.action != Action.NONE) ({ onAction(t.action) }) else null) }
                if (a.tips.size > 4) TextButton(onClick = { showAllTips = !showAllTips }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                    Text(if (showAllTips) "Mostrar menos" else "Ver ${a.tips.size - 4} más")
                }
            }
        }

        if (a.topDrain.isNotEmpty()) item {
            SectionLabel("Gasta más batería · 24 h", trailing = { TextButton(onClick = { onAction(Action.BATTERY) }) { Text("Todo") } })
            Block(padding = 16.dp) {
                a.topDrain.take(3).forEach { d -> AppBar(d.app.pkg, d.app.label, "${d.sharePercent.toInt()}%", d.sharePercent / 100f, Warn) }
            }
        }
        if (a.topData.isNotEmpty()) item {
            SectionLabel("Usa más internet · 24 h", trailing = { TextButton(onClick = { onAction(Action.NETWORK) }) { Text("Todo") } })
            Block(padding = 16.dp) {
                val max = a.topData.first().net.totalBytes.coerceAtLeast(1)
                a.topData.take(3).forEach { r -> AppBar(r.pkg, r.label, Fmt.bytes(r.net.totalBytes), r.net.totalBytes.toFloat() / max, MaterialTheme.colorScheme.primary) }
            }
        }
    }
}

/** Se conserva entre pestañas para que Inicio aparezca al instante. */
private var lastAnalysis: Analysis? = null

@Composable
private fun Tile(modifier: Modifier, label: String, value: String, fraction: Float, color: Color, sub: String, onClick: () -> Unit) {
    Block(modifier, onClick = onClick, padding = 18.dp) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = muted(), maxLines = 1)
        Spacer(Modifier.height(6.dp))
        Text(value, style = MaterialTheme.typography.headlineMedium, maxLines = 1)
        Spacer(Modifier.height(12.dp))
        ThinBar(fraction, color)
        Spacer(Modifier.height(8.dp))
        Text(sub, style = MaterialTheme.typography.labelMedium, color = muted(), maxLines = 1)
    }
}

@Composable
fun AppBar(pkg: String?, label: String, value: String, fraction: Float, color: Color, sub: String? = null, onClick: (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)
        .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier), verticalAlignment = Alignment.CenterVertically) {
        AppIcon(pkg, 38.dp)
        Spacer(Modifier.width(14.dp))
        Box(Modifier.weight(1f)) { BarRow(label, value, fraction, color, sub) }
    }
}

