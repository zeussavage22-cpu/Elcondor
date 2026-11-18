package dev.darknet.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

/** One finished sharing session. */
data class Session(val start: Long, val end: Long, val bytes: Long, val devices: Int) {
    val durationMs: Long get() = (end - start).coerceAtLeast(0)
}

/** Data shared per day (ISO date → bytes) and the latest sessions. Plain data, unit-testable. */
data class Usage(val days: Map<String, Long> = emptyMap(), val sessions: List<Session> = emptyList()) {

    fun add(day: LocalDate, bytes: Long): Usage {
        if (bytes <= 0) return this
        val k = day.toString()
        return copy(days = days + (k to (days[k] ?: 0L) + bytes))
    }

    fun addSession(s: Session) = copy(sessions = (listOf(s) + sessions).take(60))

    fun on(day: LocalDate) = days[day.toString()] ?: 0L

    fun lastDays(today: LocalDate, n: Int) = (0 until n).sumOf { on(today.minusDays(it.toLong())) }

    fun month(today: LocalDate) = days.entries.sumOf { (k, v) ->
        val d = runCatching { LocalDate.parse(k) }.getOrNull()
        if (d != null && d.year == today.year && d.month == today.month) v else 0L
    }

    /** Bytes since the plan last renewed on [resetDay]. */
    fun cycle(today: LocalDate, resetDay: Int): Long {
        val start = cycleStart(today, resetDay)
        return days.entries.sumOf { (k, v) ->
            val d = runCatching { LocalDate.parse(k) }.getOrNull()
            if (d != null && !d.isBefore(start) && !d.isAfter(today)) v else 0L
        }
    }

    /** What this cycle will add up to at the current daily pace. */
    fun forecast(today: LocalDate, resetDay: Int): Long {
        val start = cycleStart(today, resetDay)
        val end = start.plusMonths(1)
        val elapsed = (today.toEpochDay() - start.toEpochDay() + 1).coerceAtLeast(1)
        val total = (end.toEpochDay() - start.toEpochDay()).coerceAtLeast(1)
        return cycle(today, resetDay) * total / elapsed
    }

    fun series(today: LocalDate, n: Int) = (n - 1 downTo 0).map { today.minusDays(it.toLong()).let { d -> d to on(d) } }

    fun pruned(today: LocalDate): Usage {
        val oldest = today.minusDays(120)
        return copy(days = days.filterKeys { runCatching { !LocalDate.parse(it).isBefore(oldest) }.getOrDefault(false) })
    }

    fun toJson(): String = JSONObject()
        .put("days", JSONObject().apply { days.forEach { (k, v) -> put(k, v) } })
        .put("sessions", JSONArray(sessions.map {
            JSONObject().put("s", it.start).put("e", it.end).put("b", it.bytes).put("d", it.devices)
        }))
        .toString()

    companion object {
        fun cycleStart(today: LocalDate, resetDay: Int): LocalDate {
            val d = resetDay.coerceIn(1, 28)
            val thisMonth = today.withDayOfMonth(d)
            return if (today.dayOfMonth >= d) thisMonth else thisMonth.minusMonths(1)
        }

        fun fromJson(raw: String?): Usage = runCatching {
            val o = JSONObject(raw ?: return Usage())
            val days = mutableMapOf<String, Long>()
            o.optJSONObject("days")?.let { j -> j.keys().forEach { days[it] = j.optLong(it) } }
            val sessions = mutableListOf<Session>()
            o.optJSONArray("sessions")?.let { a ->
                for (i in 0 until a.length()) a.optJSONObject(i)?.let {
                    sessions += Session(it.optLong("s"), it.optLong("e"), it.optLong("b"), it.optInt("d"))
                }
            }
            Usage(days, sessions)
        }.getOrDefault(Usage())
    }
}

/** The app's usage history, kept in private preferences. */
object UsageStore {
    private val state = MutableStateFlow(Usage())
    val usage: StateFlow<Usage> = state.asStateFlow()
    @Volatile private var loaded = false

    fun load(context: Context) {
        if (loaded) return
        synchronized(this) {
            if (loaded) return
            state.value = Usage.fromJson(prefs(context).getString("usage", null)).pruned(LocalDate.now())
            loaded = true
        }
    }

    fun add(context: Context, bytes: Long) {
        load(context)
        state.value = state.value.add(LocalDate.now(), bytes)
    }

    fun addSession(context: Context, session: Session) {
        load(context)
        state.value = state.value.addSession(session)
        save(context)
    }

    fun save(context: Context) {
        load(context)
        prefs(context).edit().putString("usage", state.value.toJson()).apply()
    }

    fun clear(context: Context) {
        state.value = Usage()
        prefs(context).edit().remove("usage").apply()
    }

    private fun prefs(context: Context) = context.applicationContext.getSharedPreferences("usage", Context.MODE_PRIVATE)
}
