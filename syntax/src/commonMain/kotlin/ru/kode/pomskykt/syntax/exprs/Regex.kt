package ru.kode.pomskykt.syntax.exprs

import ru.kode.pomskykt.syntax.Span

/** Unescaped inline regex: `regex '...'`. */
data class Regex(
    val content: String,
    val span: Span,
)
