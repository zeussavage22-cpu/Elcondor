package com.elcondor.app.ui.display

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.elcondor.app.display.DisplayLimits
import com.elcondor.app.perapp.LaunchableApp
import com.elcondor.app.perapp.PerAppDpiManager
import com.elcondor.app.perapp.PerAppUiState

/**
 * Tarjeta "DPI por app": interruptor de detección, lista de reglas y alta de reglas.
 */
@Composable
fun PerAppDpiCard(
    state: PerAppUiState,
    manager: PerAppDpiManager,
    shizukuReady: Boolean,
    physicalDpi: Int?,
) {
    var showPicker by rememberSaveable { mutableStateOf(false) }

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("DPI por app", style = MaterialTheme.typography.titleMedium)
                    Text(
                        if (state.detectionActive) "Detección activa" else "Detección desactivada",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (state.detectionActive) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (state.isBusy) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                Switch(
                    checked = state.detectionActive,
                    onCheckedChange = manager::setDetectionEnabled,
                    enabled = shizukuReady && !state.isBusy,
                )
            }

            Text(
                "Android no permite DPI por app sin root. Al abrir una app con regla se cambia el DPI " +
                    "de todo el sistema y al salir se restaura el anterior. Usa un servicio de " +
                    "accesibilidad solo para saber qué app está en primer plano. Al cambiar, la app " +
                    "puede parpadear o recargarse.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (state.rules.isEmpty()) {
                Text("Sin reglas todavía.", style = MaterialTheme.typography.bodyMedium)
            } else {
                state.rules.forEach { rule ->
                    ListItem(
                        headlineContent = { Text(rule.label) },
                        supportingContent = {
                            Text(
                                rule.packageName + if (rule.packageName == state.activePackage) " · aplicada ahora" else "",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        },
                        trailingContent = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("${rule.dpi}", style = MaterialTheme.typography.titleMedium)
                                IconButton(onClick = { manager.removeRule(rule.packageName) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Quitar regla de ${rule.label}")
                                }
                            }
                        },
                    )
                    HorizontalDivider()
                }
            }

            OutlinedButton(onClick = { showPicker = true }) { Text("Añadir app") }
        }
    }

    if (showPicker) {
        AddRuleDialog(
            manager = manager,
            physicalDpi = physicalDpi,
            onDismiss = { showPicker = false },
        )
    }
}

/** Paso 1: elegir app. Paso 2: elegir DPI. */
@Composable
private fun AddRuleDialog(manager: PerAppDpiManager, physicalDpi: Int?, onDismiss: () -> Unit) {
    var apps by remember { mutableStateOf<List<LaunchableApp>?>(null) }
    var query by rememberSaveable { mutableStateOf("") }
    var selected by remember { mutableStateOf<LaunchableApp?>(null) }
    var dpiInput by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(Unit) { apps = manager.loadLaunchableApps() }

    val app = selected
    if (app == null) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Elige una app") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        placeholder = { Text("Buscar") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    val list = apps
                    Box(Modifier.heightIn(min = 120.dp, max = 400.dp)) {
                        if (list == null) {
                            CircularProgressIndicator(Modifier.align(Alignment.Center))
                        } else {
                            val q = query.trim()
                            val filtered = if (q.isEmpty()) list else list.filter {
                                it.label.contains(q, true) || it.packageName.contains(q, true)
                            }
                            LazyColumn {
                                items(filtered, key = { it.packageName }) { item ->
                                    ListItem(
                                        headlineContent = { Text(item.label) },
                                        supportingContent = { Text(item.packageName, style = MaterialTheme.typography.bodySmall) },
                                        modifier = Modifier.clickable { selected = item },
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
        )
    } else {
        val dpi = dpiInput.toIntOrNull()
        val error = dpi?.let(DisplayLimits::validateDensity)
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("DPI para ${app.label}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Menos DPI = todo más pequeño (cabe más contenido). Más DPI = más grande.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    if (physicalDpi != null) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(0.8f, 0.9f, 1.1f).forEach { factor ->
                                val value = (physicalDpi * factor).toInt()
                                FilterChip(
                                    selected = dpi == value,
                                    onClick = { dpiInput = value.toString() },
                                    label = { Text("$value") },
                                )
                            }
                        }
                    }
                    OutlinedTextField(
                        value = dpiInput,
                        onValueChange = { dpiInput = it.filter(Char::isDigit).take(4) },
                        label = { Text("DPI") },
                        supportingText = { Text(error ?: "Entre ${DisplayLimits.MIN_DPI} y ${DisplayLimits.MAX_DPI}") },
                        isError = error != null,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        dpi?.let { manager.addRule(app, it) }
                        onDismiss()
                    },
                    enabled = dpi != null && error == null,
                ) { Text("Guardar") }
            },
            dismissButton = { TextButton(onClick = { selected = null }) { Text("Atrás") } },
        )
    }
}
