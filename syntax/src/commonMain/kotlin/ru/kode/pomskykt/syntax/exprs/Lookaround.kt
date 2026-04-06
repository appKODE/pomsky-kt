package ru.kode.pomskykt.syntax.exprs

import ru.kode.pomskykt.syntax.Span

/** Lookahead/lookbehind assertion. */
data class Lookaround(
    val kind: LookaroundKind,
    val rule: Rule,
    val span: Span,
)

/** The kind of lookaround assertion. */
enum class LookaroundKind {
    /** `>>` — positive lookahead. */
    Ahead,
    /** `<<` — positive lookbehind. */
    Behind,
    /** `!>>` — negative lookahead. */
    AheadNegative,
    /** `!<<` — negative lookbehind. */
    BehindNegative,
}
