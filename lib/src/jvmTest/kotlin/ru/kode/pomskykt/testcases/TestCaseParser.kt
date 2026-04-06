package ru.kode.pomskykt.testcases

import ru.kode.pomskykt.options.RegexFlavor

data class TestCase(
    val filePath: String,
    val flavor: String?,
    val expectError: Boolean,
    val ignore: Boolean,
    val source: String,
    val expectedOutput: String,
    val expectedErrorMsg: String?,
    val expectedHelpMsg: String?,
    val expectedSpan: Pair<Int, Int>?,
)

fun parseTestCaseFile(content: String, filePath: String): TestCase {
    val lines = content.replace("\r\n", "\n").replace("\r", "\n").split("\n")
    var i = 0
    var flavor: String? = null
    var expectError = false
    var ignore = false

    // Parse #! header lines (may have comma-separated directives)
    while (i < lines.size && lines[i].startsWith("#!")) {
        val header = lines[i].removePrefix("#!").trim()
        // Split on comma for multi-directive lines: "#! expect=error, flavor=JavaScript"
        for (directive in header.split(",")) {
            val eqIdx = directive.indexOf('=')
            if (eqIdx >= 0) {
                val key = directive.substring(0, eqIdx).trim()
                val value = directive.substring(eqIdx + 1).trim()
                when (key) {
                    "flavor" -> flavor = value
                    "expect" -> if (value == "error") expectError = true
                }
            } else {
                // Boolean flags without '=' (e.g. 'ignore')
                when (directive.trim()) {
                    "ignore" -> ignore = true
                }
            }
        }
        i++
    }

    // Do NOT skip blank lines or comment lines — they are part of the source string and affect
    // span byte positions (the Pomsky lexer handles # comments itself).

    // Find the separator line
    val sepIdx = (i until lines.size).firstOrNull { lines[it] == "-----" }
        ?: error("No separator '-----' found in $filePath")

    val source = lines.subList(i, sepIdx).joinToString("\n").trimEnd()
    val expectedBlock = lines.subList(sepIdx + 1, lines.size).joinToString("\n").trimEnd()

    var errorMsg: String? = null
    var helpMsg: String? = null
    var errorSpan: Pair<Int, Int>? = null
    if (expectError) {
        for (line in expectedBlock.lines()) {
            when {
                line.startsWith("ERROR: ") -> errorMsg = line.removePrefix("ERROR: ").trim()
                line.startsWith("HELP: ") -> helpMsg = line.removePrefix("HELP: ").trim()
                line.startsWith("SPAN: ") -> {
                    val span = line.removePrefix("SPAN: ").trim()
                    val parts = span.split("..")
                    if (parts.size == 2) {
                        errorSpan = parts[0].trim().toIntOrNull()?.let { s ->
                            parts[1].trim().toIntOrNull()?.let { e -> s to e }
                        }
                    }
                }
            }
        }
    }

    return TestCase(
        filePath = filePath,
        flavor = flavor,
        expectError = expectError,
        ignore = ignore,
        source = source,
        expectedOutput = expectedBlock,
        expectedErrorMsg = errorMsg,
        expectedHelpMsg = helpMsg,
        expectedSpan = errorSpan,
    )
}

/** Default flavor is Rust (matching the Rust test runner). */
fun normalizeFlavor(raw: String?): RegexFlavor? = when (raw?.trim()?.lowercase()) {
    null -> RegexFlavor.Rust
    "pcre" -> RegexFlavor.Pcre
    "javascript", "js" -> RegexFlavor.JavaScript
    "java" -> RegexFlavor.Java
    "python" -> RegexFlavor.Python
    "rust" -> RegexFlavor.Rust
    "dotnet", ".net" -> RegexFlavor.DotNet
    "ruby" -> RegexFlavor.Ruby
    "re2" -> RegexFlavor.RE2
    else -> null
}
