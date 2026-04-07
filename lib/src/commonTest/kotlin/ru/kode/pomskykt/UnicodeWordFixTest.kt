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
    fun wordPcreNoPolyfill() {
        val (result, _) = compileOk("[word]", RegexFlavor.Pcre)
        assertEquals("\\w", result)
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
