package ru.kode.pomskykt.syntax.parse

import ru.kode.pomskykt.syntax.Span
import ru.kode.pomskykt.syntax.diagnose.*
import ru.kode.pomskykt.syntax.exprs.*
import ru.kode.pomskykt.syntax.lexer.LexErrorMsg
import ru.kode.pomskykt.syntax.lexer.Token
import ru.kode.pomskykt.syntax.lexer.tokenize
import ru.kode.pomskykt.syntax.unicode.AsciiClasses
import ru.kode.pomskykt.syntax.unicode.UnicodeData

/**
 * Public entry point: parse a pomsky expression.
 *
 * @param source The pomsky expression string
 * @param maxRecursion Maximum nesting depth (default 256)
 * @return Pair of (parsed rule or null, list of diagnostics)
 */
fun parse(source: String, maxRecursion: Int = 256): Pair<Rule?, List<ParseDiagnostic>> {
    val tokens = tokenize(source)
    val diagnostics = mutableListOf<ParseDiagnostic>()

    // Collect lex errors
    for ((token, span) in tokens) {
        if (token is Token.ErrorMsg) {
            diagnostics.add(ParseDiagnostic(
                ParseDiagnosticKind.Error(ParseErrorKind.LexError(token.error)),
                span,
            ))
        } else if (token is Token.Error) {
            diagnostics.add(ParseDiagnostic(
                ParseDiagnosticKind.Error(ParseErrorKind.UnknownToken),
                span,
            ))
        }
    }

    if (diagnostics.isNotEmpty()) return null to diagnostics

    val parser = Parser(source, tokens, maxRecursion)
    return try {
        val rule = parser.parseModified()
        if (!parser.isEmpty()) {
            diagnostics.add(ParseDiagnostic(
                ParseDiagnosticKind.Error(ParseErrorKind.LeftoverTokens),
                parser.span(),
            ))
            null to (diagnostics + parser.warnings())
        } else {
            rule to parser.warnings()
        }
    } catch (e: ParseException) {
        diagnostics.add(ParseDiagnostic(
            ParseDiagnosticKind.Error(e.kind),
            e.spanOverride ?: parser.errorSpan(),
        ))
        null to (diagnostics + parser.warnings())
    }
}

/**
 * Recursive descent parser for pomsky expressions.
 */
internal class Parser(
    private val source: String,
    private val tokens: List<Pair<Token, Span>>,
    private var recursion: Int,
) {
    private var offset: Int = 0
    private val warningsList = mutableListOf<ParseDiagnostic>()
    var isLazy: Boolean = false
    var isUnicodeAware: Boolean = true

    // --- Navigation ---

    fun isEmpty(): Boolean = offset >= tokens.size

    fun span(): Span =
        if (offset < tokens.size) tokens[offset].second
        else if (tokens.isNotEmpty()) {
            val last = tokens.last().second
            Span(last.end, last.end)
        } else Span.EMPTY

    fun errorSpan(): Span = span()

    fun lastSpan(): Span =
        if (offset > 0) tokens[offset - 1].second else Span.EMPTY

    fun peek(): Pair<Token, String>? {
        if (offset >= tokens.size) return null
        val (token, span) = tokens[offset]
        return token to sourceAt(span)
    }

    fun peekToken(): Token? =
        if (offset < tokens.size) tokens[offset].first else null

    fun peekPair(): Pair<Token, Span>? =
        if (offset < tokens.size) tokens[offset] else null

    private fun sourceAt(span: Span): String =
        source.substring(span.start, span.end)

    fun advance() { offset++ }

    fun warnings(): List<ParseDiagnostic> = warningsList

    fun addWarning(kind: ParseWarningKind, span: Span) {
        warningsList.add(ParseDiagnostic(ParseDiagnosticKind.Warning(kind), span))
    }

    // --- Token consumption ---

    fun consume(token: Token): Boolean {
        if (offset < tokens.size && tokens[offset].first == token) {
            offset++
            return true
        }
        return false
    }

    fun consumeAs(token: Token): String? {
        if (offset < tokens.size && tokens[offset].first == token) {
            val text = sourceAt(tokens[offset].second)
            offset++
            return text
        }
        return null
    }

    fun consumeReserved(name: String): Boolean {
        if (offset < tokens.size) {
            val (tok, span) = tokens[offset]
            if (tok == Token.ReservedName && sourceAt(span) == name) {
                offset++
                return true
            }
        }
        return false
    }

    fun consumeKeyword(keyword: String): Boolean {
        if (offset < tokens.size) {
            val (tok, span) = tokens[offset]
            if ((tok == Token.Identifier || tok == Token.ReservedName) && sourceAt(span) == keyword) {
                offset++
                return true
            }
        }
        return false
    }

    fun expect(token: Token) {
        if (!consume(token)) {
            throw ParseException(ParseErrorKind.ExpectedToken(token))
        }
    }

    fun expectAs(token: Token): String {
        return consumeAs(token)
            ?: throw ParseException(ParseErrorKind.ExpectedToken(token))
    }

    // --- Recursion ---

    fun recursionStart() {
        if (recursion == 0) throw ParseException(ParseErrorKind.RecursionLimit)
        recursion--
    }

    fun recursionEnd() { recursion++ }

    // --- Grammar rules ---

    /** Top-level: statements then expression. */
    fun parseModified(): Rule {
        val stmts = mutableListOf<Pair<Stmt, Span>>()

        val wasLazy = isLazy
        val wasUnicodeAware = isUnicodeAware

        // Parse all statements (mode modifiers, let bindings, test blocks)
        // interleaved, matching Rust's behavior
        while (true) {
            val stmt = parseModeModifier()
                ?: parseLet()
                ?: parseTest()
                ?: break

            val s = stmt.first
            when (s) {
                is Stmt.Enable -> when (s.setting) {
                    BooleanSetting.Lazy -> isLazy = true
                    BooleanSetting.Unicode -> isUnicodeAware = true
                    BooleanSetting.IgnoreCase, BooleanSetting.Multiline,
                    BooleanSetting.SingleLine, BooleanSetting.Extended,
                    BooleanSetting.ReuseGroups, BooleanSetting.AsciiLineBreaks -> {}
                }
                is Stmt.Disable -> when (s.setting) {
                    BooleanSetting.Lazy -> isLazy = false
                    BooleanSetting.Unicode -> isUnicodeAware = false
                    BooleanSetting.IgnoreCase, BooleanSetting.Multiline,
                    BooleanSetting.SingleLine, BooleanSetting.Extended,
                    BooleanSetting.ReuseGroups, BooleanSetting.AsciiLineBreaks -> {}
                }
                else -> {}
            }

            stmts.add(stmt)
        }

        // Check for duplicate let names
        if (stmts.size > 1) {
            val letNames = mutableSetOf<String>()
            for ((s, _) in stmts) {
                if (s is Stmt.LetDecl) {
                    if (!letNames.add(s.letBinding.name)) {
                        throw ParseException(ParseErrorKind.LetBindingExists, s.letBinding.nameSpan)
                    }
                }
            }
        }

        recursionStart()
        val rule = parseOr()
        recursionEnd()

        isLazy = wasLazy
        isUnicodeAware = wasUnicodeAware

        // Wrap statements around rule
        return stmts.foldRight(rule) { (stmt, stmtSpan), inner ->
            Rule.StmtE(StmtExpr(stmt, inner, stmtSpan.join(spanOf(inner))))
        }
    }

    private fun parseModeModifier(): Pair<Stmt, Span>? {
        val startSpan = span()
        val isEnable = when {
            consumeReserved("enable") -> true
            consumeReserved("disable") -> false
            else -> return null
        }
        val setting = when {
            consumeReserved("lazy") -> {
                isLazy = isEnable
                BooleanSetting.Lazy
            }
            consumeKeyword("unicode") -> {
                isUnicodeAware = isEnable
                BooleanSetting.Unicode
            }
            consumeKeyword("ascii") -> {
                // 'enable ascii' is equivalent to 'disable unicode'
                isUnicodeAware = !isEnable
                BooleanSetting.Unicode
            }
            consumeKeyword("ignore_case") -> {
                BooleanSetting.IgnoreCase
            }
            consumeKeyword("multiline") -> {
                BooleanSetting.Multiline
            }
            consumeKeyword("single_line") -> {
                BooleanSetting.SingleLine
            }
            consumeKeyword("extended") -> {
                BooleanSetting.Extended
            }
            consumeKeyword("reuse_groups") -> {
                BooleanSetting.ReuseGroups
            }
            consumeKeyword("ascii_line_breaks") -> {
                BooleanSetting.AsciiLineBreaks
            }
            else -> throw ParseException(ParseErrorKind.Expected("`lazy`, `unicode`, `ignore_case`, `multiline`, `single_line`, `extended`, `reuse_groups`, or `ascii_line_breaks`"))
        }
        expect(Token.Semicolon)
        val endSpan = lastSpan()
        val stmt = if (isEnable) Stmt.Enable(setting, startSpan.join(endSpan))
        else Stmt.Disable(setting, startSpan.join(endSpan))
        return stmt to startSpan.join(endSpan)
    }

    private fun parseLet(): Pair<Stmt, Span>? {
        if (!consumeReserved("let")) return null
        val startSpan = lastSpan()
        val nameSpan = span()
        val name: String
        if (peekToken() == Token.ReservedName) {
            val word = sourceAt(tokens[offset].second)
            throw ParseException(ParseErrorKind.KeywordAfterLet(word))
        }
        name = consumeAs(Token.Identifier)
            ?: throw ParseException(ParseErrorKind.ExpectedToken(Token.Identifier))
        val nameEndSpan = lastSpan()
        expect(Token.Equals)
        recursionStart()
        val rule = parseOr()
        recursionEnd()
        if (!consume(Token.Semicolon)) {
            throw ParseException(ParseErrorKind.Expected("expression or `;`"))
        }
        val endSpan = lastSpan()
        return Stmt.LetDecl(Let(name, rule, nameEndSpan)) to startSpan.join(endSpan)
    }

    private fun parseTest(): Pair<Stmt, Span>? {
        if (!consumeReserved("test")) return null
        val startSpan = lastSpan()
        expect(Token.OpenBrace)
        val cases = mutableListOf<TestCase>()
        while (!consume(Token.CloseBrace)) {
            if (isEmpty()) throw ParseException(ParseErrorKind.ExpectedToken(Token.CloseBrace))
            val case = parseTestCase() ?: break
            cases.add(case)
        }
        val endSpan = lastSpan()
        return Stmt.TestDecl(Test(cases, startSpan.join(endSpan))) to startSpan.join(endSpan)
    }

    private fun parseTestCase(): TestCase? {
        val startSpan = span()
        return when {
            consumeKeyword("match") -> {
                val matches = mutableListOf<TestCaseMatch>()

                // Check for 'match in ...' (no match strings, just in)
                val isInOnly = peek()?.let { (tok, text) ->
                    tok == Token.Identifier && text == "in"
                } ?: false

                if (!isInOnly) {
                    matches.add(parseTestMatch())
                    while (consume(Token.Comma)) {
                        matches.add(parseTestMatch())
                    }
                }

                // Optional 'in "haystack"'
                val haystack = if (consumeKeyword("in")) {
                    parseLiteral() ?: throw ParseException(
                        ParseErrorKind.ExpectedToken(Token.StringToken)
                    )
                } else null

                expect(Token.Semicolon)

                if (haystack != null) {
                    TestCase.MatchAll(TestCaseMatchAll(haystack, matches))
                } else if (matches.size > 1) {
                    val matchSpan = matches.first().span.join(matches.last().span)
                    throw ParseException(ParseErrorKind.MultipleStringsInTestCase, matchSpan)
                } else {
                    TestCase.Match(matches.first())
                }
            }
            consumeKeyword("reject") -> {
                val asSubstring = consumeKeyword("in")
                val lit = parseLiteral() ?: throw ParseException(
                    ParseErrorKind.ExpectedToken(Token.StringToken)
                )
                expect(Token.Semicolon)
                TestCase.Reject(TestCaseReject(lit, asSubstring))
            }
            else -> null
        }
    }

    private fun parseTestMatch(): TestCaseMatch {
        val lit = parseLiteral() ?: throw ParseException(
            ParseErrorKind.ExpectedToken(Token.StringToken)
        )
        val matchStartSpan = lastSpan()
        val captures = mutableListOf<TestCapture>()

        if (consumeKeyword("as")) {
            expect(Token.OpenBrace)
            var isFirst = true
            while (true) {
                if (!isFirst && !consume(Token.Comma)) break
                val capture = parseTestCapture() ?: break
                captures.add(capture)
                isFirst = false
            }
            expect(Token.CloseBrace)
        }

        return TestCaseMatch(lit, captures, matchStartSpan.join(lastSpan()))
    }

    private fun parseTestCapture(): TestCapture? {
        val identSpan = span()
        val ident = when {
            peekToken() == Token.Number -> {
                val n = expectAs(Token.Number).toIntOrNull()
                    ?: throw ParseException(ParseErrorKind.NumberError(NumberErr.TooLarge))
                CaptureIdent.Index(n)
            }
            peekToken() == Token.Identifier -> {
                CaptureIdent.Name(expectAs(Token.Identifier))
            }
            else -> return null
        }
        expect(Token.Colon)
        val lit = parseLiteral() ?: throw ParseException(ParseErrorKind.Expected("string"))
        return TestCapture(ident, identSpan, lit)
    }

    // --- Alternation ---

    fun parseOr(): Rule {
        // Optional leading pipe
        val leadingPipe = consume(Token.Pipe)

        val first = parseAnd()
        if (first == null) {
            if (leadingPipe) {
                throw ParseException(ParseErrorKind.LonePipe, lastSpan())
            }
            // No expression at all — return empty literal
            return Rule.Lit(Literal("", span()))
        }
        val startSpan = spanOf(first)

        val alternatives = mutableListOf(first)
        while (consume(Token.Pipe)) {
            val pipeSpan = lastSpan()
            val next = parseAnd() ?: throw ParseException(ParseErrorKind.LonePipe, pipeSpan)
            alternatives.add(next)
        }

        return if (alternatives.size == 1) alternatives[0]
        else Rule.Alt(Alternation(alternatives, startSpan.join(spanOf(alternatives.last()))))
    }

    // --- Intersection ---

    private fun parseAnd(): Rule? {
        consume(Token.Ampersand)
        val first = parseSequence() ?: return null
        val startSpan = spanOf(first)

        val parts = mutableListOf(first)
        while (consume(Token.Ampersand)) {
            val next = parseSequence()
                ?: throw ParseException(ParseErrorKind.Expected("expression"))
            parts.add(next)
        }

        return if (parts.size == 1) parts[0]
        else Rule.Inter(Intersection(parts, startSpan.join(spanOf(parts.last()))))
    }

    // --- Sequence ---

    private fun parseSequence(): Rule? {
        val parts = mutableListOf<Rule>()
        while (true) {
            val fix = parseFixes() ?: break
            parts.add(fix)
        }
        return when (parts.size) {
            0 -> null
            1 -> parts[0]
            else -> Rule.Grp(Group(
                parts,
                GroupKind.Implicit,
                spanOf(parts.first()).join(spanOf(parts.last())),
            ))
        }
    }

    // --- Negation + Repetition ---

    private fun parseFixes(): Rule? {
        var notsSpan = span()
        var nots = 0
        while (peekToken() == Token.Not) {
            nots++
            notsSpan = notsSpan.join(span())
            advance()
        }

        var rule = parseLookaround() ?: parseAtom()

        if (rule == null) {
            if (nots > 0) {
                throw ParseException(ParseErrorKind.Expected("expression"))
            }
            return null
        }

        // Wrap negations
        for (i in 0 until nots) {
            rule = Rule.Neg(Negation(rule!!, notsSpan))
        }

        // Parse repetition suffixes
        while (true) {
            val rep = parseRepetition() ?: break
            val (kind, quantifier, repSpan) = rep
            rule = Rule.Rep(Repetition(rule!!, kind, quantifier, spanOf(rule!!).join(repSpan)))
        }

        return rule
    }

    // --- Lookaround ---

    private fun parseLookaround(): Rule? {
        val startSpan = span()
        val kind = when {
            consume(Token.LookAhead) -> LookaroundKind.Ahead
            consume(Token.LookBehind) -> LookaroundKind.Behind
            else -> return null
        }
        recursionStart()
        val inner = parseModified()
        recursionEnd()
        return Rule.Look(Lookaround(kind, inner, startSpan.join(spanOf(inner))))
    }

    // --- Repetition ---

    private fun parseRepetition(): Triple<RepetitionKind, Quantifier, Span>? {
        val startSpan = span()
        val kind = when {
            consume(Token.Star) -> RepetitionKind.zeroInf()
            consume(Token.Plus) -> RepetitionKind.oneInf()
            consume(Token.QuestionMark) -> RepetitionKind.zeroOne()
            peekToken() == Token.OpenBrace -> parseRepetitionBraces() ?: return null
            else -> return null
        }
        val endSpan = lastSpan()
        val quantifier = when {
            consumeReserved("greedy") -> Quantifier.Greedy
            consumeReserved("lazy") -> Quantifier.Lazy
            isLazy -> Quantifier.DefaultLazy
            else -> Quantifier.DefaultGreedy
        }

        // Detect chained repetitions (e.g. '+''+', '?''?', '{3,4}''{7}')
        when (peekToken()) {
            Token.QuestionMark -> {
                val qmSpan = span()
                advance()
                throw ParseException(ParseErrorKind.RepetitionError(RepetitionErr.QmSuffix), qmSpan.join(lastSpan()))
            }
            Token.Plus, Token.Star -> {
                val repSpan = span()
                advance()
                throw ParseException(ParseErrorKind.RepetitionError(RepetitionErr.Multi), repSpan.join(lastSpan()))
            }
            Token.OpenBrace -> {
                val repStart = span()
                parseRepetitionBraces()  // parse to determine span of second repetition
                throw ParseException(ParseErrorKind.RepetitionError(RepetitionErr.Multi), repStart.join(lastSpan()))
            }
            else -> {}
        }

        return Triple(kind, quantifier, startSpan.join(lastSpan().let { if (it == Span.EMPTY) endSpan else it }))
    }

    private fun parseRepetitionBraces(): RepetitionKind? {
        if (!consume(Token.OpenBrace)) return null
        val braceStartSpan = lastSpan()

        val lowerStr = consumeAs(Token.Number)
        val lowerSpan = if (lowerStr != null) lastSpan() else null
        val lower = lowerStr?.let {
            it.toLongOrNull()?.also { v ->
                if (v > 65535L) throw ParseException(ParseErrorKind.NumberError(NumberErr.TooLarge), lastSpan())
            }?.toInt()
        }
        val hasComma = consume(Token.Comma)
        val upperStr = consumeAs(Token.Number)
        val upperSpan = if (upperStr != null) lastSpan() else null
        val upper = upperStr?.let {
            it.toLongOrNull()?.also { v ->
                if (v > 65535L) throw ParseException(ParseErrorKind.NumberError(NumberErr.TooLarge), lastSpan())
            }?.toInt()
        }

        val kind = when {
            lower != null && hasComma -> {
                if (upper != null && lower > upper) {
                    val boundsSpan = (lowerSpan ?: braceStartSpan).join(upperSpan ?: braceStartSpan)
                    throw ParseException(ParseErrorKind.RepetitionError(RepetitionErr.NotAscending), boundsSpan)
                }
                RepetitionKind(lower, upper)
            }
            hasComma && upper != null -> RepetitionKind(0, upper)
            lower != null && !hasComma && upper == null -> RepetitionKind.fixed(lower)
            lower == null && !hasComma && upper != null -> RepetitionKind.fixed(upper)
            else -> throw ParseException(ParseErrorKind.Expected("number"))
        }

        expect(Token.CloseBrace)
        return kind
    }

    // --- Atoms ---

    private fun parseAtom(): Rule? {
        return parseGroup()
            ?: parseString()
            ?: parseCharSet()
            ?: parseBoundary()
            ?: parseReference()
            ?: parseCodePointRule()
            ?: parseRange()
            ?: parseRegex()
            ?: parseVariable()
            ?: parseDot()
            ?: parseRecursion()
    }

    // --- Group ---

    private fun parseGroup(): Rule? {
        val (kind, startSpan) = parseGroupKind()
        if (kind !is GroupKind.Normal) {
            expect(Token.OpenParen)
        } else if (!consume(Token.OpenParen)) {
            return null
        }

        recursionStart()
        val inner = parseModified()
        recursionEnd()

        if (!consume(Token.CloseParen)) {
            throw ParseException(ParseErrorKind.Expected("`)` or an expression"))
        }
        val endSpan = lastSpan()

        // If the inner rule is an implicit group (sequence of parts), flatten it
        val parts = if (inner is Rule.Grp && inner.group.kind == GroupKind.Implicit) {
            inner.group.parts
        } else {
            listOf(inner)
        }
        val rule = Rule.Grp(Group(parts, kind, startSpan.join(endSpan)))
        return rule
    }

    private fun parseGroupKind(): Pair<GroupKind, Span> {
        if (consumeReserved("atomic")) {
            return GroupKind.Atomic to lastSpan()
        }
        if (consume(Token.Colon)) {
            val colonSpan = lastSpan()

            // Check for reserved word after colon
            if (peekToken() == Token.ReservedName) {
                val word = sourceAt(tokens[offset].second)
                throw ParseException(ParseErrorKind.KeywordAfterColon(word))
            }

            val name: String? = if (peekToken() == Token.Identifier) {
                val n = expectAs(Token.Identifier)
                val nameSpan = lastSpan()
                if (n.length > 128) throw ParseException(ParseErrorKind.GroupNameTooLong(n.length), nameSpan)
                for ((i, ch) in n.withIndex()) {
                    if (ch.code > 127) {
                        val charStart = nameSpan.start + i
                        val charEnd = charStart + 1
                        throw ParseException(
                            ParseErrorKind.NonAsciiIdentAfterColon(ch),
                            ru.kode.pomskykt.syntax.Span(charStart, charEnd),
                        )
                    }
                }
                n
            } else null

            return GroupKind.Capturing(Capture(name)) to colonSpan
        }
        return GroupKind.Normal to span()
    }

    // --- String ---

    private fun parseString(): Rule? {
        val lit = parseLiteral() ?: return null
        return Rule.Lit(lit)
    }

    internal fun parseLiteral(): Literal? {
        val strSpan = span()
        val text = consumeAs(Token.StringToken) ?: return null
        val inner = stripFirstLast(text)
        val content = if (text.startsWith('"')) {
            parseQuotedText(inner).getOrElse { e ->
                val pe = (e as? ParseException) ?: ParseException(ParseErrorKind.Expected("valid string"))
                // Compute absolute span for escape sequence errors
                if (pe.kind is ParseErrorKind.InvalidEscapeInStringAt && pe.spanOverride == null) {
                    val absPos = strSpan.start + 1 + pe.kind.position
                    throw ParseException(pe.kind, ru.kode.pomskykt.syntax.Span(absPos, absPos + 2))
                }
                throw pe
            }
        } else inner
        return Literal(content, strSpan)
    }

    // --- Character set ---

    private fun parseCharSet(): Rule? {
        if (!consume(Token.OpenBracket)) return null
        val startSpan = lastSpan()

        // Check for ^ (regex-style negation)
        if (consume(Token.Caret)) {
            throw ParseException(ParseErrorKind.CharClassError(CharClassErr.CaretInGroup), lastSpan())
        }

        val items = parseCharSetInner()

        if (!consume(Token.CloseBracket)) {
            throw ParseException(ParseErrorKind.Expected(
                "character class, string, code point, Unicode property or `]`"
            ))
        }
        val endSpan = lastSpan()

        if (items.isEmpty()) {
            throw ParseException(ParseErrorKind.CharClassError(CharClassErr.Empty), startSpan.join(endSpan))
        }
        return Rule.Class(CharClass(items, startSpan.join(endSpan), isUnicodeAware))
    }

    /**
     * Parse the contents of a character set: sequence of chars, ranges,
     * named classes, code points. Handles `!` negation prefixes.
     * Ported from Rust parse_char_set_inner.
     */
    private fun parseCharSetInner(): List<GroupItem> {
        val items = mutableListOf<GroupItem>()
        while (true) {
            var notsSpan = span()
            var nots = 0
            while (consume(Token.Not)) {
                nots++
                notsSpan = notsSpan.join(lastSpan())
            }

            // Try chars or range first (can't be negated)
            val charItems = parseCharGroupCharsOrRange()
            if (charItems != null) {
                if (nots > 0) {
                    throw ParseException(ParseErrorKind.UnallowedNot, notsSpan)
                }
                items.addAll(charItems)
                continue
            }

            // Try named class (identifier) — can be negated
            val negative = nots % 2 != 0
            val namedItems = parseCharGroupIdent(negative)
            if (namedItems != null) {
                if (nots > 1) {
                    throw ParseException(ParseErrorKind.UnallowedMultiNot(nots), notsSpan)
                }
                items.addAll(namedItems)
                continue
            }

            if (nots > 0) {
                throw ParseException(ParseErrorKind.ExpectedToken(Token.Identifier))
            }
            break
        }
        return items
    }

    private fun parseCharGroupIdent(negative: Boolean): List<GroupItem>? {
        if (peekToken() != Token.Identifier) {
            // Check for reserved words (keywords) in char sets
            if (peekToken() == Token.ReservedName) {
                val word = sourceAt(tokens[offset].second)
                throw ParseException(ParseErrorKind.UnexpectedKeyword(word))
            }
            return null
        }

        val identSpan = span()
        val saved = offset
        val ident = consumeAs(Token.Identifier)!!

        // Check for prefix:name pattern
        if (consume(Token.Colon)) {
            val name = consumeAs(Token.Identifier) ?: consumeAs(Token.ReservedName)
                ?: throw ParseException(ParseErrorKind.Expected("character class name"))
            val groupName = UnicodeData.parseGroupName(ident, name)
            if (groupName == null) {
                val prefixItemSpan = identSpan.join(lastSpan())
                // Diagnose why the lookup failed
                val validPrefixes = setOf("gc", "general_category", "sc", "script", "scx", "script_extensions", "blk", "block")
                if (ident !in validPrefixes) {
                    // Unknown prefix — check if name is valid without a prefix
                    if (UnicodeData.isValidNameWithoutPrefix(name)) {
                        throw ParseException(ParseErrorKind.CharClassError(CharClassErr.UnexpectedPrefix), prefixItemSpan)
                    }
                    // Check if name looks like a block (starts with "In...")
                    if (UnicodeData.isBlockLikeName(name)) {
                        throw ParseException(ParseErrorKind.CharClassError(
                            CharClassErr.WrongPrefix("block (blk)", true)
                        ), prefixItemSpan)
                    }
                } else {
                    // Valid prefix but wrong type — check if it's a block name disguised
                    if (UnicodeData.isBlockLikeName(name)) {
                        throw ParseException(ParseErrorKind.CharClassError(
                            CharClassErr.WrongPrefix("block (blk)", true)
                        ), prefixItemSpan)
                    }
                }
                throw ParseException(ParseErrorKind.CharClassError(
                    CharClassErr.UnknownNamedClass(name, false)
                ), prefixItemSpan)
            }
            return listOf(GroupItem.Named(groupName, negative, identSpan.join(lastSpan())))
        }

        // Try as ascii_* class name
        val asciiItems = parseAsciiGroup(ident, negative)
        if (asciiItems != null) {
            return asciiItems
        }

        // No prefix — try as plain name
        val groupName = UnicodeData.parseGroupName(null, ident)
        if (groupName != null) {
            return listOf(GroupItem.Named(groupName, negative, identSpan.join(lastSpan())))
        }

        // Not a known name — throw UnknownNamedClass
        throw ParseException(
            ParseErrorKind.CharClassError(CharClassErr.UnknownNamedClass(ident, false)),
            identSpan.join(lastSpan())
        )
    }

    /**
     * Tries to parse an ascii_* class name. Returns null if name doesn't match.
     */
    private fun parseAsciiGroup(name: String, negative: Boolean): List<GroupItem>? {
        if (name != "ascii" && !name.startsWith("ascii_")) return null
        if (negative) {
            throw ParseException(ParseErrorKind.CharClassError(CharClassErr.Negative))
        }
        return AsciiClasses.parse(name)
            ?: throw ParseException(ParseErrorKind.CharClassError(
                CharClassErr.UnknownNamedClass(name, false)
            ))
    }

    private fun parseCharGroupCharsOrRange(): List<GroupItem>? {
        val span1 = span()
        val first = parseStringOrChar() ?: return null

        if (consume(Token.Dash)) {
            val span2 = span()
            val second = parseStringOrChar()
                ?: throw ParseException(ParseErrorKind.Expected("code point or character"))

            if (first is StringOrChar.Shorthand) {
                addWarning(ParseWarningKind.Deprecation(
                    DeprecationWarning.ShorthandInRange(first.char)
                ), span1)
            }
            if (second is StringOrChar.Shorthand) {
                addWarning(ParseWarningKind.Deprecation(
                    DeprecationWarning.ShorthandInRange(second.char)
                ), span2)
            }

            val firstChar = toSingleChar(first, span1)
            val secondChar = toSingleChar(second, span2)
            val rangeSpan = span1.join(span2)
            if (firstChar > secondChar) {
                throw ParseException(
                    ParseErrorKind.CharClassError(CharClassErr.NonAscendingRange(firstChar, secondChar)),
                    rangeSpan,
                )
            }
            return listOf(GroupItem.CharRange(firstChar, secondChar))
        }

        return when (first) {
            is StringOrChar.Str -> {
                val content = parseQuotedText(first.value).getOrElse { e ->
                    throw (e as? ParseException) ?: ParseException(ParseErrorKind.Expected("valid string"))
                }
                // Detect supplementary code points (surrogate pairs)
                if (content.length == 2 && content[0].isHighSurrogate() && content[1].isLowSurrogate()) {
                    val cp = (content[0].code - 0xD800) * 0x400 + (content[1].code - 0xDC00) + 0x10000
                    listOf(GroupItem.CodePoint(cp))
                } else {
                    content.map { GroupItem.Char(it) }
                }
            }
            is StringOrChar.Ch -> listOf(GroupItem.Char(first.char))
            is StringOrChar.Shorthand -> listOf(GroupItem.Char(first.char))
        }
    }

    private fun parseStringOrChar(): StringOrChar? {
        // Try string literal
        val text = consumeAs(Token.StringToken)
        if (text != null) {
            val inner = stripFirstLast(text)
            val content = if (text.startsWith('"')) {
                parseQuotedText(inner).getOrElse { e ->
                    throw (e as? ParseException) ?: ParseException(ParseErrorKind.Expected("valid string"))
                }
            } else inner
            return if (content.length == 1) StringOrChar.Ch(content[0])
            else StringOrChar.Str(inner)
        }

        // Try code point
        val cp = parseCodePoint()
        if (cp != null) {
            val cpStr = cp.first
            return if (cpStr.length == 1) StringOrChar.Ch(cpStr[0])
            else StringOrChar.Str(cpStr)
        }

        // Try special char (n, r, t, a, e, f)
        val special = parseSpecialChar()
        if (special != null) {
            return StringOrChar.Shorthand(special)
        }

        return null
    }

    /**
     * Parses a special single-letter character identifier inside a char set.
     * These are: n → newline, r → carriage return, t → tab,
     * a → bell, e → escape, f → form feed.
     */
    private fun parseSpecialChar(): Char? {
        if (peekToken() != Token.Identifier) return null
        val (_, sp) = tokens[offset]
        val text = sourceAt(sp)
        val c = when (text) {
            "n" -> '\n'
            "r" -> '\r'
            "t" -> '\t'
            "a" -> '\u0007'
            "e" -> '\u001B'
            "f" -> '\u000C'
            else -> return null
        }
        advance()
        return c
    }

    private fun toSingleChar(soc: StringOrChar, sp: Span): Char = when (soc) {
        is StringOrChar.Ch -> soc.char
        is StringOrChar.Shorthand -> soc.char
        is StringOrChar.Str -> {
            val content = parseQuotedText(soc.value).getOrElse { e ->
                throw (e as? ParseException) ?: ParseException(ParseErrorKind.Expected("valid string"))
            }
            val iter = content.iterator()
            if (!iter.hasNext()) {
                throw ParseException(ParseErrorKind.CharStringError(CharStringErr.Empty), sp)
            }
            val c = iter.next()
            if (iter.hasNext()) {
                throw ParseException(ParseErrorKind.CharStringError(CharStringErr.TooManyCodePoints), sp)
            }
            c
        }
    }

    // --- Code point ---

    /**
     * Parse a code point literal (U+XXXX). Returns the code point as a String
     * (which may be 1 or 2 UTF-16 code units for supplementary code points).
     */
    private fun parseCodePoint(): Pair<String, Span>? {
        val cpSpan = span()
        val text = consumeAs(Token.CodePoint) ?: return null
        val cpTokenSpan = lastSpan()
        val hex = text.substringAfter('+').trim()
        val value = hex.toIntOrNull(16) ?: throw ParseException(ParseErrorKind.InvalidCodePoint, cpTokenSpan)
        // Reject surrogates and values above U+10FFFF
        if (value > 0x10FFFF || (value in 0xD800..0xDFFF)) {
            throw ParseException(ParseErrorKind.InvalidCodePoint, cpTokenSpan)
        }
        val str = if (value <= 0xFFFF) {
            value.toChar().toString()
        } else {
            // Supplementary code point: encode as surrogate pair
            val high = ((value - 0x10000) shr 10) + 0xD800
            val low = ((value - 0x10000) and 0x3FF) + 0xDC00
            charArrayOf(high.toChar(), low.toChar()).concatToString()
        }
        return str to cpSpan
    }

    private fun parseCodePointRule(): Rule? {
        val cp = parseCodePoint() ?: return null
        return Rule.Lit(Literal(cp.first, cp.second))
    }

    // --- Boundary ---

    private fun parseBoundary(): Rule? {
        val s = span()
        val kind = when {
            consume(Token.Caret) -> BoundaryKind.Start
            consume(Token.Dollar) -> BoundaryKind.End
            consume(Token.Percent) -> BoundaryKind.Word
            consume(Token.AngleLeft) -> BoundaryKind.WordStart
            consume(Token.AngleRight) -> BoundaryKind.WordEnd
            else -> return null
        }
        return Rule.Bound(Boundary(kind, isUnicodeAware, s))
    }

    // --- Reference ---

    private fun parseReference(): Rule? {
        if (!consume(Token.DoubleColon)) return null
        val startSpan = lastSpan()
        val target = when {
            peekToken() == Token.Identifier -> {
                ReferenceTarget.Named(expectAs(Token.Identifier))
            }
            peekToken() == Token.Number -> {
                val n = expectAs(Token.Number).toIntOrNull()
                    ?: throw ParseException(ParseErrorKind.NumberError(NumberErr.TooLarge))
                ReferenceTarget.Number(n)
            }
            peekToken() == Token.Plus -> {
                advance()
                val n = expectAs(Token.Number).toIntOrNull()
                    ?: throw ParseException(ParseErrorKind.NumberError(NumberErr.TooLarge))
                ReferenceTarget.Relative(n)
            }
            peekToken() == Token.Dash -> {
                advance()
                val n = expectAs(Token.Number).toIntOrNull()
                    ?: throw ParseException(ParseErrorKind.NumberError(NumberErr.TooLarge))
                ReferenceTarget.Relative(-n)
            }
            else -> throw ParseException(ParseErrorKind.Expected("reference target"))
        }
        return Rule.Ref(Reference(target, startSpan.join(lastSpan())))
    }

    // --- Range ---

    private fun parseRange(): Rule? {
        if (!consumeReserved("range")) return null
        val startSpan = lastSpan()
        val startLit = parseLiteral()
            ?: throw ParseException(ParseErrorKind.Expected("string"))
        if (startLit.content.isEmpty()) {
            throw ParseException(ParseErrorKind.NumberError(NumberErr.Empty), startLit.span)
        }
        expect(Token.Dash)
        val endLit = parseLiteral()
            ?: throw ParseException(ParseErrorKind.Expected("string"))
        if (endLit.content.isEmpty()) {
            throw ParseException(ParseErrorKind.NumberError(NumberErr.Empty), endLit.span)
        }
        val radix = if (consumeReserved("base")) {
            val n = expectAs(Token.Number).toIntOrNull()
                ?: throw ParseException(ParseErrorKind.NumberError(NumberErr.TooLarge))
            if (n < 2 || n > 36) throw ParseException(ParseErrorKind.NumberError(NumberErr.TooLarge))
            n
        } else 10
        val startDigits = parseNumber(startLit.content, radix)
        val endDigits = parseNumber(endLit.content, radix)
        // Full span: from `range` keyword to end (used for compile-time errors like RangeIsTooBig)
        val rangeSpan = startSpan.join(lastSpan())
        // Literal span: from first quote to end (used for parse-time errors about literal values)
        val literalRangeSpan = startLit.span.join(lastSpan())

        // Validate ordering: start must be <= end
        val startArr = startDigits.map { it.toByte() }.toByteArray()
        val endArr = endDigits.map { it.toByte() }.toByteArray()
        if (startArr.size > endArr.size ||
            (startArr.size == endArr.size && compareDigits(startArr, endArr) > 0)
        ) {
            throw ParseException(ParseErrorKind.RangeIsNotIncreasing, literalRangeSpan)
        }

        // Validate leading zeros: if lengths differ, neither can have leading zeros
        // (unless the number is just "0" itself — a single digit)
        if (startArr.size != endArr.size) {
            if (startArr.size > 1 && startArr[0].toInt() == 0) {
                throw ParseException(ParseErrorKind.RangeLeadingZeroesVariableLength, literalRangeSpan)
            }
            if (endArr.size > 1 && endArr[0].toInt() == 0) {
                throw ParseException(ParseErrorKind.RangeLeadingZeroesVariableLength, literalRangeSpan)
            }
        }

        return Rule.Rng(Range(startArr, endArr, radix, rangeSpan))
    }

    // --- Regex ---

    private fun parseRegex(): Rule? {
        if (!consumeReserved("regex")) return null
        val startSpan = lastSpan()
        val lit = parseLiteral()
            ?: throw ParseException(ParseErrorKind.Expected("string"))
        return Rule.Rgx(Regex(lit.content, startSpan.join(lit.span)))
    }

    // --- Variable ---

    private fun parseVariable(): Rule? {
        val s = span()
        // Check for missing let keyword
        if (peekToken() == Token.Identifier) {
            val saved = offset
            val name = expectAs(Token.Identifier)
            if (peekToken() == Token.Equals) {
                val identSpan = lastSpan()
                expect(Token.Equals)
                throw ParseException(ParseErrorKind.MissingLetKeyword, identSpan.join(lastSpan()))
            }
            return Rule.Var(Variable(name, s))
        }
        return null
    }

    // --- Dot ---

    private fun parseDot(): Rule? {
        if (!consume(Token.Dot)) return null
        return Rule.Dot
    }

    // --- Recursion ---

    private fun parseRecursion(): Rule? {
        if (!consumeReserved("recursion")) return null
        return Rule.Recur(Recursion(lastSpan()))
    }

    // --- Span helpers ---

    private fun spanOf(rule: Rule): Span = when (rule) {
        is Rule.Lit -> rule.literal.span
        is Rule.Class -> rule.charClass.span
        is Rule.Grp -> rule.group.span
        is Rule.Alt -> rule.alternation.span
        is Rule.Inter -> rule.intersection.span
        is Rule.Rep -> rule.repetition.span
        is Rule.Bound -> rule.boundary.span
        is Rule.Look -> rule.lookaround.span
        is Rule.Var -> rule.variable.span
        is Rule.Ref -> rule.reference.span
        is Rule.Rng -> rule.range.span
        is Rule.StmtE -> rule.stmtExpr.span
        is Rule.Neg -> rule.negation.notSpan
        is Rule.Rgx -> rule.regex.span
        is Rule.Recur -> rule.recursion.span
        Rule.Grapheme, Rule.Codepoint, Rule.Dot -> lastSpan()
    }
}

private sealed class StringOrChar {
    data class Str(val value: String) : StringOrChar()
    data class Ch(val char: Char) : StringOrChar()
    data class Shorthand(val char: Char) : StringOrChar()
}
