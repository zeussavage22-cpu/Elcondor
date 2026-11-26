package com.elcondor.optimizer.data

import android.content.ContentUris
import android.content.Context
import android.content.IntentSender
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File
import java.io.FileInputStream
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.security.MessageDigest

enum class JunkType(val title: String, val description: String, val safe: Boolean) {
    TEMP("Temporales y logs", "Restos que las apps ya no usan", true),
    THUMBS("Miniaturas", "El sistema las vuelve a crear si hacen falta", true),
    EMPTY("Carpetas vacías", "Carpetas sin contenido", true),
    APK("Instaladores APK", "Apps que ya instalaste", true),
    TRASH("Papeleras ocultas", "Restos en papeleras de otras apps", true),
    SCREENSHOTS("Capturas antiguas", "Capturas de pantalla de hace más de 30 días", false),
    OLD_DOWNLOADS("Descargas antiguas", "Sin abrir desde hace más de 90 días", false),
    DUPLICATES("Duplicados", "Copias idénticas; siempre se conserva el original", false),
    LARGE("Archivos grandes", "Más de 100 MB cada uno", false),
}

/** Un elemento encontrado: archivo directo (con acceso total) o elemento multimedia (MediaStore). */
data class JunkItem(
    val id: String,
    val name: String,
    val location: String,
    val size: Long,
    val type: JunkType,
    val file: File? = null,
    val uri: Uri? = null,
    val note: String? = null,
    val modified: Long = 0,
)

data class JunkResult(val items: Map<JunkType, List<JunkItem>>, val scannedFiles: Int, val durationMs: Long, val fullAccess: Boolean) {
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
    private const val DAY = 86_400_000L

    fun scan(ctx: Context, onProgress: (Int, String) -> Unit = { _, _ -> }): JunkResult =
        if (Perms.hasAllFiles()) scanFiles(onProgress) else scanMedia(ctx, onProgress)

    // ------------------------------------------------------------------ Acceso total
    private fun scanFiles(onProgress: (Int, String) -> Unit): JunkResult {
        val t0 = System.currentTimeMillis()
        val root = Environment.getExternalStorageDirectory()
        val skip = setOf(File(root, "Android/data").absolutePath, File(root, "Android/obb").absolutePath)
        val downloads = File(root, Environment.DIRECTORY_DOWNLOADS).absolutePath
        val now = System.currentTimeMillis()
        val out = JunkType.entries.associateWith { mutableListOf<JunkItem>() }
        val bySize = HashMap<Long, MutableList<File>>()
        var count = 0

        fun item(f: File, len: Long, t: JunkType, note: String? = null) =
            JunkItem(f.absolutePath, f.name, f.parent.orEmpty().substringAfter("/emulated/0"), len, t, file = f, note = note, modified = f.lastModified())

        fun walk(dir: File, depth: Int): Boolean {
            if (depth > 24 || dir.absolutePath in skip) return false
            val children = dir.listFiles() ?: return false
            var empty = true
            val name = dir.name.lowercase()
            val inThumbs = name == ".thumbnails"
            val inTrash = name.startsWith(".trash") || name == ".recycle"
            val inShots = name == "screenshots"
            for (f in children) {
                if (f.isDirectory) { if (!walk(f, depth + 1)) empty = false; continue }
                empty = false
                count++
                if (count % 400 == 0) onProgress(count, f.parent ?: "")
                val len = f.length()
                val ext = f.extension.lowercase()
                val lname = f.name.lowercase()
                when {
                    inThumbs -> out.getValue(JunkType.THUMBS) += item(f, len, JunkType.THUMBS)
                    inTrash -> out.getValue(JunkType.TRASH) += item(f, len, JunkType.TRASH)
                    ext in tempExt || lname.startsWith("~$") || lname == ".ds_store" || lname == "thumbs.db" ->
                        out.getValue(JunkType.TEMP) += item(f, len, JunkType.TEMP)
                    ext in apkExt -> out.getValue(JunkType.APK) += item(f, len, JunkType.APK)
                    else -> {
                        if (inShots && now - f.lastModified() > 30 * DAY) out.getValue(JunkType.SCREENSHOTS) += item(f, len, JunkType.SCREENSHOTS)
                        else if (len >= LARGE) out.getValue(JunkType.LARGE) += item(f, len, JunkType.LARGE)
                        else if (f.absolutePath.startsWith(downloads) && now - f.lastModified() > 90 * DAY)
                            out.getValue(JunkType.OLD_DOWNLOADS) += item(f, len, JunkType.OLD_DOWNLOADS)
                        if (len >= DUP_MIN && len < 4L * 1024 * 1024 * 1024) bySize.getOrPut(len) { mutableListOf() } += f
                    }
                }
            }
            // depth > 1: nunca se tocan carpetas estándar (DCIM, Music, Download…)
            if (empty && depth > 1 && !dir.name.startsWith(".")) out.getValue(JunkType.EMPTY) += item(dir, 0, JunkType.EMPTY)
            return empty
        }
        walk(root, 0)

        onProgress(count, "Buscando duplicados…")
        for ((size, files) in bySize) {
            if (files.size < 2) continue
            files.groupBy { fileHash(it, size) }.values.filter { it.size > 1 }.forEach { g ->
                val sorted = g.sortedBy { it.lastModified() }
                sorted.drop(1).forEach { out.getValue(JunkType.DUPLICATES) += item(it, size, JunkType.DUPLICATES, "Copia de ${sorted.first().name}") }
            }
        }
        dedupeAcrossTypes(out)
        out.values.forEach { l -> l.sortByDescending { it.size } }
        return JunkResult(out, count, System.currentTimeMillis() - t0, true).also { lastResult = it }
    }

    // ------------------------------------------------------------------ Solo multimedia
    private fun scanMedia(ctx: Context, onProgress: (Int, String) -> Unit): JunkResult {
        val t0 = System.currentTimeMillis()
        val out = JunkType.entries.associateWith { mutableListOf<JunkItem>() }
        val now = System.currentTimeMillis()
        val bySize = HashMap<Long, MutableList<JunkItem>>()
        var count = 0
        val collection = MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL)
        val proj = arrayOf(
            MediaStore.MediaColumns._ID, MediaStore.MediaColumns.DISPLAY_NAME, MediaStore.MediaColumns.SIZE,
            MediaStore.MediaColumns.DATE_MODIFIED, MediaStore.MediaColumns.RELATIVE_PATH, MediaStore.Files.FileColumns.MEDIA_TYPE,
        )
        val sel = "${MediaStore.Files.FileColumns.MEDIA_TYPE} != ${MediaStore.Files.FileColumns.MEDIA_TYPE_NONE}"
        try {
            ctx.contentResolver.query(collection, proj, sel, null, null)?.use { c ->
                val iId = c.getColumnIndexOrThrow(proj[0]); val iName = c.getColumnIndexOrThrow(proj[1])
                val iSize = c.getColumnIndexOrThrow(proj[2]); val iMod = c.getColumnIndexOrThrow(proj[3])
                val iPath = c.getColumnIndexOrThrow(proj[4])
                while (c.moveToNext()) {
                    count++
                    if (count % 400 == 0) onProgress(count, "Fotos, videos y audio…")
                    val id = c.getLong(iId)
                    val size = c.getLong(iSize)
                    val mod = c.getLong(iMod) * 1000
                    val path = c.getString(iPath).orEmpty()
                    val name = c.getString(iName).orEmpty()
                    val uri = ContentUris.withAppendedId(collection, id)
                    fun mk(t: JunkType, note: String? = null) = JunkItem("m$id", name, "/$path", size, t, uri = uri, note = note, modified = mod)
                    when {
                        path.contains("Screenshots", true) && now - mod > 30 * DAY -> out.getValue(JunkType.SCREENSHOTS) += mk(JunkType.SCREENSHOTS)
                        size >= LARGE -> out.getValue(JunkType.LARGE) += mk(JunkType.LARGE)
                        path.startsWith("Download", true) && now - mod > 90 * DAY -> out.getValue(JunkType.OLD_DOWNLOADS) += mk(JunkType.OLD_DOWNLOADS)
                    }
                    if (size >= DUP_MIN) bySize.getOrPut(size) { mutableListOf() } += mk(JunkType.DUPLICATES)
                }
            }
        } catch (_: Exception) { }

        onProgress(count, "Buscando duplicados…")
        for ((size, list) in bySize) {
            if (list.size < 2) continue
            list.groupBy { uriHash(ctx, it.uri!!, size) }.values.filter { it.size > 1 }.forEach { g ->
                val sorted = g.sortedBy { it.modified }
                sorted.drop(1).forEach { out.getValue(JunkType.DUPLICATES) += it.copy(note = "Copia de ${sorted.first().name}") }
            }
        }
        dedupeAcrossTypes(out)
        out.values.forEach { l -> l.sortByDescending { it.size } }
        return JunkResult(out, count, System.currentTimeMillis() - t0, false).also { lastResult = it }
    }

    /** Un archivo que es duplicado no se lista además como grande/antiguo (evita contarlo dos veces). */
    private fun dedupeAcrossTypes(out: Map<JunkType, MutableList<JunkItem>>) {
        val dupIds = out.getValue(JunkType.DUPLICATES).map { it.id }.toSet()
        listOf(JunkType.LARGE, JunkType.OLD_DOWNLOADS, JunkType.SCREENSHOTS).forEach { t -> out.getValue(t).removeAll { it.id in dupIds } }
    }

    private fun digestOf(read: (Long, ByteArray) -> Int, size: Long): String {
        val md = MessageDigest.getInstance("MD5")
        val chunk = 64 * 1024
        val buf = ByteArray(chunk)
        for (pos in longArrayOf(0, size / 2, maxOf(0, size - chunk))) {
            val n = read(pos, buf); if (n > 0) md.update(buf, 0, n)
        }
        return md.digest().joinToString("") { "%02x".format(it) }
    }

    private fun fileHash(f: File, size: Long): String = try {
        RandomAccessFile(f, "r").use { r -> digestOf({ p, b -> r.seek(p); r.read(b) }, size) }
    } catch (_: Exception) { f.absolutePath }

    private fun uriHash(ctx: Context, uri: Uri, size: Long): String = try {
        ctx.contentResolver.openFileDescriptor(uri, "r")!!.use { pfd ->
            FileInputStream(pfd.fileDescriptor).channel.use { ch ->
                digestOf({ p, b -> ch.position(p); ch.read(ByteBuffer.wrap(b)) }, size)
            }
        }
    } catch (_: Exception) { uri.toString() }

    /** Borra los archivos directos. Devuelve (borrados, bytes) y la lista de URIs que requieren confirmación del sistema. */
    fun delete(items: Collection<JunkItem>): Triple<Int, Long, List<JunkItem>> {
        var n = 0; var bytes = 0L
        val media = items.filter { it.file == null && it.uri != null }
        val direct = items.filter { it.file != null }
        val (dirs, files) = direct.partition { it.file!!.isDirectory }
        for (i in files) if (i.file!!.delete()) { n++; bytes += i.size }
        for (d in dirs.sortedByDescending { it.file!!.absolutePath.length }) if (d.file!!.listFiles().isNullOrEmpty() && d.file.delete()) n++
        lastResult = null
        return Triple(n, bytes, media)
    }

    fun mediaDeleteRequest(ctx: Context, items: List<JunkItem>): IntentSender? =
        if (Build.VERSION.SDK_INT >= 30 && items.isNotEmpty())
            MediaStore.createDeleteRequest(ctx.contentResolver, items.mapNotNull { it.uri }).intentSender
        else null
}
