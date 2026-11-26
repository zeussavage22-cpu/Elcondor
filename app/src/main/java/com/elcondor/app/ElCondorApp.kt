package com.elcondor.app

import android.app.Application
import com.elcondor.app.display.DisplayController
import com.elcondor.app.packages.PackageRepository
import com.elcondor.app.packages.TrashStore
import com.elcondor.app.shell.ShizukuShellManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Contenedor de dependencias mínimo (sin Hilt para mantenerlo simple).
 *
 * ShizukuShellManager se crea aquí para que sus listeners estén registrados
 * antes de que ShizukuProvider entregue el binder.
 */
class ElCondorApp : Application() {

    /**
     * Scope que sobrevive a Activities/ViewModels. Main.immediate = el estado
     * se muta en un solo hilo; el I/O se mueve a Dispatchers.IO donde toca.
     */
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    lateinit var shell: ShizukuShellManager
        private set
    lateinit var display: DisplayController
        private set
    lateinit var packages: PackageRepository
        private set

    override fun onCreate() {
        super.onCreate()
        shell = ShizukuShellManager(this)
        display = DisplayController(shell, appScope)
        packages = PackageRepository(this, shell, TrashStore(this))
    }
}
