package ru.kode.pomskykt

import ru.kode.pomskykt.diagnose.Severity
import ru.kode.pomskykt.options.CompileOptions
import ru.kode.pomskykt.options.RegexFlavor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class UnicodeWordFixTest {

    @Test
    fun wordDotNetUnicodePolyfill() {
        val (result, _) = compileOk("[word]", RegexFlavor.DotNet)
        assertEquals("[\\p{Alphabetic}\\p{M}\\p{Nd}\\p{Pc}]", result)
    }

    @Test
    fun wordJavaNoPolyfill() {
        val (result, _) = compileOk("[word]", RegexFlavor.Java)
        assertEquals("\\w", result)
    }

    @Test
    fun wordRustNoPolyfill() {
        val (result, _) = compileOk("[word]", RegexFlavor.Rust)
        assertEquals("\\w", result)
    }

    @Test
    fun wordPcreUnicodePolyfill() {
        val (result, _) = compileOk("[word]", RegexFlavor.Pcre)
        // PCRE uses single-letter shorthand \pM instead of \p{M} for Mark category
        assertEquals("[\\p{Alphabetic}\\pM\\p{Nd}\\p{Pc}]", result)
    }

    @Test
    fun negatedWordPcrePolyfill() {
        val (result, _) = compileOk("[!word]", RegexFlavor.Pcre)
        assertEquals("[^\\p{Alphabetic}\\pM\\p{Nd}\\p{Pc}]", result)
    }

    @Test
    fun wordPcreAsciiMode() {
        val (result, _) = compileOk("disable unicode; [word]", RegexFlavor.Pcre)
        assertEquals("[0-9A-Z_a-z]", result)
    }

    @Test
    fun wordJavaScriptUnicodePolyfill() {
        val (result, _) = compileOk("[word]", RegexFlavor.JavaScript)
        assertEquals("[\\p{Alphabetic}\\p{M}\\p{Nd}\\p{Pc}]", result)
    }

    @Test
    fun negatedWordDotNetPolyfill() {
        val (result, _) = compileOk("[!word]", RegexFlavor.DotNet)
        assertEquals("[^\\p{Alphabetic}\\p{M}\\p{Nd}\\p{Pc}]", result)
    }

    @Test
    fun wordDotNetAsciiMode() {
        // With unicode disabled, should expand to ASCII
        val (result, _) = compileOk("disable unicode; [word]", RegexFlavor.DotNet)
        assertEquals("[0-9A-Z_a-z]", result)
    }

    @Test
    fun wordPythonUnicodeWarning() {
        val (result, diags) = compileOk("[word]", RegexFlavor.Python)
        assertNotNull(result)
        val warnings = diags.filter { it.severity == Severity.Warning }
        assertTrue(
            warnings.any { "python_regex" in it.msg },
            "Expected warning suggesting python_regex flavor, got: ${warnings.map { it.msg }}"
        )
    }

    @Test
    fun wordPythonRegexNoWarning() {
        val (result, diags) = compileOk("[word]", RegexFlavor.PythonRegex)
        assertNotNull(result)
        val warnings = diags.filter { it.severity == Severity.Warning }
        assertTrue(
            warnings.none { "python_regex" in it.msg },
            "Expected no python_regex warning, got: ${warnings.map { it.msg }}"
        )
    }

    // --- Helpers ---

    private fun compileOk(
        input: String,
        flavor: RegexFlavor,
    ): Pair<String, List<ru.kode.pomskykt.diagnose.Diagnostic>> {
        val (result, diags, _) = Expr.parseAndCompile(input, CompileOptions(flavor = flavor))
        val errors = diags.filter { it.severity == Severity.Error }
        assertTrue(errors.isEmpty(), "Expected no errors but got: ${errors.map { it.msg }}")
        assertNotNull(result, "Expected non-null result")
        return result to diags
    }
}
