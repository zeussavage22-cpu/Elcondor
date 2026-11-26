package com.elcondor.app.perapp

import android.accessibilityservice.AccessibilityService
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.view.accessibility.AccessibilityEvent
import com.elcondor.app.ElCondorApp

/**
 * Servicio de accesibilidad MÍNIMO: solo escucha TYPE_WINDOW_STATE_CHANGED para
 * saber qué app está en primer plano. No lee el contenido de las ventanas
 * (canRetrieveWindowContent=false) ni realiza acciones.
 *
 * Es la forma más fiable y eficiente (por eventos, sin sondeo) de detectar el
 * cambio de app sin root.
 */
class ForegroundAppService : AccessibilityService() {

    private val manager get() = (application as ElCondorApp).perAppDpi

    /** Caché "paquete/clase → ¿es una Activity?" (las consultas al PackageManager son caras). */
    private val activityCache = HashMap<String, Boolean>()

    override fun onServiceConnected() {
        super.onServiceConnected()
        manager.onServiceConnected()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = event.packageName?.toString() ?: return
        val cls = event.className?.toString() ?: return
        // Solo cuentan las Activities: popups, menús o toasts también emiten este
        // evento y no significan que el usuario haya cambiado de app.
        if (!isActivity(pkg, cls)) return
        manager.onForegroundActivity(pkg)
    }

    override fun onInterrupt() = Unit

    override fun onUnbind(intent: Intent?): Boolean {
        manager.onServiceDisconnected()
        return super.onUnbind(intent)
    }

    private fun isActivity(pkg: String, cls: String): Boolean =
        activityCache.getOrPut("$pkg/$cls") {
            try {
                packageManager.getActivityInfo(ComponentName(pkg, cls), 0)
                true
            } catch (_: PackageManager.NameNotFoundException) {
                false
            }
        }
}
