package ru.kode.pomskykt.validation

import ru.kode.pomskykt.diagnose.Diagnostic
import ru.kode.pomskykt.diagnose.DiagnosticKind
import ru.kode.pomskykt.diagnose.Severity
import ru.kode.pomskykt.syntax.exprs.Alternation
import ru.kode.pomskykt.syntax.exprs.Group
import ru.kode.pomskykt.syntax.exprs.GroupKind
import ru.kode.pomskykt.syntax.exprs.Repetition
import ru.kode.pomskykt.syntax.exprs.Rule
import ru.kode.pomskykt.visitor.RuleVisitor

/**
 * Detects common mistakes and anti-patterns in Pomsky source.
 *
 * Unlike [Validator] which checks flavor compatibility, the linter
 * produces informational warnings that do not block compilation.
 */
class Linter : RuleVisitor {
    val warnings = mutableListOf<Diagnostic>()

    override fun visitAlternation(alternation: Alternation) {
        checkUnreachableAlternative(alternation)
    }

    override fun visitRepetition(repetition: Repetition) {
        checkUnnecessaryRepetition(repetition)
        checkRedundantNestedRepetition(repetition)
        checkQuantifierOnAnchor(repetition)
        checkQuantifierOnLookaround(repetition)
    }

    override fun visitGroup(group: Group) {
        checkEmptyGroup(group)
        checkUnnecessaryGroup(group)
    }

    /**
     * Rule 1: Unreachable alternative.
     *
     * In `'ab' | 'abc'`, the second alternative is unreachable because
     * `'ab'` always matches first (it is a prefix of `'abc'`).
     */
    private fun checkUnreachableAlternative(alternation: Alternation) {
        val rules = alternation.rules
        for (i in rules.indices) {
            val prefixContent = extractLiteralContent(rules[i]) ?: continue
            for (j in (i + 1) until rules.size) {
                val laterContent = extractLiteralContent(rules[j]) ?: continue
                if (laterContent.startsWith(prefixContent) && prefixContent.isNotEmpty()) {
                    warnings.add(
                        Diagnostic(
                            severity = Severity.Warning,
                            msg = "Alternative may be unreachable because a previous alternative always matches first",
                            span = alternation.span,
                            kind = DiagnosticKind.Other,
                        )
                    )
                    return
                }
            }
        }
    }

    /**
     * Rule 2: Unnecessary repetition `{1}`.
     *
     * `'x'{1}` is redundant — the repetition has no effect.
     */
    private fun checkUnnecessaryRepetition(repetition: Repetition) {
        if (repetition.kind.lowerBound == 1 && repetition.kind.upperBound == 1) {
            warnings.add(
                Diagnostic(
                    severity = Severity.Warning,
                    msg = "Repetition `{1}` has no effect and can be removed",
                    span = repetition.span,
                    kind = DiagnosticKind.Other,
                )
            )
        }
    }

    /**
     * Rule 3: Redundant nested repetition.
     *
     * `('x'?)?` or `('x'+)+` — nested repetitions that can be simplified.
     */
    private fun checkRedundantNestedRepetition(repetition: Repetition) {
        val inner = repetition.rule
        if (inner is Rule.Grp) {
            val parts = inner.group.parts
            if (parts.size == 1 && parts[0] is Rule.Rep) {
                warnings.add(
                    Diagnostic(
                        severity = Severity.Warning,
                        msg = "Nested repetition can be simplified",
                        span = repetition.span,
                        kind = DiagnosticKind.Other,
                    )
                )
            }
        }
    }

    /**
     * Rule 4: Empty expression.
     *
     * `()` — empty group with no content has no effect.
     */
    private fun checkEmptyGroup(group: Group) {
        val isEmpty = group.parts.isEmpty() ||
            (group.parts.size == 1 && group.parts[0] is Rule.Lit &&
                (group.parts[0] as Rule.Lit).literal.content.isEmpty())
        if (isEmpty && group.kind !is GroupKind.Capturing) {
            warnings.add(
                Diagnostic(
                    severity = Severity.Warning,
                    msg = "Empty expression has no effect",
                    span = group.span,
                    kind = DiagnosticKind.Other,
                )
            )
        }
    }

    /**
     * Rule 5: Unnecessary group.
     *
     * `('ab')?` where the group contains a single atomic element —
     * the parentheses are unnecessary. Only for non-capturing,
     * non-atomic groups. Alternations inside are excluded because
     * they need grouping for precedence.
     */
    private fun checkUnnecessaryGroup(group: Group) {
        if (group.kind != GroupKind.Normal && group.kind != GroupKind.Implicit) return
        if (group.parts.size != 1) return
        val single = group.parts[0]
        if (single is Rule.Alt) return
        warnings.add(
            Diagnostic(
                severity = Severity.Warning,
                msg = "Unnecessary parentheses around single expression",
                span = group.span,
                kind = DiagnosticKind.Other,
            )
        )
    }

    /**
     * Rule 6: Quantifier on anchor.
     *
     * `^+` or `$*` — quantifying anchors/boundaries is usually a mistake.
     */
    private fun checkQuantifierOnAnchor(repetition: Repetition) {
        if (repetition.rule is Rule.Bound) {
            warnings.add(
                Diagnostic(
                    severity = Severity.Warning,
                    msg = "Quantifier on anchor or boundary assertion has no meaningful effect",
                    span = repetition.span,
                    kind = DiagnosticKind.Other,
                )
            )
        }
    }

    /**
     * Rule 7: Quantifier on lookaround.
     *
     * `(>> 'x')+` or `(<< 'y')*` — lookarounds are zero-width assertions,
     * quantifying them is almost always a mistake.
     */
    private fun checkQuantifierOnLookaround(repetition: Repetition) {
        val inner = repetition.rule
        val isLookaround = inner is Rule.Look ||
            (inner is Rule.Grp && inner.group.parts.size == 1 && inner.group.parts[0] is Rule.Look)
        if (isLookaround) {
            warnings.add(
                Diagnostic(
                    severity = Severity.Warning,
                    msg = "Quantifier on lookaround assertion has no meaningful effect",
                    span = repetition.span,
                    kind = DiagnosticKind.Other,
                )
            )
        }
    }

    /**
     * Extracts the literal text content from a rule, if it is a simple literal
     * or a group containing a single literal.
     */
    private fun extractLiteralContent(rule: Rule): String? {
        return when (rule) {
            is Rule.Lit -> rule.literal.content
            is Rule.Grp -> {
                if (rule.group.parts.size == 1) {
                    extractLiteralContent(rule.group.parts[0])
                } else {
                    null
                }
            }
            else -> null
        }
    }
}
