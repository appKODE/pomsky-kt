package ru.kode.pomskykt.syntax.unicode

import ru.kode.pomskykt.syntax.exprs.GroupName
import ru.kode.pomskykt.syntax.exprs.ScriptExtension

/**
 * Unicode data lookup for resolving character class names.
 *
 * Provides [parseGroupName] which resolves a name (with optional kind prefix)
 * to a [GroupName], matching the Rust `parse_group_name` function.
 */
object UnicodeData {

    /**
     * Resolves a character class name to a [GroupName].
     *
     * @param kind Optional prefix like "gc", "sc", "scx", "blk", "block"
     * @param name The character class name (e.g., "Latin", "Letter", "w")
     * @return The resolved [GroupName], or null if not found.
     */
    /** Check if the name (without a prefix) is a valid class name. */
    fun isValidNameWithoutPrefix(name: String): Boolean = LOOKUP[name] != null

    /** Check if the name looks like a block name (starts with "In" and the rest is in the LOOKUP). */
    fun isBlockLikeName(name: String): Boolean {
        if (!name.startsWith("In")) return false
        return LOOKUP[name] is GroupName.CodeBlockName
    }

    fun parseGroupName(kind: String?, name: String): GroupName? {
        val lookupName = if (kind == "blk" || kind == "block") "In$name" else name

        val base = LOOKUP[lookupName] ?: return null

        // Validate kind prefix matches the resolved type
        if (kind != null) {
            when (base) {
                is GroupName.Word, is GroupName.Digit, is GroupName.Space,
                is GroupName.HorizSpace, is GroupName.VertSpace,
                is GroupName.OtherPropertyName -> return null // these don't accept prefixes

                is GroupName.CategoryName -> {
                    if (kind != "gc" && kind != "general_category") return null
                }

                is GroupName.ScriptName -> {
                    val ext = when (kind) {
                        "sc", "script" -> ScriptExtension.No
                        "scx", "script_extensions" -> ScriptExtension.Yes
                        else -> return null
                    }
                    return GroupName.ScriptName(base.script, ext)
                }

                is GroupName.CodeBlockName -> {
                    if (kind != "blk" && kind != "block") return null
                }
            }
        }

        return base
    }

    private val LOOKUP: Map<String, GroupName> by lazy {
        buildMap {
            // Categories (all aliases)
            val seen = mutableSetOf<String>()
            for (cat in Category.entries) {
                val name = GroupName.CategoryName(cat)
                Category.aliases(cat).forEach { alias ->
                    if (seen.add(alias)) put(alias, name)
                }
            }

            // Scripts (all aliases)
            for (script in Script.entries) {
                val name = GroupName.ScriptName(script, ScriptExtension.Unspecified)
                Script.aliases(script).forEach { alias ->
                    if (seen.add(alias)) put(alias, name)
                }
            }

            // Code blocks (prefixed with "In")
            for (block in CodeBlock.entries) {
                val key = "In${block.fullName}"
                if (seen.add(key)) put(key, GroupName.CodeBlockName(block))
            }
            // Short/alias names for blocks (prefixed with "In")
            for ((alias, block) in CodeBlock.shortNames) {
                val key = "In$alias"
                if (seen.add(key)) put(key, GroupName.CodeBlockName(block))
            }

            // Boolean properties (all aliases)
            for (prop in OtherProperties.entries) {
                val name = GroupName.OtherPropertyName(prop)
                OtherProperties.aliases(prop).forEach { alias ->
                    if (seen.add(alias)) put(alias, name)
                }
            }

            // Shorthands must be last so they override any conflicting Unicode property aliases
            // (e.g. "digit" = Nd category, "space" = White_Space property in Unicode)
            put("w", GroupName.Word)
            put("d", GroupName.Digit)
            put("s", GroupName.Space)
            put("h", GroupName.HorizSpace)
            put("v", GroupName.VertSpace)
            put("word", GroupName.Word)
            put("digit", GroupName.Digit)
            put("space", GroupName.Space)
            put("horiz_space", GroupName.HorizSpace)
            put("vert_space", GroupName.VertSpace)
        }
    }
}
