package ru.kode.pomskykt.syntax

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SpanTest {
    @Test
    fun emptySpan() {
        val span = Span.EMPTY
        assertTrue(span.isEmpty)
        assertNull(span.range())
        assertEquals("0..0", span.toString())
    }

    @Test
    fun spanCreation() {
        val span = Span(5, 10)
        assertEquals(5, span.start)
        assertEquals(10, span.end)
        assertEquals(5 until 10, span.range())
        assertEquals(5 until 10, span.rangeUnchecked())
    }

    @Test
    fun spanStart() {
        val span = Span(5, 10)
        assertEquals(Span(5, 5), span.start())
    }

    @Test
    fun joinNonEmpty() {
        val a = Span(5, 10)
        val b = Span(8, 15)
        assertEquals(Span(5, 15), a.join(b))
    }

    @Test
    fun joinWithEmpty() {
        val a = Span(5, 10)
        assertEquals(a, a.join(Span.EMPTY))
        assertEquals(a, Span.EMPTY.join(a))
        assertEquals(Span.EMPTY, Span.EMPTY.join(Span.EMPTY))
    }
}
