@file:OptIn(
    kotlinx.cinterop.ExperimentalForeignApi::class,
    kotlin.experimental.ExperimentalNativeApi::class,
)

package ru.kode.pomskykt.ffi

import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.nativeHeap
import kotlinx.cinterop.set
import kotlinx.cinterop.toKString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import ru.kode.pomskykt.Expr
import ru.kode.pomskykt.decompiler.Decompiler
import ru.kode.pomskykt.diagnose.Severity
import ru.kode.pomskykt.options.CompileOptions
import ru.kode.pomskykt.options.RegexFlavor

private val jsonFormat = Json {
    prettyPrint = false
    encodeDefaults = true
    ignoreUnknownKeys = true
}

/**
 * Allocate a null-terminated C string on the native heap.
 * Caller must free with [pomskyFree].
 */
private fun allocCString(value: String): CPointer<ByteVar> {
    val bytes = value.encodeToByteArray()
    val ptr = nativeHeap.allocArray<ByteVar>(bytes.size + 1)
    for (i in bytes.indices) {
        ptr[i] = bytes[i]
    }
    ptr[bytes.size] = 0
    return ptr
}

/**
 * Compile a Pomsky expression to a regex string.
 *
 * @param input Pomsky source as a null-terminated C string.
 * @param flavor Target regex flavor (0=PCRE, 1=Python, 2=Java, 3=JS, 4=.NET, 5=Ruby, 6=Rust, 7=RE2, 8=POSIX, 9=PythonRegex).
 * @return Compiled regex string, or "ERROR: ..." on failure. Caller must call [pomsky_free].
 */
@CName("pomsky_compile")
fun pomskyCompile(input: CPointer<ByteVar>, flavor: Int): CPointer<ByteVar> {
    val inputStr = input.toKString()
    val options = CompileOptions(flavor = flavorFromInt(flavor))
    val (result, diags, _) = Expr.parseAndCompile(inputStr, options)

    val output = if (result != null && diags.none { it.severity == Severity.Error }) {
        result
    } else {
        val errors = diags.filter { it.severity == Severity.Error }
        "ERROR: ${errors.firstOrNull()?.msg ?: "Unknown error"}"
    }

    return allocCString(output)
}

/**
 * Compile a Pomsky expression with JSON options, returning a JSON result.
 *
 * @param input Pomsky source as a null-terminated C string.
 * @param optionsJson JSON string with compile options.
 * @return JSON string with compile result. Caller must call [pomsky_free].
 */
@CName("pomsky_compile_json")
fun pomskyCompileJson(input: CPointer<ByteVar>, optionsJson: CPointer<ByteVar>): CPointer<ByteVar> {
    val inputStr = input.toKString()
    val optionsStr = optionsJson.toKString()

    val ffiOptions = try {
        jsonFormat.decodeFromString<FfiCompileOptions>(optionsStr)
    } catch (e: Exception) {
        val errorResult = FfiCompileResult(
            success = false,
            diagnostics = listOf(FfiDiagnostic(
                severity = "error",
                message = "Invalid options JSON: ${e.message}",
            )),
        )
        return allocCString(jsonFormat.encodeToString(errorResult))
    }

    val options = CompileOptions(
        flavor = flavorFromInt(ffiOptions.flavor),
        lintEnabled = ffiOptions.lintEnabled,
        autoAtomize = ffiOptions.autoAtomize,
        patternLibraryEnabled = ffiOptions.patternLibraryEnabled,
    )

    val (result, diags, _) = Expr.parseAndCompile(inputStr, options)

    val ffiDiags = diags.map { diag ->
        FfiDiagnostic(
            severity = if (diag.severity == Severity.Error) "error" else "warning",
            message = diag.msg,
            start = diag.span.start,
            end = diag.span.end,
            help = diag.help,
        )
    }

    val ffiResult = FfiCompileResult(
        success = result != null && diags.none { it.severity == Severity.Error },
        output = result,
        diagnostics = ffiDiags,
    )

    return allocCString(jsonFormat.encodeToString(ffiResult))
}

/**
 * Decompile a regex string to Pomsky DSL.
 *
 * @param regex Regex string as a null-terminated C string.
 * @param flavor Source regex flavor.
 * @return Pomsky DSL string, or "ERROR: ..." on failure. Caller must call [pomsky_free].
 */
@CName("pomsky_decompile")
fun pomskyDecompile(regex: CPointer<ByteVar>, flavor: Int): CPointer<ByteVar> {
    val regexStr = regex.toKString()
    val result = Decompiler.decompile(regexStr, flavorFromInt(flavor))

    val output = result.pomsky ?: "ERROR: ${result.error ?: "Unknown error"}"
    return allocCString(output)
}

/**
 * Explain a regex pattern in human-readable English.
 *
 * @param regex Regex string as a null-terminated C string.
 * @param flavor Regex flavor.
 * @return Human-readable explanation, or "ERROR: ..." on failure. Caller must call [pomsky_free].
 */
@CName("pomsky_explain")
fun pomskyExplain(regex: CPointer<ByteVar>, flavor: Int): CPointer<ByteVar> {
    val regexStr = regex.toKString()
    val result = Decompiler.explain(regexStr, flavorFromInt(flavor))

    val output = result.explanation ?: "ERROR: ${result.error ?: "Unknown error"}"
    return allocCString(output)
}

/**
 * Free a string previously returned by any pomsky_* function.
 *
 * @param ptr Pointer returned by a pomsky function. Must not be used after this call.
 */
@CName("pomsky_free")
fun pomskyFree(ptr: CPointer<ByteVar>) {
    nativeHeap.free(ptr.rawValue)
}

/**
 * Return the pomsky-kt version string.
 *
 * @return Version string. Caller must call [pomsky_free].
 */
@CName("pomsky_version")
fun pomskyVersion(): CPointer<ByteVar> {
    return allocCString("0.15.0")
}

internal fun flavorFromInt(value: Int): RegexFlavor = when (value) {
    0 -> RegexFlavor.Pcre
    1 -> RegexFlavor.Python
    2 -> RegexFlavor.Java
    3 -> RegexFlavor.JavaScript
    4 -> RegexFlavor.DotNet
    5 -> RegexFlavor.Ruby
    6 -> RegexFlavor.Rust
    7 -> RegexFlavor.RE2
    8 -> RegexFlavor.PosixExtended
    9 -> RegexFlavor.PythonRegex
    else -> RegexFlavor.Pcre
}
