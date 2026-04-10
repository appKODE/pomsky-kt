package ru.kode.pomskykt.syntax.lexer

import ru.kode.pomskykt.syntax.Span

/**
 * Tokenizes a pomsky expression into a list of (Token, Span) pairs.
 *
 * Ported from pomsky-syntax/src/lexer/tokenize.rs.
 */
fun tokenize(input: String): List<Pair<Token, Span>> {
    val result = mutableListOf<Pair<Token, Span>>()
    var remaining = input
    var offset = 0

    while (true) {
        val triple = nextToken(remaining) ?: break
        val (token, start, end) = triple
        result.add(token to Span(offset + start, offset + end))
        remaining = remaining.substring(end)
        offset += end
    }

    return result
}


private val RESERVED_WORDS = setOf(
    "U", "let", "lazy", "greedy", "range", "base", "atomic", "permute",
    "enable", "disable", "if", "else", "recursion", "regex", "test", "call",
)

private val SINGLE_TOKEN_MAP: Map<Char, Token> = mapOf(
    '^' to Token.Caret,
    '$' to Token.Dollar,
    '%' to Token.Percent,
    '<' to Token.AngleLeft,
    '>' to Token.AngleRight,
    '*' to Token.Star,
    '+' to Token.Plus,
    '?' to Token.QuestionMark,
    '|' to Token.Pipe,
    '&' to Token.Ampersand,
    ':' to Token.Colon,
    ')' to Token.CloseParen,
    '{' to Token.OpenBrace,
    '}' to Token.CloseBrace,
    ',' to Token.Comma,
    '!' to Token.Not,
    '[' to Token.OpenBracket,
    ']' to Token.CloseBracket,
    '-' to Token.Dash,
    '.' to Token.Dot,
    ';' to Token.Semicolon,
    '=' to Token.Equals,
)

/**
 * Returns the next token from [input], or null if input is exhausted.
 * Returns (token, startOffset, endOffset) where offsets are relative to [input].
 */
internal fun nextToken(input: String): Triple<Token, Int, Int>? {
    // Skip whitespace and comments
    var pos = 0
    while (pos < input.length) {
        val ch = input[pos]
        if (ch.isWhitespace()) {
            pos++
        } else if (ch == '#') {
            // Skip to end of line
            while (pos < input.length && input[pos] != '\n') pos++
            if (pos < input.length) pos++ // skip the newline
        } else {
            break
        }
    }

    if (pos >= input.length) return null

    val start = pos
    val remaining = input.substring(pos)
    val c = remaining[0]

    val (len, token) = tokenizeOne(remaining, c)

    return Triple(token, start, start + len)
}

private fun tokenizeOne(input: String, c: Char): Pair<Int, Token> {
    // Two-char operators
    if (input.startsWith(">>")) return 2 to Token.LookAhead
    if (input.startsWith("<<")) return 2 to Token.LookBehind
    if (input.startsWith("::")) return 2 to Token.DoubleColon

    // Single-char tokens (except '(' which needs special group check)
    SINGLE_TOKEN_MAP[c]?.let { return 1 to it }

    // Single-quoted string
    if (c == '\'') {
        val closeIdx = input.indexOf('\'', startIndex = 1)
        return if (closeIdx >= 0) {
            (closeIdx + 1) to Token.StringToken
        } else {
            input.length to Token.ErrorMsg(LexErrorMsg.UnclosedString)
        }
    }

    // Double-quoted string
    if (c == '"') {
        val closePos = findUnescapedQuote(input, 1)
        return if (closePos != null) {
            closePos to Token.StringToken
        } else {
            input.length to Token.ErrorMsg(LexErrorMsg.UnclosedString)
        }
    }

    // Unicode code point: U+XXXX (with optional whitespace around +)
    matchCodePoint(input)?.let { return it }

    // Number
    if (c.isDigit()) {
        val len = countWhile(input) { it.isDigit() }
        return if (c == '0' && len >= 2) {
            len to Token.ErrorMsg(LexErrorMsg.LeadingZero)
        } else {
            len to Token.Number
        }
    }

    // Identifier or reserved word
    if (c.isLetter() || c == '_') {
        val len = countWhile(input) { it.isLetterOrDigit() || it == '_' }
        val word = input.substring(0, len)
        return if (word in RESERVED_WORDS) {
            len to Token.ReservedName
        } else {
            len to Token.Identifier
        }
    }

    // Special group syntax: (?...)
    parseSpecialGroup(input)?.let { (len, err) ->
        return len to Token.ErrorMsg(err)
    }

    // Open paren (after special group check fails)
    if (c == '(') return 1 to Token.OpenParen

    // Backslash escapes
    parseBackslash(input)?.let { (len, err) ->
        return len to Token.ErrorMsg(err)
    }

    // Unknown character
    return c.toString().length to Token.Error
}

/**
 * Matches a Unicode code point literal: U+XXXX (with optional whitespace around +).
 */
private fun matchCodePoint(input: String): Pair<Int, Token>? {
    if (!input.startsWith('U')) return null
    var pos = 1

    // Skip optional whitespace
    while (pos < input.length && input[pos].isWhitespace()) pos++

    if (pos >= input.length || input[pos] != '+') return null
    pos++

    // Skip optional whitespace
    while (pos < input.length && input[pos].isWhitespace()) pos++

    // Require at least one hex/alnum/underscore char
    val hexStart = pos
    while (pos < input.length && (input[pos].isLetterOrDigit() || input[pos] == '_')) pos++

    if (pos == hexStart) return null

    // Check if all chars after U+ (ignoring whitespace) are hex digits
    val hexPart = input.substring(hexStart, pos)
    return if (hexPart.any { !it.isHexDigit() }) {
        pos to Token.ErrorMsg(LexErrorMsg.InvalidCodePoint)
    } else {
        pos to Token.CodePoint
    }
}

/**
 * Finds the closing unescaped double quote in [input] starting at [startIndex].
 * Returns the index of the character after the closing quote, or null.
 */
private fun findUnescapedQuote(input: String, startIndex: Int): Int? {
    var pos = startIndex
    while (pos < input.length) {
        when (input[pos]) {
            '"' -> return pos + 1
            '\\' -> {
                pos++ // skip backslash
                if (pos < input.length) pos++ // skip escaped char
            }
            else -> pos++
        }
    }
    return null
}

/**
 * Parses backslash escape sequences and returns the error for invalid ones.
 */
private fun parseBackslash(input: String): Pair<Int, LexErrorMsg>? {
    if (input.length < 2 || input[0] != '\\') return null

    val afterSlash = input.substring(1)
    val c = afterSlash[0]

    // \u{XXXX} or \x{XXXX} — requires at least one hex digit between braces (matching Rust)
    if ((c == 'u' || c == 'x') && afterSlash.length > 1 && afterSlash[1] == '{') {
        val closeIdx = afterSlash.indexOf('}', 2)
        if (closeIdx >= 0) {
            val content = afterSlash.substring(2, closeIdx)
            if (content.isNotEmpty() && content.all { it.isHexDigit() }) {
                return (closeIdx + 2) to LexErrorMsg.BackslashUnicode
            }
        } else {
            // Check if there are any hex digits before end-of-input
            val hexCount = (2 until afterSlash.length).count { afterSlash[it].isHexDigit() }
            if (hexCount > 0) {
                return input.length to LexErrorMsg.BackslashUnicode
            }
        }
        // Fall through to other patterns if no hex digits found
    }

    // \uXXXX (exactly 4 hex digits)
    if (c == 'u' && afterSlash.length >= 5 &&
        afterSlash.substring(1, 5).all { it.isHexDigit() }
    ) {
        return 6 to LexErrorMsg.BackslashU4
    }

    // \xXX (exactly 2 hex digits)
    if (c == 'x' && afterSlash.length >= 3 &&
        afterSlash.substring(1, 3).all { it.isHexDigit() }
    ) {
        return 4 to LexErrorMsg.BackslashX2
    }

    // \k or \g (backreferences)
    if (c == 'k' || c == 'g') {
        if (afterSlash.length >= 2) {
            val delim = afterSlash[1]
            val (open, close) = when (delim) {
                '<' -> '<' to '>'
                '{' -> '{' to '}'
                '\'' -> '\'' to '\''
                else -> {
                    // \k or \g followed by digit(s)
                    if (delim == '-' || delim == '+' || delim.isDigit()) {
                        var len = 2
                        if (delim == '-' || delim == '+') len++
                        while (len < afterSlash.length && afterSlash[len].isDigit()) len++
                        return (len + 1) to LexErrorMsg.BackslashGK
                    }
                    return 2 to LexErrorMsg.BackslashGK
                }
            }
            val closeIdx = afterSlash.indexOf(close, 2)
            return if (closeIdx >= 0) {
                (closeIdx + 2) to LexErrorMsg.BackslashGK
            } else {
                input.length to LexErrorMsg.BackslashGK
            }
        }
    }

    // \p or \P (Unicode properties)
    if (c == 'p' || c == 'P') {
        if (afterSlash.length >= 2) {
            return if (afterSlash[1] == '{') {
                val closeIdx = afterSlash.indexOf('}', 2)
                if (closeIdx >= 0) {
                    (closeIdx + 2) to LexErrorMsg.BackslashProperty
                } else {
                    input.length to LexErrorMsg.BackslashProperty
                }
            } else if (afterSlash[1].isLetterOrDigit()) {
                3 to LexErrorMsg.BackslashProperty
            } else {
                2 to LexErrorMsg.Backslash
            }
        }
    }

    // Generic backslash + any char
    val charLen = afterSlash[0].toString().length
    return (1 + charLen) to LexErrorMsg.Backslash
}

/**
 * Parses special regex group syntax `(?...)` and returns the length and error.
 */
private fun parseSpecialGroup(input: String): Pair<Int, LexErrorMsg>? {
    if (!input.startsWith("(?")) return null

    if (input.length < 3) return 2 to LexErrorMsg.GroupOther

    val afterQ = input.substring(2)

    // (?:  non-capturing
    if (afterQ.startsWith(':')) return 3 to LexErrorMsg.GroupNonCapturing

    // (?=  lookahead
    if (afterQ.startsWith('=')) return 3 to LexErrorMsg.GroupLookahead

    // (?!  negative lookahead
    if (afterQ.startsWith('!')) return 3 to LexErrorMsg.GroupLookaheadNeg

    // (?>  atomic
    if (afterQ.startsWith('>')) return 3 to LexErrorMsg.GroupAtomic

    // (?(  conditional
    if (afterQ.startsWith('(')) return 3 to LexErrorMsg.GroupConditional

    // (?|  branch reset
    if (afterQ.startsWith('|')) return 3 to LexErrorMsg.GroupBranchReset

    // (?<= lookbehind
    if (afterQ.startsWith("<=")) return 4 to LexErrorMsg.GroupLookbehind

    // (?<! negative lookbehind
    if (afterQ.startsWith("<!")) return 4 to LexErrorMsg.GroupLookbehindNeg

    // (?P< or (?< named capture
    if (afterQ.startsWith("P<") || afterQ.startsWith('<')) {
        val nameStart = if (afterQ.startsWith("P<")) 2 else 1
        val closeIdx = afterQ.indexOf('>', nameStart)
        return if (closeIdx >= 0) {
            (closeIdx + 3) to LexErrorMsg.GroupNamedCapture
        } else {
            input.length to LexErrorMsg.GroupNamedCapture
        }
    }

    // (?'name' named capture
    if (afterQ.startsWith('\'')) {
        val closeIdx = afterQ.indexOf('\'', 1)
        return if (closeIdx >= 0) {
            (closeIdx + 3) to LexErrorMsg.GroupNamedCapture
        } else {
            input.length to LexErrorMsg.GroupNamedCapture
        }
    }

    // (?P=name) PCRE backreference
    if (afterQ.startsWith("P=")) {
        val closeIdx = afterQ.indexOf(')')
        return if (closeIdx >= 0) {
            (closeIdx + 3) to LexErrorMsg.GroupPcreBackreference
        } else {
            input.length to LexErrorMsg.GroupPcreBackreference
        }
    }

    // (?P> or (?& subroutine call
    if (afterQ.startsWith("P>") || afterQ.startsWith('&')) {
        val len = if (afterQ.startsWith("P>")) 4 else 3
        return len to LexErrorMsg.GroupSubroutineCall
    }

    // (?#...) comment
    if (afterQ.startsWith('#')) {
        val closeIdx = afterQ.indexOf(')')
        return if (closeIdx >= 0) {
            (closeIdx + 3) to LexErrorMsg.GroupComment
        } else {
            input.length to LexErrorMsg.GroupComment
        }
    }

    // (?  other
    return 2 to LexErrorMsg.GroupOther
}

private fun Char.isHexDigit(): Boolean =
    this in '0'..'9' || this in 'a'..'f' || this in 'A'..'F'

private inline fun countWhile(input: String, predicate: (Char) -> Boolean): Int {
    var count = 0
    while (count < input.length && predicate(input[count])) count++
    return count
}
