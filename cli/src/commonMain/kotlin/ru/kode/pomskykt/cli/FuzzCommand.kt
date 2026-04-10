package ru.kode.pomskykt.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.ProgramResult
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.multiple
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.int
import ru.kode.pomskykt.analysis.PatternFuzzer
import ru.kode.pomskykt.options.RegexFlavor

/**
 * Fuzz-test Pomsky patterns across regex flavors.
 */
class FuzzCommand : CliktCommand(name = "fuzz") {
    private val patterns by argument(help = "Pomsky patterns to fuzz").multiple(required = true)

    private val maxTests by option(
        "--max-tests",
        help = "Maximum test strings per pattern",
    ).int().default(10)

    override fun run() {
        val flavors = listOf(
            RegexFlavor.Java,
            RegexFlavor.Pcre,
            RegexFlavor.JavaScript,
            RegexFlavor.Python,
            RegexFlavor.DotNet,
            RegexFlavor.Ruby,
        )

        val report = PatternFuzzer.fuzz(
            patterns = patterns,
            flavors = flavors,
            maxTestStrings = maxTests,
        )

        echo("Patterns: ${report.totalPatterns}")
        echo("Tests:    ${report.totalTests}")

        if (report.compileFailures.isNotEmpty()) {
            echo("\nCompile failures:")
            for ((flavor, count) in report.compileFailures) {
                echo("  $flavor: $count")
            }
        }

        if (report.mismatches.isEmpty()) {
            echo("\nNo mismatches found.")
        } else {
            echo("\nMismatches: ${report.mismatches.size}")
            for (result in report.mismatches.take(20)) {
                echo("  Pattern: ${result.pattern}")
                echo("  Input:   \"${result.testString}\"")
                for ((flavor, matched) in result.flavorResults) {
                    echo("    $flavor: ${if (matched) "MATCH" else "NO MATCH"}")
                }
                echo()
            }
            throw ProgramResult(1)
        }
    }
}
