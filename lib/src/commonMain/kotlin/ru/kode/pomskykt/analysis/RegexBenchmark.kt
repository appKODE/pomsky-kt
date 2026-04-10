package ru.kode.pomskykt.analysis

import ru.kode.pomskykt.Expr
import ru.kode.pomskykt.diagnose.Severity
import ru.kode.pomskykt.options.CompileOptions
import ru.kode.pomskykt.options.RegexFlavor
import kotlin.time.Duration
import kotlin.time.measureTime

data class BenchmarkResult(
    val pattern: String,
    val compiledRegex: String,
    val flavor: RegexFlavor,
    val iterations: Int,
    val totalTime: Duration,
    val avgTimeUs: Double,
    val opsPerSecond: Long,
    val testStringsUsed: Int,
)

object RegexBenchmark {
    fun benchmark(
        pomskyPattern: String,
        flavor: RegexFlavor = RegexFlavor.Java,
        iterations: Int = 10_000,
        warmupIterations: Int = 1_000,
    ): BenchmarkResult? {
        // 1. Compile Pomsky -> regex string
        val (result, diags, _) = Expr.parseAndCompile(
            pomskyPattern,
            CompileOptions(flavor = flavor),
        )
        if (result == null || diags.any { it.severity == Severity.Error }) return null

        // 2. Generate test strings
        val testStrings = generateSimpleTestStrings(pomskyPattern, result)

        // 3. Compile regex
        val regex = try {
            Regex(result)
        } catch (_: Exception) {
            return null
        }

        // 4. Warmup
        repeat(warmupIterations) {
            for (s in testStrings) {
                regex.containsMatchIn(s)
            }
        }

        // 5. Benchmark
        val totalOps = iterations * testStrings.size
        val elapsed = measureTime {
            repeat(iterations) {
                for (s in testStrings) {
                    regex.containsMatchIn(s)
                }
            }
        }

        // 6. Compute metrics
        val totalUs = elapsed.inWholeMicroseconds.toDouble()
        val avgUs = if (totalOps > 0) totalUs / totalOps else 0.0
        val opsPerSec = if (totalUs > 0) (totalOps * 1_000_000.0 / totalUs).toLong() else 0L

        return BenchmarkResult(
            pattern = pomskyPattern,
            compiledRegex = result,
            flavor = flavor,
            iterations = iterations,
            totalTime = elapsed,
            avgTimeUs = avgUs,
            opsPerSecond = opsPerSec,
            testStringsUsed = testStrings.size,
        )
    }

    private fun generateSimpleTestStrings(pomskyPattern: String, compiledRegex: String): List<String> {
        // Generate a mix of likely-matching and non-matching strings
        val strings = mutableListOf<String>()

        // Extract literal substrings from the compiled regex
        val literalChars = compiledRegex.filter { it.isLetterOrDigit() }
        if (literalChars.isNotEmpty()) {
            strings.add(literalChars)
        }

        // Common test strings
        strings.addAll(
            listOf(
                "",
                "a",
                "hello",
                "test123",
                "0123456789",
                "ABCDEFGHIJ",
                "foo bar baz",
                "user@example.com",
                "2024-01-15",
                "abc def ghi jkl mno pqr stu vwx yz",
            )
        )

        return strings
    }
}
