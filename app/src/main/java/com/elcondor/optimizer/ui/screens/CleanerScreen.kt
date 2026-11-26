package com.elcondor.optimizer.ui.screens

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.storage.StorageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.elcondor.optimizer.data.*
import com.elcondor.optimizer.ui.components.*
import com.elcondor.optimizer.ui.theme.Good
import com.elcondor.optimizer.ui.theme.Warn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CleanerScreen(modifier: Modifier, refresh: Int, onAction: (Action) -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val files = remember(refresh) { Perms.hasAllFiles() }
    val usage = remember(refresh) { Perms.hasUsageAccess(ctx) }
    var result by remember { mutableStateOf(JunkScanner.lastResult) }
    var scanning by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf("") }
    val selected = remember { mutableStateMapOf<String, JunkItem>() }
    val expanded = remember { mutableStateMapOf<JunkType, Boolean>() }
    var confirm by remember { mutableStateOf(false) }
    var cache by remember { mutableStateOf<Long?>(null) }
    var storage by remember { mutableStateOf(DeviceInfo.storage(ctx)) }
    var cacheRefresh by remember { mutableIntStateOf(0) }

    LaunchedEffect(refresh, cacheRefresh) {
        storage = DeviceInfo.storage(ctx)
        cache = if (usage) withContext(Dispatchers.IO) { AppsRepo.totalCache(ctx) } else null
    }
    val clearCache = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
        Toast.makeText(ctx, if (r.resultCode == Activity.RESULT_OK) "Caché de apps eliminada" else "Cancelado", Toast.LENGTH_SHORT).show()
        cacheRefresh++
    }

    fun preselect(r: JunkResult) {
        selected.clear()
        JunkType.entries.filter { it.safe }.forEach { t -> r.items[t].orEmpty().forEach { selected[it.file.absolutePath] = it } }
    }
    LaunchedEffect(result) { result?.let { if (selected.isEmpty()) preselect(it) } }

    Column(modifier.fillMaxSize()) {
        TopAppBar(title = { Text("Limpieza", fontWeight = FontWeight.Bold) })
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                SectionCard(title = "Almacenamiento", icon = Icons.Filled.Storage) {
                    MetricBar("Usado ${Fmt.bytes(storage.usedBytes)} de ${Fmt.bytes(storage.totalBytes)}", Fmt.pct(storage.usedFraction), storage.usedFraction)
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(onClick = { Nav.storage(ctx) }) { Text("Ver por categorías") }
                }
            }
            item {
                SectionCard(title = "Caché de las apps", icon = Icons.Filled.Cached) {
                    when {
                        !usage -> Text("Concede 'Acceso a datos de uso' para medir la caché.", style = MaterialTheme.typography.bodySmall)
                        cache == null -> Text("Calculando…")
                        else -> Text(Fmt.bytes(cache!!), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = Warn)
                    }
                    Text("Archivos temporales que las apps guardan. Se pueden borrar sin perder tus datos ni sesiones.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                    Button(enabled = files && Build.VERSION.SDK_INT >= 30, onClick = {
                        try { clearCache.launch(Intent(StorageManager.ACTION_CLEAR_APP_CACHE)) }
                        catch (_: Exception) { Nav.storage(ctx) }
                    }) { Icon(Icons.Filled.DeleteSweep, null); Spacer(Modifier.width(8.dp)); Text("Limpiar caché de todas las apps") }
                }
            }
            if (!files) item {
                PermissionGate("Acceso a todos los archivos",
                    "Para buscar archivos basura, duplicados, APKs y archivos grandes necesito este permiso. Nada se borra sin tu confirmación.",
                    "Conceder acceso") { Perms.openAllFiles(ctx) }
            } else item {
                SectionCard(title = "Archivos basura", icon = Icons.Filled.CleaningServices) {
                    if (scanning) { LinearProgressIndicator(Modifier.fillMaxWidth()); Spacer(Modifier.height(6.dp))
                        Text(progress, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                    else {
                        result?.let { r ->
                            Text("Encontrado: ${Fmt.bytes(r.totalBytes)}", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                            Text("${r.scannedFiles} archivos analizados en ${r.durationMs / 1000} s", style = MaterialTheme.typography.labelSmall)
                            Spacer(Modifier.height(8.dp))
                        }
                        Button(onClick = {
                            scanning = true
                            scope.launch {
                                val r = withContext(Dispatchers.IO) { JunkScanner.scan { n, p -> progress = "$n archivos · $p" } }
                                selected.clear(); preselect(r); result = r; scanning = false
                            }
                        }) { Icon(Icons.Filled.Search, null); Spacer(Modifier.width(8.dp)); Text(if (result == null) "Escanear teléfono" else "Volver a escanear") }
                    }
                }
            }
            result?.takeIf { files && !scanning }?.let { r ->
                JunkType.entries.forEach { t ->
                    val list = r.items[t].orEmpty()
                    if (list.isEmpty()) return@forEach
                    item(key = t.name) {
                        val nSel = list.count { selected.containsKey(it.file.absolutePath) }
                        val state = when (nSel) { 0 -> ToggleableState.Off; list.size -> ToggleableState.On; else -> ToggleableState.Indeterminate }
                        SectionCard {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { expanded[t] = !(expanded[t] ?: false) }) {
                                TriStateCheckbox(state, onClick = {
                                    if (state == ToggleableState.On) list.forEach { selected.remove(it.file.absolutePath) }
                                    else list.forEach { selected[it.file.absolutePath] = it }
                                })
                                Column(Modifier.weight(1f)) {
                                    Text(t.title, fontWeight = FontWeight.SemiBold)
                                    Text("${list.size} elementos · ${t.description}", style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
                                }
                                Text(Fmt.bytes(r.size(t)), fontWeight = FontWeight.Bold, color = if (t.safe) Good else Warn)
                                Icon(if (expanded[t] == true) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore, null)
                            }
                            if (expanded[t] == true) {
                                list.take(100).forEach { it ->
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Checkbox(selected.containsKey(it.file.absolutePath), onCheckedChange = { c ->
                                            if (c) selected[it.file.absolutePath] = it else selected.remove(it.file.absolutePath)
                                        })
                                        Column(Modifier.weight(1f)) {
                                            Text(it.file.name, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            Text(it.note ?: it.file.parent.orEmpty().substringAfter("/emulated/0"), style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        }
                                        Text(Fmt.bytes(it.size), style = MaterialTheme.typography.labelMedium)
                                    }
                                }
                                if (list.size > 100) Text("…y ${list.size - 100} más", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
                item {
                    val bytes = selected.values.sumOf { it.size }
                    Button(onClick = { confirm = true }, enabled = selected.isNotEmpty(), modifier = Modifier.fillMaxWidth().height(52.dp)) {
                        Icon(Icons.Filled.Delete, null); Spacer(Modifier.width(8.dp))
                        Text("Limpiar ${selected.size} elementos (${Fmt.bytes(bytes)})")
                    }
                }
            }
        }
    }

    if (confirm) AlertDialog(
        onDismissRequest = { confirm = false },
        title = { Text("¿Eliminar archivos?") },
        text = { Text("Se eliminarán ${selected.size} elementos (${Fmt.bytes(selected.values.sumOf { it.size })}). Esta acción no se puede deshacer.") },
        confirmButton = {
            TextButton(onClick = {
                confirm = false
                val items = selected.values.toList()
                scope.launch {
                    val (n, b) = withContext(Dispatchers.IO) { JunkScanner.delete(items) }
                    Toast.makeText(ctx, "Eliminados $n elementos · ${Fmt.bytes(b)} liberados", Toast.LENGTH_LONG).show()
                    selected.clear(); result = null; storage = DeviceInfo.storage(ctx)
                }
            }) { Text("Eliminar") }
        },
        dismissButton = { TextButton(onClick = { confirm = false }) { Text("Cancelar") } },
    )
}
