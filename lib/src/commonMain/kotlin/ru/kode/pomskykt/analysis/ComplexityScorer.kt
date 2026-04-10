package ru.kode.pomskykt.analysis

import ru.kode.pomskykt.regex.Regex
import ru.kode.pomskykt.regex.RegexAlternation
import ru.kode.pomskykt.regex.RegexGroup
import ru.kode.pomskykt.regex.RegexRepetition

/**
 * Walks the [Regex] IR tree and produces a complexity score from 1 to 10
 * with a detailed breakdown of contributing factors.
 *
 * The score reflects structural complexity and potential performance pitfalls
 * (e.g., nested unbounded quantifiers that can cause catastrophic backtracking).
 */
data class ComplexityReport(
    val score: Int,
    val level: Level,
    val factors: List<Factor>,
) {
    enum class Level { LOW, MEDIUM, HIGH }

    data class Factor(
        val description: String,
        val points: Int,
    )
}

object ComplexityScorer {

    /**
     * Score the complexity of a compiled [Regex] IR tree.
     *
     * @return a [ComplexityReport] with a clamped 1..10 score, level, and factor breakdown.
     */
    fun score(regex: Regex): ComplexityReport {
        val factors = mutableListOf<ComplexityReport.Factor>()
        analyze(regex, factors, repDepth = 0)
        val rawScore = factors.sumOf { it.points }
        val clamped = rawScore.coerceIn(1, 10)
        val level = when {
            clamped <= 3 -> ComplexityReport.Level.LOW
            clamped <= 6 -> ComplexityReport.Level.MEDIUM
            else -> ComplexityReport.Level.HIGH
        }
        return ComplexityReport(clamped, level, factors)
    }

    private fun analyze(
        regex: Regex,
        factors: MutableList<ComplexityReport.Factor>,
        repDepth: Int,
    ) {
        when (regex) {
            is Regex.Rep -> {
                val isUnbounded = regex.repetition.upper == null
                val newDepth = if (isUnbounded) repDepth + 1 else repDepth

                // Nested unbounded quantifiers (ReDoS risk)
                if (isUnbounded && repDepth > 0) {
                    factors.add(
                        ComplexityReport.Factor(
                            "Nested unbounded quantifiers (ReDoS risk)",
                            5,
                        )
                    )
                }

                // Quantifier nesting depth beyond 1
                if (newDepth > 1 && isUnbounded) {
                    factors.add(
                        ComplexityReport.Factor(
                            "Quantifier nesting depth: $newDepth",
                            2 * (newDepth - 1),
                        )
                    )
                }

                analyze(regex.repetition.inner, factors, newDepth)
            }
            is Regex.Alt -> {
                val count = regex.alternation.alternatives.size
                if (count >= 5) {
                    factors.add(
                        ComplexityReport.Factor(
                            "$count alternation branches",
                            count / 5,
                        )
                    )
                }
                regex.alternation.alternatives.forEach { analyze(it, factors, repDepth) }
            }
            is Regex.Sequence -> {
                regex.parts.forEach { analyze(it, factors, repDepth) }
            }
            is Regex.Group -> {
                regex.group.parts.forEach { analyze(it, factors, repDepth) }
            }
            is Regex.Look -> {
                factors.add(
                    ComplexityReport.Factor(
                        "Lookaround assertion",
                        1,
                    )
                )
                analyze(regex.lookaround.inner, factors, repDepth)
            }
            is Regex.Ref -> {
                factors.add(
                    ComplexityReport.Factor(
                        "Backreference",
                        1,
                    )
                )
            }
            is Regex.ModeGroup -> {
                analyze(regex.inner, factors, repDepth)
            }
            is Regex.CompoundCharSet -> {
                factors.add(
                    ComplexityReport.Factor(
                        "Character set intersection",
                        1,
                    )
                )
            }
            is Regex.Recursion -> {
                factors.add(
                    ComplexityReport.Factor(
                        "Recursive pattern",
                        3,
                    )
                )
            }
            // Leaf nodes: Literal, CharSet, Dot, Grapheme, Bound, Unescaped
            else -> {}
        }
    }
}
