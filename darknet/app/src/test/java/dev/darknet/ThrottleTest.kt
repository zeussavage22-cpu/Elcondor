package dev.darknet

import dev.darknet.data.Throttle
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ThrottleTest {
    @Test
    fun `reads the throttling test`() {
        assertFalse(Throttle.verdict(250.0, 240.0).warn)
        assertTrue(Throttle.verdict(250.0, 1.6).warn)
        assertTrue(Throttle.verdict(250.0, 1.6).text.contains("480p"))
        assertTrue(Throttle.verdict(0.58, 0.6).text.contains("plan reducido"))
        assertTrue(Throttle.verdict(null, 10.0).warn)
        assertFalse(Throttle.verdict(80.0, null).warn)
    }
}
