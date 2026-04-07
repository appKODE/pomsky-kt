package ru.kode.pomskykt.regex

import ru.kode.pomskykt.syntax.exprs.LookaroundKind

/**
 * Optimizes lookaround assertions in the Regex IR.
 *
 * Techniques applied:
 * - Technique 2: Outline boundary at start of positive lookahead
 *   `(?=^test)` at start of sequence -> `^(?=test)`
 * - Technique 5: Remove redundant positive lookahead when followed by same expression
 *   `(?=X)X` -> `X`
 */
internal fun Regex.optimizeAssertions(): Regex = when (this) {
    is Regex.Sequence -> optimizeSequenceAssertions(this)
    is Regex.Group -> {
        val optimizedParts = optimizeAdjacentAssertions(
            group.parts.map { it.optimizeAssertions() }
        )
        Regex.Group(RegexGroup(optimizedParts, group.kind))
    }
    is Regex.Alt -> Regex.Alt(
        RegexAlternation(alternation.alternatives.map { it.optimizeAssertions() })
    )
    is Regex.Rep -> Regex.Rep(
        RegexRepetition(
            repetition.inner.optimizeAssertions(),
            repetition.lower,
            repetition.upper,
            repetition.greedy,
        )
    )
    is Regex.Look -> Regex.Look(
        RegexLookaround(lookaround.kind, lookaround.inner.optimizeAssertions())
    )
    is Regex.ModeGroup -> Regex.ModeGroup(flags, inner.optimizeAssertions())
    else -> this
}

/**
 * Applies technique 5 (redundant lookahead removal) to a list of adjacent parts.
 * Returns the optimized list.
 */
private fun optimizeAdjacentAssertions(parts: List<Regex>): List<Regex> {
    val result = parts.toMutableList()
    var i = 0
    while (i < result.size - 1) {
        val current = result[i]
        val next = result[i + 1]
        if (current is Regex.Look &&
            current.lookaround.kind == LookaroundKind.Ahead
        ) {
            val inner = current.lookaround.inner
            if (inner == next) {
                result.removeAt(i)
                continue
            }
            // Literal prefix case: lookahead of "x" before "xy..." is redundant
            if (inner is Regex.Literal && next is Regex.Literal &&
                next.content.startsWith(inner.content)
            ) {
                result.removeAt(i)
                continue
            }
        }
        i++
    }
    return result
}

private fun optimizeSequenceAssertions(seq: Regex.Sequence): Regex {
    val parts = optimizeAdjacentAssertions(
        seq.parts.map { it.optimizeAssertions() }
    ).toMutableList()

    // Technique 2: Outline boundary at start of positive lookahead at sequence start
    // (?=^test) at start -> ^(?=test)
    if (parts.isNotEmpty()) {
        val first = parts[0]
        if (first is Regex.Look && first.lookaround.kind == LookaroundKind.Ahead) {
            val inner = first.lookaround.inner
            if (inner is Regex.Sequence && inner.parts.isNotEmpty() && inner.parts[0] is Regex.Bound) {
                val boundary = inner.parts[0]
                val restInner = if (inner.parts.size == 2) {
                    inner.parts[1]
                } else {
                    Regex.Sequence(inner.parts.drop(1))
                }
                parts[0] = Regex.Look(RegexLookaround(LookaroundKind.Ahead, restInner))
                parts.add(0, boundary)
            }
        }
    }

    return if (parts.size == 1) parts[0] else Regex.Sequence(parts)
}
