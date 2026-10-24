package ru.kode.pomskykt.analysis

import ru.kode.pomskykt.Expr
import ru.kode.pomskykt.regex.Regex
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class MatchDiagnoserTest {

    private fun compileIr(pomsky: String): Regex {
        val expr = assertNotNull(Expr.parse(pomsky).first, "Failed to parse: $pomsky")
        val (ir, diagnostics) = expr.compileToIr(pomsky)
        return assertNotNull(ir, "Failed to compile: $pomsky, diagnostics: $diagnostics")
    }

    private fun diagnose(pomsky: String, input: String): MatchDiagnosis? =
        MatchDiagnoser.diagnose(compileIr(pomsky), input)

    @Test
    fun wrongLiteral() {
        val result = assertNotNull(diagnose("'feat' ': ' .+", "feat add x"))
        assertEquals(4, result.position)
        assertEquals("feat", result.matchedPrefix)
        assertEquals("\": \"", result.expected)
        assertNull(result.missingGroup)
    }

    @Test
    fun missingDigit() {
        val result = assertNotNull(diagnose("'v' [digit]+", "vx"))
        assertEquals(1, result.position)
        assertEquals("v", result.matchedPrefix)
        assertEquals("a digit", result.expected)
    }

    @Test
    fun alternationListsBranches() {
        val result = assertNotNull(diagnose("('feat' | 'fix') ':' .*", "docs: x"))
        assertEquals(0, result.position)
        assertEquals("", result.matchedPrefix)
        assertEquals("one of feat, fix", result.expected)
    }

    @Test
    fun alternationTruncatesLongBranchList() {
        val result = assertNotNull(diagnose("'aa' | 'bb' | 'cc' | 'dd' | 'ee' | 'ff' | 'gg'", "x"))
        assertEquals("one of aa, bb, cc, dd, ee, ...", result.expected)
    }

    @Test
    fun namedGroupIsReported() {
        val result = assertNotNull(diagnose(":ticket([Letter]+ '-' [digit]+)", "ABC-x"))
        assertEquals(4, result.position)
        assertEquals("ABC-", result.matchedPrefix)
        assertEquals("a digit", result.expected)
        assertEquals("ticket", result.missingGroup)
    }

    @Test
    fun trailingInputExpectsEndOfInput() {
        val result = assertNotNull(diagnose("'feat'", "feats"))
        assertEquals(4, result.position)
        assertEquals("end of input", result.expected)
    }

    @Test
    fun characterClassRange() {
        val result = assertNotNull(diagnose("['a'-'z']+", "ABC"))
        assertEquals(0, result.position)
        assertEquals("one of a-z", result.expected)
    }

    @Test
    fun matchingInputReturnsNull() {
        assertNull(diagnose("'feat' ': ' .+", "feat: add x"))
        assertNull(diagnose(":ticket([Letter]+ '-' [digit]+)", "ABC-123"))
        assertNull(diagnose("('feat' | 'fix') ':' .*", "fix:"))
    }

    @Test
    fun anchors() {
        assertNull(diagnose("Start 'a' End", "a"))
        val result = assertNotNull(diagnose("Start 'a' End 'b'", "ab"))
        assertEquals(1, result.position)
        assertEquals("end of input", result.expected)
    }

    @Test
    fun stepCapReturnsNull() {
        val ir = compileIr("([word]* [word]*)* '!'")
        assertNull(MatchDiagnoser.diagnose(ir, "a".repeat(50), maxSteps = 10))
    }

    @Test
    fun nestedRepetitionStaysWithinDefaultCap() {
        val result = assertNotNull(diagnose("([word]* [word]*)* '!'", "a".repeat(200)))
        assertEquals(200, result.position)
        assertEquals("a word character or \"!\"", result.expected)
    }
}
