# Pomsky-Kt

Kotlin Multiplatform port of [Pomsky](https://pomsky-lang.org/), a portable regex language that compiles to standard regular expressions.

## Why a KMP Port?

The original [Pomsky](https://github.com/pomsky-lang/pomsky) is written in Rust. It provides a WASM binary for non-Rust consumers, which works but requires a WASM runtime (Chicory on JVM, wasm3 on native). This KMP port eliminates that bridge:

- **Native library dependency** -- use Pomsky directly from Kotlin/JVM, macOS, Linux, or Windows without WASM overhead
- **Zero runtime dependencies** for the core compiler -- pure Kotlin string manipulation
- **Same API on all platforms** -- Kotlin Multiplatform produces artifacts for JVM, macOS ARM64/x64, Linux x64, and Windows MinGW x64
- **CLI binary** -- native executables for all supported platforms

This port targets feature parity with the Rust implementation.

## Modules

| Module | Artifact | Description |
|--------|----------|-------------|
| `syntax` | `ru.kode.pomsky-kt:syntax` | Lexer, parser, AST, Unicode data |
| `lib` | `ru.kode.pomsky-kt:lib` | Compiler: AST to regex string for 8 flavors |
| `cli` | _(native binary)_ | Command-line tool |

## Usage

### Library

```kotlin
// Add dependency
// implementation("ru.kode.pomsky-kt:lib:0.13.0")

import ru.kode.pomskykt.Expr
import ru.kode.pomskykt.options.CompileOptions
import ru.kode.pomskykt.options.RegexFlavor

// One-step parse and compile
val (regex, diagnostics, tests) = Expr.parseAndCompile(
    "'hello' | 'world'",
    CompileOptions(flavor = RegexFlavor.Java),
)
println(regex) // hello|world

// Named capturing group
val (regex2, _, _) = Expr.parseAndCompile(":name([w]+)")
println(regex2) // (?P<name>\w+)

// Variables
val (regex3, _, _) = Expr.parseAndCompile("let hex = [0-9 a-f A-F]; '#' hex{6}")
println(regex3) // #[0-9a-fA-F]{6}
```

### CLI

```sh
# Compile a pomsky expression
pomsky "'hello' | 'world'"
# Output: hello|world

# Specify target flavor
pomsky -f javascript ":name('test')"
# Output: (?<name>test)

# JSON output
pomsky --json "'a'+"
# Output: {"version":1,"success":true,"output":"a+","diagnostics":[]}

# No trailing newline
pomsky -n "'test'"
```

### Supported Regex Flavors

| Flavor | Flag | Examples |
|--------|------|---------|
| PCRE | `pcre` (default) | PHP, Nginx, grep -P |
| Python | `python` | Python `re` module |
| Java | `java` | `java.util.regex.Pattern` |
| JavaScript | `javascript`, `js` | JS `RegExp` |
| .NET | `dotnet`, `.net` | C#, F#, VB.NET |
| Ruby | `ruby` | Ruby `Regexp` |
| Rust | `rust` | Rust `regex` crate |
| RE2 | `re2` | Go, Google RE2 |

## Building

```sh
./gradlew build                                         # All modules, all targets
./gradlew jvmTest macosArm64Test                        # Run tests
./gradlew :cli:linkReleaseExecutableMacosArm64          # Native CLI binary
```

The CLI binary is output to `cli/build/bin/macosArm64/releaseExecutable/pomsky.kexe`.

## Credits

This project is a Kotlin Multiplatform port of [Pomsky](https://github.com/pomsky-lang/pomsky) by [Ludwig Stecher](https://github.com/Aloso) and contributors, licensed under MIT/Apache-2.0.

The porting effort preserves the original architecture: lexer, recursive-descent parser, AST, compilation to regex IR, flavor-specific codegen, and optimization. Unicode data files are sourced from the original repository.

## License

MIT -- same as the original Pomsky project.
