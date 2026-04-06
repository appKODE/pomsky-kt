package ru.kode.pomskykt.syntax.exprs

import ru.kode.pomskykt.syntax.Span

/** Backreference or forward reference: `::name`, `::1`. */
data class Reference(
    val target: ReferenceTarget,
    val span: Span,
)

/** The target of a reference. */
sealed class ReferenceTarget {
    /** Named group reference: `::name`. */
    data class Named(val name: String) : ReferenceTarget()
    /** Numbered group reference: `::1`. */
    data class Number(val number: Int) : ReferenceTarget()
    /** Relative reference: `::+1`, `::-1`. */
    data class Relative(val offset: Int) : ReferenceTarget()
}
