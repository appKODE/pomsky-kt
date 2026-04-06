package ru.kode.pomskykt.regex

import ru.kode.pomskykt.options.RegexFlavor
import ru.kode.pomskykt.syntax.exprs.BoundaryKind
import ru.kode.pomskykt.syntax.exprs.LookaroundKind
import ru.kode.pomskykt.syntax.exprs.ScriptExtension
import ru.kode.pomskykt.syntax.unicode.Category

/**
 * Generates a regex string from the [Regex] IR for a given [RegexFlavor].
 *
 * Ported from pomsky-lib/src/regex/mod.rs codegen methods.
 */
fun Regex.codegen(flavor: RegexFlavor): String {
    val buf = StringBuilder()
    codegenTo(buf, flavor)
    return buf.toString()
}

fun Regex.codegenTo(buf: StringBuilder, flavor: RegexFlavor) {
    when (this) {
        is Regex.Literal -> codegenLiteral(content, buf, flavor)
        is Regex.Unescaped -> buf.append(content)
        is Regex.CharSet -> codegenCharSet(set, buf, flavor, insideCompound = false)
        is Regex.CompoundCharSet -> codegenCompoundCharSet(set, buf, flavor)
        is Regex.Grapheme -> buf.append("\\X")
        is Regex.Dot -> buf.append('.')
        is Regex.Group -> codegenGroup(group, buf, flavor)
        is Regex.Sequence -> {
            val len = parts.size
            for (part in parts) {
                val needsParens = len > 1 && part.needsParensInSequence()
                if (needsParens) buf.append("(?:")
                part.codegenTo(buf, flavor)
                if (needsParens) buf.append(')')
            }
        }
        is Regex.Alt -> codegenAlternation(alternation, buf, flavor)
        is Regex.Rep -> codegenRepetition(repetition, buf, flavor)
        is Regex.Bound -> codegenBoundary(kind, buf, flavor)
        is Regex.Look -> codegenLookaround(lookaround, buf, flavor)
        is Regex.Ref -> codegenReference(reference, buf)
        is Regex.Recursion -> codegenRecursion(buf, flavor)
    }
}

// --- Literal ---

private fun codegenLiteral(content: String, buf: StringBuilder, flavor: RegexFlavor) {
    val chars = content.codePointIterator()
    while (chars.hasNext()) {
        val cp = chars.nextCodePoint()
        if (cp == '\r'.code) {
            codegenCharEscCodePoint('\r'.code, buf, flavor)
            if (chars.hasNext()) {
                val next = chars.nextCodePoint()
                if (next != '\n'.code) codegenCharEscCodePoint(next, buf, flavor)
            }
        } else {
            codegenCharEscCodePoint(cp, buf, flavor)
        }
    }
}

/** Escape a code point for use outside a character class. */
private fun codegenCharEscCodePoint(cp: Int, buf: StringBuilder, flavor: RegexFlavor) {
    if (cp <= 0xFFFF) {
        val c = cp.toChar()
        when (c) {
            '\\', '[', ']', '{', '}', '(', ')',
            '.', '+', '*', '?', '|', '^', '$' -> {
                buf.append('\\')
                buf.append(c)
            }
            else -> compileChar(cp, buf, flavor)
        }
    } else {
        compileChar(cp, buf, flavor)
    }
}

/** Escape a character for use outside a character class. */
internal fun codegenCharEsc(c: Char, buf: StringBuilder, flavor: RegexFlavor) {
    codegenCharEscCodePoint(c.code, buf, flavor)
}

/** Escape a character for use inside `[...]`. */
internal fun codegenCharEscInClass(
    c: Char,
    buf: StringBuilder,
    isFirst: Boolean,
    flavor: RegexFlavor,
) {
    when {
        c == '\\' -> buf.append("\\\\")
        c == '-' -> buf.append("\\-")
        c == '[' || c == ']' -> { buf.append('\\'); buf.append(c) }
        c == '^' && isFirst -> buf.append("\\^")
        (c == '&' || c == '|') && flavor != RegexFlavor.JavaScript -> { buf.append('\\'); buf.append(c) }
        else -> compileChar(c.code, buf, flavor)
    }
}

/** Compile a code point to its regex representation (escape sequences, unicode). */
private fun compileChar(cp: Int, buf: StringBuilder, flavor: RegexFlavor) {
    when {
        cp == '\n'.code -> buf.append("\\n")
        cp == '\r'.code -> buf.append("\\r")
        cp == '\t'.code -> buf.append("\\t")
        cp == 0x0C -> buf.append("\\f") // form feed
        // \a and \e not supported in all flavors; Rust skips them
        cp == ' '.code -> buf.append(' ')
        cp < 0x80 -> {
            val c = cp.toChar()
            if (c in '!'.. '~') {
                buf.append(c)
            } else {
                buf.append("\\x")
                buf.append(cp.toString(16).uppercase().padStart(2, '0'))
            }
        }
        // Alphanumeric non-ASCII chars that fit in a single UTF-16 unit
        cp <= 0xFFFF && cp.toChar().isLetterOrDigit() -> {
            buf.append(cp.toChar())
        }
        cp <= 0xFF && flavor != RegexFlavor.Ruby -> {
            buf.append("\\x")
            buf.append(cp.toString(16).uppercase().padStart(2, '0'))
        }
        cp <= 0xFFFF && flavor != RegexFlavor.Pcre && flavor != RegexFlavor.RE2 -> {
            buf.append("\\u")
            buf.append(cp.toString(16).uppercase().padStart(4, '0'))
        }
        flavor == RegexFlavor.Python -> {
            buf.append("\\U")
            buf.append(cp.toString(16).uppercase().padStart(8, '0'))
        }
        flavor == RegexFlavor.DotNet -> {
            // Encode as surrogate pairs for code points > 0xFFFF
            if (cp > 0xFFFF) {
                val high = ((cp - 0x10000) shr 10) + 0xD800
                val low = ((cp - 0x10000) and 0x3FF) + 0xDC00
                buf.append("\\u")
                buf.append(high.toString(16).uppercase().padStart(4, '0'))
                buf.append("\\u")
                buf.append(low.toString(16).uppercase().padStart(4, '0'))
            } else {
                buf.append("\\u")
                buf.append(cp.toString(16).uppercase().padStart(4, '0'))
            }
        }
        else -> {
            when (flavor) {
                RegexFlavor.Pcre, RegexFlavor.Java, RegexFlavor.Ruby, RegexFlavor.RE2 ->
                    buf.append("\\x")
                else -> buf.append("\\u")
            }
            buf.append("{")
            buf.append(cp.toString(16).uppercase())
            buf.append("}")
        }
    }
}

// --- CharSet ---

private fun codegenCharSet(
    set: RegexCharSet,
    buf: StringBuilder,
    flavor: RegexFlavor,
    insideCompound: Boolean,
) {
    // Single-item optimization
    if (set.items.size == 1 && !insideCompound) {
        val item = set.items[0]
        if (!set.negative) {
            when (item) {
                is RegexCharSetItem.Shorthand -> {
                    buf.append(item.shorthand.str)
                    return
                }
                is RegexCharSetItem.Property -> {
                    codegenProperty(item.property, buf, item.negative, flavor)
                    return
                }
                is RegexCharSetItem.Range -> {
                    if (item.first == item.last) {
                        codegenCharEsc(item.first, buf, flavor)
                        return
                    }
                }
                is RegexCharSetItem.Char -> {
                    codegenCharEsc(item.char, buf, flavor)
                    return
                }
                is RegexCharSetItem.CodePoint -> {
                    compileChar(item.codePoint, buf, flavor)
                    return
                }
                is RegexCharSetItem.Literal -> {}
            }
        } else {
            // Negated single shorthand: output negated variant directly
            if (item is RegexCharSetItem.Shorthand) {
                val neg = negateShorthand(item.shorthand)
                if (neg != null) {
                    buf.append(neg.str)
                    return
                }
            }
            // Negated single property: output \P{...}
            if (item is RegexCharSetItem.Property) {
                codegenProperty(item.property, buf, !item.negative, flavor)
                return
            }
        }
    }

    if (set.negative) buf.append("[^") else if (!insideCompound) buf.append('[')

    // Sort items: props (Shorthand, Property) first, then ranges/chars by code point
    // This matches Rust's UnicodeSet which iterates props() then ranges() (BTreeSet sorted by code point)
    val props = mutableListOf<RegexCharSetItem>()
    val rangesAndChars = mutableListOf<RegexCharSetItem>()
    for (item in set.items) {
        when (item) {
            is RegexCharSetItem.Shorthand, is RegexCharSetItem.Property -> props.add(item)
            is RegexCharSetItem.Char, is RegexCharSetItem.Range,
            is RegexCharSetItem.Literal, is RegexCharSetItem.CodePoint -> rangesAndChars.add(item)
        }
    }

    // Sort ranges/chars by first code point
    rangesAndChars.sortWith(Comparator { a, b ->
        val aFirst = when (a) {
            is RegexCharSetItem.Char -> a.char.code
            is RegexCharSetItem.Range -> a.first.code
            is RegexCharSetItem.Literal -> if (a.content.isNotEmpty()) a.content[0].code else 0
            is RegexCharSetItem.CodePoint -> a.codePoint
            else -> 0
        }
        val bFirst = when (b) {
            is RegexCharSetItem.Char -> b.char.code
            is RegexCharSetItem.Range -> b.first.code
            is RegexCharSetItem.Literal -> if (b.content.isNotEmpty()) b.content[0].code else 0
            is RegexCharSetItem.CodePoint -> b.codePoint
            else -> 0
        }
        aFirst.compareTo(bFirst)
    })

    val sortedItems = props + rangesAndChars

    var isFirst = true
    for (item in sortedItems) {
        when (item) {
            is RegexCharSetItem.Shorthand -> buf.append(item.shorthand.str)
            is RegexCharSetItem.Property -> codegenProperty(item.property, buf, item.negative, flavor)
            is RegexCharSetItem.Char -> {
                codegenCharEscInClass(item.char, buf, isFirst, flavor)
            }
            is RegexCharSetItem.Range -> {
                codegenCharEscInClass(item.first, buf, isFirst, flavor)
                if (item.first.code + 1 < item.last.code) buf.append('-')
                codegenCharEscInClass(item.last, buf, false, flavor)
            }
            is RegexCharSetItem.Literal -> buf.append(item.content)
            is RegexCharSetItem.CodePoint -> compileChar(item.codePoint, buf, flavor)
        }
        isFirst = false
    }

    if (set.negative || !insideCompound) buf.append(']')
}

private fun codegenCompoundCharSet(set: RegexCompoundCharSet, buf: StringBuilder, flavor: RegexFlavor) {
    buf.append('[')
    if (set.negative) buf.append('^')
    var first = true
    for (charSet in set.sets) {
        if (!first) buf.append("&&")
        codegenCharSet(charSet, buf, flavor, insideCompound = true)
        first = false
    }
    buf.append(']')
}

// --- Property ---

/**
 * Top-level general categories that use single-letter abbreviations
 * in Java/Pcre/Rust/Ruby flavors (e.g., \pL instead of \p{Letter}).
 */
private val singleLetterCategories = setOf(
    Category.Letter,
    Category.Mark,
    Category.Number,
    Category.Punctuation,
    Category.Symbol,
    Category.Separator,
    Category.Other,
)

private fun codegenProperty(
    property: RegexProperty,
    buf: StringBuilder,
    negative: Boolean,
    flavor: RegexFlavor,
) {
    // Determine if this is a single-letter category (no braces needed)
    val isSingle = property is RegexProperty.CategoryProp &&
        property.category in singleLetterCategories &&
        flavor in listOf(RegexFlavor.Java, RegexFlavor.Pcre, RegexFlavor.Rust, RegexFlavor.Ruby)

    buf.append(if (negative) "\\P" else "\\p")
    if (!isSingle) {
        buf.append('{')
    }

    when (property) {
        is RegexProperty.CategoryProp -> {
            buf.append(property.category.abbreviation)
        }
        is RegexProperty.ScriptProp -> {
            val ext = property.extension
            val scriptName = property.script.fullName
            // Add sc=/scx= prefix for JS/Java flavors, or when explicitly specified
            if (flavor == RegexFlavor.JavaScript || flavor == RegexFlavor.Java ||
                ext != ScriptExtension.Unspecified
            ) {
                buf.append(if (ext == ScriptExtension.Yes) "scx=" else "sc=")
            }
            buf.append(scriptName)
        }
        is RegexProperty.BlockProp -> {
            // Get block name with hyphens preserved (matching Rust's as_str())
            val blockName = blockCodegenName(property.block.fullName)
            when (flavor) {
                RegexFlavor.DotNet -> {
                    buf.append("Is")
                    // .NET: remove underscores, but keep "and" lowercase, preserve hyphens
                    buf.append(
                        blockName
                            .replace("_And_", "_and_")
                            .replace("_", "")
                    )
                }
                RegexFlavor.Java -> {
                    buf.append("In")
                    // Java: replace hyphens with underscores
                    buf.append(blockName.replace('-', '_'))
                }
                RegexFlavor.Ruby -> {
                    buf.append("In")
                    // Ruby uses Title_Case for block names (capitalize each word)
                    buf.append(blockName.split("_").joinToString("_") { word ->
                        if (word.isEmpty()) word else word[0].uppercaseChar() + word.drop(1)
                    })
                }
                else -> buf.append(blockName)
            }
        }
        is RegexProperty.OtherProp -> {
            if (flavor == RegexFlavor.Java) {
                buf.append("Is")
            }
            buf.append(property.property.fullName)
        }
    }

    if (!isSingle) {
        buf.append('}')
    }
}

// --- Group ---

private fun codegenGroup(group: RegexGroup, buf: StringBuilder, flavor: RegexFlavor) {
    when (val kind = group.kind) {
        is RegexGroupKind.Named -> {
            when (flavor) {
                RegexFlavor.Python, RegexFlavor.Pcre, RegexFlavor.Rust ->
                    buf.append("(?P<${kind.name}>")
                else -> buf.append("(?<${kind.name}>")
            }
            for (part in group.parts) {
                part.codegenTo(buf, flavor)
            }
            buf.append(')')
        }
        is RegexGroupKind.Numbered -> {
            buf.append('(')
            for (part in group.parts) {
                part.codegenTo(buf, flavor)
            }
            buf.append(')')
        }
        is RegexGroupKind.Atomic -> {
            buf.append("(?>")
            for (part in group.parts) {
                part.codegenTo(buf, flavor)
            }
            buf.append(')')
        }
        is RegexGroupKind.NonCapturing -> {
            // Rust: Normal groups don't emit (?:...) wrapper — they just emit their parts,
            // but wrap individual parts that need_parens_in_sequence when there are multiple parts.
            val len = group.parts.size
            for (part in group.parts) {
                val needsParens = (len > 1 && part.needsParensInSequence()) ||
                    (len == 1 && part is Regex.Unescaped)
                if (needsParens) buf.append("(?:")
                part.codegenTo(buf, flavor)
                if (needsParens) buf.append(')')
            }
        }
    }
}

// --- Alternation ---

private fun codegenAlternation(alt: RegexAlternation, buf: StringBuilder, flavor: RegexFlavor) {
    alt.alternatives.forEachIndexed { i, regex ->
        if (i > 0) buf.append('|')
        regex.codegenTo(buf, flavor)
    }
}

// --- Repetition ---

private fun codegenRepetition(rep: RegexRepetition, buf: StringBuilder, flavor: RegexFlavor) {
    // Wrap inner in (?:...) if it needs parentheses
    val needsParens = needsParensBeforeRepetition(rep.inner, flavor)
    if (needsParens) buf.append("(?:")
    rep.inner.codegenTo(buf, flavor)
    if (needsParens) buf.append(')')

    val lower = rep.lower
    val upper = rep.upper
    var addLazy = !rep.greedy

    when {
        lower == 1 && upper == 1 -> return // {1,1} = no repetition at all
        lower == 0 && upper == 1 -> buf.append('?')
        lower == 0 && upper == null -> buf.append('*')
        lower == 1 && upper == null -> buf.append('+')
        lower == upper -> { buf.append("{$lower}"); addLazy = false }
        upper == null -> buf.append("{$lower,}")
        lower == 0 -> buf.append("{0,$upper}")
        else -> buf.append("{$lower,$upper}")
    }

    if (addLazy) buf.append('?')
}

private fun needsParensBeforeRepetition(regex: Regex, flavor: RegexFlavor): Boolean = when (regex) {
    is Regex.Literal -> regex.content.isEmpty() || regex.content.length > 1 ||
        // single-char check: if has surrogate pairs, multiple chars
        (regex.content.length == 1 && false) // single char doesn't need parens
    is Regex.Sequence -> true
    is Regex.Alt -> true
    is Regex.Rep -> true
    is Regex.Bound -> true
    is Regex.Unescaped -> true
    is Regex.Group -> {
        // For non-capturing groups: delegate to the inner if single part
        if (regex.group.kind is RegexGroupKind.NonCapturing && regex.group.parts.size == 1) {
            needsParensBeforeRepetition(regex.group.parts[0], flavor)
        } else if (regex.group.kind is RegexGroupKind.NonCapturing) {
            true
        } else {
            false
        }
    }
    is Regex.Look -> flavor == RegexFlavor.JavaScript
    is Regex.CharSet -> false
    is Regex.CompoundCharSet -> false
    is Regex.Grapheme -> false
    is Regex.Ref -> false
    is Regex.Dot -> false
    is Regex.Recursion -> false
}

// --- Boundary ---

private fun codegenBoundary(kind: BoundaryKind, buf: StringBuilder, flavor: RegexFlavor) {
    when (kind) {
        BoundaryKind.Start -> buf.append('^')
        BoundaryKind.End -> buf.append('$')
        BoundaryKind.Word -> buf.append("\\b")
        BoundaryKind.NotWord -> buf.append("\\B")
        BoundaryKind.WordStart -> when (flavor) {
            RegexFlavor.Pcre -> buf.append("[[:<:]]")
            RegexFlavor.Rust -> buf.append("\\<")
            else -> buf.append("(?<!\\w)(?=\\w)")
        }
        BoundaryKind.WordEnd -> when (flavor) {
            RegexFlavor.Pcre -> buf.append("[[:>:]]")
            RegexFlavor.Rust -> buf.append("\\>")
            else -> buf.append("(?<=\\w)(?!\\w)")
        }
    }
}

// --- Lookaround ---

private fun codegenLookaround(look: RegexLookaround, buf: StringBuilder, flavor: RegexFlavor) {
    buf.append(when (look.kind) {
        LookaroundKind.Ahead -> "(?="
        LookaroundKind.Behind -> "(?<="
        LookaroundKind.AheadNegative -> "(?!"
        LookaroundKind.BehindNegative -> "(?<!"
    })
    look.inner.codegenTo(buf, flavor)
    buf.append(')')
}

// --- Reference ---

private fun codegenReference(ref: RegexReference, buf: StringBuilder) {
    when (ref) {
        is RegexReference.Numbered -> {
            // Rust wraps all numbered references in (?:...) to prevent
            // ambiguity with octal escapes
            buf.append("(?:\\${ref.index})")
        }
        is RegexReference.Named -> buf.append("\\k<${ref.name}>")
    }
}

// --- Recursion ---

private fun codegenRecursion(buf: StringBuilder, flavor: RegexFlavor) {
    buf.append("\\g<0>")
}

// --- Shorthand helpers ---

private fun negateShorthand(sh: RegexShorthand): RegexShorthand? = when (sh) {
    RegexShorthand.Word -> RegexShorthand.NotWord
    RegexShorthand.Digit -> RegexShorthand.NotDigit
    RegexShorthand.Space -> RegexShorthand.NotSpace
    RegexShorthand.NotWord -> RegexShorthand.Word
    RegexShorthand.NotDigit -> RegexShorthand.Digit
    RegexShorthand.NotSpace -> RegexShorthand.Space
    RegexShorthand.HorizSpace -> null // no negated form
    RegexShorthand.VertSpace -> null
}

// --- needs_parens_in_sequence (matches Rust) ---

/**
 * Whether this regex node needs (?:...) wrapping when it appears
 * as a part in a multi-part Normal group (sequence context).
 */
internal fun Regex.needsParensInSequence(): Boolean = when (this) {
    is Regex.Alt -> true
    else -> false
}

// --- Block name mapping (fullName underscores -> Rust as_str() with hyphens) ---

/**
 * Converts a CodeBlock fullName (all underscores) to the codegen name that
 * preserves hyphens matching Rust's `CodeBlock::as_str()` output.
 *
 * Unicode block names use hyphens for single-letter suffixes and compound names
 * like "Latin Extended-A", "Phags-pa", "Cypro-Minoan", "Latin-1 Supplement".
 */
private val blockNameOverrides: Map<String, String> by lazy {
    mapOf(
        "Latin_1_Supplement" to "Latin-1_Supplement",
        "Latin_Extended_A" to "Latin_Extended-A",
        "Latin_Extended_B" to "Latin_Extended-B",
        "Latin_Extended_C" to "Latin_Extended-C",
        "Latin_Extended_D" to "Latin_Extended-D",
        "Latin_Extended_E" to "Latin_Extended-E",
        "Latin_Extended_F" to "Latin_Extended-F",
        "Latin_Extended_G" to "Latin_Extended-G",
        "Arabic_Extended_A" to "Arabic_Extended-A",
        "Arabic_Extended_B" to "Arabic_Extended-B",
        "Arabic_Extended_C" to "Arabic_Extended-C",
        "Cyrillic_Extended_A" to "Cyrillic_Extended-A",
        "Cyrillic_Extended_B" to "Cyrillic_Extended-B",
        "Cyrillic_Extended_C" to "Cyrillic_Extended-C",
        "Cyrillic_Extended_D" to "Cyrillic_Extended-D",
        "Miscellaneous_Mathematical_Symbols_A" to "Miscellaneous_Mathematical_Symbols-A",
        "Miscellaneous_Mathematical_Symbols_B" to "Miscellaneous_Mathematical_Symbols-B",
        "Supplemental_Arrows_A" to "Supplemental_Arrows-A",
        "Supplemental_Arrows_B" to "Supplemental_Arrows-B",
        "Supplemental_Arrows_C" to "Supplemental_Arrows-C",
        "Phags_Pa" to "Phags-pa",
        "Hangul_Jamo_Extended_A" to "Hangul_Jamo_Extended-A",
        "Hangul_Jamo_Extended_B" to "Hangul_Jamo_Extended-B",
        "Myanmar_Extended_A" to "Myanmar_Extended-A",
        "Myanmar_Extended_B" to "Myanmar_Extended-B",
        "Ethiopic_Extended_A" to "Ethiopic_Extended-A",
        "Ethiopic_Extended_B" to "Ethiopic_Extended-B",
        "Arabic_Presentation_Forms_A" to "Arabic_Presentation_Forms-A",
        "Arabic_Presentation_Forms_B" to "Arabic_Presentation_Forms-B",
        "Unified_Canadian_Aboriginal_Syllabics_Extended_A" to "Unified_Canadian_Aboriginal_Syllabics_Extended-A",
        "Devanagari_Extended_A" to "Devanagari_Extended-A",
        "Cypro_Minoan" to "Cypro-Minoan",
        "Kana_Extended_A" to "Kana_Extended-A",
        "Kana_Extended_B" to "Kana_Extended-B",
        "CJK_Unified_Ideographs_Extension_A" to "CJK_Unified_Ideographs_Extension-A",
        "CJK_Unified_Ideographs_Extension_B" to "CJK_Unified_Ideographs_Extension-B",
        "CJK_Unified_Ideographs_Extension_C" to "CJK_Unified_Ideographs_Extension-C",
        "CJK_Unified_Ideographs_Extension_D" to "CJK_Unified_Ideographs_Extension-D",
        "CJK_Unified_Ideographs_Extension_E" to "CJK_Unified_Ideographs_Extension-E",
        "CJK_Unified_Ideographs_Extension_F" to "CJK_Unified_Ideographs_Extension-F",
        "CJK_Unified_Ideographs_Extension_G" to "CJK_Unified_Ideographs_Extension-G",
        "CJK_Unified_Ideographs_Extension_H" to "CJK_Unified_Ideographs_Extension-H",
        "CJK_Unified_Ideographs_Extension_I" to "CJK_Unified_Ideographs_Extension-I",
        "CJK_Compatibility_Ideographs_Supplement" to "CJK_Compatibility_Ideographs_Supplement",
    )
}

private fun blockCodegenName(fullName: String): String =
    blockNameOverrides[fullName] ?: fullName

// --- Code point iterator for strings (handles surrogate pairs) ---

private fun String.codePointIterator(): CodePointIterator = CodePointIterator(this)

private class CodePointIterator(private val str: String) {
    private var index = 0

    fun hasNext(): Boolean = index < str.length

    fun nextCodePoint(): Int {
        val c = str[index]
        if (c.isHighSurrogate() && index + 1 < str.length) {
            val low = str[index + 1]
            if (low.isLowSurrogate()) {
                index += 2
                return ((c.code - 0xD800) shl 10) + (low.code - 0xDC00) + 0x10000
            }
        }
        index++
        return c.code
    }
}
