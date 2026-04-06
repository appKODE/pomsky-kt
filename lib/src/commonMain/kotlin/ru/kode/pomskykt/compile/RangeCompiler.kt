package ru.kode.pomskykt.compile

import ru.kode.pomskykt.diagnose.CompileErrorKind
import ru.kode.pomskykt.regex.Regex as RIR
import ru.kode.pomskykt.regex.RegexAlternation
import ru.kode.pomskykt.regex.RegexCharSet
import ru.kode.pomskykt.regex.RegexCharSetItem
import ru.kode.pomskykt.regex.RegexGroup
import ru.kode.pomskykt.regex.RegexGroupKind
import ru.kode.pomskykt.regex.RegexRepetition

/**
 * Compiles a [Range][ru.kode.pomskykt.syntax.exprs.Range] AST node to [RIR] using
 * the digit-by-digit recursive algorithm.
 *
 * Ported from pomsky-lib/src/exprs/range.rs.
 */
private const val MAX_RANGE_DIGITS = 6

internal fun compileRangeExpr(range: ru.kode.pomskykt.syntax.exprs.Range): RIR {
    if (range.start.size > MAX_RANGE_DIGITS || range.end.size > MAX_RANGE_DIGITS) {
        throw CompileException(
            CompileErrorKind.RangeIsTooBig(MAX_RANGE_DIGITS),
            range.span,
        )
    }
    val result = range(range.start, range.end, isFirst = true, radix = range.radix)
    return result.toRIR()
}

// ---------------------------------------------------------------------------
// Internal IR for range compilation (mirrors Rust's private Rule enum)
// ---------------------------------------------------------------------------

private sealed class RangeRule {
    data object Empty : RangeRule()
    data class Class(val start: Int, val end: Int) : RangeRule()
    data class Repeat(val rule: RangeRule, val min: Int, val max: Int) : RangeRule()
    data class Alt(val alternatives: List<List<RangeRule>>) : RangeRule()
}

// ---------------------------------------------------------------------------
// Factory helpers
// ---------------------------------------------------------------------------

private fun rangeClass(start: Int, end: Int): RangeRule = RangeRule.Class(start, end)

private fun RangeRule.repeat(min: Int, max: Int): RangeRule {
    if (max == 0) return RangeRule.Empty
    if (min == 1 && max == 1) return this
    return RangeRule.Repeat(this, min, max)
}

private fun RangeRule.optional(): RangeRule = when {
    this is RangeRule.Repeat && this.min <= 1 -> RangeRule.Repeat(this.rule, 0, this.max)
    else -> this.repeat(0, 1)
}

// ---------------------------------------------------------------------------
// Core recursive algorithm
// ---------------------------------------------------------------------------

private fun range(a: ByteArray, b: ByteArray, isFirst: Boolean, radix: Int): RangeRule {
    val hiDigit = radix - 1
    val loDigit = if (isFirst) 1 else 0

    // Base cases
    if (a.isEmpty() && b.isEmpty()) return RangeRule.Empty
    if (a.isNotEmpty() && b.isEmpty()) {
        error("Unexpected error compiling a range. This is a bug!")
    }
    if (a.isEmpty() && b.isNotEmpty()) {
        return range(byteArrayOf(0), b, isFirst = false, radix).optional()
    }
    if (a.size == 1 && b.size == 1) {
        return rangeClass(a[0].toInt(), b[0].toInt())
    }

    // Recursive case: both have >= 1 element, at least one has length > 1
    val ax = a[0].toInt()
    val bx = b[0].toInt()
    val aRest = a.copyOfRange(1, a.size)
    val bRest = b.copyOfRange(1, b.size)
    val min = minOf(ax, bx)
    val max = maxOf(ax, bx)

    val alternatives = mutableListOf<MutableList<RangeRule>>()

    // Alternative 1: digits below min (with longer suffix)
    if (min > loDigit && aRest.size < bRest.size) {
        alternatives += mutableListOf(
            rangeClass(loDigit, min - 1),
            rangeClass(0, hiDigit).repeat(aRest.size + 1, bRest.size),
        )
    }

    when {
        // ax == bx
        ax == bx -> {
            alternatives += mutableListOf(
                rangeClass(ax, bx),
                range(aRest, bRest, isFirst = false, radix),
            )
        }
        // ax < bx
        ax < bx -> {
            if (isFirst && ax == 0 && aRest.isEmpty()) {
                // Add zero once as a standalone alternative
                alternatives += mutableListOf(rangeClass(0, 0))
            } else {
                // Alt 2: digit = min, recurse with all-max suffix
                alternatives += mutableListOf(
                    rangeClass(min, min),
                    range(aRest, ByteArray(bRest.size) { hiDigit.toByte() }, isFirst = false, radix),
                )
            }
            if (max - min >= 2) {
                // Alt 3: middle digits
                alternatives += mutableListOf(
                    rangeClass(min + 1, max - 1),
                    rangeClass(0, hiDigit).repeat(aRest.size, bRest.size),
                )
            }
            // Alt 4: digit = max, recurse with all-zero lower bound
            alternatives += mutableListOf(
                rangeClass(max, max),
                range(ByteArray(aRest.size) { 0 }, bRest, isFirst = false, radix),
            )
        }
        // ax > bx
        else -> {
            // Alt 2: digit = min = bx, recurse with all-zero lower bound
            alternatives += mutableListOf(
                rangeClass(min, min),
                range(ByteArray(a.size) { 0 }, bRest, isFirst = false, radix),
            )
            if (max - min >= 2 && aRest.size + 2 <= bRest.size) {
                // Alt 3: middle digits
                alternatives += mutableListOf(
                    rangeClass(min + 1, max - 1),
                    rangeClass(0, hiDigit).repeat(aRest.size + 1, bRest.size - 1),
                )
            }
            // Alt 4: digit = max = ax
            alternatives += mutableListOf(
                rangeClass(max, max),
                range(aRest, ByteArray(bRest.size - 1) { hiDigit.toByte() }, isFirst = false, radix),
            )
        }
    }

    // Alternative 5: digits above max (with shorter suffix)
    if (max < hiDigit && aRest.size < bRest.size) {
        alternatives += mutableListOf(
            rangeClass(max + 1, hiDigit),
            rangeClass(0, hiDigit).repeat(aRest.size, bRest.size - 1),
        )
    }

    return mergeAndOptimizeAlternatives(alternatives)
}

// ---------------------------------------------------------------------------
// Merge and optimize
// ---------------------------------------------------------------------------

private fun mergeAndOptimizeAlternatives(
    alternatives: MutableList<MutableList<RangeRule>>,
): RangeRule {
    val acc = mutableListOf<MutableList<RangeRule>>()

    for (rules in alternatives) {
        // Pre-process: collapse identical pairs or drop trailing Empty
        if (rules.size == 2) {
            if (rules[0] == rules[1]) {
                val rule = rules.removeAt(1)
                rules[0] = rule.repeat(2, 2)
            } else if (rules[1] == RangeRule.Empty) {
                rules.removeAt(1)
            }
        }

        // Attempt to merge with last accumulated entry
        val merged = tryMergeWithLast(acc, rules)
        if (!merged) {
            acc += rules
        }
    }

    return if (acc.size == 1 && acc[0].size == 1) {
        acc[0][0]
    } else {
        RangeRule.Alt(acc.map { it.toList() })
    }
}

private fun tryMergeWithLast(
    acc: MutableList<MutableList<RangeRule>>,
    rules: MutableList<RangeRule>,
): Boolean {
    if (acc.isEmpty()) return false
    val last = acc.last()

    // Both must be [Class, tail] to merge
    if (last.size != 2 || rules.size != 2) return false
    val prevClass = last[0] as? RangeRule.Class ?: return false
    val thisClass = rules[0] as? RangeRule.Class ?: return false

    // Tails must be structurally equal and classes consecutive
    if (last[1] != rules[1]) return false
    if (prevClass.end + 1 != thisClass.start) return false

    // Merge: extend the previous class
    last[0] = RangeRule.Class(prevClass.start, thisClass.end)

    // Post-merge normalization
    if (last.size == 2) {
        if (last[0] == last[1]) {
            val rule = last.removeAt(1)
            last[0] = rule.repeat(2, 2)
        } else {
            val last2 = last[1]
            if (last2 is RangeRule.Repeat && last2.rule == last[0]) {
                val (rMin, rMax) = last2.min to last2.max
                last.removeAt(1)
                val rule = last.removeAt(0)
                last += rule.repeat(rMin + 1, rMax + 1)
            }
        }
    }

    return true
}

// ---------------------------------------------------------------------------
// Conversion to Regex IR
// ---------------------------------------------------------------------------

private fun RangeRule.toRIR(): RIR = when (this) {
    is RangeRule.Empty -> RIR.Literal("")
    is RangeRule.Class -> digitClassToRIR(start, end)
    is RangeRule.Repeat -> RIR.Rep(RegexRepetition(
        inner = rule.toRIR(),
        lower = min,
        upper = max,
        greedy = true,
    ))
    is RangeRule.Alt -> {
        // Each alternative is wrapped in a Group(NonCapturing) matching Rust's
        // Alt.to_regex() which wraps each Vec<Rule> in Group(Normal).
        // The group codegen handles parens-in-sequence for Alts within multi-part groups.
        RIR.Alt(RegexAlternation(
            alternatives.map { parts ->
                RIR.Group(RegexGroup(
                    parts.map { it.toRIR() },
                    RegexGroupKind.NonCapturing,
                ))
            }
        ))
    }
}

private fun digitClassToRIR(start: Int, end: Int): RIR {
    // Single decimal digit: emit as literal
    if (start == end && start <= 9) {
        return RIR.Literal(('0' + start).toString())
    }

    val items = mutableListOf<RegexCharSetItem>()
    when {
        end <= 9 -> {
            items += RegexCharSetItem.Range(('0' + start).toChar(), ('0' + end).toChar())
        }
        start >= 10 -> {
            items += RegexCharSetItem.Range(
                ('A'.code + start - 10).toChar(),
                ('A'.code + end - 10).toChar(),
            )
            items += RegexCharSetItem.Range(
                ('a'.code + start - 10).toChar(),
                ('a'.code + end - 10).toChar(),
            )
        }
        else -> {
            // Mixed: start <= 9 < end
            items += RegexCharSetItem.Range(('0' + start).toChar(), '9')
            items += RegexCharSetItem.Range('A', ('A'.code + end - 10).toChar())
            items += RegexCharSetItem.Range('a', ('a'.code + end - 10).toChar())
        }
    }
    return RIR.CharSet(RegexCharSet(items, negative = false))
}
