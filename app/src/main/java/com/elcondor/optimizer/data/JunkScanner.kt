package com.elcondor.optimizer.data

import android.os.Environment
import java.io.File
import java.io.RandomAccessFile
import java.security.MessageDigest

enum class JunkType(val title: String, val description: String, val safe: Boolean) {
    TEMP("Archivos temporales y logs", "Archivos .tmp, .log, .temp y restos de instalaciones", true),
    THUMBS("Miniaturas", "Vistas previas de imágenes; el sistema las regenera cuando las necesita", true),
    EMPTY("Carpetas vacías", "Carpetas sin ningún archivo dentro", true),
    APK("Instaladores APK", "Archivos de instalación de apps que ya no hacen falta", true),
    TRASH("Papeleras ocultas", "Carpetas .trash / .Trash de apps de terceros", true),
    OLD_DOWNLOADS("Descargas antiguas (+90 días)", "Archivos en Descargas que no se modifican desde hace meses", false),
    DUPLICATES("Archivos duplicados", "Copias idénticas; se conserva siempre el original más antiguo", false),
    LARGE("Archivos grandes (+100 MB)", "Revisa si todavía los necesitas", false),
}

data class JunkItem(val file: File, val size: Long, val type: JunkType, val note: String? = null)

data class JunkResult(val items: Map<JunkType, List<JunkItem>>, val scannedFiles: Int, val durationMs: Long) {
    fun size(t: JunkType) = items[t].orEmpty().sumOf { it.size }
    val safeBytes get() = JunkType.entries.filter { it.safe }.sumOf { size(it) }
    val totalBytes get() = JunkType.entries.sumOf { size(it) }
}

object JunkScanner {
    @Volatile var lastResult: JunkResult? = null

    private val tempExt = setOf("tmp", "temp", "log", "dmp", "chck")
    private val apkExt = setOf("apk", "apks", "xapk", "apkm")
    private const val LARGE = 100L * 1024 * 1024
    private const val DUP_MIN = 1L * 1024 * 1024

    fun scan(onProgress: (Int, String) -> Unit = { _, _ -> }): JunkResult {
        val t0 = System.currentTimeMillis()
        val root = Environment.getExternalStorageDirectory()
        val skip = setOf(File(root, "Android/data").absolutePath, File(root, "Android/obb").absolutePath)
        val downloads = File(root, Environment.DIRECTORY_DOWNLOADS).absolutePath
        val now = System.currentTimeMillis()
        val out = JunkType.entries.associateWith { mutableListOf<JunkItem>() }
        val bySize = HashMap<Long, MutableList<File>>()
        var count = 0

        // Recorrido post-orden iterativo para poder detectar carpetas vacías
        fun walk(dir: File, depth: Int): Boolean { // devuelve true si la carpeta quedó vacía
            if (depth > 24 || dir.absolutePath in skip) return false
            val children = dir.listFiles() ?: return false
            var empty = true
            val name = dir.name.lowercase()
            val inThumbs = name == ".thumbnails" || name == "thumbnails" && dir.parentFile?.name == "DCIM"
            val inTrash = name.startsWith(".trash") || name == ".recycle"
            for (f in children) {
                if (f.isDirectory) {
                    if (!walk(f, depth + 1)) empty = false
                    continue
                }
                empty = false
                count++
                if (count % 500 == 0) onProgress(count, f.parent ?: "")
                val len = f.length()
                val ext = f.extension.lowercase()
                val lname = f.name.lowercase()
                when {
                    inThumbs -> out.getValue(JunkType.THUMBS) += JunkItem(f, len, JunkType.THUMBS)
                    inTrash -> out.getValue(JunkType.TRASH) += JunkItem(f, len, JunkType.TRASH)
                    ext in tempExt || lname.startsWith("~$") || lname == ".ds_store" || lname == "thumbs.db" ->
                        out.getValue(JunkType.TEMP) += JunkItem(f, len, JunkType.TEMP)
                    ext in apkExt -> out.getValue(JunkType.APK) += JunkItem(f, len, JunkType.APK)
                    else -> {
                        if (len >= LARGE) out.getValue(JunkType.LARGE) += JunkItem(f, len, JunkType.LARGE)
                        if (f.absolutePath.startsWith(downloads) && now - f.lastModified() > 90L * 86_400_000)
                            out.getValue(JunkType.OLD_DOWNLOADS) += JunkItem(f, len, JunkType.OLD_DOWNLOADS)
                        if (len >= DUP_MIN && len < 2L * 1024 * 1024 * 1024) bySize.getOrPut(len) { mutableListOf() } += f
                    }
                }
            }
            if (empty && depth > 1 && !dir.name.startsWith(".")) {
                out.getValue(JunkType.EMPTY) += JunkItem(dir, 0, JunkType.EMPTY)
            }
            return empty
        }
        walk(root, 0)

        onProgress(count, "Buscando duplicados…")
        for ((size, files) in bySize) {
            if (files.size < 2) continue
            files.groupBy { quickHash(it, size) }.values.filter { it.size > 1 }.forEach { group ->
                val sorted = group.sortedBy { it.lastModified() }
                val original = sorted.first()
                sorted.drop(1).forEach {
                    out.getValue(JunkType.DUPLICATES) += JunkItem(it, size, JunkType.DUPLICATES, "Copia de ${original.name}")
                }
            }
        }
        out.values.forEach { l -> l.sortByDescending { it.size } }
        return JunkResult(out, count, System.currentTimeMillis() - t0).also { lastResult = it }
    }

    /** Hash del inicio, centro y final del archivo (rápido y fiable para detectar copias). */
    private fun quickHash(f: File, size: Long): String = try {
        val md = MessageDigest.getInstance("MD5")
        val chunk = 64 * 1024
        RandomAccessFile(f, "r").use { r ->
            val buf = ByteArray(chunk)
            for (pos in longArrayOf(0, size / 2, maxOf(0, size - chunk))) {
                r.seek(pos)
                val n = r.read(buf)
                if (n > 0) md.update(buf, 0, n)
            }
        }
        md.digest().joinToString("") { "%02x".format(it) }
    } catch (_: Exception) { f.absolutePath }

    fun delete(items: Collection<JunkItem>): Pair<Int, Long> {
        var n = 0; var bytes = 0L
        // Archivos primero, luego carpetas (más profundas primero)
        val (dirs, files) = items.partition { it.file.isDirectory }
        for (i in files) if (i.file.delete()) { n++; bytes += i.size }
        for (d in dirs.sortedByDescending { it.file.absolutePath.length }) {
            if (d.file.listFiles().isNullOrEmpty() && d.file.delete()) n++
        }
        lastResult = null
        return n to bytes
    }
}
