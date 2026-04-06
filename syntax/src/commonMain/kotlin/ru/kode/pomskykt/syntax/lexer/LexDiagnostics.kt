package ru.kode.pomskykt.syntax.lexer

/**
 * Diagnostic help messages for lexer errors.
 * Provides user-friendly suggestions for converting regex syntax to pomsky.
 *
 * Ported from pomsky-syntax/src/lexer/diagnostics.rs.
 */
internal object LexDiagnostics {

    fun getHelp(msg: LexErrorMsg, slice: String): String? = when (msg) {
        LexErrorMsg.GroupNonCapturing ->
            "Non-capturing groups are just parentheses: `(...)`. " +
                "Capturing groups use the `:(...)` syntax."

        LexErrorMsg.GroupLookahead ->
            "Lookahead uses the `>>` syntax. " +
                "For example, `>> 'bob'` matches if the position is followed by bob."

        LexErrorMsg.GroupLookaheadNeg ->
            "Negative lookahead uses the `!>>` syntax. " +
                "For example, `!>> 'bob'` matches if the position is not followed by bob."

        LexErrorMsg.GroupLookbehind ->
            "Lookbehind uses the `<<` syntax. " +
                "For example, `<< 'bob'` matches if the position is preceded with bob."

        LexErrorMsg.GroupLookbehindNeg ->
            "Negative lookbehind uses the `!<<` syntax. " +
                "For example, `!<< 'bob'` matches if the position is not preceded with bob."

        LexErrorMsg.GroupComment ->
            "Comments start with `#` and go until the end of the line."

        LexErrorMsg.GroupNamedCapture -> getNamedCaptureHelp(slice)
        LexErrorMsg.GroupPcreBackreference -> getPcreBackreferenceHelp(slice)

        LexErrorMsg.Backslash -> getBackslashHelp(slice)
        LexErrorMsg.BackslashU4 -> getBackslashHelpU4(slice)
        LexErrorMsg.BackslashX2 -> getBackslashHelpX2(slice)
        LexErrorMsg.BackslashUnicode -> getBackslashHelpUnicode(slice)
        LexErrorMsg.BackslashGK -> getBackslashGkHelp(slice)
        LexErrorMsg.BackslashProperty -> getBackslashPropertyHelp(slice)

        LexErrorMsg.GroupAtomic,
        LexErrorMsg.GroupConditional,
        LexErrorMsg.GroupBranchReset,
        LexErrorMsg.GroupSubroutineCall,
        LexErrorMsg.GroupOther,
        LexErrorMsg.UnclosedString,
        LexErrorMsg.LeadingZero,
        LexErrorMsg.InvalidCodePoint,
        LexErrorMsg.FileTooBig -> null
    }

    private fun getNamedCaptureHelp(str: String): String {
        val name = str.removePrefix("(?").removePrefix("P")
            .trim('<', '>', '\'')
        return if ('-' in name) {
            "Balancing groups are not supported"
        } else {
            "Named capturing groups use the `:name(...)` syntax. Try `:$name(...)` instead"
        }
    }

    private fun getPcreBackreferenceHelp(str: String): String {
        val name = str.removePrefix("(?P=").removeSuffix(")")
        return "Backreferences use the `::name` syntax. Try `::$name` instead"
    }

    private fun getBackslashHelp(str: String): String? {
        if (!str.startsWith('\\') || str.length < 2) return null
        return when (str[1]) {
            'b' -> "Replace `\\b` with `%` to match a word boundary"
            'B' -> "Replace `\\B` with `!%` to match a place without a word boundary"
            'A' -> "Replace `\\A` with `Start` to match the start of the string"
            'z' -> "Replace `\\z` with `End` to match the end of the string"
            'Z' -> "\\Z is not supported. Use `End` to match the end of the string.\n" +
                "Note, however, that `End` doesn't match the position before the final newline."
            'N' -> "Replace `\\N` with `![n]`"
            'X' -> "Replace `\\X` with `Grapheme`"
            'R' -> "Replace `\\R` with `([r] [n] | [v])`"
            'D' -> "Replace `\\D` with `[!d]`"
            'W' -> "Replace `\\W` with `[!w]`"
            'S' -> "Replace `\\S` with `[!s]`"
            'V' -> "Replace `\\V` with `![v]`"
            'H' -> "Replace `\\H` with `![h]`"
            'G' -> "Match attempt anchors are not supported"
            in "aefnrthvdws" -> {
                val c = str[1]
                "Replace `\\$c` with `[$c]`"
            }
            '0' -> "Replace `\\0` with `U+00`"
            in '1'..'7' -> {
                val c = str[1]
                "If this is a backreference, replace it with `::$c`.\n" +
                    "If this is an octal escape, replace it with `U+0$c`."
            }
            in '8'..'9' -> {
                val c = str[1]
                "Replace `\\$c` with `::$c`"
            }
            else -> null
        }
    }

    private fun getBackslashHelpU4(str: String): String {
        val hex = str.substring(2)
        return "Try `U+$hex` instead"
    }

    private fun getBackslashHelpX2(str: String): String {
        val hex = str.substring(2)
        return "Try `U+$hex` instead"
    }

    private fun getBackslashHelpUnicode(str: String): String {
        val hex = str.substring(2).trim('{', '}')
        return "Try `U+$hex` instead"
    }

    private fun getBackslashGkHelp(str: String): String {
        val name = str.substring(2).trim('{', '}', '<', '>', '\'')
        return if (name == "0") {
            "Recursion is currently not supported"
        } else {
            "Replace `$str` with `::$name`"
        }
    }

    private fun getBackslashPropertyHelp(str: String): String {
        val isNegative = (str.startsWith("\\P") && !str.startsWith("\\P{^")) ||
            str.startsWith("\\p{^")
        val name = str.substring(2).trim('{', '}', '^').replace(Regex("[+\\-]"), "_")
        return if (isNegative) {
            "Replace `$str` with `[!$name]`"
        } else {
            "Replace `$str` with `[$name]`"
        }
    }
}
