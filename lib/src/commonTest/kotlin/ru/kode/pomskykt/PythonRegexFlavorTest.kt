package ru.kode.pomskykt

import ru.kode.pomskykt.diagnose.Severity
import ru.kode.pomskykt.options.CompileOptions
import ru.kode.pomskykt.options.RegexFlavor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class PythonRegexFlavorTest {

    private val pythonRegexOptions = CompileOptions(flavor = RegexFlavor.PythonRegex)

    private fun compileOk(input: String): String {
        val (result, diags, _) = Expr.parseAndCompile(input, pythonRegexOptions)
        val errors = diags.filter { it.severity == Severity.Error }
        assertTrue(errors.isEmpty(), "Expected no errors for '$input', got: ${errors.map { it.msg }}")
        assertNotNull(result, "Expected result for '$input'")
        return result
    }

    private fun compileError(input: String): String {
        val (result, diags, _) = Expr.parseAndCompile(input, pythonRegexOptions)
        val errors = diags.filter { it.severity == Severity.Error }
        assertTrue(errors.isNotEmpty(), "Expected errors for '$input', got none. Result: $result")
        return errors.first().msg
    }

    @Test
    fun simpleLiteral() {
        assertEquals("hello", compileOk("'hello'"))
    }

    @Test
    fun unicodeCategorySupported() {
        // PythonRegex supports Unicode properties (unlike plain Python)
        val result = compileOk("[Letter]")
        assertEquals("\\p{L}", result)
    }

    @Test
    fun digitClass() {
        assertEquals("\\d+", compileOk("[digit]+"))
    }

    @Test
    fun namedGroup() {
        // PythonRegex uses (?P<name>...) like Python
        assertEquals("(?P<name>x)", compileOk(":name('x')"))
    }

    @Test
    fun atomicGroupNoError() {
        // PythonRegex supports atomic groups (unlike plain Python)
        val result = compileOk("atomic('test')")
        assertEquals("(?>test)", result)
    }

    @Test
    fun wordClass() {
        // PythonRegex with Unicode polyfills \w to Unicode properties
        val result = compileOk("[word]")
        assertEquals("[\\p{Alphabetic}\\p{M}\\p{Nd}\\p{Pc}]", result)
    }

    @Test
    fun spaceClass() {
        assertEquals("\\s", compileOk("[space]"))
    }

    @Test
    fun alternation() {
        assertEquals("hello|world", compileOk("'hello' | 'world'"))
    }

    @Test
    fun anchors() {
        assertEquals("^test\$", compileOk("^ 'test' $"))
    }

    @Test
    fun repetition() {
        assertEquals("a{2,5}", compileOk("'a'{2,5}"))
    }

    @Test
    fun lookahead() {
        assertEquals("(?=a)", compileOk(">> 'a'"))
    }

    @Test
    fun lookbehind() {
        assertEquals("(?<=a)", compileOk("<< 'a'"))
    }

    @Test
    fun unicodeScript() {
        val result = compileOk("[Latin]")
        assertEquals("\\p{Latin}", result)
    }

    @Test
    fun capturingGroup() {
        assertEquals("(a)", compileOk(":('a')"))
    }

    @Test
    fun recursionProducesError() {
        val msg = compileError("recursion")
        assertTrue(msg.contains("Unsupported", ignoreCase = true) || msg.contains("recursion", ignoreCase = true),
            "Expected recursion unsupported error, got: $msg")
    }
}
