package ru.kode.pomskykt.syntax

/**
 * A source code location, marked by the start and end byte offset.
 * If both are zero, this is considered "empty" or "missing".
 */
data class Span(val start: Int, val end: Int) {

    /** Whether this span is "empty" or "missing". */
    val isEmpty: Boolean get() = end == 0

    /** Converts to an IntRange, or null if empty. */
    fun range(): IntRange? =
        if (isEmpty) null else start until end

    /** Converts to an IntRange without checking if empty. */
    fun rangeUnchecked(): IntRange = start until end

    /** Returns a zero-length span pointing at this span's start. */
    fun start(): Span = Span(start, start)

    /** Returns a span covering both this and [other]. Empty spans are ignored. */
    fun join(other: Span): Span = when {
        !isEmpty && !other.isEmpty -> Span(
            minOf(start, other.start),
            maxOf(end, other.end),
        )
        !isEmpty -> this
        !other.isEmpty -> other
        else -> EMPTY
    }

    override fun toString(): String = "$start..$end"

    companion object {
        val EMPTY = Span(0, 0)

        fun new(start: Int, end: Int): Span = Span(start, end)
    }
}
