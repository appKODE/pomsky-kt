package ru.kode.pomskykt.decompiler

import ru.kode.pomskykt.options.RegexFlavor
import ru.kode.pomskykt.regex.RegexShorthand

/**
 * Tokenizes a regex string into [RegexToken]s.
 *
 * Flavor-aware: handles differences in escape syntax, named groups, etc.
 */
internal class RegexLexer(
    private val input: String,
    private val flavor: RegexFlavor,
) {
    private var pos = 0
    private var inCharClass = false
    private var charClassItemCount = 0

    fun tokenize(): List<RegexToken> {
        val tokens = mutableListOf<RegexToken>()
        while (pos < input.length) {
            val token = if (inCharClass) nextInClass() else nextOutside()
            if (token != null) tokens.add(token)
            val pending = pendingToken
            if (pending != null) {
                tokens.add(pending)
                pendingToken = null
            }
        }
        return tokens
    }

    private fun nextOutside(): RegexToken? {
        val c = input[pos]
        return when (c) {
            '\\' -> lexEscape()
            '[' -> tryTopLevelPosixBoundary() ?: run {
                pos++; inCharClass = true; charClassItemCount = 0; RegexToken.OpenBracket
            }
            '(' -> lexGroupOpen()
            ')' -> { pos++; RegexToken.CloseParen }
            '|' -> { pos++; RegexToken.Pipe }
            '.' -> { pos++; RegexToken.Dot }
            '^' -> { pos++; RegexToken.StartAnchor }
            '$' -> { pos++; RegexToken.EndAnchor }
            '*' -> { pos++; maybeQuantifierLazy(RegexToken.Star) }
            '+' -> { pos++; maybeQuantifierLazy(RegexToken.Plus) }
            '?' -> { pos++; maybeQuantifierLazy(RegexToken.Question) }
            '{' -> lexRepeat()
            else -> { pos++; RegexToken.Char(c) }
        }
    }

    private fun nextInClass(): RegexToken? {
        val c = input[pos]
        val token = when (c) {
            '\\' -> lexEscape()
            ']' -> { pos++; inCharClass = false; RegexToken.CloseBracket }
            '[' -> tryPosixClass() ?: run { pos++; RegexToken.Char('[') }
            '^' -> {
                pos++
                if (charClassItemCount == 0) RegexToken.CaretInClass
                else RegexToken.Char('^')
            }
            '-' -> { pos++; RegexToken.Hyphen }
            '&' -> {
                if (pos + 1 < input.length && input[pos + 1] == '&') {
                    pos += 2; RegexToken.ClassIntersection
                } else {
                    pos++; RegexToken.Char(c)
                }
            }
            else -> { pos++; RegexToken.Char(c) }
        }
        if (token !is RegexToken.CaretInClass && token !is RegexToken.CloseBracket) {
            charClassItemCount++
        }
        return token
    }

    private fun lexEscape(): RegexToken {
        pos++ // skip backslash
        if (pos >= input.length) return RegexToken.Char('\\')
        val c = input[pos]
        pos++
        return when (c) {
            'w' -> RegexToken.Shorthand(RegexShorthand.Word)
            'W' -> RegexToken.Shorthand(RegexShorthand.NotWord)
            'd' -> RegexToken.Shorthand(RegexShorthand.Digit)
            'D' -> RegexToken.Shorthand(RegexShorthand.NotDigit)
            's' -> RegexToken.Shorthand(RegexShorthand.Space)
            'S' -> RegexToken.Shorthand(RegexShorthand.NotSpace)
            'v' -> RegexToken.Shorthand(RegexShorthand.VertSpace)
            'h' -> RegexToken.Shorthand(RegexShorthand.HorizSpace)
            'b' -> RegexToken.WordBoundary
            'B' -> RegexToken.NotWordBoundary
            'X' -> RegexToken.GraphemeCluster
            'n' -> RegexToken.CodePoint('\n'.code)
            'r' -> RegexToken.CodePoint('\r'.code)
            't' -> RegexToken.CodePoint('\t'.code)
            'f' -> RegexToken.CodePoint(0x0C)
            'p', 'P' -> lexUnicodeProperty(c == 'P')
            'x' -> lexHexEscape()
            'u' -> lexUnicodeEscape()
            'U' -> lexLongUnicodeEscape()
            'k' -> lexNamedBackref()
            'g' -> lexRecursionOrBackref()
            '<' -> RegexToken.WordStart
            '>' -> RegexToken.WordEnd
            in '1'..'9' -> {
                val idx = parseDigits(c)
                RegexToken.BackrefNumbered(idx)
            }
            else -> RegexToken.Char(c) // escaped literal
        }
    }

    private fun lexUnicodeProperty(negative: Boolean): RegexToken {
        if (pos >= input.length) return RegexToken.Char(if (negative) 'P' else 'p')
        val c = input[pos]
        return if (c == '{') {
            pos++
            val name = readUntil('}')
            pos++ // skip '}'
            RegexToken.UnicodeProperty(name, negative)
        } else {
            // Single-letter property like \pL
            pos++
            RegexToken.UnicodeProperty(c.toString(), negative)
        }
    }

    private fun lexHexEscape(): RegexToken {
        if (pos >= input.length) return RegexToken.Char('x')
        return if (input[pos] == '{') {
            pos++
            val hex = readUntil('}')
            pos++ // skip '}'
            RegexToken.CodePoint(hex.toInt(HEX_RADIX))
        } else {
            val hex = readN(2)
            RegexToken.CodePoint(hex.toInt(HEX_RADIX))
        }
    }

    private fun lexUnicodeEscape(): RegexToken {
        if (pos >= input.length) return RegexToken.Char('u')
        return if (input[pos] == '{') {
            pos++
            val hex = readUntil('}')
            pos++ // skip '}'
            RegexToken.CodePoint(hex.toInt(HEX_RADIX))
        } else {
            val hex = readN(UNICODE_ESCAPE_LEN)
            val cp = hex.toInt(HEX_RADIX)
            // .NET surrogate pair detection: \uD800\uDC00 → single code point
            if (cp in HIGH_SURROGATE_RANGE && flavor == RegexFlavor.DotNet &&
                pos + 1 < input.length && input[pos] == '\\' && input[pos + 1] == 'u'
            ) {
                val savedPos = pos
                pos += 2 // skip \u
                val lowHex = readN(UNICODE_ESCAPE_LEN)
                val lowCp = lowHex.toInt(HEX_RADIX)
                if (lowCp in LOW_SURROGATE_RANGE) {
                    val combined = ((cp - 0xD800) shl 10) + (lowCp - 0xDC00) + 0x10000
                    RegexToken.CodePoint(combined)
                } else {
                    pos = savedPos // not a surrogate pair, rewind
                    RegexToken.CodePoint(cp)
                }
            } else {
                RegexToken.CodePoint(cp)
            }
        }
    }

    private fun lexLongUnicodeEscape(): RegexToken {
        if (pos >= input.length) return RegexToken.Char('U')
        val hex = readN(LONG_UNICODE_ESCAPE_LEN)
        return RegexToken.CodePoint(hex.toInt(HEX_RADIX))
    }

    private fun lexNamedBackref(): RegexToken {
        if (pos >= input.length) return RegexToken.Char('k')
        val delim = input[pos]
        return if (delim == '<' || delim == '\'') {
            pos++
            val close = if (delim == '<') '>' else '\''
            val name = readUntil(close)
            pos++ // skip close
            RegexToken.BackrefNamed(name)
        } else {
            RegexToken.Char('k')
        }
    }

    private fun lexRecursionOrBackref(): RegexToken {
        if (pos >= input.length || input[pos] != '<') return RegexToken.Char('g')
        pos++ // skip '<'
        val content = readUntil('>')
        pos++ // skip '>'
        return if (content == "0") {
            RegexToken.Recursion
        } else {
            val idx = content.toIntOrNull()
            if (idx != null) RegexToken.BackrefNumbered(idx)
            else RegexToken.BackrefNamed(content)
        }
    }

    private fun lexGroupOpen(): RegexToken {
        pos++ // skip '('
        if (pos >= input.length || input[pos] != '?') {
            return RegexToken.OpenParen // simple capturing group
        }
        pos++ // skip '?'
        if (pos >= input.length) return RegexToken.OpenParen

        val c = input[pos]
        return when (c) {
            ':' -> { pos++; RegexToken.NonCapturing }
            '>' -> { pos++; RegexToken.AtomicGroup }
            '=' -> { pos++; RegexToken.LookaheadPos }
            '!' -> { pos++; RegexToken.LookaheadNeg }
            '<' -> {
                pos++
                if (pos >= input.length) return RegexToken.LookbehindPos
                val next = input[pos]
                when (next) {
                    '=' -> { pos++; RegexToken.LookbehindPos }
                    '!' -> { pos++; RegexToken.LookbehindNeg }
                    else -> {
                        // Named group: (?<name>...)
                        val name = readUntil('>')
                        pos++ // skip '>'
                        RegexToken.NamedGroup(name)
                    }
                }
            }
            'P' -> {
                pos++
                if (pos < input.length && input[pos] == '<') {
                    pos++ // skip '<'
                    val name = readUntil('>')
                    pos++ // skip '>'
                    RegexToken.NamedGroup(name)
                } else if (pos < input.length && input[pos] == '=') {
                    pos++ // skip '='
                    val name = readUntil(')')
                    // don't skip ')' — it will be consumed as CloseParen
                    RegexToken.BackrefNamed(name)
                } else {
                    RegexToken.NonCapturing
                }
            }
            else -> RegexToken.NonCapturing
        }
    }

    private fun lexRepeat(): RegexToken {
        val start = pos
        pos++ // skip '{'
        val minStr = readDigits()
        if (minStr.isEmpty() || pos >= input.length) {
            pos = start + 1
            return RegexToken.Char('{')
        }
        val min = minStr.toInt()
        return when {
            pos < input.length && input[pos] == '}' -> {
                pos++ // skip '}'
                maybeQuantifierLazy(RegexToken.Repeat(min, min))
            }
            pos < input.length && input[pos] == ',' -> {
                pos++ // skip ','
                val maxStr = readDigits()
                if (pos < input.length && input[pos] == '}') {
                    pos++ // skip '}'
                    val max = if (maxStr.isEmpty()) null else maxStr.toInt()
                    maybeQuantifierLazy(RegexToken.Repeat(min, max))
                } else {
                    pos = start + 1
                    RegexToken.Char('{')
                }
            }
            else -> {
                pos = start + 1
                RegexToken.Char('{')
            }
        }
    }

    private var pendingToken: RegexToken? = null

    private fun maybeQuantifierLazy(token: RegexToken): RegexToken {
        if (pos < input.length && input[pos] == '?') {
            pos++
            // Emit the quantifier token now; store Lazy as pending
            pendingToken = RegexToken.Lazy
        }
        return token
    }

    /**
     * Try to parse `[[:>:]]` or `[[::<:]]` as word boundary at top level.
     * These are full char-class expressions that mean word-end/word-start in PCRE.
     */
    private fun tryTopLevelPosixBoundary(): RegexToken? {
        // Must match pattern: [[:X:]] where X is < or >
        if (pos + 5 >= input.length) return null
        val s = input.substring(pos, minOf(pos + 7, input.length))
        if (s.startsWith("[[:>:]]")) {
            pos += 7
            return RegexToken.WordEnd
        }
        if (s.startsWith("[[:<:]]")) {
            pos += 7
            return RegexToken.WordStart
        }
        return null
    }

    /**
     * Try to parse a POSIX bracket expression like `[:alpha:]` or `[:>:]`.
     * Called when `[` is encountered inside a char class.
     * Returns null if it's not a POSIX class (caller falls back to literal `[`).
     */
    private fun tryPosixClass(): RegexToken? {
        if (pos + 2 >= input.length || input[pos + 1] != ':') return null
        val start = pos
        pos += 2 // skip `[:`
        val nameStart = pos
        while (pos < input.length && input[pos] != ':') pos++
        if (pos + 1 >= input.length || input[pos + 1] != ']') {
            pos = start // not a valid POSIX class, rewind
            return null
        }
        val name = input.substring(nameStart, pos)
        pos += 2 // skip `:]`

        // Handle word boundary POSIX classes
        return when (name) {
            "<" -> RegexToken.WordStart
            ">" -> RegexToken.WordEnd
            else -> posixClassToToken(name)
        }
    }

    private fun posixClassToToken(name: String): RegexToken = when (name) {
        "alpha" -> RegexToken.UnicodeProperty("Letter", false)
        "digit" -> RegexToken.Shorthand(RegexShorthand.Digit)
        "alnum" -> RegexToken.UnicodeProperty("Alphanumeric", false)
        "space" -> RegexToken.Shorthand(RegexShorthand.Space)
        "upper" -> RegexToken.UnicodeProperty("Uppercase_Letter", false)
        "lower" -> RegexToken.UnicodeProperty("Lowercase_Letter", false)
        "punct" -> RegexToken.UnicodeProperty("Punctuation", false)
        "word" -> RegexToken.Shorthand(RegexShorthand.Word)
        "ascii" -> RegexToken.UnicodeProperty("ASCII", false)
        "blank" -> RegexToken.Shorthand(RegexShorthand.HorizSpace)
        "cntrl" -> RegexToken.UnicodeProperty("Control", false)
        "graph" -> RegexToken.UnicodeProperty("Graph", false)
        "print" -> RegexToken.UnicodeProperty("Print", false)
        "xdigit" -> RegexToken.UnicodeProperty("Hex_Digit", false)
        else -> RegexToken.UnicodeProperty(name, false)
    }

    private fun readUntil(end: kotlin.Char): String {
        val start = pos
        while (pos < input.length && input[pos] != end) pos++
        return input.substring(start, pos)
    }

    private fun readN(n: Int): String {
        val start = pos
        val end = minOf(pos + n, input.length)
        pos = end
        return input.substring(start, end)
    }

    private fun readDigits(): String {
        val start = pos
        while (pos < input.length && input[pos] in '0'..'9') pos++
        return input.substring(start, pos)
    }

    private fun parseDigits(first: kotlin.Char): Int {
        val sb = StringBuilder()
        sb.append(first)
        while (pos < input.length && input[pos] in '0'..'9') {
            sb.append(input[pos])
            pos++
        }
        return sb.toString().toInt()
    }

    companion object {
        private const val HEX_RADIX = 16
        private const val UNICODE_ESCAPE_LEN = 4
        private const val LONG_UNICODE_ESCAPE_LEN = 8
        private val HIGH_SURROGATE_RANGE = 0xD800..0xDBFF
        private val LOW_SURROGATE_RANGE = 0xDC00..0xDFFF
    }
}
