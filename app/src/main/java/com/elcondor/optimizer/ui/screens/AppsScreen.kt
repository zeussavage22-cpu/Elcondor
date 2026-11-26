package com.elcondor.optimizer.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.elcondor.optimizer.data.*
import com.elcondor.optimizer.ui.components.*
import com.elcondor.optimizer.ui.theme.Warn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private enum class AppSort(val label: String) { SIZE("Más pesadas"), UNUSED("Sin usar"), CACHE("Más caché"), USAGE("Más usadas hoy"), NEW("Recientes") }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppsScreen(modifier: Modifier, refresh: Int, onAction: (Action) -> Unit) {
    val ctx = LocalContext.current
    val usage = remember(refresh) { Perms.hasUsageAccess(ctx) }
    var apps by remember { mutableStateOf<List<AppEntry>?>(null) }
    var sort by remember { mutableStateOf(AppSort.SIZE) }
    var query by remember { mutableStateOf("") }

    LaunchedEffect(refresh) {
        apps = withContext(Dispatchers.IO) {
            AppsRepo.installed(ctx).also {
                AppsRepo.fillUsage(ctx, it, AppsRepo.startOfToday())
                AppsRepo.fillStorage(ctx, it)
            }
        }
    }

    Column(modifier.fillMaxSize()) {
        TopAppBar(title = { Text("Administrador de apps", fontWeight = FontWeight.Bold) })
        if (!usage) { Box(Modifier.padding(16.dp)) {
            PermissionGate("Permiso necesario", "Activa 'Acceso a datos de uso' para ver el tamaño y último uso de cada app.", "Conceder") { Perms.openUsageAccess(ctx) }
        } }
        val list = apps
        if (list == null) { Loading("Cargando apps…"); return@Column }

        val month = System.currentTimeMillis() - 30L * 86_400_000
        val unused = list.filter { it.lastUsed in 1 until month }
        val shown = list.filter { query.isBlank() || it.label.contains(query, true) || it.pkg.contains(query, true) }.let { l ->
            when (sort) {
                AppSort.SIZE -> l.sortedByDescending { it.totalBytes }
                AppSort.UNUSED -> l.filter { it.lastUsed < month }.sortedBy { if (it.lastUsed == 0L) Long.MAX_VALUE else it.lastUsed }
                AppSort.CACHE -> l.sortedByDescending { it.cacheBytes }
                AppSort.USAGE -> l.sortedByDescending { it.fgTimeMs }
                AppSort.NEW -> l.sortedByDescending { it.installTime }
            }
        }

        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                SectionCard {
                    KeyValue("Apps instaladas", "${list.size}")
                    if (usage) {
                        KeyValue("Espacio ocupado por apps", Fmt.bytes(list.sumOf { it.totalBytes }))
                        KeyValue("Sin usar en 30+ días", "${unused.size} (${Fmt.bytes(unused.sumOf { it.totalBytes })})", if (unused.isNotEmpty()) Warn else MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
            item {
                OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth(), placeholder = { Text("Buscar app") },
                    leadingIcon = { Icon(Icons.Filled.Search, null) }, singleLine = true)
            }
            item {
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AppSort.entries.forEach { s -> FilterChip(sort == s, { sort = s }, { Text(s.label) }) }
                }
            }
            items(shown, key = { it.pkg }) { a ->
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    modifier = Modifier.fillMaxWidth().clickable { Nav.appDetails(ctx, a.pkg) }) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        AppIcon(a.pkg, 42.dp)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(a.label, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            val detail = when (sort) {
                                AppSort.CACHE -> "Caché ${Fmt.bytes(a.cacheBytes)}"
                                AppSort.USAGE -> "Hoy ${Fmt.duration(a.fgTimeMs)}"
                                AppSort.NEW -> "Instalada ${Fmt.ago(a.installTime).lowercase()}"
                                else -> "${Fmt.bytes(a.totalBytes)} · Último uso: ${Fmt.ago(a.lastUsed).lowercase()}"
                            }
                            Text(detail, style = MaterialTheme.typography.labelMedium,
                                color = if (a.lastUsed in 1 until month) Warn else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (!a.system) IconButton(onClick = { Nav.uninstall(ctx, a.pkg) }) { Icon(Icons.Filled.Delete, "Desinstalar") }
                    }
                }
            }
        }
    }
}
