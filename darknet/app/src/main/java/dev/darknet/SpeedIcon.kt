package dev.darknet

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.drawable.Icon

/** A status-bar icon that shows the current speed as a number over its unit. */
object SpeedIcon {
    private const val SIZE = 96
    private var lastKey = ""
    private var last: Icon? = null

    private val number by lazy { Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
    } }
    private val unit by lazy { Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
        textSize = 34f
    } }

    /** Value and unit to draw for [bytesPerSecond], e.g. "23" and "Mb/s". */
    fun label(bytesPerSecond: Long): Pair<String, String> {
        val mbps = bytesPerSecond * 8 / 1_000_000.0
        return when {
            mbps >= 100 -> "%.0f".format(mbps) to "Mb/s"
            mbps >= 10 -> "%.0f".format(mbps) to "Mb/s"
            mbps >= 1 -> "%.1f".format(mbps) to "Mb/s"
            else -> "%.0f".format(bytesPerSecond * 8 / 1000.0) to "Kb/s"
        }
    }

    fun of(bytesPerSecond: Long): Icon {
        val (value, u) = label(bytesPerSecond)
        val key = "$value $u"
        last?.let { if (key == lastKey) return it }
        val bitmap = Bitmap.createBitmap(SIZE, SIZE, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        number.textSize = when (value.length) { 1, 2 -> 62f; 3 -> 52f; else -> 40f }
        canvas.drawText(value, SIZE / 2f, 56f, number)
        canvas.drawText(u, SIZE / 2f, 92f, unit)
        return Icon.createWithBitmap(bitmap).also { last = it; lastKey = key }
    }
}
