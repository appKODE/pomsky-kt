package ru.kode.pomskykt.options

/**
 * Target regex flavor for compilation.
 *
 * Ported from pomsky-lib/src/options.rs.
 */
enum class RegexFlavor {
    Pcre,
    Python,
    Java,
    JavaScript,
    DotNet,
    Ruby,
    Rust,
    RE2,
    PosixExtended,
    PythonRegex,
}
