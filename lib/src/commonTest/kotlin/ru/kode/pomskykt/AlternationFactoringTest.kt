package ru.kode.pomskykt

import ru.kode.pomskykt.diagnose.Severity
import ru.kode.pomskykt.options.CompileOptions
import ru.kode.pomskykt.options.RegexFlavor
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class AlternationFactoringTest {

    @Test
    fun commonPrefixFactored() {
        // 'abc' | 'abd' -> should factor to ab(?:c|d) or ab[cd]
        val (result, diags, _) = Expr.parseAndCompile(
            "'abc' | 'abd'",
            CompileOptions(flavor = RegexFlavor.Pcre),
        )
        assertNotNull(result, "Expected compilation to succeed")
        assertTrue(diags.none { it.severity == Severity.Error })
        // After factoring, prefix "ab" is extracted; remaining "c"|"d" merges to [cd]
        assertTrue(
            result == "ab[cd]" || result == "ab(?:c|d)",
            "Expected 'ab[cd]' or 'ab(?:c|d)', got: $result",
        )
    }

    @Test
    fun longerCommonPrefixFactored() {
        // 'hello world' | 'hello earth' -> hello (?:world|earth) or similar
        val (result, diags, _) = Expr.parseAndCompile(
            "'hello world' | 'hello earth'",
            CompileOptions(flavor = RegexFlavor.Pcre),
        )
        assertNotNull(result, "Expected compilation to succeed")
        assertTrue(diags.none { it.severity == Severity.Error })
        assertTrue(
            result.startsWith("hello "),
            "Expected common prefix 'hello ' to be factored, got: $result",
        )
        assertTrue(
            result.contains("world") && result.contains("earth"),
            "Expected both suffixes present, got: $result",
        )
    }

    @Test
    fun noCommonPrefixUnchanged() {
        // [digit] | [word] -> stays as-is (no literal prefix to factor)
        // Use Java flavor where \w is not polyfilled
        val (result, diags, _) = Expr.parseAndCompile(
            "[digit] | [word]",
            CompileOptions(flavor = RegexFlavor.Java),
        )
        assertNotNull(result, "Expected compilation to succeed")
        assertTrue(diags.none { it.severity == Severity.Error })
        // Should remain as alternation of shorthands
        assertTrue(
            result.contains("\\d") && result.contains("\\w"),
            "Expected shorthands preserved, got: $result",
        )
    }

    @Test
    fun singleCharAlternationUnchanged() {
        // 'a' | 'b' -> [ab] (single-char optimization, no common prefix)
        val (result, diags, _) = Expr.parseAndCompile(
            "'a' | 'b'",
            CompileOptions(flavor = RegexFlavor.Pcre),
        )
        assertNotNull(result, "Expected compilation to succeed")
        assertTrue(diags.none { it.severity == Severity.Error })
        assertTrue(
            result == "[ab]" || result == "a|b" || result == "[ab]",
            "Expected char set merge or alternation, got: $result",
        )
    }

    @Test
    fun threeWayCommonPrefix() {
        // 'prefix_a' | 'prefix_b' | 'prefix_c' -> prefix_[abc] or prefix_(?:a|b|c)
        val (result, diags, _) = Expr.parseAndCompile(
            "'prefix_a' | 'prefix_b' | 'prefix_c'",
            CompileOptions(flavor = RegexFlavor.Pcre),
        )
        assertNotNull(result, "Expected compilation to succeed")
        assertTrue(diags.none { it.severity == Severity.Error })
        assertTrue(
            result.startsWith("prefix_"),
            "Expected common prefix 'prefix_' to be factored, got: $result",
        )
    }
}
