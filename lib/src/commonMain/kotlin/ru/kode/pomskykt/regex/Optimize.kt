package ru.kode.pomskykt.regex

/**
 * Optimization pass for the regex IR.
 *
 * Simplifies the IR by removing empty nodes, unwrapping single-element
 * groups/alternations, merging adjacent char sets, and reducing nested
 * repetitions.
 *
 * Ported from pomsky-lib/src/regex/optimize.rs.
 */
fun Regex.optimize(): Regex? {
    val (result, count) = optimizeInner()
    return when (count) {
        Count.Zero -> null
        Count.One, Count.Many -> result
    }
}

private enum class Count { Zero, One, Many }

private operator fun Count.plus(other: Count): Count = when {
    this == Count.Zero && other == Count.Zero -> Count.Zero
    this == Count.Zero && other == Count.One -> Count.One
    this == Count.One && other == Count.Zero -> Count.One
    else -> Count.Many
}

private fun Regex.optimizeInner(): Pair<Regex, Count> = when (this) {
    is Regex.Literal -> when {
        content.isEmpty() -> this to Count.Zero
        content.length == 1 -> this to Count.One
        else -> this to Count.Many
    }
    is Regex.Unescaped -> this to Count.Many
    is Regex.CharSet -> this to Count.One
    is Regex.CompoundCharSet -> this to Count.One
    is Regex.Grapheme -> this to Count.One
    is Regex.Dot -> this to Count.One
    is Regex.Bound -> this to Count.One
    is Regex.Ref -> this to Count.One
    is Regex.Recursion -> this to Count.One

    is Regex.Group -> optimizeGroup(group)
    is Regex.Sequence -> optimizeSequence(parts)
    is Regex.Alt -> optimizeAlternation(alternation)
    is Regex.Rep -> optimizeRepetition(repetition)
    is Regex.Look -> {
        val (inner, _) = lookaround.inner.optimizeInner()
        Regex.Look(RegexLookaround(lookaround.kind, inner)) to Count.One
    }
}

private fun optimizeSequence(parts: List<Regex>): Pair<Regex, Count> {
    val optimized = parts.mapNotNull { part ->
        val (opt, count) = part.optimizeInner()
        if (count == Count.Zero) null else opt
    }
    return when (optimized.size) {
        0 -> Regex.Literal("") to Count.Zero
        1 -> optimized[0].optimizeInner()
        else -> Regex.Sequence(optimized) to Count.Many
    }
}

private fun optimizeGroup(group: RegexGroup): Pair<Regex, Count> {
    val parts = group.parts.mapNotNull { part ->
        val (opt, count) = part.optimizeInner()
        if (count == Count.Zero) null else opt
    }

    if (parts.isEmpty()) {
        // Capturing groups must be preserved even when empty: :() → ()
        if (group.kind is RegexGroupKind.Numbered || group.kind is RegexGroupKind.Named) {
            return Regex.Group(RegexGroup(emptyList(), group.kind)) to Count.One
        }
        return Regex.Literal("") to Count.Zero
    }

    // Unwrap single-element non-capturing group, unless the single element is
    // an Unescaped (raw regex needs wrapping). Alternation is safe to unwrap because
    // the parent sequence/group codegen handles (?:...) wrapping via needsParensInSequence.
    if (parts.size == 1 && group.kind is RegexGroupKind.NonCapturing) {
        val single = parts[0]
        if (single !is Regex.Unescaped) {
            return single.optimizeInner()
        }
    }

    // For capturing groups, always return Count.One
    val count = when {
        group.kind is RegexGroupKind.Numbered || group.kind is RegexGroupKind.Named -> Count.One
        parts.isEmpty() -> Count.Zero
        else -> parts.fold(Count.Zero) { acc, part ->
            val (_, c) = part.optimizeInner()
            acc + c
        }
    }

    return Regex.Group(RegexGroup(parts, group.kind)) to count
}

private fun optimizeAlternation(alt: RegexAlternation): Pair<Regex, Count> {
    val parts = alt.alternatives.toMutableList()

    // Check for empty first alternative: '' | x -> x? lazy
    if (parts.isNotEmpty()) {
        val first = parts[0]
        if (first is Regex.Literal && first.content.isEmpty()) {
            parts.removeAt(0)
            val inner = Regex.Alt(RegexAlternation(parts))
            val rep = Regex.Rep(RegexRepetition(inner, 0, 1, greedy = false))
            return rep.optimizeInner()
        }
    }

    // Check for empty last alternative: x | '' -> x? greedy
    if (parts.isNotEmpty()) {
        val last = parts.last()
        if (last is Regex.Literal && last.content.isEmpty()) {
            parts.removeAt(parts.lastIndex)
            val inner = Regex.Alt(RegexAlternation(parts))
            val rep = Regex.Rep(RegexRepetition(inner, 0, 1, greedy = true))
            return rep.optimizeInner()
        }
    }

    // Optimize each part
    val optimized = parts.map { it.optimizeInner().first }

    // Merge single-char alternatives into a char set
    val merged = mergeCharAlternatives(optimized)

    return when {
        merged.isEmpty() -> Regex.Literal("") to Count.Zero
        merged.size == 1 -> merged[0].optimizeInner()
        else -> Regex.Alt(RegexAlternation(merged)) to Count.One
    }
}

private fun optimizeRepetition(rep: RegexRepetition): Pair<Regex, Count> {
    // {1,1} repetition is identity
    if (rep.lower == 1 && rep.upper == 1) {
        return rep.inner.optimizeInner()
    }

    val (inner, count) = rep.inner.optimizeInner()

    // Empty content removes the repetition
    if (count == Count.Zero) {
        return Regex.Literal("") to Count.Zero
    }

    // Try to reduce nested repetitions: Rep(Rep(x, a, b), c, d) -> Rep(x, reduced)
    if (count == Count.One && inner is Regex.Rep) {
        val innerRep = inner.repetition
        if (innerRep.greedy == rep.greedy) {
            val reduced = reduceRepetitions(
                outerLower = rep.lower, outerUpper = rep.upper,
                innerLower = innerRep.lower, innerUpper = innerRep.upper,
            )
            if (reduced != null) {
                val newRep = Regex.Rep(
                    RegexRepetition(innerRep.inner, reduced.first, reduced.second, rep.greedy)
                )
                return newRep to Count.One
            }
        }
    }

    return Regex.Rep(RegexRepetition(inner, rep.lower, rep.upper, rep.greedy)) to Count.One
}

/**
 * Try to reduce nested repetitions into a single one.
 * Returns (lower, upper) pair or null if reduction is not possible.
 */
private fun reduceRepetitions(
    outerLower: Int,
    outerUpper: Int?,
    innerLower: Int,
    innerUpper: Int?,
): Pair<Int, Int?>? {
    // Case 1: Either has unbounded upper
    if ((outerLower in 0..1 && innerLower in 0..1 && innerUpper == null) ||
        (outerLower in 0..1 && outerUpper == null && innerLower in 0..1)
    ) {
        val lower = minOf(innerLower, outerLower)
        return lower to null
    }

    // Case 2: Outer is {0,1} and inner has bounded upper, or vice versa
    if (outerLower in 0..1 && outerUpper != null && innerLower == 0 && innerUpper == 1) {
        return 0 to outerUpper
    }
    if (outerLower == 0 && outerUpper == 1 && innerLower in 0..1 && innerUpper != null) {
        return 0 to innerUpper
    }

    // Case 3: Both unbounded
    if (outerUpper == null && innerUpper == null) {
        val mul = mulRepetitions(outerLower, innerLower) ?: return null
        return mul to null
    }

    // Case 4: Both exact (lower == upper)
    if (outerUpper != null && outerLower == outerUpper &&
        innerUpper != null && innerLower == innerUpper
    ) {
        val mul = mulRepetitions(outerLower, innerLower) ?: return null
        return mul to mul
    }

    // Case 5: Both bounded with low 0 or 1
    if (outerLower in 0..1 && outerUpper != null &&
        innerLower in 0..1 && innerUpper != null
    ) {
        val lower = minOf(innerLower, outerLower)
        val upper = mulRepetitions(outerUpper, innerUpper) ?: return null
        return lower to upper
    }

    return null
}

private fun mulRepetitions(a: Int, b: Int): Int? {
    val res = a.toLong() * b.toLong()
    return if (res > 65535) null else res.toInt()
}

/**
 * Merge single-character Literal alternatives into a CharSet.
 */
private fun mergeCharAlternatives(parts: List<Regex>): List<Regex> {
    val chars = mutableListOf<Char>()
    val others = mutableListOf<Regex>()

    for (part in parts) {
        if (part is Regex.Literal && part.content.length == 1) {
            chars.add(part.content[0])
        } else {
            others.add(part)
        }
    }

    if (chars.size <= 1) return parts // Not worth merging

    // Sort and merge consecutive chars into ranges
    chars.sort()
    val items = mutableListOf<RegexCharSetItem>()
    var i = 0
    while (i < chars.size) {
        val start = chars[i]
        var end = start
        while (i + 1 < chars.size && chars[i + 1].code == end.code + 1) {
            i++
            end = chars[i]
        }
        if (end == start) {
            items.add(RegexCharSetItem.Char(start))
        } else {
            items.add(RegexCharSetItem.Range(start, end))
        }
        i++
    }
    val charSet = Regex.CharSet(RegexCharSet(items, negative = false))
    return listOf(charSet) + others
}
