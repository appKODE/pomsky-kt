package ru.kode.pomskykt.syntax.exprs

import ru.kode.pomskykt.syntax.Span

/** Integer range expression: `range '0'-'255'`. */
data class Range(
    val start: ByteArray,
    val end: ByteArray,
    val radix: Int,
    val span: Span,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Range) return false
        return start.contentEquals(other.start) &&
            end.contentEquals(other.end) &&
            radix == other.radix &&
            span == other.span
    }

    override fun hashCode(): Int {
        var result = start.contentHashCode()
        result = 31 * result + end.contentHashCode()
        result = 31 * result + radix
        result = 31 * result + span.hashCode()
        return result
    }
}
