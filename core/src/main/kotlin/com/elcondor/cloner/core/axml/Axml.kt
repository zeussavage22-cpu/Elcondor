package com.elcondor.cloner.core.axml

import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Reader/writer for Android's compiled binary XML format (the format of AndroidManifest.xml
 * inside an APK). Written from the public format description in AOSP's ResourceTypes.h.
 *
 * The document is parsed into a small mutable model; unknown chunks are preserved verbatim.
 */
object ChunkType {
    const val STRING_POOL = 0x0001
    const val XML = 0x0003
    const val XML_START_NAMESPACE = 0x0100
    const val XML_END_NAMESPACE = 0x0101
    const val XML_START_ELEMENT = 0x0102
    const val XML_END_ELEMENT = 0x0103
    const val XML_CDATA = 0x0104
    const val XML_RESOURCE_MAP = 0x0180
}

/** Res_value data types. */
object ValueType {
    const val NULL = 0x00
    const val REFERENCE = 0x01
    const val ATTRIBUTE = 0x02
    const val STRING = 0x03
    const val INT_DEC = 0x10
    const val INT_HEX = 0x11
    const val INT_BOOLEAN = 0x12
}

const val NO_INDEX = -1
const val ANDROID_NS = "http://schemas.android.com/apk/res/android"

class AxmlFormatException(message: String) : Exception(message)

class Attribute(
    var ns: Int,
    var name: Int,
    var rawValue: Int,
    var dataType: Int,
    var data: Int,
    var res0: Int = 0,
)

sealed class Node {
    var lineNumber: Int = 0
    var comment: Int = NO_INDEX
}

class StartNamespace(var prefix: Int, var uri: Int) : Node()
class EndNamespace(var prefix: Int, var uri: Int) : Node()
class EndElement(var ns: Int, var name: Int) : Node()
class CData(var data: Int, var dataType: Int, var typedData: Int, var res0: Int = 0) : Node()
class RawChunk(val bytes: ByteArray) : Node()

class StartElement(var ns: Int, var name: Int) : Node() {
    val attributes: MutableList<Attribute> = mutableListOf()
    var idAttribute: Attribute? = null
    var classAttribute: Attribute? = null
    var styleAttribute: Attribute? = null
}

class StringPool(
    val strings: MutableList<String>,
    var utf8: Boolean,
    var flags: Int,
    /** Style offsets (relative to the style data) and the raw style data, kept verbatim. */
    val styleOffsets: IntArray,
    val styleData: ByteArray,
) {
    val styleCount: Int get() = styleOffsets.size

    companion object {
        private const val UTF8_FLAG = 0x100
        private const val SORTED_FLAG = 0x1

        fun empty() = StringPool(mutableListOf(), utf8 = false, flags = 0, IntArray(0), ByteArray(0))

        fun parse(b: Bytes, pos: Int, headerSize: Int, size: Int): StringPool {
            val stringCount = b.i32(pos + 8)
            val styleCount = b.i32(pos + 12)
            val flags = b.i32(pos + 16)
            val stringsStart = b.i32(pos + 20)
            val stylesStart = b.i32(pos + 24)
            if (stringCount < 0 || styleCount < 0) throw AxmlFormatException("Bad string pool counts")
            val utf8 = flags and UTF8_FLAG != 0
            val offsetsPos = pos + headerSize
            val strings = ArrayList<String>(stringCount)
            for (i in 0 until stringCount) {
                val off = pos + stringsStart + b.i32(offsetsPos + 4 * i)
                strings.add(if (utf8) b.utf8String(off) else b.utf16String(off))
            }
            val styleOffsets = IntArray(styleCount) { b.i32(offsetsPos + 4 * stringCount + 4 * it) }
            val styleData = if (styleCount > 0 && stylesStart > 0) {
                b.bytes.copyOfRange(pos + stylesStart, pos + size)
            } else ByteArray(0)
            return StringPool(strings, utf8, flags, styleOffsets, styleData)
        }
    }

    fun write(out: Writer) {
        val data = ByteArrayOutputStream()
        val offsets = IntArray(strings.size)
        for ((i, s) in strings.withIndex()) {
            offsets[i] = data.size()
            if (utf8) writeUtf8(data, s) else writeUtf16(data, s)
        }
        while (data.size() % 4 != 0) data.write(0)
        val headerSize = 28
        val stringsStart = headerSize + 4 * strings.size + 4 * styleCount
        val stylesStart = if (styleCount > 0) stringsStart + data.size() else 0
        val total = stringsStart + data.size() + styleData.size
        out.u16(ChunkType.STRING_POOL)
        out.u16(headerSize)
        out.i32(total)
        out.i32(strings.size)
        out.i32(styleCount)
        // Appended strings break any sort order, so drop the SORTED flag.
        out.i32((flags and SORTED_FLAG.inv() and UTF8_FLAG.inv()) or (if (utf8) UTF8_FLAG else 0))
        out.i32(stringsStart)
        out.i32(stylesStart)
        offsets.forEach { out.i32(it) }
        styleOffsets.forEach { out.i32(it) }
        out.bytes(data.toByteArray())
        out.bytes(styleData)
    }

    private fun writeUtf8(out: ByteArrayOutputStream, s: String) {
        val encoded = s.toByteArray(Charsets.UTF_8)
        writeUtf8Length(out, s.length)
        writeUtf8Length(out, encoded.size)
        out.write(encoded)
        out.write(0)
    }

    private fun writeUtf8Length(out: ByteArrayOutputStream, len: Int) {
        if (len > 0x7fff) throw AxmlFormatException("String too long for UTF-8 pool")
        if (len > 0x7f) {
            out.write(0x80 or (len shr 8))
            out.write(len and 0xff)
        } else {
            out.write(len)
        }
    }

    private fun writeUtf16(out: ByteArrayOutputStream, s: String) {
        val len = s.length
        if (len > 0x7fff) {
            val hi = 0x8000 or (len ushr 16)
            out.write(hi and 0xff); out.write(hi shr 8)
            out.write(len and 0xff); out.write((len shr 8) and 0xff)
        } else {
            out.write(len and 0xff); out.write(len shr 8)
        }
        for (c in s) {
            out.write(c.code and 0xff); out.write(c.code shr 8)
        }
        out.write(0); out.write(0)
    }
}

class AxmlDocument(
    val pool: StringPool,
    var resourceIds: IntArray,
    val nodes: MutableList<Node>,
) {
    // ---------------------------------------------------------------- strings

    fun str(index: Int): String? = if (index in pool.strings.indices) pool.strings[index] else null

    /** Returns the index of [s] in the pool, appending it if missing. */
    fun intern(s: String): Int {
        val existing = pool.strings.lastIndexOf(s)
        if (existing >= resourceIds.size) return existing
        pool.strings.add(s)
        return pool.strings.size - 1
    }

    // ---------------------------------------------------------------- attributes

    fun resId(attr: Attribute): Int =
        if (attr.name in resourceIds.indices) resourceIds[attr.name] else 0

    fun attrName(attr: Attribute): String? = str(attr.name)

    fun elementName(e: StartElement): String? = str(e.name)

    fun findAttr(e: StartElement, resId: Int): Attribute? = e.attributes.firstOrNull { resId(it) == resId }

    /** Finds an attribute without namespace by name (for example `package` on `<manifest>`). */
    fun findPlainAttr(e: StartElement, name: String): Attribute? =
        e.attributes.firstOrNull { it.ns == NO_INDEX && attrName(it) == name && resId(it) == 0 }

    fun stringValue(attr: Attribute): String? = when {
        attr.rawValue != NO_INDEX -> str(attr.rawValue)
        attr.dataType == ValueType.STRING -> str(attr.data)
        else -> null
    }

    fun setString(attr: Attribute, value: String) {
        val idx = intern(value)
        attr.rawValue = idx
        attr.dataType = ValueType.STRING
        attr.data = idx
    }

    fun intValue(attr: Attribute): Int? = when (attr.dataType) {
        ValueType.INT_DEC, ValueType.INT_HEX, ValueType.INT_BOOLEAN -> attr.data
        else -> null
    }

    fun removeAttr(e: StartElement, attr: Attribute) {
        e.attributes.remove(attr)
        if (e.idAttribute === attr) e.idAttribute = null
        if (e.classAttribute === attr) e.classAttribute = null
        if (e.styleAttribute === attr) e.styleAttribute = null
    }

    /**
     * Adds an `android:` attribute with a string value, keeping attributes sorted by resource id
     * (the framework's attribute lookup relies on that order).
     */
    fun addAndroidStringAttr(e: StartElement, name: String, resId: Int, value: String): Attribute {
        val nameIdx = ensureAttributeName(name, resId)
        val nsIdx = intern(ANDROID_NS)
        val attr = Attribute(nsIdx, nameIdx, NO_INDEX, ValueType.STRING, 0)
        setString(attr, value)
        val insertAt = e.attributes.indexOfFirst { resId(it).toLong() and 0xffffffffL > resId.toLong() and 0xffffffffL }
        if (insertAt < 0) e.attributes.add(attr) else e.attributes.add(insertAt, attr)
        return attr
    }

    /**
     * Makes sure an attribute name string that maps to [resId] exists in the resource-mapped
     * prefix of the string pool, inserting it (and shifting every string reference) if needed.
     */
    private fun ensureAttributeName(name: String, resId: Int): Int {
        resourceIds.indexOf(resId).let { if (it >= 0 && pool.strings[it] == name) return it }
        val position = resourceIds.size
        if (position < pool.styleCount) throw AxmlFormatException("Cannot insert attribute name before styled strings")
        shiftStringRefs(position)
        pool.strings.add(position, name)
        resourceIds = resourceIds + resId
        return position
    }

    private fun shiftStringRefs(from: Int) {
        fun s(i: Int) = if (i != NO_INDEX && i >= from) i + 1 else i
        for (node in nodes) {
            node.comment = s(node.comment)
            when (node) {
                is StartNamespace -> { node.prefix = s(node.prefix); node.uri = s(node.uri) }
                is EndNamespace -> { node.prefix = s(node.prefix); node.uri = s(node.uri) }
                is StartElement -> {
                    node.ns = s(node.ns); node.name = s(node.name)
                    for (a in node.attributes) {
                        a.ns = s(a.ns); a.name = s(a.name); a.rawValue = s(a.rawValue)
                        if (a.dataType == ValueType.STRING) a.data = s(a.data)
                    }
                }
                is EndElement -> { node.ns = s(node.ns); node.name = s(node.name) }
                is CData -> {
                    node.data = s(node.data)
                    if (node.dataType == ValueType.STRING) node.typedData = s(node.typedData)
                }
                is RawChunk -> Unit
            }
        }
    }

    // ---------------------------------------------------------------- elements

    val elements: List<StartElement> get() = nodes.filterIsInstance<StartElement>()

    fun rootElement(): StartElement? = nodes.firstOrNull { it is StartElement } as StartElement?

    /** Removes the element starting at [start] together with all of its children. */
    fun removeElement(start: StartElement) {
        val from = nodes.indexOf(start)
        if (from < 0) return
        var depth = 0
        var to = from
        while (to < nodes.size) {
            when (nodes[to]) {
                is StartElement -> depth++
                is EndElement -> { depth--; if (depth == 0) break }
                else -> Unit
            }
            to++
        }
        if (to >= nodes.size) throw AxmlFormatException("Unbalanced element")
        repeat(to - from + 1) { nodes.removeAt(from) }
    }

    /** Visits every element with the stack of its ancestors (outermost first). */
    fun walk(visitor: (element: StartElement, ancestors: List<StartElement>) -> Unit) {
        val stack = ArrayList<StartElement>()
        for (node in nodes.toList()) {
            when (node) {
                is StartElement -> { visitor(node, stack.toList()); stack.add(node) }
                is EndElement -> if (stack.isNotEmpty()) stack.removeAt(stack.size - 1)
                else -> Unit
            }
        }
    }

    // ---------------------------------------------------------------- serialization

    fun toByteArray(): ByteArray {
        val body = Writer()
        pool.write(body)
        if (resourceIds.isNotEmpty()) {
            body.u16(ChunkType.XML_RESOURCE_MAP)
            body.u16(8)
            body.i32(8 + 4 * resourceIds.size)
            resourceIds.forEach { body.i32(it) }
        }
        for (node in nodes) writeNode(body, node)
        val out = Writer()
        out.u16(ChunkType.XML)
        out.u16(8)
        out.i32(8 + body.size)
        out.bytes(body.toByteArray())
        return out.toByteArray()
    }

    private fun writeNode(w: Writer, node: Node) {
        if (node is RawChunk) { w.bytes(node.bytes); return }
        val (type, extSize) = when (node) {
            is StartNamespace -> ChunkType.XML_START_NAMESPACE to 8
            is EndNamespace -> ChunkType.XML_END_NAMESPACE to 8
            is StartElement -> ChunkType.XML_START_ELEMENT to 20 + 20 * node.attributes.size
            is EndElement -> ChunkType.XML_END_ELEMENT to 8
            is CData -> ChunkType.XML_CDATA to 12
            is RawChunk -> error("unreachable")
        }
        w.u16(type)
        w.u16(16)
        w.i32(16 + extSize)
        w.i32(node.lineNumber)
        w.i32(node.comment)
        when (node) {
            is StartNamespace -> { w.i32(node.prefix); w.i32(node.uri) }
            is EndNamespace -> { w.i32(node.prefix); w.i32(node.uri) }
            is EndElement -> { w.i32(node.ns); w.i32(node.name) }
            is CData -> { w.i32(node.data); w.u16(8); w.u8(node.res0); w.u8(node.dataType); w.i32(node.typedData) }
            is StartElement -> {
                fun idx(a: Attribute?) = if (a == null) 0 else node.attributes.indexOfFirst { it === a } + 1
                w.i32(node.ns); w.i32(node.name)
                w.u16(20); w.u16(20); w.u16(node.attributes.size)
                w.u16(idx(node.idAttribute)); w.u16(idx(node.classAttribute)); w.u16(idx(node.styleAttribute))
                for (a in node.attributes) {
                    w.i32(a.ns); w.i32(a.name); w.i32(a.rawValue)
                    w.u16(8); w.u8(a.res0); w.u8(a.dataType); w.i32(a.data)
                }
            }
            is RawChunk -> Unit
        }
    }

    companion object {
        fun parse(bytes: ByteArray): AxmlDocument {
            val b = Bytes(bytes)
            if (bytes.size < 8 || b.u16(0) != ChunkType.XML) throw AxmlFormatException("Not a binary XML file")
            val fileSize = minOf(b.i32(4).let { if (it <= 0) bytes.size else it }, bytes.size)
            var pool: StringPool? = null
            var resIds = IntArray(0)
            val nodes = mutableListOf<Node>()
            var pos = b.u16(2)
            while (pos + 8 <= fileSize) {
                val type = b.u16(pos)
                val headerSize = b.u16(pos + 2)
                val size = b.i32(pos + 4)
                if (size < 8 || pos + size > bytes.size) throw AxmlFormatException("Bad chunk at $pos")
                when (type) {
                    ChunkType.STRING_POOL -> if (pool == null) pool = StringPool.parse(b, pos, headerSize, size)
                    ChunkType.XML_RESOURCE_MAP -> resIds = IntArray((size - headerSize) / 4) { b.i32(pos + headerSize + 4 * it) }
                    in ChunkType.XML_START_NAMESPACE..ChunkType.XML_CDATA -> nodes.add(parseNode(b, pos, type, headerSize))
                    else -> nodes.add(RawChunk(bytes.copyOfRange(pos, pos + size)))
                }
                pos += size
            }
            return AxmlDocument(pool ?: StringPool.empty(), resIds, nodes)
        }

        private fun parseNode(b: Bytes, pos: Int, type: Int, headerSize: Int): Node {
            val e = pos + headerSize
            val node: Node = when (type) {
                ChunkType.XML_START_NAMESPACE -> StartNamespace(b.i32(e), b.i32(e + 4))
                ChunkType.XML_END_NAMESPACE -> EndNamespace(b.i32(e), b.i32(e + 4))
                ChunkType.XML_END_ELEMENT -> EndElement(b.i32(e), b.i32(e + 4))
                ChunkType.XML_CDATA -> CData(b.i32(e), b.u8(e + 7), b.i32(e + 8), b.u8(e + 6))
                else -> {
                    val el = StartElement(b.i32(e), b.i32(e + 4))
                    val attrStart = b.u16(e + 8)
                    val attrSize = b.u16(e + 10).let { if (it < 20) 20 else it }
                    val count = b.u16(e + 12)
                    for (i in 0 until count) {
                        val a = e + attrStart + i * attrSize
                        el.attributes.add(Attribute(b.i32(a), b.i32(a + 4), b.i32(a + 8), b.u8(a + 15), b.i32(a + 16), b.u8(a + 14)))
                    }
                    fun ref(i: Int) = if (i in 1..el.attributes.size) el.attributes[i - 1] else null
                    el.idAttribute = ref(b.u16(e + 14))
                    el.classAttribute = ref(b.u16(e + 16))
                    el.styleAttribute = ref(b.u16(e + 18))
                    el
                }
            }
            node.lineNumber = b.i32(pos + 8)
            node.comment = b.i32(pos + 12)
            return node
        }
    }
}

/** Little-endian random access helper over a byte array. */
class Bytes(val bytes: ByteArray) {
    private val buf = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
    fun u8(pos: Int): Int = bytes[pos].toInt() and 0xff
    fun u16(pos: Int): Int = buf.getShort(pos).toInt() and 0xffff
    fun i32(pos: Int): Int = buf.getInt(pos)

    fun utf16String(pos: Int): String {
        var p = pos
        var len = u16(p); p += 2
        if (len and 0x8000 != 0) { len = ((len and 0x7fff) shl 16) or u16(p); p += 2 }
        val chars = CharArray(len) { u16(p + 2 * it).toChar() }
        return String(chars)
    }

    fun utf8String(pos: Int): String {
        var p = pos
        // UTF-16 length (unused), then UTF-8 byte length; each is 1 or 2 bytes.
        if (u8(p) and 0x80 != 0) p += 2 else p += 1
        var len = u8(p)
        if (len and 0x80 != 0) { len = ((len and 0x7f) shl 8) or u8(p + 1); p += 2 } else p += 1
        return String(bytes, p, len, Charsets.UTF_8)
    }
}

class Writer {
    private val out = ByteArrayOutputStream()
    val size: Int get() = out.size()
    fun u8(v: Int) = out.write(v and 0xff)
    fun u16(v: Int) { out.write(v and 0xff); out.write((v shr 8) and 0xff) }
    fun i32(v: Int) { u16(v and 0xffff); u16((v ushr 16) and 0xffff) }
    fun bytes(b: ByteArray) = out.write(b)
    fun toByteArray(): ByteArray = out.toByteArray()
}
