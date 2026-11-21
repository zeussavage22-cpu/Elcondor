package dev.darknet

import dev.darknet.data.ShareStatus
import dev.darknet.ui.parseChecks
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProTest {

    @Test
    fun `speed icon labels`() {
        assertEquals("230" to "Mb/s", SpeedIcon.label(28_750_000))
        assertEquals("23" to "Mb/s", SpeedIcon.label(2_875_000))
        assertEquals("2.5" to "Mb/s", SpeedIcon.label(312_500))
        assertEquals("80" to "Kb/s", SpeedIcon.label(10_000))
    }

    @Test
    fun `checks parse and bad json becomes a failed check`() {
        val c = parseChecks("""[{"name":"A","state":"ok","detail":"d"},{"name":"B","state":"warn","detail":""}]""")
        assertEquals(listOf("A", "B"), c.map { it.name })
        assertEquals("fail", parseChecks("nope").single().state)
    }

    @Test
    fun `status carries engine details`() {
        val s = ShareStatus.parse(
            """{"state":"active","fastPath":true,"fastPath6":true,"udpFast":true,"congestion":"bbr","mtu":1420,"combined":2,"clients":[]}""",
        )
        assertTrue(s.fastPath6 && s.udpFast)
        assertEquals("bbr", s.congestion)
        assertEquals(1420, s.mtu)
        assertEquals(2, s.combined)
    }
}
