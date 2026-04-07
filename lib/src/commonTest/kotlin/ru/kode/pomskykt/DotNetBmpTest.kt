package ru.kode.pomskykt

import ru.kode.pomskykt.diagnose.Severity
import ru.kode.pomskykt.options.CompileOptions
import ru.kode.pomskykt.options.RegexFlavor
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DotNetBmpTest {

    @Test
    fun supplementaryCodePointCompilesForDotNet() {
        // U+1F600 (emoji) should compile fine for DotNet (positive match via surrogate pairs)
        val (result, diags, _) = Expr.parseAndCompile(
            "U+1F600",
            CompileOptions(flavor = RegexFlavor.DotNet),
        )
        assertNotNull(result, "Expected U+1F600 to compile for DotNet")
        assertTrue(
            diags.none { it.severity == Severity.Error },
            "Expected no errors for positive U+1F600 in DotNet",
        )
    }

    @Test
    fun negatedSupplementaryCodePointFailsForDotNet() {
        // !U+1F600 (negated emoji) should produce error for DotNet
        val (result, diags, _) = Expr.parseAndCompile(
            "!U+1F600",
            CompileOptions(flavor = RegexFlavor.DotNet),
        )
        assertNull(result, "Expected negated U+1F600 to fail for DotNet")
        assertTrue(
            diags.any { it.severity == Severity.Error },
            "Expected error for negated supplementary code point in DotNet",
        )
        assertTrue(
            diags.any { it.msg.contains("U+FFFF") || it.msg.contains("negated") || it.msg.contains(".NET") },
            "Expected error message to mention BMP limitation",
        )
    }

    @Test
    fun negatedBmpCharIsValidForDotNet() {
        // !['a'] should NOT produce error for DotNet (BMP char is fine)
        val (result, diags, _) = Expr.parseAndCompile(
            "!'a'",
            CompileOptions(flavor = RegexFlavor.DotNet),
        )
        assertNotNull(result, "Expected negated 'a' to compile for DotNet")
        assertTrue(
            diags.none { it.severity == Severity.Error },
            "Expected no errors for negated BMP char in DotNet",
        )
    }

    @Test
    fun supplementaryCodePointCompilesForJava() {
        // U+1F600 should compile fine for Java (no limitation)
        val (result, diags, _) = Expr.parseAndCompile(
            "U+1F600",
            CompileOptions(flavor = RegexFlavor.Java),
        )
        assertNotNull(result, "Expected U+1F600 to compile for Java")
        assertTrue(
            diags.none { it.severity == Severity.Error },
            "Expected no errors for U+1F600 in Java",
        )
    }

    @Test
    fun negatedSupplementaryCodePointWorksForJava() {
        // !U+1F600 should work fine for Java (no BMP limitation)
        val (result, diags, _) = Expr.parseAndCompile(
            "!U+1F600",
            CompileOptions(flavor = RegexFlavor.Java),
        )
        assertNotNull(result, "Expected negated U+1F600 to compile for Java")
        assertTrue(
            diags.none { it.severity == Severity.Error },
            "Expected no errors for negated supplementary code point in Java",
        )
    }
}
