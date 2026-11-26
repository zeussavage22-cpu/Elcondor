package com.elcondor.app.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.elcondor.app.display.DensityController
import com.elcondor.app.display.DensityUiState
import com.elcondor.app.shell.ShizukuShellManager
import com.elcondor.app.shell.ShizukuState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(shell: ShizukuShellManager, density: DensityController) {
    val shizukuState by shell.state.collectAsStateWithLifecycle()
    val densityState by density.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current

    // Al quedar listo Shizuku, leemos el DPI actual.
    LaunchedEffect(shizukuState is ShizukuState.Ready) {
        if (shizukuState is ShizukuState.Ready) density.refresh()
    }

    LaunchedEffect(densityState.message) {
        densityState.message?.let {
            snackbar.showSnackbar(it)
            density.consumeMessage()
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("El Cóndor") }) },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            ShizukuStatusCard(
                state = shizukuState,
                onEnableRequested = { handleEnableRequest(context, shell, shizukuState) },
            )
            DensityCard(
                state = densityState,
                enabled = shizukuState is ShizukuState.Ready,
                onApply = density::applyDensity,
                onReset = density::resetToFactory,
            )
        }
    }

    // Diálogo de seguridad: se muestra encima de todo mientras corre la cuenta atrás.
    densityState.pendingConfirmationSeconds?.let { seconds ->
        SafetyConfirmDialog(
            secondsLeft = seconds,
            newDpi = densityState.pendingDpi,
            onConfirm = density::confirm,
            onRevert = density::revertNow,
        )
    }
}

/** Decide qué hacer cuando el usuario activa el interruptor según el estado actual. */
private fun handleEnableRequest(context: Context, shell: ShizukuShellManager, state: ShizukuState) {
    when (state) {
        ShizukuState.PermissionRequired -> shell.requestPermission()
        ShizukuState.NotRunning, ShizukuState.PermissionDeniedPermanently, ShizukuState.Unsupported -> {
            // Abrimos Shizuku para que el usuario lo inicie / conceda el permiso / actualice.
            context.packageManager.getLaunchIntentForPackage(ShizukuShellManager.SHIZUKU_PACKAGE)
                ?.let(context::startActivity)
        }
        ShizukuState.NotInstalled -> openUrl(
            context,
            "market://details?id=${ShizukuShellManager.SHIZUKU_PACKAGE}",
            fallback = "https://shizuku.rikka.app/download/",
        )
        is ShizukuState.Ready -> Unit
    }
}

private fun openUrl(context: Context, url: String, fallback: String) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    try {
        context.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(fallback)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}

@Composable
private fun ShizukuStatusCard(state: ShizukuState, onEnableRequested: () -> Unit) {
    val (title, detail) = when (state) {
        ShizukuState.NotInstalled -> "Shizuku no instalado" to "Toca el interruptor para instalarlo."
        ShizukuState.NotRunning -> "Shizuku detenido" to "Inícialo con Depuración inalámbrica o ADB."
        ShizukuState.Unsupported -> "Shizuku desactualizado" to "Se requiere la versión 11 o superior."
        ShizukuState.PermissionRequired -> "Permiso pendiente" to "Toca el interruptor para conceder acceso."
        ShizukuState.PermissionDeniedPermanently ->
            "Permiso denegado" to "Concédelo manualmente en la app Shizuku → Apps autorizadas."
        is ShizukuState.Ready ->
            "Shizuku activo" to "Modo ${if (state.isRoot) "root" else "ADB/shell"} · API v${state.version}"
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(detail, style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(Modifier.width(12.dp))
            Switch(
                checked = state is ShizukuState.Ready,
                // El permiso solo se revoca desde Shizuku: apagar el switch no hace nada.
                onCheckedChange = { checked -> if (checked) onEnableRequested() },
                enabled = state !is ShizukuState.Ready,
            )
        }
    }
}

@Composable
private fun DensityCard(
    state: DensityUiState,
    enabled: Boolean,
    onApply: (Int) -> Unit,
    onReset: () -> Unit,
) {
    var input by rememberSaveable { mutableStateOf("") }
    val parsed = input.toIntOrNull()
    val inputValid = parsed != null && parsed in DensityController.MIN_DPI..DensityController.MAX_DPI
    val locked = !enabled || state.isBusy || state.pendingConfirmationSeconds != null

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Densidad (DPI)", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                if (state.isBusy) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            }

            val info = state.info
            Text(
                text = if (info == null) "Sin datos"
                else "Físico: ${info.physical} · Actual: ${info.effective}" +
                    if (info.override != null) " (override)" else " (fábrica)",
                style = MaterialTheme.typography.bodyMedium,
            )

            // Atajos relativos al DPI físico: más pequeño = más contenido en pantalla.
            if (info != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(0.85f, 0.92f, 1.0f, 1.1f).forEach { factor ->
                        val value = (info.physical * factor).toInt()
                        FilterChip(
                            selected = parsed == value,
                            onClick = { input = value.toString() },
                            label = { Text(value.toString()) },
                            enabled = !locked,
                        )
                    }
                }
            }

            OutlinedTextField(
                value = input,
                onValueChange = { new -> input = new.filter(Char::isDigit).take(4) },
                label = { Text("Nuevo DPI") },
                supportingText = {
                    Text("Entre ${DensityController.MIN_DPI} y ${DensityController.MAX_DPI}")
                },
                isError = input.isNotEmpty() && !inputValid,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                enabled = !locked,
                modifier = Modifier.fillMaxWidth(),
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { parsed?.let(onApply) },
                    enabled = !locked && inputValid,
                ) { Text("Aplicar") }
                OutlinedButton(
                    onClick = onReset,
                    enabled = !locked,
                ) { Text("Restaurar fábrica") }
            }
        }
    }
}

@Composable
private fun SafetyConfirmDialog(
    secondsLeft: Int,
    newDpi: Int?,
    onConfirm: () -> Unit,
    onRevert: () -> Unit,
) {
    AlertDialog(
        // No se cierra tocando fuera ni con "Atrás": el usuario debe decidir
        // (o dejar que el temporizador revierta).
        onDismissRequest = {},
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
        title = { Text("¿Ves bien la pantalla?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Nuevo DPI: ${newDpi ?: "—"}. Se revertirá automáticamente en:")
                Text(
                    "$secondsLeft s",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                )
                LinearProgressIndicator(
                    progress = { secondsLeft / DensityController.CONFIRM_TIMEOUT_SECONDS.toFloat() },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = { Button(onClick = onConfirm) { Text("Mantener") } },
        dismissButton = { TextButton(onClick = onRevert) { Text("Revertir") } },
    )
}
