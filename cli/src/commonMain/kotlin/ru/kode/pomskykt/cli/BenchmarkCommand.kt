package ru.kode.pomskykt.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.ProgramResult
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.choice
import com.github.ajalt.clikt.parameters.types.int
import ru.kode.pomskykt.analysis.RegexBenchmark
import ru.kode.pomskykt.options.RegexFlavor

/**
 * Benchmark regex matching performance for a Pomsky pattern.
 */
class BenchmarkCommand : CliktCommand(name = "benchmark") {
    private val input by argument(help = "Pomsky pattern to benchmark")

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

    private val iterations by option(
        "--iterations",
        help = "Number of benchmark iterations",
    ).int().default(10_000)

    private val warmup by option(
        "--warmup",
        help = "Number of warmup iterations",
    ).int().default(1_000)

    override fun run() {
        val result = RegexBenchmark.benchmark(
            pomskyPattern = input,
            flavor = flavor,
            iterations = iterations,
            warmupIterations = warmup,
        )

        if (result == null) {
            echo("error: Failed to compile pattern", err = true)
            throw ProgramResult(1)
        }

        echo("Pattern:     ${result.pattern}")
        echo("Regex:       ${result.compiledRegex}")
        echo("Flavor:      ${result.flavor}")
        echo("Iterations:  ${result.iterations}")
        echo("Test strings: ${result.testStringsUsed}")
        echo("Total time:  ${result.totalTime}")
        val avgStr = ((result.avgTimeUs * 100).toLong() / 100.0).toString()
        echo("Avg latency: $avgStr us")
        echo("Throughput:  ${result.opsPerSecond} ops/sec")
    }
}
