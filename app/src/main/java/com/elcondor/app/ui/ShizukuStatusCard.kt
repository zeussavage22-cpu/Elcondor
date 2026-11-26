package com.elcondor.app.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.elcondor.app.shell.ShizukuShellManager
import com.elcondor.app.shell.ShizukuState

/** Tarjeta con interruptor: muestra el estado de Shizuku y guía al usuario para activarlo. */
@Composable
fun ShizukuStatusCard(state: ShizukuState, shell: ShizukuShellManager, modifier: Modifier = Modifier) {
    val context = LocalContext.current
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

    Card(modifier = modifier.fillMaxWidth()) {
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
                onCheckedChange = { checked -> if (checked) handleEnableRequest(context, shell, state) },
                enabled = state !is ShizukuState.Ready,
            )
        }
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
    fun view(uri: String) = Intent(Intent.ACTION_VIEW, Uri.parse(uri)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    try {
        context.startActivity(view(url))
    } catch (_: ActivityNotFoundException) {
        runCatching { context.startActivity(view(fallback)) }
    }
}
