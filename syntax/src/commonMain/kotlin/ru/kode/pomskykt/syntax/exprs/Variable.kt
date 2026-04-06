package ru.kode.pomskykt.syntax.exprs

import ru.kode.pomskykt.syntax.Span

/** Named variable reference. */
data class Variable(
    val name: String,
    val span: Span,
)
