package ru.kode.pomskykt.syntax.lexer

/**
 * An error message for a token that is invalid in a pomsky expression.
 * These represent regex syntax that pomsky handles differently.
 */
enum class LexErrorMsg(val message: String) {
    // Group syntax errors
    GroupNonCapturing("This syntax is not supported"),
    GroupLookahead("This syntax is not supported"),
    GroupLookaheadNeg("This syntax is not supported"),
    GroupLookbehind("This syntax is not supported"),
    GroupLookbehindNeg("This syntax is not supported"),
    GroupNamedCapture("This syntax is not supported"),
    GroupPcreBackreference("This syntax is not supported"),
    GroupComment("Comments have a different syntax"),
    GroupAtomic("Atomic groups are not supported"),
    GroupConditional("Conditionals are not supported"),
    GroupBranchReset("Branch reset groups are not supported"),
    GroupSubroutineCall("Subroutines are not supported"),
    GroupOther("This syntax is not supported"),

    // Backslash escape errors
    Backslash("Backslash escapes are not supported"),
    BackslashU4("Backslash escapes are not supported"),
    BackslashX2("Backslash escapes are not supported"),
    BackslashUnicode("Backslash escapes are not supported"),
    BackslashProperty("Backslash escapes are not supported"),
    BackslashGK("Backslash escapes are not supported"),

    // Other errors
    UnclosedString("This string literal doesn't have a closing quote"),
    LeadingZero("Numbers can't have leading zeroes"),
    InvalidCodePoint("Code point contains non-hexadecimal digit"),
    FileTooBig("File too big (> 4 GiB)");

    /** Returns a help message for fixing this error, or null. */
    fun getHelp(slice: String): String? = LexDiagnostics.getHelp(this, slice)
}
