package ru.kode.pomskykt

import ru.kode.pomskykt.diagnose.Severity
import ru.kode.pomskykt.options.CompileOptions
import ru.kode.pomskykt.options.RegexFlavor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PermutationTest {

    @Test
    fun twoElements() {
        val (result, _) = compileOk("permute('a' 'b')")
        val regex = Regex(result)
        assertTrue(regex.matches("ab"))
        assertTrue(regex.matches("ba"))
        assertFalse(regex.matches("aa"))
        assertFalse(regex.matches("bb"))
    }

    @Test
    fun threeElements() {
        val (result, _) = compileOk("permute('x' 'y' 'z')")
        val regex = Regex(result)
        // All 6 orderings should match
        assertTrue(regex.matches("xyz"))
        assertTrue(regex.matches("xzy"))
        assertTrue(regex.matches("yxz"))
        assertTrue(regex.matches("yzx"))
        assertTrue(regex.matches("zxy"))
        assertTrue(regex.matches("zyx"))
        // Wrong content should not match
        assertFalse(regex.matches("xxx"))
        assertFalse(regex.matches("xy"))
    }

    @Test
    fun withCharClass() {
        val (result, _) = compileOk("permute([digit] 'a')", RegexFlavor.Java)
        val regex = Regex(result)
        assertTrue(regex.matches("0a"))
        assertTrue(regex.matches("a5"))
        assertFalse(regex.matches("aa"))
    }

    @Test
    fun singleElement() {
        val (result, _) = compileOk("permute('x')")
        assertEquals("x", result)
    }

    @Test
    fun tooLargePermutation() {
        val elements = (1..9).joinToString(" ") { "'$it'" }
        val (result, diags) = compile("permute($elements)")
        assertNull(result)
        assertTrue(diags.any { it.severity == Severity.Error && "Permutation" in it.msg })
    }

    @Test
    fun formatterRoundTrip() {
        val input = "permute('a' 'b' 'c')"
        val formatted = ru.kode.pomskykt.format.PomskyFormatter.format(input)
        assertNotNull(formatted)
        assertTrue(formatted.contains("permute("))
    }

    // --- Helpers ---

    private fun compileOk(
        input: String,
        flavor: RegexFlavor = RegexFlavor.Java,
    ): Pair<String, List<ru.kode.pomskykt.diagnose.Diagnostic>> {
        val (result, diags, _) = Expr.parseAndCompile(input, CompileOptions(flavor = flavor))
        val errors = diags.filter { it.severity == Severity.Error }
        assertTrue(errors.isEmpty(), "Expected no errors but got: ${errors.map { it.msg }}")
        assertNotNull(result, "Expected non-null result")
        return result to diags
    }

    private fun compile(
        input: String,
        flavor: RegexFlavor = RegexFlavor.Java,
    ): Pair<String?, List<ru.kode.pomskykt.diagnose.Diagnostic>> {
        val (result, diags, _) = Expr.parseAndCompile(input, CompileOptions(flavor = flavor))
        return result to diags
    }

    private fun assertFalse(value: Boolean, message: String? = null) {
        assertTrue(!value, message)
    }
}
