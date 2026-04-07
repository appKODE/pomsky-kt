package ru.kode.pomskykt

import ru.kode.pomskykt.diagnose.Severity
import ru.kode.pomskykt.options.CompileOptions
import ru.kode.pomskykt.options.RegexFlavor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ModeModifierTest {

    private fun compileOk(
        input: String,
        flavor: RegexFlavor = RegexFlavor.Pcre,
    ): String {
        val (result, diags, _) = Expr.parseAndCompile(input, CompileOptions(flavor = flavor))
        val errors = diags.filter { it.severity == Severity.Error }
        assertTrue(errors.isEmpty(), "Expected no errors for '$input', got: ${errors.map { it.msg }}")
        assertNotNull(result, "Expected result for '$input'")
        return result
    }

    @Test
    fun enableIgnoreCase() {
        val result = compileOk("enable ignore_case; 'hello'")
        assertEquals("(?i:hello)", result)
    }

    @Test
    fun enableIgnoreCaseJava() {
        val result = compileOk("enable ignore_case; 'hello'", RegexFlavor.Java)
        assertEquals("(?i:hello)", result)
    }

    @Test
    fun enableIgnoreCaseRust() {
        val result = compileOk("enable ignore_case; 'hello'", RegexFlavor.Rust)
        assertEquals("(?i:hello)", result)
    }

    @Test
    fun enableMultiline() {
        val result = compileOk("enable multiline; ^ 'hello'")
        assertEquals("(?m:^hello)", result)
    }

    @Test
    fun enableMultilineJava() {
        val result = compileOk("enable multiline; ^ 'hello'", RegexFlavor.Java)
        assertEquals("(?m:^hello)", result)
    }

    @Test
    fun enableBothModifiers() {
        val result = compileOk("enable ignore_case; enable multiline; 'test'")
        assertEquals("(?i:(?m:test))", result)
    }

    @Test
    fun disableIgnoreCase() {
        val result = compileOk("disable ignore_case; 'test'")
        assertEquals("(?-i:test)", result)
    }

    @Test
    fun disableMultiline() {
        val result = compileOk("disable multiline; 'test'")
        assertEquals("(?-m:test)", result)
    }

    @Test
    fun scopedModifiers() {
        val result = compileOk("enable ignore_case; 'a' | (disable ignore_case; 'b')")
        assertEquals("(?i:a|(?-i:b))", result)
    }

    // --- single_line ---

    @Test
    fun enableSingleLine() {
        val result = compileOk("enable single_line; . 'test'")
        assertEquals("(?s:.test)", result)
    }

    @Test
    fun disableSingleLine() {
        val result = compileOk("disable single_line; 'test'")
        assertEquals("(?-s:test)", result)
    }

    // --- extended ---

    @Test
    fun enableExtended() {
        val result = compileOk("enable extended; 'hello'")
        assertEquals("(?x:hello)", result)
    }

    @Test
    fun disableExtended() {
        val result = compileOk("disable extended; 'hello'")
        assertEquals("(?-x:hello)", result)
    }

    // --- reuse_groups (PCRE only) ---

    @Test
    fun enableReuseGroupsPcre() {
        val result = compileOk("enable reuse_groups; :('a') | :('b')")
        assertEquals("(?J:(a)|(b))", result)
    }

    @Test
    fun reuseGroupsUnsupportedInJava() {
        val (result, diags, _) = Expr.parseAndCompile(
            "enable reuse_groups; 'test'",
            CompileOptions(flavor = RegexFlavor.Java),
        )
        val errors = diags.filter { it.severity == Severity.Error }
        assertTrue(errors.isNotEmpty(), "Expected error for reuse_groups in Java")
    }

    // --- ascii_line_breaks ---

    @Test
    fun enableAsciiLineBreaksPcre() {
        val result = compileOk("enable ascii_line_breaks; 'test'")
        assertEquals("(?d:test)", result)
    }

    @Test
    fun asciiLineBreaksUnsupportedInRust() {
        val (result, diags, _) = Expr.parseAndCompile(
            "enable ascii_line_breaks; 'test'",
            CompileOptions(flavor = RegexFlavor.Rust),
        )
        val errors = diags.filter { it.severity == Severity.Error }
        assertTrue(errors.isNotEmpty(), "Expected error for ascii_line_breaks in Rust")
    }

    // --- combined ---

    @Test
    fun combinedAllModifiers() {
        val result = compileOk("enable ignore_case; enable single_line; 'test'")
        assertEquals("(?i:(?s:test))", result)
    }

    @Test
    fun enableExtendedAndMultiline() {
        val result = compileOk("enable extended; enable multiline; ^ 'test'")
        assertEquals("(?x:(?m:^test))", result)
    }
}
