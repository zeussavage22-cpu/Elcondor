package com.elcondor.cloner.core

import java.io.BufferedOutputStream
import java.io.ByteArrayOutputStream
import java.io.Closeable
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.CRC32
import java.util.zip.Deflater
import java.util.zip.DeflaterOutputStream
import java.util.zip.ZipEntry

/**
 * Minimal ZIP writer that copies already-compressed entry data verbatim and aligns the data of
 * uncompressed entries (as zipalign does), which Android requires for resources.arsc and for
 * native libraries loaded directly from the APK.
 */
class AlignedZipWriter(file: File) : Closeable {
    private class CentralEntry(
        val name: ByteArray, val method: Int, val crc: Long,
        val compressedSize: Long, val size: Long, val offset: Long, val utf8: Boolean,
    )

    private val out: OutputStream = BufferedOutputStream(file.outputStream(), 1 shl 16)
    private var position = 0L
    private val entries = mutableListOf<CentralEntry>()

    /** Copies an entry whose (possibly compressed) bytes come from [rawData]. */
    fun addRaw(name: String, method: Int, crc: Long, compressedSize: Long, size: Long, rawData: InputStream, alignment: Int) {
        val nameBytes = name.toByteArray(Charsets.UTF_8)
        val utf8 = nameBytes.size != name.length
        val offset = position
        val extra = if (method == ZipEntry.STORED && alignment > 1) alignmentExtra(offset, nameBytes.size, alignment) else ByteArray(0)
        writeLocalHeader(nameBytes, utf8, method, crc, compressedSize, size, extra)
        val copied = rawData.copyTo(CountingStream())
        if (copied != compressedSize) throw IllegalStateException("Size mismatch for $name: $copied != $compressedSize")
        entries += CentralEntry(nameBytes, method, crc, compressedSize, size, offset, utf8)
    }

    /** Adds [data] compressed with DEFLATE. */
    fun addDeflated(name: String, data: ByteArray) {
        val crc = CRC32().apply { update(data) }.value
        val buffer = ByteArrayOutputStream()
        DeflaterOutputStream(buffer, Deflater(Deflater.BEST_COMPRESSION, true)).use { it.write(data) }
        val compressed = buffer.toByteArray()
        addRaw(name, ZipEntry.DEFLATED, crc, compressed.size.toLong(), data.size.toLong(), compressed.inputStream(), 0)
    }

    override fun close() {
        val cdStart = position
        for (e in entries) {
            u32(0x02014b50); u16(20); u16(20); u16(if (e.utf8) 0x0800 else 0); u16(e.method)
            u16(DOS_TIME); u16(DOS_DATE); u32(e.crc); u32(e.compressedSize); u32(e.size)
            u16(e.name.size); u16(0); u16(0); u16(0); u16(0); u32(0); u32(e.offset)
            bytes(e.name)
        }
        val cdSize = position - cdStart
        u32(0x06054b50); u16(0); u16(0); u16(entries.size); u16(entries.size)
        u32(cdSize); u32(cdStart); u16(0)
        out.close()
    }

    private fun writeLocalHeader(name: ByteArray, utf8: Boolean, method: Int, crc: Long, csize: Long, size: Long, extra: ByteArray) {
        if (csize > 0xffffffffL || size > 0xffffffffL || position > 0xffffffffL) throw IllegalStateException("APK too large")
        u32(0x04034b50); u16(20); u16(if (utf8) 0x0800 else 0); u16(method)
        u16(DOS_TIME); u16(DOS_DATE); u32(crc); u32(csize); u32(size)
        u16(name.size); u16(extra.size)
        bytes(name); bytes(extra)
    }

    /** Builds an "Android ZIP alignment" extra field (0xd935) padding the data to [alignment]. */
    private fun alignmentExtra(offset: Long, nameLength: Int, alignment: Int): ByteArray {
        val dataStartWithoutPadding = offset + 30 + nameLength + 6
        val padding = ((alignment - dataStartWithoutPadding % alignment) % alignment).toInt()
        val field = ByteArray(6 + padding)
        field[0] = 0x35; field[1] = 0xd9.toByte()
        field[2] = ((2 + padding) and 0xff).toByte(); field[3] = ((2 + padding) shr 8).toByte()
        field[4] = (alignment and 0xff).toByte(); field[5] = (alignment shr 8).toByte()
        return field
    }

    private fun u16(v: Int) { out.write(v and 0xff); out.write((v shr 8) and 0xff); position += 2 }
    private fun u32(v: Long) { u16((v and 0xffff).toInt()); u16(((v shr 16) and 0xffff).toInt()) }
    private fun bytes(b: ByteArray) { out.write(b); position += b.size }

    private inner class CountingStream : OutputStream() {
        override fun write(b: Int) { out.write(b); position++ }
        override fun write(b: ByteArray, off: Int, len: Int) { out.write(b, off, len); position += len }
    }

    private companion object {
        // 1981-01-01 00:00, a fixed timestamp keeps the output reproducible.
        const val DOS_TIME = 0
        const val DOS_DATE = (1 shl 9) or (1 shl 5) or 1
    }
}
