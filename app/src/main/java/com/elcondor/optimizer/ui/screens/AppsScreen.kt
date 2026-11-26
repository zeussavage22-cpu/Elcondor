package com.elcondor.optimizer.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.elcondor.optimizer.data.*
import com.elcondor.optimizer.ui.components.*
import com.elcondor.optimizer.ui.theme.Warn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private enum class AppSort(val label: String, val needsUsage: Boolean) {
    SIZE("Pesadas", false), UNUSED("Sin usar", true), CACHE("Caché", true), USAGE("Más usadas", true), NEW("Recientes", false), FROZEN("Congeladas", false)
}

@Composable
fun AppsScreen(modifier: Modifier, refresh: Int, onAction: (Action) -> Unit) {
    val ctx = LocalContext.current
    val usage = remember(refresh) { Perms.hasUsageAccess(ctx) }
    var apps by remember { mutableStateOf<List<AppEntry>?>(null) }
    var sort by remember { mutableStateOf(AppSort.SIZE) }
    var query by remember { mutableStateOf("") }
    var showSystem by remember { mutableStateOf(false) }
    var sheet by remember { mutableStateOf<AppEntry?>(null) }
    var reload by remember { mutableIntStateOf(0) }

    LaunchedEffect(refresh, showSystem, reload) {
        apps = withContext(Dispatchers.IO) {
            AppsRepo.installed(ctx, includeSystem = showSystem).also { AppsRepo.fillUsage(ctx, it, AppsRepo.startOfToday()); AppsRepo.fillStorage(ctx, it) }
        }
    }
    val month = System.currentTimeMillis() - 30L * 86_400_000

    LazyColumn(modifier, contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = BottomSpace), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        val list = apps
        item {
            Header("Apps", list?.let {
                val unused = it.count { a -> a.lastUsed in 1 until month }
                "${it.size} instaladas · ${Fmt.bytes(it.sumOf { a -> a.totalBytes })}" + if (usage && unused > 0) " · $unused sin usar" else ""
            })
        }
        if (list == null) { item { Loading("Cargando apps…") }; return@LazyColumn }
        if (!usage) item { Block(onClick = { onAction(Action.USAGE_PERM) }, padding = 18.dp) {
            Text("Activa los datos de uso", style = MaterialTheme.typography.titleSmall)
            Text("Para ver caché, último uso y apps abandonadas.", style = MaterialTheme.typography.bodySmall, color = muted())
        } }
        item {
            TextField(query, { query = it }, Modifier.fillMaxWidth(), placeholder = { Text("Buscar") },
                leadingIcon = { Icon(Icons.Outlined.Search, null) }, singleLine = true, shape = CircleShape,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer, unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                    focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent))
        }
        item {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (Pro.ready) FilterChip(showSystem, { showSystem = !showSystem }, { Text("Sistema") }, shape = CircleShape)
                AppSort.entries.filter { (usage || !it.needsUsage) && (it != AppSort.FROZEN || list.any { a -> !a.enabled }) }.forEach { s ->
                    FilterChip(sort == s, { sort = s }, { Text(s.label) }, shape = CircleShape)
                }
            }
        }
        val shown = list.filter { query.isBlank() || it.label.contains(query, true) }.let { l ->
            when (sort) {
                AppSort.SIZE -> l.sortedByDescending { it.totalBytes }
                AppSort.UNUSED -> l.filter { it.lastUsed < month }.sortedBy { if (it.lastUsed == 0L) Long.MAX_VALUE else it.lastUsed }
                AppSort.CACHE -> l.sortedByDescending { it.cacheBytes }
                AppSort.USAGE -> l.sortedByDescending { it.fgTimeMs }
                AppSort.NEW -> l.sortedByDescending { it.installTime }
                AppSort.FROZEN -> l.filter { !it.enabled }
            }
        }
        items(shown, key = { it.pkg }) { a ->
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(MaterialTheme.colorScheme.surfaceContainer).clickable { sheet = a }
                .padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                AppIcon(a.pkg, 44.dp)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(a.label, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    val detail = when (sort) {
                        AppSort.CACHE -> "Caché ${Fmt.bytes(a.cacheBytes)}"
                        AppSort.USAGE -> "Hoy ${Fmt.duration(a.fgTimeMs)}"
                        AppSort.NEW -> "Instalada: ${Fmt.ago(a.installTime).lowercase()}"
                        AppSort.FROZEN -> "Congelada"
                        else -> if (usage) "${Fmt.bytes(a.totalBytes)} · ${Fmt.ago(a.lastUsed).lowercase()}" else Fmt.bytes(a.totalBytes)
                    }
                    Text(detail, style = MaterialTheme.typography.labelMedium, color = if (usage && a.lastUsed in 1 until month) Warn else muted())
                }
                if (!a.enabled) Text("Congelada", style = MaterialTheme.typography.labelMedium, color = Warn)
                else if (!a.system) IconButton(onClick = { Nav.uninstall(ctx, a.pkg) }) { Icon(Icons.Outlined.DeleteOutline, "Desinstalar", tint = muted()) }
            }
        }
    }
    sheet?.let { a -> AppSheet(a.pkg, a.label, a.system, onDismiss = { sheet = null }, onChanged = { reload++ }) }
}
