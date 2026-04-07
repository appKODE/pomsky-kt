package ru.kode.pomskykt

import ru.kode.pomskykt.diagnose.Severity
import ru.kode.pomskykt.options.CompileOptions
import ru.kode.pomskykt.options.RegexFlavor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class AutoAtomizeTest {

    @Test
    fun digitPlusLiteral() {
        // [digit]+ 'abc' -> digits followed by literal 'a' — disjoint, should atomize
        val result = compileAtomized("[digit]+ 'abc'", RegexFlavor.Pcre)
        assertEquals("(?>\\d+)abc", result)
    }

    @Test
    fun wordPlusColonDigitPlus() {
        // [word]+ ':' [digit]+ -> word followed by ':' is disjoint, so atomize first rep.
        // The trailing [digit]+ has no following element, so it is NOT atomized.
        val result = compileAtomized("[word]+ ':' [digit]+", RegexFlavor.Pcre)
        assertEquals("(?>\\w+):\\d+", result)
    }

    @Test
    fun literalOverlapNotAtomized() {
        // 'a'+ 'a' -> chars overlap, should NOT atomize
        val result = compileAtomized("'a'+ 'a'", RegexFlavor.Pcre)
        assertEquals("a+a", result)
    }

    @Test
    fun dotNotAtomized() {
        // .+ 'x' -> dot matches everything, should NOT atomize
        val result = compileAtomized(".+ 'x'", RegexFlavor.Pcre)
        assertEquals(".+x", result)
    }

    @Test
    fun disabledByDefault() {
        // Without autoAtomize flag, no atomic groups added
        val result = compileNoAtomize("[digit]+ 'abc'", RegexFlavor.Pcre)
        assertEquals("\\d+abc", result)
    }

    @Test
    fun javaFlavorSupported() {
        val result = compileAtomized("[digit]+ 'abc'", RegexFlavor.Java)
        assertEquals("(?>\\d+)abc", result)
    }

    @Test
    fun dotnetFlavorSupported() {
        val result = compileAtomized("[digit]+ 'abc'", RegexFlavor.DotNet)
        assertEquals("(?>\\d+)abc", result)
    }

    @Test
    fun jsFlavorNotAtomized() {
        // JavaScript does not support atomic groups — pass should be skipped
        val result = compileAtomized("[digit]+ 'abc'", RegexFlavor.JavaScript)
        // JS uses \p{Nd} for digit class, not \d
        assertEquals("\\p{Nd}+abc", result)
    }

    @Test
    fun pythonFlavorNotAtomized() {
        val result = compileAtomized("[digit]+ 'abc'", RegexFlavor.Python)
        assertEquals("\\d+abc", result)
    }

    @Test
    fun spacePlusDigit() {
        // [space]+ [digit] -> disjoint, should atomize
        val result = compileAtomized("[space]+ [digit]", RegexFlavor.Pcre)
        assertEquals("(?>\\s+)\\d", result)
    }

    @Test
    fun boundedRepetitionNotAtomized() {
        // [digit]{2,5} 'abc' -> bounded (upper != null), should NOT atomize
        val result = compileAtomized("[digit]{2,5} 'abc'", RegexFlavor.Pcre)
        assertEquals("\\d{2,5}abc", result)
    }

    @Test
    fun lazyRepetitionNotAtomized() {
        // enable lazy; [digit]+ 'abc' -> lazy (not greedy), should NOT atomize
        val result = compileAtomized("enable lazy; [digit]+ 'abc'", RegexFlavor.Pcre)
        assertEquals("\\d+?abc", result)
    }

    @Test
    fun nestedGroupAtomized() {
        // ([digit]+) 'x' inside a group — inner rep should atomize
        val result = compileAtomized("([digit]+ 'x')", RegexFlavor.Pcre)
        assertEquals("(?>\\d+)x", result)
    }

    // --- Helpers ---

    private fun compileAtomized(input: String, flavor: RegexFlavor): String {
        val options = CompileOptions(flavor = flavor, autoAtomize = true)
        val (result, diags, _) = Expr.parseAndCompile(input, options)
        val errors = diags.filter { it.severity == Severity.Error }
        assertTrue(errors.isEmpty(), "Expected no errors for '$input', got: ${errors.map { it.msg }}")
        assertNotNull(result, "Expected result for '$input'")
        return result
    }

    private fun compileNoAtomize(input: String, flavor: RegexFlavor): String {
        val options = CompileOptions(flavor = flavor, autoAtomize = false)
        val (result, diags, _) = Expr.parseAndCompile(input, options)
        val errors = diags.filter { it.severity == Severity.Error }
        assertTrue(errors.isEmpty(), "Expected no errors for '$input', got: ${errors.map { it.msg }}")
        assertNotNull(result, "Expected result for '$input'")
        return result
    }
}
