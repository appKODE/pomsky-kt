package ru.kode.pomskykt.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.ProgramResult
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.optional
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.choice
import ru.kode.pomskykt.Expr
import ru.kode.pomskykt.diagnose.Severity
import ru.kode.pomskykt.options.CompileOptions
import ru.kode.pomskykt.options.RegexFlavor
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Compile a pomsky expression to a regular expression.
 *
 * This is the default (and main) command.
 */
class CompileCommand : CliktCommand(name = "compile") {
    private val input by argument(
        help = "Pomsky expression to compile",
    ).optional()

    private val flavor by option(
        "-f", "--flavor",
        help = "Target regex flavor",
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
    ).default(RegexFlavor.Pcre)

    private val json by option(
        "--json",
        help = "Output result as JSON",
    ).flag()

    private val noNewLine by option(
        "-n", "--no-new-line",
        help = "Don't print a newline after the output",
    ).flag()

    private val debug by option(
        "-d", "--debug",
        help = "Show debug information",
    ).flag()

    override fun run() {
        val expression = input
        if (expression == null) {
            echo("Error: No input provided. Pass a pomsky expression as an argument.", err = true)
            echo("Usage: pomsky compile <INPUT>", err = true)
            throw ProgramResult(2)
        }

        val options = CompileOptions(flavor = flavor)
        val (result, diagnostics, tests) = Expr.parseAndCompile(expression, options)

        val hasErrors = diagnostics.any { it.severity == Severity.Error }

        if (json) {
            outputJson(result, diagnostics, hasErrors)
        } else {
            outputHuman(result, diagnostics, hasErrors)
        }

        if (hasErrors) throw ProgramResult(1)
    }

    private fun outputJson(
        result: String?,
        diagnostics: List<ru.kode.pomskykt.diagnose.Diagnostic>,
        hasErrors: Boolean,
    ) {
        val jsonDiags = diagnostics.map { diag ->
            JsonDiagnostic(
                severity = if (diag.severity == Severity.Error) "error" else "warning",
                code = diag.code?.value,
                description = diag.msg,
            )
        }
        val output = JsonResult(
            success = !hasErrors,
            output = result,
            diagnostics = jsonDiags,
        )
        echo(jsonFormat.encodeToString(output))
    }

    private fun outputHuman(
        result: String?,
        diagnostics: List<ru.kode.pomskykt.diagnose.Diagnostic>,
        hasErrors: Boolean,
    ) {
        // Print diagnostics to stderr
        for (diag in diagnostics) {
            val prefix = if (diag.severity == Severity.Error) "error" else "warning"
            echo("$prefix: ${diag.msg}", err = true)
            if (diag.help != null) {
                echo("  help: ${diag.help}", err = true)
            }
        }

        // Print result to stdout
        if (result != null) {
            if (noNewLine) {
                print(result)
            } else {
                echo(result)
            }
        }
    }
}

@Serializable
private data class JsonResult(
    val version: Int = 1,
    val success: Boolean,
    val output: String? = null,
    val diagnostics: List<JsonDiagnostic> = emptyList(),
)

@Serializable
private data class JsonDiagnostic(
    val severity: String,
    val code: Int? = null,
    val description: String,
)

private val jsonFormat = Json {
    prettyPrint = false
    encodeDefaults = true
}
