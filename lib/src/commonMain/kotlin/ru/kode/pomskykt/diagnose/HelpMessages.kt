package ru.kode.pomskykt.diagnose

import ru.kode.pomskykt.syntax.Span
import ru.kode.pomskykt.syntax.diagnose.CharClassErr
import ru.kode.pomskykt.syntax.diagnose.CharStringErr
import ru.kode.pomskykt.syntax.diagnose.ParseDiagnosticKind
import ru.kode.pomskykt.syntax.diagnose.ParseErrorKind
import ru.kode.pomskykt.syntax.diagnose.RepetitionErr
import ru.kode.pomskykt.syntax.diagnose.getLexHelp

/**
 * Help messages for parse-time diagnostics.
 *
 * Ported from pomsky-syntax/src/error.rs and pomsky-syntax/src/lexer/diagnostics.rs.
 */
fun getParseHelp(kind: ParseDiagnosticKind, input: String, span: Span): String? =
    when (kind) {
        is ParseDiagnosticKind.Error -> getParseErrorHelp(kind.error, input, span)
        is ParseDiagnosticKind.Warning -> null
    }

@Suppress("CyclomaticComplexMethod")
fun getParseErrorHelp(kind: ParseErrorKind, input: String, span: Span): String? = when (kind) {
    is ParseErrorKind.LexError ->
        getLexHelp(kind.msg, safeSubstring(input, span))

    is ParseErrorKind.LonePipe ->
        "Add an empty string ('') to match nothing"

    is ParseErrorKind.RepetitionError -> when (kind.error) {
        RepetitionErr.Multi ->
            "Add parentheses around the first repetition."
        RepetitionErr.QmSuffix ->
            "If you meant to make the repetition lazy, append the `lazy` keyword instead.\n" +
                "If this is intentional, consider adding parentheses around the inner repetition."
        RepetitionErr.NotAscending -> null
    }

    is ParseErrorKind.CharClassError -> when (val err = kind.error) {
        is CharClassErr.CaretInGroup ->
            "Use `![...]` to negate a character class"
        is CharClassErr.NonAscendingRange ->
            "Switch the characters: '${err.last}'-'${err.first}'"
        is CharClassErr.UnknownNamedClass -> getUnknownNamedClassHelp(err.found)
        else -> null
    }

    is ParseErrorKind.CharStringError -> {
        // Only suggest `range` expression when the string contains only digits
        val slice = safeSubstring(input, span).trim('\'', '"')
        if (kind.error == CharStringErr.TooManyCodePoints && slice.all { it.isDigit() }) {
            "Try a `range` expression instead:\nhttps://pomsky-lang.org/docs/language-tour/ranges/"
        } else null
    }

    is ParseErrorKind.RecursionLimit ->
        "Try a less nested expression. It helps to refactor it using variables:\n" +
            "https://pomsky-lang.org/docs/language-tour/variables/"

    is ParseErrorKind.LetBindingExists ->
        "Use a different name"

    is ParseErrorKind.MissingLetKeyword -> {
        val name = safeSubstring(input, span).trimEnd('=', ' ')
            .split("=").first().trim()
        if (name.isNotEmpty()) {
            "Try `let $name = ...`"
        } else {
            null
        }
    }

    is ParseErrorKind.KeywordAfterLet ->
        "Use a different variable name"

    is ParseErrorKind.RangeIsNotIncreasing -> {
        val source = safeSubstring(input, span)
        getRangeSwapHelp(source)
    }

    is ParseErrorKind.RangeLeadingZeroesVariableLength -> {
        val source = safeSubstring(input, span)
        getLeadingZeroHelp(source)
    }

    is ParseErrorKind.UnallowedMultiNot -> {
        if (kind.count % 2 == 0) {
            "The number of exclamation marks is even, so you can remove all of them"
        } else {
            "The number of exclamation marks is odd, so you can replace them with a single `!`"
        }
    }

    is ParseErrorKind.MultipleStringsInTestCase ->
        "Use `in \"some string\"` to match substrings in a haystack"

    else -> null
}

/**
 * Help messages for compile-time diagnostics.
 *
 * Ported from pomsky-lib/src/diagnose/compile_error.rs.
 */
@Suppress("CyclomaticComplexMethod")
fun getCompileHelp(kind: CompileErrorKind, span: Span, input: String): String? = when (kind) {
    is CompileErrorKind.NameUsedMultipleTimes ->
        "Give this group a different name"

    is CompileErrorKind.EmptyClassNegated ->
        "The group is empty because it contains both `${kind.group1}` and `${kind.group2}`, " +
            "which together match every code point"

    is CompileErrorKind.RelativeRefZero ->
        "Perhaps you meant `::-1` to refer to the previous or surrounding capturing group"

    is CompileErrorKind.BadIntersection ->
        "One character sets can be intersected.\nParentheses may be required to clarify the parsing order."

    is CompileErrorKind.IllegalNegation -> when (kind.kind) {
        is IllegalNegationKind.DotNetChar -> null
        else ->
            "Only the following expressions can be negated:\n" +
                "- character sets\n" +
                "- string literals and alternations that match exactly one code point\n" +
                "- lookarounds\n" +
                "- the `%` word boundary"
    }

    is CompileErrorKind.NegativeShorthandInAsciiMode ->
        "Enable Unicode for this expression"

    is CompileErrorKind.UnicodeInAsciiMode ->
        "Enable Unicode for this expression"

    is CompileErrorKind.InfiniteRecursion ->
        "A recursive expression must have a branch that doesn't reach the `recursion`, or can repeat 0 times"

    is CompileErrorKind.DotNetNumberedRefWithMixedGroups ->
        "Use a named reference, or don't mix named and unnamed capturing groups"

    is CompileErrorKind.UnknownVariable -> {
        val found = kind.found
        if (found.startsWith("U") && found.length > 1) {
            val rest = found.drop(1)
            "Perhaps you meant a code point: `U+$rest`"
        } else if (kind.similar != null) {
            "Perhaps you meant `${kind.similar}`"
        } else {
            null
        }
    }

    is CompileErrorKind.NestedQuantifiers ->
        "Rewrite to avoid nesting unbounded quantifiers. Use atomic groups or ensure alternatives don't overlap."

    is CompileErrorKind.UnknownReferenceNumber -> {
        if (kind.number == 0) {
            "Capturing group numbers start with 1"
        } else {
            null
        }
    }

    is CompileErrorKind.UnknownReferenceName -> {
        if (kind.similar != null) {
            "Perhaps you meant `${kind.similar}`"
        } else {
            null
        }
    }

    else -> null
}

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

private fun safeSubstring(input: String, span: Span): String =
    if (span.start >= 0 && span.end <= input.length && span.start <= span.end) {
        input.substring(span.start, span.end)
    } else {
        ""
    }

/**
 * Given source text of the form `'234'-'35'`, return `Switch the numbers: '35'-'234'`.
 */
private fun getRangeSwapHelp(source: String): String? {
    val match = Regex("""'([^']*)'[^']*'([^']*)'""").find(source) ?: return null
    val first = match.groupValues[1]
    val second = match.groupValues[2]
    return "Switch the numbers: '$second'-'$first'"
}

/**
 * Given source text of the form `'039'-'4918'`, return leading-zero help.
 * The source covers the full `'039'-'4918'` span.
 */
private fun getLeadingZeroHelp(source: String): String? {
    val match = Regex("""'0+(\d+)'-'(\d+)'""").find(source) ?: return null
    val stripped = match.groupValues[1]
    val second = match.groupValues[2]
    return "Precede with a repeated zero: '0'* range '$stripped'-'$second'"
}

private fun getUnknownNamedClassHelp(found: String): String? {
    // Simple prefix-based suggestions for common near-misses
    val suggestions = mapOf(
        "Grapheme" to "Grapheme_Base",
    )
    val suggestion = suggestions[found] ?: return null
    return "Perhaps you meant `$suggestion`"
}
