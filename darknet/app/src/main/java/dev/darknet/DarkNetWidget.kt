package dev.darknet

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import dev.darknet.data.Bytes
import dev.darknet.data.ShareStatus

/** Home screen widget: status, devices and data; tap to start or stop sharing. */
class DarkNetWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        val views = render(context, ShareService.status.value)
        ids.forEach { manager.updateAppWidget(it, views) }
    }

    companion object {
        fun refresh(context: Context, status: ShareStatus) {
            val manager = AppWidgetManager.getInstance(context) ?: return
            val ids = manager.getAppWidgetIds(ComponentName(context, DarkNetWidget::class.java))
            if (ids.isEmpty()) return
            val views = render(context, status)
            ids.forEach { manager.updateAppWidget(it, views) }
        }

        private fun render(context: Context, status: ShareStatus): RemoteViews {
            val views = RemoteViews(context.packageName, R.layout.widget_darknet)
            views.setTextViewText(
                R.id.widget_title,
                when (status.state) {
                    ShareStatus.State.ACTIVE -> "Compartiendo"
                    ShareStatus.State.STARTING -> "Iniciando…"
                    ShareStatus.State.STOPPING -> "Deteniendo…"
                    else -> "DarkNet"
                },
            )
            views.setTextViewText(
                R.id.widget_subtitle,
                if (status.isActive) "${status.devices.size} conectados · ${Bytes.format(status.total)}" else "Toca para compartir",
            )
            views.setInt(R.id.widget_icon, "setBackgroundResource", if (status.isActive) R.drawable.widget_dot_on else R.drawable.widget_dot_off)

            val flags = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            val tap = when {
                status.isActive -> PendingIntent.getForegroundService(
                    context, 31, Intent(context, ShareService::class.java).setAction(ShareService.ACTION_STOP), flags,
                )
                status.isBusy || ShizukuLink.state.value != ShizukuState.READY -> PendingIntent.getActivity(
                    context, 32, Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK), flags,
                )
                else -> PendingIntent.getForegroundService(context, 33, Intent(context, ShareService::class.java), flags)
            }
            views.setOnClickPendingIntent(R.id.widget_root, tap)
            return views
        }
    }
}
