package ru.kode.pomskykt.syntax.lexer

import ru.kode.pomskykt.syntax.Span
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TokenizerTest {

    private fun tokens(input: String): List<Token> =
        tokenize(input).map { it.first }

    private fun tokenSpans(input: String): List<Pair<Token, Span>> =
        tokenize(input)

    // --- Single-character tokens ---

    @Test
    fun singleCharTokens() {
        assertEquals(listOf(Token.Caret), tokens("^"))
        assertEquals(listOf(Token.Dollar), tokens("$"))
        assertEquals(listOf(Token.Percent), tokens("%"))
        assertEquals(listOf(Token.Star), tokens("*"))
        assertEquals(listOf(Token.Plus), tokens("+"))
        assertEquals(listOf(Token.QuestionMark), tokens("?"))
        assertEquals(listOf(Token.Pipe), tokens("|"))
        assertEquals(listOf(Token.Ampersand), tokens("&"))
        assertEquals(listOf(Token.Colon), tokens(":"))
        assertEquals(listOf(Token.OpenParen), tokens("("))
        assertEquals(listOf(Token.CloseParen), tokens(")"))
        assertEquals(listOf(Token.OpenBrace), tokens("{"))
        assertEquals(listOf(Token.CloseBrace), tokens("}"))
        assertEquals(listOf(Token.Comma), tokens(","))
        assertEquals(listOf(Token.Not), tokens("!"))
        assertEquals(listOf(Token.OpenBracket), tokens("["))
        assertEquals(listOf(Token.CloseBracket), tokens("]"))
        assertEquals(listOf(Token.Dash), tokens("-"))
        assertEquals(listOf(Token.Dot), tokens("."))
        assertEquals(listOf(Token.Semicolon), tokens(";"))
        assertEquals(listOf(Token.Equals), tokens("="))
    }

    // --- Multi-character operators ---

    @Test
    fun lookAhead() {
        assertEquals(listOf(Token.LookAhead), tokens(">>"))
    }

    @Test
    fun lookBehind() {
        assertEquals(listOf(Token.LookBehind), tokens("<<"))
    }

    @Test
    fun doubleColon() {
        assertEquals(listOf(Token.DoubleColon), tokens("::"))
    }

    @Test
    fun angleLeftRight() {
        // Single < and > are separate tokens
        assertEquals(listOf(Token.AngleLeft, Token.AngleRight), tokens("< >"))
    }

    // --- Strings ---

    @Test
    fun singleQuotedString() {
        assertEquals(listOf(Token.StringToken), tokens("'hello'"))
    }

    @Test
    fun doubleQuotedString() {
        assertEquals(listOf(Token.StringToken), tokens("\"hello\""))
    }

    @Test
    fun doubleQuotedStringWithEscape() {
        assertEquals(listOf(Token.StringToken), tokens("\"hello\\\"world\""))
    }

    @Test
    fun unclosedSingleQuoteString() {
        val result = tokens("'hello")
        assertEquals(1, result.size)
        assertTrue(result[0] is Token.ErrorMsg)
        assertEquals(
            LexErrorMsg.UnclosedString,
            (result[0] as Token.ErrorMsg).error,
        )
    }

    @Test
    fun unclosedDoubleQuoteString() {
        val result = tokens("\"hello")
        assertEquals(1, result.size)
        assertEquals(
            LexErrorMsg.UnclosedString,
            (result[0] as Token.ErrorMsg).error,
        )
    }

    @Test
    fun emptyStrings() {
        assertEquals(listOf(Token.StringToken), tokens("''"))
        assertEquals(listOf(Token.StringToken), tokens("\"\""))
    }

    // --- Numbers ---

    @Test
    fun numbers() {
        assertEquals(listOf(Token.Number), tokens("0"))
        assertEquals(listOf(Token.Number), tokens("42"))
        assertEquals(listOf(Token.Number), tokens("999"))
    }

    @Test
    fun leadingZero() {
        val result = tokens("007")
        assertEquals(1, result.size)
        assertEquals(
            LexErrorMsg.LeadingZero,
            (result[0] as Token.ErrorMsg).error,
        )
    }

    // --- Code points ---

    @Test
    fun codePoint() {
        assertEquals(listOf(Token.CodePoint), tokens("U+0041"))
        assertEquals(listOf(Token.CodePoint), tokens("U+FF"))
        assertEquals(listOf(Token.CodePoint), tokens("U + 1F600"))
    }

    @Test
    fun invalidCodePoint() {
        val result = tokens("U+GHIJ")
        assertEquals(1, result.size)
        assertEquals(
            LexErrorMsg.InvalidCodePoint,
            (result[0] as Token.ErrorMsg).error,
        )
    }

    // --- Identifiers and reserved words ---

    @Test
    fun identifier() {
        assertEquals(listOf(Token.Identifier), tokens("hello"))
        assertEquals(listOf(Token.Identifier), tokens("my_group"))
        assertEquals(listOf(Token.Identifier), tokens("_private"))
    }

    @Test
    fun reservedWords() {
        for (word in listOf("let", "lazy", "greedy", "range", "base", "atomic",
            "enable", "disable", "if", "else", "recursion", "regex", "test", "call")) {
            assertEquals(listOf(Token.ReservedName), tokens(word), "Expected '$word' to be ReservedName")
        }
    }

    @Test
    fun uIsReserved() {
        // Bare "U" without + is a reserved name
        assertEquals(listOf(Token.ReservedName), tokens("U"))
    }

    // --- Whitespace and comments ---

    @Test
    fun whitespaceSkipped() {
        assertEquals(listOf(Token.Star, Token.Plus), tokens("  *  +  "))
    }

    @Test
    fun commentsSkipped() {
        assertEquals(listOf(Token.Star), tokens("# this is a comment\n*"))
    }

    @Test
    fun multipleComments() {
        val result = tokens("# comment 1\n# comment 2\n*")
        assertEquals(listOf(Token.Star), result)
    }

    @Test
    fun emptyInput() {
        assertTrue(tokenize("").isEmpty())
    }

    @Test
    fun onlyWhitespace() {
        assertTrue(tokenize("   \n\t  ").isEmpty())
    }

    @Test
    fun onlyComments() {
        assertTrue(tokenize("# just a comment").isEmpty())
    }

    // --- Spans ---

    @Test
    fun spanTracking() {
        val result = tokenSpans("'hi' + 'lo'")
        assertEquals(3, result.size)
        assertEquals(Token.StringToken to Span(0, 4), result[0])
        assertEquals(Token.Plus to Span(5, 6), result[1])
        assertEquals(Token.StringToken to Span(7, 11), result[2])
    }

    @Test
    fun spanAfterComment() {
        val result = tokenSpans("# comment\n*")
        assertEquals(1, result.size)
        assertEquals(Token.Star, result[0].first)
        assertEquals(Span(10, 11), result[0].second)
    }

    // --- Complex expressions ---

    @Test
    fun simpleExpression() {
        assertEquals(
            listOf(Token.StringToken, Token.Pipe, Token.StringToken),
            tokens("'hello' | 'world'"),
        )
    }

    @Test
    fun repetitionExpression() {
        assertEquals(
            listOf(Token.StringToken, Token.OpenBrace, Token.Number,
                Token.Comma, Token.Number, Token.CloseBrace),
            tokens("'a'{1,3}"),
        )
    }

    @Test
    fun capturingGroup() {
        assertEquals(
            listOf(Token.Colon, Token.Identifier, Token.OpenParen,
                Token.StringToken, Token.CloseParen),
            tokens(":name('test')"),
        )
    }

    @Test
    fun letBinding() {
        assertEquals(
            listOf(Token.ReservedName, Token.Identifier, Token.Equals,
                Token.StringToken, Token.Semicolon),
            tokens("let x = 'test';"),
        )
    }

    // --- Backslash errors ---

    @Test
    fun backslashEscapes() {
        val result = tokens("\\b")
        assertEquals(1, result.size)
        assertEquals(LexErrorMsg.Backslash, (result[0] as Token.ErrorMsg).error)
    }

    @Test
    fun backslashU4() {
        val result = tokens("\\u0041")
        assertEquals(1, result.size)
        assertEquals(LexErrorMsg.BackslashU4, (result[0] as Token.ErrorMsg).error)
    }

    @Test
    fun backslashX2() {
        val result = tokens("\\x41")
        assertEquals(1, result.size)
        assertEquals(LexErrorMsg.BackslashX2, (result[0] as Token.ErrorMsg).error)
    }

    @Test
    fun backslashUnicodeBraces() {
        val result = tokens("\\u{1F600}")
        assertEquals(1, result.size)
        assertEquals(LexErrorMsg.BackslashUnicode, (result[0] as Token.ErrorMsg).error)
    }

    @Test
    fun backslashProperty() {
        val result = tokens("\\pL")
        assertEquals(1, result.size)
        assertEquals(LexErrorMsg.BackslashProperty, (result[0] as Token.ErrorMsg).error)
    }

    @Test
    fun backslashPropertyBraces() {
        val result = tokens("\\p{Latin}")
        assertEquals(1, result.size)
        assertEquals(LexErrorMsg.BackslashProperty, (result[0] as Token.ErrorMsg).error)
    }

    @Test
    fun backslashGK() {
        val result = tokens("\\k<name>")
        assertEquals(1, result.size)
        assertEquals(LexErrorMsg.BackslashGK, (result[0] as Token.ErrorMsg).error)
    }

    // --- Special group errors ---

    @Test
    fun groupNonCapturing() {
        val result = tokens("(?:")
        assertEquals(1, result.size)
        assertEquals(LexErrorMsg.GroupNonCapturing, (result[0] as Token.ErrorMsg).error)
    }

    @Test
    fun groupLookahead() {
        val result = tokens("(?=")
        assertEquals(1, result.size)
        assertEquals(LexErrorMsg.GroupLookahead, (result[0] as Token.ErrorMsg).error)
    }

    @Test
    fun groupLookbehind() {
        val result = tokens("(?<=")
        assertEquals(1, result.size)
        assertEquals(LexErrorMsg.GroupLookbehind, (result[0] as Token.ErrorMsg).error)
    }

    @Test
    fun groupNamedCapture() {
        val result = tokens("(?<name>")
        assertEquals(1, result.size)
        assertEquals(LexErrorMsg.GroupNamedCapture, (result[0] as Token.ErrorMsg).error)
    }

    @Test
    fun groupComment() {
        val result = tokens("(?#comment)")
        assertEquals(1, result.size)
        assertEquals(LexErrorMsg.GroupComment, (result[0] as Token.ErrorMsg).error)
    }

    // --- Error display ---

    @Test
    fun tokenDisplayName() {
        assertEquals("`^`", Token.Caret.displayName)
        assertEquals("string", Token.StringToken.displayName)
        assertEquals("number", Token.Number.displayName)
        assertEquals("identifier", Token.Identifier.displayName)
        assertEquals("error", Token.Error.displayName)
        assertEquals("error", Token.ErrorMsg(LexErrorMsg.UnclosedString).displayName)
    }

    // --- Diagnostics help ---

    @Test
    fun diagnosticsHelp() {
        assertEquals(
            "Replace `\\b` with `%` to match a word boundary",
            LexErrorMsg.Backslash.getHelp("\\b"),
        )
        assertEquals(
            "Try `U+0041` instead",
            LexErrorMsg.BackslashU4.getHelp("\\u0041"),
        )
        assertNull(LexErrorMsg.UnclosedString.getHelp("'test"))
    }

    private fun assertNull(value: String?) {
        assertEquals(null, value)
    }
}
