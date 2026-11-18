package dev.darknet

import dev.darknet.data.DnsChoice
import dev.darknet.data.DnsProbe
import dev.darknet.data.Health
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NetCheckTest {

    @Test
    fun `dns query has header and question`() {
        val q = DnsProbe.query(0x1234, "a.bc")
        assertEquals(12 + 6 + 4, q.size)
        assertEquals(0x12, q[0].toInt())
        assertEquals(0x34, q[1].toInt())
        assertEquals(1, q[5].toInt())
        assertEquals(1, q[12].toInt())
        assertEquals('a'.code, q[13].toInt())
        assertEquals(2, q[14].toInt())
    }

    @Test
    fun `dns answer must match id and have no error`() {
        val ok = byteArrayOf(0x12, 0x34, 0x81.toByte(), 0x80.toByte()) + ByteArray(8)
        assertTrue(DnsProbe.isAnswer(ok, ok.size, 0x1234))
        assertFalse(DnsProbe.isAnswer(ok, ok.size, 0x1235))
        val refused = ok.copyOf().also { it[3] = 0x85.toByte() }
        assertFalse(DnsProbe.isAnswer(refused, refused.size, 0x1234))
        assertFalse(DnsProbe.isAnswer(ok, 8, 0x1234))
    }

    @Test
    fun `every dns choice has servers`() {
        DnsChoice.labels.keys.filter { it != "auto" }.forEach { assertEquals(it, 4, DnsChoice.servers[it]?.size) }
        DnsChoice.autoCandidates.forEach { assertTrue(DnsChoice.servers.containsKey(it.first)) }
    }

    @Test
    fun `health goes offline after three failures and counts the drop`() {
        var h = Health()
        h = h.add(30).add(null).add(null)
        assertFalse(h.offline)
        h = h.add(null)
        assertTrue(h.offline)
        assertEquals(1, h.drops)
        h = h.add(null)
        assertEquals(1, h.drops)
        h = h.add(40)
        assertFalse(h.offline)
        h = h.add(null).add(null).add(null)
        assertEquals(2, h.drops)
    }

    @Test
    fun `health grades quality`() {
        var h = Health()
        assertEquals(Health.Quality.UNKNOWN, h.quality)
        repeat(10) { h = h.add(25) }
        assertEquals(Health.Quality.GREAT, h.quality)
        repeat(10) { h = h.add(120) }
        assertEquals(Health.Quality.GOOD, h.quality)
        var slow = Health()
        repeat(6) { slow = slow.add(400) }
        assertTrue(slow.slow)
        assertEquals(Health.Quality.BAD, slow.quality)
        var jumpy = Health()
        listOf(20L, 200, 20, 200, 20, 200).forEach { jumpy = jumpy.add(it) }
        assertEquals(Health.Quality.UNSTABLE, jumpy.quality)
        var window = Health()
        repeat(100) { window = window.add(10) }
        assertEquals(Health.WINDOW, window.samples.size)
    }
}
