package ru.kode.pomskykt.syntax.exprs

import ru.kode.pomskykt.syntax.Span

/** Group (sequence with optional capturing): `:(name)` or `(...)`. */
data class Group(
    val parts: List<Rule>,
    val kind: GroupKind,
    val span: Span,
)

/** The kind of a group. */
sealed class GroupKind {
    data class Capturing(val capture: Capture) : GroupKind()
    data object Atomic : GroupKind()
    data object Normal : GroupKind()
    data object Implicit : GroupKind()
}

/** Metadata for a capturing group. */
data class Capture(
    val name: String? = null,
)
