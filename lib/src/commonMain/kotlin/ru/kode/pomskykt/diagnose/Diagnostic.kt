package ru.kode.pomskykt.diagnose

import ru.kode.pomskykt.syntax.Span

/**
 * A compiler diagnostic (error or warning).
 *
 * Ported from pomsky-lib/src/diagnose/diagnostics.rs.
 */
data class Diagnostic(
    val severity: Severity,
    val msg: String,
    val code: DiagnosticCode? = null,
    val help: String? = null,
    val span: Span = Span.EMPTY,
    val kind: DiagnosticKind = DiagnosticKind.Other,
)

enum class Severity { Error, Warning }

enum class DiagnosticKind {
    Syntax, Resolve, Compat, Unsupported,
    Deprecated, Limits, Invalid, Test, Other,
}

/**
 * Numeric diagnostic codes.
 */
enum class DiagnosticCode(val value: Int) {
    // Lex errors
    UnknownToken(1),
    RegexGroupSyntax(2),
    RegexBackslashSyntax(3),
    UnclosedString(4),
    LeadingZero(6),
    FileTooBig(7),

    // Parse errors
    UnexpectedToken(100),
    UnexpectedReservedWord(101),
    IdentTooLong(103),
    RangeIsNotIncreasing(104),
    UnallowedNot(106),
    InvalidEscapeInString(108),
    InvalidNumber(110),
    RepetitionNotAscending(111),
    CharClassUnknownShorthand(116),
    MissingKeyword(120),

    // Compile errors
    LetBindingExists(300),
    UnsupportedRegexFeature(301),
    UnsupportedPomskySyntax(302),
    HugeReference(303),
    UnknownReference(304),
    NameUsedMultipleTimes(305),
    CaptureInLet(308),
    ReferenceInLet(309),
    UnknownVariable(310),
    RecursiveVariable(311),
    RangeIsTooBig(312),
    RecursionLimit(313),
    IllegalNegation(317),
    InfiniteRecursion(322),

    // Warnings
    PossiblyUnsupported(400),

    // Test diagnostics
    TestNoExactMatch(500),
    TestMissingSubstringMatch(501),
}

/**
 * Regex feature that may not be supported in all flavors.
 */
enum class Feature {
    AtomicGroups, Lookaround, Grapheme,
    UnicodeScript, UnicodeBlock, UnicodeProp,
    SpecificUnicodeProp, Backreference, ForwardReference,
    NegativeShorthandW, NegativeShorthandS, ShorthandW,
    MixedReferences, RepeatedAssertion, Recursion,
    UnicodeWordBoundaries, WordStartEnd, ScriptExtensions,
    CharSetIntersection, RepetitionAbove1000,
    ReuseGroups, AsciiLineBreaks,
    NamedGroups,
}
