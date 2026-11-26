package com.elcondor.optimizer.service

import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.widget.Toast
import com.elcondor.optimizer.data.AppsRepo
import com.elcondor.optimizer.data.Fmt
import kotlin.concurrent.thread

/** Botón de Ajustes rápidos: optimización con un toque desde la cortina de notificaciones. */
class OptimizeTileService : TileService() {
    override fun onStartListening() {
        qsTile?.apply { state = Tile.STATE_INACTIVE; label = "Optimizar"; updateTile() }
    }

    override fun onClick() {
        val tile = qsTile ?: return
        tile.state = Tile.STATE_ACTIVE; tile.label = "Optimizando…"; tile.updateTile()
        val main = mainExecutor
        thread {
            val freed = AppsRepo.boost(applicationContext)
            main.execute {
                Toast.makeText(applicationContext, "Cóndor Care: ${Fmt.bytes(freed)} de RAM liberada", Toast.LENGTH_SHORT).show()
                tile.state = Tile.STATE_INACTIVE; tile.label = "Optimizar"; tile.updateTile()
            }
        }
    }
}
