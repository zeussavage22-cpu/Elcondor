package com.elcondor.optimizer.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.elcondor.optimizer.data.Nav
import com.elcondor.optimizer.data.Perms
import com.elcondor.optimizer.ui.components.*
import com.elcondor.optimizer.ui.theme.Good

/**
 * Configuración guiada de permisos. Las apps instaladas fuera de Play Store tienen los
 * "ajustes restringidos" bloqueados en Android 13+, por eso se explica cómo desbloquearlos.
 */
@Composable
fun PermissionsScreen(modifier: Modifier, refresh: Int, onDone: () -> Unit) {
    val ctx = LocalContext.current
    val prefs = remember { Perms.prefs(ctx) }
    var tick by remember { mutableIntStateOf(0) }
    val usage = remember(refresh, tick) { Perms.hasUsageAccess(ctx) }
    val media = remember(refresh, tick) { Perms.hasMedia(ctx) }
    val files = remember(refresh, tick) { Perms.hasAllFiles() }
    val askedUsage = remember(refresh, tick) { prefs.getBoolean("asked_usage", false) }
    val askedFiles = remember(refresh, tick) { prefs.getBoolean("asked_files", false) }
    val askedMedia = remember(refresh, tick) { prefs.getBoolean("asked_media", false) }
    val stuck = (askedUsage && !usage) || (askedFiles && !files)
    var showHelp by remember { mutableStateOf(false) }
    LaunchedEffect(stuck) { if (stuck) showHelp = true }

    val mediaLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { tick++ }

    LazyColumn(modifier, contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 40.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Column(Modifier.statusBarsPadding().padding(start = 8.dp, top = 36.dp, bottom = 16.dp, end = 8.dp)) {
                Text("Configura\nCóndor", style = MaterialTheme.typography.displayMedium)
                Spacer(Modifier.height(10.dp))
                Text("Tres accesos para analizar todo tu teléfono. Todo se queda en tu S25 Ultra: nada se sube a internet.",
                    style = MaterialTheme.typography.bodyMedium, color = muted())
            }
        }
        item {
            PermBlock(Icons.Outlined.QueryStats, "Datos de uso", "Ver qué apps gastan batería e internet, su tamaño y caché.", usage) {
                prefs.edit().putBoolean("asked_usage", true).apply(); Perms.openUsageAccess(ctx)
            }
        }
        item {
            PermBlock(Icons.Outlined.PhotoLibrary, "Fotos, videos y audio", "Encontrar duplicados, videos pesados y capturas viejas.", media) {
                if (askedMedia && !media) Perms.openAppInfo(ctx)  // denegado antes: se activa desde Info de la app › Permisos
                else { prefs.edit().putBoolean("asked_media", true).apply(); mediaLauncher.launch(Perms.mediaPermissions()) }
            }
        }
        item {
            PermBlock(Icons.Outlined.FolderOpen, "Todos los archivos", "Opcional. Limpieza profunda: temporales, APKs, carpetas vacías.", files) {
                prefs.edit().putBoolean("asked_files", true).apply(); Perms.openAllFiles(ctx)
            }
        }
        item {
            Block(onClick = { showHelp = !showHelp }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.HelpOutline, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(12.dp))
                    Text("¿No te deja activarlo o aparece en gris?", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                }
                if (showHelp) {
                    Spacer(Modifier.height(14.dp))
                    Text("Android bloquea estos ajustes en apps instaladas fuera de Play Store (\"Ajuste restringido\"). Se desbloquea así:",
                        style = MaterialTheme.typography.bodySmall, color = muted())
                    Spacer(Modifier.height(14.dp))
                    Step(1, "Intenta activarlo una vez (toca Activar arriba). Si sale el aviso de ajuste restringido, toca Aceptar.")
                    Step(2, "Toca el botón de abajo para abrir la Información de la app.")
                    Step(3, "Arriba a la derecha toca ⋮ y elige \"Permitir ajustes restringidos\". Confirma con tu huella o PIN.")
                    Step(4, "Vuelve aquí y toca Activar otra vez. Ya no estará bloqueado.")
                    Spacer(Modifier.height(10.dp))
                    PillButton("Abrir información de la app", Modifier.fillMaxWidth(), icon = Icons.Outlined.Info) { Perms.openAppInfo(ctx) }
                    Spacer(Modifier.height(18.dp))
                    Text("¿Sigue sin dejarte?", style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(4.dp))
                    Text("Samsung Auto Blocker puede impedirlo. Desactívalo un momento en Ajustes › Seguridad y privacidad › Auto Blocker, concede los permisos y vuelve a activarlo.",
                        style = MaterialTheme.typography.bodySmall, color = muted())
                    Spacer(Modifier.height(10.dp))
                    OutlinedButton(onClick = { Nav.security(ctx) }, shape = CircleShape, modifier = Modifier.fillMaxWidth().height(50.dp)) { Text("Abrir Seguridad y privacidad") }
                }
            }
        }
        item {
            Spacer(Modifier.height(12.dp))
            PillButton(if (usage && media) "Empezar" else "Continuar así", Modifier.fillMaxWidth(), onClick = onDone)
            if (!usage || !media) Text("Puedes activarlos después desde Herramientas › Permisos.",
                style = MaterialTheme.typography.labelMedium, color = muted(), modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        }
    }
}

@Composable
private fun PermBlock(icon: ImageVector, title: String, text: String, granted: Boolean, onGrant: () -> Unit) {
    Block {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(46.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceContainerHigh), contentAlignment = Alignment.Center) {
                Icon(icon, null, Modifier.size(22.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(text, style = MaterialTheme.typography.bodySmall, color = muted())
            }
            Spacer(Modifier.width(10.dp))
            if (granted) Box(Modifier.size(36.dp).clip(CircleShape).background(Good.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.Check, "Concedido", tint = Good, modifier = Modifier.size(20.dp))
            } else FilledTonalButton(onClick = onGrant, shape = CircleShape) { Text("Activar") }
        }
    }
}

@Composable
private fun Step(n: Int, text: String) {
    Row(Modifier.padding(vertical = 6.dp), verticalAlignment = Alignment.Top) {
        Box(Modifier.size(26.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
            Text("$n", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
        }
        Spacer(Modifier.width(12.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 3.dp))
    }
}
