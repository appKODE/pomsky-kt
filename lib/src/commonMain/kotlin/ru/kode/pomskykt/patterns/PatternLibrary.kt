package ru.kode.pomskykt.patterns

import ru.kode.pomskykt.syntax.exprs.Rule
import ru.kode.pomskykt.syntax.parse.parse

object PatternLibrary {
    val patterns: List<Pair<String, Rule>> by lazy {
        definitions.map { (name, pomsky) ->
            val (rule, diags) = parse(pomsky)
            requireNotNull(rule) { "Built-in pattern '$name' failed to parse: ${diags.map { it.kind }}" }
            name to rule
        }
    }

    private val definitions = listOf(
        "email" to "[word '.' '+' '-']+ '@' [word '-']+ ('.' [word '-']+)+",
        "semver" to "[digit]+ '.' [digit]+ '.' [digit]+ ('-' [word '.' '-']+)? ('+' [word '.' '-']+)?",
        "uuid" to "[digit 'a'-'f' 'A'-'F']{8} '-' [digit 'a'-'f' 'A'-'F']{4} '-' [digit 'a'-'f' 'A'-'F']{4} '-' [digit 'a'-'f' 'A'-'F']{4} '-' [digit 'a'-'f' 'A'-'F']{12}",
        "date_iso8601" to "[digit]{4} '-' [digit]{2} '-' [digit]{2}",
        "time_24h" to "[digit]{2} ':' [digit]{2} (':' [digit]{2})?",
        "hex_color" to "'#' ([digit 'a'-'f' 'A'-'F']{6} | [digit 'a'-'f' 'A'-'F']{3})",
        "ipv4" to "range '0'-'255' ('.' range '0'-'255'){3}",
        "jira_ticket" to "['A'-'Z']{2,10} '-' [digit]+",
        "conventional_commit" to "('feat' | 'fix' | 'docs' | 'style' | 'refactor' | 'perf' | 'test' | 'build' | 'ci' | 'chore' | 'revert') ('(' [word '-' '/' '.']+ ')')? '!'? ': ' .+",
    )
}
