package ru.kode.pomskykt.format

import ru.kode.pomskykt.Expr
import ru.kode.pomskykt.options.CompileOptions
import ru.kode.pomskykt.options.RegexFlavor
import ru.kode.pomskykt.diagnose.Severity
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class FormatterTest {

    @Test
    fun simpleLiteral() {
        val result = PomskyFormatter.format("'hello'")
        assertEquals("'hello'", result)
    }

    @Test
    fun alternationSpacing() {
        val result = PomskyFormatter.format("'a'|'b'|'c'")
        assertEquals("'a' | 'b' | 'c'", result)
    }

    @Test
    fun alternationAlreadySpaced() {
        val result = PomskyFormatter.format("'a' | 'b' | 'c'")
        assertEquals("'a' | 'b' | 'c'", result)
    }

    @Test
    fun letBinding() {
        val result = PomskyFormatter.format("let x='a';x+")
        assertNotNull(result)
        assertTrue(result.contains("let x = 'a';"), "Expected 'let x = 'a';' in: $result")
        assertTrue(result.contains("x+"), "Expected 'x+' in: $result")
    }

    @Test
    fun enableStatement() {
        val result = PomskyFormatter.format("enable ignore_case;'test'")
        assertNotNull(result)
        assertTrue(result.contains("enable ignore_case;"), "Expected 'enable ignore_case;' in: $result")
        assertTrue(result.contains("'test'"), "Expected 'test' in: $result")
    }

    @Test
    fun nestedGroupStructure() {
        val result = PomskyFormatter.format(":name('a' 'b')")
        assertEquals(":name('a' 'b')", result)
    }

    @Test
    fun capturingGroup() {
        val result = PomskyFormatter.format(":('a')")
        assertEquals(":('a')", result)
    }

    @Test
    fun repetition() {
        val result = PomskyFormatter.format("[digit]+")
        assertEquals("[digit]+", result)
    }

    @Test
    fun repetitionBraces() {
        val result = PomskyFormatter.format("'x'{2,5}")
        assertEquals("'x'{2,5}", result)
    }

    @Test
    fun boundaries() {
        val result = PomskyFormatter.format("^ 'test' $")
        assertEquals("^ 'test' $", result)
    }

    @Test
    fun lookahead() {
        val result = PomskyFormatter.format(">> 'a'")
        assertEquals(">> 'a'", result)
    }

    @Test
    fun charClassMultiple() {
        val result = PomskyFormatter.format("['a'-'z' 'A'-'Z']")
        assertEquals("['a'-'z' 'A'-'Z']", result)
    }

    @Test
    fun roundTripCompile() {
        // Format then compile, verify the regex output matches original
        val originalSource = "'hello' | 'world'"
        val formatted = PomskyFormatter.format(originalSource)
        assertNotNull(formatted)

        val (originalRegex, origDiags, _) = Expr.parseAndCompile(originalSource, CompileOptions(flavor = RegexFlavor.Pcre))
        val (formattedRegex, fmtDiags, _) = Expr.parseAndCompile(formatted, CompileOptions(flavor = RegexFlavor.Pcre))

        assertTrue(origDiags.none { it.severity == Severity.Error })
        assertTrue(fmtDiags.none { it.severity == Severity.Error })
        assertEquals(originalRegex, formattedRegex)
    }

    @Test
    fun roundTripCompileComplex() {
        val source = "let d = [digit]; ^ d+ ('.' d+)? $"
        val formatted = PomskyFormatter.format(source)
        assertNotNull(formatted)

        val (origRegex, _, _) = Expr.parseAndCompile(source, CompileOptions(flavor = RegexFlavor.Pcre))
        val (fmtRegex, _, _) = Expr.parseAndCompile(formatted, CompileOptions(flavor = RegexFlavor.Pcre))
        assertEquals(origRegex, fmtRegex)
    }

    @Test
    fun dot() {
        val result = PomskyFormatter.format(".")
        assertEquals(".", result)
    }

    @Test
    fun negation() {
        val result = PomskyFormatter.format("![word]")
        assertEquals("![word]", result)
    }

    @Test
    fun invalidSourceReturnsNull() {
        val result = PomskyFormatter.format("'unclosed")
        assertEquals(null, result)
    }

    @Test
    fun wordBoundary() {
        val result = PomskyFormatter.format("%")
        assertEquals("%", result)
    }

    @Test
    fun atomicGroup() {
        val result = PomskyFormatter.format("atomic('test')")
        assertEquals("atomic('test')", result)
    }

    @Test
    fun lazyRepetition() {
        val result = PomskyFormatter.format("'a'+ lazy")
        assertNotNull(result)
        assertTrue(result.contains("'a'+ lazy"), "Expected lazy quantifier in: $result")
    }

    @Test
    fun disableStatement() {
        val result = PomskyFormatter.format("disable unicode;'test'")
        assertNotNull(result)
        assertTrue(result.contains("disable unicode;"), "Expected 'disable unicode;' in: $result")
    }
}
