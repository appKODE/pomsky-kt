package ru.kode.pomskykt

import ru.kode.pomskykt.diagnose.Severity
import ru.kode.pomskykt.options.CompileOptions
import ru.kode.pomskykt.options.RegexFlavor
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ForwardRefValidationTest {

    private fun compile(
        input: String,
        flavor: RegexFlavor,
    ): Pair<String?, List<ru.kode.pomskykt.diagnose.Diagnostic>> {
        val (result, diags, _) = Expr.parseAndCompile(input, CompileOptions(flavor = flavor))
        return result to diags
    }

    @Test
    fun namedForwardRefInRustErrors() {
        // ::name is a reference to group :name, which is defined after the reference
        val (result, diags) = compile("::name :name('x')", RegexFlavor.Rust)
        assertNull(result, "Expected compilation to fail for forward reference in Rust")
        val errors = diags.filter { it.severity == Severity.Error }
        assertTrue(errors.any { "forward reference" in it.msg.lowercase() || "Unsupported" in it.msg },
            "Expected forward reference error, got: ${errors.map { it.msg }}")
    }

    @Test
    fun namedForwardRefInJavaSucceeds() {
        // Java supports forward references
        val (result, diags) = compile("::name :name('x')", RegexFlavor.Java)
        assertNotNull(result, "Expected compilation to succeed for forward reference in Java")
        val errors = diags.filter { it.severity == Severity.Error }
        assertTrue(errors.isEmpty(), "Expected no errors, got: ${errors.map { it.msg }}")
    }

    @Test
    fun namedBackRefInRustStillErrors() {
        // Backreference (not forward) should still fail in Rust (backrefs not supported)
        val (result, diags) = compile(":name('x') ::name", RegexFlavor.Rust)
        assertNull(result, "Expected compilation to fail for backreference in Rust")
        val errors = diags.filter { it.severity == Severity.Error }
        assertTrue(errors.any { "backreference" in it.msg.lowercase() || "Unsupported" in it.msg },
            "Expected backreference error, got: ${errors.map { it.msg }}")
    }

    @Test
    fun numberedForwardRefInRustErrors() {
        // Numbered forward reference should already error in Rust
        val (result, diags) = compile("::1 :('x')", RegexFlavor.Rust)
        assertNull(result, "Expected compilation to fail for numbered forward reference in Rust")
        val errors = diags.filter { it.severity == Severity.Error }
        assertTrue(errors.isNotEmpty(), "Expected errors for numbered forward ref in Rust")
    }
}
