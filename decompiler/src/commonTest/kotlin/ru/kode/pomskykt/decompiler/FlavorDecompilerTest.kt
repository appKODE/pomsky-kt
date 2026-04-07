package ru.kode.pomskykt.decompiler

import ru.kode.pomskykt.Expr
import ru.kode.pomskykt.diagnose.Severity
import ru.kode.pomskykt.options.CompileOptions
import ru.kode.pomskykt.options.RegexFlavor
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertEquals

/**
 * Flavor-specific decompiler tests covering all 8 supported regex flavors.
 */
class FlavorDecompilerTest {

    // --- Java ---

    @Test
    fun javaNamedGroup() {
        val result = decompile("(?<name>abc)", RegexFlavor.Java)
        assertEquals(":name('abc')", result)
    }

    @Test
    fun javaUnicodeBraced() {
        val result = decompile("\\x{1F600}", RegexFlavor.Java)
        assertNotNull(result)
    }

    @Test
    fun javaRoundTrip() {
        assertRoundTrip("^ ['a'-'z']+ [digit]{2,4} $", RegexFlavor.Java)
    }

    // --- Rust ---

    @Test
    fun rustNamedGroup() {
        val result = decompile("(?P<name>abc)", RegexFlavor.Rust)
        assertEquals(":name('abc')", result)
    }

    @Test
    fun rustWordBoundaries() {
        val result = decompile("\\<foo\\>", RegexFlavor.Rust)
        assertEquals("<% 'foo' %>", result)
    }

    @Test
    fun rustRoundTrip() {
        assertRoundTrip("^ 'hello' [digit]+", RegexFlavor.Rust)
    }

    // --- PCRE ---

    @Test
    fun pcreNamedGroup() {
        val result = decompile("(?P<name>abc)", RegexFlavor.Pcre)
        assertEquals(":name('abc')", result)
    }

    @Test
    fun pcrePosixWordBoundary() {
        // [[:>:]] is a full char class containing only the POSIX word-end class
        // This is parsed as a boundary assertion
        val result = decompile("[[:>:]]", RegexFlavor.Pcre)
        assertEquals("%>", result)
    }

    @Test
    fun pcrePosixWordStart() {
        val result = decompile("[[:<:]]", RegexFlavor.Pcre)
        assertEquals("<%", result)
    }

    @Test
    fun pcreRoundTrip() {
        assertRoundTrip("'test' [digit]+", RegexFlavor.Pcre)
    }

    // --- JavaScript ---

    @Test
    fun jsNamedGroup() {
        val result = decompile("(?<name>abc)", RegexFlavor.JavaScript)
        assertEquals(":name('abc')", result)
    }

    @Test
    fun jsUnicodeEscape() {
        val result = decompile("\\u0041", RegexFlavor.JavaScript)
        assertEquals("'A'", result)
    }

    @Test
    fun jsRoundTrip() {
        assertRoundTrip("^ 'hello' $", RegexFlavor.JavaScript)
    }

    // --- Python ---

    @Test
    fun pythonNamedGroup() {
        val result = decompile("(?P<name>abc)", RegexFlavor.Python)
        assertEquals(":name('abc')", result)
    }

    @Test
    fun pythonNamedBackref() {
        val result = decompile("(?P<x>a)(?P=x)", RegexFlavor.Python)
        assertEquals(":x('a') ::x", result)
    }

    @Test
    fun pythonLongUnicode() {
        val result = decompile("\\U0001F600", RegexFlavor.Python)
        assertNotNull(result)
    }

    @Test
    fun pythonRoundTrip() {
        assertRoundTrip("^ 'test' [digit]+ $", RegexFlavor.Python)
    }

    // --- .NET ---

    @Test
    fun dotnetNamedGroup() {
        val result = decompile("(?<name>abc)", RegexFlavor.DotNet)
        assertEquals(":name('abc')", result)
    }

    @Test
    fun dotnetSurrogatePair() {
        // U+1F600 = surrogate pair \uD83D\uDE00
        val result = decompile("\\uD83D\\uDE00", RegexFlavor.DotNet)
        assertNotNull(result)
    }

    @Test
    fun dotnetRoundTrip() {
        assertRoundTrip("^ 'hello' $", RegexFlavor.DotNet)
    }

    // --- Ruby ---

    @Test
    fun rubyNamedGroup() {
        val result = decompile("(?<name>abc)", RegexFlavor.Ruby)
        assertEquals(":name('abc')", result)
    }

    @Test
    fun rubyNamedBackref() {
        val result = decompile("\\k<name>", RegexFlavor.Ruby)
        assertEquals("::name", result)
    }

    @Test
    fun rubyQuoteBackref() {
        val result = decompile("\\k'name'", RegexFlavor.Ruby)
        assertEquals("::name", result)
    }

    @Test
    fun rubyRoundTrip() {
        assertRoundTrip("^ ['a'-'z']+ $", RegexFlavor.Ruby)
    }

    // --- RE2 ---

    @Test
    fun re2BasicPattern() {
        val result = decompile("^[a-z]+$", RegexFlavor.RE2)
        assertEquals("Start ['a'-'z']+ End", result)
    }

    @Test
    fun re2RoundTrip() {
        assertRoundTrip("^ 'hello' ['0'-'9']+ $", RegexFlavor.RE2)
    }

    // --- Cross-flavor consistency ---

    @Test
    fun allFlavorsHandleBasicLiteral() {
        for (flavor in RegexFlavor.entries) {
            val result = Decompiler.decompile("hello", flavor)
            assertNull(result.error, "Error for flavor $flavor: ${result.error}")
            assertEquals("'hello'", result.pomsky, "Mismatch for flavor $flavor")
        }
    }

    @Test
    fun allFlavorsHandleCharClass() {
        for (flavor in RegexFlavor.entries) {
            val result = Decompiler.decompile("[a-z]", flavor)
            assertNull(result.error, "Error for flavor $flavor: ${result.error}")
            assertEquals("['a'-'z']", result.pomsky, "Mismatch for flavor $flavor")
        }
    }

    @Test
    fun allFlavorsHandleAnchors() {
        for (flavor in RegexFlavor.entries) {
            val result = Decompiler.decompile("^abc$", flavor)
            assertNull(result.error, "Error for flavor $flavor: ${result.error}")
            assertEquals("Start 'abc' End", result.pomsky, "Mismatch for flavor $flavor")
        }
    }

    // --- Helpers ---

    private fun decompile(regex: String, flavor: RegexFlavor): String {
        val result = Decompiler.decompile(regex, flavor)
        assertNull(result.error, "Decompile error: ${result.error}")
        return result.pomsky!!
    }

    private fun assertRoundTrip(pomskySource: String, flavor: RegexFlavor) {
        val (regex1, diags1) = Expr.parseAndCompile(pomskySource, CompileOptions(flavor = flavor))
        assertNotNull(regex1, "Failed to compile Pomsky: ${diags1.map { it.msg }}")

        val decompiled = Decompiler.decompile(regex1, flavor)
        assertNotNull(decompiled.pomsky, "Failed to decompile: ${decompiled.error}")

        val (regex2, diags2) = Expr.parseAndCompile(decompiled.pomsky!!, CompileOptions(flavor = flavor))
        assertNotNull(regex2, "Failed to recompile '${decompiled.pomsky}': ${diags2.map { it.msg }}")

        assertEquals(regex1, regex2,
            "Round-trip mismatch ($flavor):\n" +
                "  pomsky: $pomskySource\n  regex: $regex1\n" +
                "  decompiled: ${decompiled.pomsky}\n  recompiled: $regex2")
    }
}
