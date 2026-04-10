package ru.kode.pomskykt.analysis

import ru.kode.pomskykt.options.RegexFlavor
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RegexBenchmarkTest {
    @Test
    fun simpleLiteralBenchmarks() {
        val result = RegexBenchmark.benchmark(
            "'hello'",
            flavor = RegexFlavor.Java,
            iterations = 100,
            warmupIterations = 10,
        )
        assertNotNull(result)
        assertTrue(result.opsPerSecond > 0, "Expected positive ops/sec")
        assertTrue(result.avgTimeUs >= 0, "Expected non-negative avg time")
    }

    @Test
    fun digitPatternBenchmarks() {
        val result = RegexBenchmark.benchmark(
            "[digit]+",
            flavor = RegexFlavor.Java,
            iterations = 100,
            warmupIterations = 10,
        )
        assertNotNull(result)
        assertTrue(result.iterations == 100)
        assertTrue(result.testStringsUsed > 0)
    }

    @Test
    fun invalidPatternReturnsNull() {
        // Use a pattern that will cause a compile error
        val result = RegexBenchmark.benchmark(
            "atomic('test')",
            flavor = RegexFlavor.JavaScript, // atomic not supported in JS
            iterations = 100,
        )
        assertNull(result)
    }

    @Test
    fun benchmarkResultHasCorrectPattern() {
        val result = RegexBenchmark.benchmark(
            "'test'",
            flavor = RegexFlavor.Java,
            iterations = 50,
        )
        assertNotNull(result)
        assertTrue(result.pattern == "'test'")
        assertTrue(result.compiledRegex == "test")
        assertTrue(result.flavor == RegexFlavor.Java)
    }

    @Test
    fun totalTimeIsPositive() {
        val result = RegexBenchmark.benchmark(
            "[word]+ ':' [digit]+",
            flavor = RegexFlavor.Java,
            iterations = 100,
            warmupIterations = 10,
        )
        assertNotNull(result)
        assertTrue(result.totalTime.isPositive(), "Expected positive total time")
    }
}
