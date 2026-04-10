package ru.kode.pomskykt.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.ProgramResult
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.choice
import ru.kode.pomskykt.decompiler.Decompiler
import ru.kode.pomskykt.options.RegexFlavor

/**
 * Explain a regex pattern in human-readable English.
 */
class ExplainCommand : CliktCommand(name = "explain") {
    private val input by argument(help = "Regex pattern to explain")

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
        val result = Decompiler.explain(input, flavor)
        if (result.explanation != null) {
            echo(result.explanation)
        } else {
            echo("error: ${result.error}", err = true)
            throw ProgramResult(1)
        }
    }
}
