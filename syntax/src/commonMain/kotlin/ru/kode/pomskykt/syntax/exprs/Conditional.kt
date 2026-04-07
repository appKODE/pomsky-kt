package ru.kode.pomskykt.syntax.exprs

import ru.kode.pomskykt.syntax.Span

/**
 * A conditional expression: `if (>> 'cond') 'yes' else 'no'`.
 *
 * The condition must be a lookaround assertion.
 */
data class Conditional(
    val condition: Rule,
    val thenBranch: Rule,
    val elseBranch: Rule?,
    val span: Span,
)
