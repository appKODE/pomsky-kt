package ru.kode.pomskykt

import ru.kode.pomskykt.diagnose.Severity
import ru.kode.pomskykt.options.CompileOptions
import ru.kode.pomskykt.options.RegexFlavor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class RedosDetectionTest {

    private fun compileWithWarnings(
        input: String,
        flavor: RegexFlavor = RegexFlavor.Pcre,
    ): Pair<String?, List<ru.kode.pomskykt.diagnose.Diagnostic>> {
        val (result, diags, _) = Expr.parseAndCompile(input, CompileOptions(flavor = flavor))
        return result to diags
    }

    @Test
    fun nestedPlusPlus() {
        val (result, diags) = compileWithWarnings("('a'+)+")
        // Should compile successfully (warning, not error)
        assertNotNull(result, "Expected compilation to succeed with warning")
        val warnings = diags.filter { it.severity == Severity.Warning }
        assertTrue(warnings.any { "catastrophic backtracking" in it.msg },
            "Expected ReDoS warning, got: ${warnings.map { it.msg }}")
    }

    @Test
    fun nestedStarStar() {
        val (result, diags) = compileWithWarnings("('a'*)*")
        assertNotNull(result, "Expected compilation to succeed with warning")
        val warnings = diags.filter { it.severity == Severity.Warning }
        assertTrue(warnings.any { "catastrophic backtracking" in it.msg },
            "Expected ReDoS warning, got: ${warnings.map { it.msg }}")
    }

    @Test
    fun nestedAlternationPlus() {
        val (result, diags) = compileWithWarnings("('a'+ | 'b')+")
        assertNotNull(result, "Expected compilation to succeed with warning")
        val warnings = diags.filter { it.severity == Severity.Warning }
        assertTrue(warnings.any { "catastrophic backtracking" in it.msg },
            "Expected ReDoS warning, got: ${warnings.map { it.msg }}")
    }

    @Test
    fun singlePlusNoWarning() {
        val (result, diags) = compileWithWarnings("('a'+)")
        assertNotNull(result)
        val warnings = diags.filter { it.severity == Severity.Warning }
        assertTrue(warnings.none { "catastrophic backtracking" in it.msg },
            "Should not have ReDoS warning for single quantifier")
    }

    @Test
    fun charClassPlusNoWarning() {
        val (result, diags) = compileWithWarnings("[w]+")
        assertNotNull(result)
        val warnings = diags.filter { it.severity == Severity.Warning }
        assertTrue(warnings.none { "catastrophic backtracking" in it.msg },
            "Should not have ReDoS warning for char class with single quantifier")
    }

    @Test
    fun nonNestedRepetitionsNoWarning() {
        val (result, diags) = compileWithWarnings("('hello')+ 'world'+")
        assertNotNull(result)
        val warnings = diags.filter { it.severity == Severity.Warning }
        assertTrue(warnings.none { "catastrophic backtracking" in it.msg },
            "Should not have ReDoS warning for non-nested repetitions")
    }
}
