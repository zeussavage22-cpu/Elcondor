package com.elcondor.app.tile

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.elcondor.app.ElCondorApp
import com.elcondor.app.MainActivity
import com.elcondor.app.animation.AnimationScale
import com.elcondor.app.animation.AnimationScales
import com.elcondor.app.shell.ShizukuState
import kotlinx.coroutines.launch

/**
 * Mosaico de Ajustes rápidos: alterna animaciones 0.25x ↔ 1x con un toque.
 *
 * Corre en el proceso de la app, así que reutiliza el ShizukuShellManager de
 * ElCondorApp. El estado del mosaico se lee de Settings.Global (sin Shizuku).
 */
class AnimationSpeedTileService : TileService() {

    private val app get() = application as ElCondorApp

    override fun onStartListening() {
        super.onStartListening()
        // Puede que Shizuku haya arrancado mientras el panel estaba cerrado.
        app.shell.refreshState()
        render()
    }

    override fun onClick() {
        super.onClick()
        if (app.shell.state.value !is ShizukuState.Ready) {
            // Sin Shizuku no podemos escribir ajustes: llevamos al usuario a la app.
            openApp()
            return
        }

        qsTile?.apply {
            subtitleCompat("Aplicando…")
            updateTile()
        }
        // appScope (no un scope del servicio): el sistema puede desvincular el
        // TileService en cuanto se cierra el panel, y no queremos cortar el comando.
        app.appScope.launch {
            app.animations.toggleFast()
            render()
        }
    }

    private fun render() {
        val tile = qsTile ?: return
        val scales = app.animations.readScales()
        val shizukuReady = app.shell.state.value is ShizukuState.Ready

        tile.label = "Animaciones"
        tile.state = if (scales.isFast) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.subtitleCompat(
            when {
                !shizukuReady -> "Shizuku inactivo"
                scales.uniform != null -> "${AnimationScale.format(scales.uniform!!)}x"
                else -> "Mixto"
            },
        )
        tile.contentDescription = if (scales.isFast) "Animaciones rápidas activadas" else
            "Animaciones a ${AnimationScale.format(scales.uniform ?: AnimationScales.DEFAULT)}x"
        tile.updateTile()
    }

    private fun Tile.subtitleCompat(text: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) subtitle = text
    }

    @SuppressLint("StartActivityAndCollapseDeprecated")
    private fun openApp() {
        val intent = Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            // Android 14+ exige PendingIntent (la variante con Intent lanza excepción).
            startActivityAndCollapse(
                PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE),
            )
        } else {
            @Suppress("DEPRECATION")
            startActivityAndCollapse(intent)
        }
    }
}
