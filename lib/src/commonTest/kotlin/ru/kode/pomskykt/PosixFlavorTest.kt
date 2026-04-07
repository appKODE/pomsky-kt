package ru.kode.pomskykt

import ru.kode.pomskykt.diagnose.Severity
import ru.kode.pomskykt.options.CompileOptions
import ru.kode.pomskykt.options.RegexFlavor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class PosixFlavorTest {

    private val posixOptions = CompileOptions(flavor = RegexFlavor.PosixExtended)

    private fun compileOk(input: String): String {
        val (result, diags, _) = Expr.parseAndCompile(input, posixOptions)
        val errors = diags.filter { it.severity == Severity.Error }
        assertTrue(errors.isEmpty(), "Expected no errors for '$input', got: ${errors.map { it.msg }}")
        assertNotNull(result, "Expected result for '$input'")
        return result
    }

    private fun compileError(input: String): String {
        val (result, diags, _) = Expr.parseAndCompile(input, posixOptions)
        val errors = diags.filter { it.severity == Severity.Error }
        assertTrue(errors.isNotEmpty(), "Expected errors for '$input', got none. Result: $result")
        return errors.first().msg
    }

    @Test
    fun simpleLiteral() {
        assertEquals("hello", compileOk("'hello'"))
    }

    @Test
    fun digitClass() {
        assertEquals("[[:digit:]]+", compileOk("[digit]+"))
    }

    @Test
    fun wordClass() {
        assertEquals("[_[:alnum:]]", compileOk("[word]"))
    }

    @Test
    fun spaceClass() {
        assertEquals("[[:space:]]", compileOk("[space]"))
    }

    @Test
    fun anchors() {
        assertEquals("^test\$", compileOk("^ 'test' $"))
    }

    @Test
    fun alternation() {
        // Single-char alternatives get optimized to a character class
        val result = compileOk("'a' | 'b'")
        assertTrue(result == "a|b" || result == "[ab]", "Got: $result")
    }

    @Test
    fun repetitionBraces() {
        assertEquals("a{2,5}", compileOk("'a'{2,5}"))
    }

    @Test
    fun capturingGroupBecomesPlainGroup() {
        // ERE has no non-capturing groups — non-capturing becomes plain (...)
        assertEquals("(group)", compileOk(":('group')"))
    }

    @Test
    fun lookaheadProducesError() {
        val msg = compileError(">> 'a'")
        assertTrue(msg.contains("lookahead", ignoreCase = true) || msg.contains("Unsupported", ignoreCase = true),
            "Expected lookahead unsupported error, got: $msg")
    }

    @Test
    fun lookbehindProducesError() {
        val msg = compileError("<< 'a'")
        assertTrue(msg.contains("lookahead", ignoreCase = true) || msg.contains("Unsupported", ignoreCase = true),
            "Expected lookbehind unsupported error, got: $msg")
    }

    @Test
    fun wordBoundaryProducesError() {
        val msg = compileError("%")
        assertTrue(msg.contains("boundar", ignoreCase = true) || msg.contains("Unsupported", ignoreCase = true),
            "Expected word boundary unsupported error, got: $msg")
    }

    @Test
    fun negatedDigitClass() {
        assertEquals("[^[:digit:]]", compileOk("[!digit]"))
    }

    @Test
    fun negatedWordClass() {
        assertEquals("[^_[:alnum:]]", compileOk("[!word]"))
    }

    @Test
    fun negatedSpaceClass() {
        assertEquals("[^[:space:]]", compileOk("[!space]"))
    }

    @Test
    fun dotWorks() {
        assertEquals(".", compileOk("."))
    }

    @Test
    fun starWorks() {
        assertEquals("a*", compileOk("'a'*"))
    }

    @Test
    fun plusWorks() {
        assertEquals("a+", compileOk("'a'+"))
    }

    @Test
    fun questionWorks() {
        assertEquals("a?", compileOk("'a'?"))
    }

    @Test
    fun unicodePropertyProducesError() {
        val msg = compileError("[Letter]")
        assertTrue(msg.contains("Unsupported", ignoreCase = true) || msg.contains("Unicode", ignoreCase = true),
            "Expected Unicode property unsupported error, got: $msg")
    }

    @Test
    fun recursionProducesError() {
        val msg = compileError("recursion")
        assertTrue(msg.contains("Unsupported", ignoreCase = true) || msg.contains("recursion", ignoreCase = true),
            "Expected recursion unsupported error, got: $msg")
    }

    @Test
    fun charRange() {
        assertEquals("[a-z]", compileOk("['a'-'z']"))
    }

    @Test
    fun letBinding() {
        val result = compileOk("let x = 'test'; x+")
        assertEquals("(test)+", result)
    }
}
