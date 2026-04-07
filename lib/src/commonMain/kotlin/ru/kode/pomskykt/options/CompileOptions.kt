package ru.kode.pomskykt.options

import ru.kode.pomskykt.features.PomskyFeatures

/**
 * Options for compiling a pomsky expression.
 *
 * Ported from pomsky-lib/src/options.rs.
 */
data class CompileOptions(
    val flavor: RegexFlavor = RegexFlavor.Pcre,
    val maxRangeSize: Int = 6,
    val allowedFeatures: PomskyFeatures = PomskyFeatures.default(),
    /** When true, run the linter pass to detect common mistakes and anti-patterns. */
    val lintEnabled: Boolean = false,
)
