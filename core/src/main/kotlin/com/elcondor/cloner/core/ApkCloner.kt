package com.elcondor.cloner.core

import com.android.apksig.ApkSigner
import com.android.apksig.KeyConfig
import org.apache.commons.compress.archivers.zip.ZipFile
import java.io.File

/** Result of cloning: the signed APKs (base first) ready to be installed together. */
data class CloneResult(
    val originalPackage: String,
    val newPackage: String,
    val apks: List<File>,
)

enum class CloneStep { READING, REWRITING, SIGNING, DONE }

/**
 * Turns the APK files of an installed app (base + optional splits) into an independently
 * installable copy with a different package name, signed with [identity].
 */
class ApkCloner(private val identity: SigningIdentity) {

    fun clone(
        inputs: List<File>,
        outDir: File,
        options: CloneOptions,
        onProgress: (step: CloneStep, current: Int, total: Int) -> Unit = { _, _, _ -> },
    ): CloneResult {
        require(inputs.isNotEmpty()) { "No APK files given" }
        onProgress(CloneStep.READING, 0, inputs.size)
        val infos = inputs.map { it to ManifestEditor.readInfo(readManifest(it)) }
        val base = infos.firstOrNull { it.second.split == null }
            ?: throw IllegalArgumentException("No base APK found")
        val ordered = listOf(base) + infos.filter { it !== base }
        val minSdk = base.second.minSdk

        outDir.mkdirs()
        val outputs = ordered.mapIndexed { i, (file, info) ->
            val name = if (info.split == null) "base.apk" else "split_${info.split.replace(Regex("[^A-Za-z0-9_.-]"), "_")}.apk"
            val unsigned = File(outDir, "$name.unsigned")
            val signed = File(outDir, name)
            onProgress(CloneStep.REWRITING, i, ordered.size)
            rewriteApk(file, unsigned, options)
            onProgress(CloneStep.SIGNING, i, ordered.size)
            sign(unsigned, signed, minSdk)
            unsigned.delete()
            signed
        }
        onProgress(CloneStep.DONE, ordered.size, ordered.size)
        return CloneResult(base.second.packageName, options.newPackage, outputs)
    }

    private fun readManifest(apk: File): ByteArray = ZipFile.builder().setFile(apk).get().use { zip ->
        val entry = zip.getEntry(MANIFEST) ?: throw IllegalArgumentException("${apk.name} has no $MANIFEST")
        zip.getInputStream(entry).use { it.readBytes() }
    }

    private fun rewriteApk(input: File, output: File, options: CloneOptions) {
        ZipFile.builder().setFile(input).get().use { zip ->
            AlignedZipWriter(output).use { out ->
                val seen = HashSet<String>()
                for (entry in zip.entriesInPhysicalOrder) {
                    val name = entry.name
                    if (entry.isDirectory || !seen.add(name) || isSignatureFile(name)) continue
                    if (name == MANIFEST) {
                        val original = zip.getInputStream(entry).use { it.readBytes() }
                        out.addDeflated(MANIFEST, ManifestEditor.rewrite(original, options))
                        continue
                    }
                    // Uncompressed native libraries must be page aligned (16 KiB covers both
                    // 4 KiB and 16 KiB page devices); other uncompressed files 4-byte aligned.
                    val alignment = if (name.endsWith(".so")) 16384 else 4
                    zip.getRawInputStream(entry).use { raw ->
                        out.addRaw(name, entry.method, entry.crc, entry.compressedSize, entry.size, raw, alignment)
                    }
                }
            }
        }
    }

    private fun sign(input: File, output: File, minSdk: Int) {
        val signer = ApkSigner.SignerConfig.Builder("CERT", KeyConfig.Jca(identity.privateKey), listOf(identity.certificate)).build()
        ApkSigner.Builder(listOf(signer))
            .setInputApk(input)
            .setOutputApk(output)
            .setMinSdkVersion(minSdk)
            .setV1SigningEnabled(minSdk < 24)
            .setV2SigningEnabled(true)
            .setV3SigningEnabled(true)
            .setV4SigningEnabled(false)
            .setAlignmentPreserved(true)
            .build()
            .sign()
    }

    companion object {
        const val MANIFEST = "AndroidManifest.xml"

        fun isSignatureFile(name: String): Boolean {
            val upper = name.uppercase()
            if (upper == "STAMP-CERT-SHA256") return true
            if (!upper.startsWith("META-INF/") || upper.count { it == '/' } != 1) return false
            return upper == "META-INF/MANIFEST.MF" ||
                upper.endsWith(".SF") || upper.endsWith(".RSA") || upper.endsWith(".DSA") || upper.endsWith(".EC")
        }
    }
}
