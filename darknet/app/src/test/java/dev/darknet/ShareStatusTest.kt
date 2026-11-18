package dev.darknet

import dev.darknet.data.Bytes
import dev.darknet.data.ShareStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ShareStatusTest {

    private val active = """
        {"state":"active","detail":"","since":1000,"fastPath":true,"upstreamOk":true,"up":1000,"down":9000,
         "clients":[{"name":"laptop","ip":"192.168.43.10","mac":"aa:bb","usb":false}]}
    """.trimIndent()

    @Test
    fun `parses an active status`() {
        val s = ShareStatus.parse(active)
        assertEquals(ShareStatus.State.ACTIVE, s.state)
        assertTrue(s.fastPath)
        assertTrue(s.upstreamOk)
        assertEquals(10_000, s.total)
        assertEquals("laptop", s.devices.single().name)
    }

    @Test
    fun `speed comes from the change between reports`() {
        val before = ShareStatus.parse(active)
        val after = ShareStatus.parse(active.replace("\"down\":9000", "\"down\":19000"), before, 2_000)
        assertEquals(5_000, after.speed)
    }

    @Test
    fun `garbage becomes an error, unknown states become idle`() {
        assertEquals(ShareStatus.State.ERROR, ShareStatus.parse("nope").state)
        assertEquals(ShareStatus.State.IDLE, ShareStatus.parse("{\"state\":\"weird\"}").state)
    }

    @Test
    fun `formats bytes and durations`() {
        assertEquals("999 B", Bytes.format(999))
        assertEquals("1.5 MB", Bytes.format(1_500_000))
        assertEquals("12 GB", Bytes.format(12_000_000_000))
        assertEquals("1:02:03", Bytes.duration(3_723_000))
        assertEquals("0:05", Bytes.duration(5_000))
    }
}
