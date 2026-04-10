package ru.kode.pomskykt

import ru.kode.pomskykt.diagnose.Severity
import ru.kode.pomskykt.options.CompileOptions
import ru.kode.pomskykt.options.RegexFlavor
import ru.kode.pomskykt.syntax.exprs.TestCase
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class TestRunnerTest {

    @Test
    fun testBlockExtracted() {
        val (result, diags, tests) = Expr.parseAndCompile(
            "test { match 'hello'; } 'hello'",
            CompileOptions(flavor = RegexFlavor.Java),
        )
        assertNotNull(result)
        assertTrue(diags.none { it.severity == Severity.Error })
        assertTrue(tests.isNotEmpty(), "Expected test blocks to be extracted")
        assertTrue(tests[0].cases.isNotEmpty())
    }

    @Test
    fun matchTestPassesAgainstCompiledRegex() {
        val (result, _, tests) = Expr.parseAndCompile(
            "test { match 'hello'; } 'hello'",
            CompileOptions(flavor = RegexFlavor.Java),
        )
        assertNotNull(result)
        val regex = Regex(result)
        val case = tests[0].cases[0] as TestCase.Match
        assertTrue(regex.containsMatchIn(case.testCaseMatch.literal.content))
    }

    @Test
    fun rejectTestPassesAgainstCompiledRegex() {
        val (result, _, tests) = Expr.parseAndCompile(
            "test { reject 'world'; } 'hello'",
            CompileOptions(flavor = RegexFlavor.Java),
        )
        assertNotNull(result)
        val regex = Regex(result)
        val case = tests[0].cases[0] as TestCase.Reject
        assertTrue(!regex.matches(case.testCaseReject.literal.content))
    }

    @Test
    fun noTestBlocksReturnsEmptyList() {
        val (result, _, tests) = Expr.parseAndCompile(
            "'hello'",
            CompileOptions(flavor = RegexFlavor.Java),
        )
        assertNotNull(result)
        assertTrue(tests.isEmpty())
    }

    @Test
    fun multipleTestCases() {
        val (result, _, tests) = Expr.parseAndCompile(
            "test { match 'cat'; match 'dog'; reject 'bird'; } 'cat' | 'dog'",
            CompileOptions(flavor = RegexFlavor.Java),
        )
        assertNotNull(result)
        assertTrue(tests.isNotEmpty())
        assertTrue(tests[0].cases.size >= 3, "Expected at least 3 test cases")
    }
}
