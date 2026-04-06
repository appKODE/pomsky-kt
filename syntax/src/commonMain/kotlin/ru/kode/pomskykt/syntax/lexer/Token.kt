package ru.kode.pomskykt.syntax.lexer

/**
 * A token encountered while lexing a pomsky expression.
 */
sealed class Token {
    // Boundaries
    /** `^` (start boundary) */
    data object Caret : Token()
    /** `$` (end boundary) */
    data object Dollar : Token()
    /** `%` (\b boundary) */
    data object Percent : Token()
    /** `<` (word start) */
    data object AngleLeft : Token()
    /** `>` (word end) */
    data object AngleRight : Token()

    // Repetition
    /** `*` (*? repetition) */
    data object Star : Token()
    /** `+` (+? repetition) */
    data object Plus : Token()
    /** `?` (?? repetition) */
    data object QuestionMark : Token()

    // Operators
    /** `|` (or) */
    data object Pipe : Token()
    /** `&` (and) */
    data object Ampersand : Token()

    // Groups and structure
    /** `:` (capturing group start) */
    data object Colon : Token()
    /** `(` (open group) */
    data object OpenParen : Token()
    /** `)` (close group) */
    data object CloseParen : Token()
    /** `{` (open repetition) */
    data object OpenBrace : Token()
    /** `}` (close repetition) */
    data object CloseBrace : Token()
    /** `,` (comma in repetition) */
    data object Comma : Token()

    // Misc single-char
    /** `!` (negation) */
    data object Not : Token()
    /** `[` (open character class) */
    data object OpenBracket : Token()
    /** `]` (close character class) */
    data object CloseBracket : Token()
    /** `-` (unicode range) */
    data object Dash : Token()
    /** `.` (any code point except newline) */
    data object Dot : Token()
    /** `;` (delimits modifiers) */
    data object Semicolon : Token()
    /** `=` (for assignments) */
    data object Equals : Token()

    // Multi-char operators
    /** `>>` (positive lookahead) */
    data object LookAhead : Token()
    /** `<<` (positive lookbehind) */
    data object LookBehind : Token()
    /** `::` (back reference) */
    data object DoubleColon : Token()

    // Literals and names
    /** `"Hello"` or `'Hello'` */
    data object StringToken : Token()
    /** `U+FFF03` (Unicode code point) */
    data object CodePoint : Token()
    /** `12` (number in repetition) */
    data object Number : Token()
    /** `hello` (capturing group name) */
    data object Identifier : Token()
    /** `lazy` (reserved name) */
    data object ReservedName : Token()

    // Errors
    /** Illegal token with a specific error message */
    data class ErrorMsg(val error: LexErrorMsg) : Token()
    /** Token representing an unknown character */
    data object Error : Token()

    /** Display representation for error messages. */
    val displayName: String
        get() = when (this) {
            Caret -> "`^`"
            Dollar -> "`$`"
            Percent -> "`%`"
            Star -> "`*`"
            Plus -> "`+`"
            QuestionMark -> "`?`"
            Pipe -> "`|`"
            Ampersand -> "`&`"
            Colon -> "`:`"
            OpenParen -> "`(`"
            CloseParen -> "`)`"
            OpenBrace -> "`{`"
            CloseBrace -> "`}`"
            Comma -> "`,`"
            LookAhead -> "`>>`"
            LookBehind -> "`<<`"
            AngleLeft -> "`<`"
            AngleRight -> "`>`"
            DoubleColon -> "`::`"
            Not -> "`!`"
            OpenBracket -> "`[`"
            Dash -> "`-`"
            CloseBracket -> "`]`"
            Dot -> "`.`"
            Semicolon -> "`;`"
            Equals -> "`=`"
            StringToken -> "string"
            CodePoint -> "code point"
            Number -> "number"
            Identifier -> "identifier"
            ReservedName -> "reserved name"
            is ErrorMsg -> "error"
            Error -> "error"
        }
}
