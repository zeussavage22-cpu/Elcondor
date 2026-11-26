package com.elcondor.optimizer.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.elcondor.optimizer.data.Nav
import com.elcondor.optimizer.data.Pro
import com.elcondor.optimizer.ui.components.AppIcon
import com.elcondor.optimizer.ui.components.NavRow
import com.elcondor.optimizer.ui.components.muted
import com.elcondor.optimizer.ui.theme.Bad
import com.elcondor.optimizer.ui.theme.Warn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Hoja inferior con acciones sobre una app. Las acciones Pro requieren Shizuku. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppSheet(pkg: String, label: String, system: Boolean, onDismiss: () -> Unit, onChanged: () -> Unit = {}) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var restricted by remember { mutableStateOf<Boolean?>(null) }
    var enabled by remember { mutableStateOf(true) }
    var confirmDisable by remember { mutableStateOf(false) }
    val canDisable = pkg !in Pro.protectedPkgs
    val isSystem = remember(pkg) {
        system || try {
            val f = ctx.packageManager.getApplicationInfo(pkg, android.content.pm.PackageManager.MATCH_DISABLED_COMPONENTS).flags
            (f and android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0 && (f and android.content.pm.ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) == 0
        } catch (_: Exception) { false }
    }

    LaunchedEffect(pkg) {
        enabled = try { ctx.packageManager.getApplicationInfo(pkg, 0).enabled } catch (_: Exception) { true }
        if (Pro.ready) restricted = withContext(Dispatchers.IO) { Pro.isRestricted(pkg) }
    }
    fun act(msg: String, block: () -> Boolean) {
        scope.launch {
            val ok = withContext(Dispatchers.IO) { block() }
            Toast.makeText(ctx, if (ok) msg else "No se pudo completar", Toast.LENGTH_SHORT).show()
            onChanged()
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.surfaceContainer) {
        Column(Modifier.padding(start = 20.dp, end = 20.dp, bottom = 28.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AppIcon(pkg, 52.dp)
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(label, style = MaterialTheme.typography.titleLarge)
                    Text(pkg, style = MaterialTheme.typography.labelMedium, color = muted())
                }
            }
            Spacer(Modifier.height(14.dp))
            if (Pro.ready) {
                NavRow(Icons.Outlined.StopCircle, "Detener ahora", "Cierra la app y sus servicios en segundo plano") {
                    act("$label detenida") { Pro.forceStop(pkg) }
                }
                val r = restricted
                NavRow(Icons.Outlined.Bedtime, if (r == true) "Quitar restricción" else "Restringir segundo plano",
                    if (r == true) "Volverá a funcionar en segundo plano" else "No podrá gastar batería ni datos sin que la abras",
                    value = when (r) { true -> "Restringida"; false -> null; null -> "…" }, valueColor = Warn) {
                    act(if (r == true) "Restricción quitada" else "$label restringida") { Pro.restrictBackground(pkg, r != true) }
                    restricted = r != true
                }
                if (canDisable) NavRow(Icons.Outlined.AcUnit, if (enabled) "Congelar app" else "Descongelar app",
                    if (enabled) "La desactiva por completo (no se borra nada)" else "Vuelve a activarla",
                    value = if (!enabled) "Congelada" else null, valueColor = Bad) {
                    if (enabled) confirmDisable = true
                    else { act("$label activada") { Pro.setEnabled(pkg, true) }; enabled = true }
                }
            } else {
                Text("Activa el Modo Pro (Shizuku) para detener, restringir o congelar apps.",
                    style = MaterialTheme.typography.bodySmall, color = muted(), modifier = Modifier.padding(vertical = 8.dp))
            }
            NavRow(Icons.Outlined.Info, "Información de la app", "Permisos, almacenamiento, batería") { Nav.appDetails(ctx, pkg) }
            if (!isSystem) NavRow(Icons.Outlined.DeleteOutline, "Desinstalar", null) { Nav.uninstall(ctx, pkg); onDismiss() }
        }
    }

    if (confirmDisable) AlertDialog(
        onDismissRequest = { confirmDisable = false },
        title = { Text("¿Congelar $label?") },
        text = { Text("La app desaparecerá y no funcionará hasta que la descongeles desde Cóndor › Apps › filtro \"Congeladas\". Si es una app del sistema, podría afectar funciones del teléfono.") },
        confirmButton = { TextButton(onClick = {
            confirmDisable = false; enabled = false
            act("$label congelada") { Pro.setEnabled(pkg, false) }
        }) { Text("Congelar") } },
        dismissButton = { TextButton(onClick = { confirmDisable = false }) { Text("Cancelar") } },
    )
}
