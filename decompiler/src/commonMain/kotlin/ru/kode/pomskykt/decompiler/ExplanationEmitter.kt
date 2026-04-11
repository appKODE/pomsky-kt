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
 * Emits human-readable English explanations from [Regex] IR nodes.
 */
internal class ExplanationEmitter {
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
            is Regex.Grapheme -> buf.append("a single visible character (grapheme)")
            is Regex.Dot -> buf.append("any character")
            is Regex.Group -> emitGroup(regex.group, ctx)
            is Regex.Sequence -> emitSequence(regex.parts, ctx)
            is Regex.Alt -> emitAlternation(regex.alternation, ctx)
            is Regex.Rep -> emitRepetition(regex.repetition, ctx)
            is Regex.Bound -> emitBoundary(regex.kind)
            is Regex.Look -> emitLookaround(regex.lookaround, ctx)
            is Regex.Ref -> emitReference(regex.reference)
            is Regex.Recursion -> buf.append("the whole pattern repeated inside itself")
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
        buf.append("literal regex pattern: ")
        buf.append(content)
    }

    private fun emitCharSet(set: RegexCharSet) {
        if (set.items.size == 1) {
            emitSingleCharSetItem(set.items[0], set.negative)
            return
        }
        if (set.negative) {
            buf.append("a character not matching: ")
        } else {
            buf.append("a character matching: ")
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
                    buf.append(negatedShorthandDescription(item.shorthand))
                } else {
                    buf.append(shorthandDescription(item.shorthand))
                }
            }
            is RegexCharSetItem.Range -> {
                if (negative) buf.append("a non-")
                else buf.append("a ")
                buf.append(rangeDescription(item.first, item.last))
            }
            is RegexCharSetItem.Char -> {
                if (negative) buf.append("not ")
                buf.append("'")
                buf.append(item.char)
                buf.append("'")
            }
            is RegexCharSetItem.Property -> {
                if (item.negative xor negative) buf.append("a non-")
                else buf.append("a ")
                emitPropertyDescription(item.property)
            }
            is RegexCharSetItem.Literal -> {
                if (negative) buf.append("not ")
                buf.append("'")
                buf.append(item.content)
                buf.append("'")
            }
            is RegexCharSetItem.CodePoint -> {
                if (negative) buf.append("not ")
                buf.append("U+")
                buf.append(item.codePoint.toString(HEX_RADIX).uppercase())
            }
        }
    }

    private fun emitCharSetItemBrief(item: RegexCharSetItem) {
        when (item) {
            is RegexCharSetItem.Shorthand -> buf.append(shorthandBrief(item.shorthand))
            is RegexCharSetItem.Range -> buf.append(rangeDescription(item.first, item.last))
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
            is RegexCharSetItem.Shorthand -> shorthandPlural(item.shorthand)
            is RegexCharSetItem.Range -> rangePlural(item.first, item.last)
            else -> null
        }
    }

    private fun emitPropertyDescription(property: RegexProperty) {
        when (property) {
            is RegexProperty.CategoryProp -> {
                buf.append("Unicode ")
                buf.append(property.category.name)
            }
            is RegexProperty.ScriptProp -> {
                buf.append("Unicode script ")
                buf.append(property.script.fullName)
            }
            is RegexProperty.BlockProp -> {
                buf.append("Unicode block ")
                buf.append(property.block.fullName)
            }
            is RegexProperty.OtherProp -> {
                buf.append("Unicode property ")
                buf.append(property.property.fullName)
            }
        }
    }

    private fun emitCompoundCharSet(set: RegexCompoundCharSet) {
        if (set.negative) buf.append("not ")
        buf.append("intersection of ")
        set.sets.forEachIndexed { i, charSet ->
            if (i > 0) buf.append(" and ")
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
                buf.append(" (saved as group ${kind.index})")
            }
            is RegexGroupKind.Named -> {
                emitGroupContents(group.parts)
                buf.append(" (saved as \"${kind.name}\")")
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
            if (i > 0) buf.append(", followed by ")
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
        buf.append("either ")
        alt.alternatives.forEachIndexed { i, regex ->
            when {
                i == alt.alternatives.size - 1 && alt.alternatives.size > 1 -> buf.append(", or ")
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
                buf.append("exactly ${rep.lower} $plural")
                if (!rep.greedy) buf.append(" (lazy)")
                return
            }
        }

        // Try to use natural plural phrasing for simple char sets
        val plural = if (inner is Regex.CharSet) charSetPluralNoun(inner.set) else null

        when {
            rep.lower == 0 && rep.upper == 1 -> {
                buf.append("optionally ")
                emitNode(inner, Context.REPETITION)
            }
            rep.lower == 0 && rep.upper == null -> {
                if (plural != null) {
                    buf.append("any number of $plural")
                } else {
                    buf.append("any number of ")
                    emitNode(inner, Context.REPETITION)
                }
            }
            rep.lower == 1 && rep.upper == null -> {
                if (plural != null) {
                    buf.append("one or more $plural")
                } else {
                    buf.append("one or more of ")
                    emitNode(inner, Context.REPETITION)
                }
            }
            rep.lower == rep.upper -> {
                buf.append("exactly ${rep.lower} of ")
                emitNode(inner, Context.REPETITION)
            }
            rep.upper == null -> {
                if (plural != null) {
                    buf.append("${rep.lower} or more $plural")
                } else {
                    buf.append("${rep.lower} or more of ")
                    emitNode(inner, Context.REPETITION)
                }
            }
            else -> {
                buf.append("between ${rep.lower} and ${rep.upper} of ")
                emitNode(inner, Context.REPETITION)
            }
        }

        if (!rep.greedy) buf.append(" (lazy)")
    }

    private fun emitBoundary(kind: BoundaryKind) {
        buf.append(
            when (kind) {
                BoundaryKind.Start -> "start of string"
                BoundaryKind.End -> "end of string"
                BoundaryKind.Word -> "a word boundary (start or end of a word)"
                BoundaryKind.NotWord -> "a non-word boundary (middle of a word)"
                BoundaryKind.WordStart -> "start of word"
                BoundaryKind.WordEnd -> "end of word"
            }
        )
    }

    private fun emitLookaround(look: RegexLookaround, ctx: Context) {
        when (look.kind) {
            LookaroundKind.Ahead -> {
                buf.append("if followed by ")
                emitNode(look.inner, Context.GROUP)
            }
            LookaroundKind.AheadNegative -> {
                buf.append("if not followed by ")
                emitNode(look.inner, Context.GROUP)
            }
            LookaroundKind.Behind -> {
                buf.append("if preceded by ")
                emitNode(look.inner, Context.GROUP)
            }
            LookaroundKind.BehindNegative -> {
                buf.append("if not preceded by ")
                emitNode(look.inner, Context.GROUP)
            }
        }
    }

    private fun emitReference(ref: RegexReference) {
        when (ref) {
            is RegexReference.Named -> {
                buf.append("the same \"${ref.name}\" text again")
            }
            is RegexReference.Numbered -> {
                buf.append("the same group ${ref.index} text again")
            }
        }
    }

    private fun shorthandDescription(sh: RegexShorthand): String = when (sh) {
        RegexShorthand.Word -> "a word character (letter, digit, or underscore)"
        RegexShorthand.Digit -> "a digit (0-9)"
        RegexShorthand.Space -> "a whitespace character (space, tab, newline)"
        RegexShorthand.NotWord -> "a non-word character"
        RegexShorthand.NotDigit -> "a non-digit character"
        RegexShorthand.NotSpace -> "a non-whitespace character"
        RegexShorthand.VertSpace -> "a vertical whitespace character (newline, etc.)"
        RegexShorthand.HorizSpace -> "a horizontal whitespace character (space, tab)"
    }

    private fun negatedShorthandDescription(sh: RegexShorthand): String = when (sh) {
        RegexShorthand.Word -> "a non-word character"
        RegexShorthand.Digit -> "a non-digit character"
        RegexShorthand.Space -> "a non-whitespace character"
        RegexShorthand.NotWord -> "a word character"
        RegexShorthand.NotDigit -> "a digit (0-9)"
        RegexShorthand.NotSpace -> "a whitespace character"
        RegexShorthand.VertSpace -> "a non-vertical-whitespace character"
        RegexShorthand.HorizSpace -> "a non-horizontal-whitespace character"
    }

    private fun shorthandBrief(sh: RegexShorthand): String = when (sh) {
        RegexShorthand.Word -> "word character (letter/digit/_)"
        RegexShorthand.Digit -> "digit (0-9)"
        RegexShorthand.Space -> "whitespace"
        RegexShorthand.NotWord -> "non-word character"
        RegexShorthand.NotDigit -> "non-digit"
        RegexShorthand.NotSpace -> "non-whitespace"
        RegexShorthand.VertSpace -> "vertical whitespace"
        RegexShorthand.HorizSpace -> "horizontal whitespace"
    }

    private fun shorthandPlural(sh: RegexShorthand): String = when (sh) {
        RegexShorthand.Word -> "word characters (letters, digits, or underscores)"
        RegexShorthand.Digit -> "digits (0-9)"
        RegexShorthand.Space -> "whitespace characters"
        RegexShorthand.NotWord -> "non-word characters"
        RegexShorthand.NotDigit -> "non-digit characters"
        RegexShorthand.NotSpace -> "non-whitespace characters"
        RegexShorthand.VertSpace -> "vertical whitespace characters"
        RegexShorthand.HorizSpace -> "horizontal whitespace characters"
    }

    private fun rangeDescription(first: Char, last: Char): String = when {
        first == 'a' && last == 'z' -> "letter (a-z)"
        first == 'A' && last == 'Z' -> "uppercase letter (A-Z)"
        first == '0' && last == '9' -> "digit (0-9)"
        else -> "character in range '$first'-'$last'"
    }

    private fun rangePlural(first: Char, last: Char): String = when {
        first == 'a' && last == 'z' -> "letters (a-z)"
        first == 'A' && last == 'Z' -> "uppercase letters (A-Z)"
        first == '0' && last == '9' -> "digits (0-9)"
        else -> "characters in range '$first'-'$last'"
    }

    private enum class Context {
        TOP, SEQUENCE, ALTERNATION, GROUP, REPETITION,
    }

    companion object {
        private const val HEX_RADIX = 16
    }
}
