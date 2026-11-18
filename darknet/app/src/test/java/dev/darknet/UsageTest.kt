package dev.darknet

import dev.darknet.data.Session
import dev.darknet.data.Usage
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class UsageTest {
    private val today = LocalDate.of(2026, 11, 18)

    @Test
    fun `totals by day, week, month and plan cycle`() {
        val u = Usage().add(today, 100).add(today, 50).add(today.minusDays(1), 20).add(LocalDate.of(2026, 10, 30), 7)
        assertEquals(150, u.on(today))
        assertEquals(170, u.lastDays(today, 7))
        assertEquals(170, u.month(today))
        assertEquals(177, u.cycle(today, 25)) // cycle started on 25 Oct
        assertEquals(170, u.cycle(today, 1))
    }

    @Test
    fun `cycle start rolls back a month before the reset day`() {
        assertEquals(LocalDate.of(2026, 11, 1), Usage.cycleStart(today, 1))
        assertEquals(LocalDate.of(2026, 10, 25), Usage.cycleStart(today, 25))
    }

    @Test
    fun `forecast scales the cycle so far to the whole cycle`() {
        // 18 days into a 30-day November cycle, 18 GB used: about 30 GB.
        val u = (0 until 18).fold(Usage()) { acc, d -> acc.add(LocalDate.of(2026, 11, 1).plusDays(d.toLong()), 1_000_000_000) }
        assertEquals(30_000_000_000, u.forecast(today, 1))
    }

    @Test
    fun `round trips through json`() {
        val u = Usage().add(today, 1234).addSession(Session(1, 70_001, 1234, 3))
        assertEquals(u, Usage.fromJson(u.toJson()))
        assertEquals(Usage(), Usage.fromJson("nope"))
    }
}
