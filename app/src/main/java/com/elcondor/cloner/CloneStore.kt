package com.elcondor.cloner

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class CloneRecord(val packageName: String, val originalPackage: String, val label: String, val createdAt: Long)

/** Remembers the clones made with this app (kept in private SharedPreferences). */
class CloneStore(context: Context) {
    private val prefs = context.getSharedPreferences("clones", Context.MODE_PRIVATE)

    fun all(): List<CloneRecord> {
        val json = prefs.getString(KEY, null) ?: return emptyList()
        return runCatching {
            val arr = JSONArray(json)
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                CloneRecord(o.getString("pkg"), o.getString("orig"), o.getString("label"), o.optLong("at"))
            }
        }.getOrDefault(emptyList())
    }

    fun put(record: CloneRecord) = save(all().filter { it.packageName != record.packageName } + record)

    fun remove(pkg: String) = save(all().filter { it.packageName != pkg })

    private fun save(records: List<CloneRecord>) {
        val arr = JSONArray()
        records.forEach {
            arr.put(JSONObject().put("pkg", it.packageName).put("orig", it.originalPackage).put("label", it.label).put("at", it.createdAt))
        }
        prefs.edit().putString(KEY, arr.toString()).apply()
    }

    private companion object { const val KEY = "records" }
}
