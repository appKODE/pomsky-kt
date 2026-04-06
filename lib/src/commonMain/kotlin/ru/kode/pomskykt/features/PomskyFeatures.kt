package ru.kode.pomskykt.features

import ru.kode.pomskykt.diagnose.CompileError
import ru.kode.pomskykt.diagnose.CompileErrorKind
import ru.kode.pomskykt.syntax.Span

/**
 * Bitfield of allowed pomsky features.
 *
 * Ported from pomsky-lib/src/features.rs.
 */
class PomskyFeatures private constructor(private var bits: Int) {

    fun supports(feature: Int): Boolean = (bits and feature) != 0

    fun require(feature: Int, span: Span): CompileError? {
        return if (!supports(feature)) {
            CompileError(
                CompileErrorKind.UnsupportedPomskySyntax(featureToUnsupported(feature)),
                span,
            )
        } else null
    }

    // Builder methods
    fun grapheme(support: Boolean) = apply { setBit(GRAPHEME, support) }
    fun numberedGroups(support: Boolean) = apply { setBit(NUMBERED_GROUPS, support) }
    fun namedGroups(support: Boolean) = apply { setBit(NAMED_GROUPS, support) }
    fun atomicGroups(support: Boolean) = apply { setBit(ATOMIC_GROUPS, support) }
    fun references(support: Boolean) = apply { setBit(REFERENCES, support) }
    fun lazyMode(support: Boolean) = apply { setBit(LAZY_MODE, support) }
    fun asciiMode(support: Boolean) = apply { setBit(ASCII_MODE, support) }
    fun ranges(support: Boolean) = apply { setBit(RANGES, support) }
    fun variables(support: Boolean) = apply { setBit(VARIABLES, support) }
    fun lookahead(support: Boolean) = apply { setBit(LOOKAHEAD, support) }
    fun lookbehind(support: Boolean) = apply { setBit(LOOKBEHIND, support) }
    fun boundaries(support: Boolean) = apply { setBit(BOUNDARIES, support) }
    fun regexes(support: Boolean) = apply { setBit(REGEXES, support) }
    fun dot(support: Boolean) = apply { setBit(DOT, support) }
    fun recursion(support: Boolean) = apply { setBit(RECURSION, support) }
    fun intersection(support: Boolean) = apply { setBit(INTERSECTION, support) }

    private fun setBit(bit: Int, support: Boolean) {
        bits = if (support) bits or bit else bits and bit.inv()
    }

    companion object {
        const val GRAPHEME = 1 shl 0
        const val NUMBERED_GROUPS = 1 shl 1
        const val NAMED_GROUPS = 1 shl 2
        const val REFERENCES = 1 shl 3
        const val LAZY_MODE = 1 shl 4
        const val ASCII_MODE = 1 shl 5
        const val RANGES = 1 shl 6
        const val VARIABLES = 1 shl 7
        const val LOOKAHEAD = 1 shl 8
        const val LOOKBEHIND = 1 shl 9
        const val BOUNDARIES = 1 shl 10
        const val ATOMIC_GROUPS = 1 shl 11
        const val REGEXES = 1 shl 12
        const val DOT = 1 shl 13
        const val RECURSION = 1 shl 14
        const val INTERSECTION = 1 shl 15

        private const val ALL_BITS = (1 shl 16) - 1

        /** All features enabled. */
        fun default() = PomskyFeatures(ALL_BITS)

        /** No features enabled. */
        fun none() = PomskyFeatures(0)

        private fun featureToUnsupported(feature: Int): UnsupportedError = when (feature) {
            GRAPHEME -> UnsupportedError.Grapheme
            NUMBERED_GROUPS -> UnsupportedError.NumberedGroups
            NAMED_GROUPS -> UnsupportedError.NamedGroups
            ATOMIC_GROUPS -> UnsupportedError.AtomicGroups
            REFERENCES -> UnsupportedError.References
            LAZY_MODE -> UnsupportedError.LazyMode
            ASCII_MODE -> UnsupportedError.AsciiMode
            RANGES -> UnsupportedError.Ranges
            VARIABLES -> UnsupportedError.Variables
            LOOKAHEAD -> UnsupportedError.Lookahead
            LOOKBEHIND -> UnsupportedError.Lookbehind
            BOUNDARIES -> UnsupportedError.Boundaries
            REGEXES -> UnsupportedError.Regexes
            DOT -> UnsupportedError.Dot
            RECURSION -> UnsupportedError.Recursion
            INTERSECTION -> UnsupportedError.Intersection
            else -> UnsupportedError.Grapheme // fallback
        }
    }
}

/** A pomsky feature that the user has disabled. */
enum class UnsupportedError {
    Grapheme, NumberedGroups, NamedGroups, AtomicGroups,
    References, LazyMode, AsciiMode, Ranges, Variables,
    Lookahead, Lookbehind, Boundaries, Regexes, Dot,
    Recursion, Intersection,
}

/** Convert an [UnsupportedError] to a human-readable message. Ported from Rust Display. */
fun UnsupportedError.toMessage(): String = when (this) {
    UnsupportedError.Grapheme -> "Grapheme isn't supported"
    UnsupportedError.NumberedGroups -> "Numbered capturing groups aren't supported"
    UnsupportedError.NamedGroups -> "Named capturing groups aren't supported"
    UnsupportedError.AtomicGroups -> "Atomic groups aren't supported"
    UnsupportedError.References -> "References aren't supported"
    UnsupportedError.LazyMode -> "Lazy mode isn't supported"
    UnsupportedError.AsciiMode -> "Disabling Unicode isn't supported"
    UnsupportedError.Ranges -> "Ranges aren't supported"
    UnsupportedError.Variables -> "Variables aren't supported"
    UnsupportedError.Lookahead -> "Lookahead isn't supported"
    UnsupportedError.Lookbehind -> "Lookbehind isn't supported"
    UnsupportedError.Boundaries -> "Word boundaries aren't supported"
    UnsupportedError.Regexes -> "Unescaped regexes aren't supported"
    UnsupportedError.Dot -> "The dot isn't supported"
    UnsupportedError.Recursion -> "Recursion isn't supported"
    UnsupportedError.Intersection -> "Intersection isn't supported"
}
