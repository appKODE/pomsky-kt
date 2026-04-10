package ru.kode.pomskykt.analysis

import ru.kode.pomskykt.Expr
import ru.kode.pomskykt.diagnose.Severity
import ru.kode.pomskykt.options.CompileOptions
import ru.kode.pomskykt.options.RegexFlavor

/**
 * Result of testing a single pattern against a single test string across multiple flavors.
 *
 * @param pattern the original Pomsky source pattern
 * @param testString the string tested against each flavor's compiled regex
 * @param flavorResults map of flavor to whether the test string matched that flavor's compiled regex
 * @param mismatch true if not all flavors agreed on the match result
 */
data class FuzzResult(
    val pattern: String,
    val testString: String,
    val flavorResults: Map<RegexFlavor, Boolean>,
    val mismatch: Boolean,
)

/**
 * Aggregated report from a fuzz run.
 *
 * @param totalPatterns number of Pomsky patterns submitted for fuzzing
 * @param totalTests total number of (pattern, testString) pairs evaluated
 * @param mismatches list of [FuzzResult] entries where flavors disagreed
 * @param compileFailures count of compile failures per flavor
 */
data class FuzzReport(
    val totalPatterns: Int,
    val totalTests: Int,
    val mismatches: List<FuzzResult>,
    val compileFailures: Map<RegexFlavor, Int>,
)

/**
 * Cross-flavor pattern fuzzer for Pomsky expressions.
 *
 * Compiles each Pomsky pattern to multiple regex flavors, then tests a set of
 * candidate strings against each compiled regex (using [kotlin.text.Regex]) to
 * detect behavioral mismatches across flavors.
 *
 * Because the `lib` module does not depend on the decompiler, test strings are
 * derived from literals embedded in the Pomsky source plus a fixed set of common
 * probe strings.
 */
object PatternFuzzer {

    /** Fixed probe strings used for every pattern. */
    private val DEFAULT_PROBES = listOf(
        "",
        "a",
        "0",
        "Z",
        " ",
        "hello",
        "test123",
        "abc def",
        "\n",
        "!@#",
    )

    /**
     * Fuzz [patterns] across [flavors].
     *
     * For each pattern the fuzzer:
     * 1. Compiles the Pomsky source to a regex string for every requested flavor.
     * 2. Derives test strings from the pattern's literal content combined with
     *    a fixed probe set.
     * 3. Evaluates each test string against each flavor's regex and records
     *    any disagreements.
     *
     * @param patterns list of Pomsky source expressions
     * @param flavors flavors to compare (need at least 2 for mismatch detection)
     * @param maxTestStrings upper bound on test strings per pattern
     * @return a [FuzzReport] summarising the run
     */
    fun fuzz(
        patterns: List<String>,
        flavors: List<RegexFlavor> = listOf(RegexFlavor.Java, RegexFlavor.Pcre, RegexFlavor.JavaScript),
        maxTestStrings: Int = 10,
    ): FuzzReport {
        val mismatches = mutableListOf<FuzzResult>()
        val compileFailures = mutableMapOf<RegexFlavor, Int>()
        var totalTests = 0

        for (pattern in patterns) {
            // 1. Compile to each flavor
            val compiled = mutableMapOf<RegexFlavor, String>()
            for (flavor in flavors) {
                val (result, diags, _) = Expr.parseAndCompile(
                    pattern,
                    CompileOptions(flavor = flavor),
                )
                if (result != null && diags.none { it.severity == Severity.Error }) {
                    compiled[flavor] = result
                } else {
                    compileFailures[flavor] = (compileFailures[flavor] ?: 0) + 1
                }
            }

            if (compiled.size < 2) continue // need at least 2 flavors to compare

            // 2. Generate test strings from the pattern's literal content + default probes
            val testStrings = buildTestStrings(pattern, maxTestStrings)

            // 3. Test each string against each flavor's compiled regex
            for (testString in testStrings) {
                totalTests++
                val results = mutableMapOf<RegexFlavor, Boolean>()
                for ((flavor, regexStr) in compiled) {
                    results[flavor] = try {
                        Regex(regexStr).containsMatchIn(testString)
                    } catch (_: Exception) {
                        false // regex not supported on this KMP platform
                    }
                }

                // Check if all flavors agree
                val allValues = results.values.toSet()
                if (allValues.size > 1) {
                    mismatches.add(
                        FuzzResult(
                            pattern = pattern,
                            testString = testString,
                            flavorResults = results,
                            mismatch = true,
                        )
                    )
                }
            }
        }

        return FuzzReport(
            totalPatterns = patterns.size,
            totalTests = totalTests,
            mismatches = mismatches,
            compileFailures = compileFailures,
        )
    }

    // -- Test string generation ------------------------------------------------

    /**
     * Build test strings for a given Pomsky pattern by extracting quoted literals
     * from the source and combining them with the default probe set.
     */
    private fun buildTestStrings(pattern: String, maxTestStrings: Int): List<String> {
        val extracted = extractLiterals(pattern)
        return (extracted + DEFAULT_PROBES).distinct().take(maxTestStrings)
    }

    /**
     * Extract quoted string literals (`'...'` and `"..."`) from the Pomsky source.
     * Also extracts bare identifiers that look like they could be test-worthy
     * (e.g., character class names turned into sample chars).
     */
    private fun extractLiterals(pattern: String): List<String> {
        val literals = mutableListOf<String>()
        var i = 0
        while (i < pattern.length) {
            val ch = pattern[i]
            if (ch == '\'' || ch == '"') {
                val closing = pattern.indexOf(ch, i + 1)
                if (closing > i) {
                    val content = pattern.substring(i + 1, closing)
                    if (content.isNotEmpty()) {
                        literals.add(content)
                    }
                    i = closing + 1
                    continue
                }
            }
            i++
        }
        return literals
    }
}
