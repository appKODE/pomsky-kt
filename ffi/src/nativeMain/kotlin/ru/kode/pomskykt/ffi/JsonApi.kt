package ru.kode.pomskykt.ffi

import kotlinx.serialization.Serializable

/**
 * JSON-serializable compile options for [pomskyCompileJson].
 */
@Serializable
data class FfiCompileOptions(
    /** Regex flavor (0=PCRE, 1=Python, 2=Java, 3=JS, 4=.NET, 5=Ruby, 6=Rust, 7=RE2, 8=POSIX, 9=PythonRegex). */
    val flavor: Int = 0,
    /** Enable linter warnings. */
    val lintEnabled: Boolean = false,
    /** Enable auto-atomicization. */
    val autoAtomize: Boolean = false,
    /** Enable built-in pattern library. */
    val patternLibraryEnabled: Boolean = true,
)

/**
 * JSON-serializable compile result from [pomskyCompileJson].
 */
@Serializable
data class FfiCompileResult(
    val success: Boolean,
    val output: String? = null,
    val diagnostics: List<FfiDiagnostic> = emptyList(),
)

/**
 * JSON-serializable diagnostic.
 */
@Serializable
data class FfiDiagnostic(
    val severity: String,
    val message: String,
    val start: Int = 0,
    val end: Int = 0,
    val help: String? = null,
)
