package dev.darknet

import dev.darknet.data.SignalLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SignalLevelTest {
    @Test
    fun `bars from dBm`() {
        assertEquals(4 to -92, SignalLevel.of(-112, -92))
        assertEquals(3 to -98, SignalLevel.of(-98, null))
        assertEquals(2 to -112, SignalLevel.of(null, -112))
        assertEquals(0 to -130, SignalLevel.of(-130, null))
        assertNull(SignalLevel.of(Int.MAX_VALUE, null))
    }
}
