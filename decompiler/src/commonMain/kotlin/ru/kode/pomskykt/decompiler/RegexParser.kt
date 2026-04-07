package ru.kode.pomskykt.decompiler

import ru.kode.pomskykt.regex.Regex
import ru.kode.pomskykt.regex.RegexAlternation
import ru.kode.pomskykt.regex.RegexCharSet
import ru.kode.pomskykt.regex.RegexCharSetItem
import ru.kode.pomskykt.regex.RegexGroup
import ru.kode.pomskykt.regex.RegexGroupKind
import ru.kode.pomskykt.regex.RegexLookaround
import ru.kode.pomskykt.regex.RegexProperty
import ru.kode.pomskykt.regex.RegexReference
import ru.kode.pomskykt.regex.RegexRepetition
import ru.kode.pomskykt.syntax.exprs.BoundaryKind
import ru.kode.pomskykt.syntax.exprs.LookaroundKind
import ru.kode.pomskykt.syntax.unicode.Category

/**
 * Recursive-descent parser that converts [RegexToken]s into [Regex] IR nodes.
 */
internal class RegexParser(private val tokens: List<RegexToken>) {
    private var pos = 0
    private var nextGroupIndex = 1

    fun parse(): Regex {
        val result = parseAlternation()
        return result
    }

    private fun parseAlternation(): Regex {
        val first = parseSequence()
        if (!hasMore() || peek() != RegexToken.Pipe) return first
        val alternatives = mutableListOf(first)
        while (hasMore() && peek() == RegexToken.Pipe) {
            advance() // skip Pipe
            alternatives.add(parseSequence())
        }
        return Regex.Alt(RegexAlternation(alternatives))
    }

    private fun parseSequence(): Regex {
        val parts = mutableListOf<Regex>()
        while (hasMore() && peek() !is RegexToken.Pipe && peek() !is RegexToken.CloseParen) {
            parts.add(parseAtom())
        }
        return when (parts.size) {
            0 -> Regex.Literal("")
            1 -> parts[0]
            else -> Regex.Sequence(parts)
        }
    }

    private fun parseAtom(): Regex {
        val base = parseBase()
        return maybeRepetition(base)
    }

    private fun parseBase(): Regex {
        val token = peek()
        return when (token) {
            is RegexToken.Char -> { advance(); charToRegex(token.value) }
            is RegexToken.CodePoint -> { advance(); codePointToRegex(token.value) }
            is RegexToken.Dot -> { advance(); Regex.Dot }
            is RegexToken.Shorthand -> { advance(); shorthandToRegex(token) }
            is RegexToken.UnicodeProperty -> { advance(); propertyToRegex(token) }
            is RegexToken.StartAnchor -> { advance(); Regex.Bound(BoundaryKind.Start) }
            is RegexToken.EndAnchor -> { advance(); Regex.Bound(BoundaryKind.End) }
            is RegexToken.WordBoundary -> { advance(); Regex.Bound(BoundaryKind.Word) }
            is RegexToken.NotWordBoundary -> { advance(); Regex.Bound(BoundaryKind.NotWord) }
            is RegexToken.WordStart -> { advance(); Regex.Bound(BoundaryKind.WordStart) }
            is RegexToken.WordEnd -> { advance(); Regex.Bound(BoundaryKind.WordEnd) }
            is RegexToken.GraphemeCluster -> { advance(); Regex.Grapheme }
            is RegexToken.Recursion -> { advance(); Regex.Recursion }
            is RegexToken.BackrefNumbered -> { advance(); Regex.Ref(RegexReference.Numbered(token.index)) }
            is RegexToken.BackrefNamed -> { advance(); Regex.Ref(RegexReference.Named(token.name)) }
            is RegexToken.OpenBracket -> parseCharClass()
            is RegexToken.OpenParen -> parseCapturingGroup()
            is RegexToken.NonCapturing -> parseNonCapturingGroup()
            is RegexToken.NamedGroup -> parseNamedGroup(token.name)
            is RegexToken.AtomicGroup -> parseAtomicGroup()
            is RegexToken.LookaheadPos -> parseLookaround(LookaroundKind.Ahead)
            is RegexToken.LookaheadNeg -> parseLookaround(LookaroundKind.AheadNegative)
            is RegexToken.LookbehindPos -> parseLookaround(LookaroundKind.Behind)
            is RegexToken.LookbehindNeg -> parseLookaround(LookaroundKind.BehindNegative)
            else -> { advance(); Regex.Literal("") }
        }
    }

    private fun charToRegex(c: Char): Regex = Regex.Literal(c.toString())

    private fun codePointToRegex(cp: Int): Regex {
        return if (cp <= 0xFFFF) {
            Regex.Literal(cp.toChar().toString())
        } else {
            val chars = StringBuilder()
            chars.appendCodePoint(cp)
            Regex.Literal(chars.toString())
        }
    }

    private fun shorthandToRegex(token: RegexToken.Shorthand): Regex =
        Regex.CharSet(RegexCharSet(listOf(RegexCharSetItem.Shorthand(token.kind))))

    private fun propertyToRegex(token: RegexToken.UnicodeProperty): Regex {
        val property = parsePropertyName(token.name)
        return Regex.CharSet(
            RegexCharSet(listOf(RegexCharSetItem.Property(property, token.negative)))
        )
    }

    private fun parsePropertyName(name: String): RegexProperty {
        // Handle sc=/scx= prefixed script names
        if (name.startsWith("sc=") || name.startsWith("scx=")) {
            val stripped = name.removePrefix("scx=").removePrefix("sc=")
            val ext = if (name.startsWith("scx=")) {
                ru.kode.pomskykt.syntax.exprs.ScriptExtension.Yes
            } else {
                ru.kode.pomskykt.syntax.exprs.ScriptExtension.No
            }
            val script = ru.kode.pomskykt.syntax.unicode.Script.entries.find {
                it.fullName.equals(stripped, ignoreCase = true)
            }
            if (script != null) return RegexProperty.ScriptProp(script, ext)
        }

        // Handle Is/In prefixed block names (DotNet: IsXxx, Java/Ruby: InXxx)
        val blockName = when {
            name.startsWith("Is") -> name.removePrefix("Is")
            name.startsWith("In") -> name.removePrefix("In")
            else -> null
        }
        if (blockName != null) {
            val block = ru.kode.pomskykt.syntax.unicode.CodeBlock.entries.find { cb ->
                val normalized = cb.fullName.replace("_", "").replace("-", "")
                val inputNormalized = blockName.replace("_", "").replace("-", "")
                normalized.equals(inputNormalized, ignoreCase = true)
            }
            if (block != null) return RegexProperty.BlockProp(block)
        }

        // Try category by abbreviation or full name
        val category = Category.entries.find {
            it.abbreviation == name || it.name.equals(name, ignoreCase = true)
        }
        if (category != null) return RegexProperty.CategoryProp(category)

        // Try script (without prefix)
        val script = ru.kode.pomskykt.syntax.unicode.Script.entries.find {
            it.fullName.equals(name, ignoreCase = true)
        }
        if (script != null) {
            return RegexProperty.ScriptProp(
                script,
                ru.kode.pomskykt.syntax.exprs.ScriptExtension.Unspecified,
            )
        }

        // Try block (without prefix)
        val block = ru.kode.pomskykt.syntax.unicode.CodeBlock.entries.find { cb ->
            val normalized = cb.fullName.replace("_", "").replace("-", "")
            val inputNormalized = name.replace("_", "").replace("-", "")
            normalized.equals(inputNormalized, ignoreCase = true)
        }
        if (block != null) return RegexProperty.BlockProp(block)

        // Try other properties
        val other = ru.kode.pomskykt.syntax.unicode.OtherProperties.entries.find {
            it.fullName.equals(name, ignoreCase = true) ||
                it.fullName.equals(name.removePrefix("Is"), ignoreCase = true)
        }
        if (other != null) return RegexProperty.OtherProp(other)

        // Fallback: treat as category abbreviation
        val fallback = Category.entries.find { it.abbreviation == name }
        return if (fallback != null) RegexProperty.CategoryProp(fallback)
        else RegexProperty.CategoryProp(Category.Letter)
    }

    private fun parseCharClass(): Regex {
        advance() // skip OpenBracket
        val negative = hasMore() && peek() is RegexToken.CaretInClass
        if (negative) advance()

        val items = mutableListOf<RegexCharSetItem>()
        while (hasMore() && peek() !is RegexToken.CloseBracket) {
            if (peek() is RegexToken.ClassIntersection) {
                advance()
                val firstSet = RegexCharSet(items.toList(), negative)
                val secondItems = mutableListOf<RegexCharSetItem>()
                while (hasMore() && peek() !is RegexToken.CloseBracket &&
                    peek() !is RegexToken.ClassIntersection
                ) {
                    parseCharClassItem(secondItems)
                }
                val secondSet = RegexCharSet(secondItems)
                if (hasMore() && peek() is RegexToken.CloseBracket) advance()
                return Regex.CompoundCharSet(
                    ru.kode.pomskykt.regex.RegexCompoundCharSet(
                        listOf(firstSet, secondSet),
                    )
                )
            }
            parseCharClassItem(items)
        }
        if (hasMore()) advance() // skip CloseBracket

        return Regex.CharSet(RegexCharSet(items, negative))
    }

    private fun parseCharClassItem(items: MutableList<RegexCharSetItem>) {
        val token = peek()
        when (token) {
            is RegexToken.Char -> {
                advance()
                if (hasMore() && peek() is RegexToken.Hyphen) {
                    advance() // skip Hyphen
                    if (hasMore() && peek() !is RegexToken.CloseBracket &&
                        peek() !is RegexToken.ClassIntersection
                    ) {
                        val endToken = peek()
                        advance()
                        val endChar = when (endToken) {
                            is RegexToken.Char -> endToken.value
                            is RegexToken.CodePoint -> endToken.value.toChar()
                            else -> token.value
                        }
                        items.add(RegexCharSetItem.Range(token.value, endChar))
                    } else {
                        items.add(RegexCharSetItem.Char(token.value))
                        items.add(RegexCharSetItem.Char('-'))
                    }
                } else {
                    items.add(RegexCharSetItem.Char(token.value))
                }
            }
            is RegexToken.CodePoint -> {
                advance()
                if (hasMore() && peek() is RegexToken.Hyphen) {
                    advance() // skip Hyphen
                    if (hasMore() && peek() !is RegexToken.CloseBracket &&
                        peek() !is RegexToken.ClassIntersection
                    ) {
                        val endToken = peek()
                        advance()
                        val endCp = when (endToken) {
                            is RegexToken.CodePoint -> endToken.value
                            is RegexToken.Char -> endToken.value.code
                            else -> token.value
                        }
                        if (token.value <= 0xFFFF && endCp <= 0xFFFF) {
                            items.add(RegexCharSetItem.Range(token.value.toChar(), endCp.toChar()))
                        } else {
                            items.add(RegexCharSetItem.CodePoint(token.value))
                            items.add(RegexCharSetItem.Char('-'))
                            items.add(RegexCharSetItem.CodePoint(endCp))
                        }
                    } else {
                        if (token.value <= 0xFFFF) {
                            items.add(RegexCharSetItem.Char(token.value.toChar()))
                        } else {
                            items.add(RegexCharSetItem.CodePoint(token.value))
                        }
                        items.add(RegexCharSetItem.Char('-'))
                    }
                } else if (token.value <= 0xFFFF) {
                    items.add(RegexCharSetItem.Char(token.value.toChar()))
                } else {
                    items.add(RegexCharSetItem.CodePoint(token.value))
                }
            }
            is RegexToken.Shorthand -> {
                advance()
                items.add(RegexCharSetItem.Shorthand(token.kind))
            }
            is RegexToken.UnicodeProperty -> {
                advance()
                val property = parsePropertyName(token.name)
                items.add(RegexCharSetItem.Property(property, token.negative))
            }
            is RegexToken.Hyphen -> {
                advance()
                items.add(RegexCharSetItem.Char('-'))
            }
            else -> advance()
        }
    }

    private fun parseCapturingGroup(): Regex {
        advance() // skip OpenParen
        val index = nextGroupIndex++
        val inner = parseAlternation()
        if (hasMore() && peek() is RegexToken.CloseParen) advance()
        return Regex.Group(RegexGroup(listOf(inner), RegexGroupKind.Numbered(index)))
    }

    private fun parseNonCapturingGroup(): Regex {
        advance() // skip NonCapturing
        val inner = parseAlternation()
        if (hasMore() && peek() is RegexToken.CloseParen) advance()
        return Regex.Group(RegexGroup(listOf(inner), RegexGroupKind.NonCapturing))
    }

    private fun parseNamedGroup(name: String): Regex {
        advance() // skip NamedGroup
        val index = nextGroupIndex++
        val inner = parseAlternation()
        if (hasMore() && peek() is RegexToken.CloseParen) advance()
        return Regex.Group(RegexGroup(listOf(inner), RegexGroupKind.Named(name, index)))
    }

    private fun parseAtomicGroup(): Regex {
        advance() // skip AtomicGroup
        val inner = parseAlternation()
        if (hasMore() && peek() is RegexToken.CloseParen) advance()
        return Regex.Group(RegexGroup(listOf(inner), RegexGroupKind.Atomic))
    }

    private fun parseLookaround(kind: LookaroundKind): Regex {
        advance() // skip lookaround token
        val inner = parseAlternation()
        if (hasMore() && peek() is RegexToken.CloseParen) advance()
        return Regex.Look(RegexLookaround(kind, inner))
    }

    private fun maybeRepetition(base: Regex): Regex {
        if (!hasMore()) return base
        val token = peek()
        val (lower, upper) = when (token) {
            is RegexToken.Star -> { advance(); 0 to null }
            is RegexToken.Plus -> { advance(); 1 to null }
            is RegexToken.Question -> { advance(); 0 to 1 }
            is RegexToken.Repeat -> { advance(); token.min to token.max }
            else -> return base
        }
        val greedy = !(hasMore() && peek() is RegexToken.Lazy)
        if (!greedy) advance() // skip Lazy

        @Suppress("UNCHECKED_CAST")
        return Regex.Rep(
            RegexRepetition(
                inner = base,
                lower = lower,
                upper = upper as Int?,
                greedy = greedy,
            )
        )
    }

    private fun hasMore(): Boolean = pos < tokens.size
    private fun peek(): RegexToken = tokens[pos]
    private fun advance() { pos++ }

    private fun StringBuilder.appendCodePoint(cp: Int) {
        if (cp <= 0xFFFF) {
            append(cp.toChar())
        } else {
            val high = ((cp - 0x10000) shr 10) + 0xD800
            val low = ((cp - 0x10000) and 0x3FF) + 0xDC00
            append(high.toChar())
            append(low.toChar())
        }
    }
}
