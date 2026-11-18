package dev.darknet.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import dev.darknet.ShizukuState

/** First-run guide: what DarkNet does, getting Shizuku ready, and two tips. */
@Composable
fun Onboarding(shizuku: ShizukuState, onRequestShizuku: () -> Unit, onDone: () -> Unit) {
    var step by rememberSaveable { mutableIntStateOf(0) }
    val context = LocalContext.current
    val last = 2

    Column(Modifier.fillMaxSize().padding(24.dp).navigationBarsPadding()) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            for (i in 0..last) {
                Box(
                    Modifier.height(4.dp).width(if (i == step) 28.dp else 12.dp).clip(CircleShape)
                        .background(if (i <= step) Dn.Violet else Dn.Border),
                )
            }
        }
        Spacer(Modifier.height(32.dp))

        AnimatedContent(
            targetState = step,
            transitionSpec = {
                val dir = if (targetState > initialState) 1 else -1
                (slideInHorizontally { it / 4 * dir } + fadeIn()) togetherWith (slideOutHorizontally { -it / 4 * dir } + fadeOut())
            },
            modifier = Modifier.weight(1f),
            label = "onboarding",
        ) { s ->
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                when (s) {
                    0 -> Page(Icons.Filled.Hub, "Bienvenido a DarkNet",
                        "Comparte el internet de tu teléfono por hotspot a través de un túnel propio, con un motor que usa el TCP de Android para ir más rápido. No necesitas root.")
                    1 -> {
                        Page(Icons.Filled.Key, "Conecta Shizuku",
                            "DarkNet usa Shizuku para controlar el hotspot con permisos del sistema. Se activa una vez con la depuración inalámbrica.")
                        Panel {
                            Text(
                                when (shizuku) {
                                    ShizukuState.READY -> "✓ Shizuku listo y con permiso"
                                    ShizukuState.NO_PERMISSION -> "Shizuku está activo: falta darle permiso a DarkNet."
                                    ShizukuState.NOT_RUNNING -> "Shizuku está instalado pero apagado: ábrelo y pulsa Iniciar."
                                    ShizukuState.NOT_INSTALLED -> "Shizuku no está instalado."
                                },
                                style = Dn.Body.copy(color = if (shizuku == ShizukuState.READY) Dn.Green else Dn.Text),
                            )
                            when (shizuku) {
                                ShizukuState.NO_PERMISSION -> TextButton(onClick = onRequestShizuku) { Text("Conceder permiso", color = Dn.VioletSoft) }
                                ShizukuState.NOT_INSTALLED -> TextButton(onClick = {
                                    runCatching {
                                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=moe.shizuku.privileged.api")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                                    }
                                }) { Text("Instalar Shizuku", color = Dn.VioletSoft) }
                                ShizukuState.NOT_RUNNING -> TextButton(onClick = {
                                    context.packageManager.getLaunchIntentForPackage("moe.shizuku.privileged.api")?.let { runCatching { context.startActivity(it) } }
                                }) { Text("Abrir Shizuku", color = Dn.VioletSoft) }
                                ShizukuState.READY -> Unit
                            }
                        }
                    }
                    else -> {
                        Page(Icons.Filled.Wifi, "Dos consejos",
                            "Para la mejor velocidad, pon tu hotspot en 5 GHz: Ajustes → Conexiones → Mobile Hotspot → Configurar → Banda.")
                        Panel {
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.CheckCircle, null, tint = Dn.Green)
                                Text("Si algo falla, Ajustes → Ver registro dice exactamente dónde.", style = Dn.Body)
                            }
                        }
                    }
                }
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            if (step > 0) TextButton(onClick = { step-- }) { Text("Atrás", color = Dn.Muted) }
            Spacer(Modifier.weight(1f))
            Button(
                onClick = { if (step < last) step++ else onDone() },
                colors = ButtonDefaults.buttonColors(containerColor = Dn.Violet),
            ) { Text(if (step < last) "Siguiente" else "Empezar", color = Dn.Background) }
        }
    }
}

@Composable
private fun Page(icon: ImageVector, title: String, body: String) {
    Box(Modifier.size(64.dp).clip(CircleShape).background(Dn.Violet.copy(alpha = 0.16f)), contentAlignment = Alignment.Center) {
        Icon(icon, null, tint = Dn.VioletSoft, modifier = Modifier.size(32.dp))
    }
    Text(title, style = Dn.Title)
    Text(body, style = Dn.Body.copy(color = Dn.Muted))
}
