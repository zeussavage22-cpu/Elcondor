package com.elcondor.app.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.elcondor.app.ElCondorApp
import com.elcondor.app.ui.animation.AnimationsScreen
import com.elcondor.app.ui.display.DisplayScreen
import com.elcondor.app.ui.display.SafetyConfirmDialog
import com.elcondor.app.ui.packages.PackagesScreen
import com.elcondor.app.ui.packages.PackagesViewModel

private enum class Destination(val title: String, val icon: ImageVector) {
    Display("Pantalla", Icons.Default.Settings),
    Animations("Animaciones", Icons.Default.PlayArrow),
    Packages("Paquetes", Icons.AutoMirrored.Filled.List),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppRoot(app: ElCondorApp) {
    var destination by rememberSaveable { mutableStateOf(Destination.Display) }
    val snackbar = remember { SnackbarHostState() }
    val shizukuState by app.shell.state.collectAsStateWithLifecycle()
    val displayState by app.display.state.collectAsStateWithLifecycle()
    val packagesViewModel = viewModel { PackagesViewModel(app.packages, app.shell) }

    Scaffold(
        topBar = { TopAppBar(title = { Text(destination.title) }) },
        bottomBar = {
            NavigationBar {
                Destination.entries.forEach { dest ->
                    NavigationBarItem(
                        selected = destination == dest,
                        onClick = { destination = dest },
                        icon = { Icon(dest.icon, contentDescription = null) },
                        label = { Text(dest.title) },
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        val contentModifier = Modifier.padding(padding)
        when (destination) {
            Destination.Display -> DisplayScreen(app.display, app.shell, shizukuState, snackbar, contentModifier)
            Destination.Animations -> AnimationsScreen(app.animations, app.shell, shizukuState, snackbar, contentModifier)
            Destination.Packages -> PackagesScreen(packagesViewModel, app.shell, shizukuState, snackbar, contentModifier)
        }
    }

    // Visible desde cualquier pestaña mientras corre la cuenta atrás.
    displayState.pending?.let { pending ->
        SafetyConfirmDialog(pending, onConfirm = app.display::confirm, onRevert = app.display::revertNow)
    }
}
