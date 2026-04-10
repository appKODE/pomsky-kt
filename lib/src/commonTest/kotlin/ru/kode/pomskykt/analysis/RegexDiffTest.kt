package ru.kode.pomskykt.analysis

import ru.kode.pomskykt.regex.Regex
import ru.kode.pomskykt.regex.RegexAlternation
import ru.kode.pomskykt.regex.RegexCharSet
import ru.kode.pomskykt.regex.RegexCharSetItem
import ru.kode.pomskykt.regex.RegexGroup
import ru.kode.pomskykt.regex.RegexGroupKind
import ru.kode.pomskykt.regex.RegexLookaround
import ru.kode.pomskykt.regex.RegexRepetition
import ru.kode.pomskykt.syntax.exprs.BoundaryKind
import ru.kode.pomskykt.syntax.exprs.LookaroundKind
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RegexDiffTest {

    @Test
    fun identicalPatternsNoDiffs() {
        val a = Regex.Literal("hello")
        val b = Regex.Literal("hello")
        val report = RegexDiff.diff(a, b)
        assertTrue(report.onlyInA.isEmpty())
        assertTrue(report.onlyInB.isEmpty())
        assertTrue(report.structuralDiffs.isEmpty())
        assertEquals(1, report.common.size)
    }

    @Test
    fun differentLiterals() {
        val a = Regex.Literal("cat")
        val b = Regex.Literal("dog")
        val report = RegexDiff.diff(a, b)
        assertTrue(report.onlyInA.any { "cat" in it.description })
        assertTrue(report.onlyInB.any { "dog" in it.description })
    }

    @Test
    fun charSetDifference() {
        val a = Regex.CharSet(
            RegexCharSet(
                items = listOf(RegexCharSetItem.Range('a', 'z')),
                negative = false,
            )
        )
        val b = Regex.CharSet(
            RegexCharSet(
                items = listOf(
                    RegexCharSetItem.Range('a', 'z'),
                    RegexCharSetItem.Range('A', 'Z'),
                ),
                negative = false,
            )
        )
        val report = RegexDiff.diff(a, b)
        assertTrue(report.onlyInB.any { "A" in it.description && "Z" in it.description })
        assertTrue(report.common.any { "a" in it.description && "z" in it.description })
    }

    @Test
    fun quantifierDifference() {
        val inner = Regex.Literal("x")
        val a = Regex.Rep(RegexRepetition(inner = inner, lower = 1, upper = null, greedy = true))
        val b = Regex.Rep(RegexRepetition(inner = inner, lower = 0, upper = null, greedy = true))
        val report = RegexDiff.diff(a, b)
        assertTrue(report.onlyInA.any { "one or more" in it.description })
        assertTrue(report.onlyInB.any { "zero or more" in it.description })
    }

    @Test
    fun anchorDifference() {
        val a = Regex.Sequence(
            parts = listOf(
                Regex.Bound(BoundaryKind.Start),
                Regex.Literal("abc"),
                Regex.Bound(BoundaryKind.End),
            )
        )
        val b = Regex.Literal("abc")
        val report = RegexDiff.diff(a, b)
        assertTrue(report.onlyInA.any { "start" in it.description.lowercase() })
        assertTrue(report.onlyInA.any { "end" in it.description.lowercase() })
    }

    @Test
    fun alternationBranchDifference() {
        val a = Regex.Alt(
            RegexAlternation(
                alternatives = listOf(
                    Regex.Literal("a"),
                    Regex.Literal("b"),
                )
            )
        )
        val b = Regex.Alt(
            RegexAlternation(
                alternatives = listOf(
                    Regex.Literal("a"),
                    Regex.Literal("b"),
                    Regex.Literal("c"),
                )
            )
        )
        val report = RegexDiff.diff(a, b)
        assertTrue(report.onlyInB.any { "c" in it.description })
    }

    @Test
    fun groupKindDifference() {
        val a = Regex.Group(
            RegexGroup(
                parts = listOf(Regex.Literal("x")),
                kind = RegexGroupKind.Numbered(1),
            )
        )
        val b = Regex.Group(
            RegexGroup(
                parts = listOf(Regex.Literal("x")),
                kind = RegexGroupKind.Named("foo", 1),
            )
        )
        val report = RegexDiff.diff(a, b)
        assertTrue(report.onlyInA.any { "capturing group #1" in it.description })
        assertTrue(report.onlyInB.any { "named group 'foo'" in it.description })
    }

    @Test
    fun lookaroundDifference() {
        val a = Regex.Look(RegexLookaround(kind = LookaroundKind.Ahead, inner = Regex.Literal("x")))
        val b = Regex.Look(RegexLookaround(kind = LookaroundKind.Behind, inner = Regex.Literal("x")))
        val report = RegexDiff.diff(a, b)
        assertTrue(report.onlyInA.any { "lookahead" in it.description })
        assertTrue(report.onlyInB.any { "lookbehind" in it.description })
    }

    @Test
    fun structuralDiffNodeType() {
        val a = Regex.Literal("x")
        val b = Regex.Dot
        val report = RegexDiff.diff(a, b)
        assertTrue(report.structuralDiffs.any { "node type differs" in it })
    }

    @Test
    fun structuralDiffSequenceLength() {
        val a = Regex.Sequence(parts = listOf(Regex.Literal("a"), Regex.Literal("b")))
        val b = Regex.Sequence(parts = listOf(Regex.Literal("a")))
        val report = RegexDiff.diff(a, b)
        assertTrue(report.structuralDiffs.any { "sequence length differs" in it })
    }

    @Test
    fun structuralDiffRepetitionBounds() {
        val a = Regex.Rep(RegexRepetition(inner = Regex.Literal("x"), lower = 1, upper = 3, greedy = true))
        val b = Regex.Rep(RegexRepetition(inner = Regex.Literal("x"), lower = 2, upper = 5, greedy = true))
        val report = RegexDiff.diff(a, b)
        assertTrue(report.structuralDiffs.any { "repetition bounds differ" in it })
    }

    @Test
    fun dotAndGraphemeFeatures() {
        val a = Regex.Dot
        val b = Regex.Grapheme
        val report = RegexDiff.diff(a, b)
        assertTrue(report.onlyInA.any { "any character" in it.description })
        assertTrue(report.onlyInB.any { "grapheme" in it.description })
    }

    @Test
    fun negatedCharSet() {
        val a = Regex.CharSet(
            RegexCharSet(
                items = listOf(RegexCharSetItem.Char('a')),
                negative = false,
            )
        )
        val b = Regex.CharSet(
            RegexCharSet(
                items = listOf(RegexCharSetItem.Char('a')),
                negative = true,
            )
        )
        val report = RegexDiff.diff(a, b)
        assertTrue(report.onlyInA.any { "char 'a'" in it.description && "negated" !in it.description })
        assertTrue(report.onlyInB.any { "negated" in it.description && "char 'a'" in it.description })
    }
}
