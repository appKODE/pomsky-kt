package ru.kode.pomskykt.diagnose

import ru.kode.pomskykt.features.UnsupportedError
import ru.kode.pomskykt.features.toMessage
import ru.kode.pomskykt.options.RegexFlavor
import ru.kode.pomskykt.syntax.Span

/**
 * A compile error with source location.
 *
 * Ported from pomsky-lib/src/diagnose/compile_error.rs.
 */
data class CompileError(
    val kind: CompileErrorKind,
    val span: Span,
)

/**
 * All possible compile error kinds.
 */
sealed class CompileErrorKind {
    data class Unsupported(val feature: Feature, val flavor: RegexFlavor) : CompileErrorKind()
    data class UnsupportedPomskySyntax(val error: UnsupportedError) : CompileErrorKind()
    data object HugeReference : CompileErrorKind()
    data class UnknownReferenceNumber(val number: Int) : CompileErrorKind()
    data class UnknownReferenceName(val found: String, val similar: String? = null) : CompileErrorKind()
    data class NameUsedMultipleTimes(val name: String) : CompileErrorKind()
    data class EmptyClassNegated(val group1: String, val group2: String) : CompileErrorKind()
    data object NegatedHorizVertSpace : CompileErrorKind()
    data class IllegalNegation(val kind: IllegalNegationKind) : CompileErrorKind()
    data object CaptureInLet : CompileErrorKind()
    data object ReferenceInLet : CompileErrorKind()
    data object RelativeRefZero : CompileErrorKind()
    data class UnknownVariable(val found: String, val similar: String? = null) : CompileErrorKind()
    data object RecursiveVariable : CompileErrorKind()
    data class RangeIsTooBig(val maxSize: Int) : CompileErrorKind()
    data object NegativeShorthandInAsciiMode : CompileErrorKind()
    data object UnicodeInAsciiMode : CompileErrorKind()
    data object DotNetNumberedRefWithMixedGroups : CompileErrorKind()
    data class RubyLookaheadInLookbehind(val wasWordBoundary: Boolean) : CompileErrorKind()
    data class UnsupportedInLookbehind(val flavor: RegexFlavor, val feature: Feature) : CompileErrorKind()
    data class LookbehindNotConstantLength(val flavor: RegexFlavor) : CompileErrorKind()
    data object NestedTest : CompileErrorKind()
    data object InfiniteRecursion : CompileErrorKind()
    data object BadIntersection : CompileErrorKind()
    data object EmptyIntersection : CompileErrorKind()
    data object NestedQuantifiers : CompileErrorKind()
}

sealed class IllegalNegationKind {
    data class Literal(val text: String) : IllegalNegationKind()
    data class DotNetChar(val charStr: String, val codePoint: Int) : IllegalNegationKind()
    data object Unescaped : IllegalNegationKind()
    data object Grapheme : IllegalNegationKind()
    data object Dot : IllegalNegationKind()
    data object Group : IllegalNegationKind()
    data object Alternation : IllegalNegationKind()
    data object Repetition : IllegalNegationKind()
    data object Reference : IllegalNegationKind()
    data object Recursion : IllegalNegationKind()
    data object Boundary : IllegalNegationKind()
}

// ---------------------------------------------------------------------------
// Human-readable message formatting (ported from Rust Display impls)
// ---------------------------------------------------------------------------

/** Convert a [CompileErrorKind] to a human-readable message. Ported from compile_error.rs Display. */
fun CompileErrorKind.toMessage(): String = when (this) {
    is CompileErrorKind.Unsupported -> {
        when (feature) {
            Feature.SpecificUnicodeProp ->
                "This Unicode property is not supported in the `$flavor` regex flavor"
            Feature.UnicodeWordBoundaries ->
                "In the `$flavor` flavor, word boundaries may only be used when Unicode is disabled"
            Feature.ShorthandW ->
                "In the `$flavor` flavor, `word` can only be used when Unicode is disabled"
            Feature.NegativeShorthandW ->
                "In the `$flavor` flavor, `word` can only be negated in a character class when Unicode is disabled"
            Feature.NegativeShorthandS ->
                "In the `$flavor` flavor, `space` can only be negated in a character class when Unicode is disabled"
            else ->
                "Unsupported feature `${feature.displayName}` in the `$flavor` regex flavor"
        }
    }
    is CompileErrorKind.UnsupportedPomskySyntax -> error.toMessage()
    is CompileErrorKind.HugeReference -> "Group references this large aren't supported"
    is CompileErrorKind.UnknownReferenceNumber ->
        "Reference to unknown group. There is no group number $number"
    is CompileErrorKind.UnknownReferenceName ->
        "Reference to unknown group. There is no group named `$found`"
    is CompileErrorKind.NameUsedMultipleTimes -> "Group name `$name` used multiple times"
    is CompileErrorKind.EmptyClassNegated ->
        "This negated character class matches nothing"
    is CompileErrorKind.NegatedHorizVertSpace ->
        "horiz_space and vert_space can't be negated within a character class"
    is CompileErrorKind.IllegalNegation -> kind.toMessage()
    is CompileErrorKind.CaptureInLet ->
        "Capturing groups within `let` statements are currently not supported"
    is CompileErrorKind.ReferenceInLet ->
        "References within `let` statements are currently not supported"
    is CompileErrorKind.RelativeRefZero -> "Relative references can't be 0"
    is CompileErrorKind.UnknownVariable -> "Variable `$found` doesn't exist"
    is CompileErrorKind.RecursiveVariable -> "Variables can't be used recursively"
    is CompileErrorKind.RangeIsTooBig ->
        "Range is too big, it isn't allowed to contain more than $maxSize digits"
    is CompileErrorKind.NegativeShorthandInAsciiMode ->
        "Shorthands can't be negated when Unicode is disabled"
    is CompileErrorKind.UnicodeInAsciiMode ->
        "Unicode properties can't be used when Unicode is disabled"
    is CompileErrorKind.DotNetNumberedRefWithMixedGroups ->
        "In the .NET flavor, numeric references are forbidden when there are both named " +
            "and unnamed capturing groups. This is because .NET counts named and unnamed " +
            "capturing groups separately, which is inconsistent with other flavors."
    is CompileErrorKind.RubyLookaheadInLookbehind -> {
        if (wasWordBoundary) {
            "In the Ruby flavor, `<` and `>` word boundaries are not allowed within lookbehind"
        } else {
            "In the Ruby flavor, lookahead is not allowed within lookbehind"
        }
    }
    is CompileErrorKind.NestedTest ->
        "Unit tests may only appear at the top level of the expression"
    is CompileErrorKind.UnsupportedInLookbehind ->
        "Feature `$feature` is not supported within lookbehinds in the $flavor flavor"
    is CompileErrorKind.LookbehindNotConstantLength ->
        "This kind of lookbehind is not supported in the $flavor flavor"
    is CompileErrorKind.InfiniteRecursion -> "This recursion never terminates"
    is CompileErrorKind.BadIntersection ->
        "Intersecting these expressions is not supported. Only character sets can be intersected."
    is CompileErrorKind.EmptyIntersection ->
        "Intersection of expressions that do not overlap"
    is CompileErrorKind.NestedQuantifiers ->
        "This expression may cause catastrophic backtracking (ReDoS) due to nested quantifiers"
}

/** Display name for a [Feature]. Ported from Rust Feature::name(). */
val Feature.displayName: String
    get() = when (this) {
        Feature.AtomicGroups -> "atomic groups"
        Feature.Lookaround -> "lookahead/behind"
        Feature.Grapheme -> "grapheme cluster matcher (\\X)"
        Feature.UnicodeScript -> "Unicode scripts (\\p{Script})"
        Feature.UnicodeBlock -> "Unicode blocks (\\p{InBlock})"
        Feature.UnicodeProp -> "Unicode properties (\\p{Property})"
        Feature.SpecificUnicodeProp -> "This particular Unicode property"
        Feature.Backreference -> "backreference"
        Feature.ForwardReference -> "forward reference"
        Feature.NegativeShorthandW -> "negative \\w shorthand in character class"
        Feature.NegativeShorthandS -> "negative \\s shorthand in character class"
        Feature.ShorthandW -> "\\w shorthand"
        Feature.MixedReferences -> "references to both named and numbered groups"
        Feature.RepeatedAssertion -> "single repeated assertion"
        Feature.Recursion -> "recursion"
        Feature.UnicodeWordBoundaries -> "word boundaries in Unicode mode"
        Feature.WordStartEnd -> "word start and word end"
        Feature.ScriptExtensions -> "Unicode script extensions"
        Feature.CharSetIntersection -> "Character set intersections"
        Feature.RepetitionAbove1000 -> "Repetition above 1000"
        Feature.ReuseGroups -> "reuse_groups mode ((?J))"
        Feature.AsciiLineBreaks -> "ascii_line_breaks mode ((?d))"
    }

fun IllegalNegationKind.toMessage(): String {
    return when (this) {
        is IllegalNegationKind.Literal -> "String literal \"$text\" can't be negated"
        is IllegalNegationKind.DotNetChar ->
            "Code point '$charStr' (U+${codePoint.toString(16).uppercase().padStart(4, '0')}) can't be negated in the .NET flavor, because it is above U+FFFF, and is therefore incorrectly treated as two code points by .NET."
        is IllegalNegationKind.Unescaped -> "An inline regex can't be negated"
        is IllegalNegationKind.Grapheme -> "A grapheme can't be negated"
        is IllegalNegationKind.Dot -> "The dot can't be negated"
        is IllegalNegationKind.Group -> "This group can't be negated"
        is IllegalNegationKind.Alternation -> "This alternation can't be negated"
        is IllegalNegationKind.Repetition -> "A repetition can't be negated"
        is IllegalNegationKind.Reference -> "A reference can't be negated"
        is IllegalNegationKind.Recursion -> "Recursion can't be negated"
        is IllegalNegationKind.Boundary -> "This boundary can't be negated"
    }
}
