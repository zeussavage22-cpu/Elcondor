package dev.darknet.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.darknet.ShizukuState
import dev.darknet.data.Bytes
import dev.darknet.data.ShareStatus
import kotlinx.coroutines.delay

@Composable
fun HomeScreen(
    status: ShareStatus,
    shizuku: ShizukuState,
    fastEngineEnabled: Boolean,
    onToggle: () -> Unit,
    onFixShizuku: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("DarkNet", style = Dn.Title)
            Spacer(Modifier.weight(1f))
            StatusChip(shizuku)
        }

        if (shizuku != ShizukuState.READY) ShizukuCard(shizuku, onFixShizuku)

        PowerButton(status = status, enabled = shizuku == ShizukuState.READY, onClick = onToggle)

        if (status.state == ShareStatus.State.ERROR) {
            Panel {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Filled.Warning, null, tint = Dn.Red)
                    Text("No se pudo compartir", style = Dn.Heading)
                }
                Text(status.detail, style = Dn.Small)
                Text("Ajustes → Ver registro tiene los detalles.", style = Dn.Small)
            }
        }

        if (status.isActive) {
            LiveStats(status)
            EngineCard(status, fastEngineEnabled)
        }
    }
}

@Composable
private fun StatusChip(shizuku: ShizukuState) {
    val (text, color) = when (shizuku) {
        ShizukuState.READY -> "Shizuku listo" to Dn.Green
        ShizukuState.NO_PERMISSION -> "Falta permiso" to Dn.Amber
        ShizukuState.NOT_RUNNING -> "Shizuku apagado" to Dn.Amber
        ShizukuState.NOT_INSTALLED -> "Sin Shizuku" to Dn.Red
    }
    Text(
        text,
        style = Dn.Small.copy(color = color, fontWeight = FontWeight.SemiBold),
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 10.dp, vertical = 6.dp),
    )
}

@Composable
private fun ShizukuCard(state: ShizukuState, onFix: () -> Unit) {
    Panel {
        Text("Falta un paso", style = Dn.Heading)
        Text(
            when (state) {
                ShizukuState.NOT_INSTALLED -> "DarkNet usa Shizuku para controlar el hotspot sin root. Instálalo desde Play Store o GitHub."
                ShizukuState.NOT_RUNNING -> "Abre Shizuku y actívalo (por depuración inalámbrica)."
                ShizukuState.NO_PERMISSION -> "Dale permiso a DarkNet en Shizuku."
                ShizukuState.READY -> ""
            },
            style = Dn.Small,
        )
        if (state == ShizukuState.NO_PERMISSION) {
            Button(onClick = onFix, colors = ButtonDefaults.buttonColors(containerColor = Dn.Violet)) {
                Text("Conceder permiso", color = Dn.Background)
            }
        }
    }
}

@Composable
private fun PowerButton(status: ShareStatus, enabled: Boolean, onClick: () -> Unit) {
    val haptics = LocalHapticFeedback.current
    val pulse by rememberInfiniteTransition(label = "pulse").animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2200, easing = LinearEasing), RepeatMode.Restart),
        label = "pulseValue",
    )
    val spin by rememberInfiniteTransition(label = "spin").animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(1100, easing = LinearEasing)),
        label = "spinValue",
    )
    val active = status.isActive
    val busy = status.isBusy

    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(230.dp)
                .clickable(
                    enabled = enabled && !busy,
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                ) {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onClick()
                },
            contentAlignment = Alignment.Center,
        ) {
            Canvas(Modifier.fillMaxSize()) {
                val r = size.minDimension / 2
                if (active) {
                    for (wave in 0..1) {
                        val p = (pulse + wave * 0.5f) % 1f
                        drawCircle(Dn.Cyan.copy(alpha = (1 - p) * 0.16f), radius = r * (0.62f + p * 0.38f))
                    }
                }
                drawCircle(
                    Brush.radialGradient(
                        listOf((if (active) Dn.Violet else Dn.Border).copy(alpha = 0.35f), Color.Transparent),
                        center = center, radius = r,
                    ),
                    radius = r,
                )
                val stroke = 5.dp.toPx()
                val inset = r * 0.38f
                val arcSize = androidx.compose.ui.geometry.Size(size.width - inset * 2, size.height - inset * 2)
                val topLeft = Offset(inset, inset)
                drawArc(Dn.Border, 0f, 360f, false, topLeft, arcSize, style = Stroke(stroke))
                when {
                    busy -> drawArc(
                        Brush.sweepGradient(listOf(Color.Transparent, Dn.Cyan)), spin, 120f, false,
                        topLeft, arcSize, style = Stroke(stroke, cap = StrokeCap.Round),
                    )
                    active -> drawArc(
                        Brush.sweepGradient(listOf(Dn.Violet, Dn.Cyan, Dn.Violet)), 0f, 360f, false,
                        topLeft, arcSize, style = Stroke(stroke),
                    )
                }
            }
            Box(
                modifier = Modifier
                    .size(128.dp)
                    .clip(CircleShape)
                    .background(if (active) Dn.Violet.copy(alpha = 0.18f) else Dn.Surface),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.PowerSettingsNew,
                    contentDescription = if (active) "Dejar de compartir" else "Empezar a compartir",
                    tint = when {
                        !enabled -> Dn.Muted
                        active -> Dn.Cyan
                        else -> Dn.VioletSoft
                    },
                    modifier = Modifier.size(56.dp),
                )
            }
        }

        Text(
            when (status.state) {
                ShareStatus.State.ACTIVE -> "Compartiendo"
                ShareStatus.State.STARTING -> "Iniciando…"
                ShareStatus.State.STOPPING -> "Deteniendo…"
                ShareStatus.State.ERROR -> "Toca para reintentar"
                ShareStatus.State.IDLE -> "Toca para compartir"
            },
            style = Dn.Heading.copy(fontSize = 20.sp),
        )
        if (active) Elapsed(status.since)
    }
}

@Composable
private fun Elapsed(since: Long) {
    val now = remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(since) {
        while (true) {
            now.longValue = System.currentTimeMillis()
            delay(1000)
        }
    }
    if (since > 0) Text(Bytes.duration(now.longValue - since), style = Dn.Small)
}

@Composable
private fun LiveStats(status: ShareStatus) {
    Panel {
        Row {
            SectionLabel("Velocidad ahora", Modifier.weight(1f))
            Text(Bytes.rate(status.speed), style = Dn.Small.copy(color = Dn.Cyan, fontWeight = FontWeight.SemiBold))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Stat("Bajada", Bytes.format(status.bytesDown), Modifier.weight(1f))
            Stat("Subida", Bytes.format(status.bytesUp), Modifier.weight(1f))
            Stat("Equipos", status.devices.size.toString(), Modifier.weight(1f))
        }
    }
}

@Composable
fun Stat(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, style = Dn.Small)
        Text(value, style = Dn.Value)
    }
}

@Composable
private fun EngineCard(status: ShareStatus, fastEngineEnabled: Boolean) {
    Panel {
        StatusLine(
            icon = Icons.Filled.Bolt,
            tint = if (status.fastPath) Dn.Cyan else Dn.Muted,
            title = when {
                status.fastPath -> "Motor rápido activo"
                fastEngineEnabled -> "Motor de respaldo"
                else -> "Motor estándar"
            },
            subtitle = when {
                status.fastPath -> "El TCP de Android lleva las conexiones."
                fastEngineEnabled -> "Tu teléfono no permitió el motor rápido; todo va por el motor de respaldo."
                else -> "El motor rápido está desactivado en Ajustes."
            },
        )
        StatusLine(
            icon = Icons.Filled.Shield,
            tint = if (status.upstreamOk) Dn.Green else Dn.Amber,
            title = if (status.upstreamOk) "Salida por DarkNet" else "Comprobando la salida…",
            subtitle = if (status.upstreamOk) "El hotspot usa el túnel de DarkNet." else "Si no cambia, el hotspot puede estar usando los datos directamente.",
        )
    }
}

@Composable
fun StatusLine(icon: ImageVector, tint: Color, title: String, subtitle: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = Dn.Body.copy(fontWeight = FontWeight.SemiBold))
            Text(subtitle, style = Dn.Small)
        }
    }
}

@Composable
fun Spacer16() = Spacer(Modifier.height(16.dp))
