package ru.kode.pomskykt.regex

/**
 * Factors common prefixes from alternation branches to produce smaller regex.
 *
 * Example: `'abc' | 'abd'` -> `ab(?:c|d)` instead of `abc|abd`.
 */
internal fun Regex.factorAlternations(): Regex = when (this) {
    is Regex.Alt -> factorAlt(this)
    is Regex.Sequence -> Regex.Sequence(parts.map { it.factorAlternations() })
    is Regex.Group -> Regex.Group(
        RegexGroup(group.parts.map { it.factorAlternations() }, group.kind)
    )
    is Regex.Rep -> Regex.Rep(
        RegexRepetition(
            repetition.inner.factorAlternations(),
            repetition.lower,
            repetition.upper,
            repetition.greedy,
        )
    )
    is Regex.Look -> Regex.Look(
        RegexLookaround(lookaround.kind, lookaround.inner.factorAlternations())
    )
    is Regex.ModeGroup -> Regex.ModeGroup(flags, inner.factorAlternations())
    else -> this
}

private fun factorAlt(alt: Regex.Alt): Regex {
    val alternatives = alt.alternation.alternatives.map { it.factorAlternations() }
    if (alternatives.size < 2) return Regex.Alt(RegexAlternation(alternatives))

    val prefix = findCommonLiteralPrefix(alternatives)
    if (prefix.isEmpty()) return Regex.Alt(RegexAlternation(alternatives))

    val stripped = alternatives.map { stripLiteralPrefix(it, prefix) }

    val prefixNode = Regex.Literal(prefix)
    val altNode = Regex.Alt(RegexAlternation(stripped))
    return Regex.Sequence(listOf(prefixNode, altNode))
}

private fun findCommonLiteralPrefix(alternatives: List<Regex>): String {
    val literals = alternatives.map { leadingLiteral(it) }
    if (literals.any { it == null }) return ""

    val strings = literals.filterNotNull()
    if (strings.isEmpty()) return ""

    var prefix = strings[0]
    for (s in strings.drop(1)) {
        while (!s.startsWith(prefix)) {
            prefix = prefix.dropLast(1)
            if (prefix.isEmpty()) return ""
        }
    }
    return prefix
}

private fun leadingLiteral(regex: Regex): String? = when (regex) {
    is Regex.Literal -> regex.content
    is Regex.Sequence -> {
        if (regex.parts.isNotEmpty() && regex.parts[0] is Regex.Literal) {
            (regex.parts[0] as Regex.Literal).content
        } else {
            null
        }
    }
    else -> null
}

private fun stripLiteralPrefix(regex: Regex, prefix: String): Regex = when (regex) {
    is Regex.Literal -> {
        val remaining = regex.content.removePrefix(prefix)
        Regex.Literal(remaining)
    }
    is Regex.Sequence -> {
        val first = regex.parts[0]
        if (first is Regex.Literal) {
            val remaining = first.content.removePrefix(prefix)
            val newParts = if (remaining.isEmpty()) {
                regex.parts.drop(1)
            } else {
                listOf(Regex.Literal(remaining)) + regex.parts.drop(1)
            }
            if (newParts.size == 1) newParts[0] else Regex.Sequence(newParts)
        } else {
            regex
        }
    }
    else -> regex
}
