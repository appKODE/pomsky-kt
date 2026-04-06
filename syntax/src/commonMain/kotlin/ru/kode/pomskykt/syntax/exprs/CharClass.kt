package ru.kode.pomskykt.syntax.exprs

import ru.kode.pomskykt.syntax.Span
import ru.kode.pomskykt.syntax.unicode.Category
import ru.kode.pomskykt.syntax.unicode.CodeBlock
import ru.kode.pomskykt.syntax.unicode.OtherProperties
import ru.kode.pomskykt.syntax.unicode.Script

/** Character class: `[a-z]`, `[word]`, `[Latin]`. */
data class CharClass(
    val inner: List<GroupItem>,
    val span: Span,
    val unicodeAware: Boolean = true,
)

/** A single item in a character class. */
sealed class GroupItem {
    /** A single character: `'a'`. */
    data class Char(val char: kotlin.Char) : GroupItem()

    /** A supplementary (above U+FFFF) code point stored as an int. */
    data class CodePoint(val codePoint: Int) : GroupItem()

    /** A character range: `'a'-'z'`. */
    data class CharRange(val first: kotlin.Char, val last: kotlin.Char) : GroupItem()

    /** A named class: `[word]`, `[Latin]`, `[Letter]`. */
    data class Named(
        val name: GroupName,
        val negative: Boolean,
        val span: Span,
    ) : GroupItem()
}

/** A named character group (shorthand, Unicode category/script/block/property). */
sealed class GroupName {
    /** `[word]` / `[w]` — word character. */
    data object Word : GroupName()
    /** `[digit]` / `[d]` — digit. */
    data object Digit : GroupName()
    /** `[space]` / `[s]` — whitespace. */
    data object Space : GroupName()
    /** `[horiz_space]` / `[h]` — horizontal whitespace. */
    data object HorizSpace : GroupName()
    /** `[vert_space]` / `[v]` — vertical whitespace. */
    data object VertSpace : GroupName()
    /** Unicode general category (e.g., `[Letter]`, `[Number]`). */
    data class CategoryName(val category: Category) : GroupName()
    /** Unicode script (e.g., `[Latin]`, `[Greek]`). */
    data class ScriptName(
        val script: Script,
        val extension: ScriptExtension,
    ) : GroupName()
    /** Unicode block (e.g., `[InBasicLatin]`). */
    data class CodeBlockName(val block: CodeBlock) : GroupName()
    /** Other Unicode property (e.g., `[ascii]`). */
    data class OtherPropertyName(val property: OtherProperties) : GroupName()
}

/** Whether a script match uses script extensions (scx:) or strict script (sc:). */
enum class ScriptExtension {
    Yes,
    No,
    Unspecified,
}
