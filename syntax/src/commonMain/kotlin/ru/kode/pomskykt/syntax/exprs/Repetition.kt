package ru.kode.pomskykt.syntax.exprs

import ru.kode.pomskykt.syntax.Span

/** Repetition: `rule{n,m}`, `rule+`, `rule*`, `rule?`. */
data class Repetition(
    val rule: Rule,
    val kind: RepetitionKind,
    val quantifier: Quantifier,
    val span: Span,
)

/** The bounds of a repetition. */
data class RepetitionKind(
    val lowerBound: Int,
    val upperBound: Int?, // null = infinity
) {
    companion object {
        /** `*` — zero or more. */
        fun zeroInf() = RepetitionKind(0, null)
        /** `+` — one or more. */
        fun oneInf() = RepetitionKind(1, null)
        /** `?` — zero or one. */
        fun zeroOne() = RepetitionKind(0, 1)
        /** `{n}` — exactly n. */
        fun fixed(n: Int) = RepetitionKind(n, n)
    }
}

/** Whether a repetition is greedy or lazy. */
enum class Quantifier {
    Greedy,
    Lazy,
    DefaultGreedy,
    DefaultLazy,
}
