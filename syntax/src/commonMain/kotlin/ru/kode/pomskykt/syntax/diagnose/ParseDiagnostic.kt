package ru.kode.pomskykt.syntax.diagnose

import ru.kode.pomskykt.syntax.Span
import ru.kode.pomskykt.syntax.lexer.LexDiagnostics
import ru.kode.pomskykt.syntax.lexer.LexErrorMsg
import ru.kode.pomskykt.syntax.lexer.Token

/** A diagnostic (error or warning) from parsing. */
data class ParseDiagnostic(
    val kind: ParseDiagnosticKind,
    val span: Span,
)

sealed class ParseDiagnosticKind {
    data class Error(val error: ParseErrorKind) : ParseDiagnosticKind()
    data class Warning(val warning: ParseWarningKind) : ParseDiagnosticKind()
}

/** A parse error with location. */
data class ParseError(
    val kind: ParseErrorKind,
    val span: Span,
) {
    fun toDiagnostic() = ParseDiagnostic(ParseDiagnosticKind.Error(kind), span)
}

/** All possible parse error kinds. */
sealed class ParseErrorKind {
    data object UnknownToken : ParseErrorKind()
    data class LexError(val msg: LexErrorMsg) : ParseErrorKind()
    data class KeywordAfterLet(val word: String) : ParseErrorKind()
    data class KeywordAfterColon(val word: String) : ParseErrorKind()
    data class NonAsciiIdentAfterColon(val char: Char) : ParseErrorKind()
    data class GroupNameTooLong(val len: Int) : ParseErrorKind()
    data class UnexpectedKeyword(val word: String) : ParseErrorKind()
    data class Expected(val what: String) : ParseErrorKind()
    data object LeftoverTokens : ParseErrorKind()
    data class ExpectedToken(val token: Token) : ParseErrorKind()
    data object RangeIsNotIncreasing : ParseErrorKind()
    data object RangeLeadingZeroesVariableLength : ParseErrorKind()
    data object UnallowedNot : ParseErrorKind()
    data class UnallowedMultiNot(val count: Int) : ParseErrorKind()
    data object LonePipe : ParseErrorKind()
    data object LetBindingExists : ParseErrorKind()
    data object MissingLetKeyword : ParseErrorKind()
    data class InvalidEscapeInStringAt(val position: Int) : ParseErrorKind()
    data class CharStringError(val error: CharStringErr) : ParseErrorKind()
    data class CharClassError(val error: CharClassErr) : ParseErrorKind()
    data object InvalidCodePoint : ParseErrorKind()
    data class NumberError(val error: NumberErr) : ParseErrorKind()
    data class RepetitionError(val error: RepetitionErr) : ParseErrorKind()
    data object MultipleStringsInTestCase : ParseErrorKind()
    data object RecursionLimit : ParseErrorKind()
}

enum class CharStringErr { Empty, TooManyCodePoints }

sealed class CharClassErr {
    data object Empty : CharClassErr()
    data object CaretInGroup : CharClassErr()
    data class NonAscendingRange(val first: Char, val last: Char) : CharClassErr()
    data object Invalid : CharClassErr()
    data object Unallowed : CharClassErr()
    data class UnknownNamedClass(val found: String, val extraInPrefix: Boolean) : CharClassErr()
    data object Negative : CharClassErr()
    data object UnexpectedPrefix : CharClassErr()
    data class WrongPrefix(val expected: String, val hasInPrefix: Boolean) : CharClassErr()
}

enum class NumberErr { Empty, InvalidDigit, TooLarge, TooSmall, Zero }

enum class RepetitionErr { NotAscending, QmSuffix, Multi }

/** A parse warning with location. */
data class ParseWarning(
    val kind: ParseWarningKind,
    val span: Span,
) {
    fun toDiagnostic() = ParseDiagnostic(ParseDiagnosticKind.Warning(kind), span)
}

sealed class ParseWarningKind {
    data class Deprecation(val warning: DeprecationWarning) : ParseWarningKind()
}

sealed class DeprecationWarning {
    data class Unicode(val text: String) : DeprecationWarning()
    data class ShorthandInRange(val char: Char) : DeprecationWarning()
}

// ---------------------------------------------------------------------------
// Human-readable message formatting (ported from Rust Display impls)
// ---------------------------------------------------------------------------

/** Convert a [ParseDiagnosticKind] to a human-readable message. */
fun ParseDiagnosticKind.toMessage(): String = when (this) {
    is ParseDiagnosticKind.Error -> error.toMessage()
    is ParseDiagnosticKind.Warning -> warning.toMessage()
}

/** Convert a [ParseErrorKind] to a human-readable message. Ported from pomsky-syntax/src/error.rs Display. */
fun ParseErrorKind.toMessage(): String = when (this) {
    is ParseErrorKind.UnknownToken -> "Unknown token"
    is ParseErrorKind.LexError -> msg.message
    is ParseErrorKind.KeywordAfterLet -> "Unexpected keyword `$word`"
    is ParseErrorKind.KeywordAfterColon -> "Unexpected keyword `$word`"
    is ParseErrorKind.NonAsciiIdentAfterColon -> {
        val num = char.code
        "Group name contains illegal code point `$char` (U+${num.toString(16).padStart(4, '0').uppercase()}). Group names must be ASCII only."
    }
    is ParseErrorKind.GroupNameTooLong ->
        "Group name is too long. It is $len code points long, but must be at most 128 code points."
    is ParseErrorKind.UnexpectedKeyword -> "Unexpected keyword `$word`"
    is ParseErrorKind.Expected -> "Expected $what"
    is ParseErrorKind.LeftoverTokens -> "There are leftover tokens that couldn't be parsed"
    is ParseErrorKind.ExpectedToken -> "Expected ${token.displayName}"
    is ParseErrorKind.RangeIsNotIncreasing ->
        "The first number in a range must be smaller than the second"
    is ParseErrorKind.RangeLeadingZeroesVariableLength ->
        "Leading zeroes are not allowed, unless both numbers have the same number of digits"
    is ParseErrorKind.UnallowedNot -> "This code point or range can't be negated"
    is ParseErrorKind.UnallowedMultiNot ->
        "A shorthand character class can't be negated more than once"
    is ParseErrorKind.LonePipe -> "A pipe must be followed by an expression"
    is ParseErrorKind.LetBindingExists ->
        "A variable with the same name already exists in this scope"
    is ParseErrorKind.MissingLetKeyword ->
        "A variable declaration must start with the `let` keyword"
    is ParseErrorKind.InvalidEscapeInStringAt ->
        "Unsupported escape sequence in string"
    is ParseErrorKind.InvalidCodePoint ->
        "This code point is outside the allowed range"
    is ParseErrorKind.MultipleStringsInTestCase ->
        "Test cases can't have multiple strings"
    is ParseErrorKind.CharStringError -> error.toMessage()
    is ParseErrorKind.CharClassError -> error.toMessage()
    is ParseErrorKind.NumberError -> error.toMessage()
    is ParseErrorKind.RepetitionError -> error.toMessage()
    is ParseErrorKind.RecursionLimit -> "Recursion limit reached"
}

fun CharStringErr.toMessage(): String = when (this) {
    CharStringErr.Empty -> "Strings used in ranges can't be empty"
    CharStringErr.TooManyCodePoints -> "Strings used in ranges can only contain 1 code point"
}

fun CharClassErr.toMessage(): String = when (this) {
    is CharClassErr.Empty -> "This character class is empty"
    is CharClassErr.CaretInGroup -> "`^` is not allowed here"
    is CharClassErr.NonAscendingRange -> {
        val a = first.code
        val b = last.code
        "Character range must be in increasing order, but it is U+${a.toString(16).padStart(4, '0').uppercase()} - U+${b.toString(16).padStart(4, '0').uppercase()}"
    }
    is CharClassErr.Invalid ->
        "Expected string, range, code point or named character class"
    is CharClassErr.Unallowed ->
        "This combination of character classes is not allowed"
    is CharClassErr.UnknownNamedClass -> {
        if (extraInPrefix) {
            "Unknown character class `${found.replaceFirst("In", "blk:")}`"
        } else {
            "Unknown character class `$found`"
        }
    }
    is CharClassErr.Negative -> "This character class can't be negated"
    is CharClassErr.UnexpectedPrefix -> "This character class cannot have a prefix"
    is CharClassErr.WrongPrefix -> {
        if (hasInPrefix) {
            "This character class has the wrong prefix; it should be $expected,\n" +
                "and the `In` at the start should be removed"
        } else {
            "This character class has the wrong prefix; it should be $expected"
        }
    }
}

fun NumberErr.toMessage(): String = when (this) {
    NumberErr.Empty -> "cannot parse integer from empty string"
    NumberErr.InvalidDigit -> "invalid digit found in string"
    NumberErr.TooLarge -> "number too large"
    NumberErr.TooSmall -> "number too small"
    NumberErr.Zero -> "number would be zero for non-zero type"
}

fun RepetitionErr.toMessage(): String = when (this) {
    RepetitionErr.NotAscending -> "Lower bound can't be greater than the upper bound"
    RepetitionErr.QmSuffix -> "Unexpected `?` following a repetition"
    RepetitionErr.Multi -> "Only one repetition allowed"
}

fun ParseWarningKind.toMessage(): String = when (this) {
    is ParseWarningKind.Deprecation -> warning.toMessage()
}

fun DeprecationWarning.toMessage(): String = when (this) {
    is DeprecationWarning.Unicode -> text
    is DeprecationWarning.ShorthandInRange -> {
        val hex = char.code.toString(16).uppercase().padStart(2, '0')
        "Shorthands in character ranges are deprecated. Use U+$hex instead"
    }
}

/**
 * Returns the help text for a [LexErrorMsg], given the source slice that triggered the error.
 * Delegates to [LexDiagnostics] which is internal to this module.
 */
fun getLexHelp(msg: LexErrorMsg, slice: String): String? = LexDiagnostics.getHelp(msg, slice)
