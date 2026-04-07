package ru.kode.pomskykt.regex

/**
 * Auto-atomicization optimization pass.
 *
 * Inserts atomic groups `(?>...)` around greedy unbounded repetitions where
 * the repeated content and the following element have disjoint leading character
 * sets — meaning backtracking into the repetition can never lead to a match.
 *
 * This is a conservative pass: when uncertain about character overlap, it does
 * NOT atomize (safe default).
 */
internal fun Regex.autoAtomize(): Regex = when (this) {
    is Regex.Sequence -> {
        Regex.Sequence(atomizeAdjacentParts(parts))
    }
    is Regex.Group -> Regex.Group(RegexGroup(atomizeAdjacentParts(group.parts), group.kind))
    is Regex.Alt -> Regex.Alt(RegexAlternation(alternation.alternatives.map { it.autoAtomize() }))
    is Regex.Rep -> Regex.Rep(
        RegexRepetition(repetition.inner.autoAtomize(), repetition.lower, repetition.upper, repetition.greedy)
    )
    is Regex.Look -> Regex.Look(RegexLookaround(lookaround.kind, lookaround.inner.autoAtomize()))
    is Regex.ModeGroup -> Regex.ModeGroup(flags, inner.autoAtomize())
    else -> this
}

/**
 * Process a list of adjacent parts, atomizing greedy unbounded repetitions
 * where the following element has disjoint leading characters.
 */
private fun atomizeAdjacentParts(parts: List<Regex>): List<Regex> {
    val newParts = mutableListOf<Regex>()
    for (i in parts.indices) {
        val part = parts[i].autoAtomize()
        if (i + 1 < parts.size && canAtomizeAt(part, parts[i + 1])) {
            newParts.add(wrapAtomic(part))
        } else {
            newParts.add(part)
        }
    }
    return newParts
}

/**
 * Returns true if [current] is a greedy unbounded repetition whose content
 * is character-disjoint from [next], making it safe to wrap in an atomic group.
 */
private fun canAtomizeAt(current: Regex, next: Regex): Boolean {
    if (current !is Regex.Rep) return false
    val rep = current.repetition
    if (!rep.greedy || rep.upper != null) return false
    val currentKind = leadingCharKind(rep.inner)
    val nextKind = leadingCharKind(next)
    return areDisjoint(currentKind, nextKind)
}

private fun wrapAtomic(rep: Regex): Regex {
    return Regex.Group(RegexGroup(listOf(rep), RegexGroupKind.Atomic))
}

/**
 * Conservative classification of the leading characters a regex node can match.
 */
private sealed class CharKind {
    /** Matches `\d` — digits 0-9. */
    data object Digits : CharKind()
    /** Matches `\w` — word characters (letters, digits, underscore). */
    data object Word : CharKind()
    /** Matches `\s` — whitespace. */
    data object Space : CharKind()
    /** Matches a specific set of literal characters. */
    data class Literal(val chars: Set<Char>) : CharKind()
    /** Matches anything (dot, unknown, or complex). Cannot prove disjointness. */
    data object Any : CharKind()
}

/**
 * Extract the leading character kind from a regex node.
 * Returns [CharKind.Any] when unsure (conservative).
 */
private fun leadingCharKind(regex: Regex): CharKind = when (regex) {
    is Regex.Literal -> {
        if (regex.content.isNotEmpty()) {
            CharKind.Literal(setOf(regex.content[0]))
        } else {
            CharKind.Any
        }
    }
    is Regex.CharSet -> charSetToKind(regex.set)
    is Regex.Dot -> CharKind.Any
    is Regex.Group -> {
        if (regex.group.parts.isNotEmpty()) {
            leadingCharKind(regex.group.parts[0])
        } else {
            CharKind.Any
        }
    }
    is Regex.Sequence -> {
        if (regex.parts.isNotEmpty()) {
            leadingCharKind(regex.parts[0])
        } else {
            CharKind.Any
        }
    }
    is Regex.Alt -> {
        val kinds = regex.alternation.alternatives.map { leadingCharKind(it) }
        mergeCharKinds(kinds)
    }
    is Regex.Rep -> leadingCharKind(regex.repetition.inner)
    is Regex.Unescaped -> CharKind.Any
    is Regex.Grapheme -> CharKind.Any
    is Regex.CompoundCharSet -> CharKind.Any
    is Regex.Bound -> CharKind.Any
    is Regex.Look -> CharKind.Any
    is Regex.Ref -> CharKind.Any
    is Regex.Recursion -> CharKind.Any
    is Regex.ModeGroup -> leadingCharKind(regex.inner)
}

private fun charSetToKind(set: RegexCharSet): CharKind {
    if (set.negative) return CharKind.Any
    if (set.items.size == 1) {
        return when (val item = set.items[0]) {
            is RegexCharSetItem.Shorthand -> shorthandToKind(item.shorthand)
            is RegexCharSetItem.Char -> CharKind.Literal(setOf(item.char))
            is RegexCharSetItem.Range -> {
                // Small ranges can be enumerated; large ones are too broad
                val size = item.last.code - item.first.code + 1
                if (size in 1..128) {
                    CharKind.Literal((item.first..item.last).toSet())
                } else {
                    CharKind.Any
                }
            }
            else -> CharKind.Any
        }
    }
    // Multiple items: try to merge
    val kinds = set.items.map { item ->
        when (item) {
            is RegexCharSetItem.Shorthand -> shorthandToKind(item.shorthand)
            is RegexCharSetItem.Char -> CharKind.Literal(setOf(item.char))
            else -> CharKind.Any
        }
    }
    return mergeCharKinds(kinds)
}

private fun shorthandToKind(sh: RegexShorthand): CharKind = when (sh) {
    RegexShorthand.Digit -> CharKind.Digits
    RegexShorthand.Word -> CharKind.Word
    RegexShorthand.Space -> CharKind.Space
    else -> CharKind.Any // negated shorthands match too broadly
}

/**
 * Merge multiple CharKinds into one. If any is [CharKind.Any], result is Any.
 * Merging Digits into Word yields Word (since Word is a superset).
 */
private fun mergeCharKinds(kinds: List<CharKind>): CharKind {
    if (kinds.isEmpty()) return CharKind.Any
    var result = kinds[0]
    for (i in 1 until kinds.size) {
        result = mergeTwo(result, kinds[i])
        if (result is CharKind.Any) return CharKind.Any
    }
    return result
}

private fun mergeTwo(a: CharKind, b: CharKind): CharKind = when {
    a is CharKind.Any || b is CharKind.Any -> CharKind.Any
    a == b -> a
    // Word is superset of Digits
    (a is CharKind.Word && b is CharKind.Digits) || (a is CharKind.Digits && b is CharKind.Word) -> CharKind.Word
    // Literal + Literal = merged Literal
    a is CharKind.Literal && b is CharKind.Literal -> CharKind.Literal(a.chars + b.chars)
    // Literal chars that are all digits -> compatible with Digits
    a is CharKind.Digits && b is CharKind.Literal && b.chars.all { it in '0'..'9' } -> CharKind.Digits
    a is CharKind.Literal && b is CharKind.Digits && a.chars.all { it in '0'..'9' } -> CharKind.Digits
    else -> CharKind.Any
}

/**
 * Returns true if two CharKinds are provably disjoint (no overlap).
 */
private fun areDisjoint(a: CharKind, b: CharKind): Boolean {
    if (a is CharKind.Any || b is CharKind.Any) return false
    return when {
        // Digits vs Space
        (a is CharKind.Digits && b is CharKind.Space) ||
        (a is CharKind.Space && b is CharKind.Digits) -> true
        // Word vs Space
        (a is CharKind.Word && b is CharKind.Space) ||
        (a is CharKind.Space && b is CharKind.Word) -> true
        // Same kind = overlaps
        a is CharKind.Digits && b is CharKind.Digits -> false
        a is CharKind.Word && b is CharKind.Word -> false
        a is CharKind.Space && b is CharKind.Space -> false
        // Digits vs Word: digits are subset of word
        (a is CharKind.Digits && b is CharKind.Word) ||
        (a is CharKind.Word && b is CharKind.Digits) -> false
        // Literal vs Literal
        a is CharKind.Literal && b is CharKind.Literal -> a.chars.intersect(b.chars).isEmpty()
        // Literal vs Digits
        a is CharKind.Literal && b is CharKind.Digits -> a.chars.none { it in '0'..'9' }
        a is CharKind.Digits && b is CharKind.Literal -> b.chars.none { it in '0'..'9' }
        // Literal vs Word (letters, digits, underscore)
        a is CharKind.Literal && b is CharKind.Word -> a.chars.none { it.isLetterOrDigit() || it == '_' }
        a is CharKind.Word && b is CharKind.Literal -> b.chars.none { it.isLetterOrDigit() || it == '_' }
        // Literal vs Space
        a is CharKind.Literal && b is CharKind.Space -> a.chars.none { it.isWhitespace() }
        a is CharKind.Space && b is CharKind.Literal -> b.chars.none { it.isWhitespace() }
        else -> false
    }
}
