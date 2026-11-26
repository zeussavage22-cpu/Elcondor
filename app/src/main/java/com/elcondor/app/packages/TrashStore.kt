package com.elcondor.app.packages

import android.content.Context
import android.util.Log
import androidx.core.util.AtomicFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

data class TrashEntry(
    val packageName: String,
    val label: String,
    val removedAt: Long,
)

/**
 * Historial persistente de lo desinstalado por la app (JSON en filesDir).
 *
 * No es la única fuente de verdad: [PackageRepository] lo concilia con
 * `pm list packages -u`, así que aunque este archivo se pierda (o el borrado
 * se hiciera desde ADB) los paquetes siguen apareciendo en la papelera.
 * Este archivo aporta lo que pm no sabe: el nombre legible y la fecha.
 *
 * AtomicFile: si la app muere a mitad de escritura, se conserva la versión anterior.
 */
class TrashStore(context: Context) {

    private val file = AtomicFile(File(context.filesDir, "trash.json"))
    private val mutex = Mutex()
    private var cache: MutableMap<String, TrashEntry>? = null

    suspend fun entries(): Map<String, TrashEntry> = mutex.withLock { loadLocked().toMap() }

    suspend fun add(entry: TrashEntry) = mutate { it[entry.packageName] = entry }

    suspend fun remove(packageName: String) = mutate { it.remove(packageName) }

    /** Olvida entradas de paquetes que ya no están desinstalados (restaurados por fuera). */
    suspend fun retainOnly(packageNames: Set<String>) = mutate { it.keys.retainAll(packageNames) }

    private suspend fun mutate(block: (MutableMap<String, TrashEntry>) -> Unit) = mutex.withLock {
        val map = loadLocked()
        val before = map.toMap()
        block(map)
        if (map != before) saveLocked(map)
    }

    private suspend fun loadLocked(): MutableMap<String, TrashEntry> {
        cache?.let { return it }
        val loaded = withContext(Dispatchers.IO) {
            try {
                if (!file.baseFile.exists()) return@withContext mutableMapOf()
                val array = JSONArray(String(file.readFully(), Charsets.UTF_8))
                (0 until array.length())
                    .map(array::getJSONObject)
                    .map {
                        TrashEntry(
                            packageName = it.getString("package"),
                            label = it.optString("label", it.getString("package")),
                            removedAt = it.optLong("removedAt", 0L),
                        )
                    }
                    .filter { PackageParsers.isValidPackageName(it.packageName) }
                    .associateByTo(mutableMapOf()) { it.packageName }
            } catch (e: Exception) {
                // Archivo corrupto: empezamos vacío; la conciliación con pm recupera la lista.
                Log.w(TAG, "No se pudo leer la papelera", e)
                mutableMapOf()
            }
        }
        cache = loaded
        return loaded
    }

    private suspend fun saveLocked(map: Map<String, TrashEntry>) = withContext(Dispatchers.IO) {
        val json = JSONArray().apply {
            map.values.sortedBy { it.packageName }.forEach { entry ->
                put(
                    JSONObject()
                        .put("package", entry.packageName)
                        .put("label", entry.label)
                        .put("removedAt", entry.removedAt),
                )
            }
        }
        val stream = file.startWrite()
        try {
            stream.write(json.toString().toByteArray(Charsets.UTF_8))
            file.finishWrite(stream)
        } catch (e: Exception) {
            file.failWrite(stream)
            Log.e(TAG, "No se pudo guardar la papelera", e)
        }
    }

    private companion object {
        const val TAG = "TrashStore"
    }
}
