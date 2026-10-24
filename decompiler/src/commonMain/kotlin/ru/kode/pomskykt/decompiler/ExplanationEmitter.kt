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
import ru.kode.pomskykt.syntax.exprs.BoundaryKind

/**
 * Emits human-readable explanations from [Regex] IR nodes, using the phrases of [strings].
 */
internal class ExplanationEmitter(private val strings: ExplanationStrings = EnglishStrings) {
    private val buf = StringBuilder()

    fun explain(regex: Regex): String {
        emitNode(regex, Context.TOP)
        return buf.toString().trim()
    }

    private fun emitNode(regex: Regex, ctx: Context) {
        when (regex) {
            is Regex.Literal -> emitLiteral(regex.content)
            is Regex.Unescaped -> emitUnescaped(regex.content)
            is Regex.CharSet -> emitCharSet(regex.set)
            is Regex.CompoundCharSet -> emitCompoundCharSet(regex.set)
            is Regex.Grapheme -> buf.append(strings.grapheme)
            is Regex.Dot -> buf.append(strings.anyCharacter)
            is Regex.Group -> emitGroup(regex.group, ctx)
            is Regex.Sequence -> emitSequence(regex.parts, ctx)
            is Regex.Alt -> emitAlternation(regex.alternation, ctx)
            is Regex.Rep -> emitRepetition(regex.repetition, ctx)
            is Regex.Bound -> emitBoundary(regex.kind)
            is Regex.Look -> emitLookaround(regex.lookaround, ctx)
            is Regex.Ref -> emitReference(regex.reference)
            is Regex.Recursion -> buf.append(strings.recursion)
            is Regex.ModeGroup -> emitNode(regex.inner, ctx)
        }
    }

    private fun emitLiteral(content: String) {
        if (content.isEmpty()) return
        buf.append('"')
        buf.append(content)
        buf.append('"')
    }

    private fun emitUnescaped(content: String) {
        buf.append(strings.unescaped)
        buf.append(content)
    }

    private fun emitCharSet(set: RegexCharSet) {
        if (set.items.size == 1) {
            emitSingleCharSetItem(set.items[0], set.negative)
            return
        }
        if (set.negative) {
            buf.append(strings.charNotMatching)
        } else {
            buf.append(strings.charMatching)
        }
        set.items.forEachIndexed { i, item ->
            if (i > 0) buf.append(", ")
            emitCharSetItemBrief(item)
        }
    }

    private fun emitSingleCharSetItem(item: RegexCharSetItem, negative: Boolean) {
        when (item) {
            is RegexCharSetItem.Shorthand -> {
                if (negative) {
                    buf.append(strings.negatedShorthandDescription(item.shorthand))
                } else {
                    buf.append(strings.shorthandDescription(item.shorthand))
                }
            }
            is RegexCharSetItem.Range -> {
                buf.append(if (negative) strings.negatedArticle else strings.article)
                buf.append(strings.rangeDescription(item.first, item.last))
            }
            is RegexCharSetItem.Char -> {
                if (negative) buf.append(strings.not)
                buf.append("'")
                buf.append(item.char)
                buf.append("'")
            }
            is RegexCharSetItem.Property -> {
                buf.append(if (item.negative xor negative) strings.negatedArticle else strings.article)
                emitPropertyDescription(item.property)
            }
            is RegexCharSetItem.Literal -> {
                if (negative) buf.append(strings.not)
                buf.append("'")
                buf.append(item.content)
                buf.append("'")
            }
            is RegexCharSetItem.CodePoint -> {
                if (negative) buf.append(strings.not)
                buf.append("U+")
                buf.append(item.codePoint.toString(HEX_RADIX).uppercase())
            }
        }
    }

    private fun emitCharSetItemBrief(item: RegexCharSetItem) {
        when (item) {
            is RegexCharSetItem.Shorthand -> buf.append(strings.shorthandBrief(item.shorthand))
            is RegexCharSetItem.Range -> buf.append(strings.rangeDescription(item.first, item.last))
            is RegexCharSetItem.Char -> {
                buf.append("'")
                buf.append(item.char)
                buf.append("'")
            }
            is RegexCharSetItem.Property -> emitPropertyDescription(item.property)
            is RegexCharSetItem.Literal -> {
                buf.append("'")
                buf.append(item.content)
                buf.append("'")
            }
            is RegexCharSetItem.CodePoint -> {
                buf.append("U+")
                buf.append(item.codePoint.toString(HEX_RADIX).uppercase())
            }
        }
    }

    /**
     * Describe a character set item for use in repetition context (e.g., "digits" plural).
     * Returns null if the set is not a simple single-item set.
     */
    private fun charSetPluralNoun(set: RegexCharSet): String? {
        if (set.items.size != 1 || set.negative) return null
        return when (val item = set.items[0]) {
            is RegexCharSetItem.Shorthand -> strings.shorthandPlural(item.shorthand)
            is RegexCharSetItem.Range -> strings.rangePlural(item.first, item.last)
            else -> null
        }
    }

    private fun emitPropertyDescription(property: RegexProperty) {
        when (property) {
            is RegexProperty.CategoryProp -> {
                buf.append(strings.unicodeCategory)
                buf.append(property.category.name)
            }
            is RegexProperty.ScriptProp -> {
                buf.append(strings.unicodeScript)
                buf.append(property.script.fullName)
            }
            is RegexProperty.BlockProp -> {
                buf.append(strings.unicodeBlock)
                buf.append(property.block.fullName)
            }
            is RegexProperty.OtherProp -> {
                buf.append(strings.unicodeProperty)
                buf.append(property.property.fullName)
            }
        }
    }

    private fun emitCompoundCharSet(set: RegexCompoundCharSet) {
        if (set.negative) buf.append(strings.not)
        buf.append(strings.intersectionOf)
        set.sets.forEachIndexed { i, charSet ->
            if (i > 0) buf.append(strings.and)
            emitCharSet(charSet)
        }
    }

    private fun emitGroup(group: RegexGroup, ctx: Context) {
        val kind = group.kind
        when (kind) {
            is RegexGroupKind.NonCapturing -> {
                val merged = mergeLiterals(group.parts)
                val inner = if (merged.size == 1) merged[0] else Regex.Sequence(merged)
                emitNode(inner, ctx)
            }
            is RegexGroupKind.Numbered -> {
                emitGroupContents(group.parts)
                buf.append(strings.savedAsGroup(kind.index))
            }
            is RegexGroupKind.Named -> {
                emitGroupContents(group.parts)
                buf.append(strings.savedAsName(kind.name))
            }
            is RegexGroupKind.Atomic -> {
                emitGroupContents(group.parts)
            }
        }
    }

    private fun emitGroupContents(parts: List<Regex>) {
        val inner = if (parts.size == 1) parts[0] else Regex.Sequence(parts)
        emitNode(inner, Context.GROUP)
    }

    private fun emitSequence(parts: List<Regex>, ctx: Context) {
        val merged = mergeLiterals(parts)
        merged.forEachIndexed { i, part ->
            if (i > 0) buf.append(strings.followedBy)
            emitNode(part, Context.SEQUENCE)
        }
    }

    /**
     * Merge consecutive single-char [Regex.Literal] nodes into one literal.
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
        buf.append(strings.either)
        alt.alternatives.forEachIndexed { i, regex ->
            when {
                i == alt.alternatives.size - 1 && alt.alternatives.size > 1 -> buf.append(strings.orLast)
                i > 0 -> buf.append(", ")
            }
            emitNode(regex, Context.ALTERNATION)
        }
    }

    private fun emitRepetition(rep: RegexRepetition, ctx: Context) {
        val inner = rep.inner

        // For exact repetitions of simple char sets, use natural phrasing like "exactly 4 digits (0-9)"
        if (rep.lower == rep.upper && inner is Regex.CharSet) {
            val plural = charSetPluralNoun(inner.set)
            if (plural != null) {
                buf.append(strings.exactlyPlural(rep.lower, plural))
                if (!rep.greedy) buf.append(strings.lazy)
                return
            }
        }

        // Try to use natural plural phrasing for simple char sets
        val plural = if (inner is Regex.CharSet) charSetPluralNoun(inner.set) else null

        when {
            rep.lower == 0 && rep.upper == 1 -> {
                buf.append(strings.optionally)
                emitNode(inner, Context.REPETITION)
            }
            rep.lower == 0 && rep.upper == null -> {
                if (plural != null) {
                    buf.append(strings.anyNumberPlural(plural))
                } else {
                    buf.append(strings.anyNumberOf)
                    emitNode(inner, Context.REPETITION)
                }
            }
            rep.lower == 1 && rep.upper == null -> {
                if (plural != null) {
                    buf.append(strings.oneOrMorePlural(plural))
                } else {
                    buf.append(strings.oneOrMoreOf)
                    emitNode(inner, Context.REPETITION)
                }
            }
            rep.lower == rep.upper -> {
                buf.append(strings.exactlyOf(rep.lower))
                emitNode(inner, Context.REPETITION)
            }
            rep.upper == null -> {
                if (plural != null) {
                    buf.append(strings.atLeastPlural(rep.lower, plural))
                } else {
                    buf.append(strings.atLeastOf(rep.lower))
                    emitNode(inner, Context.REPETITION)
                }
            }
            else -> {
                buf.append(strings.betweenOf(rep.lower, checkNotNull(rep.upper)))
                emitNode(inner, Context.REPETITION)
            }
        }

        if (!rep.greedy) buf.append(strings.lazy)
    }

    private fun emitBoundary(kind: BoundaryKind) {
        buf.append(strings.boundary(kind))
    }

    private fun emitLookaround(look: RegexLookaround, ctx: Context) {
        buf.append(strings.lookaround(look.kind))
        emitNode(look.inner, Context.GROUP)
    }

    private fun emitReference(ref: RegexReference) {
        when (ref) {
            is RegexReference.Named -> {
                buf.append(strings.reference(ref.name))
            }
            is RegexReference.Numbered -> {
                buf.append(strings.reference(ref.index))
            }
        }
    }

    private enum class Context {
        TOP, SEQUENCE, ALTERNATION, GROUP, REPETITION,
    }

    companion object {
        private const val HEX_RADIX = 16
    }
}
