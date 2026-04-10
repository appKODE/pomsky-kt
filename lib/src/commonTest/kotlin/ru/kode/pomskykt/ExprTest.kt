package ru.kode.pomskykt

import ru.kode.pomskykt.diagnose.Severity
import ru.kode.pomskykt.options.CompileOptions
import ru.kode.pomskykt.options.RegexFlavor
import ru.kode.pomskykt.features.PomskyFeatures
import ru.kode.pomskykt.syntax.exprs.Rule
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ExprTest {
    @Test
    fun version() {
        assertEquals("0.15.0", Expr.VERSION)
    }

    // --- Parse tests ---

    @Test
    fun parseSimple() {
        val (expr, diags) = Expr.parse("'hello' | 'world'")
        assertNotNull(expr)
        assertTrue(diags.none { it.severity == Severity.Error })
    }

    @Test
    fun parseInvalid() {
        val (expr, diags) = Expr.parse("'unclosed")
        assertNull(expr)
        assertTrue(diags.any { it.severity == Severity.Error })
    }

    // --- End-to-end compile tests ---

    @Test
    fun compileLiteral() {
        val (result, diags) = compileOk("'hello'")
        assertEquals("hello", result)
    }

    @Test
    fun compileAlternation() {
        val (result, _) = compileOk("'hello' | 'world'")
        assertEquals("hello|world", result)
    }

    @Test
    fun compileStar() {
        val (result, _) = compileOk("'a'*")
        assertEquals("a*", result)
    }

    @Test
    fun compilePlus() {
        val (result, _) = compileOk("'a'+")
        assertEquals("a+", result)
    }

    @Test
    fun compileQuestion() {
        val (result, _) = compileOk("'a'?")
        assertEquals("a?", result)
    }

    @Test
    fun compileLazy() {
        val (result, _) = compileOk("'a'+ lazy")
        assertEquals("a+?", result)
    }

    @Test
    fun compileRepetitionBraces() {
        val (result, _) = compileOk("'a'{2,5}")
        assertEquals("a{2,5}", result)
    }

    @Test
    fun compileRepetitionFixed() {
        val (result, _) = compileOk("'a'{3}")
        assertEquals("a{3}", result)
    }

    @Test
    fun compileSequence() {
        val (result, _) = compileOk("'hello' 'world'")
        assertEquals("helloworld", result)
    }

    @Test
    fun compileGroup() {
        val (result, _) = compileOk("('a' | 'b')")
        // Optimization may merge single-char alternatives into a char set
        assertTrue(result == "(?:a|b)" || result == "[ab]", "Got: $result")
    }

    @Test
    fun compileCapturingGroup() {
        val (result, _) = compileOk(":('a')")
        assertEquals("(a)", result)
    }

    @Test
    fun compileNamedCapturingGroup() {
        val (result, _) = compileOk(":name('a')")
        assertEquals("(?P<name>a)", result)
    }

    @Test
    fun compileNamedGroupJavaScript() {
        val (result, _) = compileOk(":name('a')", RegexFlavor.JavaScript)
        assertEquals("(?<name>a)", result)
    }

    @Test
    fun compileBoundaryStart() {
        val (result, _) = compileOk("^")
        assertEquals("^", result)
    }

    @Test
    fun compileBoundaryEnd() {
        val (result, _) = compileOk("$")
        assertEquals("$", result)
    }

    @Test
    fun compileBoundaryWord() {
        val (result, _) = compileOk("%")
        assertEquals("\\b", result)
    }

    @Test
    fun compileDot() {
        val (result, _) = compileOk(".")
        assertEquals(".", result)
    }

    @Test
    fun compileCharClassShorthand() {
        val (result, _) = compileOk("[w]")
        // PCRE (default) polyfills \w to Unicode properties
        assertEquals("[\\p{Alphabetic}\\pM\\p{Nd}\\p{Pc}]", result)
    }

    @Test
    fun compileCharClassNegatedShorthand() {
        val (result, _) = compileOk("[!w]")
        assertEquals("[^\\p{Alphabetic}\\pM\\p{Nd}\\p{Pc}]", result)
    }

    @Test
    fun compileCharClassDigit() {
        val (result, _) = compileOk("[d]")
        assertEquals("\\d", result)
    }

    @Test
    fun compileCharClassRange() {
        val (result, _) = compileOk("['a'-'z']")
        assertEquals("[a-z]", result)
    }

    @Test
    fun compileCharClassMultiple() {
        val (result, _) = compileOk("['a'-'z' 'A'-'Z']")
        // Ranges sorted by code point: A-Z (65-90) before a-z (97-122)
        assertEquals("[A-Za-z]", result)
    }

    @Test
    fun compileUnicodeCategory() {
        val (result, _) = compileOk("[Letter]")
        // Pcre flavor uses abbreviated category: \pL
        assertEquals("\\pL", result)
    }

    @Test
    fun compileUnicodeScript() {
        val (result, _) = compileOk("[Latin]")
        assertEquals("\\p{Latin}", result)
    }

    @Test
    fun compileLookahead() {
        val (result, _) = compileOk(">> 'a'")
        assertEquals("(?=a)", result)
    }

    @Test
    fun compileLookbehind() {
        val (result, _) = compileOk("<< 'a'")
        assertEquals("(?<=a)", result)
    }

    @Test
    fun compileNegativeLookahead() {
        val (result, _) = compileOk("!>> 'a'")
        assertEquals("(?!a)", result)
    }

    @Test
    fun compileNegatedCharClass() {
        val (result, _) = compileOk("![w]")
        assertEquals("[^\\p{Alphabetic}\\pM\\p{Nd}\\p{Pc}]", result)
    }

    @Test
    fun compileLetBinding() {
        val (result, _) = compileOk("let x = 'test'; x+")
        assertEquals("(?:test)+", result)
    }

    @Test
    fun compileLetBindingMultipleUses() {
        val (result, _) = compileOk("let d = [d]; d d d")
        assertEquals("\\d\\d\\d", result)
    }

    @Test
    fun compileRegexLiteral() {
        val (result, _) = compileOk("regex '[a-z]+'")
        assertEquals("[a-z]+", result)
    }

    @Test
    fun compileEscaping() {
        val (result, _) = compileOk("'a.b'")
        assertEquals("a\\.b", result)
    }

    @Test
    fun compileEscapingBrackets() {
        val (result, _) = compileOk("'[test]'")
        assertEquals("\\[test\\]", result)
    }

    @Test
    fun compileCodePoint() {
        val (result, _) = compileOk("U+0041")
        assertEquals("A", result)
    }

    @Test
    fun compileEnableLazy() {
        val (result, _) = compileOk("enable lazy; 'a'+")
        assertEquals("a+?", result)
    }

    @Test
    fun compileRecursion() {
        val (result, _) = compileOk("recursion", RegexFlavor.Pcre)
        assertEquals("\\g<0>", result)
    }

    // --- Complex expressions ---

    @Test
    fun compileEmailLike() {
        // Use char ranges instead of string literals in char class
        val (result, _) = compileOk("[w]+ '@' [w]+")
        assertNotNull(result)
        assertTrue(result.contains("@"))
    }

    @Test
    fun compileNumberPattern() {
        val (result, _) = compileOk("let digit = [d]; ^ digit+ ('.' digit+)? $")
        assertNotNull(result)
        assertTrue(result.startsWith("^"))
        assertTrue(result.endsWith("$"))
    }

    // --- Options ---

    @Test
    fun compileOptionsDefaults() {
        val opts = CompileOptions()
        assertEquals(RegexFlavor.Pcre, opts.flavor)
    }

    @Test
    fun featuresDefault() {
        val features = PomskyFeatures.default()
        assertTrue(features.supports(PomskyFeatures.GRAPHEME))
    }

    @Test
    fun featuresDisable() {
        val features = PomskyFeatures.default().grapheme(false)
        assertTrue(!features.supports(PomskyFeatures.GRAPHEME))
    }

    @Test
    fun parseAndCompileApi() {
        val (result, diags, tests) = Expr.parseAndCompile("'hello' | 'world'")
        assertEquals("hello|world", result)
        assertTrue(diags.none { it.severity == Severity.Error })
    }

    // --- Helpers ---

    private fun compileOk(
        input: String,
        flavor: RegexFlavor = RegexFlavor.Pcre,
    ): Pair<String, List<ru.kode.pomskykt.diagnose.Diagnostic>> {
        val (result, diags, _) = Expr.parseAndCompile(input, CompileOptions(flavor = flavor))
        val errors = diags.filter { it.severity == Severity.Error }
        assertTrue(errors.isEmpty(), "Expected no errors for '$input', got: ${errors.map { it.msg }}")
        assertNotNull(result, "Expected result for '$input'")
        return result to diags
    }
}
