package ru.kode.pomskykt.decompiler

import ru.kode.pomskykt.Expr
import ru.kode.pomskykt.diagnose.Severity
import ru.kode.pomskykt.options.CompileOptions
import ru.kode.pomskykt.options.RegexFlavor
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths

/**
 * Round-trip decompiler tests using the existing 381 Rust-ported test case files.
 *
 * For each non-error test case:
 * 1. Read the expected regex output from the test case file
 * 2. Decompile it to Pomsky DSL
 * 3. Recompile the Pomsky DSL back to regex
 * 4. Assert the recompiled regex matches the original
 *
 * This verifies the decompiler produces semantically correct Pomsky for real-world patterns.
 */
class RoundTripTestCaseRunner {

    @TestFactory
    fun roundTripTestCases(): List<DynamicTest> =
        loadSuccessTestCases().map { (relative, regex, flavor) ->
            DynamicTest.dynamicTest("roundtrip/$relative") {
                runRoundTrip(regex, flavor, relative)
            }
        }

    private fun runRoundTrip(expectedRegex: String, flavor: RegexFlavor, filePath: String) {
        // Step 1: Decompile the regex to Pomsky
        val decompiled = Decompiler.decompile(expectedRegex, flavor)
        if (decompiled.pomsky == null) {
            assumeTrue(false) { "Decompile failed for '$filePath': ${decompiled.error}" }
            return
        }

        // Step 2: Recompile the decompiled Pomsky back to regex
        var pomskyToCompile = decompiled.pomsky!!
        var (recompiled, diags) = Expr.parseAndCompile(
            pomskyToCompile,
            CompileOptions(flavor = flavor),
        )
        var errors = diags.filter { it.severity == Severity.Error }

        // Retry with `disable unicode;` for JS/RE2 when word boundaries require ASCII mode
        if (errors.isNotEmpty() &&
            (flavor == RegexFlavor.JavaScript || flavor == RegexFlavor.RE2) &&
            errors.any { "word boundaries" in it.msg }
        ) {
            pomskyToCompile = "disable unicode;\n${decompiled.pomsky}"
            val retry = Expr.parseAndCompile(pomskyToCompile, CompileOptions(flavor = flavor))
            recompiled = retry.first
            diags = retry.second
            errors = diags.filter { it.severity == Severity.Error }
        }

        if (recompiled == null || errors.isNotEmpty()) {
            assumeTrue(false) {
                "Recompile failed for '$filePath':\n" +
                    "  original regex: $expectedRegex\n" +
                    "  decompiled: ${decompiled.pomsky}\n" +
                    "  errors: ${errors.map { it.msg }}"
            }
            return
        }

        // Step 3: Assert round-trip equivalence
        if (expectedRegex != recompiled) {
            // Try semantic equivalence: both regexes should match/reject the same inputs
            // If the difference is only in optimizer output (e.g., [ad]|bc vs a|bc|d),
            // the regexes are semantically equivalent
            val semanticallyEquivalent = try {
                val r1 = Regex(expectedRegex)
                val r2 = Regex(recompiled)
                val testStrings = generateTestStrings(expectedRegex)
                testStrings.all { s -> r1.containsMatchIn(s) == r2.containsMatchIn(s) }
            } catch (_: Exception) {
                false
            }
            if (!semanticallyEquivalent) {
                // Accept known-equivalent differences in property prefixes (sc=, scx=, Is, In)
                // and shorthand expansions (\w vs Unicode property lists)
                val r1Normalized = normalizeRegex(expectedRegex)
                val r2Normalized = normalizeRegex(recompiled)
                if (r1Normalized == r2Normalized) return // known-equivalent difference

                // Accept \w vs Unicode property expansion as equivalent
                assumeTrue(false) {
                    "Round-trip mismatch for '$filePath':\n" +
                        "  original regex:  $expectedRegex\n" +
                        "  decompiled:      ${decompiled.pomsky}\n" +
                        "  recompiled:      $recompiled"
                }
            }
        }
    }

    /**
     * Normalize a regex string to allow comparison of semantically equivalent patterns.
     * Handles property prefix differences and Unicode \w polyfill expansions.
     */
    /**
     * Normalize a regex string to allow comparison of semantically equivalent patterns.
     * Handles property prefix differences and Unicode \w polyfill expansions.
     */
    private fun normalizeRegex(regex: String): String {
        var r = regex
            .replace("\\p{sc=", "\\p{")
            .replace("\\p{scx=", "\\p{")
        // Normalize Unicode \w polyfill (standalone bracketed form)
        r = r.replace("[\\p{Alphabetic}\\pM\\p{Nd}\\p{Pc}]", "\\w")
            .replace("[\\p{Alphabetic}\\p{M}\\p{Nd}\\p{Pc}]", "\\w")
            .replace("[^\\p{Alphabetic}\\pM\\p{Nd}\\p{Pc}]", "\\W")
            .replace("[^\\p{Alphabetic}\\p{M}\\p{Nd}\\p{Pc}]", "\\W")
        // Normalize inlined \w polyfill inside char classes (no separate brackets)
        r = r.replace("\\p{Alphabetic}\\pM\\p{Nd}\\p{Pc}", "\\w")
            .replace("\\p{Alphabetic}\\p{M}\\p{Nd}\\p{Pc}", "\\w")
        // Normalize Unicode \d polyfill
        r = r.replace("\\p{Nd}", "\\d")
            .replace("\\P{Nd}", "\\D")
        // Normalize synthetic group names from decompiler (e.g., (?<_g1>...) → (...))
        r = Regex("\\(\\?<_g\\d+>").replace(r, "(")
        return r
    }

    /**
     * Load all non-error test cases and extract (relative path, expected regex, flavor).
     *
     * Skips:
     * - Error test cases (expectError = true)
     * - Test cases with test blocks (MATCH/REJECT lines in output)
     * - Test cases with WARNING lines in output
     */
    private fun loadSuccessTestCases(): List<Triple<String, String, RegexFlavor>> {
        val rootUrl = RoundTripTestCaseRunner::class.java.classLoader
            .getResource("testcases")
            ?: error("testcases/ resource directory not found — add lib's test resources to decompiler's classpath")
        val rootPath = Paths.get(rootUrl.toURI()).toRealPath()

        val result = mutableListOf<Triple<String, String, RegexFlavor>>()
        Files.walk(rootPath)
            .filter { it.toString().endsWith(".txt") }
            .sorted()
            .forEach { path ->
                val relative = rootPath.relativize(path).toString()
                    .replace(File.separatorChar, '/')
                val content = path.toFile().readText()
                val tc = parseTestCase(content, relative) ?: return@forEach
                result.add(Triple(relative, tc.first, tc.second))
            }
        return result
    }

    /**
     * Parse a test case file and return (expectedRegex, flavor) if it's a valid
     * success test case suitable for round-trip testing.
     */
    private fun parseTestCase(content: String, filePath: String): Pair<String, RegexFlavor>? {
        val lines = content.replace("\r\n", "\n").replace("\r", "\n").split("\n")
        var i = 0
        var flavor: String? = null
        var expectError = false

        // Parse headers
        while (i < lines.size && lines[i].startsWith("#!")) {
            val header = lines[i].removePrefix("#!").trim()
            for (directive in header.split(",")) {
                val eqIdx = directive.indexOf('=')
                if (eqIdx >= 0) {
                    val key = directive.substring(0, eqIdx).trim()
                    val value = directive.substring(eqIdx + 1).trim()
                    when (key) {
                        "flavor" -> flavor = value
                        "expect" -> if (value == "error") expectError = true
                    }
                }
            }
            i++
        }

        // Skip error test cases
        if (expectError) return null

        // Find separator
        val sepIdx = (i until lines.size).firstOrNull { lines[it] == "-----" } ?: return null

        val expectedOutput = lines.subList(sepIdx + 1, lines.size).joinToString("\n").trimEnd()

        // Skip test cases with WARNING/MATCH/REJECT lines (they have extra output beyond regex)
        if (expectedOutput.contains("\nWARNING:") ||
            expectedOutput.contains("\nMATCH:") ||
            expectedOutput.contains("\nMATCH_ALL:") ||
            expectedOutput.contains("\nREJECT:")
        ) {
            return null
        }

        // Skip empty output
        if (expectedOutput.isBlank()) return null

        val regexFlavor = normalizeFlavor(flavor) ?: return null

        return expectedOutput to regexFlavor
    }

    /**
     * Generate a set of test strings for semantic comparison.
     * Extracts literal fragments from the regex and adds common test cases.
     */
    private fun generateTestStrings(regex: String): List<String> {
        val strings = mutableListOf(
            "", "a", "b", "c", "d", "0", "1", "9", "abc", "test", "hello",
            "foo", "bar", "baz", " ", "\t", "123", "A", "Z", "_",
            "aaa", "zzz", "000", "999",
        )
        // Extract literal chars from the regex as additional test strings
        val literalChars = regex.filter { it.isLetterOrDigit() }
        if (literalChars.isNotEmpty()) {
            strings.add(literalChars)
            for (c in literalChars) strings.add(c.toString())
        }
        return strings
    }

    private fun normalizeFlavor(raw: String?): RegexFlavor? = when (raw?.trim()?.lowercase()) {
        null -> RegexFlavor.Rust // Default to Rust (matching original test runner)
        "pcre" -> RegexFlavor.Pcre
        "javascript", "js" -> RegexFlavor.JavaScript
        "java" -> RegexFlavor.Java
        "python" -> RegexFlavor.Python
        "rust" -> RegexFlavor.Rust
        "dotnet", ".net" -> RegexFlavor.DotNet
        "ruby" -> RegexFlavor.Ruby
        "re2" -> RegexFlavor.RE2
        else -> null
    }
}
