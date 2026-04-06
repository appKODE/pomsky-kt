package ru.kode.pomskykt.syntax.parse

import ru.kode.pomskykt.syntax.diagnose.ParseErrorKind

/** Count the number of UTF-8 bytes this character would use (standalone BMP char). */
internal fun Char.utf8ByteCount(): Int = when {
    code < 0x80 -> 1
    code < 0x800 -> 2
    isHighSurrogate() || isLowSurrogate() -> 0  // part of surrogate pair, handled separately
    else -> 3
}

/** Count the number of UTF-8 bytes this string would use. KMP-compatible. */
internal fun String.utf8ByteCount(): Int {
    var bytes = 0
    var i = 0
    while (i < length) {
        val high = this[i]
        if (high.isHighSurrogate() && i + 1 < length) {
            val low = this[i + 1]
            if (low.isLowSurrogate()) {
                bytes += 4  // supplementary code point = 4 UTF-8 bytes
                i += 2
                continue
            }
        }
        bytes += when {
            high.code < 0x80 -> 1
            high.code < 0x800 -> 2
            else -> 3
        }
        i++
    }
    return bytes
}

/**
 * Helper functions for the parser.
 * Ported from pomsky-syntax/src/parse/helper.rs.
 */

/** Process escape sequences in a double-quoted string. */
internal fun parseQuotedText(input: String): Result<String> {
    if (!input.contains('\\')) return Result.success(input)
    val sb = StringBuilder(input.length)
    var i = 0
    while (i < input.length) {
        if (input[i] == '\\' && i + 1 < input.length) {
            when (input[i + 1]) {
                '\\' -> sb.append('\\')
                '"' -> sb.append('"')
                else -> return Result.failure(
                    ParseException(ParseErrorKind.InvalidEscapeInStringAt(i))
                )
            }
            i += 2
        } else {
            sb.append(input[i])
            i++
        }
    }
    return Result.success(sb.toString())
}

/** Remove first and last character (quotes). */
internal fun stripFirstLast(s: String): String =
    if (s.length >= 2) s.substring(1, s.length - 1) else s

/** Parse a number string in the given radix (2-36), returning digit bytes. */
internal fun parseNumber(src: String, radix: Int): List<Int> {
    return src.map { c ->
        val digit = c.digitToIntOrNull(radix)
            ?: throw ParseException(ParseErrorKind.NumberError(ru.kode.pomskykt.syntax.diagnose.NumberErr.InvalidDigit))
        digit
    }
}

/** Check if digit sequence starts with 0 and has more digits. */
internal fun hasLeadingZero(digits: List<Int>): Boolean =
    digits.size >= 2 && digits[0] == 0

/** Compare two same-length digit arrays lexicographically. */
internal fun compareDigits(a: ByteArray, b: ByteArray): Int {
    for (i in a.indices) {
        val cmp = a[i].compareTo(b[i])
        if (cmp != 0) return cmp
    }
    return 0
}

/** Simple exception wrapper for parser errors. Optionally carries a span override. */
internal class ParseException(val kind: ParseErrorKind, val spanOverride: ru.kode.pomskykt.syntax.Span? = null) : Exception()
