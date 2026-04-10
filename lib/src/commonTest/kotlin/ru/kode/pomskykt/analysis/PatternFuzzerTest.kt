package ru.kode.pomskykt.analysis

import ru.kode.pomskykt.options.RegexFlavor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PatternFuzzerTest {

    @Test
    fun simpleLiteralNoMismatches() {
        val report = PatternFuzzer.fuzz(
            patterns = listOf("'hello'"),
            flavors = listOf(RegexFlavor.Java, RegexFlavor.Pcre, RegexFlavor.JavaScript),
        )
        assertEquals(1, report.totalPatterns)
        assertTrue(report.mismatches.isEmpty(), "Expected no mismatches for simple literal")
    }

    @Test
    fun multiplePatternsProcessed() {
        val report = PatternFuzzer.fuzz(
            patterns = listOf("'a'", "'b'", "[digit]+"),
            flavors = listOf(RegexFlavor.Java, RegexFlavor.JavaScript),
        )
        assertEquals(3, report.totalPatterns)
        assertTrue(report.totalTests > 0)
    }

    @Test
    fun unsupportedFeatureRecordsCompileFailure() {
        // Recursion is not supported in JavaScript
        val report = PatternFuzzer.fuzz(
            patterns = listOf("recursion"),
            flavors = listOf(RegexFlavor.Java, RegexFlavor.JavaScript),
        )
        assertTrue(
            report.compileFailures.isNotEmpty(),
            "Expected compile failure for unsupported feature"
        )
    }

    @Test
    fun emptyPatternListProducesEmptyReport() {
        val report = PatternFuzzer.fuzz(
            patterns = emptyList(),
            flavors = listOf(RegexFlavor.Java),
        )
        assertEquals(0, report.totalPatterns)
        assertEquals(0, report.totalTests)
    }

    @Test
    fun singleFlavorSkipsComparison() {
        val report = PatternFuzzer.fuzz(
            patterns = listOf("'hello'"),
            flavors = listOf(RegexFlavor.Java),
        )
        // With only 1 flavor, compiled.size < 2, so no tests are run
        assertEquals(0, report.totalTests)
        assertTrue(report.mismatches.isEmpty())
    }

    @Test
    fun extractedLiteralsAreUsedAsTestStrings() {
        val report = PatternFuzzer.fuzz(
            patterns = listOf("'world'"),
            flavors = listOf(RegexFlavor.Java, RegexFlavor.Pcre),
        )
        // "world" is extracted from the pattern; it should match in all flavors
        assertTrue(report.totalTests > 0)
        assertTrue(report.mismatches.isEmpty(), "Extracted literal should match consistently")
    }

    @Test
    fun compileFailureCountsAccumulate() {
        // Test with multiple patterns that fail for a flavor
        val report = PatternFuzzer.fuzz(
            patterns = listOf("recursion", "recursion"),
            flavors = listOf(RegexFlavor.Pcre, RegexFlavor.JavaScript),
        )
        val jsFailures = report.compileFailures[RegexFlavor.JavaScript] ?: 0
        assertTrue(jsFailures >= 1, "Expected JavaScript compile failures to accumulate")
    }

    @Test
    fun fuzzReportFieldsAreConsistent() {
        val patterns = listOf("'abc'", "[word]+")
        val report = PatternFuzzer.fuzz(
            patterns = patterns,
            flavors = listOf(RegexFlavor.Java, RegexFlavor.Pcre),
        )
        assertEquals(patterns.size, report.totalPatterns)
        // Every mismatch should have mismatch=true
        for (result in report.mismatches) {
            assertTrue(result.mismatch)
        }
    }
}
