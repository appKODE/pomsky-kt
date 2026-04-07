package ru.kode.pomskykt.decompiler

import ru.kode.pomskykt.Expr
import ru.kode.pomskykt.options.CompileOptions
import ru.kode.pomskykt.options.RegexFlavor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class DecompilerTest {

    // --- Basic literals ---

    @Test
    fun decompileLiteral() {
        val result = decompile("hello")
        assertEquals("'hello'", result)
    }

    @Test
    fun decompileSingleChar() {
        val result = decompile("a")
        assertEquals("'a'", result)
    }

    @Test
    fun decompileEscapedMetachars() {
        val result = decompile("\\.")
        assertEquals("'.'", result)
    }

    @Test
    fun decompileEmptyString() {
        val result = Decompiler.decompile("")
        assertEquals("", result.pomsky)
        assertNull(result.error)
    }

    // --- Character classes ---

    @Test
    fun decompileDigitShorthand() {
        val result = decompile("\\d")
        assertEquals("[digit]", result)
    }

    @Test
    fun decompileWordShorthand() {
        val result = decompile("\\w")
        assertEquals("[word]", result)
    }

    @Test
    fun decompileSpaceShorthand() {
        val result = decompile("\\s")
        assertEquals("[space]", result)
    }

    @Test
    fun decompileNegatedDigit() {
        val result = decompile("\\D")
        assertEquals("![digit]", result)
    }

    @Test
    fun decompileCharRange() {
        val result = decompile("[a-z]")
        assertEquals("['a'-'z']", result)
    }

    @Test
    fun decompileCharClass() {
        val result = decompile("[abc]")
        assertEquals("['a' 'b' 'c']", result)
    }

    @Test
    fun decompileNegatedCharClass() {
        val result = decompile("[^a-z]")
        assertEquals("!['a'-'z']", result)
    }

    @Test
    fun decompileMixedCharClass() {
        val result = decompile("[a-zA-Z0-9_]")
        assertEquals("['a'-'z' 'A'-'Z' '0'-'9' '_']", result)
    }

    // --- Anchors ---

    @Test
    fun decompileStartAnchor() {
        val result = decompile("^abc")
        assertEquals("Start 'abc'", result)
    }

    @Test
    fun decompileEndAnchor() {
        val result = decompile("abc$")
        assertEquals("'abc' End", result)
    }

    @Test
    fun decompileWordBoundary() {
        val result = decompile("\\bword\\b")
        assertEquals("% 'word' %", result)
    }

    // --- Quantifiers ---

    @Test
    fun decompileStar() {
        val result = decompile("a*")
        assertEquals("'a'*", result)
    }

    @Test
    fun decompilePlus() {
        val result = decompile("a+")
        assertEquals("'a'+", result)
    }

    @Test
    fun decompileOptional() {
        val result = decompile("a?")
        assertEquals("'a'?", result)
    }

    @Test
    fun decompileRepeatExact() {
        val result = decompile("a{3}")
        assertEquals("'a'{3}", result)
    }

    @Test
    fun decompileRepeatRange() {
        val result = decompile("a{2,5}")
        assertEquals("'a'{2,5}", result)
    }

    @Test
    fun decompileRepeatMin() {
        val result = decompile("a{2,}")
        assertEquals("'a'{2,}", result)
    }

    @Test
    fun decompileLazyPlus() {
        val result = decompile("a+?")
        assertEquals("'a'+ lazy", result)
    }

    @Test
    fun decompileLazyStar() {
        val result = decompile("a*?")
        assertEquals("'a'* lazy", result)
    }

    // --- Groups ---

    @Test
    fun decompileCapturingGroup() {
        val result = decompile("(abc)")
        assertEquals(":('abc')", result)
    }

    @Test
    fun decompileNonCapturingGroup() {
        val result = decompile("(?:abc)")
        assertEquals("'abc'", result)
    }

    @Test
    fun decompileNamedGroup() {
        val result = decompile("(?<name>abc)")
        assertEquals(":name('abc')", result)
    }

    // --- Alternation ---

    @Test
    fun decompileAlternation() {
        val result = decompile("a|b|c")
        assertEquals("'a' | 'b' | 'c'", result)
    }

    @Test
    fun decompileAlternationInGroup() {
        val result = decompile("(?:foo|bar)")
        assertEquals("('foo' | 'bar')", result)
    }

    // --- Lookaround ---

    @Test
    fun decompileLookahead() {
        val result = decompile("(?=abc)")
        assertEquals(">> 'abc'", result)
    }

    @Test
    fun decompileNegativeLookahead() {
        val result = decompile("(?!abc)")
        assertEquals("!>> 'abc'", result)
    }

    @Test
    fun decompileLookbehind() {
        val result = decompile("(?<=abc)")
        assertEquals("<< 'abc'", result)
    }

    @Test
    fun decompileNegativeLookbehind() {
        val result = decompile("(?<!abc)")
        assertEquals("!<< 'abc'", result)
    }

    // --- Dot ---

    @Test
    fun decompileDot() {
        val result = decompile(".")
        assertEquals(".", result)
    }

    @Test
    fun decompileDotStar() {
        val result = decompile(".*")
        assertEquals(".*", result)
    }

    // --- Backreferences ---

    @Test
    fun decompileNamedBackref() {
        val result = decompile("\\k<name>")
        assertEquals("::name", result)
    }

    // --- Complex patterns ---

    @Test
    fun decompileEmailLike() {
        val result = decompile("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$")
        assertNotNull(result)
    }

    @Test
    fun decompileKarandashTitlePattern() {
        val result = decompile("^((\\[MAPS-\\d+\\])+)?(\\[(ios|android|core)\\]) .+")
        assertNotNull(result)
    }

    // --- Round-trip tests ---

    @Test
    fun roundTripSimpleLiteral() {
        assertRoundTrip("'hello'")
    }

    @Test
    fun roundTripDigitPlus() {
        assertRoundTrip("[digit]+")
    }

    @Test
    fun roundTripStartEnd() {
        assertRoundTrip("^ 'test' $")
    }

    @Test
    fun roundTripAlternation() {
        assertRoundTrip("'foo' | 'bar' | 'baz'")
    }

    @Test
    fun roundTripCharRange() {
        assertRoundTrip("['a'-'z']+")
    }

    @Test
    fun roundTripOptional() {
        assertRoundTrip("'prefix'? 'body'")
    }

    @Test
    fun roundTripLookahead() {
        assertRoundTrip(">> 'test'")
    }

    @Test
    fun roundTripCapturingGroup() {
        assertRoundTrip(":('hello')")
    }

    // --- Error handling ---

    @Test
    fun decompileReturnsErrorOnInvalidEscape() {
        // A well-formed regex should not produce an error
        val result = Decompiler.decompile("abc", RegexFlavor.Java)
        assertNull(result.error)
        assertNotNull(result.pomsky)
    }

    // --- Helpers ---

    private fun decompile(regex: String): String {
        val result = Decompiler.decompile(regex, RegexFlavor.Java)
        assertNull(result.error, "Decompile error: ${result.error}")
        return result.pomsky!!
    }

    /**
     * Compile Pomsky to regex, decompile back, recompile, and verify the regex output matches.
     */
    private fun assertRoundTrip(pomskySource: String) {
        val (regex1, diags1) = Expr.parseAndCompile(pomskySource, CompileOptions(flavor = RegexFlavor.Java))
        assertNotNull(regex1, "Failed to compile original Pomsky: ${diags1.map { it.msg }}")

        val decompiled = Decompiler.decompile(regex1, RegexFlavor.Java)
        assertNotNull(decompiled.pomsky, "Failed to decompile: ${decompiled.error}")

        val (regex2, diags2) = Expr.parseAndCompile(decompiled.pomsky!!, CompileOptions(flavor = RegexFlavor.Java))
        assertNotNull(regex2, "Failed to recompile decompiled Pomsky '${decompiled.pomsky}': ${diags2.map { it.msg }}")

        assertEquals(regex1, regex2, "Round-trip mismatch:\n  original pomsky: $pomskySource\n  regex: $regex1\n  decompiled: ${decompiled.pomsky}\n  recompiled: $regex2")
    }
}
