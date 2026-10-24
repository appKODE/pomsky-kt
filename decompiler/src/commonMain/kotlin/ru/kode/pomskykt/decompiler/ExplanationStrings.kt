package ru.kode.pomskykt.decompiler

import ru.kode.pomskykt.regex.RegexShorthand
import ru.kode.pomskykt.syntax.exprs.BoundaryKind
import ru.kode.pomskykt.syntax.exprs.LookaroundKind

/** Language of [Decompiler.explain] output. */
enum class ExplainLocale {
    EN, RU,
}

/**
 * Per-locale phrase table used by [ExplanationEmitter].
 *
 * Repetition phrases come in two forms: a prefix followed by the emitted node
 * (`one or more of `), and a plural form for simple char sets (`one or more digits (0-9)`).
 * The plural form defaults to prefix + noun, which suits languages that avoid number agreement.
 */
internal interface ExplanationStrings {
    val grapheme: String
    val anyCharacter: String
    val recursion: String
    val unescaped: String
    val charMatching: String
    val charNotMatching: String
    val article: String
    val negatedArticle: String
    val not: String
    val unicodeCategory: String
    val unicodeScript: String
    val unicodeBlock: String
    val unicodeProperty: String
    val intersectionOf: String
    val and: String
    val followedBy: String
    val either: String
    val orLast: String
    val lazy: String
    val optionally: String
    val anyNumberOf: String
    val oneOrMoreOf: String

    fun savedAsGroup(index: Int): String
    fun savedAsName(name: String): String
    fun exactlyOf(count: Int): String
    fun atLeastOf(count: Int): String
    fun betweenOf(lower: Int, upper: Int): String
    fun reference(name: String): String
    fun reference(index: Int): String
    fun boundary(kind: BoundaryKind): String
    fun lookaround(kind: LookaroundKind): String
    fun shorthandDescription(sh: RegexShorthand): String
    fun negatedShorthandDescription(sh: RegexShorthand): String
    fun shorthandBrief(sh: RegexShorthand): String
    fun shorthandPlural(sh: RegexShorthand): String
    fun rangeDescription(first: Char, last: Char): String
    fun rangePlural(first: Char, last: Char): String

    fun exactlyPlural(count: Int, plural: String): String = exactlyOf(count) + plural
    fun anyNumberPlural(plural: String): String = anyNumberOf + plural
    fun oneOrMorePlural(plural: String): String = oneOrMoreOf + plural
    fun atLeastPlural(count: Int, plural: String): String = atLeastOf(count) + plural

    companion object {
        fun of(locale: ExplainLocale): ExplanationStrings = when (locale) {
            ExplainLocale.EN -> EnglishStrings
            ExplainLocale.RU -> RussianStrings
        }
    }
}

internal object EnglishStrings : ExplanationStrings {
    override val grapheme = "a single visible character (grapheme)"
    override val anyCharacter = "any character"
    override val recursion = "the whole pattern repeated inside itself"
    override val unescaped = "literal regex pattern: "
    override val charMatching = "a character matching: "
    override val charNotMatching = "a character not matching: "
    override val article = "a "
    override val negatedArticle = "a non-"
    override val not = "not "
    override val unicodeCategory = "Unicode "
    override val unicodeScript = "Unicode script "
    override val unicodeBlock = "Unicode block "
    override val unicodeProperty = "Unicode property "
    override val intersectionOf = "intersection of "
    override val and = " and "
    override val followedBy = ", followed by "
    override val either = "either "
    override val orLast = ", or "
    override val lazy = " (lazy)"
    override val optionally = "optionally "
    override val anyNumberOf = "any number of "
    override val oneOrMoreOf = "one or more of "

    override fun savedAsGroup(index: Int) = " (saved as group $index)"
    override fun savedAsName(name: String) = " (saved as \"$name\")"
    override fun exactlyOf(count: Int) = "exactly $count of "
    override fun atLeastOf(count: Int) = "$count or more of "
    override fun betweenOf(lower: Int, upper: Int) = "between $lower and $upper of "
    override fun exactlyPlural(count: Int, plural: String) = "exactly $count $plural"
    override fun anyNumberPlural(plural: String) = "any number of $plural"
    override fun oneOrMorePlural(plural: String) = "one or more $plural"
    override fun atLeastPlural(count: Int, plural: String) = "$count or more $plural"
    override fun reference(name: String) = "the same \"$name\" text again"
    override fun reference(index: Int) = "the same group $index text again"

    override fun boundary(kind: BoundaryKind) = when (kind) {
        BoundaryKind.Start -> "start of string"
        BoundaryKind.End -> "end of string"
        BoundaryKind.Word -> "a word boundary (start or end of a word)"
        BoundaryKind.NotWord -> "a non-word boundary (middle of a word)"
        BoundaryKind.WordStart -> "start of word"
        BoundaryKind.WordEnd -> "end of word"
    }

    override fun lookaround(kind: LookaroundKind) = when (kind) {
        LookaroundKind.Ahead -> "if followed by "
        LookaroundKind.AheadNegative -> "if not followed by "
        LookaroundKind.Behind -> "if preceded by "
        LookaroundKind.BehindNegative -> "if not preceded by "
    }

    override fun shorthandDescription(sh: RegexShorthand) = when (sh) {
        RegexShorthand.Word -> "a word character (letter, digit, or underscore)"
        RegexShorthand.Digit -> "a digit (0-9)"
        RegexShorthand.Space -> "a whitespace character (space, tab, newline)"
        RegexShorthand.NotWord -> "a non-word character"
        RegexShorthand.NotDigit -> "a non-digit character"
        RegexShorthand.NotSpace -> "a non-whitespace character"
        RegexShorthand.VertSpace -> "a vertical whitespace character (newline, etc.)"
        RegexShorthand.HorizSpace -> "a horizontal whitespace character (space, tab)"
    }

    override fun negatedShorthandDescription(sh: RegexShorthand) = when (sh) {
        RegexShorthand.Word -> "a non-word character"
        RegexShorthand.Digit -> "a non-digit character"
        RegexShorthand.Space -> "a non-whitespace character"
        RegexShorthand.NotWord -> "a word character"
        RegexShorthand.NotDigit -> "a digit (0-9)"
        RegexShorthand.NotSpace -> "a whitespace character"
        RegexShorthand.VertSpace -> "a non-vertical-whitespace character"
        RegexShorthand.HorizSpace -> "a non-horizontal-whitespace character"
    }

    override fun shorthandBrief(sh: RegexShorthand) = when (sh) {
        RegexShorthand.Word -> "word character (letter/digit/_)"
        RegexShorthand.Digit -> "digit (0-9)"
        RegexShorthand.Space -> "whitespace"
        RegexShorthand.NotWord -> "non-word character"
        RegexShorthand.NotDigit -> "non-digit"
        RegexShorthand.NotSpace -> "non-whitespace"
        RegexShorthand.VertSpace -> "vertical whitespace"
        RegexShorthand.HorizSpace -> "horizontal whitespace"
    }

    override fun shorthandPlural(sh: RegexShorthand) = when (sh) {
        RegexShorthand.Word -> "word characters (letters, digits, or underscores)"
        RegexShorthand.Digit -> "digits (0-9)"
        RegexShorthand.Space -> "whitespace characters"
        RegexShorthand.NotWord -> "non-word characters"
        RegexShorthand.NotDigit -> "non-digit characters"
        RegexShorthand.NotSpace -> "non-whitespace characters"
        RegexShorthand.VertSpace -> "vertical whitespace characters"
        RegexShorthand.HorizSpace -> "horizontal whitespace characters"
    }

    override fun rangeDescription(first: Char, last: Char) = when {
        first == 'a' && last == 'z' -> "letter (a-z)"
        first == 'A' && last == 'Z' -> "uppercase letter (A-Z)"
        first == '0' && last == '9' -> "digit (0-9)"
        else -> "character in range '$first'-'$last'"
    }

    override fun rangePlural(first: Char, last: Char) = when {
        first == 'a' && last == 'z' -> "letters (a-z)"
        first == 'A' && last == 'Z' -> "uppercase letters (A-Z)"
        first == '0' && last == '9' -> "digits (0-9)"
        else -> "characters in range '$first'-'$last'"
    }
}

/**
 * Russian phrases. Repetitions use "N раз: <что>" so nouns stay in the nominative
 * singular and need no number agreement.
 */
internal object RussianStrings : ExplanationStrings {
    override val grapheme = "один видимый символ (графема)"
    override val anyCharacter = "любой символ"
    override val recursion = "весь шаблон, повторённый внутри себя"
    override val unescaped = "литеральный шаблон regex: "
    override val charMatching = "символ из набора: "
    override val charNotMatching = "символ не из набора: "
    override val article = ""
    override val negatedArticle = "не "
    override val not = "не "
    override val unicodeCategory = "Unicode-категория "
    override val unicodeScript = "Unicode-письменность "
    override val unicodeBlock = "Unicode-блок "
    override val unicodeProperty = "Unicode-свойство "
    override val intersectionOf = "пересечение "
    override val and = " и "
    override val followedBy = ", затем "
    override val either = "либо "
    override val orLast = ", либо "
    override val lazy = " (лениво)"
    override val optionally = "необязательно "
    override val anyNumberOf = "любое число раз: "
    override val oneOrMoreOf = "1 или более раз: "

    override fun savedAsGroup(index: Int) = " (сохраняется как группа $index)"
    override fun savedAsName(name: String) = " (сохраняется как \"$name\")"
    override fun exactlyOf(count: Int) = "ровно $count ${times(count)}: "
    override fun atLeastOf(count: Int) = "$count или более раз: "
    override fun betweenOf(lower: Int, upper: Int) = "от $lower до $upper раз: "
    override fun reference(name: String) = "тот же текст \"$name\" ещё раз"
    override fun reference(index: Int) = "тот же текст группы $index ещё раз"

    /** "раз" / "раза" agreement for a count: 1 раз, 2 раза, 5 раз, 22 раза. */
    private fun times(count: Int): String {
        val lastTwo = count % 100
        return if (count % 10 in 2..4 && lastTwo !in 12..14) "раза" else "раз"
    }

    override fun boundary(kind: BoundaryKind) = when (kind) {
        BoundaryKind.Start -> "начало строки"
        BoundaryKind.End -> "конец строки"
        BoundaryKind.Word -> "граница слова (начало или конец слова)"
        BoundaryKind.NotWord -> "не граница слова (середина слова)"
        BoundaryKind.WordStart -> "начало слова"
        BoundaryKind.WordEnd -> "конец слова"
    }

    override fun lookaround(kind: LookaroundKind) = when (kind) {
        LookaroundKind.Ahead -> "если далее следует "
        LookaroundKind.AheadNegative -> "если далее не следует "
        LookaroundKind.Behind -> "если перед этим стоит "
        LookaroundKind.BehindNegative -> "если перед этим не стоит "
    }

    override fun shorthandDescription(sh: RegexShorthand) = when (sh) {
        RegexShorthand.Word -> "символ слова (буква, цифра или подчёркивание)"
        RegexShorthand.Digit -> "цифра (0-9)"
        RegexShorthand.Space -> "пробельный символ (пробел, табуляция, перевод строки)"
        RegexShorthand.NotWord -> "не символ слова"
        RegexShorthand.NotDigit -> "не цифра"
        RegexShorthand.NotSpace -> "не пробельный символ"
        RegexShorthand.VertSpace -> "вертикальный пробельный символ (перевод строки и т. п.)"
        RegexShorthand.HorizSpace -> "горизонтальный пробельный символ (пробел, табуляция)"
    }

    override fun negatedShorthandDescription(sh: RegexShorthand) = when (sh) {
        RegexShorthand.Word -> "не символ слова"
        RegexShorthand.Digit -> "не цифра"
        RegexShorthand.Space -> "не пробельный символ"
        RegexShorthand.NotWord -> "символ слова"
        RegexShorthand.NotDigit -> "цифра (0-9)"
        RegexShorthand.NotSpace -> "пробельный символ"
        RegexShorthand.VertSpace -> "не вертикальный пробельный символ"
        RegexShorthand.HorizSpace -> "не горизонтальный пробельный символ"
    }

    override fun shorthandBrief(sh: RegexShorthand) = when (sh) {
        RegexShorthand.Word -> "символ слова (буква/цифра/_)"
        RegexShorthand.Digit -> "цифра (0-9)"
        RegexShorthand.Space -> "пробельный символ"
        RegexShorthand.NotWord -> "не символ слова"
        RegexShorthand.NotDigit -> "не цифра"
        RegexShorthand.NotSpace -> "не пробельный символ"
        RegexShorthand.VertSpace -> "вертикальный пробельный символ"
        RegexShorthand.HorizSpace -> "горизонтальный пробельный символ"
    }

    override fun shorthandPlural(sh: RegexShorthand) = shorthandDescription(sh)

    override fun rangeDescription(first: Char, last: Char) = when {
        first == 'a' && last == 'z' -> "буква (a-z)"
        first == 'A' && last == 'Z' -> "заглавная буква (A-Z)"
        first == '0' && last == '9' -> "цифра (0-9)"
        else -> "символ в диапазоне '$first'-'$last'"
    }

    override fun rangePlural(first: Char, last: Char) = rangeDescription(first, last)
}
