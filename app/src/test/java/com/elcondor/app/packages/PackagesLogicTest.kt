package com.elcondor.app.packages

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PackagesLogicTest {

    @Test fun `parses pm list output`() {
        val out = """
            package:com.google.android.youtube
            package:android
            package:com.android.chrome
            package:com.android.chrome
            garbage line
        """.trimIndent()
        assertEquals(
            listOf("android", "com.android.chrome", "com.google.android.youtube"),
            PackageParsers.parsePackageList(out),
        )
    }

    @Test fun `rejects shell injection in package names`() {
        assertTrue(PackageParsers.isValidPackageName("com.example.app_2"))
        assertTrue(PackageParsers.isValidPackageName("android"))
        listOf("com.x; reboot", "com.x && rm -rf /", "\$(id)", "com.x`id`", "", ".com", "com..x", "com.x\nreboot")
            .forEach { assertFalse(it, PackageParsers.isValidPackageName(it)) }
    }

    @Test fun `pm list drops lines with invalid names`() {
        assertEquals(emptyList<String>(), PackageParsers.parsePackageList("package:com.x;reboot"))
    }

    @Test fun `uninstall output`() {
        assertTrue(PackageParsers.isUninstallSuccess("Success\n"))
        assertFalse(PackageParsers.isUninstallSuccess("Failure [not installed for 0]"))
        assertEquals("not installed for 0", PackageParsers.failureReason("Failure [not installed for 0]", ""))
    }

    @Test fun `install-existing output`() {
        assertTrue(PackageParsers.isInstallExistingSuccess("Package com.x installed for user: 0"))
        assertFalse(PackageParsers.isInstallExistingSuccess("Package com.x doesn't exist"))
    }

    @Test fun `removal policy`() {
        val policy = RemovalPolicy(dynamicProtected = setOf("com.my.launcher"))
        assertEquals(RemovalRisk.BLOCKED, policy.riskOf("com.android.systemui"))
        assertEquals(RemovalRisk.BLOCKED, policy.riskOf("com.android.providers.settings"))
        assertEquals(RemovalRisk.BLOCKED, policy.riskOf("com.my.launcher"))
        assertEquals(RemovalRisk.CAUTION, policy.riskOf("com.google.android.gms"))
        assertEquals(RemovalRisk.CAUTION, policy.riskOf("com.android.cellbroadcastreceiver"))
        assertEquals(RemovalRisk.NORMAL, policy.riskOf("com.facebook.appmanager"))
    }
}
