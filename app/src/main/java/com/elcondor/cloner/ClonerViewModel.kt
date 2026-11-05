package com.elcondor.cloner

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.elcondor.cloner.core.ApkCloner
import com.elcondor.cloner.core.CloneOptions
import com.elcondor.cloner.core.CloneStep
import com.elcondor.cloner.core.SigningIdentity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

sealed interface JobState {
    data object Idle : JobState
    data class Working(val text: String) : JobState
    data class Done(val packageName: String, val label: String) : JobState
    data class Failed(val message: String) : JobState
}

data class CloneItem(val record: CloneRecord, val installed: Boolean)

class ClonerViewModel(app: Application) : AndroidViewModel(app) {
    private val context get() = getApplication<Application>()
    private val store = CloneStore(app)

    private val _apps = MutableStateFlow<List<InstalledApp>>(emptyList())
    val apps: StateFlow<List<InstalledApp>> = _apps

    private val _loading = MutableStateFlow(true)
    val loading: StateFlow<Boolean> = _loading

    private val _clones = MutableStateFlow<List<CloneItem>>(emptyList())
    val clones: StateFlow<List<CloneItem>> = _clones

    private val _job = MutableStateFlow<JobState>(JobState.Idle)
    val job: StateFlow<JobState> = _job

    private var pending: CloneRecord? = null

    init {
        refreshApps()
        viewModelScope.launch {
            InstallEvents.results.collect { result ->
                val record = pending ?: return@collect
                if (result.packageName != null && result.packageName != record.packageName) return@collect
                pending = null
                File(context.cacheDir, "clones").deleteRecursively()
                if (result.success) {
                    store.put(record)
                    _job.value = JobState.Done(record.packageName, record.label)
                    refreshApps()
                } else {
                    _job.value = JobState.Failed(result.message ?: "Falló la instalación")
                }
                refreshClones()
            }
        }
    }

    fun refreshApps() {
        viewModelScope.launch {
            _loading.value = true
            _apps.value = withContext(Dispatchers.IO) { AppScanner.load(context) }
            _loading.value = false
            refreshClones()
        }
    }

    fun refreshClones() {
        viewModelScope.launch {
            _clones.value = withContext(Dispatchers.IO) {
                store.all().map { CloneItem(it, AppScanner.isInstalled(context, it.packageName)) }
                    .sortedByDescending { it.record.createdAt }
            }
        }
    }

    fun forget(pkg: String) {
        store.remove(pkg)
        refreshClones()
    }

    fun suggestPackage(original: String) = AppScanner.suggestPackage(context, original)

    fun dismissJob() {
        if (_job.value !is JobState.Working) _job.value = JobState.Idle
    }

    fun clone(app: InstalledApp, newPackage: String, label: String, removePermissions: Set<String>) {
        if (_job.value is JobState.Working) return
        _job.value = JobState.Working("Preparando…")
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val identity = SigningIdentity.loadOrCreate(File(context.filesDir, "clone-signing.p12"), KEY_PASSWORD)
                    val outDir = File(context.cacheDir, "clones/$newPackage").apply { deleteRecursively(); mkdirs() }
                    val options = CloneOptions(newPackage, label.ifBlank { null }, removePermissions)
                    val result = ApkCloner(identity).clone(app.apkFiles.map(::File), outDir, options) { step, i, n ->
                        val part = if (n > 1) " (${minOf(i + 1, n)}/$n)" else ""
                        _job.value = JobState.Working(
                            when (step) {
                                CloneStep.READING -> "Leyendo la app original…"
                                CloneStep.REWRITING -> "Creando la copia$part…"
                                CloneStep.SIGNING -> "Firmando$part…"
                                CloneStep.DONE -> "Listo para instalar"
                            }
                        )
                    }
                    pending = CloneRecord(newPackage, app.packageName, label.ifBlank { app.label }, System.currentTimeMillis())
                    _job.value = JobState.Working("Instalando… confirma en la ventana del sistema")
                    CloneInstaller.install(context, result.apks, newPackage)
                }
            } catch (e: Throwable) {
                pending = null
                _job.value = JobState.Failed(e.message ?: e.javaClass.simpleName)
            }
        }
    }

    private companion object {
        val KEY_PASSWORD = "elcondor-cloner".toCharArray()
    }
}
