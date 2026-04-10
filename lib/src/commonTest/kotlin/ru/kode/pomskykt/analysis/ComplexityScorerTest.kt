package ru.kode.pomskykt.analysis

import ru.kode.pomskykt.regex.Regex
import ru.kode.pomskykt.regex.RegexAlternation
import ru.kode.pomskykt.regex.RegexCharSet
import ru.kode.pomskykt.regex.RegexCharSetItem
import ru.kode.pomskykt.regex.RegexCompoundCharSet
import ru.kode.pomskykt.regex.RegexGroup
import ru.kode.pomskykt.regex.RegexGroupKind
import ru.kode.pomskykt.regex.RegexLookaround
import ru.kode.pomskykt.regex.RegexReference
import ru.kode.pomskykt.regex.RegexRepetition
import ru.kode.pomskykt.regex.RegexShorthand
import ru.kode.pomskykt.syntax.exprs.LookaroundKind
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ComplexityScorerTest {

    @Test
    fun simpleLiteralIsLow() {
        // "hello" -> Literal("hello")
        val report = ComplexityScorer.score(Regex.Literal("hello"))
        assertEquals(ComplexityReport.Level.LOW, report.level)
        assertTrue(report.score <= 3)
    }

    @Test
    fun simpleCharClassIsLow() {
        // [a-z]+ -> Rep(CharSet([a-z]), 1, null, true)
        val report = ComplexityScorer.score(
            Regex.Rep(
                RegexRepetition(
                    inner = Regex.CharSet(
                        RegexCharSet(
                            items = listOf(RegexCharSetItem.Range('a', 'z')),
                        )
                    ),
                    lower = 1,
                    upper = null,
                    greedy = true,
                )
            )
        )
        assertEquals(ComplexityReport.Level.LOW, report.level)
    }

    @Test
    fun nestedQuantifiersIsHigh() {
        // (a+)+ -> Rep(Group([Rep(Literal("a"), 1, null, true)]), 1, null, true)
        val inner = Regex.Rep(
            RegexRepetition(
                inner = Regex.Literal("a"),
                lower = 1,
                upper = null,
                greedy = true,
            )
        )
        val outer = Regex.Rep(
            RegexRepetition(
                inner = Regex.Group(
                    RegexGroup(
                        parts = listOf(inner),
                        kind = RegexGroupKind.Numbered(1),
                    )
                ),
                lower = 1,
                upper = null,
                greedy = true,
            )
        )
        val report = ComplexityScorer.score(outer)
        assertEquals(ComplexityReport.Level.HIGH, report.level)
        assertTrue(report.score >= 7)
        assertTrue(report.factors.any { "nested" in it.description.lowercase() })
    }

    @Test
    fun manyAlternationsAddComplexity() {
        // a|b|c|d|e|f|g|h|i|j -> 10 alternation branches
        val alts = ('a'..'j').map { Regex.Literal(it.toString()) }
        val report = ComplexityScorer.score(
            Regex.Alt(RegexAlternation(alts))
        )
        assertTrue(report.factors.any { "alternation" in it.description.lowercase() })
    }

    @Test
    fun fewAlternationsNoFactor() {
        // a|b|c -> 3 branches, below threshold of 5
        val alts = ('a'..'c').map { Regex.Literal(it.toString()) }
        val report = ComplexityScorer.score(
            Regex.Alt(RegexAlternation(alts))
        )
        assertTrue(report.factors.none { "alternation" in it.description.lowercase() })
    }

    @Test
    fun lookaroundAddsComplexity() {
        // (?=\d)a -> Sequence(Look(Ahead, CharSet(\d)), Literal("a"))
        val report = ComplexityScorer.score(
            Regex.Sequence(
                listOf(
                    Regex.Look(
                        RegexLookaround(
                            kind = LookaroundKind.Ahead,
                            inner = Regex.CharSet(
                                RegexCharSet(
                                    items = listOf(RegexCharSetItem.Shorthand(RegexShorthand.Digit)),
                                )
                            ),
                        )
                    ),
                    Regex.Literal("a"),
                )
            )
        )
        assertTrue(report.factors.any { "lookaround" in it.description.lowercase() })
    }

    @Test
    fun backreferenceAddsComplexity() {
        // (a)\1 -> Sequence(Group(Literal("a")), Ref(Numbered(1)))
        val report = ComplexityScorer.score(
            Regex.Sequence(
                listOf(
                    Regex.Group(
                        RegexGroup(
                            parts = listOf(Regex.Literal("a")),
                            kind = RegexGroupKind.Numbered(1),
                        )
                    ),
                    Regex.Ref(RegexReference.Numbered(1)),
                )
            )
        )
        assertTrue(report.factors.any { "backreference" in it.description.lowercase() })
    }

    @Test
    fun recursionAddsComplexity() {
        // a(?R)?b -> Sequence(Literal("a"), Rep(Recursion, 0, 1, true), Literal("b"))
        val report = ComplexityScorer.score(
            Regex.Sequence(
                listOf(
                    Regex.Literal("a"),
                    Regex.Rep(
                        RegexRepetition(
                            inner = Regex.Recursion,
                            lower = 0,
                            upper = 1,
                            greedy = true,
                        )
                    ),
                    Regex.Literal("b"),
                )
            )
        )
        assertTrue(report.factors.any { "recursive" in it.description.lowercase() })
    }

    @Test
    fun scoreIsClamped() {
        // ((a+)+)+ -> three levels of unbounded nesting
        val innermost = Regex.Rep(
            RegexRepetition(
                inner = Regex.Literal("a"),
                lower = 1,
                upper = null,
                greedy = true,
            )
        )
        val middle = Regex.Rep(
            RegexRepetition(
                inner = Regex.Group(
                    RegexGroup(
                        parts = listOf(innermost),
                        kind = RegexGroupKind.Numbered(1),
                    )
                ),
                lower = 1,
                upper = null,
                greedy = true,
            )
        )
        val outer = Regex.Rep(
            RegexRepetition(
                inner = Regex.Group(
                    RegexGroup(
                        parts = listOf(middle),
                        kind = RegexGroupKind.Numbered(2),
                    )
                ),
                lower = 1,
                upper = null,
                greedy = true,
            )
        )
        val report = ComplexityScorer.score(outer)
        assertTrue(report.score <= 10)
        assertTrue(report.score >= 1)
    }

    @Test
    fun compoundCharSetAddsComplexity() {
        val report = ComplexityScorer.score(
            Regex.CompoundCharSet(
                RegexCompoundCharSet(
                    sets = listOf(
                        RegexCharSet(listOf(RegexCharSetItem.Range('a', 'z'))),
                        RegexCharSet(listOf(RegexCharSetItem.Shorthand(RegexShorthand.Word))),
                    ),
                )
            )
        )
        assertTrue(report.factors.any { "intersection" in it.description.lowercase() })
    }

    @Test
    fun dotAndBoundAreLeafNodes() {
        val report = ComplexityScorer.score(
            Regex.Sequence(
                listOf(
                    Regex.Dot,
                    Regex.Grapheme,
                )
            )
        )
        assertEquals(ComplexityReport.Level.LOW, report.level)
        assertTrue(report.factors.isEmpty())
    }

    @Test
    fun boundedRepetitionDoesNotIncreaseNestingDepth() {
        // (a{2,3})+ -> outer is unbounded at depth 0, inner is bounded so does not nest
        val inner = Regex.Rep(
            RegexRepetition(
                inner = Regex.Literal("a"),
                lower = 2,
                upper = 3,
                greedy = true,
            )
        )
        val outer = Regex.Rep(
            RegexRepetition(
                inner = Regex.Group(
                    RegexGroup(
                        parts = listOf(inner),
                        kind = RegexGroupKind.NonCapturing,
                    )
                ),
                lower = 1,
                upper = null,
                greedy = true,
            )
        )
        val report = ComplexityScorer.score(outer)
        // No nested unbounded quantifiers factor since inner is bounded
        assertTrue(report.factors.none { "nested" in it.description.lowercase() })
    }
}
