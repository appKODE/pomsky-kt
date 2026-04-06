package ru.kode.pomskykt.syntax.exprs

import ru.kode.pomskykt.syntax.Span

/** Boundary assertion (anchors and word boundaries). */
data class Boundary(
    val kind: BoundaryKind,
    val unicodeAware: Boolean,
    val span: Span,
)

/** The kind of boundary. */
enum class BoundaryKind {
    /** `^` — start of string. */
    Start,
    /** `$` — end of string. */
    End,
    /** `%` — word boundary. */
    Word,
    /** `!%` — not word boundary. */
    NotWord,
    /** `<` — beginning of word. */
    WordStart,
    /** `>` — end of word. */
    WordEnd,
}
