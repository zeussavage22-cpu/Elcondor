package com.elcondor.app.ui.packages

import android.text.format.DateUtils
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.elcondor.app.packages.RemovalRisk
import com.elcondor.app.packages.SystemPackage
import com.elcondor.app.packages.TrashItem
import com.elcondor.app.shell.ShizukuShellManager
import com.elcondor.app.shell.ShizukuState
import com.elcondor.app.ui.ShizukuStatusCard

@Composable
fun PackagesScreen(
    viewModel: PackagesViewModel,
    shell: ShizukuShellManager,
    shizukuState: ShizukuState,
    snackbar: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbar.showSnackbar(it)
            viewModel.consumeMessage()
        }
    }

    if (shizukuState !is ShizukuState.Ready) {
        Column(modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            ShizukuStatusCard(state = shizukuState, shell = shell)
            Text("Activa Shizuku para ver y gestionar los paquetes del sistema.")
        }
        return
    }

    var tab by rememberSaveable { mutableIntStateOf(0) }
    var toUninstall by remember { mutableStateOf<SystemPackage?>(null) }

    val filteredInstalled = remember(state.installed, state.query) { state.installed.filterBy(state.query) { it.label to it.packageName } }
    val filteredTrash = remember(state.trash, state.query) { state.trash.filterBy(state.query) { it.label to it.packageName } }

    Column(modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = tab) {
            Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Sistema (${state.installed.size})") })
            Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Papelera (${state.trash.size})") })
        }

        OutlinedTextField(
            value = state.query,
            onValueChange = viewModel::setQuery,
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                IconButton(onClick = viewModel::refresh, enabled = !state.isLoading) {
                    Icon(Icons.Default.Refresh, contentDescription = "Recargar")
                }
            },
            placeholder = { Text("Buscar por nombre o paquete") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        )

        if (state.isLoading) LinearProgressIndicator(Modifier.fillMaxWidth())

        Box(Modifier.weight(1f)) {
            when {
                !state.hasLoaded && state.isLoading -> CenteredText("Leyendo paquetes…")
                tab == 0 && filteredInstalled.isEmpty() -> CenteredText("Sin resultados")
                tab == 1 && filteredTrash.isEmpty() ->
                    CenteredText(if (state.trash.isEmpty()) "La papelera está vacía" else "Sin resultados")
                tab == 0 -> LazyColumn {
                    items(filteredInstalled, key = { it.packageName }) { pkg ->
                        InstalledRow(
                            pkg = pkg,
                            busy = pkg.packageName in state.inFlight,
                            onUninstall = { toUninstall = pkg },
                        )
                        HorizontalDivider()
                    }
                }
                else -> LazyColumn {
                    items(filteredTrash, key = { it.packageName }) { item ->
                        TrashRow(
                            item = item,
                            busy = item.packageName in state.inFlight,
                            onRestore = { viewModel.restore(item) },
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }

    toUninstall?.let { pkg ->
        UninstallDialog(
            pkg = pkg,
            onConfirm = {
                viewModel.uninstall(pkg)
                toUninstall = null
            },
            onDismiss = { toUninstall = null },
        )
    }
}

@Composable
private fun InstalledRow(pkg: SystemPackage, busy: Boolean, onUninstall: () -> Unit) {
    ListItem(
        headlineContent = { Text(pkg.label) },
        supportingContent = { Text(pkg.packageName, style = MaterialTheme.typography.bodySmall) },
        overlineContent = when (pkg.risk) {
            RemovalRisk.BLOCKED -> ({ Text("Protegido", color = MaterialTheme.colorScheme.outline) })
            RemovalRisk.CAUTION -> ({ Text("Precaución", color = MaterialTheme.colorScheme.error) })
            RemovalRisk.NORMAL -> null
        },
        trailingContent = {
            when {
                busy -> CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                pkg.risk == RemovalRisk.BLOCKED -> AssistChip(
                    onClick = {},
                    enabled = false,
                    label = { Text("Bloqueado") },
                    colors = AssistChipDefaults.assistChipColors(),
                )
                else -> OutlinedButton(onClick = onUninstall) { Text("Quitar") }
            }
        },
    )
}

@Composable
private fun TrashRow(item: TrashItem, busy: Boolean, onRestore: () -> Unit) {
    ListItem(
        headlineContent = { Text(item.label) },
        supportingContent = {
            Column {
                Text(item.packageName, style = MaterialTheme.typography.bodySmall)
                Text(
                    text = item.removedAt?.let { "Quitado " + DateUtils.getRelativeTimeSpanString(it) }
                        ?: "Quitado fuera de la app",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                )
            }
        },
        trailingContent = {
            if (busy) CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
            else FilledTonalButton(onClick = onRestore) { Text("Restaurar") }
        },
    )
}

@Composable
private fun UninstallDialog(pkg: SystemPackage, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    val caution = pkg.risk == RemovalRisk.CAUTION
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("¿Quitar ${pkg.label}?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(pkg.packageName, style = MaterialTheme.typography.bodySmall)
                Text("Se desinstala solo para tu usuario. El APK sigue en /system y podrás restaurarlo desde la Papelera.")
                if (caution) {
                    Text(
                        "⚠ Este paquete es importante para el sistema o para otras apps. " +
                            "Quitarlo puede causar fallos hasta que lo restaures.",
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = if (caution) ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                else ButtonDefaults.buttonColors(),
            ) { Text(if (caution) "Quitar de todas formas" else "Quitar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}

@Composable
private fun CenteredText(text: String) {
    Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Text(text, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.outline)
    }
}

private inline fun <T> List<T>.filterBy(query: String, crossinline fields: (T) -> Pair<String, String>): List<T> {
    val q = query.trim()
    if (q.isEmpty()) return this
    return filter {
        val (label, pkg) = fields(it)
        label.contains(q, ignoreCase = true) || pkg.contains(q, ignoreCase = true)
    }
}
