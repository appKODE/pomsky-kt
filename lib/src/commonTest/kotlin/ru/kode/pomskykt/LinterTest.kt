package ru.kode.pomskykt

import ru.kode.pomskykt.diagnose.Severity
import ru.kode.pomskykt.options.CompileOptions
import ru.kode.pomskykt.options.RegexFlavor
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class LinterTest {

    // --- Rule 1: Unreachable alternative ---

    @Test
    fun unreachableAlternativePrefixLiteral() {
        val (result, diags) = compile("'ab' | 'abc'")
        assertNotNull(result)
        assertHasWarning(diags, "unreachable")
    }

    @Test
    fun unreachableAlternativeIdenticalLiterals() {
        val (result, diags) = compile("'foo' | 'foobar'")
        assertNotNull(result)
        assertHasWarning(diags, "unreachable")
    }

    @Test
    fun noWarningForNonPrefixAlternatives() {
        val (result, diags) = compile("'hello' | 'world'")
        assertNotNull(result)
        assertNoWarning(diags, "unreachable")
    }

    @Test
    fun noWarningForReversedPrefixOrder() {
        // 'abc' comes first — 'ab' is not a prefix of 'abc' in terms of reachability
        // Actually 'abc' does NOT make 'ab' unreachable (shorter match comes second)
        val (result, diags) = compile("'abc' | 'ab'")
        assertNotNull(result)
        assertNoWarning(diags, "unreachable")
    }

    // --- Rule 2: Unnecessary repetition {1} ---

    @Test
    fun unnecessaryRepetitionOne() {
        val (result, diags) = compile("'x'{1}")
        assertNotNull(result)
        assertHasWarning(diags, "Repetition `{1}` has no effect")
    }

    @Test
    fun noWarningForRepetitionTwo() {
        val (result, diags) = compile("'x'{2}")
        assertNotNull(result)
        assertNoWarning(diags, "Repetition `{1}`")
    }

    @Test
    fun noWarningForRepetitionStar() {
        val (result, diags) = compile("'x'*")
        assertNotNull(result)
        assertNoWarning(diags, "Repetition `{1}`")
    }

    // --- Rule 3: Redundant nested repetition ---

    @Test
    fun redundantNestedOptionalOptional() {
        val (result, diags) = compile("('x'?)?")
        assertNotNull(result)
        assertHasWarning(diags, "Nested repetition can be simplified")
    }

    @Test
    fun redundantNestedPlusPlus() {
        val (result, diags) = compile("('x'+)+")
        assertNotNull(result)
        assertHasWarning(diags, "Nested repetition can be simplified")
    }

    @Test
    fun noWarningForSingleRepetition() {
        val (result, diags) = compile("'x'+")
        assertNotNull(result)
        assertNoWarning(diags, "Nested repetition")
    }

    // --- Rule 4: Empty expression ---

    @Test
    fun emptyGroupLiteral() {
        val (result, diags) = compile("('') 'a'")
        assertNotNull(result)
        assertHasWarning(diags, "Empty expression has no effect")
    }

    @Test
    fun noWarningForNonEmptyGroup() {
        val (result, diags) = compile("('a' | 'b')")
        assertNotNull(result)
        assertNoWarning(diags, "Empty expression")
    }

    // --- Rule 5: Unnecessary group ---

    @Test
    fun unnecessaryGroupSingleLiteral() {
        val (result, diags) = compile("('ab')")
        assertNotNull(result)
        assertHasWarning(diags, "Unnecessary parentheses")
    }

    @Test
    fun noWarningForGroupWithAlternation() {
        val (result, diags) = compile("('a' | 'b')")
        assertNotNull(result)
        assertNoWarning(diags, "Unnecessary parentheses")
    }

    @Test
    fun noWarningForCapturingGroup() {
        val (result, diags) = compile(":('a')")
        assertNotNull(result)
        assertNoWarning(diags, "Unnecessary parentheses")
    }

    // --- Rule 6: Quantifier on anchor ---

    @Test
    fun quantifierOnStartAnchor() {
        val (result, diags) = compile("^+ 'a'")
        assertNotNull(result)
        assertHasWarning(diags, "Quantifier on anchor")
    }

    @Test
    fun quantifierOnEndAnchor() {
        val (result, diags) = compile("'a' $+")
        assertNotNull(result)
        assertHasWarning(diags, "Quantifier on anchor")
    }

    @Test
    fun quantifierOnWordBoundary() {
        val (result, diags) = compile("%* 'a'")
        assertNotNull(result)
        assertHasWarning(diags, "Quantifier on anchor")
    }

    @Test
    fun noWarningForNormalBoundary() {
        val (result, diags) = compile("^ 'a' $")
        assertNotNull(result)
        assertNoWarning(diags, "Quantifier on anchor")
    }

    // --- Rule 7: Quantifier on lookaround ---

    @Test
    fun quantifierOnLookahead() {
        val (result, diags) = compile("(>> 'x')+")
        assertNotNull(result)
        assertHasWarning(diags, "Quantifier on lookaround")
    }

    @Test
    fun quantifierOnLookbehind() {
        val (result, diags) = compile("(<< 'y')*")
        assertNotNull(result)
        assertHasWarning(diags, "Quantifier on lookaround")
    }

    @Test
    fun noWarningForLookaroundWithoutQuantifier() {
        val (result, diags) = compile("(>> 'x') 'a'")
        assertNotNull(result)
        assertNoWarning(diags, "Quantifier on lookaround")
    }

    // --- Helpers ---

    private fun compile(
        input: String,
        flavor: RegexFlavor = RegexFlavor.Pcre,
    ): Pair<String?, List<ru.kode.pomskykt.diagnose.Diagnostic>> {
        val (result, diags, _) = Expr.parseAndCompile(
            input,
            CompileOptions(flavor = flavor, lintEnabled = true),
        )
        return result to diags
    }

    private fun assertHasWarning(
        diags: List<ru.kode.pomskykt.diagnose.Diagnostic>,
        substring: String,
    ) {
        val warnings = diags.filter { it.severity == Severity.Warning }
        assertTrue(
            warnings.any { substring in it.msg },
            "Expected warning containing '$substring', got: ${warnings.map { it.msg }}"
        )
    }

    private fun assertNoWarning(
        diags: List<ru.kode.pomskykt.diagnose.Diagnostic>,
        substring: String,
    ) {
        val warnings = diags.filter { it.severity == Severity.Warning }
        assertTrue(
            warnings.none { substring in it.msg },
            "Expected no warning containing '$substring', got: ${warnings.map { it.msg }}"
        )
    }
}
