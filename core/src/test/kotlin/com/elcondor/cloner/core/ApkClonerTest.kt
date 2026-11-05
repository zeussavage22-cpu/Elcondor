package com.elcondor.cloner.core

import com.android.apksig.ApkVerifier
import com.elcondor.cloner.core.axml.AxmlDocument
import org.apache.commons.compress.archivers.zip.ZipFile
import org.junit.Test
import java.io.File
import java.nio.file.Files
import java.util.zip.ZipEntry
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ApkClonerTest {

    private val identity by lazy { SigningIdentity.generate() }

    private fun fixture(): File {
        val tmp = Files.createTempFile("demo", ".apk").toFile()
        javaClass.getResourceAsStream("/demo.apk")!!.use { input -> tmp.outputStream().use { input.copyTo(it) } }
        return tmp
    }

    private fun manifestOf(apk: File): AxmlDocument = ZipFile.builder().setFile(apk).get().use { zip ->
        AxmlDocument.parse(zip.getInputStream(zip.getEntry("AndroidManifest.xml")).readBytes())
    }

    /** Collects "tag@attr=value" for every string attribute, for easy assertions. */
    private fun dump(doc: AxmlDocument): List<String> {
        val out = mutableListOf<String>()
        doc.walk { e, _ ->
            for (a in e.attributes) {
                val v = doc.stringValue(a) ?: continue
                out += "${doc.elementName(e)}@${doc.attrName(a)}=$v"
            }
        }
        return out
    }

    @Test
    fun roundTripKeepsDocumentIntact() {
        val original = ZipFile.builder().setFile(fixture()).get().use { zip ->
            zip.getInputStream(zip.getEntry("AndroidManifest.xml")).readBytes()
        }
        val doc = AxmlDocument.parse(original)
        val again = AxmlDocument.parse(doc.toByteArray())
        assertEquals(doc.pool.strings, again.pool.strings)
        assertContentEquals(doc.resourceIds, again.resourceIds)
        assertEquals(dump(doc), dump(again))
        // A second serialization must be byte-for-byte stable.
        assertContentEquals(again.toByteArray(), AxmlDocument.parse(again.toByteArray()).toByteArray())
    }

    @Test
    fun clonesAndSignsApk() {
        val out = Files.createTempDirectory("clone").toFile()
        val result = ApkCloner(identity).clone(
            listOf(fixture()), out,
            CloneOptions(
                newPackage = "com.example.demo.clon1",
                newLabel = "Demo Clon",
                removePermissions = setOf("android.permission.CAMERA"),
            ),
        )
        assertEquals("com.example.demo", result.originalPackage)
        val apk = result.apks.single()
        val attrs = dump(manifestOf(apk))
        println(attrs.joinToString("\n"))

        val expected = listOf(
            "manifest@package=com.example.demo.clon1",
            "permission@name=com.example.demo.clon1.permission.C2D_MESSAGE",
            "uses-permission@name=com.example.demo.clon1.permission.C2D_MESSAGE",
            "uses-permission@name=android.permission.INTERNET",
            "application@name=com.example.demo.DemoApp",
            "application@label=Demo Clon",
            "application@process=com.example.demo.clon1:main",
            "activity@name=com.example.demo.MainActivity",
            "activity@label=Demo Clon",
            "activity@taskAffinity=com.example.demo.clon1.task",
            "activity@name=com.example.demo.SecondActivity",
            "activity-alias@name=com.example.demo.Alias",
            "activity-alias@targetActivity=com.example.demo.MainActivity",
            "service@name=com.example.demo.sync.SyncService",
            "service@permission=com.example.demo.clon1.permission.C2D_MESSAGE",
            "provider@authorities=com.example.demo.clon1.fileprovider;com.example.demo.clon1.com.thirdparty.sdk",
        )
        for (line in expected) assertTrue(line in attrs, "missing: $line")
        assertFalse(attrs.any { it.contains("CAMERA") }, "CAMERA should be removed")
        assertFalse(attrs.any { it.contains("sharedUserId") }, "sharedUserId should be removed")

        val info = ManifestEditor.readInfo(ZipFile.builder().setFile(apk).get().use { z ->
            z.getInputStream(z.getEntry("AndroidManifest.xml")).readBytes()
        })
        assertEquals(21, info.minSdk)
        assertNull(info.split)

        val verify = ApkVerifier.Builder(apk).build().verify()
        assertTrue(verify.isVerified, "signature errors: ${verify.errors}")
        assertTrue(verify.isVerifiedUsingV2Scheme)
        assertTrue(verify.isVerifiedUsingV1Scheme, "minSdk 21 needs v1")
        assertEquals(identity.certificate, verify.signerCertificates.single())

        ZipFile.builder().setFile(apk).get().use { zip ->
            val so = zip.getEntry("lib/arm64-v8a/libdemo.so")
            assertEquals(ZipEntry.STORED, so.method)
            assertEquals(0L, so.dataOffset % 16384, "native lib must be page aligned")
            val arsc = zip.getEntry("resources.arsc")
            if (arsc.method == ZipEntry.STORED) assertEquals(0L, arsc.dataOffset % 4)
            assertNull(zip.getEntry("META-INF/A.RSA"), "old signature must be dropped")
        }
    }

    @Test
    fun keyIsPersistedAndReloaded() {
        val file = File(Files.createTempDirectory("key").toFile(), "k.p12")
        val a = SigningIdentity.loadOrCreate(file, "pw".toCharArray())
        val b = SigningIdentity.loadOrCreate(file, "pw".toCharArray())
        assertEquals(a.certificate, b.certificate)
    }

    @Test
    fun packageNameValidation() {
        assertTrue(PackageNames.isValid("com.whatsapp.clon1"))
        assertFalse(PackageNames.isValid("whatsapp"))
        assertFalse(PackageNames.isValid("com.1abc"))
        assertFalse(PackageNames.isValid("com..x"))
    }

    /** Optional: `./gradlew :core:test -PtestApk=/path/app.apk` clones a real APK. */
    @Test
    fun clonesExternalApkIfProvided() {
        val path = System.getProperty("testApk") ?: return
        val out = File(System.getProperty("testOut"), "external").apply { deleteRecursively(); mkdirs() }
        val src = File(path)
        val pkg = ManifestEditor.readInfo(ZipFile.builder().setFile(src).get().use { z ->
            z.getInputStream(z.getEntry("AndroidManifest.xml")).readBytes()
        }).packageName
        val result = ApkCloner(identity).clone(listOf(src), out, CloneOptions("$pkg.clon1", "Clon"))
        val verify = ApkVerifier.Builder(result.apks.single()).build().verify()
        assertTrue(verify.isVerified, "signature errors: ${verify.errors}")
        println("External clone written to ${result.apks.single()}")
    }
}
