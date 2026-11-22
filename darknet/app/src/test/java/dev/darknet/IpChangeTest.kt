package dev.darknet

import dev.darknet.data.IpChange
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class IpChangeTest {
    @Test
    fun `a new IPv6 network counts even when IPv4 stays`() {
        assertTrue(IpChange.changed("172.56.73.240", "172.56.73.240", "2607:fb90:1:2:a::1", "2607:fb90:9:8:b::2"))
        assertFalse(IpChange.changed("172.56.73.240", "172.56.73.240", "2607:fb90:1:2:a::1", "2607:fb90:1:2:ffff::9"))
        assertTrue(IpChange.changed("172.56.73.240", "172.56.10.1", "", ""))
        assertFalse(IpChange.changed("", "", "", ""))
        assertEquals("2607:fb90:1:2::", IpChange.short6("2607:fb90:1:2:a::1"))
    }
}
