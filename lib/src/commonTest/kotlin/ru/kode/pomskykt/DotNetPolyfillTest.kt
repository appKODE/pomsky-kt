package ru.kode.pomskykt

import ru.kode.pomskykt.diagnose.Severity
import ru.kode.pomskykt.options.CompileOptions
import ru.kode.pomskykt.options.RegexFlavor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class DotNetPolyfillTest {

    @Test
    fun dotNetCasedLetterPolyfill() {
        val (result, _) = compileOk("[LC]", RegexFlavor.DotNet)
        assertEquals("[\\p{Lu}\\p{Ll}\\p{Lt}]", result)
    }

    @Test
    fun dotNetNegatedCasedLetterPolyfill() {
        val (result, _) = compileOk("[!LC]", RegexFlavor.DotNet)
        assertEquals("[^\\p{Lu}\\p{Ll}\\p{Lt}]", result)
    }

    @Test
    fun javaCasedLetterNoPolyfill() {
        val (result, _) = compileOk("[LC]", RegexFlavor.Java)
        assertEquals("\\p{LC}", result)
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
