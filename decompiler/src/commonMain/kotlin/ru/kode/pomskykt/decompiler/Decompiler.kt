package ru.kode.pomskykt.decompiler

import ru.kode.pomskykt.options.RegexFlavor
import ru.kode.pomskykt.regex.Regex

/**
 * Decompiles regex strings to Pomsky DSL source.
 *
 * Given a raw regex string and a target flavor, produces idiomatic Pomsky DSL
 * that compiles to an equivalent regex.
 *
 * Example:
 * ```
 * val result = Decompiler.decompile("^[a-z]+\\d{2,4}$", RegexFlavor.Java)
 * // result.pomsky = "Start ['a'-'z']+ [digit]{2,4} End"
 * ```
 */
object Decompiler {
    /**
     * Decompile a regex string to Pomsky DSL.
     *
     * @param regex The regex string to decompile.
     * @param flavor The regex flavor (affects parsing of escapes, named groups, etc.).
     * @return [DecompileResult] with Pomsky source, or error message on failure.
     */
    fun decompile(
        regex: String,
        flavor: RegexFlavor = RegexFlavor.Java,
    ): DecompileResult {
        if (regex.isEmpty()) return DecompileResult(pomsky = "", error = null)
        return try {
            val ir = parse(regex, flavor)
            val pomsky = PomskyEmitter().emit(ir)
            DecompileResult(pomsky = pomsky, error = null)
        } catch (e: IllegalArgumentException) {
            DecompileResult(pomsky = null, error = e.message)
        } catch (e: NumberFormatException) {
            DecompileResult(pomsky = null, error = "Invalid escape sequence in regex: ${e.message}")
        }
    }

    /**
     * Parse a regex string into the [Regex] IR without emitting Pomsky.
     *
     * Useful for analysis, transformation, or custom emission.
     *
     * @param regex The regex string to parse.
     * @param flavor The regex flavor.
     * @return The parsed [Regex] IR tree.
     */
    fun parse(
        regex: String,
        flavor: RegexFlavor = RegexFlavor.Java,
    ): Regex {
        val tokens = RegexLexer(regex, flavor).tokenize()
        return RegexParser(tokens).parse()
    }
}

/**
 * Result of decompiling a regex string to Pomsky DSL.
 */
data class DecompileResult(
    val pomsky: String?,
    val error: String?,
)
