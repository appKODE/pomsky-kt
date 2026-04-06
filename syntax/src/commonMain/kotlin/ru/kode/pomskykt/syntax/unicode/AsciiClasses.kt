package ru.kode.pomskykt.syntax.unicode

import ru.kode.pomskykt.syntax.exprs.GroupItem

/**
 * ASCII/POSIX character class parsing.
 *
 * Ported from pomsky-syntax/src/exprs/char_class/ascii.rs.
 */
object AsciiClasses {

    /**
     * Parses an ASCII character class name to a list of [GroupItem]s.
     * Returns null if the name is not a recognized ASCII class.
     * ASCII classes cannot be negated.
     */
    fun parse(name: String): List<GroupItem>? = when (name) {
        "ascii_alpha" -> listOf(
            GroupItem.CharRange('a', 'z'),
            GroupItem.CharRange('A', 'Z'),
        )
        "ascii_alnum" -> listOf(
            GroupItem.CharRange('0', '9'),
            GroupItem.CharRange('a', 'z'),
            GroupItem.CharRange('A', 'Z'),
        )
        "ascii" -> listOf(
            GroupItem.CharRange('\u0000', '\u007F'),
        )
        "ascii_blank" -> listOf(
            GroupItem.Char(' '),
            GroupItem.Char('\t'),
        )
        "ascii_cntrl" -> listOf(
            GroupItem.CharRange('\u0000', '\u001F'),
            GroupItem.Char('\u007F'),
        )
        "ascii_digit" -> listOf(
            GroupItem.CharRange('0', '9'),
        )
        "ascii_graph" -> listOf(
            GroupItem.CharRange('!', '~'),
        )
        "ascii_lower" -> listOf(
            GroupItem.CharRange('a', 'z'),
        )
        "ascii_print" -> listOf(
            GroupItem.CharRange(' ', '~'),
        )
        "ascii_punct" -> listOf(
            GroupItem.CharRange('!', '/'),
            GroupItem.CharRange(':', '@'),
            GroupItem.CharRange('[', '`'),
            GroupItem.CharRange('{', '~'),
        )
        "ascii_space" -> listOf(
            GroupItem.CharRange('\t', '\r'),
            GroupItem.Char(' '),
        )
        "ascii_upper" -> listOf(
            GroupItem.CharRange('A', 'Z'),
        )
        "ascii_word" -> listOf(
            GroupItem.CharRange('0', '9'),
            GroupItem.CharRange('a', 'z'),
            GroupItem.CharRange('A', 'Z'),
            GroupItem.Char('_'),
        )
        "ascii_xdigit" -> listOf(
            GroupItem.CharRange('0', '9'),
            GroupItem.CharRange('a', 'f'),
            GroupItem.CharRange('A', 'F'),
        )
        else -> null
    }
}
