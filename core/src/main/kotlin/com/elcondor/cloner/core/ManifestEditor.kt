package com.elcondor.cloner.core

import com.elcondor.cloner.core.axml.AxmlDocument
import com.elcondor.cloner.core.axml.StartElement
import com.elcondor.cloner.core.axml.ValueType

/** Resource ids of the framework attributes we touch (android.R.attr). */
internal object Attr {
    const val LABEL = 0x01010001
    const val NAME = 0x01010003
    const val MANAGE_SPACE_ACTIVITY = 0x01010004
    const val PERMISSION = 0x01010006
    const val READ_PERMISSION = 0x01010007
    const val WRITE_PERMISSION = 0x01010008
    const val PERMISSION_GROUP = 0x0101000a
    const val SHARED_USER_ID = 0x0101000b
    const val PROCESS = 0x01010011
    const val TASK_AFFINITY = 0x01010012
    const val AUTHORITIES = 0x01010018
    const val TARGET_ACTIVITY = 0x01010202
    const val MIN_SDK_VERSION = 0x0101020c
    const val SHARED_USER_LABEL = 0x01010261
    const val BACKUP_AGENT = 0x0101027f
    const val APP_COMPONENT_FACTORY = 0x0101057a
    const val SHARED_USER_MAX_SDK_VERSION = 0x0101064d
}

data class CloneOptions(
    /** Package name of the clone; must differ from the original. */
    val newPackage: String,
    /** Name shown under the launcher icon, or null to keep the original one. */
    val newLabel: String? = null,
    /** `uses-permission` entries to drop from the clone. */
    val removePermissions: Set<String> = emptySet(),
)

data class ManifestInfo(
    val packageName: String,
    val split: String?,
    val minSdk: Int,
    val usesPermissions: List<String>,
)

/**
 * Rewrites a compiled AndroidManifest.xml so that the app can be installed as an independent copy:
 * the package name changes, relative class names are made absolute, and every name that must be
 * unique on the device (content provider authorities, declared permissions) is renamed.
 */
object ManifestEditor {

    private val COMPONENT_TAGS = setOf("activity", "activity-alias", "service", "receiver", "provider")
    private val PERMISSION_DECL_TAGS = setOf("permission", "permission-group", "permission-tree")
    private val USES_PERMISSION_TAGS = setOf("uses-permission", "uses-permission-sdk-23", "uses-permission-sdk-m")
    private val PERMISSION_REF_ATTRS = setOf(Attr.PERMISSION, Attr.READ_PERMISSION, Attr.WRITE_PERMISSION, Attr.PERMISSION_GROUP)

    fun readInfo(manifest: ByteArray): ManifestInfo {
        val doc = AxmlDocument.parse(manifest)
        val root = doc.rootElement() ?: throw IllegalArgumentException("Manifest has no root element")
        val pkg = doc.findPlainAttr(root, "package")?.let(doc::stringValue)
            ?: throw IllegalArgumentException("Manifest has no package name")
        val split = doc.findPlainAttr(root, "split")?.let(doc::stringValue)
        var minSdk = 1
        val perms = mutableListOf<String>()
        doc.walk { e, _ ->
            when (doc.elementName(e)) {
                "uses-sdk" -> doc.findAttr(e, Attr.MIN_SDK_VERSION)?.let { a -> doc.intValue(a)?.let { minSdk = it } }
                in USES_PERMISSION_TAGS -> doc.findAttr(e, Attr.NAME)?.let(doc::stringValue)?.let(perms::add)
            }
        }
        return ManifestInfo(pkg, split, minSdk, perms)
    }

    fun rewrite(manifest: ByteArray, options: CloneOptions): ByteArray {
        val doc = AxmlDocument.parse(manifest)
        val root = doc.rootElement() ?: throw IllegalArgumentException("Manifest has no root element")
        val pkgAttr = doc.findPlainAttr(root, "package") ?: throw IllegalArgumentException("Manifest has no package name")
        val oldPkg = doc.stringValue(pkgAttr) ?: throw IllegalArgumentException("Manifest has no package name")
        val newPkg = options.newPackage
        require(PackageNames.isValid(newPkg)) { "Nombre de paquete no válido: $newPkg" }
        require(newPkg != oldPkg) { "El paquete del clon debe ser distinto al original" }

        fun renameUnique(value: String): String =
            if (value.contains(oldPkg)) value.replace(oldPkg, newPkg) else "$newPkg.$value"

        fun absoluteClass(value: String): String = when {
            value.startsWith(".") -> oldPkg + value
            !value.contains('.') -> "$oldPkg.$value"
            else -> value
        }

        doc.setString(pkgAttr, newPkg)

        // An app with a shared user id can't be installed with a different signature: drop it.
        listOf(Attr.SHARED_USER_ID, Attr.SHARED_USER_LABEL, Attr.SHARED_USER_MAX_SDK_VERSION).forEach { id ->
            doc.findAttr(root, id)?.let { doc.removeAttr(root, it) }
        }

        // 1st pass: declared permissions get new names; remember the mapping for references.
        val permissionMap = HashMap<String, String>()
        doc.walk { e, _ ->
            if (doc.elementName(e) in PERMISSION_DECL_TAGS) {
                val a = doc.findAttr(e, Attr.NAME) ?: return@walk
                val old = doc.stringValue(a) ?: return@walk
                val new = renameUnique(old)
                permissionMap[old] = new
                doc.setString(a, new)
            }
        }

        val toRemove = mutableListOf<StartElement>()
        val launcherComponents = mutableSetOf<StartElement>()
        doc.walk { e, ancestors ->
            val tag = doc.elementName(e)
            when (tag) {
                "application" -> {
                    listOf(Attr.NAME, Attr.BACKUP_AGENT, Attr.MANAGE_SPACE_ACTIVITY, Attr.APP_COMPONENT_FACTORY).forEach { id ->
                        doc.findAttr(e, id)?.let { a -> doc.stringValue(a)?.let { doc.setString(a, absoluteClass(it)) } }
                    }
                    if (options.newLabel != null) {
                        val label = doc.findAttr(e, Attr.LABEL)
                        if (label != null) doc.setString(label, options.newLabel)
                        else doc.addAndroidStringAttr(e, "label", Attr.LABEL, options.newLabel)
                    }
                }
                in COMPONENT_TAGS -> {
                    doc.findAttr(e, Attr.NAME)?.let { a -> doc.stringValue(a)?.let { doc.setString(a, absoluteClass(it)) } }
                    if (tag == "activity-alias") {
                        doc.findAttr(e, Attr.TARGET_ACTIVITY)?.let { a -> doc.stringValue(a)?.let { doc.setString(a, absoluteClass(it)) } }
                    }
                    if (tag == "provider") {
                        doc.findAttr(e, Attr.AUTHORITIES)?.let { a ->
                            doc.stringValue(a)?.let { v ->
                                doc.setString(a, v.split(';').joinToString(";") { renameUnique(it.trim()) })
                            }
                        }
                    }
                }
                "category" -> {
                    val name = doc.findAttr(e, Attr.NAME)?.let(doc::stringValue)
                    if (name == "android.intent.category.LAUNCHER") {
                        ancestors.lastOrNull { doc.elementName(it) == "activity" || doc.elementName(it) == "activity-alias" }
                            ?.let(launcherComponents::add)
                    }
                }
                in USES_PERMISSION_TAGS -> {
                    val a = doc.findAttr(e, Attr.NAME)
                    val name = a?.let(doc::stringValue)
                    if (name != null && name in options.removePermissions) toRemove.add(e)
                }
            }

            // References to renamed permissions, wherever they appear.
            for (attr in e.attributes.toList()) {
                val id = doc.resId(attr)
                if (id == Attr.NAME && tag !in USES_PERMISSION_TAGS) continue
                if (id != Attr.NAME && id !in PERMISSION_REF_ATTRS) continue
                val v = doc.stringValue(attr) ?: continue
                permissionMap[v]?.let { doc.setString(attr, it) }
            }

            // Process names and task affinities derived from the package name.
            listOf(Attr.PROCESS, Attr.TASK_AFFINITY).forEach { id ->
                doc.findAttr(e, id)?.let { a ->
                    val v = doc.stringValue(a)
                    if (v != null && a.dataType == ValueType.STRING && v.startsWith(oldPkg)) {
                        doc.setString(a, newPkg + v.removePrefix(oldPkg))
                    }
                }
            }
        }

        if (options.newLabel != null) {
            for (component in launcherComponents) {
                doc.findAttr(component, Attr.LABEL)?.let { doc.setString(it, options.newLabel) }
            }
        }
        toRemove.forEach(doc::removeElement)
        return doc.toByteArray()
    }
}

object PackageNames {
    private val SEGMENT = Regex("[A-Za-z][A-Za-z0-9_]*")

    fun isValid(name: String): Boolean {
        val parts = name.split('.')
        return name.length <= 255 && parts.size >= 2 && parts.all { SEGMENT.matches(it) }
    }
}
