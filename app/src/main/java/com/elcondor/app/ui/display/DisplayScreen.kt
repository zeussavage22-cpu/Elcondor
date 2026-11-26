package com.elcondor.app.ui.display

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.elcondor.app.display.DisplayController
import com.elcondor.app.display.DisplayLimits
import com.elcondor.app.display.DisplayUiState
import com.elcondor.app.display.PendingChange
import com.elcondor.app.display.Resolution
import com.elcondor.app.shell.ShizukuShellManager
import com.elcondor.app.shell.ShizukuState
import com.elcondor.app.ui.ShizukuStatusCard

@Composable
fun DisplayScreen(
    controller: DisplayController,
    shell: ShizukuShellManager,
    shizukuState: ShizukuState,
    snackbar: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    val state by controller.state.collectAsStateWithLifecycle()
    val ready = shizukuState is ShizukuState.Ready

    // Al quedar listo Shizuku, leemos los valores actuales.
    LaunchedEffect(ready) { if (ready) controller.refresh() }

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbar.showSnackbar(it)
            controller.consumeMessage()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        ShizukuStatusCard(state = shizukuState, shell = shell)
        DensityCard(state, enabled = ready, onApply = controller::applyDensity, onReset = controller::resetDensity)
        ResolutionCard(state, enabled = ready, onApply = controller::applySize, onReset = controller::resetSize)
    }
}

@Composable
private fun SettingCard(title: String, busy: Boolean, content: @Composable ColumnScope.() -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                if (busy) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
            }
            content()
        }
    }
}

@Composable
private fun DensityCard(state: DisplayUiState, enabled: Boolean, onApply: (Int) -> Unit, onReset: () -> Unit) {
    var input by rememberSaveable { mutableStateOf("") }
    val parsed = input.toIntOrNull()
    val error = parsed?.let(DisplayLimits::validateDensity)
    val locked = !enabled || state.isLocked
    val density = state.info?.density

    SettingCard("Densidad (DPI)", busy = state.isBusy) {
        Text(
            if (density == null) "Sin datos"
            else "Físico: ${density.physical} · Actual: ${density.effective}" +
                if (density.override != null) " (override)" else " (fábrica)",
            style = MaterialTheme.typography.bodyMedium,
        )

        // Atajos relativos al DPI físico: más pequeño = más contenido en pantalla.
        if (density != null) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(0.85f, 0.92f, 1.0f, 1.1f).forEach { factor ->
                    val value = (density.physical * factor).toInt()
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
            supportingText = { Text(error ?: "Entre ${DisplayLimits.MIN_DPI} y ${DisplayLimits.MAX_DPI}") },
            isError = error != null,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            enabled = !locked,
            modifier = Modifier.fillMaxWidth(),
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { parsed?.let(onApply) }, enabled = !locked && parsed != null && error == null) {
                Text("Aplicar")
            }
            OutlinedButton(onClick = onReset, enabled = !locked) { Text("Restaurar fábrica") }
        }
    }
}

@Composable
private fun ResolutionCard(
    state: DisplayUiState,
    enabled: Boolean,
    onApply: (Resolution) -> Unit,
    onReset: () -> Unit,
) {
    var widthInput by rememberSaveable { mutableStateOf("") }
    var heightInput by rememberSaveable { mutableStateOf("") }
    val locked = !enabled || state.isLocked
    val size = state.info?.size

    val target = run {
        val w = widthInput.toIntOrNull()
        val h = heightInput.toIntOrNull()
        if (w != null && h != null) Resolution(w, h) else null
    }
    val error = if (target != null && size != null) DisplayLimits.validateSize(target, size.physical) else null

    SettingCard("Resolución", busy = state.isBusy) {
        Text(
            if (size == null) "Sin datos"
            else "Físico: ${size.physical} · Actual: ${size.effective}" +
                if (size.override != null) " (override)" else " (fábrica)",
            style = MaterialTheme.typography.bodyMedium,
        )

        // Presets que mantienen la proporción del panel. Menos píxeles = menos GPU/batería.
        if (size != null) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(0.67f, 0.75f, 0.85f, 1.0f).forEach { factor ->
                    val preset = size.physical.scaled(factor)
                    FilterChip(
                        selected = target == preset,
                        onClick = {
                            widthInput = preset.width.toString()
                            heightInput = preset.height.toString()
                        },
                        label = { Text("${(factor * 100).toInt()}%") },
                        enabled = !locked,
                    )
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            DimensionField("Ancho", widthInput, { widthInput = it }, !locked, error != null, Modifier.weight(1f))
            Text("×")
            DimensionField("Alto", heightInput, { heightInput = it }, !locked, error != null, Modifier.weight(1f))
        }
        Text(
            error ?: "Se mantiene la orientación y una proporción similar a la del panel",
            style = MaterialTheme.typography.bodySmall,
            color = if (error != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { target?.let(onApply) }, enabled = !locked && target != null && error == null) {
                Text("Aplicar")
            }
            OutlinedButton(onClick = onReset, enabled = !locked) { Text("Restaurar fábrica") }
        }
    }
}

@Composable
private fun DimensionField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    enabled: Boolean,
    isError: Boolean,
    modifier: Modifier,
) {
    OutlinedTextField(
        value = value,
        onValueChange = { onValueChange(it.filter(Char::isDigit).take(5)) },
        label = { Text(label) },
        isError = isError,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        enabled = enabled,
        modifier = modifier,
    )
}

/**
 * Diálogo de seguridad compartido por DPI y resolución. Se dibuja desde la raíz
 * de la app para que siga visible aunque el usuario cambie de pestaña.
 */
@Composable
fun SafetyConfirmDialog(pending: PendingChange, onConfirm: () -> Unit, onRevert: () -> Unit) {
    AlertDialog(
        // No se cierra tocando fuera ni con "Atrás": el usuario debe decidir
        // (o dejar que el temporizador revierta).
        onDismissRequest = {},
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
        title = { Text("¿Ves bien la pantalla?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("${pending.change.description}. Se revertirá automáticamente en:")
                Text(
                    "${pending.secondsLeft} s",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                )
                LinearProgressIndicator(
                    progress = { pending.secondsLeft / DisplayController.CONFIRM_TIMEOUT_SECONDS.toFloat() },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = { Button(onClick = onConfirm) { Text("Mantener") } },
        dismissButton = { TextButton(onClick = onRevert) { Text("Revertir") } },
    )
}
