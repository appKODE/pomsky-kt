package ru.kode.pomskykt.analysis

import ru.kode.pomskykt.regex.Regex
import ru.kode.pomskykt.regex.RegexAlternation
import ru.kode.pomskykt.regex.RegexCharSet
import ru.kode.pomskykt.regex.RegexCharSetItem
import ru.kode.pomskykt.regex.RegexGroup
import ru.kode.pomskykt.regex.RegexGroupKind
import ru.kode.pomskykt.regex.RegexRepetition
import ru.kode.pomskykt.regex.RegexShorthand
import kotlin.test.Test
import kotlin.test.assertTrue

class TestGeneratorTest {

    @Test
    fun literalGeneratesExactMatch() {
        val ir = Regex.Literal("hello")
        val result = TestGenerator.generate(ir)
        assertTrue(result.matching.contains("hello"))
        assertTrue(result.nonMatching.isNotEmpty())
    }

    @Test
    fun digitCharSetGeneratesDigits() {
        val ir = Regex.CharSet(
            RegexCharSet(
                items = listOf(RegexCharSetItem.Shorthand(RegexShorthand.Digit)),
                negative = false,
            )
        )
        val result = TestGenerator.generate(ir)
        assertTrue(result.matching.all { it.length == 1 && it[0].isDigit() })
    }

    @Test
    fun rangeCharSetGeneratesInRange() {
        val ir = Regex.CharSet(
            RegexCharSet(
                items = listOf(RegexCharSetItem.Range('a', 'z')),
                negative = false,
            )
        )
        val result = TestGenerator.generate(ir)
        assertTrue(result.matching.all { it.length == 1 && it[0] in 'a'..'z' })
        assertTrue(result.nonMatching.all { it.length == 1 && it[0] !in 'a'..'z' })
    }

    @Test
    fun plusRepetitionGeneratesMultiple() {
        val inner = Regex.CharSet(
            RegexCharSet(
                items = listOf(RegexCharSetItem.Shorthand(RegexShorthand.Digit)),
                negative = false,
            )
        )
        val ir = Regex.Rep(RegexRepetition(inner = inner, lower = 1, upper = null, greedy = true))
        val result = TestGenerator.generate(ir)
        assertTrue(result.matching.any { it.length > 1 })
        assertTrue(result.nonMatching.contains(""))
    }

    @Test
    fun exactRepetitionGeneratesCorrectLength() {
        val inner = Regex.CharSet(
            RegexCharSet(
                items = listOf(RegexCharSetItem.Range('A', 'Z')),
                negative = false,
            )
        )
        val ir = Regex.Rep(RegexRepetition(inner = inner, lower = 3, upper = 3, greedy = true))
        val result = TestGenerator.generate(ir)
        assertTrue(result.matching.any { it.length == 3 })
    }

    @Test
    fun alternationCoversAllBranches() {
        val ir = Regex.Alt(
            RegexAlternation(
                alternatives = listOf(
                    Regex.Literal("cat"),
                    Regex.Literal("dog"),
                    Regex.Literal("bird"),
                )
            )
        )
        val result = TestGenerator.generate(ir)
        assertTrue(result.matching.contains("cat"))
        assertTrue(result.matching.contains("dog"))
        assertTrue(result.matching.contains("bird"))
    }

    @Test
    fun sequenceConcatenatesParts() {
        val ir = Regex.Sequence(
            parts = listOf(
                Regex.Literal("ab"),
                Regex.Literal("cd"),
            )
        )
        val result = TestGenerator.generate(ir)
        assertTrue(result.matching.contains("abcd"))
    }

    @Test
    fun maxExamplesLimitsOutput() {
        val ir = Regex.Alt(
            RegexAlternation(
                alternatives = (1..20).map { Regex.Literal("opt$it") },
            )
        )
        val result = TestGenerator.generate(ir, TestGeneratorOptions(maxExamples = 3))
        assertTrue(result.matching.size <= 3)
    }

    @Test
    fun negatedCharSetSwapsResults() {
        val ir = Regex.CharSet(
            RegexCharSet(
                items = listOf(RegexCharSetItem.Shorthand(RegexShorthand.Digit)),
                negative = true,
            )
        )
        val result = TestGenerator.generate(ir)
        assertTrue(result.matching.all { it.length == 1 && !it[0].isDigit() })
    }

    @Test
    fun starRepetitionIncludesEmpty() {
        val inner = Regex.Literal("x")
        val ir = Regex.Rep(RegexRepetition(inner = inner, lower = 0, upper = null, greedy = true))
        val result = TestGenerator.generate(ir)
        assertTrue(result.matching.contains(""))
        assertTrue(result.matching.any { it.isNotEmpty() })
    }

    @Test
    fun optionalRepetitionIncludesEmptyAndOne() {
        val inner = Regex.Literal("y")
        val ir = Regex.Rep(RegexRepetition(inner = inner, lower = 0, upper = 1, greedy = true))
        val result = TestGenerator.generate(ir)
        assertTrue(result.matching.contains(""))
        assertTrue(result.matching.contains("y"))
    }

    @Test
    fun dotGeneratesSingleChar() {
        val result = TestGenerator.generate(Regex.Dot)
        assertTrue(result.matching.contains("x"))
    }

    @Test
    fun graphemeGeneratesSingleChar() {
        val result = TestGenerator.generate(Regex.Grapheme)
        assertTrue(result.matching.contains("a"))
    }

    @Test
    fun groupIsTransparent() {
        val ir = Regex.Group(
            RegexGroup(
                parts = listOf(Regex.Literal("abc")),
                kind = RegexGroupKind.Numbered(1),
            )
        )
        val result = TestGenerator.generate(ir)
        assertTrue(result.matching.contains("abc"))
    }

    @Test
    fun charSetItemCharGeneratesThatChar() {
        val ir = Regex.CharSet(
            RegexCharSet(
                items = listOf(RegexCharSetItem.Char('Q')),
                negative = false,
            )
        )
        val result = TestGenerator.generate(ir)
        assertTrue(result.matching.contains("Q"))
    }

    @Test
    fun wordShorthandGeneratesWordChars() {
        val ir = Regex.CharSet(
            RegexCharSet(
                items = listOf(RegexCharSetItem.Shorthand(RegexShorthand.Word)),
                negative = false,
            )
        )
        val result = TestGenerator.generate(ir)
        assertTrue(result.matching.containsAll(listOf("a", "Z", "0")))
    }

    @Test
    fun exactRepetitionNonMatchingHasWrongLength() {
        val inner = Regex.Literal("x")
        val ir = Regex.Rep(RegexRepetition(inner = inner, lower = 3, upper = 3, greedy = true))
        val result = TestGenerator.generate(ir)
        assertTrue(result.matching.contains("xxx"))
        // Non-matching should have length != 3
        assertTrue(result.nonMatching.all { it.count { c -> c == 'x' } != 3 || it.length != 3 })
    }
}
