package ru.kode.pomskykt.analysis

import ru.kode.pomskykt.regex.Regex
import ru.kode.pomskykt.regex.RegexAlternation
import ru.kode.pomskykt.regex.RegexCharSet
import ru.kode.pomskykt.regex.RegexCharSetItem
import ru.kode.pomskykt.regex.RegexGroupKind
import ru.kode.pomskykt.regex.RegexGroup
import ru.kode.pomskykt.regex.RegexLookaround
import ru.kode.pomskykt.regex.RegexReference
import ru.kode.pomskykt.regex.RegexRepetition
import ru.kode.pomskykt.regex.RegexShorthand
import ru.kode.pomskykt.syntax.exprs.BoundaryKind
import ru.kode.pomskykt.syntax.exprs.LookaroundKind
import kotlin.test.Test
import kotlin.test.assertTrue

class RailroadDiagramTest {
    @Test
    fun simpleLiteral() {
        val result = RailroadDiagram.render(Regex.Literal("hello"))
        assertTrue(result.contains("hello"), "Expected 'hello' in: $result")
    }

    @Test
    fun digitShorthand() {
        val ir = Regex.CharSet(RegexCharSet(
            items = listOf(RegexCharSetItem.Shorthand(RegexShorthand.Digit)),
            negative = false,
        ))
        val result = RailroadDiagram.render(ir)
        assertTrue(result.contains("\\d"), "Expected '\\d' in: $result")
    }

    @Test
    fun sequenceChains() {
        val ir = Regex.Sequence(listOf(
            Regex.Literal("ab"),
            Regex.Literal("cd"),
        ))
        val result = RailroadDiagram.render(ir)
        assertTrue(result.contains("ab"), "Expected 'ab' in: $result")
        assertTrue(result.contains("cd"), "Expected 'cd' in: $result")
    }

    @Test
    fun alternationBranches() {
        val ir = Regex.Alt(RegexAlternation(listOf(
            Regex.Literal("cat"),
            Regex.Literal("dog"),
        )))
        val result = RailroadDiagram.render(ir)
        assertTrue(result.contains("cat"), "Expected 'cat' in: $result")
        assertTrue(result.contains("dog"), "Expected 'dog' in: $result")
        assertTrue(
            result.contains("\u250c") || result.contains("\u251c") || result.contains("\u2514"),
            "Expected branch characters in: $result"
        )
    }

    @Test
    fun repetitionAnnotation() {
        val ir = Regex.Rep(RegexRepetition(
            inner = Regex.Literal("x"),
            lower = 1,
            upper = null,
            greedy = true,
        ))
        val result = RailroadDiagram.render(ir)
        assertTrue(result.contains("x"), "Expected 'x' in: $result")
        assertTrue(result.contains("+"), "Expected '+' quantifier in: $result")
    }

    @Test
    fun boundaryLabels() {
        val ir = Regex.Sequence(listOf(
            Regex.Bound(BoundaryKind.Start),
            Regex.Literal("test"),
            Regex.Bound(BoundaryKind.End),
        ))
        val result = RailroadDiagram.render(ir)
        assertTrue(result.contains("START"), "Expected 'START' in: $result")
        assertTrue(result.contains("END"), "Expected 'END' in: $result")
    }

    @Test
    fun dotNode() {
        val result = RailroadDiagram.render(Regex.Dot)
        assertTrue(result.contains("."), "Expected '.' in: $result")
    }

    @Test
    fun negatedCharSet() {
        val ir = Regex.CharSet(RegexCharSet(
            items = listOf(RegexCharSetItem.Shorthand(RegexShorthand.Digit)),
            negative = true,
        ))
        val result = RailroadDiagram.render(ir)
        assertTrue(result.contains("^"), "Expected negation marker in: $result")
    }

    @Test
    fun starQuantifier() {
        val ir = Regex.Rep(RegexRepetition(
            inner = Regex.Literal("a"),
            lower = 0,
            upper = null,
            greedy = true,
        ))
        val result = RailroadDiagram.render(ir)
        assertTrue(result.contains("*"), "Expected '*' quantifier in: $result")
    }

    @Test
    fun optionalQuantifier() {
        val ir = Regex.Rep(RegexRepetition(
            inner = Regex.Literal("a"),
            lower = 0,
            upper = 1,
            greedy = true,
        ))
        val result = RailroadDiagram.render(ir)
        assertTrue(result.contains("?"), "Expected '?' quantifier in: $result")
    }

    @Test
    fun lazyQuantifier() {
        val ir = Regex.Rep(RegexRepetition(
            inner = Regex.Literal("a"),
            lower = 1,
            upper = null,
            greedy = false,
        ))
        val result = RailroadDiagram.render(ir)
        assertTrue(result.contains("lazy"), "Expected 'lazy' in: $result")
    }

    @Test
    fun namedGroup() {
        val ir = Regex.Group(RegexGroup(
            parts = listOf(Regex.Literal("x")),
            kind = RegexGroupKind.Named("foo", 1),
        ))
        val result = RailroadDiagram.render(ir)
        assertTrue(result.contains("foo"), "Expected group name 'foo' in: $result")
    }

    @Test
    fun lookahead() {
        val ir = Regex.Look(RegexLookaround(
            kind = LookaroundKind.Ahead,
            inner = Regex.Literal("x"),
        ))
        val result = RailroadDiagram.render(ir)
        assertTrue(result.contains("(?="), "Expected '(?=' in: $result")
    }

    @Test
    fun namedBackreference() {
        val ir = Regex.Ref(RegexReference.Named("foo"))
        val result = RailroadDiagram.render(ir)
        assertTrue(result.contains("\\k<foo>"), "Expected '\\k<foo>' in: $result")
    }

    @Test
    fun recursionNode() {
        val result = RailroadDiagram.render(Regex.Recursion)
        assertTrue(result.contains("(?R)"), "Expected '(?R)' in: $result")
    }

    @Test
    fun graphemeNode() {
        val result = RailroadDiagram.render(Regex.Grapheme)
        assertTrue(result.contains("Grapheme"), "Expected 'Grapheme' in: $result")
    }

    @Test
    fun threeWayAlternation() {
        val ir = Regex.Alt(RegexAlternation(listOf(
            Regex.Literal("a"),
            Regex.Literal("b"),
            Regex.Literal("c"),
        )))
        val result = RailroadDiagram.render(ir)
        assertTrue(result.contains("a"), "Expected 'a' in: $result")
        assertTrue(result.contains("b"), "Expected 'b' in: $result")
        assertTrue(result.contains("c"), "Expected 'c' in: $result")
        assertTrue(result.contains("\u251c"), "Expected middle branch char in: $result")
    }

    @Test
    fun charRange() {
        val ir = Regex.CharSet(RegexCharSet(
            items = listOf(RegexCharSetItem.Range('a', 'z')),
            negative = false,
        ))
        val result = RailroadDiagram.render(ir)
        assertTrue(result.contains("a-z"), "Expected 'a-z' in: $result")
    }
}
