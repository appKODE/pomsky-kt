package ru.kode.pomskykt.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.ProgramResult
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.choice
import ru.kode.pomskykt.analysis.ComplexityScorer
import ru.kode.pomskykt.decompiler.Decompiler
import ru.kode.pomskykt.options.RegexFlavor

/**
 * Analyze regex complexity and ReDoS risk.
 */
class ComplexityCommand : CliktCommand(name = "complexity") {
    private val input by argument(help = "Regex pattern to analyze")

    private val flavor by option(
        "-f", "--flavor",
        help = "Regex flavor",
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
        val ir = try {
            Decompiler.parse(input, flavor)
        } catch (e: IllegalArgumentException) {
            echo("error: ${e.message}", err = true)
            throw ProgramResult(1)
        }

        val report = ComplexityScorer.score(ir)
        echo("Score: ${report.score}/10 (${report.level})")
        if (report.factors.isNotEmpty()) {
            echo("Factors:")
            for (factor in report.factors) {
                echo("  - ${factor.description} (+${factor.points})")
            }
        }
    }
}
