package ru.kode.pomskykt

import ru.kode.pomskykt.diagnose.Severity
import ru.kode.pomskykt.options.CompileOptions
import ru.kode.pomskykt.options.RegexFlavor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ConditionalTest {

    @Test
    fun conditionalWithElse() {
        val (result, _) = compileOk("if (>> 'a') 'yes' else 'no'")
        assertEquals("(?=a)yes|(?!a)no", result)
    }

    @Test
    fun conditionalWithoutElse() {
        val (result, _) = compileOk("if (>> 'test') 'match'")
        assertEquals("(?=test)match", result)
    }

    @Test
    fun conditionalNegativeLookahead() {
        val (result, _) = compileOk("if (!>> 'x') 'a' else 'b'")
        assertEquals("(?!x)a|(?=x)b", result)
    }

    @Test
    fun conditionalJavaFlavor() {
        val (result, _) = compileOk("if (>> 'a') 'yes' else 'no'", RegexFlavor.Java)
        assertEquals("(?=a)yes|(?!a)no", result)
    }

    @Test
    fun conditionalPcreFlavor() {
        val (result, _) = compileOk("if (>> 'a') 'yes' else 'no'", RegexFlavor.Pcre)
        assertEquals("(?=a)yes|(?!a)no", result)
    }

    @Test
    fun conditionalNonLookaroundFails() {
        val (result, diags) = compile("if ('a') 'yes' else 'no'")
        assertTrue(diags.any { it.severity == Severity.Error })
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
}
