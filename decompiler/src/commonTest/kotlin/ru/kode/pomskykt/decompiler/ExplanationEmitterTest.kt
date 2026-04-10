package ru.kode.pomskykt.decompiler

import ru.kode.pomskykt.options.RegexFlavor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ExplanationEmitterTest {

    @Test
    fun explainLiteral() {
        val result = explain("hello")
        assertNotNull(result)
        assertTrue(result.contains("hello"), "Expected explanation to contain 'hello', got: $result")
    }

    @Test
    fun explainDigitPlus() {
        val result = explain("\\d+")
        assertNotNull(result)
        assertTrue(result.contains("one or more"), "Expected 'one or more', got: $result")
        assertTrue(result.contains("digit"), "Expected 'digit', got: $result")
    }

    @Test
    fun explainAnchoredLetters() {
        val result = explain("^[a-z]+\$")
        assertNotNull(result)
        assertTrue(result.contains("start of string"), "Expected 'start of string', got: $result")
        assertTrue(result.contains("end of string"), "Expected 'end of string', got: $result")
        assertTrue(result.contains("letter"), "Expected 'letter', got: $result")
    }

    @Test
    fun explainAlternation() {
        val result = explain("cat|dog")
        assertNotNull(result)
        assertTrue(result.contains("either"), "Expected 'either', got: $result")
        assertTrue(result.contains("cat"), "Expected 'cat', got: $result")
        assertTrue(result.contains("dog"), "Expected 'dog', got: $result")
    }

    @Test
    fun explainPlatePattern() {
        val result = explain("^[A-Z]{2}\\d{4}\$")
        assertNotNull(result)
        assertTrue(result.contains("exactly 2"), "Expected 'exactly 2', got: $result")
        assertTrue(result.contains("uppercase"), "Expected 'uppercase', got: $result")
        assertTrue(result.contains("exactly 4"), "Expected 'exactly 4', got: $result")
        assertTrue(result.contains("digit"), "Expected 'digit', got: $result")
    }

    @Test
    fun explainLookahead() {
        val result = explain("(?=\\d)")
        assertNotNull(result)
        assertTrue(result.contains("followed by"), "Expected 'followed by', got: $result")
        assertTrue(result.contains("digit"), "Expected 'digit', got: $result")
        assertTrue(result.contains("without consuming"), "Expected 'without consuming', got: $result")
    }

    @Test
    fun explainNamedGroup() {
        val result = explain("(?P<year>\\d{4})")
        assertNotNull(result)
        assertTrue(result.contains("captured as"), "Expected 'captured as', got: $result")
        assertTrue(result.contains("year"), "Expected 'year', got: $result")
        assertTrue(result.contains("exactly 4"), "Expected 'exactly 4', got: $result")
    }

    @Test
    fun explainWordBoundaries() {
        val result = explain("\\b\\w+\\b")
        assertNotNull(result)
        assertTrue(result.contains("word boundary"), "Expected 'word boundary', got: $result")
        assertTrue(result.contains("one or more"), "Expected 'one or more', got: $result")
        assertTrue(result.contains("word character"), "Expected 'word character', got: $result")
    }

    @Test
    fun explainEmptyPattern() {
        val result = Decompiler.explain("", RegexFlavor.Java)
        assertNull(result.error)
        assertEquals("empty pattern", result.explanation)
    }

    // --- Helper ---

    private fun explain(regex: String): String? {
        val result = Decompiler.explain(regex, RegexFlavor.Java)
        assertNull(result.error, "Explain error: ${result.error}")
        return result.explanation
    }
}
