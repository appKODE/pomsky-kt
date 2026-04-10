package ru.kode.pomskykt.syntax.exprs

import ru.kode.pomskykt.syntax.Span

/**
 * The root AST node for a pomsky expression.
 *
 * Ported from pomsky-syntax/src/exprs/rule.rs.
 */
sealed class Rule {
    data class Lit(val literal: Literal) : Rule()
    data class Class(val charClass: CharClass) : Rule()
    data class Grp(val group: Group) : Rule()
    data class Alt(val alternation: Alternation) : Rule()
    data class Inter(val intersection: Intersection) : Rule()
    data class Rep(val repetition: Repetition) : Rule()
    data class Bound(val boundary: Boundary) : Rule()
    data class Look(val lookaround: Lookaround) : Rule()
    data class Var(val variable: Variable) : Rule()
    data class Ref(val reference: Reference) : Rule()
    data class Rng(val range: Range) : Rule()
    data class StmtE(val stmtExpr: StmtExpr) : Rule()
    data class Neg(val negation: Negation) : Rule()
    data class Rgx(val regex: Regex) : Rule()
    data class Recur(val recursion: Recursion) : Rule()
    data class Cond(val conditional: Conditional) : Rule()
    data class Perm(val permutation: Permutation) : Rule()
    data object Grapheme : Rule()
    data object Codepoint : Rule()
    data object Dot : Rule()
}

/** String literal: `'hello'` or `"hello"`. */
data class Literal(
    val content: String,
    val span: Span,
)

/** Alternation: `a | b | c`. */
data class Alternation(
    val rules: List<Rule>,
    val span: Span,
)

/** Intersection: `a & b & c`. */
data class Intersection(
    val rules: List<Rule>,
    val span: Span,
)
