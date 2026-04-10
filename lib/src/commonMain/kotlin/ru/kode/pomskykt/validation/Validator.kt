package ru.kode.pomskykt.validation

import ru.kode.pomskykt.diagnose.CompileError
import ru.kode.pomskykt.diagnose.CompileErrorKind
import ru.kode.pomskykt.diagnose.Feature
import ru.kode.pomskykt.features.PomskyFeatures
import ru.kode.pomskykt.options.CompileOptions
import ru.kode.pomskykt.options.RegexFlavor
import ru.kode.pomskykt.syntax.Span
import ru.kode.pomskykt.syntax.exprs.*
import ru.kode.pomskykt.visitor.NestingKind
import ru.kode.pomskykt.visitor.RuleVisitor

/**
 * Validates an AST for feature compatibility and regex flavor constraints.
 *
 * Ported from pomsky-lib/src/validation.rs.
 */
class Validator(val options: CompileOptions) : RuleVisitor {
    var firstRecursion: Span? = null
        private set
    private var layer: Int = 0
    // Track nesting inside lookbehinds (for Ruby checks)
    // Stack: true = this nesting level is a lookbehind
    private val lookaroundStack = ArrayDeque<Boolean>()
    // Pending: the next Lookaround down() call should push this value
    private var nextLookaroundIsLookbehind: Boolean = false
    private var hasPendingLookaround: Boolean = false
    private val errors = mutableListOf<CompileError>()

    private fun insideLookbehind(): Boolean = lookaroundStack.any { it }

    val compileErrors: List<CompileError> get() = errors

    private fun require(feature: Int, span: Span) {
        options.allowedFeatures.require(feature, span)?.let { errors.add(it) }
    }

    private fun unsupported(feature: Feature, span: Span) {
        errors.add(CompileError(CompileErrorKind.Unsupported(feature, flavor()), span))
    }

    private fun flavor(): RegexFlavor = options.flavor

    override fun down(kind: NestingKind) {
        layer++
        if (kind == NestingKind.Lookaround) {
            if (hasPendingLookaround) {
                lookaroundStack.addLast(nextLookaroundIsLookbehind)
                hasPendingLookaround = false
            } else {
                lookaroundStack.addLast(false)
            }
        }
    }

    override fun up(kind: NestingKind) {
        layer--
        if (kind == NestingKind.Lookaround && lookaroundStack.isNotEmpty()) {
            lookaroundStack.removeLast()
        }
    }

    override fun visitRepetition(repetition: Repetition) {
        if (repetition.quantifier == Quantifier.Lazy || repetition.quantifier == Quantifier.DefaultLazy) {
            require(PomskyFeatures.LAZY_MODE, repetition.span)
        }

        // ReDoS detection: nested unbounded quantifiers
        if (repetition.kind.upperBound == null) {
            if (containsUnboundedRepetition(repetition.rule)) {
                errors.add(CompileError(CompileErrorKind.NestedQuantifiers, repetition.span))
            }
        }

        // Ruby: repeated assertions (boundary or lookaround inside a repetition) are unsupported
        if (flavor() == RegexFlavor.Ruby) {
            val inner = repetition.rule
            if (isAssertionOrContainsAssertion(inner)) {
                unsupported(Feature.RepeatedAssertion, repetition.span)
            }
        }

        // RE2: repetition above 1000 is unsupported
        if (flavor() == RegexFlavor.RE2) {
            val upper = repetition.kind.upperBound
            if (upper != null && upper > 1000) {
                unsupported(Feature.RepetitionAbove1000, repetition.span)
            }
        }
    }

    /**
     * Returns true if the rule is a boundary/lookaround, or is a group that
     * (possibly nested) contains only boundary/lookaround (so repeating the group
     * repeats an assertion). Empty literals are treated as transparent.
     */
    private fun isAssertionOrContainsAssertion(rule: Rule): Boolean = when (rule) {
        is Rule.Bound -> true
        is Rule.Look -> true
        is Rule.Lit -> rule.literal.content.isEmpty() // empty string is transparent
        is Rule.Grp -> {
            val parts = rule.group.parts
            val nonEmptyParts = parts.filter { p ->
                !(p is Rule.Lit && p.literal.content.isEmpty())
            }
            nonEmptyParts.isNotEmpty() && nonEmptyParts.all { isAssertionOrContainsAssertion(it) }
        }
        is Rule.Neg -> isAssertionOrContainsAssertion(rule.negation.rule)
        else -> false
    }

    override fun visitIntersection(intersection: Intersection) {
        require(PomskyFeatures.INTERSECTION, intersection.span)
    }

    override fun visitGroup(group: Group) {
        val kind = group.kind
        when (kind) {
            is GroupKind.Capturing -> {
                if (kind.capture.name != null) {
                    require(PomskyFeatures.NAMED_GROUPS, group.span)
                    // POSIX ERE has no named groups
                    if (flavor() == RegexFlavor.PosixExtended) {
                        unsupported(Feature.NamedGroups, group.span)
                    }
                } else {
                    require(PomskyFeatures.NUMBERED_GROUPS, group.span)
                }
            }
            is GroupKind.Atomic -> {
                require(PomskyFeatures.ATOMIC_GROUPS, group.span)
                // Atomic groups are unsupported in JS, Python, RE2, Ruby, Rust, POSIX ERE
                if (flavor() in listOf(
                        RegexFlavor.JavaScript,
                        RegexFlavor.Python,
                        RegexFlavor.RE2,
                        RegexFlavor.Ruby,
                        RegexFlavor.Rust,
                        RegexFlavor.PosixExtended,
                    )
                ) {
                    unsupported(Feature.AtomicGroups, group.span)
                }
            }
            else -> {}
        }
    }

    override fun visitBoundary(boundary: Boundary) {
        require(PomskyFeatures.BOUNDARIES, boundary.span)

        // POSIX ERE: word boundaries (\b) are not supported
        if (flavor() == RegexFlavor.PosixExtended) {
            if (boundary.kind in listOf(
                    BoundaryKind.Word, BoundaryKind.NotWord,
                    BoundaryKind.WordStart, BoundaryKind.WordEnd,
                )
            ) {
                unsupported(Feature.UnicodeWordBoundaries, boundary.span)
            }
        }

        // Ruby: <word-start> and <word-end> are not allowed inside lookbehind
        if (flavor() == RegexFlavor.Ruby && insideLookbehind()) {
            if (boundary.kind == BoundaryKind.WordStart || boundary.kind == BoundaryKind.WordEnd) {
                errors.add(CompileError(
                    CompileErrorKind.RubyLookaheadInLookbehind(wasWordBoundary = true),
                    boundary.span,
                ))
            }
        }
    }

    override fun visitLookaround(lookaround: Lookaround) {
        val isLookbehind = lookaround.kind == LookaroundKind.Behind ||
            lookaround.kind == LookaroundKind.BehindNegative

        when (lookaround.kind) {
            LookaroundKind.Ahead, LookaroundKind.AheadNegative -> {
                require(PomskyFeatures.LOOKAHEAD, lookaround.span)
                // Rust, RE2, POSIX ERE don't support lookahead
                if (flavor() == RegexFlavor.Rust || flavor() == RegexFlavor.RE2 ||
                    flavor() == RegexFlavor.PosixExtended
                ) {
                    unsupported(Feature.Lookaround, lookaround.span)
                }
                // Ruby: lookahead inside lookbehind is not allowed
                if (flavor() == RegexFlavor.Ruby && insideLookbehind()) {
                    errors.add(CompileError(
                        CompileErrorKind.RubyLookaheadInLookbehind(wasWordBoundary = false),
                        lookaround.span,
                    ))
                }
            }
            LookaroundKind.Behind, LookaroundKind.BehindNegative -> {
                require(PomskyFeatures.LOOKBEHIND, lookaround.span)
                // Rust, RE2, POSIX ERE don't support lookbehind
                if (flavor() == RegexFlavor.Rust || flavor() == RegexFlavor.RE2 ||
                    flavor() == RegexFlavor.PosixExtended
                ) {
                    unsupported(Feature.Lookaround, lookaround.span)
                }
            }
        }

        // Signal to down() that the next Lookaround nesting push should record this as lookbehind
        nextLookaroundIsLookbehind = isLookbehind
        hasPendingLookaround = true
    }

    override fun visitCharClass(charClass: CharClass) {
        for (item in charClass.inner) {
            if (item !is GroupItem.Named) continue
            val name = item.name
            val span = item.span

            when (name) {
                // Negated word/space shorthands in ASCII mode
                is GroupName.Word -> {
                    if (item.negative && !charClass.unicodeAware) {
                        errors.add(CompileError(CompileErrorKind.NegativeShorthandInAsciiMode, span))
                    }
                    // JS: negated word in a class with other elements is unsupported in Unicode mode
                    // (when alone, it can be polyfilled by negating the whole set)
                    if (item.negative && charClass.unicodeAware &&
                        flavor() == RegexFlavor.JavaScript && charClass.inner.size > 1
                    ) {
                        unsupported(Feature.NegativeShorthandW, span)
                    }
                    // Python unicode mode: hint about \w not being fully Unicode
                    if (charClass.unicodeAware && flavor() == RegexFlavor.Python) {
                        errors.add(CompileError(CompileErrorKind.PythonWordUnicodeHint, span))
                    }
                }
                is GroupName.Space -> {
                    if (item.negative && !charClass.unicodeAware) {
                        errors.add(CompileError(CompileErrorKind.NegativeShorthandInAsciiMode, span))
                    }
                }
                // Unicode properties unsupported in certain flavors
                is GroupName.CategoryName -> {
                    if (!charClass.unicodeAware) {
                        errors.add(CompileError(CompileErrorKind.UnicodeInAsciiMode, span))
                    } else {
                        when (flavor()) {
                            RegexFlavor.Python -> unsupported(Feature.UnicodeProp, span)
                            RegexFlavor.PosixExtended -> unsupported(Feature.UnicodeProp, span)
                            else -> {}
                        }
                    }
                }
                is GroupName.ScriptName -> {
                    if (!charClass.unicodeAware) {
                        errors.add(CompileError(CompileErrorKind.UnicodeInAsciiMode, span))
                    } else {
                        when (flavor()) {
                            RegexFlavor.Python -> unsupported(Feature.UnicodeProp, span)
                            RegexFlavor.PosixExtended -> unsupported(Feature.UnicodeProp, span)
                            RegexFlavor.DotNet -> unsupported(Feature.UnicodeScript, span)
                            else -> {}
                        }
                        if (name.extension == ScriptExtension.Yes) {
                            when (flavor()) {
                                RegexFlavor.Java -> unsupported(Feature.ScriptExtensions, span)
                                else -> {}
                            }
                        }
                    }
                }
                is GroupName.CodeBlockName -> {
                    if (!charClass.unicodeAware) {
                        errors.add(CompileError(CompileErrorKind.UnicodeInAsciiMode, span))
                    } else {
                        when (flavor()) {
                            RegexFlavor.JavaScript -> unsupported(Feature.UnicodeBlock, span)
                            RegexFlavor.PosixExtended -> unsupported(Feature.UnicodeProp, span)
                            else -> {}
                        }
                    }
                }
                is GroupName.OtherPropertyName -> {
                    if (!charClass.unicodeAware) {
                        errors.add(CompileError(CompileErrorKind.UnicodeInAsciiMode, span))
                    } else {
                        when (flavor()) {
                            RegexFlavor.Ruby -> unsupported(Feature.SpecificUnicodeProp, span)
                            RegexFlavor.Python -> unsupported(Feature.UnicodeProp, span)
                            RegexFlavor.PosixExtended -> unsupported(Feature.UnicodeProp, span)
                            else -> {}
                        }
                    }
                }
                else -> {}
            }
        }
    }

    override fun visitReference(reference: Reference) {
        require(PomskyFeatures.REFERENCES, reference.span)

        // POSIX ERE: backreferences are not supported
        if (flavor() == RegexFlavor.PosixExtended) {
            unsupported(Feature.Backreference, reference.span)
        }

        // Ruby: references to both named and numbered groups are unsupported (MixedReferences)
        // This is checked at compile time in RuleCompiler, not here.
    }

    override fun visitRange(range: Range) {
        require(PomskyFeatures.RANGES, range.span)
    }

    override fun visitRegex(regex: Regex) {
        require(PomskyFeatures.REGEXES, regex.span)
    }

    override fun visitConditional(conditional: Conditional) {
        require(PomskyFeatures.CONDITIONALS, conditional.span)
        // Condition must be a lookaround (possibly wrapped in negations)
        var inner = conditional.condition
        while (inner is Rule.Neg) {
            inner = inner.negation.rule
        }
        if (inner !is Rule.Look) {
            errors.add(CompileError(CompileErrorKind.ConditionalRequiresLookaround, conditional.span))
        }
    }

    override fun visitRecursion(recursion: Recursion) {
        require(PomskyFeatures.RECURSION, recursion.span)
        if (firstRecursion == null) firstRecursion = recursion.span

        // Recursion is only supported in PCRE and Ruby
        if (flavor() !in listOf(RegexFlavor.Pcre, RegexFlavor.Ruby)) {
            unsupported(Feature.Recursion, recursion.span)
        }
        // Also explicitly unsupported for POSIX ERE (already covered by the check above)
    }

    override fun visitGrapheme() {
        require(PomskyFeatures.GRAPHEME, Span.EMPTY)
    }

    override fun visitDot() {
        require(PomskyFeatures.DOT, Span.EMPTY)
    }

    /**
     * Recursively checks whether a rule contains an unbounded repetition (upper == null).
     * Walks through groups, alternations, negations, and stmt expressions,
     * but NOT through variable references (let bindings).
     */
    private fun containsUnboundedRepetition(rule: Rule): Boolean = when (rule) {
        is Rule.Rep -> rule.repetition.kind.upperBound == null
        is Rule.Grp -> rule.group.parts.any { containsUnboundedRepetition(it) }
        is Rule.Alt -> rule.alternation.rules.any { containsUnboundedRepetition(it) }
        is Rule.Neg -> containsUnboundedRepetition(rule.negation.rule)
        is Rule.StmtE -> containsUnboundedRepetition(rule.stmtExpr.rule)
        is Rule.Look -> containsUnboundedRepetition(rule.lookaround.rule)
        is Rule.Cond -> {
            val elseB = rule.conditional.elseBranch
            containsUnboundedRepetition(rule.conditional.thenBranch) ||
                (elseB != null && containsUnboundedRepetition(elseB))
        }
        else -> false
    }
}
