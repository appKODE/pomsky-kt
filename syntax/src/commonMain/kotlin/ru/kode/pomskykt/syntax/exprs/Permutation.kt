package ru.kode.pomskykt.syntax.exprs

import ru.kode.pomskykt.syntax.Span

data class Permutation(
    val rules: List<Rule>,
    val span: Span,
)
