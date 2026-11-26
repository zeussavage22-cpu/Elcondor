package com.elcondor.app

import android.app.Application
import com.elcondor.app.display.DensityController
import com.elcondor.app.shell.ShizukuShellManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Contenedor de dependencias mínimo (sin Hilt para mantener el paso 1 simple).
 *
 * ShizukuShellManager se crea aquí para que sus listeners estén registrados
 * antes de que ShizukuProvider entregue el binder.
 */
class ElCondorApp : Application() {

    /**
     * Scope que sobrevive a Activities/ViewModels. Main.immediate = el estado
     * se muta en un solo hilo; el trabajo de I/O se mueve a Dispatchers.IO
     * dentro de ShizukuShellManager.
     */
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    lateinit var shell: ShizukuShellManager
        private set
    lateinit var density: DensityController
        private set

    override fun onCreate() {
        super.onCreate()
        shell = ShizukuShellManager(this)
        density = DensityController(shell, appScope)
    }
}
