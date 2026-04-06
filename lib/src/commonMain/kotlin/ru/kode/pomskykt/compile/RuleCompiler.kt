package ru.kode.pomskykt.compile

import ru.kode.pomskykt.diagnose.*
import ru.kode.pomskykt.features.PomskyFeatures
import ru.kode.pomskykt.options.CompileOptions
import ru.kode.pomskykt.options.RegexFlavor
import ru.kode.pomskykt.syntax.Span
import ru.kode.pomskykt.syntax.exprs.*
import ru.kode.pomskykt.syntax.exprs.ScriptExtension
import ru.kode.pomskykt.syntax.unicode.Category
import ru.kode.pomskykt.syntax.unicode.OtherProperties
import ru.kode.pomskykt.regex.Regex as RIR
import ru.kode.pomskykt.regex.RegexAlternation
import ru.kode.pomskykt.regex.RegexCharSet
import ru.kode.pomskykt.regex.RegexCharSetItem
import ru.kode.pomskykt.regex.RegexCompoundCharSet
import ru.kode.pomskykt.regex.RegexGroup
import ru.kode.pomskykt.regex.RegexGroupKind
import ru.kode.pomskykt.regex.RegexLookaround
import ru.kode.pomskykt.regex.RegexProperty
import ru.kode.pomskykt.regex.RegexReference
import ru.kode.pomskykt.regex.RegexRepetition
import ru.kode.pomskykt.regex.RegexShorthand

/** Extract the span from an AST rule node, falling back to [Span.EMPTY]. */
private fun spanOf(rule: Rule): Span = when (rule) {
    is Rule.Lit -> rule.literal.span
    is Rule.Class -> rule.charClass.span
    is Rule.Grp -> rule.group.span
    is Rule.Alt -> rule.alternation.span
    is Rule.Inter -> rule.intersection.span
    is Rule.Rep -> rule.repetition.span
    is Rule.Bound -> rule.boundary.span
    is Rule.Look -> rule.lookaround.span
    is Rule.Var -> rule.variable.span
    is Rule.Ref -> rule.reference.span
    is Rule.Rng -> rule.range.span
    is Rule.Neg -> rule.negation.notSpan
    is Rule.Rgx -> rule.regex.span
    is Rule.Recur -> rule.recursion.span
    is Rule.StmtE -> Span.EMPTY
    Rule.Grapheme, Rule.Codepoint, Rule.Dot -> Span.EMPTY
}

/**
 * Compiles a pomsky AST [Rule] to a [Regex] intermediate representation.
 *
 * Ported from pomsky-lib/src/exprs/ (16 Rust files).
 */
fun compileRule(rule: Rule, options: CompileOptions, state: CompileState): RIR {
    return when (rule) {
        is Rule.Lit -> compileLiteral(rule.literal)
        is Rule.Class -> compileCharClass(rule.charClass, options)
        is Rule.Grp -> compileGroup(rule.group, options, state)
        is Rule.Alt -> compileAlternation(rule.alternation, options, state)
        is Rule.Inter -> compileIntersection(rule.intersection, options, state)
        is Rule.Rep -> compileRepetition(rule.repetition, options, state)
        is Rule.Bound -> compileBoundary(rule.boundary, options, state)
        is Rule.Look -> compileLookaround(rule.lookaround, options, state)
        is Rule.Var -> compileVariable(rule.variable, options, state)
        is Rule.Ref -> compileReference(rule.reference, options, state)
        is Rule.Rng -> compileRange(rule.range)
        is Rule.StmtE -> compileStmtExpr(rule.stmtExpr, options, state)
        is Rule.Neg -> compileNegation(rule.negation, options, state)
        is Rule.Rgx -> compileRegexLiteral(rule.regex)
        is Rule.Recur -> RIR.Recursion
        Rule.Grapheme -> compileGrapheme(options)
        Rule.Codepoint -> compileCodepoint()
        Rule.Dot -> RIR.Dot
    }
}

// --- Literal ---

private fun compileLiteral(literal: Literal): RIR =
    RIR.Literal(literal.content)

// --- Group ---

private fun compileGroup(group: Group, options: CompileOptions, state: CompileState): RIR {
    if (group.kind is GroupKind.Capturing) {
        val k = group.kind as GroupKind.Capturing
        val name = k.capture.name
        val isNamed = name != null
        if (isNamed) {
            // Check for duplicate group name
            if (!state.definedGroupNames.add(name!!)) {
                throw CompileException(CompileErrorKind.NameUsedMultipleTimes(name), group.span)
            }
        }

        // Record whether this absolute index is a named group
        state.groupIndexIsNamed[state.nextIdx] = isNamed
        state.nextIdx++
    }

    state.nestingDepth++
    val parts = group.parts.map { compileRule(it, options, state) }
    state.nestingDepth--

    val kind = when (val k = group.kind) {
        is GroupKind.Capturing -> {
            val name = k.capture.name
            if (name != null) {
                RegexGroupKind.Named(name, state.nextIdx - 1)
            } else {
                RegexGroupKind.Numbered(state.nextIdx - 1)
            }
        }
        is GroupKind.Atomic -> RegexGroupKind.Atomic
        is GroupKind.Normal -> RegexGroupKind.NonCapturing
        is GroupKind.Implicit -> RegexGroupKind.NonCapturing
    }
    return RIR.Group(RegexGroup(parts, kind))
}

// --- Alternation ---

private fun compileAlternation(alt: Alternation, options: CompileOptions, state: CompileState): RIR {
    val parts = alt.rules.map { compileRule(it, options, state) }
    return RIR.Alt(RegexAlternation(parts))
}

// --- Intersection ---

private fun compileIntersection(inter: Intersection, options: CompileOptions, state: CompileState): RIR {
    // Compile each part and try to convert to char sets for intersection
    val parts = inter.rules.map { compileRule(it, options, state) }
    val charSets = parts.map { regex ->
        expandToCharSet(regex)
            ?: throw CompileException(CompileErrorKind.BadIntersection, inter.span)
    }

    // Check for empty intersection: two single-char sets with no overlap
    if (charSets.size >= 2) {
        val first = charSets[0]
        if (!first.negative && first.items.size == 1 && first.items[0] is RegexCharSetItem.Char) {
            val firstChar = (first.items[0] as RegexCharSetItem.Char).char
            val allDistinct = charSets.drop(1).all { cs ->
                !cs.negative && cs.items.size == 1 &&
                    cs.items[0] is RegexCharSetItem.Char &&
                    (cs.items[0] as RegexCharSetItem.Char).char != firstChar
            }
            if (allDistinct) {
                throw CompileException(CompileErrorKind.EmptyIntersection, inter.span)
            }
        }
    }

    // De Morgan optimization: if all parts are negated char sets with no overlap,
    // merge them into a single negated char set: ![A] & ![B] = ![A B]
    if (charSets.all { it.negative }) {
        val mergedItems = charSets.flatMap { it.items }
        return RIR.CharSet(RegexCharSet(mergedItems, negative = true))
    }

    // Build compound char set
    val result = RIR.CompoundCharSet(RegexCompoundCharSet(charSets))

    // Check flavor restrictions AFTER successful intersection compilation
    if (options.flavor == RegexFlavor.DotNet ||
        options.flavor == RegexFlavor.Python ||
        options.flavor == RegexFlavor.RE2
    ) {
        throw CompileException(
            CompileErrorKind.Unsupported(Feature.CharSetIntersection, options.flavor),
            inter.span,
        )
    }

    return result
}

/**
 * Checks if a charset contains complementary shorthand pairs (e.g. \w and \W).
 * Returns a pair of display names if found, null otherwise.
 */
private fun checkComplementaryPairs(items: List<RegexCharSetItem>): Pair<String, String>? {
    val shorthands = items.filterIsInstance<RegexCharSetItem.Shorthand>().map { it.shorthand }.toSet()
    // Direct complementary pairs: word + !word, digit + !digit, space + !space
    val shorthandPairs = listOf(
        Triple(RegexShorthand.Space, RegexShorthand.NotSpace, "space"),
        Triple(RegexShorthand.Word, RegexShorthand.NotWord, "word"),
        Triple(RegexShorthand.Digit, RegexShorthand.NotDigit, "digit"),
    )
    for ((pos, neg, name) in shorthandPairs) {
        if (pos in shorthands && neg in shorthands) {
            return name to "!$name"
        }
    }

    // "Covering" pairs where union = everything (making negation empty):
    // word + !digit → word ⊃ digit, so word ∪ !digit = everything
    if (RegexShorthand.Word in shorthands && RegexShorthand.NotDigit in shorthands)
        return "!digit" to "word"
    if (RegexShorthand.NotDigit in shorthands && RegexShorthand.Word in shorthands)
        return "!digit" to "word"

    // !word + !space → !(word ∩ space) = everything (word ∩ space = ∅)
    if (RegexShorthand.NotWord in shorthands && RegexShorthand.NotSpace in shorthands)
        return "!space" to "!word"

    // !digit + !space → !(digit ∩ space) = everything (digit ∩ space = ∅)
    if (RegexShorthand.NotDigit in shorthands && RegexShorthand.NotSpace in shorthands)
        return "!space" to "!digit"

    // Check for Unicode property + its negation
    val props = items.filterIsInstance<RegexCharSetItem.Property>()
    for (prop in props.filter { !it.negative }) {
        val negProp = props.find { it.negative && it.property == prop.property }
        if (negProp != null) {
            val name = prop.property.displayName()
            return name to "!$name"
        }
    }
    return null
}

private fun RegexProperty.displayName(): String = when (this) {
    is RegexProperty.CategoryProp -> category.name
    is RegexProperty.ScriptProp -> script.name
    is RegexProperty.BlockProp -> block.name
    is RegexProperty.OtherProp -> property.fullName
}

private fun expandToCharSet(regex: RIR): RegexCharSet? {
    return when (regex) {
        is RIR.CharSet -> regex.set
        is RIR.Literal -> {
            val cpCount = countCodePoints(regex.content)
            if (cpCount == 1) {
                val cp = firstCodePoint(regex.content)
                if (cp <= 0xFFFF) {
                    RegexCharSet(listOf(RegexCharSetItem.Char(cp.toChar())), negative = false)
                } else {
                    RegexCharSet(listOf(RegexCharSetItem.CodePoint(cp)), negative = false)
                }
            } else {
                null
            }
        }
        is RIR.Group -> {
            if (regex.group.parts.size == 1) {
                expandToCharSet(regex.group.parts[0])
            } else {
                null
            }
        }
        else -> null
    }
}

// --- Repetition ---

private fun compileRepetition(rep: Repetition, options: CompileOptions, state: CompileState): RIR {
    // Check for infinite recursion: recursion inside a repetition that must repeat >= 1 time
    // without a non-recursive alternative.
    if (rep.kind.lowerBound > 0 && containsUnguardedRecursion(rep.rule)) {
        val recursionSpan = findRecursionSpan(rep.rule) ?: rep.rule.spanOrEmpty()
        throw CompileException(CompileErrorKind.InfiniteRecursion, recursionSpan)
    }

    val content = compileRule(rep.rule, options, state)
    val greedy = rep.quantifier == Quantifier.Greedy || rep.quantifier == Quantifier.DefaultGreedy
    return RIR.Rep(RegexRepetition(
        inner = content,
        lower = rep.kind.lowerBound,
        upper = rep.kind.upperBound,
        greedy = greedy,
    ))
}

/** Returns true if the rule contains a recursion node not guarded by an alternation. */
private fun containsUnguardedRecursion(rule: Rule): Boolean = when (rule) {
    is Rule.Recur -> true
    is Rule.Grp -> rule.group.parts.any { containsUnguardedRecursion(it) }
    is Rule.Rep -> containsUnguardedRecursion(rule.repetition.rule)
    is Rule.Look -> containsUnguardedRecursion(rule.lookaround.rule)
    is Rule.Neg -> containsUnguardedRecursion(rule.negation.rule)
    is Rule.StmtE -> containsUnguardedRecursion(rule.stmtExpr.rule)
    // Alternation: only if ALL alternatives contain recursion (no escape path)
    is Rule.Alt -> rule.alternation.rules.all { containsUnguardedRecursion(it) }
    else -> false
}

/** Find the span of the first recursion node within a rule tree. */
private fun findRecursionSpan(rule: Rule): ru.kode.pomskykt.syntax.Span? = when (rule) {
    is Rule.Recur -> rule.recursion.span
    is Rule.Grp -> rule.group.parts.firstNotNullOfOrNull { findRecursionSpan(it) }
    is Rule.Alt -> rule.alternation.rules.firstNotNullOfOrNull { findRecursionSpan(it) }
    is Rule.Rep -> findRecursionSpan(rule.repetition.rule)
    is Rule.Look -> findRecursionSpan(rule.lookaround.rule)
    is Rule.Neg -> findRecursionSpan(rule.negation.rule)
    is Rule.StmtE -> findRecursionSpan(rule.stmtExpr.rule)
    else -> null
}

private fun Rule.spanOrEmpty(): ru.kode.pomskykt.syntax.Span = when (this) {
    is Rule.Lit -> literal.span
    is Rule.Class -> charClass.span
    is Rule.Grp -> group.span
    is Rule.Alt -> alternation.span
    is Rule.Inter -> intersection.span
    is Rule.Rep -> repetition.span
    is Rule.Bound -> boundary.span
    is Rule.Look -> lookaround.span
    is Rule.Var -> variable.span
    is Rule.Ref -> reference.span
    is Rule.Rng -> range.span
    is Rule.StmtE -> stmtExpr.span
    is Rule.Neg -> negation.notSpan
    is Rule.Rgx -> regex.span
    is Rule.Recur -> recursion.span
    else -> ru.kode.pomskykt.syntax.Span.EMPTY
}

// --- Boundary ---

private fun compileBoundary(boundary: Boundary, options: CompileOptions, state: CompileState): RIR {
    if (options.flavor == RegexFlavor.RE2 &&
        (boundary.kind == BoundaryKind.WordStart || boundary.kind == BoundaryKind.WordEnd)
    ) {
        throw CompileException(
            CompileErrorKind.Unsupported(Feature.WordStartEnd, options.flavor),
            boundary.span,
        )
    }
    if ((options.flavor == RegexFlavor.JavaScript || options.flavor == RegexFlavor.RE2) &&
        boundary.unicodeAware &&
        boundary.kind in listOf(BoundaryKind.Word, BoundaryKind.NotWord, BoundaryKind.WordStart, BoundaryKind.WordEnd)
    ) {
        throw CompileException(
            CompileErrorKind.Unsupported(Feature.UnicodeWordBoundaries, options.flavor),
            boundary.span,
        )
    }
    return RIR.Bound(boundary.kind)
}

// --- Character class ---

private fun compileCharClass(charClass: CharClass, options: CompileOptions): RIR {
    val items = mutableListOf<RegexCharSetItem>()
    var negative = false

    for (item in charClass.inner) {
        when (item) {
            is GroupItem.Char -> items.add(RegexCharSetItem.Char(item.char))
            is GroupItem.CodePoint -> items.add(RegexCharSetItem.CodePoint(item.codePoint))
            is GroupItem.CharRange -> items.add(RegexCharSetItem.Range(item.first, item.last))
            is GroupItem.Named -> {
                val (regexItems, isNeg) = namedClassToItems(item.name, item.negative, charClass.unicodeAware, options.flavor)
                if (isNeg) negative = true
                items.addAll(regexItems)
            }
        }
    }

    // Deduplicate and compact Char items into ranges
    val compacted = deduplicateAndMergeChars(items)

    if (compacted.size == 1 && !negative) {
        val single = compacted[0]
        if (single is RegexCharSetItem.Char) {
            return RIR.Literal(single.char.toString())
        }
    }

    return RIR.CharSet(RegexCharSet(compacted, negative))
}

private fun namedClassToItems(
    name: GroupName,
    negative: Boolean,
    unicodeAware: Boolean,
    flavor: RegexFlavor = RegexFlavor.Pcre,
): Pair<List<RegexCharSetItem>, Boolean> {
    val items = mutableListOf<RegexCharSetItem>()
    var setNegative = false

    when (name) {
        is GroupName.Word -> {
            if (!unicodeAware && flavor != RegexFlavor.RE2 && flavor != RegexFlavor.JavaScript) {
                // ASCII mode: expand \w to [0-9A-Z_a-z]
                items.add(RegexCharSetItem.Range('0', '9'))
                items.add(RegexCharSetItem.Range('A', 'Z'))
                items.add(RegexCharSetItem.Char('_'))
                items.add(RegexCharSetItem.Range('a', 'z'))
                if (negative) setNegative = true
            } else if (unicodeAware && flavor == RegexFlavor.JavaScript) {
                // JS unicode: \w → [\p{Alphabetic}\p{M}\p{Nd}\p{Pc}]
                items.add(RegexCharSetItem.Property(RegexProperty.OtherProp(OtherProperties.Alphabetic), false))
                items.add(RegexCharSetItem.Property(RegexProperty.CategoryProp(Category.Mark), false))
                items.add(RegexCharSetItem.Property(RegexProperty.CategoryProp(Category.DecimalNumber), false))
                items.add(RegexCharSetItem.Property(RegexProperty.CategoryProp(Category.ConnectorPunctuation), false))
                if (negative) setNegative = true
            } else {
                val sh = if (negative) RegexShorthand.NotWord else RegexShorthand.Word
                items.add(RegexCharSetItem.Shorthand(sh))
            }
        }
        is GroupName.Digit -> {
            if (!unicodeAware && flavor != RegexFlavor.RE2 && flavor != RegexFlavor.JavaScript) {
                // ASCII mode: expand \d to [0-9]
                items.add(RegexCharSetItem.Range('0', '9'))
                if (negative) setNegative = true
            } else if (unicodeAware && (flavor == RegexFlavor.JavaScript || flavor == RegexFlavor.RE2)) {
                // JS/RE2 unicode: \d → \p{Nd}
                items.add(RegexCharSetItem.Property(RegexProperty.CategoryProp(Category.DecimalNumber), false))
                if (negative) setNegative = true
            } else {
                val sh = if (negative) RegexShorthand.NotDigit else RegexShorthand.Digit
                items.add(RegexCharSetItem.Shorthand(sh))
            }
        }
        is GroupName.Space -> {
            if (!unicodeAware && flavor != RegexFlavor.RE2) {
                // ASCII mode: expand \s to [\t-\r ]
                items.add(RegexCharSetItem.Range('\t', '\r'))
                items.add(RegexCharSetItem.Char(' '))
                if (negative) setNegative = true
            } else if (unicodeAware && flavor == RegexFlavor.RE2) {
                // RE2 unicode: \s plus extra Unicode whitespace chars
                items.add(RegexCharSetItem.Shorthand(RegexShorthand.Space))
                items.add(RegexCharSetItem.CodePoint(0x0B))   // VT
                items.add(RegexCharSetItem.CodePoint(0xA0))   // NBSP
                items.add(RegexCharSetItem.CodePoint(0x1680)) // Ogham Space Mark
                items.add(RegexCharSetItem.Range('\u2000', '\u200A')) // En Quad..Hair Space
                items.add(RegexCharSetItem.CodePoint(0x2028)) // Line Separator
                items.add(RegexCharSetItem.CodePoint(0x2029)) // Paragraph Separator
                items.add(RegexCharSetItem.CodePoint(0x202F)) // Narrow NBSP
                items.add(RegexCharSetItem.CodePoint(0x205F)) // Medium Math Space
                items.add(RegexCharSetItem.CodePoint(0x3000)) // Ideographic Space
                items.add(RegexCharSetItem.CodePoint(0xFEFF)) // BOM
                if (negative) setNegative = true
            } else {
                val sh = if (negative) RegexShorthand.NotSpace else RegexShorthand.Space
                items.add(RegexCharSetItem.Shorthand(sh))
            }
        }
        is GroupName.HorizSpace -> {
            if (flavor == RegexFlavor.Rust) {
                // Rust doesn't support \h — expand to [\p{Zs}\t]
                items.add(RegexCharSetItem.Property(RegexProperty.CategoryProp(Category.SpaceSeparator), false))
                items.add(RegexCharSetItem.Char('\t'))
            } else {
                items.add(RegexCharSetItem.Shorthand(RegexShorthand.HorizSpace))
            }
            if (negative) setNegative = true
        }
        is GroupName.VertSpace -> {
            if (flavor == RegexFlavor.Rust) {
                // Rust doesn't support \v — expand to [\n-\r\x85\u2028\u2029]
                items.add(RegexCharSetItem.Range('\n', '\r'))
                items.add(RegexCharSetItem.CodePoint(0x85))
                items.add(RegexCharSetItem.CodePoint(0x2028))
                items.add(RegexCharSetItem.CodePoint(0x2029))
            } else {
                items.add(RegexCharSetItem.Shorthand(RegexShorthand.VertSpace))
            }
            if (negative) setNegative = true
        }
        is GroupName.CategoryName -> {
            items.add(RegexCharSetItem.Property(RegexProperty.CategoryProp(name.category), negative))
        }
        is GroupName.ScriptName -> {
            val ext = when (name.extension) {
                ScriptExtension.Yes -> ScriptExtension.Yes
                ScriptExtension.No -> when (flavor) {
                    RegexFlavor.Pcre -> ScriptExtension.No
                    else -> ScriptExtension.Unspecified
                }
                else -> ScriptExtension.Unspecified
            }
            items.add(RegexCharSetItem.Property(
                RegexProperty.ScriptProp(name.script, ext),
                negative,
            ))
        }
        is GroupName.CodeBlockName -> {
            items.add(RegexCharSetItem.Property(RegexProperty.BlockProp(name.block), negative))
        }
        is GroupName.OtherPropertyName -> {
            items.add(RegexCharSetItem.Property(RegexProperty.OtherProp(name.property), negative))
        }
    }

    return items to setNegative
}

// --- Lookaround ---

private fun compileLookaround(look: Lookaround, options: CompileOptions, state: CompileState): RIR {
    // Ruby: lookahead not allowed inside lookbehind (even via variables)
    if (options.flavor == RegexFlavor.Ruby && state.inLookbehind) {
        if (look.kind == LookaroundKind.Ahead || look.kind == LookaroundKind.AheadNegative) {
            throw CompileException(
                CompileErrorKind.RubyLookaheadInLookbehind(wasWordBoundary = false),
                look.span,
            )
        }
    }

    val prevInLookbehind = state.inLookbehind
    if (look.kind == LookaroundKind.Behind || look.kind == LookaroundKind.BehindNegative) {
        state.inLookbehind = true
    }
    state.nestingDepth++
    val inner = compileRule(look.rule, options, state)
    state.nestingDepth--
    state.inLookbehind = prevInLookbehind
    return RIR.Look(RegexLookaround(look.kind, inner))
}

// --- Reference ---

private fun compileReference(ref: Reference, options: CompileOptions, state: CompileState): RIR {
    // References inside let bindings are not allowed
    if (state.inLetBinding) {
        throw CompileException(CompileErrorKind.ReferenceInLet, ref.span)
    }

    return when (val target = ref.target) {
        is ReferenceTarget.Named -> {
            val groupIndex = state.usedNames[target.name]
                ?: throw CompileException(
                    CompileErrorKind.UnknownReferenceName(target.name),
                    ref.span,
                )
            if (options.flavor == RegexFlavor.Rust || options.flavor == RegexFlavor.RE2) {
                throw CompileException(
                    CompileErrorKind.Unsupported(Feature.Backreference, options.flavor),
                    ref.span,
                )
            }
            if (options.flavor == RegexFlavor.Ruby) {
                RIR.Ref(RegexReference.Named(target.name))
            } else {
                val idx = if (options.flavor == RegexFlavor.DotNet) {
                    state.numberedGroupsCount + groupIndex.fromNamed
                } else {
                    groupIndex.absolute
                }
                RIR.Ref(RegexReference.Numbered(idx))
            }
        }
        is ReferenceTarget.Number -> {
            if (target.number > 255) {
                throw CompileException(CompileErrorKind.HugeReference, ref.span)
            }
            if (target.number == 0) {
                throw CompileException(
                    CompileErrorKind.UnknownReferenceNumber(0),
                    ref.span,
                )
            }
            // DotNet: numeric references forbidden when there are both named and unnamed groups
            if (options.flavor == RegexFlavor.DotNet &&
                state.namedGroupsCount > 0 && state.numberedGroupsCount > 0
            ) {
                throw CompileException(CompileErrorKind.DotNetNumberedRefWithMixedGroups, ref.span)
            }
            // Ruby: numeric reference to an UNNAMED group is forbidden when named groups exist
            val targetIsNamed = state.groupIndexIsNamed[target.number] ?: false
            if (options.flavor == RegexFlavor.Ruby && !targetIsNamed &&
                state.namedGroupsCount > 0 && state.numberedGroupsCount > 0
            ) {
                throw CompileException(
                    CompileErrorKind.Unsupported(Feature.MixedReferences, options.flavor),
                    ref.span,
                )
            }
            // Check for forward reference (group not yet defined: target >= nextIdx)
            if (target.number >= state.nextIdx) {
                if (options.flavor == RegexFlavor.Rust || options.flavor == RegexFlavor.RE2) {
                    throw CompileException(
                        CompileErrorKind.Unsupported(Feature.ForwardReference, options.flavor),
                        ref.span,
                    )
                }
            }
            if (options.flavor == RegexFlavor.Rust || options.flavor == RegexFlavor.RE2) {
                throw CompileException(
                    CompileErrorKind.Unsupported(Feature.Backreference, options.flavor),
                    ref.span,
                )
            }
            // Ruby: resolve numeric references to named references when the target group has a name
            if (options.flavor == RegexFlavor.Ruby) {
                val name = state.usedNames.entries.find { it.value.absolute == target.number }?.key
                if (name != null) {
                    return RIR.Ref(RegexReference.Named(name))
                }
            }
            RIR.Ref(RegexReference.Numbered(target.number))
        }
        is ReferenceTarget.Relative -> {
            if (target.offset == 0) {
                throw CompileException(CompileErrorKind.RelativeRefZero, ref.span)
            }
            if (options.flavor == RegexFlavor.Rust || options.flavor == RegexFlavor.RE2) {
                throw CompileException(
                    CompileErrorKind.Unsupported(Feature.Backreference, options.flavor),
                    ref.span,
                )
            }
            val absolute = state.nextIdx + target.offset
            if (absolute < 0) {
                throw CompileException(
                    CompileErrorKind.UnknownReferenceNumber(target.offset),
                    ref.span,
                )
            }
            // DotNet: relative (numbered) references forbidden when there are both named and unnamed groups
            if (options.flavor == RegexFlavor.DotNet &&
                state.namedGroupsCount > 0 && state.numberedGroupsCount > 0
            ) {
                throw CompileException(CompileErrorKind.DotNetNumberedRefWithMixedGroups, ref.span)
            }
            // Ruby: resolve numeric references to named references when the target group has a name
            if (options.flavor == RegexFlavor.Ruby) {
                val name = state.usedNames.entries.find { it.value.absolute == absolute }?.key
                if (name != null) {
                    return RIR.Ref(RegexReference.Named(name))
                }
            }
            RIR.Ref(RegexReference.Numbered(absolute))
        }
    }
}

// --- Statement ---

private fun compileStmtExpr(stmtExpr: StmtExpr, options: CompileOptions, state: CompileState): RIR {
    return when (val stmt = stmtExpr.stmt) {
        is Stmt.Enable, is Stmt.Disable -> {
            compileRule(stmtExpr.rule, options, state)
        }
        is Stmt.LetDecl -> {
            // Check the let binding body for captures and references (not allowed)
            checkLetBindingForCaptures(stmt.letBinding.rule, state, stmtExpr.span)
            state.pushVariable(stmt.letBinding.name, stmt.letBinding.rule)
            val result = compileRule(stmtExpr.rule, options, state)
            state.popVariable()
            result
        }
        is Stmt.TestDecl -> {
            // Test blocks may only appear at the top level (nestingDepth == 0)
            if (state.nestingDepth > 0) {
                throw CompileException(CompileErrorKind.NestedTest, stmt.test.span)
            }
            compileRule(stmtExpr.rule, options, state)
        }
    }
}

/** Recursively checks a let-binding rule for captures and references, throwing if found. */
private fun checkLetBindingForCaptures(rule: Rule, state: CompileState, span: ru.kode.pomskykt.syntax.Span) {
    when (rule) {
        is Rule.Grp -> {
            if (rule.group.kind is GroupKind.Capturing) {
                throw CompileException(CompileErrorKind.CaptureInLet, rule.group.span)
            }
            rule.group.parts.forEach { checkLetBindingForCaptures(it, state, span) }
        }
        is Rule.Ref -> {
            throw CompileException(CompileErrorKind.ReferenceInLet, rule.reference.span)
        }
        is Rule.Alt -> rule.alternation.rules.forEach { checkLetBindingForCaptures(it, state, span) }
        is Rule.Rep -> checkLetBindingForCaptures(rule.repetition.rule, state, span)
        is Rule.Look -> checkLetBindingForCaptures(rule.lookaround.rule, state, span)
        is Rule.Neg -> checkLetBindingForCaptures(rule.negation.rule, state, span)
        is Rule.StmtE -> {
            when (rule.stmtExpr.stmt) {
                is Stmt.LetDecl -> checkLetBindingForCaptures(rule.stmtExpr.rule, state, span)
                else -> checkLetBindingForCaptures(rule.stmtExpr.rule, state, span)
            }
        }
        else -> {}
    }
}

// --- Variable ---

private fun compileVariable(variable: Variable, options: CompileOptions, state: CompileState): RIR {
    val binding = state.variables.asReversed().withIndex().find { (idx, pair) ->
        pair.first == variable.name && (state.variables.size - 1 - idx) !in state.currentVars
    }

    if (binding != null) {
        val (revIdx, pair) = binding
        val realIdx = state.variables.size - 1 - revIdx
        // Special case for built-in spanless rules: pass the variable's call span
        if (pair.second == Rule.Grapheme) return compileGrapheme(options, variable.span)
        state.currentVars.add(realIdx)
        val result = compileRule(pair.second, options, state)
        state.currentVars.remove(realIdx)
        return result
    }

    val recursive = state.variables.asReversed().any { it.first == variable.name }
    if (recursive) {
        throw CompileException(CompileErrorKind.RecursiveVariable, variable.span)
    }
    throw CompileException(
        CompileErrorKind.UnknownVariable(variable.name),
        variable.span,
    )
}

// --- Range ---

private fun compileRange(range: Range): RIR = compileRangeExpr(range)

// --- Regex literal ---

private fun compileRegexLiteral(regex: ru.kode.pomskykt.syntax.exprs.Regex): RIR =
    RIR.Unescaped(regex.content)

// --- Grapheme ---

private fun compileGrapheme(options: CompileOptions, span: Span = Span.EMPTY): RIR {
    if (options.flavor !in listOf(RegexFlavor.Pcre, RegexFlavor.Java, RegexFlavor.Ruby)) {
        throw CompileException(
            CompileErrorKind.Unsupported(Feature.Grapheme, options.flavor),
            span,
        )
    }
    return RIR.Grapheme
}

// --- Codepoint ---

private fun compileCodepoint(): RIR {
    return RIR.CharSet(RegexCharSet(
        listOf(
            RegexCharSetItem.Shorthand(RegexShorthand.Space),
            RegexCharSetItem.Shorthand(RegexShorthand.NotSpace),
        ),
        negative = false,
    ))
}

// --- Negation ---

private fun compileNegation(negation: Negation, options: CompileOptions, state: CompileState): RIR {
    val inner = compileRule(negation.rule, options, state)
    val innerSpan = spanOf(negation.rule)
    return negateRegex(inner, negation.notSpan, options.flavor, innerSpan)
}

private fun negateRegex(regex: RIR, span: Span, flavor: RegexFlavor, innerSpan: Span = span): RIR {
    return when (regex) {
        is RIR.CharSet -> {
            // DotNet: supplementary codepoints (> U+FFFF) can't be negated.
            // These appear either as RegexCharSetItem.Literal or as surrogate pair Char items.
            if (flavor == RegexFlavor.DotNet) {
                for (item in regex.set.items) {
                    if (item is RegexCharSetItem.CodePoint && item.codePoint > 0xFFFF) {
                        val chars = codePointToString(item.codePoint)
                        throw CompileException(
                            CompileErrorKind.IllegalNegation(IllegalNegationKind.DotNetChar(chars, item.codePoint)),
                            span,
                        )
                    }
                    if (item is RegexCharSetItem.Literal) {
                        val cp = firstCodePoint(item.content)
                        if (cp > 0xFFFF) {
                            throw CompileException(
                                CompileErrorKind.IllegalNegation(IllegalNegationKind.DotNetChar(item.content, cp)),
                                span,
                            )
                        }
                    }
                    if (item is RegexCharSetItem.Char && item.char.isSurrogate()) {
                        val cp = item.char.code
                        throw CompileException(
                            CompileErrorKind.IllegalNegation(IllegalNegationKind.DotNetChar(item.char.toString(), cp)),
                            span,
                        )
                    }
                }
            }
            // Check for complementary pairs that make the class empty when negated
            val emptyCheck = checkComplementaryPairs(regex.set.items)
            if (emptyCheck != null) {
                throw CompileException(
                    CompileErrorKind.EmptyClassNegated(emptyCheck.first, emptyCheck.second),
                    innerSpan,
                )
            }
            RIR.CharSet(RegexCharSet(regex.set.items, !regex.set.negative))
        }
        is RIR.CompoundCharSet -> {
            // CompoundCharSet negation - toggle negative on first set
            val sets = regex.set.sets.toMutableList()
            if (sets.isNotEmpty()) {
                sets[0] = RegexCharSet(sets[0].items, !sets[0].negative)
            }
            RIR.CompoundCharSet(RegexCompoundCharSet(sets))
        }
        is RIR.Look -> {
            val negKind = when (regex.lookaround.kind) {
                LookaroundKind.Ahead -> LookaroundKind.AheadNegative
                LookaroundKind.Behind -> LookaroundKind.BehindNegative
                LookaroundKind.AheadNegative -> LookaroundKind.Ahead
                LookaroundKind.BehindNegative -> LookaroundKind.Behind
            }
            RIR.Look(RegexLookaround(negKind, regex.lookaround.inner))
        }
        is RIR.Bound -> {
            if (regex.kind == BoundaryKind.Word) {
                RIR.Bound(BoundaryKind.NotWord)
            } else if (regex.kind == BoundaryKind.NotWord) {
                RIR.Bound(BoundaryKind.Word)
            } else {
                throw CompileException(CompileErrorKind.IllegalNegation(IllegalNegationKind.Boundary), span)
            }
        }
        is RIR.Literal -> {
            // Count Unicode code points (not UTF-16 chars)
            val codePointCount = countCodePoints(regex.content)
            if (codePointCount == 1) {
                val codePoint = firstCodePoint(regex.content)
                // For BMP characters, use Char; for supplementary, use the string
                if (codePoint <= 0xFFFF) {
                    RIR.CharSet(RegexCharSet(
                        listOf(RegexCharSetItem.Char(codePoint.toChar())),
                        negative = true,
                    ))
                } else {
                    // Supplementary code point: DotNet can't negate above U+FFFF
                    if (flavor == RegexFlavor.DotNet) {
                        throw CompileException(
                            CompileErrorKind.IllegalNegation(IllegalNegationKind.DotNetChar(regex.content, codePoint)),
                            span,
                        )
                    }
                    // Supplementary code point — emit as a negated char set
                    RIR.CharSet(RegexCharSet(
                        listOf(RegexCharSetItem.CodePoint(codePoint)),
                        negative = true,
                    ))
                }
            } else {
                throw CompileException(
                    CompileErrorKind.IllegalNegation(IllegalNegationKind.Literal(regex.content)),
                    span,
                )
            }
        }
        is RIR.Group -> {
            if (regex.group.kind == RegexGroupKind.NonCapturing && regex.group.parts.size == 1) {
                negateRegex(regex.group.parts[0], span, flavor)
            } else {
                throw CompileException(CompileErrorKind.IllegalNegation(IllegalNegationKind.Group), span)
            }
        }
        is RIR.Alt -> {
            throw CompileException(CompileErrorKind.IllegalNegation(IllegalNegationKind.Alternation), span)
        }
        is RIR.Rep -> {
            throw CompileException(CompileErrorKind.IllegalNegation(IllegalNegationKind.Repetition), span)
        }
        is RIR.Ref -> {
            throw CompileException(CompileErrorKind.IllegalNegation(IllegalNegationKind.Reference), span)
        }
        is RIR.Recursion -> {
            throw CompileException(CompileErrorKind.IllegalNegation(IllegalNegationKind.Recursion), span)
        }
        is RIR.Dot -> {
            throw CompileException(CompileErrorKind.IllegalNegation(IllegalNegationKind.Dot), span)
        }
        is RIR.Grapheme -> {
            throw CompileException(CompileErrorKind.IllegalNegation(IllegalNegationKind.Grapheme), span)
        }
        is RIR.Unescaped -> {
            throw CompileException(CompileErrorKind.IllegalNegation(IllegalNegationKind.Unescaped), span)
        }
        else -> throw CompileException(CompileErrorKind.IllegalNegation(IllegalNegationKind.Group), span)
    }
}

/**
 * Deduplicates char items and merges consecutive chars into ranges.
 * Non-Char items (Shorthand, Property, Range, CodePoint) are left in order.
 * Result: chars sorted, deduplicated, then consecutive runs merged into Range items.
 */
private fun deduplicateAndMergeChars(items: List<RegexCharSetItem>): List<RegexCharSetItem> {
    val chars = mutableSetOf<Char>()
    val nonChars = mutableListOf<RegexCharSetItem>()
    val seen = mutableSetOf<RegexCharSetItem>()

    for (item in items) {
        if (item is RegexCharSetItem.Char) {
            chars.add(item.char)
        } else {
            // Deduplicate non-Char items (Properties, Shorthands, etc.)
            if (seen.add(item)) {
                nonChars.add(item)
            }
        }
    }

    if (chars.isEmpty()) return nonChars

    // Merge consecutive chars into ranges
    val merged = mutableListOf<RegexCharSetItem>()
    val sortedChars = chars.sorted()
    var i = 0
    while (i < sortedChars.size) {
        val start = sortedChars[i]
        var end = start
        while (i + 1 < sortedChars.size && sortedChars[i + 1].code == end.code + 1) {
            i++
            end = sortedChars[i]
        }
        if (end == start) {
            merged.add(RegexCharSetItem.Char(start))
        } else {
            merged.add(RegexCharSetItem.Range(start, end))
        }
        i++
    }

    return nonChars + merged
}

/** Count the number of Unicode code points in a string (handling surrogate pairs). */
private fun countCodePoints(s: String): Int {
    var count = 0
    var i = 0
    while (i < s.length) {
        if (s[i].isHighSurrogate() && i + 1 < s.length && s[i + 1].isLowSurrogate()) {
            i += 2
        } else {
            i++
        }
        count++
    }
    return count
}

/** Get the first code point from a string (handling surrogate pairs). */
private fun firstCodePoint(s: String): Int {
    if (s.isEmpty()) return 0
    val high = s[0]
    if (high.isHighSurrogate() && s.length > 1 && s[1].isLowSurrogate()) {
        return (high.code - 0xD800) * 0x400 + (s[1].code - 0xDC00) + 0x10000
    }
    return high.code
}

/** Convert a code point to a string (handling supplementary code points). */
private fun codePointToString(cp: Int): String {
    if (cp <= 0xFFFF) return cp.toChar().toString()
    val high = ((cp - 0x10000) shr 10) + 0xD800
    val low = ((cp - 0x10000) and 0x3FF) + 0xDC00
    return charArrayOf(high.toChar(), low.toChar()).concatToString()
}

/** Internal exception for compile errors. */
internal class CompileException(
    val kind: CompileErrorKind,
    val span: Span,
) : Exception()
