package ru.kode.pomskykt.decompiler

import ru.kode.pomskykt.regex.RegexShorthand

/**
 * Tokens produced by the regex lexer.
 */
internal sealed class RegexToken {
    data class Char(val value: kotlin.Char) : RegexToken()
    data class CodePoint(val value: Int) : RegexToken()
    data class Shorthand(val kind: RegexShorthand) : RegexToken()
    data class UnicodeProperty(val name: String, val negative: Boolean) : RegexToken()

    // Character class delimiters
    data object OpenBracket : RegexToken()
    data object CloseBracket : RegexToken()
    data object CaretInClass : RegexToken()
    data object Hyphen : RegexToken()
    data object ClassIntersection : RegexToken()

    // Groups
    data object OpenParen : RegexToken()
    data object CloseParen : RegexToken()
    data object NonCapturing : RegexToken()
    data class NamedGroup(val name: String) : RegexToken()
    data object AtomicGroup : RegexToken()
    data object LookaheadPos : RegexToken()
    data object LookaheadNeg : RegexToken()
    data object LookbehindPos : RegexToken()
    data object LookbehindNeg : RegexToken()

    // Quantifiers
    data object Star : RegexToken()
    data object Plus : RegexToken()
    data object Question : RegexToken()
    data class Repeat(val min: Int, val max: Int?) : RegexToken()
    data object Lazy : RegexToken()

    // Anchors
    data object StartAnchor : RegexToken()
    data object EndAnchor : RegexToken()
    data object WordBoundary : RegexToken()
    data object NotWordBoundary : RegexToken()
    data object WordStart : RegexToken()
    data object WordEnd : RegexToken()

    // Operators
    data object Pipe : RegexToken()
    data object Dot : RegexToken()

    // Backreferences
    data class BackrefNumbered(val index: Int) : RegexToken()
    data class BackrefNamed(val name: String) : RegexToken()

    // Special
    data object GraphemeCluster : RegexToken()
    data object Recursion : RegexToken()
}
