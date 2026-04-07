package ru.kode.pomskykt.dsl

import ru.kode.pomskykt.Expr
import ru.kode.pomskykt.options.CompileOptions
import ru.kode.pomskykt.options.RegexFlavor

/**
 * Type-safe Kotlin DSL for building Pomsky expressions.
 *
 * Example:
 * ```kotlin
 * val pattern = pomsky {
 *     start
 *     literal("hello")
 *     oneOrMore { word }
 *     end
 * }
 * pattern.toRegex() // ^hello\w+$
 * ```
 */
fun pomsky(block: PomskyBuilder.() -> Unit): PomskyPattern {
    val builder = PomskyBuilder()
    builder.block()
    return PomskyPattern(builder.build())
}

/**
 * A compiled Pomsky pattern that can be converted to a regex string.
 */
class PomskyPattern(val source: String) {

    /**
     * Compile this Pomsky pattern to a regex string for the given flavor.
     *
     * @throws IllegalStateException if compilation fails
     */
    fun toRegex(flavor: RegexFlavor = RegexFlavor.Java): String {
        val (result, diags, _) = Expr.parseAndCompile(source, CompileOptions(flavor = flavor))
        if (result == null) {
            val errors = diags.joinToString("; ") { it.msg }
            error("Failed to compile Pomsky: $errors")
        }
        return result
    }

    /**
     * Compile this Pomsky pattern and return a Kotlin [Regex] object.
     *
     * @throws IllegalStateException if compilation fails
     */
    fun toKotlinRegex(flavor: RegexFlavor = RegexFlavor.Java): Regex {
        return Regex(toRegex(flavor))
    }

    override fun toString(): String = source
}

/**
 * Builder for constructing Pomsky expressions using a type-safe DSL.
 */
class PomskyBuilder {
    private val parts = mutableListOf<String>()

    // --- Anchors ---

    /** Match the start of the string. */
    val start: PomskyBuilder get() = also { parts.add("Start") }

    /** Match the end of the string. */
    val end: PomskyBuilder get() = also { parts.add("End") }

    /** Match a word boundary. */
    val wordBoundary: PomskyBuilder get() = also { parts.add("%") }

    // --- Literals ---

    /** Match a literal string (automatically escaped). */
    fun literal(text: String) {
        parts.add("'${text.replace("'", "\\'")}'")
    }

    /** Insert a raw Pomsky expression. */
    fun raw(pomskyExpr: String) {
        parts.add(pomskyExpr)
    }

    // --- Character Classes ---

    /** Match a word character `[word]`. */
    val word: PomskyBuilder get() = also { parts.add("[word]") }

    /** Match a digit `[digit]`. */
    val digit: PomskyBuilder get() = also { parts.add("[digit]") }

    /** Match a whitespace character `[space]`. */
    val space: PomskyBuilder get() = also { parts.add("[space]") }

    /** Match any character (dot). */
    val any: PomskyBuilder get() = also { parts.add(".") }

    /** Match a character range, e.g. `charRange('a', 'z')` produces `['a'-'z']`. */
    fun charRange(from: Char, to: Char) {
        parts.add("['$from'-'$to']")
    }

    /** Match any of the given characters. */
    fun charClass(vararg chars: Char) {
        val items = chars.joinToString(" ") { "'$it'" }
        parts.add("[$items]")
    }

    // --- Quantifiers ---

    /** Match the inner pattern zero or one time. */
    fun optional(block: PomskyBuilder.() -> Unit) {
        val inner = buildInner(block)
        parts.add("($inner)?")
    }

    /** Match the inner pattern zero or more times. */
    fun zeroOrMore(block: PomskyBuilder.() -> Unit) {
        val inner = buildInner(block)
        parts.add("($inner)*")
    }

    /** Match the inner pattern one or more times. */
    fun oneOrMore(block: PomskyBuilder.() -> Unit) {
        val inner = buildInner(block)
        parts.add("($inner)+")
    }

    /** Match the inner pattern exactly [n] times. */
    fun repeat(n: Int, block: PomskyBuilder.() -> Unit) {
        val inner = buildInner(block)
        parts.add("($inner){$n}")
    }

    /** Match the inner pattern between [min] and [max] times. */
    fun repeat(min: Int, max: Int, block: PomskyBuilder.() -> Unit) {
        val inner = buildInner(block)
        parts.add("($inner){$min,$max}")
    }

    /** Match the inner pattern at least [min] times. */
    fun atLeast(min: Int, block: PomskyBuilder.() -> Unit) {
        val inner = buildInner(block)
        parts.add("($inner){$min,}")
    }

    // --- Groups ---

    /** Non-capturing group. */
    fun group(block: PomskyBuilder.() -> Unit) {
        val inner = buildInner(block)
        parts.add("($inner)")
    }

    /** Numbered capturing group. */
    fun capture(block: PomskyBuilder.() -> Unit) {
        val inner = buildInner(block)
        parts.add(":($inner)")
    }

    /** Named capturing group. */
    fun capture(name: String, block: PomskyBuilder.() -> Unit) {
        val inner = buildInner(block)
        parts.add(":$name($inner)")
    }

    // --- Alternation ---

    /** Match any one of the alternatives. */
    fun either(vararg alternatives: PomskyBuilder.() -> Unit) {
        val alts = alternatives.map { buildInner(it) }
        parts.add("(${alts.joinToString(" | ")})")
    }

    // --- Lookaround ---

    /** Positive lookahead. */
    fun lookahead(block: PomskyBuilder.() -> Unit) {
        val inner = buildInner(block)
        parts.add("(>> $inner)")
    }

    /** Negative lookahead. */
    fun negativeLookahead(block: PomskyBuilder.() -> Unit) {
        val inner = buildInner(block)
        parts.add("(!>> $inner)")
    }

    /** Positive lookbehind. */
    fun lookbehind(block: PomskyBuilder.() -> Unit) {
        val inner = buildInner(block)
        parts.add("(<< $inner)")
    }

    // --- Backreferences ---

    /** Backreference by name. */
    fun ref(name: String) {
        parts.add("::$name")
    }

    /** Backreference by index. */
    fun ref(index: Int) {
        parts.add("::$index")
    }

    // --- Internal ---

    private fun buildInner(block: PomskyBuilder.() -> Unit): String {
        val inner = PomskyBuilder()
        inner.block()
        return inner.build()
    }

    internal fun build(): String = parts.joinToString(" ")
}
