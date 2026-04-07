package ru.kode.pomskykt.regex

/**
 * Removes redundant alternation branches where one branch subsumes another.
 *
 * Conservative approach: only eliminates what is provably safe.
 * - Exact duplicate alternatives (structural equality)
 * - CharSet subsumption (one char set fully contains another)
 * - Dot subsumption (dot covers any single non-dot element)
 */
internal fun Regex.eliminateDeadBranches(): Regex = when (this) {
    is Regex.Alt -> eliminateAlt(this)
    is Regex.Sequence -> Regex.Sequence(parts.map { it.eliminateDeadBranches() })
    is Regex.Group -> Regex.Group(
        RegexGroup(group.parts.map { it.eliminateDeadBranches() }, group.kind)
    )
    is Regex.Rep -> Regex.Rep(
        RegexRepetition(
            repetition.inner.eliminateDeadBranches(),
            repetition.lower,
            repetition.upper,
            repetition.greedy,
        )
    )
    is Regex.Look -> Regex.Look(
        RegexLookaround(lookaround.kind, lookaround.inner.eliminateDeadBranches())
    )
    is Regex.ModeGroup -> Regex.ModeGroup(flags, inner.eliminateDeadBranches())
    else -> this
}

private fun eliminateAlt(alt: Regex.Alt): Regex {
    // First, recurse into each alternative
    val alternatives = alt.alternation.alternatives.map { it.eliminateDeadBranches() }

    // Remove exact duplicates (keep first occurrence)
    val unique = alternatives.distinct()

    // Remove subsumed alternatives
    val result = unique.filterIndexed { i, candidate ->
        // Keep candidate unless some other alternative subsumes it
        unique.indices.none { j ->
            j != i && subsumes(unique[j], candidate)
        }
    }

    return when (result.size) {
        0 -> Regex.Alt(RegexAlternation(alternatives))
        1 -> result[0]
        else -> Regex.Alt(RegexAlternation(result))
    }
}

/**
 * Returns true if [a] provably subsumes [b], meaning [b] is redundant when [a] is present.
 */
private fun subsumes(a: Regex, b: Regex): Boolean {
    // Dot subsumes any single-element non-dot alternative
    if (a is Regex.Dot && isSingleElement(b) && b !is Regex.Dot) {
        return true
    }

    // CharSet subsumption
    if (a is Regex.CharSet && b is Regex.CharSet && !a.set.negative && !b.set.negative) {
        return charSetCovers(a.set, b.set)
    }

    return false
}

/**
 * Returns true if the regex represents a single-element match (one character).
 */
private fun isSingleElement(r: Regex): Boolean = when (r) {
    is Regex.CharSet -> true
    is Regex.Literal -> r.content.length == 1
    is Regex.Dot -> true
    else -> false
}

/**
 * Returns true if char set [a] fully covers char set [b],
 * meaning every character matched by [b] is also matched by [a].
 */
private fun charSetCovers(a: RegexCharSet, b: RegexCharSet): Boolean {
    // Every item in b must be covered by some item in a
    return b.items.all { bItem ->
        a.items.any { aItem -> itemCoversItem(aItem, bItem) }
    }
}

private fun itemCoversItem(a: RegexCharSetItem, b: RegexCharSetItem): Boolean = when {
    a == b -> true
    a is RegexCharSetItem.Range && b is RegexCharSetItem.Range ->
        a.first <= b.first && a.last >= b.last
    a is RegexCharSetItem.Range && b is RegexCharSetItem.Char ->
        b.char in a.first..a.last
    else -> false
}
