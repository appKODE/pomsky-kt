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
    fun starRepetitionIncludesEmptyAndNonEmpty() {
        val inner = Regex.Literal("x")
        val ir = Regex.Rep(RegexRepetition(inner = inner, lower = 0, upper = null, greedy = true))
        val result = TestGenerator.generate(ir)
        assertTrue(result.matching.contains(""), "Should include empty string")
        assertTrue(result.matching.any { it.isNotEmpty() }, "Should include non-empty strings")
        // Non-empty samples should come before empty for better sequence combinations
        assertTrue(result.matching.first().isNotEmpty(), "First sample should be non-empty")
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
        assertTrue(result.matching.isNotEmpty(), "Dot should generate samples")
        assertTrue(result.matching.all { it.length == 1 }, "Dot samples should be single chars")
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
    fun sequenceCrossCombiresAlternationVariants() {
        // Pattern like (feat|fix|chore):.*
        val alt = Regex.Alt(
            RegexAlternation(
                alternatives = listOf(
                    Regex.Literal("feat"),
                    Regex.Literal("fix"),
                    Regex.Literal("chore"),
                )
            )
        )
        val ir = Regex.Sequence(
            parts = listOf(
                alt,
                Regex.Literal(":"),
            )
        )
        val result = TestGenerator.generate(ir)
        assertTrue(result.matching.contains("feat:"), "Should contain 'feat:', got: ${result.matching}")
        assertTrue(result.matching.contains("fix:"), "Should contain 'fix:', got: ${result.matching}")
        assertTrue(result.matching.contains("chore:"), "Should contain 'chore:', got: ${result.matching}")
    }

    @Test
    fun dotStarUsesDefaultSampleTexts() {
        val ir = Regex.Rep(RegexRepetition(inner = Regex.Dot, lower = 0, upper = null, greedy = true))
        val result = TestGenerator.generate(ir)
        assertTrue(result.matching.any { it.length > 1 }, "Should have multi-char samples, got: ${result.matching}")
        assertTrue(result.matching.contains(""), "Should include empty for star")
    }

    @Test
    fun dotPlusUsesDefaultSampleTexts() {
        val ir = Regex.Rep(RegexRepetition(inner = Regex.Dot, lower = 1, upper = null, greedy = true))
        val result = TestGenerator.generate(ir)
        assertTrue(result.matching.all { it.isNotEmpty() }, "Plus should not include empty, got: ${result.matching}")
        assertTrue(result.matching.any { it.length > 1 }, "Should have multi-char samples, got: ${result.matching}")
    }

    @Test
    fun customSampleTextsUsedForDotStar() {
        val ir = Regex.Sequence(
            parts = listOf(
                Regex.Literal("prefix:"),
                Regex.Rep(RegexRepetition(inner = Regex.Dot, lower = 0, upper = null, greedy = true)),
            )
        )
        val options = TestGeneratorOptions(sampleTexts = listOf("add login", "fix bug"))
        val result = TestGenerator.generate(ir, options)
        assertTrue(
            result.matching.any { it.startsWith("prefix:") && it.contains("add login") },
            "Should use custom sample text, got: ${result.matching}"
        )
    }

    @Test
    fun conventionalCommitPattern() {
        // (feat|fix|chore):.*
        val ir = Regex.Sequence(
            parts = listOf(
                Regex.Alt(
                    RegexAlternation(
                        alternatives = listOf(
                            Regex.Literal("feat"),
                            Regex.Literal("fix"),
                            Regex.Literal("chore"),
                        )
                    )
                ),
                Regex.Literal(":"),
                Regex.Rep(RegexRepetition(inner = Regex.Dot, lower = 0, upper = null, greedy = true)),
            )
        )
        val result = TestGenerator.generate(ir)
        // Should have varied prefixes, not just "feat:"
        val prefixes = result.matching.map { it.substringBefore(":") }.distinct()
        assertTrue(prefixes.size >= 3, "Should cover all alternation branches, got prefixes: $prefixes")
        // Should have non-empty content after colon for some
        assertTrue(
            result.matching.any { it.contains(":") && it.substringAfter(":").isNotEmpty() },
            "Should have content after colon, got: ${result.matching}"
        )
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
