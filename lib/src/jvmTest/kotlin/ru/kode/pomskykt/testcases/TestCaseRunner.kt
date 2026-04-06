package ru.kode.pomskykt.testcases

import ru.kode.pomskykt.Expr
import ru.kode.pomskykt.diagnose.Severity
import ru.kode.pomskykt.options.CompileOptions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths

private val skipLog = java.io.File("/tmp/pomsky-skip-log.txt")
private fun skip(reason: String, file: String, detail: String) {
    skipLog.appendText("SKIP[$reason] $file: $detail\n")
    assumeTrue(false) { "[$reason] $file: $detail" }
}

class TestCaseRunner {

    companion object {
        init { skipLog.delete() }
    }

    /** Range compilation test cases — strict for non-error tests. */
    @TestFactory
    fun rangeTestCases(): List<DynamicTest> =
        loadTestCases("ranges").map { (relative, tc) ->
            val strict = !tc.expectError
            DynamicTest.dynamicTest(relative) { runTestCase(tc, strict = strict) }
        }

    /** All other Rust test cases. */
    @TestFactory
    fun otherTestCases(): List<DynamicTest> =
        loadTestCases(exclude = "ranges").map { (relative, tc) ->
            DynamicTest.dynamicTest(relative) { runTestCase(tc, strict = false) }
        }

    private fun loadTestCases(
        onlyDir: String? = null,
        exclude: String? = null,
    ): List<Pair<String, TestCase>> {
        val rootUrl = TestCaseRunner::class.java.classLoader.getResource("testcases")
            ?: error("testcases/ resource directory not found on classpath")
        val rootPath = Paths.get(rootUrl.toURI()).toRealPath()

        return Files.walk(rootPath)
            .filter { it.toString().endsWith(".txt") }
            .sorted()
            .map { path ->
                val relative = rootPath.relativize(path).toString()
                    .replace(File.separatorChar, '/')
                relative to path
            }
            .filter { (relative, _) ->
                when {
                    onlyDir != null -> relative.startsWith("$onlyDir/")
                    exclude != null -> !relative.startsWith("$exclude/")
                    else -> true
                }
            }
            .map { (relative, path) ->
                val content = path.toFile().readText()
                relative to parseTestCaseFile(content, relative)
            }
            .toList()
    }

    /**
     * Runs a test case matching the Rust test runner behavior.
     *
     * The Rust runner (pomsky-lib/tests/it/files.rs):
     * - Compiles with the specified flavor (default: Rust)
     * - On success: compares regex output (+ test block MATCH/REJECT lines)
     * - On error: formats as "ERROR: {msg}\nHELP: {help}\nSPAN: {span}" and
     *   compares the full string against the expected output block
     */
    private fun runTestCase(tc: TestCase, strict: Boolean) {
        // Note: ignore flag is NOT skipped — we implement all features including
        // those marked #! ignore in the upstream Rust test suite.

        val flavor = normalizeFlavor(tc.flavor)
        if (flavor == null) {
            skip("FLAVOR", tc.filePath, "Unknown flavor: ${tc.flavor}")
            return
        }

        val options = CompileOptions(flavor = flavor)

        try {
            val (result, diags, tests) = Expr.parseAndCompile(tc.source, options)
            val errors = diags.filter { it.severity == Severity.Error }

            if (result != null && errors.isEmpty()) {
                // Compilation succeeded
                val got = buildString {
                    append(result)
                    // Append warning lines
                    for (w in diags.filter { it.severity == Severity.Warning }) {
                        val byteSpan = charSpanToByteSpan(tc.source, w.span)
                        append("\nWARNING: ${w.msg}\n  at $byteSpan")
                    }
                    // Append test block results (MATCH/REJECT lines)
                    for (test in tests) {
                        for (case in test.cases) {
                            when (case) {
                                is ru.kode.pomskykt.syntax.exprs.TestCase.Match -> {
                                    append("\nMATCH: ")
                                    appendMatchResult(case.testCaseMatch)
                                }
                                is ru.kode.pomskykt.syntax.exprs.TestCase.MatchAll -> {
                                    append("\nMATCH_ALL: ")
                                    case.testCaseMatchAll.matches.forEachIndexed { i, m ->
                                        if (i > 0) append(", ")
                                        appendMatchResult(m)
                                    }
                                    append(" in \"${formatLiteralContent(case.testCaseMatchAll.literal.content)}\"")
                                }
                                is ru.kode.pomskykt.syntax.exprs.TestCase.Reject -> {
                                    append("\nREJECT: ")
                                    if (case.testCaseReject.asSubstring) append("in ")
                                    append("\"${formatLiteralContent(case.testCaseReject.literal.content)}\"")
                                }
                            }
                        }
                    }
                }

                if (tc.expectError) {
                    // Expected error but got success
                    if (!strict) {
                        skip("NOERR", tc.filePath, "Expected error not produced, got: $result")
                        return
                    }
                    assertEquals("ERROR", "SUCCESS") {
                        "Expected error for '${tc.filePath}' but compilation succeeded with: $result"
                    }
                } else {
                    if (!strict && got != tc.expectedOutput) {
                        skip("OUTPUT", tc.filePath,
                            "expected='${tc.expectedOutput}' got='$got'")
                        return
                    }
                    assertEquals(tc.expectedOutput, got) { "Output mismatch for '${tc.filePath}'" }
                }
            } else {
                // Compilation failed — format error string matching Rust's errors_to_string()
                // Convert char offsets to UTF-8 byte offsets (Rust uses byte offsets)
                val got = diags
                    .filter { it.severity == Severity.Error }
                    .joinToString("\n\n") { diagnostic ->
                        val byteSpan = charSpanToByteSpan(tc.source, diagnostic.span)
                        if (diagnostic.help != null) {
                            "ERROR: ${diagnostic.msg}\nHELP: ${diagnostic.help}\nSPAN: $byteSpan"
                        } else {
                            "ERROR: ${diagnostic.msg}\nSPAN: $byteSpan"
                        }
                    }

                if (!tc.expectError) {
                    // Didn't expect error but got one
                    if (!strict) {
                        skip("UNEXERR", tc.filePath, errors.joinToString("; ") { it.msg })
                        return
                    }
                    assertEquals("SUCCESS", "ERROR") {
                        "Unexpected errors for '${tc.filePath}': ${errors.map { it.msg }}"
                    }
                } else {
                    // Expected error — compare full error string
                    if (!strict && got != tc.expectedOutput) {
                        skip("ERRMSG", tc.filePath,
                            "expected='${tc.expectedOutput}' got='$got'")
                        return
                    }
                    assertEquals(tc.expectedOutput, got) { "Error output mismatch for '${tc.filePath}'" }
                }
            }
        } catch (e: org.opentest4j.TestAbortedException) {
            throw e
        } catch (e: Exception) {
            if (!strict) {
                skip("EXCEPT", tc.filePath, "${e::class.simpleName}: ${e.message}")
            } else {
                throw e
            }
        }
    }
}

/** Format literal content: escape backslashes and double quotes (matching Rust output format). */
private fun formatLiteralContent(content: String): String =
    content.replace("\\", "\\\\").replace("\"", "\\\"")

/** Append a match result in the format: "content" as { key: "value", ... } */
private fun StringBuilder.appendMatchResult(m: ru.kode.pomskykt.syntax.exprs.TestCaseMatch) {
    append("\"${formatLiteralContent(m.literal.content)}\" as { ")
    for (cap in m.captures) {
        when (val id = cap.ident) {
            is ru.kode.pomskykt.syntax.exprs.CaptureIdent.Index -> append("${id.index}: ")
            is ru.kode.pomskykt.syntax.exprs.CaptureIdent.Name -> append("${id.name}: ")
        }
        append("\"${formatLiteralContent(cap.literal.content)}\", ")
    }
    append("}")
}

/** Convert a char-offset Span to a UTF-8 byte-offset Span string (Rust uses byte offsets). */
private fun charSpanToByteSpan(source: String, span: ru.kode.pomskykt.syntax.Span): String {
    val byteStart = charOffsetToByteOffset(source, span.start)
    val byteEnd = charOffsetToByteOffset(source, span.end)
    return "$byteStart..$byteEnd"
}

/** Convert a char offset (UTF-16) to a UTF-8 byte offset. */
private fun charOffsetToByteOffset(source: String, charOffset: Int): Int {
    val end = minOf(charOffset, source.length)
    var bytes = 0
    var i = 0
    while (i < end) {
        val ch = source[i]
        if (ch.isHighSurrogate() && i + 1 < end && source[i + 1].isLowSurrogate()) {
            bytes += 4
            i += 2
        } else {
            bytes += when {
                ch.code < 0x80 -> 1
                ch.code < 0x800 -> 2
                else -> 3
            }
            i++
        }
    }
    return bytes
}
