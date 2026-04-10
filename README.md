# Pomsky-Kt

Kotlin Multiplatform port of [Pomsky](https://pomsky-lang.org/), a portable regex language that compiles to standard regular expressions — with significant extensions beyond the Rust original.

## Why Pomsky-Kt?

The original [Pomsky](https://github.com/pomsky-lang/pomsky) is written in Rust. This KMP port eliminates the WASM bridge and adds features the Rust version doesn't have:

- **Native library dependency** — use from Kotlin/JVM, macOS, Linux, or Windows without WASM overhead
- **Zero runtime dependencies** — pure Kotlin string manipulation
- **Regex decompiler** — convert any regex back to readable Pomsky DSL
- **Kotlin DSL** — type-safe builder API for constructing patterns programmatically
- **ReDoS detection** — static analysis for catastrophic backtracking
- **Linter** — detect common pattern mistakes
- **Auto-formatter** — consistent Pomsky source formatting
- **10 regex flavors** — PCRE, Java, JavaScript, Python, .NET, Ruby, Rust, RE2, POSIX ERE, Python `regex`

## Modules

| Module | Artifact | Description |
|--------|----------|-------------|
| `syntax` | `ru.kode.pomsky-kt:syntax` | Lexer, parser, AST, Unicode data |
| `lib` | `ru.kode.pomsky-kt:lib` | Compiler, optimizer, linter, formatter |
| `decompiler` | `ru.kode.pomsky-kt:decompiler` | Regex string to Pomsky DSL converter |
| `dsl` | `ru.kode.pomsky-kt:dsl` | Type-safe Kotlin builder API |
| `cli` | _(native binary)_ | Command-line tool |

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
val regex = pattern.toKotlinRegex() // Kotlin Regex object ready to use
println("PROJ-123".matches(regex)) // true
```

### Mode Modifiers

```pomsky
enable ignore_case;
'hello' [word]+

enable multiline;
^ 'start of line'

enable single_line;
.+                    # dot matches newlines too
```

### Conditionals

```pomsky
if (>> [digit]) 
    'starts with digit'
else
    'starts with non-digit'
```

### Variables & Named Patterns

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
| Backreferences | Yes | Yes | Yes | Yes | Yes | Yes | Yes | No | No | No |
| Word boundaries (Unicode) | Yes | Yes | Yes | Yes | Yes | Yes | Yes | No | No | No |
| Char class intersection | Yes | Yes | Yes | Yes | Yes | Yes | Yes | Yes | Yes | Yes |
| Recursion | Yes | No | No | No | No | No | Yes | No | No | No |
| Conditionals | Yes | No | No | No | No | No | No | No | No | No |

\* Ruby: lookahead inside lookbehind is not allowed.

## Optimizations

The compiler applies several optimization passes automatically:

- **Alternation factoring** — `'abc' | 'abd'` compiles to `ab[cd]` instead of `abc|abd`
- **Dead branch elimination** — removes duplicate and subsumed alternation branches
- **Assertion optimization** — removes redundant lookaheads, outlines boundary assertions
- **Auto-atomicization** (opt-in) — inserts `(?>...)` where provably safe to prevent backtracking

## CLI

```sh
pomsky "'hello' | 'world'"                    # Compile to regex
pomsky -f javascript ":name('test')"          # Specify flavor
pomsky --json "'a'+"                          # JSON output
```

## Building

```sh
./gradlew build                                         # All modules, all targets
./gradlew jvmTest macosArm64Test                        # Run tests
./gradlew :cli:linkReleaseExecutableMacosArm64          # Native CLI binary
```

## Credits

Kotlin Multiplatform port of [Pomsky](https://github.com/pomsky-lang/pomsky) by [Ludwig Stecher](https://github.com/Aloso) and contributors, licensed under MIT/Apache-2.0.

## License

MIT
