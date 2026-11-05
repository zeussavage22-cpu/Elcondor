package com.elcondor.cloner

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.elcondor.cloner.core.PackageNames

class MainActivity : ComponentActivity() {
    private val vm: ClonerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val dark = isSystemInDarkTheme()
            MaterialTheme(
                colorScheme = if (dark) darkColorScheme(primary = Color(0xFF7FB3E6))
                else lightColorScheme(primary = Color(0xFF1E3A5F))
            ) {
                ClonerScreen(vm)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ClonerScreen(vm: ClonerViewModel) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var selected by remember { mutableStateOf<InstalledApp?>(null) }
    val job by vm.job.collectAsStateWithLifecycle()

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { vm.refreshClones() }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Elcondor Cloner") }) },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            TabRow(selectedTabIndex = tab) {
                Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Apps") })
                Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Mis clones") })
            }
            when (tab) {
                0 -> AppsTab(vm, onSelect = { selected = it })
                else -> ClonesTab(vm)
            }
        }
    }

    selected?.let { app ->
        CloneDialog(
            app = app,
            suggestedPackage = remember(app.packageName) { vm.suggestPackage(app.packageName) },
            onDismiss = { selected = null },
            onClone = { pkg, label, perms ->
                selected = null
                vm.clone(app, pkg, label, perms)
            },
        )
    }
    JobDialog(job, vm)
}

@Composable
private fun AppsTab(vm: ClonerViewModel, onSelect: (InstalledApp) -> Unit) {
    val apps by vm.apps.collectAsStateWithLifecycle()
    val loading by vm.loading.collectAsStateWithLifecycle()
    var query by rememberSaveable { mutableStateOf("") }
    var showSystem by rememberSaveable { mutableStateOf(false) }

    val filtered = apps.filter { app ->
        (showSystem || !app.isSystem) &&
            (query.isBlank() || app.label.contains(query, true) || app.packageName.contains(query, true))
    }

    Column(Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            placeholder = { Text("Buscar app") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        )
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Mostrar apps del sistema", Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
            Switch(checked = showSystem, onCheckedChange = { showSystem = it })
        }
        if (loading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else {
            LazyColumn(Modifier.fillMaxSize()) {
                items(filtered, key = { it.packageName }) { app ->
                    AppRow(app.icon, app.label, app.packageName, Modifier.clickable { onSelect(app) })
                }
            }
        }
    }
}

@Composable
private fun AppRow(icon: androidx.compose.ui.graphics.ImageBitmap?, title: String, subtitle: String, modifier: Modifier = Modifier) {
    Row(
        modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) Image(icon, contentDescription = null, Modifier.size(44.dp))
        else Spacer(Modifier.size(44.dp))
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun ClonesTab(vm: ClonerViewModel) {
    val clones by vm.clones.collectAsStateWithLifecycle()
    val context = LocalContext.current
    if (clones.isEmpty()) {
        Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
            Text(
                "Todavía no tienes clones.\nElige una app en la pestaña \"Apps\" para crear una copia.",
                style = MaterialTheme.typography.bodyLarge,
            )
        }
        return
    }
    LazyColumn(Modifier.fillMaxSize()) {
        items(clones, key = { it.record.packageName }) { item ->
            Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)) {
                Text(item.record.label, style = MaterialTheme.typography.titleMedium)
                Text(item.record.packageName, style = MaterialTheme.typography.bodySmall)
                Text("Copia de ${item.record.originalPackage}", style = MaterialTheme.typography.bodySmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 6.dp)) {
                    if (item.installed) {
                        OutlinedButton(onClick = { AppScanner.launch(context, item.record.packageName) }) { Text("Abrir") }
                        OutlinedButton(onClick = {
                            context.startActivity(Intent(Intent.ACTION_DELETE, Uri.parse("package:${item.record.packageName}")))
                        }) { Text("Desinstalar") }
                    } else {
                        Text("No instalado", Modifier.align(Alignment.CenterVertically), color = MaterialTheme.colorScheme.error)
                        TextButton(onClick = { vm.forget(item.record.packageName) }) { Text("Quitar de la lista") }
                    }
                }
            }
            HorizontalDivider()
        }
    }
}

@Composable
private fun CloneDialog(
    app: InstalledApp,
    suggestedPackage: String,
    onDismiss: () -> Unit,
    onClone: (pkg: String, label: String, removePermissions: Set<String>) -> Unit,
) {
    val context = LocalContext.current
    var label by remember { mutableStateOf("${app.label} 2") }
    var pkg by remember { mutableStateOf(suggestedPackage) }
    var blocked by remember { mutableStateOf(setOf<String>()) }
    var needsInstallPermission by remember { mutableStateOf(false) }
    val groups = PERMISSION_GROUPS.filter { g -> g.permissions.any { it in app.requestedPermissions } }

    val pkgError = when {
        !PackageNames.isValid(pkg) -> "Nombre de paquete no válido (ej: com.app.clon1)"
        pkg == app.packageName -> "Debe ser distinto al original"
        else -> null
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Clonar ${app.label}") },
        text = {
            Column(Modifier.heightIn(max = 480.dp).verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    value = label, onValueChange = { label = it },
                    label = { Text("Nombre del clon") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.size(8.dp))
                OutlinedTextField(
                    value = pkg, onValueChange = { pkg = it.trim() },
                    label = { Text("Paquete") }, singleLine = true,
                    isError = pkgError != null,
                    supportingText = { Text(pkgError ?: "Identificador único del clon") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    modifier = Modifier.fillMaxWidth(),
                )
                if (groups.isNotEmpty()) {
                    Spacer(Modifier.size(8.dp))
                    Text("Bloquear en el clon", fontWeight = FontWeight.SemiBold)
                    groups.forEach { g ->
                        val checked = g.permissions.all { it in blocked }
                        Row(
                            Modifier.fillMaxWidth().clickable {
                                blocked = if (checked) blocked - g.permissions.toSet() else blocked + g.permissions
                            },
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(checked = checked, onCheckedChange = null)
                            Spacer(Modifier.width(8.dp))
                            Text(g.title)
                        }
                    }
                    Text(
                        "Algunas apps pueden fallar si les quitas permisos que necesitan.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                if (needsInstallPermission) {
                    Spacer(Modifier.size(8.dp))
                    Text(
                        "Primero permite que Elcondor Cloner instale apps (\"Fuentes desconocidas\") y vuelve a tocar Clonar.",
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(enabled = pkgError == null, onClick = {
                if (!context.packageManager.canRequestPackageInstalls()) {
                    needsInstallPermission = true
                    context.startActivity(
                        Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}"))
                    )
                } else {
                    onClone(pkg, label.trim(), blocked)
                }
            }) { Text("Clonar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}

@Composable
private fun JobDialog(job: JobState, vm: ClonerViewModel) {
    val context = LocalContext.current
    when (job) {
        JobState.Idle -> Unit
        is JobState.Working -> AlertDialog(
            onDismissRequest = {},
            confirmButton = {},
            title = { Text("Clonando") },
            text = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.size(28.dp))
                    Spacer(Modifier.width(16.dp))
                    Text(job.text)
                }
            },
        )
        is JobState.Done -> AlertDialog(
            onDismissRequest = vm::dismissJob,
            title = { Text("Clon instalado") },
            text = { Text("\"${job.label}\" ya está en tu pantalla de inicio.") },
            confirmButton = {
                TextButton(onClick = { AppScanner.launch(context, job.packageName); vm.dismissJob() }) { Text("Abrir") }
            },
            dismissButton = { TextButton(onClick = vm::dismissJob) { Text("Cerrar") } },
        )
        is JobState.Failed -> AlertDialog(
            onDismissRequest = vm::dismissJob,
            title = { Text("No se pudo clonar") },
            text = { Text(job.message) },
            confirmButton = { TextButton(onClick = vm::dismissJob) { Text("Aceptar") } },
        )
    }
}
