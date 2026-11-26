package com.elcondor.optimizer.ui.screens

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.storage.StorageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.elcondor.optimizer.data.*
import com.elcondor.optimizer.ui.components.*
import com.elcondor.optimizer.ui.theme.Good
import com.elcondor.optimizer.ui.theme.Warn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun CleanerScreen(modifier: Modifier, refresh: Int, onAction: (Action) -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val files = remember(refresh) { Perms.hasAllFiles() }
    val media = remember(refresh) { Perms.hasMedia(ctx) }
    val usage = remember(refresh) { Perms.hasUsageAccess(ctx) }
    var result by remember { mutableStateOf(JunkScanner.lastResult) }
    var scanning by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf("") }
    val selected = remember { mutableStateMapOf<String, JunkItem>() }
    val expanded = remember { mutableStateMapOf<JunkType, Boolean>() }
    var confirm by remember { mutableStateOf(false) }
    var cache by remember { mutableStateOf<Long?>(null) }
    var storage by remember { mutableStateOf(DeviceInfo.storage(ctx)) }
    var reload by remember { mutableIntStateOf(0) }
    var pendingMediaBytes by remember { mutableLongStateOf(0) }
    var directDone by remember { mutableStateOf(0 to 0L) }

    LaunchedEffect(refresh, reload) {
        storage = DeviceInfo.storage(ctx)
        cache = if (usage) withContext(Dispatchers.IO) { AppsRepo.totalCache(ctx) } else null
    }
    val clearCache = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
        Toast.makeText(ctx, if (r.resultCode == Activity.RESULT_OK) "Caché eliminada" else "Cancelado", Toast.LENGTH_SHORT).show()
        reload++
    }
    val mediaDelete = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { r ->
        val extra = if (r.resultCode == Activity.RESULT_OK) pendingMediaBytes else 0L
        val (n, b) = directDone
        Toast.makeText(ctx, "${Fmt.bytes(b + extra)} liberados", Toast.LENGTH_LONG).show()
        directDone = 0 to 0L; pendingMediaBytes = 0; reload++
    }

    fun preselect(r: JunkResult) {
        selected.clear()
        JunkType.entries.filter { it.safe }.forEach { t -> r.items[t].orEmpty().forEach { selected[it.id] = it } }
    }
    fun scan() {
        scanning = true
        scope.launch {
            val r = withContext(Dispatchers.IO) { JunkScanner.scan(ctx) { n, _ -> progress = "$n archivos revisados" } }
            preselect(r); result = r; scanning = false
        }
    }
    LaunchedEffect(result) { result?.let { if (selected.isEmpty()) preselect(it) } }

    LazyColumn(modifier, contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = BottomSpace)) {
        item { Header("Limpiar", "${Fmt.bytes(storage.freeBytes)} libres de ${Fmt.bytes(storage.totalBytes)}") }
        item {
            Block {
                val r = result
                Text(if (r == null) "Almacenamiento" else "Basura encontrada", style = MaterialTheme.typography.labelMedium, color = muted())
                Text(if (r == null) Fmt.pct(storage.usedFraction) else Fmt.bytes(r.totalBytes), style = MaterialTheme.typography.displayMedium,
                    color = if (r == null) levelColor(storage.usedFraction) else Warn)
                Spacer(Modifier.height(10.dp))
                ThinBar(storage.usedFraction, levelColor(storage.usedFraction))
                Spacer(Modifier.height(18.dp))
                when {
                    !media && !files -> Locked("Permite el acceso a fotos y archivos para buscar basura, duplicados y archivos pesados.") { onAction(Action.FILES_PERM) }
                    scanning -> {
                        LinearProgressIndicator(Modifier.fillMaxWidth())
                        Text(progress, style = MaterialTheme.typography.labelMedium, color = muted(), modifier = Modifier.padding(top = 8.dp))
                    }
                    else -> PillButton(if (r == null) "Buscar basura" else "Buscar de nuevo", Modifier.fillMaxWidth(), icon = Icons.Outlined.Search) { scan() }
                }
                if (media && !files) Text("Modo fotos y videos. Activa \"Todos los archivos\" para una limpieza más profunda.",
                    style = MaterialTheme.typography.labelMedium, color = muted(),
                    modifier = Modifier.padding(top = 10.dp).clickable { onAction(Action.FILES_PERM) })
            }
        }
        item {
            SectionLabel("Caché de apps", Modifier.padding(top = 12.dp))
            Block(padding = 18.dp) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(cache?.let { Fmt.bytes(it) } ?: if (usage) "Calculando…" else "—", style = MaterialTheme.typography.headlineMedium)
                        Text("Se borra sin perder tus datos ni sesiones", style = MaterialTheme.typography.bodySmall, color = muted())
                    }
                }
                Spacer(Modifier.height(14.dp))
                if (Pro.ready) FilledTonalButton(onClick = {
                    scope.launch {
                        val b = withContext(Dispatchers.IO) { Pro.trimCaches(ctx) }
                        Toast.makeText(ctx, "${Fmt.bytes(b)} de caché eliminados", Toast.LENGTH_LONG).show(); reload++
                    }
                }, shape = CircleShape, modifier = Modifier.fillMaxWidth().height(50.dp)) { Icon(Icons.Outlined.DeleteSweep, null); Spacer(Modifier.width(8.dp)); Text("Limpiar toda la caché (Pro)") }
                else if (files && Build.VERSION.SDK_INT >= 30) FilledTonalButton(onClick = {
                    try { clearCache.launch(Intent(StorageManager.ACTION_CLEAR_APP_CACHE)) } catch (_: Exception) { Nav.storage(ctx) }
                }, shape = CircleShape, modifier = Modifier.fillMaxWidth().height(50.dp)) { Icon(Icons.Outlined.DeleteSweep, null); Spacer(Modifier.width(8.dp)); Text("Limpiar toda la caché") }
                else Text("Para borrar la caché de todas las apps de un toque activa \"Todos los archivos\" en Permisos. Mientras tanto, puedes borrarla app por app en la pestaña Apps.",
                    style = MaterialTheme.typography.bodySmall, color = muted())
            }
        }
        result?.takeIf { !scanning }?.let { r ->
            item { SectionLabel("Resultados") }
            JunkType.entries.forEach { t ->
                val list = r.items[t].orEmpty()
                if (list.isEmpty()) return@forEach
                item(key = t.name) {
                    val nSel = list.count { selected.containsKey(it.id) }
                    val state = when (nSel) { 0 -> ToggleableState.Off; list.size -> ToggleableState.On; else -> ToggleableState.Indeterminate }
                    Block(padding = 10.dp, modifier = Modifier.padding(bottom = 8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { expanded[t] = !(expanded[t] ?: false) }) {
                            TriStateCheckbox(state, onClick = {
                                if (state == ToggleableState.On) list.forEach { selected.remove(it.id) } else list.forEach { selected[it.id] = it }
                            })
                            Column(Modifier.weight(1f)) {
                                Text(t.title, style = MaterialTheme.typography.bodyLarge)
                                Text("${list.size} · ${t.description}", style = MaterialTheme.typography.labelMedium, color = muted(), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            Text(Fmt.bytes(r.size(t)), style = MaterialTheme.typography.titleSmall, color = if (t.safe) Good else Warn)
                            Icon(if (expanded[t] == true) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore, null, tint = muted(), modifier = Modifier.padding(horizontal = 6.dp))
                        }
                        if (expanded[t] == true) {
                            list.take(100).forEach { it ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(selected.containsKey(it.id), onCheckedChange = { c -> if (c) selected[it.id] = it else selected.remove(it.id) })
                                    Column(Modifier.weight(1f)) {
                                        Text(it.name, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text(it.note ?: it.location, style = MaterialTheme.typography.labelMedium, color = muted(), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                    Text(Fmt.bytes(it.size), style = MaterialTheme.typography.labelMedium, color = muted(), modifier = Modifier.padding(end = 10.dp))
                                }
                            }
                            if (list.size > 100) Text("…y ${list.size - 100} más", style = MaterialTheme.typography.labelMedium, color = muted(), modifier = Modifier.padding(12.dp))
                        }
                    }
                }
            }
            item {
                Spacer(Modifier.height(8.dp))
                PillButton("Limpiar ${Fmt.bytes(selected.values.sumOf { it.size })}", Modifier.fillMaxWidth(), enabled = selected.isNotEmpty(),
                    icon = Icons.Outlined.DeleteSweep) { confirm = true }
            }
        }
    }

    if (confirm) AlertDialog(
        onDismissRequest = { confirm = false },
        title = { Text("¿Eliminar ${selected.size} elementos?") },
        text = { Text("Se liberarán ${Fmt.bytes(selected.values.sumOf { it.size })}. No se puede deshacer.") },
        confirmButton = {
            TextButton(onClick = {
                confirm = false
                val items = selected.values.toList()
                scope.launch {
                    val (n, b, mediaItems) = withContext(Dispatchers.IO) { JunkScanner.delete(items) }
                    selected.clear(); result = null
                    val sender = JunkScanner.mediaDeleteRequest(ctx, mediaItems)
                    if (sender != null) {
                        directDone = n to b; pendingMediaBytes = mediaItems.sumOf { it.size }
                        mediaDelete.launch(IntentSenderRequest.Builder(sender).build())
                    } else {
                        Toast.makeText(ctx, "${Fmt.bytes(b)} liberados", Toast.LENGTH_LONG).show(); reload++
                    }
                }
            }) { Text("Eliminar") }
        },
        dismissButton = { TextButton(onClick = { confirm = false }) { Text("Cancelar") } },
    )
}
