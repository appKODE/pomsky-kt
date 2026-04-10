# Pomsky-Kt

Kotlin Multiplatform port of [Pomsky](https://pomsky-lang.org/), a portable regex language that compiles to standard regular expressions — with significant extensions beyond the Rust original.

## Why Pomsky-Kt?

The original [Pomsky](https://github.com/pomsky-lang/pomsky) is written in Rust. This KMP port eliminates the WASM bridge and adds features the Rust version doesn't have:

- **Native library dependency** — use from Kotlin/JVM, macOS, Linux, or Windows without WASM overhead
- **C FFI shared library** — `libpomsky.dylib`/`.so`/`.dll` for any language with C FFI support
- **Zero runtime dependencies** — pure Kotlin string manipulation
- **Regex decompiler** — convert any regex back to readable Pomsky DSL
- **Regex explainer** — human-readable English descriptions of regex patterns
- **Kotlin DSL** — type-safe builder API for constructing patterns programmatically
- **Built-in pattern library** — `email`, `semver`, `uuid`, `jira_ticket`, `conventional_commit`, and more
- **Permutations** — `permute('a' 'b' 'c')` matches all orderings
- **ReDoS detection** — static analysis for catastrophic backtracking
- **Complexity scoring** — rate patterns 1-10 for performance risk
- **Linter** — detect common pattern mistakes
- **Auto-formatter** — consistent Pomsky source formatting
- **Test generator** — auto-generate matching/non-matching sample strings
- **Regex diff** — compare two patterns structurally
- **Capture group inference** — generate typed Kotlin data classes from named groups
- **Railroad diagrams** — ASCII visualization of pattern structure
- **Pattern fuzzer** — cross-flavor correctness testing
- **Performance benchmarker** — regex matching throughput measurement
- **10 regex flavors** — PCRE, Java, JavaScript, Python, .NET, Ruby, Rust, RE2, POSIX ERE, Python `regex`

## Modules

| Module | Artifact | Description |
|--------|----------|-------------|
| `syntax` | `ru.kode.pomsky-kt:syntax` | Lexer, parser, AST, Unicode data |
| `lib` | `ru.kode.pomsky-kt:lib` | Compiler, optimizer, linter, formatter, analysis tools |
| `decompiler` | `ru.kode.pomsky-kt:decompiler` | Regex to Pomsky DSL converter + explainer |
| `dsl` | `ru.kode.pomsky-kt:dsl` | Type-safe Kotlin builder API |
| `ffi` | _(native library)_ | C-compatible shared library (`.dylib`/`.so`/`.dll`) |
| `cli` | _(native binary)_ | Command-line tool with 8 subcommands |

## Usage

### Compile Pomsky to Regex

```kotlin
import ru.kode.pomskykt.Expr
import ru.kode.pomskykt.options.CompileOptions
import ru.kode.pomskykt.options.RegexFlavor

val (regex, diagnostics, tests) = Expr.parseAndCompile(
    "'hello' | 'world'",
    CompileOptions(flavor = RegexFlavor.Java),
)
println(regex) // hello|world
```

### Decompile Regex to Pomsky

```kotlin
import ru.kode.pomskykt.decompiler.Decompiler
import ru.kode.pomskykt.options.RegexFlavor

val result = Decompiler.decompile(
    "^(\\[PROJ-\\d+\\])+\\[(ios|android|core)\\] .+",
    RegexFlavor.Java,
)
println(result.pomsky)
// Start ('[PROJ-' [digit]+ ']')+ '[' ('ios' | 'android' | 'core') '] ' .+
```

### Explain Regex in English

```kotlin
val result = Decompiler.explain("^[A-Z]{2}\\d{4}$", RegexFlavor.Java)
println(result.explanation)
// start of string, exactly 2 of an uppercase letter (A-Z), followed by exactly 4 digits (0-9), end of string
```

### Built-in Pattern Library

9 pre-defined patterns available as variables without `let` bindings:

```pomsky
^ email $                          # RFC 5322 email
^ semver $                         # Semantic versioning (1.2.3-beta.1+build)
^ uuid $                           # UUID (8-4-4-4-12 hex)
^ jira_ticket ': ' .+ $           # JIRA-123: description
^ conventional_commit $            # feat(scope): message
^ date_iso8601 $                   # 2024-01-15
^ time_24h $                       # 14:30:00
^ hex_color $                      # #fff or #ffffff
^ ipv4 $                           # 0.0.0.0 - 255.255.255.255
```

```kotlin
val (regex, _, _) = Expr.parseAndCompile(
    "^ jira_ticket ': ' .+",
    CompileOptions(flavor = RegexFlavor.Java),
)
println(regex) // ^[A-Z]{2,10}-\d+: .+
```

### Permutations

Match all orderings of given elements:

```pomsky
permute('a' 'b' 'c')              # abc|acb|bac|bca|cab|cba
```

```kotlin
val (regex, _, _) = Expr.parseAndCompile("permute('x' 'y')", CompileOptions(flavor = RegexFlavor.Java))
println(regex) // xy|yx
```

### Kotlin DSL

```kotlin
import ru.kode.pomskykt.dsl.pomsky

val pattern = pomsky {
    start
    capture("prefix") {
        optional { oneOrMore { charRange('A', 'Z') }; literal("-") }
    }
    oneOrMore { digit }
    end
}
val regex = pattern.toKotlinRegex()
println("PROJ-123".matches(regex)) // true
```

### Complexity Scoring

Rate patterns 1-10 for ReDoS risk:

```kotlin
import ru.kode.pomskykt.analysis.ComplexityScorer
import ru.kode.pomskykt.decompiler.Decompiler

val ir = Decompiler.parse("(a+)+b", RegexFlavor.Java)
val report = ComplexityScorer.score(ir)
println("${report.score}/10 (${report.level})") // 8/10 (HIGH)
report.factors.forEach { println("  - ${it.description} (+${it.points})") }
```

### Test Generator

Auto-generate matching and non-matching sample strings:

```kotlin
import ru.kode.pomskykt.analysis.TestGenerator

val ir = Decompiler.parse("[A-Z]{2}\\d{4}", RegexFlavor.Java)
val tests = TestGenerator.generate(ir)
println("Match:    ${tests.matching}")   // [AA0000, MZ5999, ...]
println("No match: ${tests.nonMatching}") // [a0000, AA123, ...]
```

### Regex Diff

Compare two patterns structurally:

```kotlin
import ru.kode.pomskykt.analysis.RegexDiff

val a = Decompiler.parse("[a-z]+", RegexFlavor.Java)
val b = Decompiler.parse("[a-zA-Z]+", RegexFlavor.Java)
val diff = RegexDiff.diff(a, b)
diff.onlyInB.forEach { println("B adds: ${it.description}") }
// B adds: range A-Z
```

### Capture Group Type Inference

Generate typed Kotlin data classes from named captures:

```kotlin
import ru.kode.pomskykt.analysis.CaptureInference

val result = CaptureInference.inferGroups(
    ":year([digit]{4}) '-' :month([digit]{2}) '-' :day([digit]{2})",
)
println(result?.code)
// data class MatchGroups(
//     val year: String,   // group 1: "year"
//     val month: String,  // group 2: "month"
//     val day: String,    // group 3: "day"
// )
//
// fun Regex.extractMatchGroups(input: String): MatchGroups? { ... }
```

### Railroad Diagram

ASCII visualization of pattern structure:

```kotlin
import ru.kode.pomskykt.analysis.RailroadDiagram

val ir = Decompiler.parse("cat|dog|bird", RegexFlavor.Java)
println(RailroadDiagram.render(ir))
// ┌── "cat" ──┐
// ├── "dog" ──┤
// └── "bird" ─┘
```

### Pattern Fuzzer

Test patterns across flavors for correctness:

```kotlin
import ru.kode.pomskykt.analysis.PatternFuzzer
import ru.kode.pomskykt.options.RegexFlavor

val report = PatternFuzzer.fuzz(
    patterns = listOf("'hello'", "[digit]+", "^ [word]+ $"),
    flavors = listOf(RegexFlavor.Java, RegexFlavor.Pcre, RegexFlavor.JavaScript),
)
println("${report.totalPatterns} patterns, ${report.mismatches.size} mismatches")
```

### Performance Benchmarker

Measure regex matching throughput:

```kotlin
import ru.kode.pomskykt.analysis.RegexBenchmark

val result = RegexBenchmark.benchmark(
    "'hello' | 'world'",
    iterations = 10_000,
)
println("${result?.opsPerSecond} ops/sec, avg ${result?.avgTimeUs} us")
```

### Mode Modifiers

```pomsky
enable ignore_case;
'hello' [word]+

enable multiline;
^ 'start of line'
```

### Conditionals

```pomsky
if (>> [digit])
    'starts with digit'
else
    'starts with non-digit'
```

### Variables

```pomsky
let hex = [0-9 a-f A-F];
let color = '#' hex{6} | '#' hex{3};
color
```

### Linter

```kotlin
val (regex, diagnostics, _) = Expr.parseAndCompile(
    "'abc' | 'abcd'",
    CompileOptions(lintEnabled = true),
)
// diagnostics contains: "Alternative may be unreachable because a previous alternative always matches first"
```

### ReDoS Detection

```kotlin
val (regex, diagnostics, _) = Expr.parseAndCompile("([word]+ '.')+")
// diagnostics contains warning: "This expression may cause catastrophic backtracking (ReDoS)"
```

### Auto-Formatter

```kotlin
import ru.kode.pomskykt.format.PomskyFormatter
import ru.kode.pomskykt.format.FormatOptions

val formatted = PomskyFormatter.format(
    "let x='hello';x|'world'",
    FormatOptions(indentWidth = 2),
)
// let x = 'hello';
// x | 'world'
```

### Auto-Atomicization

```kotlin
val (regex, _, _) = Expr.parseAndCompile(
    "[digit]+ 'abc'",
    CompileOptions(flavor = RegexFlavor.Pcre, autoAtomize = true),
)
println(regex) // (?>[0-9]+)abc  — atomic group prevents backtracking
```

## C FFI (Native Library)

Use pomsky-kt from C, Python, Ruby, or any language with C FFI:

```c
#include "libpomsky_api.h"
#include <stdio.h>
#include <string.h>

int main() {
    // Compile Pomsky to regex (flavor 2 = Java)
    char* result = pomsky_compile("'hello' | 'world'", 2);

    if (strncmp(result, "ERROR:", 6) == 0) {
        fprintf(stderr, "%s\n", result);
    } else {
        printf("Regex: %s\n", result);  // hello|world
    }

    pomsky_free(result);
    return 0;
}
```

**Available C functions:**

| Function | Description |
|----------|-------------|
| `pomsky_compile(input, flavor)` | Compile Pomsky to regex string |
| `pomsky_compile_json(input, options_json)` | Compile with JSON options, returns JSON result |
| `pomsky_decompile(regex, flavor)` | Decompile regex to Pomsky DSL |
| `pomsky_explain(regex, flavor)` | Explain regex in human-readable English |
| `pomsky_version()` | Get version string |
| `pomsky_free(ptr)` | Free any returned string |

**Flavor codes:** 0=PCRE, 1=Python, 2=Java, 3=JavaScript, 4=.NET, 5=Ruby, 6=Rust, 7=RE2, 8=POSIX, 9=PythonRegex

## Supported Regex Flavors

| Flavor | Flag | Examples |
|--------|------|---------|
| PCRE | `pcre` (default) | PHP, Nginx, grep -P |
| Python | `python` | Python `re` module |
| Python regex | `python_regex` | Python `regex` module (Unicode properties, atomic groups) |
| Java | `java` | `java.util.regex.Pattern` |
| JavaScript | `javascript`, `js` | JS `RegExp` (ES2018+) |
| .NET | `dotnet`, `.net` | C#, F#, VB.NET |
| Ruby | `ruby` | Ruby `Regexp` |
| Rust | `rust` | Rust `regex` crate |
| RE2 | `re2` | Go, Google RE2 |
| POSIX ERE | `posix` | grep -E, awk, sed -E |

## Mode Modifiers

| Modifier | Regex Flag | Supported Flavors |
|----------|-----------|-------------------|
| `ignore_case` | `(?i:...)` | All |
| `multiline` | `(?m:...)` | All |
| `single_line` | `(?s:...)` | All |
| `extended` | `(?x:...)` | All |
| `reuse_groups` | `(?J:...)` | PCRE only |
| `ascii_line_breaks` | `(?d:...)` | PCRE, Java |

## Feature Compatibility

Shows which features are supported per regex flavor. **P** = polyfilled (expanded at compile time).

| Feature | PCRE | Java | JS | .NET | Python | PythonRegex | Ruby | Rust | RE2 | POSIX ERE |
|---------|------|------|----|------|--------|-------------|------|------|-----|-----------|
| Unicode properties (`\p{...}`) | Yes | Yes | Yes | Yes | No | Yes | Yes | Yes | Partial | No |
| Unicode scripts | Yes | Yes | Yes | No | No | Yes | Yes | Yes | No | No |
| Script extensions (`scx:`) | Yes | No | Yes | No | No | Yes | Yes | Yes | No | No |
| Unicode blocks (`blk:`) | Yes | Yes | No | Yes | No | Yes | Yes | Yes | No | No |
| `[word]` Unicode | **P** | Yes | **P** | **P** | Warn | **P** | Yes | Yes | Yes | No |
| `\p{LC}` (Cased_Letter) | Yes | Yes | Yes | **P** | No | Yes | Yes | Yes | No | No |
| Lookahead | Yes | Yes | Yes | Yes | Yes | Yes | Yes* | No | No | No |
| Lookbehind | Yes | Yes | Yes | Yes | Yes | Yes | Yes* | No | No | No |
| Atomic groups | Yes | Yes | No | Yes | No | Yes | No | No | No | No |
| Named groups | Yes | Yes | Yes | Yes | Yes | Yes | Yes | Yes | Yes | No |
| Backreferences | Yes | Yes | Yes | Yes | Yes | Yes | Yes** | No | No | No |
| Word boundaries | Yes | Yes | ASCII | Yes | Yes | Yes | Yes | Yes | ASCII | No |
| Char class intersection | Yes | Yes | Yes | No | No | No | Yes | Yes | No | No |
| Recursion | Yes | No | No | No | No | No | Yes | No | No | No |
| Conditionals*** | Yes | Yes | Yes | Yes | Yes | Yes | Yes* | No | No | No |
| Permutations | Yes | Yes | Yes | Yes | Yes | Yes | Yes | Yes | Yes | Yes |

\* Ruby: lookahead inside lookbehind is not allowed.
\** Ruby: mixing named and numbered group references is not allowed.
\*** Conditionals compile to lookahead assertions, so they work in all flavors that support lookahead.
ASCII = supported in ASCII mode only; Unicode-aware word boundaries are not supported.

## Optimizations

The compiler applies several optimization passes automatically:

- **Alternation factoring** — `'abc' | 'abd'` compiles to `ab[cd]` instead of `abc|abd`
- **Dead branch elimination** — removes duplicate and subsumed alternation branches
- **Assertion optimization** — removes redundant lookaheads, outlines boundary assertions
- **Auto-atomicization** (opt-in) — inserts `(?>...)` where provably safe to prevent backtracking

## CLI

```sh
pomsky compile "'hello' | 'world'"                      # Compile to regex
pomsky compile -f javascript ":name('test')"            # Specify flavor
pomsky compile --json "'a'+"                             # JSON output
pomsky migrate "^[a-z]+$"                                # Regex to Pomsky
pomsky explain "^[a-z]+$"                                # Human-readable explanation
pomsky complexity "(a+)+"                                 # Complexity score
pomsky test "test { match 'hi'; reject 'bye'; } 'hi'"   # Run test blocks
pomsky visualize "cat|dog"                                # ASCII railroad diagram
pomsky fuzz "'hello'" "[digit]+"                          # Cross-flavor fuzzing
pomsky benchmark "'hello' | 'world'" --iterations 10000  # Performance benchmark
```

## Building

```sh
./gradlew build                                         # All modules, all targets
./gradlew jvmTest macosArm64Test                        # Run tests
./gradlew :cli:linkReleaseExecutableMacosArm64          # Native CLI binary
./gradlew :ffi:linkReleaseSharedMacosArm64              # Native shared library
```

## Credits

Kotlin Multiplatform port of [Pomsky](https://github.com/pomsky-lang/pomsky) by [Ludwig Stecher](https://github.com/Aloso) and contributors, licensed under MIT/Apache-2.0.

## License

MIT
