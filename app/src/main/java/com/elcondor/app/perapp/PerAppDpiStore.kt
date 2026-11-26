package com.elcondor.app.perapp

import android.content.Context
import androidx.core.content.edit
import com.elcondor.app.packages.PackageParsers

/**
 * Persistencia del DPI por app (SharedPreferences).
 *
 * Además de las reglas guarda el "modo regla": si la app muere mientras una app
 * con regla tenía su DPI aplicado, al volver a arrancar sabemos que hay que
 * restaurar el DPI base.
 */
class PerAppDpiStore(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences("per_app_dpi", Context.MODE_PRIVATE)

    fun rules(): Map<String, Int> =
        prefs.all
            .filterKeys { it.startsWith(RULE_PREFIX) }
            .mapNotNull { (key, value) ->
                val pkg = key.removePrefix(RULE_PREFIX)
                (value as? Int)?.takeIf { PackageParsers.isValidPackageName(pkg) }?.let { pkg to it }
            }
            .toMap()

    fun putRule(packageName: String, dpi: Int) = prefs.edit { putInt(RULE_PREFIX + packageName, dpi) }

    fun removeRule(packageName: String) = prefs.edit { remove(RULE_PREFIX + packageName) }

    /** true mientras hay un DPI de regla aplicado (y el base pendiente de restaurar). */
    var ruleModeActive: Boolean
        get() = prefs.getBoolean(KEY_RULE_MODE, false)
        set(value) = prefs.edit(commit = true) { putBoolean(KEY_RULE_MODE, value) }

    /** DPI base a restaurar. null = sin override (`wm density reset`). */
    var baseline: Int?
        get() = prefs.getInt(KEY_BASELINE, NO_OVERRIDE).takeIf { it != NO_OVERRIDE }
        set(value) = prefs.edit(commit = true) { putInt(KEY_BASELINE, value ?: NO_OVERRIDE) }

    private companion object {
        const val RULE_PREFIX = "dpi:"
        const val KEY_RULE_MODE = "rule_mode_active"
        const val KEY_BASELINE = "baseline"
        const val NO_OVERRIDE = -1
    }
}
