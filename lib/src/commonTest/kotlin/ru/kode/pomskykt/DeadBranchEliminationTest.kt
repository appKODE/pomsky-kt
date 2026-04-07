package ru.kode.pomskykt

import ru.kode.pomskykt.diagnose.Severity
import ru.kode.pomskykt.options.CompileOptions
import ru.kode.pomskykt.options.RegexFlavor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class DeadBranchEliminationTest {

    private fun compile(input: String): String {
        val (result, diags, _) = Expr.parseAndCompile(
            input,
            CompileOptions(flavor = RegexFlavor.Pcre),
        )
        assertTrue(diags.none { it.severity == Severity.Error }, "Unexpected errors: $diags")
        assertNotNull(result, "Expected compilation to succeed")
        return result
    }

    @Test
    fun removeDuplicateLiterals() {
        // 'abc' | 'abc' | 'def' -> duplicate 'abc' removed -> abc|def
        val result = compile("'abc' | 'abc' | 'def'")
        assertEquals("abc|def", result)
    }

    @Test
    fun removeSubsumedCharRange() {
        // ['a'-'z'] | ['a'-'c'] -> ['a'-'z'] only
        val result = compile("['a'-'z'] | ['a'-'c']")
        assertEquals("[a-z]", result)
    }

    @Test
    fun keepDistinctAlternatives() {
        // 'abc' | 'def' -> unchanged
        val result = compile("'abc' | 'def'")
        assertEquals("abc|def", result)
    }

    @Test
    fun handleSingleAlternativeUnwrap() {
        // 'abc' | 'abc' -> after removing duplicate, single alt unwraps
        val result = compile("'abc' | 'abc'")
        assertEquals("abc", result)
    }

    @Test
    fun nestedAlternationOptimized() {
        // Alternations inside non-capturing groups should also be optimized
        // In pomsky, () is non-capturing, so it gets unwrapped by optimizer
        val result = compile("('abc' | 'abc' | 'def')")
        assertEquals("abc|def", result)
    }

    @Test
    fun nestedAlternationInCapturingGroup() {
        // :() is capturing in pomsky — group preserved, duplicates removed
        val result = compile(":('abc' | 'abc' | 'def')")
        assertEquals("(abc|def)", result)
    }

    @Test
    fun keepNonSubsumedCharSets() {
        // ['a'-'c'] | ['x'-'z'] -> both kept, no subsumption
        val result = compile("['a'-'c'] | ['x'-'z']")
        assertTrue(
            result.contains("a-c") && result.contains("x-z"),
            "Expected both ranges preserved, got: $result",
        )
    }

    @Test
    fun widerRangeSubsumesNarrower() {
        // ['a'-'z'] | ['d'-'f'] -> wider range subsumes narrower
        val result = compile("['a'-'z'] | ['d'-'f']")
        assertEquals("[a-z]", result)
    }

    @Test
    fun threeWayDuplicateRemoval() {
        // 'xyz' | 'xyz' | 'xyz' -> single 'xyz'
        val result = compile("'xyz' | 'xyz' | 'xyz'")
        assertEquals("xyz", result)
    }

    @Test
    fun mixedDuplicatesAndDistinct() {
        // 'abc' | 'def' | 'abc' -> dedup to 'abc' | 'def'
        val result = compile("'abc' | 'def' | 'abc'")
        assertEquals("abc|def", result)
    }
}
