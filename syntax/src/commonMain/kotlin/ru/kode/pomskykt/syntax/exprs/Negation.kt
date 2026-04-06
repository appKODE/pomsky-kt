package ru.kode.pomskykt.syntax.exprs

import ru.kode.pomskykt.syntax.Span

/** Negated expression: `!expr`. */
data class Negation(
    val rule: Rule,
    val notSpan: Span,
)
