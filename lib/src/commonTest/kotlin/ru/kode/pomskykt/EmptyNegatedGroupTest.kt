package ru.kode.pomskykt

import ru.kode.pomskykt.diagnose.Severity
import ru.kode.pomskykt.options.CompileOptions
import ru.kode.pomskykt.options.RegexFlavor
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class EmptyNegatedGroupTest {

    @Test
    fun negatedWordDigitSpaceProducesError() {
        // word + digit + space together cover all code points,
        // so negating them produces an empty class
        val (result, diags, _) = Expr.parseAndCompile(
            "![word digit space]",
            CompileOptions(flavor = RegexFlavor.Rust),
        )
        assertNull(result, "Expected compilation to fail for ![word digit space]")
        assertTrue(
            diags.any { it.severity == Severity.Error },
            "Expected error diagnostic for empty negated class",
        )
    }

    @Test
    fun negatedWordDigitSpaceProducesErrorPcre() {
        // Same check for Pcre flavor
        val (result, diags, _) = Expr.parseAndCompile(
            "![word digit space]",
            CompileOptions(flavor = RegexFlavor.Pcre),
        )
        assertNull(result, "Expected compilation to fail for ![word digit space] in Pcre")
        assertTrue(
            diags.any { it.severity == Severity.Error },
            "Expected error diagnostic for empty negated class in Pcre",
        )
    }

    @Test
    fun negatedWordIsValid() {
        // ![word] is a valid negated class — it matches non-word characters
        val (result, diags, _) = Expr.parseAndCompile(
            "![word]",
            CompileOptions(flavor = RegexFlavor.Rust),
        )
        assertNotNull(result, "Expected ![word] to compile successfully")
        assertTrue(
            diags.none { it.severity == Severity.Error },
            "Expected no errors for ![word]",
        )
    }

    @Test
    fun negatedCharLiteralsIsValid() {
        // !['a' 'b'] is a valid negated class
        val (result, diags, _) = Expr.parseAndCompile(
            "!['a' 'b']",
            CompileOptions(flavor = RegexFlavor.Rust),
        )
        assertNotNull(result, "Expected !['a' 'b'] to compile successfully")
        assertTrue(
            diags.none { it.severity == Severity.Error },
            "Expected no errors for !['a' 'b']",
        )
    }

    @Test
    fun negatedComplementaryPairProducesError() {
        // ![word !word] — direct complementary pair, always empty
        val (result, diags, _) = Expr.parseAndCompile(
            "![word !word]",
            CompileOptions(flavor = RegexFlavor.Rust),
        )
        assertNull(result, "Expected compilation to fail for ![word !word]")
        assertTrue(
            diags.any { it.severity == Severity.Error },
            "Expected error for complementary pair negation",
        )
    }
}
