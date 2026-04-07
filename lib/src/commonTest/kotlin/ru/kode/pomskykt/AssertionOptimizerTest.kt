package ru.kode.pomskykt

import ru.kode.pomskykt.diagnose.Severity
import ru.kode.pomskykt.options.CompileOptions
import ru.kode.pomskykt.options.RegexFlavor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class AssertionOptimizerTest {

    @Test
    fun redundantLookaheadBeforeSameLiteral() {
        // (>> 'a') 'a' 'b' -> 'a' 'b' (lookahead of 'a' before literal 'a' is redundant)
        val (result, diags, _) = Expr.parseAndCompile(
            "(>> 'a') 'a' 'b'",
            CompileOptions(flavor = RegexFlavor.Pcre),
        )
        assertNotNull(result, "Expected compilation to succeed")
        assertTrue(diags.none { it.severity == Severity.Error })
        assertEquals("ab", result, "Redundant lookahead should be removed")
    }

    @Test
    fun nonMatchingLookaheadPreserved() {
        // (>> 'a') 'b' -> stays as-is (lookahead content differs from next)
        val (result, diags, _) = Expr.parseAndCompile(
            "(>> 'a') 'b'",
            CompileOptions(flavor = RegexFlavor.Pcre),
        )
        assertNotNull(result, "Expected compilation to succeed")
        assertTrue(diags.none { it.severity == Severity.Error })
        assertEquals("(?=a)b", result, "Non-matching lookahead should be preserved")
    }

    @Test
    fun negativeLookaheadPreserved() {
        // (!>> 'a') 'b' -> stays as-is (negative lookahead is not redundant)
        val (result, diags, _) = Expr.parseAndCompile(
            "(!>> 'a') 'b'",
            CompileOptions(flavor = RegexFlavor.Pcre),
        )
        assertNotNull(result, "Expected compilation to succeed")
        assertTrue(diags.none { it.severity == Severity.Error })
        assertEquals("(?!a)b", result, "Negative lookahead should be preserved")
    }

    @Test
    fun lookaheadAlonePreserved() {
        // >> 'a' alone -> stays as-is
        val (result, diags, _) = Expr.parseAndCompile(
            ">> 'a'",
            CompileOptions(flavor = RegexFlavor.Pcre),
        )
        assertNotNull(result, "Expected compilation to succeed")
        assertTrue(diags.none { it.severity == Severity.Error })
        assertEquals("(?=a)", result, "Standalone lookahead should be preserved")
    }
}
