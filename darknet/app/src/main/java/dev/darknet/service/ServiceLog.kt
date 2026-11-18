package dev.darknet.service

import android.util.Log
import org.json.JSONArray
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** A small in-memory log the app can show, so problems on a phone can be reported. */
internal object ServiceLog {
    private const val TAG = "DarkNet"
    private const val CAPACITY = 400
    private val lines = ArrayDeque<String>()
    private val time = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)

    fun info(message: String) = add("I", message)
    fun warn(message: String) = add("W", message)
    fun error(message: String, failure: Throwable? = null) =
        add("E", if (failure == null) message else "$message: ${failure.javaClass.simpleName}: ${failure.message}")

    @Synchronized
    private fun add(level: String, message: String) {
        Log.println(if (level == "E") Log.ERROR else Log.INFO, TAG, message)
        lines.addLast("${time.format(Date())} $level $message")
        while (lines.size > CAPACITY) lines.removeFirst()
    }

    @Synchronized
    fun dump(): String = JSONArray(lines.toList()).toString()
}
