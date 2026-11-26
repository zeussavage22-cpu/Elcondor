package com.elcondor.app.ui.animation

import android.app.StatusBarManager
import android.content.ComponentName
import android.content.Context
import android.graphics.drawable.Icon
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.elcondor.app.R
import com.elcondor.app.animation.AnimationController
import com.elcondor.app.animation.AnimationKey
import com.elcondor.app.animation.AnimationScale
import com.elcondor.app.animation.AnimationUiState
import com.elcondor.app.animation.ImmersiveMode
import com.elcondor.app.shell.ShizukuShellManager
import com.elcondor.app.shell.ShizukuState
import com.elcondor.app.tile.AnimationSpeedTileService
import com.elcondor.app.ui.ShizukuStatusCard
import kotlinx.coroutines.launch
import java.util.concurrent.Executor
import kotlin.math.roundToInt

@Composable
fun AnimationsScreen(
    controller: AnimationController,
    shell: ShizukuShellManager,
    shizukuState: ShizukuState,
    snackbar: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    val state by controller.state.collectAsStateWithLifecycle()
    val ready = shizukuState is ShizukuState.Ready
    val locked = !ready || state.isBusy

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
        if (!ready) ShizukuStatusCard(state = shizukuState, shell = shell)
        SpeedCard(state, locked, onSetAll = controller::setAll, onSet = controller::set)
        TileCard(snackbar)
        ImmersiveCard(state, locked, onSelect = controller::setImmersive)
    }
}

@Composable
private fun SectionCard(title: String, busy: Boolean = false, content: @Composable ColumnScope.() -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                if (busy) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
            }
            content()
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SpeedCard(
    state: AnimationUiState,
    locked: Boolean,
    onSetAll: (Float) -> Unit,
    onSet: (AnimationKey, Float) -> Unit,
) {
    SectionCard("Velocidad de animaciones", busy = state.isBusy) {
        Text(
            "Valores menores = interfaz más ágil. 0x desactiva las animaciones por completo.",
            style = MaterialTheme.typography.bodyMedium,
        )

        Text("Las tres a la vez", style = MaterialTheme.typography.labelLarge)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AnimationScale.PRESETS.forEach { preset ->
                FilterChip(
                    selected = state.scales.uniform == preset,
                    onClick = { onSetAll(preset) },
                    label = { Text("${AnimationScale.format(preset)}x") },
                    enabled = !locked,
                )
            }
        }

        // Ajuste fino individual: se aplica al soltar el slider, no en cada frame.
        AnimationKey.entries.forEach { key ->
            ScaleSlider(key, current = state.scales[key], enabled = !locked, onCommit = { onSet(key, it) })
        }
    }
}

@Composable
private fun ScaleSlider(key: AnimationKey, current: Float, enabled: Boolean, onCommit: (Float) -> Unit) {
    // Slider 0..2 en pasos de 0.05 (41 posiciones); valores >2 se muestran al tope.
    var value by remember(current) { mutableFloatStateOf(current.coerceIn(0f, SLIDER_MAX)) }
    Column {
        Row {
            Text(key.label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            Text("${AnimationScale.format(value)}x", style = MaterialTheme.typography.bodyMedium)
        }
        Slider(
            value = value,
            onValueChange = { value = (it / STEP).roundToInt() * STEP },
            onValueChangeFinished = { if (value != current) onCommit(value) },
            valueRange = 0f..SLIDER_MAX,
            steps = (SLIDER_MAX / STEP).toInt() - 1,
            enabled = enabled,
        )
    }
}

@Composable
private fun TileCard(snackbar: SnackbarHostState) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    SectionCard("Mosaico de Ajustes rápidos") {
        Text(
            "Alterna entre animaciones rápidas (0.25x) y normales (1x) desde el panel de notificaciones.",
            style = MaterialTheme.typography.bodyMedium,
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Button(onClick = {
                requestAddTile(context) { message -> scope.launch { snackbar.showSnackbar(message) } }
            }) { Text("Añadir al panel") }
        } else {
            Text(
                "Desliza el panel de notificaciones → editar (lápiz) → arrastra \"Animaciones\".",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Android 13+: el sistema muestra su propio diálogo "¿Añadir mosaico?". */
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private fun requestAddTile(context: Context, onResult: (String) -> Unit) {
    val statusBar = context.getSystemService(StatusBarManager::class.java)
    val mainExecutor: Executor = context.mainExecutor
    statusBar.requestAddTileService(
        ComponentName(context, AnimationSpeedTileService::class.java),
        "Animaciones",
        Icon.createWithResource(context, R.drawable.ic_tile_speed),
        mainExecutor,
    ) { result ->
        onResult(
            when (result) {
                StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ADDED -> "Mosaico añadido"
                StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ALREADY_ADDED -> "El mosaico ya estaba en el panel"
                StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_NOT_ADDED -> "No se añadió el mosaico"
                else -> "No se pudo pedir el mosaico (código $result)"
            },
        )
    }
}

@Composable
private fun ImmersiveCard(state: AnimationUiState, locked: Boolean, onSelect: (ImmersiveMode) -> Unit) {
    SectionCard("Modo inmersivo") {
        if (!state.immersiveSupported) {
            Text(
                "⚠ Android ${Build.VERSION.RELEASE} ignora policy_control (se eliminó en Android 11). " +
                    "El ajuste se guarda pero no tendrá efecto salvo en algunas ROMs personalizadas.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
        ImmersiveMode.entries.forEach { mode ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(
                        selected = state.immersive == mode,
                        enabled = !locked,
                        role = Role.RadioButton,
                        onClick = { onSelect(mode) },
                    ),
            ) {
                RadioButton(selected = state.immersive == mode, onClick = null, enabled = !locked)
                Text(mode.label, modifier = Modifier.padding(start = 8.dp))
            }
        }
    }
}

private const val SLIDER_MAX = 2f
private const val STEP = 0.05f
