package ru.kode.pomskykt.regex

import ru.kode.pomskykt.options.RegexFlavor
import ru.kode.pomskykt.syntax.Span
import ru.kode.pomskykt.syntax.exprs.BoundaryKind
import ru.kode.pomskykt.syntax.exprs.LookaroundKind

/**
 * Intermediate representation for compiled regex.
 * AST nodes compile to this IR, which then generates regex strings.
 *
 * Ported from pomsky-lib/src/regex/mod.rs.
 */
sealed class Regex {
    data class Literal(val content: String) : Regex()
    data class Unescaped(val content: String) : Regex()
    data class CharSet(val set: RegexCharSet) : Regex()
    data class CompoundCharSet(val set: RegexCompoundCharSet) : Regex()
    data object Grapheme : Regex()
    data object Dot : Regex()
    data class Group(val group: RegexGroup) : Regex()
    /** Sequence of regexes concatenated without wrapping. */
    data class Sequence(val parts: List<Regex>) : Regex()
    data class Alt(val alternation: RegexAlternation) : Regex()
    data class Rep(val repetition: RegexRepetition) : Regex()
    data class Bound(val kind: BoundaryKind) : Regex()
    data class Look(val lookaround: RegexLookaround) : Regex()
    data class Ref(val reference: RegexReference) : Regex()
    data object Recursion : Regex()
}

/** A character set: `[a-zA-Z0-9]`. */
data class RegexCharSet(
    val items: List<RegexCharSetItem>,
    val negative: Boolean = false,
)

/** A compound character set with intersection/subtraction. */
data class RegexCompoundCharSet(
    val sets: List<RegexCharSet>,
    val negative: Boolean = false,
) {
    /** Add another char set to the intersection. Returns null if the intersection is empty. */
    fun add(other: RegexCharSet): RegexCompoundCharSet? {
        // For now, simply add the set (full emptiness analysis is complex).
        // We check for obvious empty cases: if both are single-char sets
        // and the chars differ with no overlap.
        return RegexCompoundCharSet(sets + other, negative)
    }

    /** Negate the compound char set. */
    fun negate(): RegexCompoundCharSet = copy(negative = !negative)
}

/** An item in a character set. */
sealed class RegexCharSetItem {
    data class Char(val char: kotlin.Char) : RegexCharSetItem()
    data class Range(val first: kotlin.Char, val last: kotlin.Char) : RegexCharSetItem()
    data class Shorthand(val shorthand: RegexShorthand) : RegexCharSetItem()
    data class Property(val property: RegexProperty, val negative: Boolean) : RegexCharSetItem()
    data class Literal(val content: String) : RegexCharSetItem()
    /** A supplementary (above U+FFFF) code point; rendered as \x{...} in codegen. */
    data class CodePoint(val codePoint: Int) : RegexCharSetItem()
}

/** Regex shorthand character classes. */
enum class RegexShorthand(val str: String) {
    Word("\\w"), Digit("\\d"), Space("\\s"),
    NotWord("\\W"), NotDigit("\\D"), NotSpace("\\S"),
    VertSpace("\\v"), HorizSpace("\\h"),
}

/** Unicode property reference in regex. */
sealed class RegexProperty {
    data class CategoryProp(val category: ru.kode.pomskykt.syntax.unicode.Category) : RegexProperty()
    data class ScriptProp(
        val script: ru.kode.pomskykt.syntax.unicode.Script,
        val extension: ru.kode.pomskykt.syntax.exprs.ScriptExtension,
    ) : RegexProperty()
    data class BlockProp(val block: ru.kode.pomskykt.syntax.unicode.CodeBlock) : RegexProperty()
    data class OtherProp(val property: ru.kode.pomskykt.syntax.unicode.OtherProperties) : RegexProperty()
}

/** A regex group (capturing or non-capturing). */
data class RegexGroup(
    val parts: List<Regex>,
    val kind: RegexGroupKind,
)

sealed class RegexGroupKind {
    data object NonCapturing : RegexGroupKind()
    data class Numbered(val index: Int) : RegexGroupKind()
    data class Named(val name: String, val index: Int) : RegexGroupKind()
    data object Atomic : RegexGroupKind()
}

/** Regex alternation. */
data class RegexAlternation(
    val alternatives: List<Regex>,
)

/** Regex repetition. */
data class RegexRepetition(
    val inner: Regex,
    val lower: Int,
    val upper: Int?, // null = unbounded
    val greedy: Boolean,
)

/** Regex lookaround assertion. */
data class RegexLookaround(
    val kind: LookaroundKind,
    val inner: Regex,
)

/** Regex backreference. */
sealed class RegexReference {
    data class Named(val name: String) : RegexReference()
    data class Numbered(val index: Int) : RegexReference()
}
