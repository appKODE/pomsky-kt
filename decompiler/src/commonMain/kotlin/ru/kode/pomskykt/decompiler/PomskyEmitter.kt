package ru.kode.pomskykt.decompiler

import ru.kode.pomskykt.regex.Regex
import ru.kode.pomskykt.regex.RegexAlternation
import ru.kode.pomskykt.regex.RegexCharSet
import ru.kode.pomskykt.regex.RegexCharSetItem
import ru.kode.pomskykt.regex.RegexCompoundCharSet
import ru.kode.pomskykt.regex.RegexGroup
import ru.kode.pomskykt.regex.RegexGroupKind
import ru.kode.pomskykt.regex.RegexLookaround
import ru.kode.pomskykt.regex.RegexProperty
import ru.kode.pomskykt.regex.RegexReference
import ru.kode.pomskykt.regex.RegexRepetition
import ru.kode.pomskykt.regex.RegexShorthand
import ru.kode.pomskykt.syntax.exprs.BoundaryKind
import ru.kode.pomskykt.syntax.exprs.LookaroundKind

/**
 * Emits Pomsky DSL source from [Regex] IR nodes.
 */
internal class PomskyEmitter(
    private val groupNames: Map<Int, String> = emptyMap(),
    private val renameUnnamedGroups: Boolean = false,
) {
    private val buf = StringBuilder()

    fun emit(regex: Regex): String {
        emitNode(regex, Context.TOP)
        return buf.toString().trim()
    }

    private fun emitNode(regex: Regex, ctx: Context) {
        when (regex) {
            is Regex.Literal -> emitLiteral(regex.content)
            is Regex.Unescaped -> emitUnescaped(regex.content)
            is Regex.CharSet -> emitCharSet(regex.set)
            is Regex.CompoundCharSet -> emitCompoundCharSet(regex.set)
            is Regex.Grapheme -> buf.append("Grapheme")
            is Regex.Dot -> buf.append('.')
            is Regex.Group -> emitGroup(regex.group, ctx)
            is Regex.Sequence -> emitSequence(regex.parts, ctx)
            is Regex.Alt -> emitAlternation(regex.alternation, ctx)
            is Regex.Rep -> emitRepetition(regex.repetition, ctx)
            is Regex.Bound -> emitBoundary(regex.kind)
            is Regex.Look -> emitLookaround(regex.lookaround, ctx)
            is Regex.Ref -> emitReference(regex.reference)
            is Regex.Recursion -> buf.append("recursion")
            is Regex.ModeGroup -> emitModeGroup(regex, ctx)
        }
    }

    private fun emitLiteral(content: String) {
        if (content.isEmpty()) return
        // Split on control chars that can't be in Pomsky string literals
        val parts = mutableListOf<Any>() // String for literal parts, Int for code points
        val currentLiteral = StringBuilder()
        for (c in content) {
            when (c) {
                '\n', '\r', '\t', '\u000C' -> {
                    if (currentLiteral.isNotEmpty()) {
                        parts.add(currentLiteral.toString())
                        currentLiteral.clear()
                    }
                    parts.add(c.code)
                }
                else -> currentLiteral.append(c)
            }
        }
        if (currentLiteral.isNotEmpty()) parts.add(currentLiteral.toString())

        parts.forEachIndexed { i, part ->
            if (i > 0) buf.append(' ')
            when (part) {
                is String -> {
                    buf.append('\'')
                    for (c in part) {
                        when (c) {
                            '\'' -> buf.append("\\'")
                            '\\' -> buf.append("\\")
                            else -> buf.append(c)
                        }
                    }
                    buf.append('\'')
                }
                is Int -> {
                    buf.append("U+")
                    buf.append(part.toString(HEX_RADIX).uppercase().padStart(2, '0'))
                }
            }
        }
    }

    private fun emitUnescaped(content: String) {
        buf.append("regex '")
        buf.append(content.replace("'", "\\'"))
        buf.append('\'')
    }

    private fun emitCharSet(set: RegexCharSet) {
        // Single-item optimizations
        if (set.items.size == 1 && !set.negative) {
            val item = set.items[0]
            when (item) {
                is RegexCharSetItem.Shorthand -> {
                    buf.append(shorthandToPomsky(item.shorthand))
                    return
                }
                is RegexCharSetItem.Property -> {
                    emitPropertyItem(item)
                    return
                }
                is RegexCharSetItem.Char -> {
                    emitLiteral(item.char.toString())
                    return
                }
                is RegexCharSetItem.Range -> {
                    if (item.first == item.last) {
                        emitLiteral(item.first.toString())
                        return
                    }
                }
                is RegexCharSetItem.CodePoint, is RegexCharSetItem.Literal -> {}
            }
        }

        // Negated single shorthand
        if (set.items.size == 1 && set.negative) {
            val item = set.items[0]
            if (item is RegexCharSetItem.Shorthand) {
                val negName = negatedShorthandToPomsky(item.shorthand)
                if (negName != null) {
                    buf.append(negName)
                    return
                }
            }
        }

        // [\s\S] or [\S\s] or [\w\W] etc. — shorthand + its negation = Codepoint (any character)
        if (set.items.size == 2 && !set.negative) {
            val a = set.items[0]
            val b = set.items[1]
            if (a is RegexCharSetItem.Shorthand && b is RegexCharSetItem.Shorthand &&
                isComplementaryShorthands(a.shorthand, b.shorthand)
            ) {
                buf.append("Codepoint")
                return
            }
        }

        if (set.negative) buf.append('!')
        buf.append('[')
        var first = true
        for (item in set.items) {
            if (!first) buf.append(' ')
            emitCharSetItem(item)
            first = false
        }
        buf.append(']')
    }

    private fun emitCharSetItem(item: RegexCharSetItem) {
        when (item) {
            is RegexCharSetItem.Char -> {
                when (item.char) {
                    '\n', '\r', '\t', '\u000C' -> {
                        buf.append("U+")
                        buf.append(item.char.code.toString(HEX_RADIX).uppercase().padStart(2, '0'))
                    }
                    else -> {
                        buf.append('\'')
                        when (item.char) {
                            '\'' -> buf.append("\\'")
                            '\\' -> buf.append("\\\\")
                            else -> buf.append(item.char)
                        }
                        buf.append('\'')
                    }
                }
            }
            is RegexCharSetItem.Range -> {
                val firstIsControl = item.first in CONTROL_CHARS
                val lastIsControl = item.last in CONTROL_CHARS
                if (firstIsControl) {
                    buf.append("U+")
                    buf.append(item.first.code.toString(HEX_RADIX).uppercase().padStart(2, '0'))
                } else {
                    buf.append('\'')
                    buf.append(item.first)
                    buf.append('\'')
                }
                buf.append('-')
                if (lastIsControl) {
                    buf.append("U+")
                    buf.append(item.last.code.toString(HEX_RADIX).uppercase().padStart(2, '0'))
                } else {
                    buf.append('\'')
                    buf.append(item.last)
                    buf.append('\'')
                }
            }
            is RegexCharSetItem.Shorthand -> buf.append(shorthandToBareName(item.shorthand))
            is RegexCharSetItem.Property -> {
                if (item.negative) buf.append('!')
                emitPropertyName(item.property)
            }
            is RegexCharSetItem.Literal -> {
                buf.append('\'')
                buf.append(item.content.replace("'", "\\'"))
                buf.append('\'')
            }
            is RegexCharSetItem.CodePoint -> {
                buf.append("U+")
                buf.append(item.codePoint.toString(HEX_RADIX).uppercase())
            }
        }
    }

    private fun emitPropertyItem(item: RegexCharSetItem.Property) {
        if (item.negative) buf.append('!')
        buf.append('[')
        emitPropertyName(item.property)
        buf.append(']')
    }

    private fun emitPropertyName(property: RegexProperty) {
        when (property) {
            is RegexProperty.CategoryProp -> buf.append(property.category.abbreviation)
            is RegexProperty.ScriptProp -> {
                when (property.extension) {
                    ru.kode.pomskykt.syntax.exprs.ScriptExtension.Yes -> buf.append("scx:")
                    ru.kode.pomskykt.syntax.exprs.ScriptExtension.No -> buf.append("sc:")
                    ru.kode.pomskykt.syntax.exprs.ScriptExtension.Unspecified -> {}
                }
                buf.append(property.script.fullName)
            }
            is RegexProperty.BlockProp -> {
                buf.append("blk:")
                buf.append(property.block.fullName)
            }
            is RegexProperty.OtherProp -> buf.append(property.property.fullName)
        }
    }

    private fun emitCompoundCharSet(set: RegexCompoundCharSet) {
        // Pomsky intersection uses & as a top-level operator between char sets: [a] & [b]
        if (set.negative) buf.append("!(")
        set.sets.forEachIndexed { i, charSet ->
            if (i > 0) buf.append(" & ")
            emitCharSet(charSet)
        }
        if (set.negative) buf.append(')')
    }

    private fun emitGroup(group: RegexGroup, ctx: Context) {
        val kind = group.kind
        when (kind) {
            is RegexGroupKind.NonCapturing -> {
                val merged = mergeLiterals(group.parts)
                val inner = if (merged.size == 1) merged[0] else Regex.Sequence(merged)
                // Preserve grouping when content has alternation (prevents scope loss)
                val needsParens = inner is Regex.Alt || ctx == Context.REPETITION ||
                    (merged.size > 1 && merged.any { it is Regex.Alt })
                if (needsParens) {
                    buf.append('(')
                    emitNode(inner, Context.GROUP)
                    buf.append(')')
                } else if (merged.size == 1) {
                    emitNode(merged[0], Context.GROUP)
                } else {
                    merged.forEachIndexed { i, part ->
                        if (i > 0) buf.append(' ')
                        emitNode(part, Context.GROUP)
                    }
                }
            }
            is RegexGroupKind.Numbered -> {
                if (renameUnnamedGroups) {
                    buf.append(":_g")
                    buf.append(kind.index)
                    buf.append('(')
                } else {
                    buf.append(":(")
                }
                emitGroupContents(group.parts)
                buf.append(')')
            }
            is RegexGroupKind.Named -> {
                buf.append(':')
                buf.append(kind.name)
                buf.append('(')
                emitGroupContents(group.parts)
                buf.append(')')
            }
            is RegexGroupKind.Atomic -> {
                buf.append("atomic(")
                emitGroupContents(group.parts)
                buf.append(')')
            }
        }
    }

    private fun emitGroupContents(parts: List<Regex>) {
        val merged = mergeLiterals(parts)
        merged.forEachIndexed { i, part ->
            if (i > 0) buf.append(' ')
            emitNode(part, Context.GROUP)
        }
    }

    private fun emitSequence(parts: List<Regex>, ctx: Context) {
        val merged = mergeLiterals(parts)
        merged.forEachIndexed { i, part ->
            if (i > 0) buf.append(' ')
            emitNode(part, Context.SEQUENCE)
        }
    }

    /**
     * Merge consecutive [Regex.Literal] nodes into single literals.
     * E.g., Literal("h"), Literal("e"), Literal("l") -> Literal("hel")
     */
    private fun mergeLiterals(parts: List<Regex>): List<Regex> {
        val result = mutableListOf<Regex>()
        val literalBuf = StringBuilder()
        for (part in parts) {
            if (part is Regex.Literal && part.content.length <= 1) {
                literalBuf.append(part.content)
            } else {
                if (literalBuf.isNotEmpty()) {
                    result.add(Regex.Literal(literalBuf.toString()))
                    literalBuf.clear()
                }
                result.add(part)
            }
        }
        if (literalBuf.isNotEmpty()) {
            result.add(Regex.Literal(literalBuf.toString()))
        }
        return result
    }

    private fun emitAlternation(alt: RegexAlternation, ctx: Context) {
        val needsParens = ctx == Context.SEQUENCE || ctx == Context.REPETITION
        if (needsParens) buf.append('(')
        alt.alternatives.forEachIndexed { i, regex ->
            if (i > 0) buf.append(" | ")
            emitNode(regex, Context.ALTERNATION)
        }
        if (needsParens) buf.append(')')
    }

    private fun emitRepetition(rep: RegexRepetition, ctx: Context) {
        val inner = rep.inner
        val needsParens = needsParensForRepetition(inner)
        if (needsParens) buf.append('(')
        emitNode(inner, Context.REPETITION)
        if (needsParens) buf.append(')')

        val suffix = when {
            rep.lower == 0 && rep.upper == 1 -> "?"
            rep.lower == 0 && rep.upper == null -> "*"
            rep.lower == 1 && rep.upper == null -> "+"
            rep.lower == rep.upper -> "{${rep.lower}}"
            rep.upper == null -> "{${rep.lower},}"
            rep.lower == 0 -> "{0,${rep.upper}}"
            else -> "{${rep.lower},${rep.upper}}"
        }
        buf.append(suffix)

        if (!rep.greedy) buf.append(" lazy")
    }

    private fun needsParensForRepetition(regex: Regex): Boolean = when (regex) {
        is Regex.Sequence -> true
        is Regex.Alt -> true
        is Regex.Rep -> true
        is Regex.Literal -> regex.content.length > 1
        is Regex.Group -> regex.group.kind is RegexGroupKind.NonCapturing && regex.group.parts.size > 1
        is Regex.ModeGroup -> true
        else -> false
    }

    private fun emitBoundary(kind: BoundaryKind) {
        buf.append(
            when (kind) {
                BoundaryKind.Start -> "Start"
                BoundaryKind.End -> "End"
                BoundaryKind.Word -> "%"
                BoundaryKind.NotWord -> "!%"
                BoundaryKind.WordStart -> "<%"
                BoundaryKind.WordEnd -> "%>"
            }
        )
    }

    private fun emitLookaround(look: RegexLookaround, ctx: Context) {
        val needsParens = ctx == Context.SEQUENCE || ctx == Context.REPETITION
        if (needsParens) buf.append('(')
        val prefix = when (look.kind) {
            LookaroundKind.Ahead -> ">> "
            LookaroundKind.Behind -> "<< "
            LookaroundKind.AheadNegative -> "!>> "
            LookaroundKind.BehindNegative -> "!<< "
        }
        buf.append(prefix)
        emitNode(look.inner, Context.GROUP)
        if (needsParens) buf.append(')')
    }

    private fun emitModeGroup(modeGroup: Regex.ModeGroup, ctx: Context) {
        val flags = modeGroup.flags
        val modifiers = listOfNotNull(
            flags.ignoreCase?.let { if (it) "enable ignore_case" else "disable ignore_case" },
            flags.multiline?.let { if (it) "enable multiline" else "disable multiline" },
            flags.singleLine?.let { if (it) "enable single_line" else "disable single_line" },
            flags.extended?.let { if (it) "enable extended" else "disable extended" },
            flags.reuseGroups?.let { if (it) "enable reuse_groups" else "disable reuse_groups" },
            flags.asciiLineBreaks?.let { if (it) "enable ascii_line_breaks" else "disable ascii_line_breaks" },
        )
        for (mod in modifiers) {
            buf.append('(')
            buf.append(mod)
            buf.append("; ")
        }
        emitNode(modeGroup.inner, Context.GROUP)
        repeat(modifiers.size) { buf.append(')') }
    }

    private fun emitReference(ref: RegexReference) {
        when (ref) {
            is RegexReference.Named -> {
                buf.append("::")
                buf.append(ref.name)
            }
            is RegexReference.Numbered -> {
                buf.append("::")
                // Prefer named reference when available (required for .NET mixed groups)
                val name = groupNames[ref.index]
                when {
                    name != null -> buf.append(name)
                    renameUnnamedGroups -> { buf.append("_g"); buf.append(ref.index) }
                    else -> buf.append(ref.index)
                }
            }
        }
    }

    private fun shorthandToPomsky(sh: RegexShorthand): String = when (sh) {
        RegexShorthand.Word -> "[word]"
        RegexShorthand.Digit -> "[digit]"
        RegexShorthand.Space -> "[space]"
        RegexShorthand.NotWord -> "![word]"
        RegexShorthand.NotDigit -> "![digit]"
        RegexShorthand.NotSpace -> "![space]"
        RegexShorthand.VertSpace -> "[vert_space]"
        RegexShorthand.HorizSpace -> "[horiz_space]"
    }

    private fun negatedShorthandToPomsky(sh: RegexShorthand): String? = when (sh) {
        RegexShorthand.Word -> "![word]"
        RegexShorthand.Digit -> "![digit]"
        RegexShorthand.Space -> "![space]"
        RegexShorthand.NotWord -> "[word]"
        RegexShorthand.NotDigit -> "[digit]"
        RegexShorthand.NotSpace -> "[space]"
        else -> null
    }

    /**
     * Bare identifier form for use inside character classes: `[word digit !space]`.
     * Unlike [shorthandToPomsky], this does NOT wrap in brackets.
     */
    private fun shorthandToBareName(sh: RegexShorthand): String = when (sh) {
        RegexShorthand.Word -> "word"
        RegexShorthand.Digit -> "digit"
        RegexShorthand.Space -> "space"
        RegexShorthand.NotWord -> "!word"
        RegexShorthand.NotDigit -> "!digit"
        RegexShorthand.NotSpace -> "!space"
        RegexShorthand.VertSpace -> "vert_space"
        RegexShorthand.HorizSpace -> "horiz_space"
    }

    private fun isComplementaryShorthands(a: RegexShorthand, b: RegexShorthand): Boolean {
        val pair = setOf(a, b)
        return pair == setOf(RegexShorthand.Space, RegexShorthand.NotSpace) ||
            pair == setOf(RegexShorthand.Word, RegexShorthand.NotWord) ||
            pair == setOf(RegexShorthand.Digit, RegexShorthand.NotDigit)
    }

    private enum class Context {
        TOP, SEQUENCE, ALTERNATION, GROUP, REPETITION,
    }

    companion object {
        private const val HEX_RADIX = 16
        private val CONTROL_CHARS = charArrayOf('\n', '\r', '\t', '\u000C')
    }
}
