package ru.kode.pomskykt.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.ProgramResult
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.choice
import ru.kode.pomskykt.Expr
import ru.kode.pomskykt.diagnose.Severity
import ru.kode.pomskykt.options.CompileOptions
import ru.kode.pomskykt.options.RegexFlavor
import ru.kode.pomskykt.syntax.exprs.TestCase

/**
 * Run test blocks from a Pomsky expression against the compiled regex.
 */
class TestCommand : CliktCommand(name = "test") {
    private val input by argument(help = "Pomsky expression with test blocks")

    private val flavor by option(
        "-f", "--flavor",
    ).choice(
        "pcre" to RegexFlavor.Pcre,
        "python" to RegexFlavor.Python,
        "java" to RegexFlavor.Java,
        "javascript" to RegexFlavor.JavaScript,
        "js" to RegexFlavor.JavaScript,
        "dotnet" to RegexFlavor.DotNet,
        ".net" to RegexFlavor.DotNet,
        "ruby" to RegexFlavor.Ruby,
        "rust" to RegexFlavor.Rust,
        "re2" to RegexFlavor.RE2,
    ).default(RegexFlavor.Java)

    override fun run() {
        val options = CompileOptions(flavor = flavor)
        val (result, diagnostics, tests) = Expr.parseAndCompile(input, options)

        if (result == null) {
            for (diag in diagnostics.filter { it.severity == Severity.Error }) {
                echo("error: ${diag.msg}", err = true)
            }
            throw ProgramResult(1)
        }

        if (tests.isEmpty()) {
            echo("No test blocks found.", err = true)
            throw ProgramResult(1)
        }

        val regex = Regex(result)
        var passed = 0
        var failed = 0

        for (test in tests) {
            for (case in test.cases) {
                when (case) {
                    is TestCase.Match -> {
                        val text = case.testCaseMatch.literal.content
                        if (regex.containsMatchIn(text)) {
                            echo("  PASS: match \"$text\"")
                            passed++
                        } else {
                            echo("  FAIL: match \"$text\" — no match found", err = true)
                            failed++
                        }
                    }
                    is TestCase.Reject -> {
                        val text = case.testCaseReject.literal.content
                        val matches = if (case.testCaseReject.asSubstring) {
                            regex.containsMatchIn(text)
                        } else {
                            regex.matches(text)
                        }
                        if (!matches) {
                            echo("  PASS: reject \"$text\"")
                            passed++
                        } else {
                            echo("  FAIL: reject \"$text\" — unexpected match", err = true)
                            failed++
                        }
                    }
                    is TestCase.MatchAll -> {
                        val text = case.testCaseMatchAll.literal.content
                        val allMatches = regex.findAll(text).toList()
                        echo("  PASS: match_all in \"$text\" (${allMatches.size} matches)")
                        passed++
                    }
                }
            }
        }

        echo("\n$passed passed, $failed failed")
        if (failed > 0) throw ProgramResult(1)
    }
}
