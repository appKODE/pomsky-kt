package ru.kode.pomskykt.analysis

import ru.kode.pomskykt.regex.Regex
import ru.kode.pomskykt.regex.RegexAlternation
import ru.kode.pomskykt.regex.RegexCharSet
import ru.kode.pomskykt.regex.RegexCharSetItem
import ru.kode.pomskykt.regex.RegexCompoundCharSet
import ru.kode.pomskykt.regex.RegexGroup
import ru.kode.pomskykt.regex.RegexRepetition
import ru.kode.pomskykt.regex.RegexShorthand

/**
 * Generated test strings: examples that should match the pattern
 * and examples that should not.
 */
data class GeneratedTests(
    val matching: List<String>,
    val nonMatching: List<String>,
)

/**
 * Options controlling test string generation.
 *
 * @param maxExamples maximum number of matching/non-matching examples to return
 * @param unboundedRepeatSamples repeat counts to use for unbounded quantifiers (e.g. `+`, `*`)
 * @param coverAllBranches if true, generate at least one sample per alternation branch
 */
data class TestGeneratorOptions(
    val maxExamples: Int = 10,
    val unboundedRepeatSamples: List<Int> = listOf(1, 3),
    val coverAllBranches: Boolean = true,
)

/**
 * Walks the [Regex] IR tree and produces matching and non-matching sample strings.
 *
 * This is a best-effort generator: it handles common node types and produces
 * reasonable samples, but does not guarantee that generated strings will
 * actually match/not-match the final compiled regex (lookarounds, backreferences,
 * and recursion are approximated with placeholders).
 */
object TestGenerator {

    /**
     * Generate sample matching and non-matching strings for the given [regex] IR.
     */
    fun generate(regex: Regex, options: TestGeneratorOptions = TestGeneratorOptions()): GeneratedTests {
        val matching = generateMatching(regex, options).distinct().take(options.maxExamples)
        val nonMatching = generateNonMatching(regex, options).distinct().take(options.maxExamples)
        return GeneratedTests(matching, nonMatching)
    }

    // -- Matching generation --------------------------------------------------

    private fun generateMatching(regex: Regex, options: TestGeneratorOptions): List<String> {
        return when (regex) {
            is Regex.Literal -> listOf(regex.content)
            is Regex.Dot -> listOf("x")
            is Regex.Grapheme -> listOf("a")
            is Regex.CharSet -> generateMatchingCharSet(regex.set)
            is Regex.CompoundCharSet -> generateMatchingCompoundCharSet(regex.set)
            is Regex.Sequence -> generateMatchingSequence(regex.parts, options)
            is Regex.Alt -> generateMatchingAlt(regex.alternation, options)
            is Regex.Rep -> generateMatchingRep(regex.repetition, options)
            is Regex.Group -> generateMatchingGroup(regex.group, options)
            is Regex.Bound -> listOf("") // zero-width, contributes no text
            is Regex.Look -> listOf("") // zero-width assertion
            is Regex.Ref -> listOf("ref")
            is Regex.Recursion -> listOf("...")
            is Regex.Unescaped -> listOf(regex.content)
            is Regex.ModeGroup -> generateMatching(regex.inner, options)
        }
    }

    private fun generateMatchingCharSet(set: RegexCharSet): List<String> {
        val samples = generateSamplesFromCharSet(set.items)
        return if (set.negative) {
            // Negated set: matching chars are those NOT in the set
            generateNonMatchingSamplesForCharSetItems(set.items)
        } else {
            samples
        }
    }

    private fun generateMatchingCompoundCharSet(set: RegexCompoundCharSet): List<String> {
        // Approximate: generate from the first inner set
        if (set.sets.isEmpty()) return listOf("a")
        val samples = generateMatchingCharSet(set.sets.first())
        return if (set.negative) {
            generateNonMatchingSamplesForCharSetItems(set.sets.first().items)
        } else {
            samples
        }
    }

    private fun generateMatchingSequence(parts: List<Regex>, options: TestGeneratorOptions): List<String> {
        if (parts.isEmpty()) return listOf("")
        // Take one sample from each part and concatenate
        val partSamples = parts.map { generateMatching(it, options) }
        // Generate a single concatenation using the first sample from each part
        val base = partSamples.map { it.firstOrNull() ?: "" }.joinToString("")
        return listOf(base)
    }

    private fun generateMatchingAlt(alternation: RegexAlternation, options: TestGeneratorOptions): List<String> {
        return if (options.coverAllBranches) {
            alternation.alternatives.flatMap { alt ->
                generateMatching(alt, options).take(1)
            }
        } else {
            generateMatching(alternation.alternatives.first(), options).take(1)
        }
    }

    private fun generateMatchingRep(repetition: RegexRepetition, options: TestGeneratorOptions): List<String> {
        val innerSamples = generateMatching(repetition.inner, options)
        val innerSample = innerSamples.firstOrNull() ?: ""
        val lower = repetition.lower
        val upper = repetition.upper

        return when {
            // Star: lower=0, upper=null
            lower == 0 && upper == null -> {
                listOf("") + options.unboundedRepeatSamples.map { count ->
                    innerSample.repeat(count)
                }
            }
            // Plus: lower=1, upper=null
            lower >= 1 && upper == null -> {
                options.unboundedRepeatSamples.map { count ->
                    innerSample.repeat(maxOf(lower, count))
                } + listOf(innerSample.repeat(lower + 2))
            }
            // Optional: lower=0, upper=1
            lower == 0 && upper == 1 -> {
                listOf("", innerSample)
            }
            // Exact: lower==upper
            upper != null && lower == upper -> {
                listOf(innerSample.repeat(lower))
            }
            // Range: lower..upper
            upper != null -> {
                listOf(
                    innerSample.repeat(lower),
                    innerSample.repeat(upper),
                )
            }
            else -> listOf(innerSample)
        }.distinct()
    }

    private fun generateMatchingGroup(group: RegexGroup, options: TestGeneratorOptions): List<String> {
        return generateMatchingSequence(group.parts, options)
    }

    // -- Non-matching generation ----------------------------------------------

    private fun generateNonMatching(regex: Regex, options: TestGeneratorOptions): List<String> {
        return when (regex) {
            is Regex.Literal -> {
                val results = mutableListOf<String>()
                if (regex.content.isNotEmpty()) {
                    results.add(regex.content + "x")
                    if (regex.content.length > 1) {
                        results.add(regex.content.dropLast(1))
                    }
                } else {
                    results.add("x")
                }
                results
            }
            is Regex.CharSet -> generateNonMatchingCharSet(regex.set)
            is Regex.Rep -> generateNonMatchingRep(regex.repetition, options)
            is Regex.Alt -> {
                // Hard to generate true non-matching for alternation; use a generic non-match
                listOf("~~~impossible~~~")
            }
            is Regex.Sequence -> {
                // Produce a truncated version
                val matching = generateMatchingSequence(regex.parts, options)
                matching.mapNotNull { s ->
                    if (s.length > 1) s.dropLast(1) else null
                }.ifEmpty { listOf("~~~impossible~~~") }
            }
            else -> emptyList()
        }
    }

    private fun generateNonMatchingCharSet(set: RegexCharSet): List<String> {
        return if (set.negative) {
            // Negated set: non-matching chars are those IN the set
            generateSamplesFromCharSet(set.items)
        } else {
            generateNonMatchingSamplesForCharSetItems(set.items)
        }
    }

    private fun generateNonMatchingRep(repetition: RegexRepetition, options: TestGeneratorOptions): List<String> {
        val innerSamples = generateMatching(repetition.inner, options)
        val innerSample = innerSamples.firstOrNull() ?: ""
        val lower = repetition.lower
        val upper = repetition.upper

        return when {
            // Plus: non-matching is empty string
            lower >= 1 && upper == null -> listOf("")
            // Exact: n-1 and n+1 repetitions
            upper != null && lower == upper && lower > 0 -> {
                val results = mutableListOf<String>()
                if (lower > 1) results.add(innerSample.repeat(lower - 1))
                results.add(innerSample.repeat(lower + 1))
                results
            }
            // Range: (n-1) and (m+1) repetitions
            upper != null && lower > 0 -> {
                val results = mutableListOf<String>()
                if (lower > 1) results.add(innerSample.repeat(lower - 1))
                results.add(innerSample.repeat(upper + 1))
                results
            }
            // lower=n, upper=null: (n-1) repetitions
            lower > 0 && upper == null -> {
                if (lower > 1) listOf(innerSample.repeat(lower - 1))
                else listOf("")
            }
            else -> emptyList()
        }
    }

    // -- Character set helpers ------------------------------------------------

    /**
     * Generate sample characters from a list of char set items.
     * Each item contributes one or more single-character strings.
     */
    private fun generateSamplesFromCharSet(items: List<RegexCharSetItem>): List<String> {
        return items.flatMap { item ->
            when (item) {
                is RegexCharSetItem.Char -> listOf(item.char.toString())
                is RegexCharSetItem.Range -> {
                    val results = mutableListOf(item.first.toString(), item.last.toString())
                    val mid = ((item.first.code + item.last.code) / 2).toChar()
                    if (mid != item.first && mid != item.last) {
                        results.add(mid.toString())
                    }
                    results
                }
                is RegexCharSetItem.Shorthand -> samplesForShorthand(item.shorthand)
                is RegexCharSetItem.Property -> listOf("a") // generic fallback
                is RegexCharSetItem.CodePoint -> {
                    val chars = StringBuilder()
                    chars.appendCodePoint(item.codePoint)
                    listOf(chars.toString())
                }
                is RegexCharSetItem.Literal -> {
                    if (item.content.isNotEmpty()) listOf(item.content[0].toString())
                    else listOf("a")
                }
            }
        }
    }

    /**
     * Generate characters that are NOT in the given char set items,
     * suitable for non-matching samples (or matching samples of negated sets).
     */
    private fun generateNonMatchingSamplesForCharSetItems(items: List<RegexCharSetItem>): List<String> {
        // Collect what IS in the set, then pick chars outside
        val results = mutableListOf<String>()
        for (item in items) {
            when (item) {
                is RegexCharSetItem.Range -> {
                    // Pick characters outside the range
                    if (item.first > 'A') results.add("A")
                    else results.add("~")
                    if (item.last < '0') results.add("0")
                    else if (item.last < 'z') results.add("~")
                }
                is RegexCharSetItem.Shorthand -> {
                    results.addAll(nonMatchingSamplesForShorthand(item.shorthand))
                }
                is RegexCharSetItem.Char -> {
                    val alt = if (item.char != 'x') "x" else "y"
                    results.add(alt)
                }
                is RegexCharSetItem.Property -> results.add("0")
                is RegexCharSetItem.CodePoint -> results.add("a")
                is RegexCharSetItem.Literal -> results.add("~")
            }
        }
        return results.ifEmpty { listOf("~") }
    }

    private fun samplesForShorthand(shorthand: RegexShorthand): List<String> {
        return when (shorthand) {
            RegexShorthand.Digit -> listOf("0", "5", "9")
            RegexShorthand.Word -> listOf("a", "Z", "0")
            RegexShorthand.Space -> listOf(" ")
            RegexShorthand.NotDigit -> listOf("a")
            RegexShorthand.NotWord -> listOf("!")
            RegexShorthand.NotSpace -> listOf("a")
            RegexShorthand.VertSpace -> listOf("\n")
            RegexShorthand.HorizSpace -> listOf("\t")
        }
    }

    private fun nonMatchingSamplesForShorthand(shorthand: RegexShorthand): List<String> {
        return when (shorthand) {
            RegexShorthand.Digit -> listOf("a")
            RegexShorthand.Word -> listOf("!")
            RegexShorthand.Space -> listOf("a")
            RegexShorthand.NotDigit -> listOf("0")
            RegexShorthand.NotWord -> listOf("a")
            RegexShorthand.NotSpace -> listOf(" ")
            RegexShorthand.VertSpace -> listOf("a")
            RegexShorthand.HorizSpace -> listOf("a")
        }
    }

    private fun StringBuilder.appendCodePoint(codePoint: Int) {
        if (codePoint <= 0xFFFF) {
            append(codePoint.toChar())
        } else {
            // Supplementary code point -> surrogate pair
            val high = ((codePoint - 0x10000) shr 10) + 0xD800
            val low = ((codePoint - 0x10000) and 0x3FF) + 0xDC00
            append(high.toChar())
            append(low.toChar())
        }
    }
}
