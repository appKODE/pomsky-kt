package ru.kode.pomskykt.ffi

import ru.kode.pomskykt.Expr
import ru.kode.pomskykt.decompiler.Decompiler
import ru.kode.pomskykt.diagnose.Severity
import ru.kode.pomskykt.options.CompileOptions
import ru.kode.pomskykt.options.RegexFlavor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests the FFI logic without C pointers — validates the underlying
 * compilation, decompilation, and explanation work correctly.
 */
class FfiLogicTest {

    @Test
    fun compileSimpleExpression() {
        val (result, diags, _) = Expr.parseAndCompile(
            "'hello' | 'world'",
            CompileOptions(flavor = RegexFlavor.Java),
        )
        assertNotNull(result)
        assertTrue(diags.none { it.severity == Severity.Error })
        assertEquals("hello|world", result)
    }

    @Test
    fun compileWithError() {
        val (result, diags, _) = Expr.parseAndCompile(
            "atomic('test')",
            CompileOptions(flavor = RegexFlavor.JavaScript),
        )
        assertNull(result)
        assertTrue(diags.any { it.severity == Severity.Error })
    }

    @Test
    fun decompileRegex() {
        val result = Decompiler.decompile("^[a-z]+$", RegexFlavor.Java)
        assertNotNull(result.pomsky)
        assertTrue(result.error == null)
    }

    @Test
    fun explainRegex() {
        val result = Decompiler.explain("\\d+", RegexFlavor.Java)
        assertNotNull(result.explanation)
        assertTrue(result.explanation!!.contains("digit"))
    }

    @Test
    fun allFlavorsCompile() {
        val flavors = listOf(
            RegexFlavor.Pcre, RegexFlavor.Python, RegexFlavor.Java,
            RegexFlavor.JavaScript, RegexFlavor.DotNet, RegexFlavor.Ruby,
            RegexFlavor.Rust, RegexFlavor.RE2, RegexFlavor.PosixExtended,
            RegexFlavor.PythonRegex,
        )
        for (flavor in flavors) {
            val (result, diags, _) = Expr.parseAndCompile(
                "'hello'",
                CompileOptions(flavor = flavor),
            )
            assertNotNull(result, "Failed to compile for flavor $flavor")
            assertTrue(diags.none { it.severity == Severity.Error }, "Errors for flavor $flavor")
        }
    }

    @Test
    fun compileWithLintEnabled() {
        val (result, diags, _) = Expr.parseAndCompile(
            "'x'{1}",
            CompileOptions(flavor = RegexFlavor.Java, lintEnabled = true),
        )
        assertNotNull(result)
        assertTrue(diags.any { it.severity == Severity.Warning })
    }

    @Test
    fun compileWithPatternLibrary() {
        val (result, diags, _) = Expr.parseAndCompile(
            "email",
            CompileOptions(flavor = RegexFlavor.Java, patternLibraryEnabled = true),
        )
        assertNotNull(result)
        assertTrue(diags.none { it.severity == Severity.Error })
    }

    @Test
    fun compileWithoutPatternLibrary() {
        val (result, diags, _) = Expr.parseAndCompile(
            "email",
            CompileOptions(flavor = RegexFlavor.Java, patternLibraryEnabled = false),
        )
        assertNull(result)
        assertTrue(diags.any { it.severity == Severity.Error })
    }
}
