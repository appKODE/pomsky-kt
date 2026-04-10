package ru.kode.pomskykt

import ru.kode.pomskykt.diagnose.Severity
import ru.kode.pomskykt.options.CompileOptions
import ru.kode.pomskykt.options.RegexFlavor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CharSetIntersectionPolyfillTest {

    // --- Basic range intersection ---

    @Test
    fun rangeIntersectionDotNet() {
        // ['a'-'z'] & ['d'-'p'] -> [d-p]
        val (result, _) = compileOk("['a'-'z'] & ['d'-'p']", RegexFlavor.DotNet)
        assertEquals("[d-p]", result)
    }

    @Test
    fun rangeIntersectionPython() {
        val (result, _) = compileOk("['a'-'z'] & ['d'-'p']", RegexFlavor.Python)
        assertEquals("[d-p]", result)
    }

    @Test
    fun rangeIntersectionRE2() {
        val (result, _) = compileOk("['a'-'z'] & ['d'-'p']", RegexFlavor.RE2)
        assertEquals("[d-p]", result)
    }

    @Test
    fun rangeIntersectionPosix() {
        val (result, _) = compileOk("['a'-'z'] & ['d'-'p']", RegexFlavor.PosixExtended)
        assertEquals("[d-p]", result)
    }

    // --- Shorthand intersection ---

    @Test
    fun digitIntersectRange() {
        // [digit] & ['0'-'5'] -> [0-5]
        val (result, _) = compileOk("[digit] & ['0'-'5']", RegexFlavor.DotNet)
        assertEquals("[0-5]", result)
    }

    @Test
    fun wordIntersectDigitPython() {
        // [word] & [digit] -> [0-9] (digit is a subset of word)
        // Use Python flavor where [word] stays as \w shorthand (not expanded to Unicode properties)
        val (result, _) = compileOk("[word] & [digit]", RegexFlavor.Python)
        assertEquals("[0-9]", result)
    }

    // --- Multi-way intersection ---

    @Test
    fun threeWayIntersection() {
        // ['a'-'z'] & ['d'-'k'] & ['f'-'h'] -> [f-h]
        val (result, _) = compileOk("['a'-'z'] & ['d'-'k'] & ['f'-'h']", RegexFlavor.DotNet)
        assertEquals("[f-h]", result)
    }

    // --- Negated set intersection ---

    @Test
    fun negatedIntersection() {
        // [!digit] & [word] -> word minus digits = [A-Z_a-z]
        // Use Python flavor where [word] stays as \w shorthand (not expanded to Unicode properties)
        val (result, _) = compileOk("[!digit] & [word]", RegexFlavor.Python)
        assertNotNull(result)
        val regex = Regex(result)
        assertTrue(regex.matches("a"))
        assertTrue(regex.matches("Z"))
        assertTrue(regex.matches("_"))
        assertTrue(!regex.matches("0"))
        assertTrue(!regex.matches("9"))
    }

    // --- Java flavor still uses && syntax ---

    @Test
    fun javaUsesNativeSyntax() {
        val (result, _) = compileOk("['a'-'z'] & ['d'-'p']", RegexFlavor.Java)
        assertTrue(result.contains("&&"), "Java should use && syntax, got: $result")
    }

    // --- Property intersection falls back to error ---

    @Test
    fun propertyIntersectionFails() {
        // [Letter] & ['a'-'z'] -> can't expand Unicode property statically
        val (result, diags) = compile("[Letter] & ['a'-'z']", RegexFlavor.DotNet)
        assertNull(result)
        assertTrue(diags.any { it.severity == Severity.Error })
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

    private fun compile(
        input: String,
        flavor: RegexFlavor,
    ): Pair<String?, List<ru.kode.pomskykt.diagnose.Diagnostic>> {
        val (result, diags, _) = Expr.parseAndCompile(input, CompileOptions(flavor = flavor))
        return result to diags
    }
}
