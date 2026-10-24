package ru.kode.pomskykt.analysis

import ru.kode.pomskykt.regex.Regex
import ru.kode.pomskykt.regex.RegexAlternation
import ru.kode.pomskykt.regex.RegexCharSet
import ru.kode.pomskykt.regex.RegexCharSetItem
import ru.kode.pomskykt.regex.RegexGroup
import ru.kode.pomskykt.regex.RegexGroupKind
import ru.kode.pomskykt.regex.RegexLookaround
import ru.kode.pomskykt.regex.RegexProperty
import ru.kode.pomskykt.regex.RegexRepetition
import ru.kode.pomskykt.regex.RegexShorthand
import ru.kode.pomskykt.syntax.exprs.BoundaryKind
import ru.kode.pomskykt.syntax.exprs.LookaroundKind
import ru.kode.pomskykt.syntax.unicode.Category
import ru.kode.pomskykt.syntax.unicode.OtherProperties

/**
 * Explains why an input does not fully match a pattern.
 *
 * @param position index in the input where matching could not continue
 * @param matchedPrefix the part of the input before [position]
 * @param expected short human description of what was expected at [position],
 *   e.g. `":"`, `a digit`, `one of feat, fix`, `end of input`
 * @param missingGroup name of the innermost named capturing group the failure is inside, or null.
 *   `let` variables are inlined during compilation, so only named groups are reported.
 */
data class MatchDiagnosis(
    val position: Int,
    val matchedPrefix: String,
    val expected: String,
    val missingGroup: String?,
)

/**
 * Finds the longest prefix of an input that a [Regex] IR can match, anchored at the
 * start as a full-match validator would use it, and describes what was expected next.
 *
 * Simulates the pattern over sets of input positions (NFA-style), so there is no
 * exponential backtracking; total work is bounded by a step cap.
 *
 * Backreferences, recursion, raw regex (`regex '...'`), Unicode scripts/blocks and most
 * other Unicode properties are not simulated: patterns containing them yield null. Lookbehinds are
 * treated as always passing, and characters are matched as single UTF-16 units.
 */
object MatchDiagnoser {

    /** Default upper bound on simulation steps; exceeding it makes [diagnose] return null. */
    const val MAX_STEPS = 100_000

    /**
     * Diagnose why [input] does not fully match [regex].
     *
     * @return the furthest failure, or null if [input] fully matches, the step cap is
     *   exceeded, or the pattern uses an unsupported construct
     */
    fun diagnose(regex: Regex, input: String, maxSteps: Int = MAX_STEPS): MatchDiagnosis? {
        val run = Simulation(input, maxSteps)
        val ends = try {
            run.match(regex, setOf(0))
        } catch (e: Abort) {
            return null
        }
        if (input.length in ends) return null
        ends.forEach { run.fail(it, END_OF_INPUT) }
        if (run.farthest < 0) {
            // Only silent (lookaround) failures happened; report the start.
            return MatchDiagnosis(0, "", describe(regex), null)
        }
        return MatchDiagnosis(
            position = run.farthest,
            matchedPrefix = input.substring(0, run.farthest),
            expected = run.expected.joinToString(" or "),
            missingGroup = run.group,
        )
    }

    private const val MAX_LISTED_BRANCHES = 5
    private const val END_OF_INPUT = "end of input"
    private const val VERTICAL_SPACE = "\n\u000B\u000C\r\u0085  "

    private class Abort : RuntimeException()

    /** Per-call simulation state: step counter and the furthest failure seen so far. */
    private class Simulation(private val input: String, private val maxSteps: Int) {
        var farthest = -1
        val expected = mutableListOf<String>()
        var group: String? = null

        private val groups = ArrayDeque<String>()
        private var steps = 0
        private var ignoreCase = false
        private var silent = 0

        fun fail(pos: Int, what: String) {
            if (silent > 0 || pos < farthest) return
            if (pos > farthest) {
                farthest = pos
                expected.clear()
                group = groups.lastOrNull()
            }
            if (what !in expected) expected.add(what)
        }

        /** Returns every position where [node] can end when started at any of [starts]. */
        fun match(node: Regex, starts: Set<Int>): Set<Int> {
            if (starts.isEmpty()) return starts
            steps += starts.size
            if (steps > maxSteps) throw Abort()
            return when (node) {
                is Regex.Literal -> matchLiteral(node.content, starts)
                is Regex.CharSet -> matchChar(node, starts) { inSet(node.set, it) }
                is Regex.CompoundCharSet -> matchChar(node, starts) { c ->
                    node.set.sets.all { inSet(it, c) } != node.set.negative
                }
                is Regex.Dot -> matchChar(node, starts) { it != '\n' }
                is Regex.Grapheme -> matchChar(node, starts) { true }
                is Regex.Sequence -> node.parts.fold(starts) { acc, part -> match(part, acc) }
                is Regex.Group -> matchGroup(node.group, starts)
                is Regex.Alt -> matchAlt(node.alternation, starts)
                is Regex.Rep -> matchRep(node.repetition, starts)
                is Regex.Bound -> starts.filterTo(mutableSetOf()) { p ->
                    atBoundary(node.kind, p).also { if (!it) fail(p, describe(node)) }
                }
                is Regex.Look -> matchLook(node.lookaround, starts)
                is Regex.ModeGroup -> {
                    val saved = ignoreCase
                    ignoreCase = node.flags.ignoreCase ?: ignoreCase
                    match(node.inner, starts).also { ignoreCase = saved }
                }
                is Regex.Ref, is Regex.Recursion, is Regex.Unescaped -> throw Abort()
            }
        }

        private fun matchLiteral(content: String, starts: Set<Int>): Set<Int> {
            val ends = mutableSetOf<Int>()
            for (p in starts) {
                if (input.regionMatches(p, content, 0, content.length, ignoreCase)) {
                    ends.add(p + content.length)
                } else {
                    fail(p, quote(content))
                }
            }
            return ends
        }

        private fun matchChar(node: Regex, starts: Set<Int>, accepts: (Char) -> Boolean): Set<Int> {
            val ends = mutableSetOf<Int>()
            for (p in starts) {
                val c = input.getOrNull(p)
                val ok = c != null && if (ignoreCase) {
                    accepts(c) || accepts(c.lowercaseChar()) || accepts(c.uppercaseChar())
                } else {
                    accepts(c)
                }
                if (ok) ends.add(p + 1) else fail(p, describe(node))
            }
            return ends
        }

        private fun matchGroup(group: RegexGroup, starts: Set<Int>): Set<Int> {
            val kind = group.kind
            if (kind is RegexGroupKind.Named) groups.addLast(kind.name)
            val ends = group.parts.fold(starts) { acc, part -> match(part, acc) }
            if (kind is RegexGroupKind.Named) groups.removeLast()
            return ends
        }

        /**
         * Runs each start position separately so that, when no branch gets past it,
         * the per-branch expectations are replaced by one `one of ...` description.
         */
        private fun matchAlt(alt: RegexAlternation, starts: Set<Int>): Set<Int> {
            val ends = mutableSetOf<Int>()
            for (p in starts) {
                val keep = if (farthest == p) expected.size else 0
                val wasBehind = farthest < p
                alt.alternatives.forEach { ends += match(it, setOf(p)) }
                if (farthest == p && silent == 0) {
                    while (expected.size > keep) expected.removeLast()
                    expected.add(describeAlt(alt))
                    if (wasBehind) group = groups.lastOrNull()
                }
            }
            return ends
        }

        private fun matchRep(rep: RegexRepetition, starts: Set<Int>): Set<Int> {
            val ends = if (rep.lower == 0) starts.toMutableSet() else mutableSetOf()
            var frontier = starts
            var count = 0
            while (frontier.isNotEmpty() && (rep.upper == null || count < rep.upper)) {
                frontier = match(rep.inner, frontier)
                count++
                if (count >= rep.lower) {
                    // Positions already reached were expanded before; only new ones matter.
                    frontier = frontier - ends
                    ends += frontier
                }
            }
            return ends
        }

        private fun matchLook(look: RegexLookaround, starts: Set<Int>): Set<Int> {
            val negative = look.kind == LookaroundKind.AheadNegative
            if (look.kind != LookaroundKind.Ahead && !negative) return starts
            return starts.filterTo(mutableSetOf()) { p ->
                silent++
                val found = match(look.inner, setOf(p)).isNotEmpty()
                silent--
                (found != negative).also { ok ->
                    if (!ok) fail(p, (if (negative) "not " else "") + describe(look.inner))
                }
            }
        }

        private fun atBoundary(kind: BoundaryKind, p: Int): Boolean {
            val before = p > 0 && isWord(input[p - 1])
            val after = p < input.length && isWord(input[p])
            return when (kind) {
                BoundaryKind.Start -> p == 0
                BoundaryKind.End -> p == input.length
                BoundaryKind.Word -> before != after
                BoundaryKind.NotWord -> before == after
                BoundaryKind.WordStart -> !before && after
                BoundaryKind.WordEnd -> before && !after
            }
        }
    }

    // -- Character matching ---------------------------------------------------

    private fun isWord(c: Char): Boolean = c.isLetterOrDigit() || c == '_'

    private fun inSet(set: RegexCharSet, c: Char): Boolean = set.items.any { inItem(it, c) } != set.negative

    private fun inItem(item: RegexCharSetItem, c: Char): Boolean = when (item) {
        is RegexCharSetItem.Char -> c == item.char
        is RegexCharSetItem.Range -> c in item.first..item.last
        is RegexCharSetItem.Literal -> c in item.content
        is RegexCharSetItem.CodePoint -> c.code == item.codePoint
        is RegexCharSetItem.Shorthand -> inShorthand(item.shorthand, c)
        is RegexCharSetItem.Property -> inProperty(item.property, c) != item.negative
    }

    private fun inShorthand(shorthand: RegexShorthand, c: Char): Boolean = when (shorthand) {
        RegexShorthand.Word -> isWord(c)
        RegexShorthand.Digit -> c.isDigit()
        RegexShorthand.Space -> c.isWhitespace()
        RegexShorthand.NotWord -> !isWord(c)
        RegexShorthand.NotDigit -> !c.isDigit()
        RegexShorthand.NotSpace -> !c.isWhitespace()
        RegexShorthand.VertSpace -> c in VERTICAL_SPACE
        RegexShorthand.HorizSpace -> c.isWhitespace() && c !in VERTICAL_SPACE
    }

    private fun inProperty(property: RegexProperty, c: Char): Boolean {
        if (property is RegexProperty.OtherProp) {
            return when (property.property) {
                // Approximations via Kotlin's char categories; `[word]` compiles to Alphabetic.
                OtherProperties.Alphabetic -> c.isLetter() || c.category == CharCategory.LETTER_NUMBER
                OtherProperties.WhiteSpace -> c.isWhitespace()
                OtherProperties.Uppercase -> c.isUpperCase()
                OtherProperties.Lowercase -> c.isLowerCase()
                else -> throw Abort()
            }
        }
        if (property !is RegexProperty.CategoryProp) throw Abort()
        val code = c.category.code
        val abbreviation = property.category.abbreviation
        return when {
            abbreviation == "LC" -> code == "Lu" || code == "Ll" || code == "Lt"
            abbreviation.length == 1 -> code.startsWith(abbreviation)
            else -> code == abbreviation
        }
    }

    // -- Descriptions ---------------------------------------------------------

    private fun quote(content: String): String = "\"$content\""

    private fun describe(node: Regex): String = when (node) {
        is Regex.Literal -> quote(node.content)
        is Regex.CharSet -> describeCharSet(node.set)
        is Regex.CompoundCharSet -> "a matching character"
        is Regex.Dot, is Regex.Grapheme -> "any character"
        is Regex.Sequence -> node.parts.firstOrNull()?.let { describe(it) } ?: END_OF_INPUT
        is Regex.Group -> node.group.parts.firstOrNull()?.let { describe(it) } ?: END_OF_INPUT
        is Regex.Alt -> describeAlt(node.alternation)
        is Regex.Rep -> describe(node.repetition.inner)
        is Regex.ModeGroup -> describe(node.inner)
        is Regex.Look -> describe(node.lookaround.inner)
        is Regex.Bound -> when (node.kind) {
            BoundaryKind.Start -> "start of input"
            BoundaryKind.End -> END_OF_INPUT
            BoundaryKind.Word, BoundaryKind.WordStart, BoundaryKind.WordEnd -> "a word boundary"
            BoundaryKind.NotWord -> "no word boundary"
        }
        is Regex.Ref, is Regex.Recursion, is Regex.Unescaped -> "more input"
    }

    private fun describeAlt(alt: RegexAlternation): String {
        val names = alt.alternatives.map { if (it is Regex.Literal) it.content else describe(it) }
        val listed = names.take(MAX_LISTED_BRANCHES) + if (names.size > MAX_LISTED_BRANCHES) listOf("...") else emptyList()
        return "one of " + listed.joinToString(", ")
    }

    private fun describeCharSet(set: RegexCharSet): String {
        val single = set.items.singleOrNull()
        if (!set.negative && single != null) {
            when (single) {
                is RegexCharSetItem.Char -> return quote(single.char.toString())
                is RegexCharSetItem.Shorthand -> return withArticle(itemName(single))
                is RegexCharSetItem.Property -> if (!single.negative) return withArticle(itemName(single))
                else -> Unit
            }
        }
        if (!set.negative && WORD_POLYFILL.all { it in set.items }) return "a word character"
        val listed = set.items.joinToString(", ") { itemName(it) }
        return if (set.negative) "any character except $listed" else "one of $listed"
    }

    private fun withArticle(noun: String): String = if (noun.first() in "aeiou") "an $noun" else "a $noun"

    private fun itemName(item: RegexCharSetItem): String = when (item) {
        is RegexCharSetItem.Char -> item.char.toString()
        is RegexCharSetItem.Range -> "${item.first}-${item.last}"
        is RegexCharSetItem.Literal -> item.content
        is RegexCharSetItem.CodePoint -> "U+" + item.codePoint.toString(HEX_RADIX).uppercase()
        is RegexCharSetItem.Shorthand -> when (item.shorthand) {
            RegexShorthand.Word -> "word character"
            RegexShorthand.Digit -> "digit"
            RegexShorthand.Space -> "whitespace character"
            RegexShorthand.NotWord -> "non-word character"
            RegexShorthand.NotDigit -> "non-digit"
            RegexShorthand.NotSpace -> "non-whitespace character"
            RegexShorthand.VertSpace -> "line break"
            RegexShorthand.HorizSpace -> "horizontal whitespace character"
        }
        is RegexCharSetItem.Property -> (if (item.negative) "non-" else "") + when (val p = item.property) {
            is RegexProperty.CategoryProp -> p.category.fullName.replace('_', ' ').lowercase()
            is RegexProperty.ScriptProp -> p.script.fullName
            is RegexProperty.BlockProp -> p.block.fullName
            is RegexProperty.OtherProp -> p.property.fullName.replace('_', ' ').lowercase()
        }
    }

    private const val HEX_RADIX = 16

    /** Unicode-aware flavors compile `[word]` to this set of properties. */
    private val WORD_POLYFILL = listOf(
        RegexCharSetItem.Property(RegexProperty.OtherProp(OtherProperties.Alphabetic), negative = false),
        RegexCharSetItem.Property(RegexProperty.CategoryProp(Category.DecimalNumber), negative = false),
    )
}
