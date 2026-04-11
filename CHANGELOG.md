# Changelog

All notable changes to pomsky-kt will be documented in this file.

## [0.17.0] - 2026-04-11

### Improved

- **Regex explanation readability** — `Decompiler.explain()` now generates friendlier, less technical descriptions:
  - Groups: content-first phrasing — `exactly 4 digits (0-9) (saved as "year")` instead of `captured as "year": exactly 4 digits (0-9)`
  - Lookaround: `if followed by ...` / `if not preceded by ...` instead of `followed by ... (without consuming)`
  - Repetition: natural plurals — `one or more digits (0-9)`, `any number of word characters` instead of `one or more of a digit (0-9)`, `zero or more of a word character`
  - References: `the same "name" text again` instead of `same text as captured in "name"`
  - Word characters: hints added — `a word character (letter, digit, or underscore)`, `a whitespace character (space, tab, newline)`
  - Boundaries: hints added — `a word boundary (start or end of a word)`
  - Special nodes: `a single visible character (grapheme)` instead of `a grapheme cluster`

## [0.16.0] - 2026-04-10

### Fixed

- **Decompiler round-trip: 40 → 0 failures** — All 182 round-trip tests now pass with zero skips. Regex → Pomsky → Regex produces correct output for all test cases across all 10 flavors.
  - **Char class item syntax** — Shorthands (`word`, `digit`, `space`) and properties now emit as bare identifiers inside character classes instead of invalid nested bracket forms like `[![word] 'a'-'f']`. (~20 tests fixed)
  - **Property name syntax** — Categories use abbreviations (`Nd` not `DecimalNumber`), blocks get `blk:` prefix (`blk:Basic_Latin`), script extensions use `:` separator (`scx:Greek` not `scx=Greek`). (8 tests fixed)
  - **Intersection syntax** — Character set intersections use top-level `[a] & [b]` operator instead of nested `[[a] & [b]]` brackets. (5 tests fixed)
  - **Control characters** — Control chars (`\n`, `\r`, `\t`, `\f`) in character classes now emit as `U+0A`, `U+0D`, `U+09`, `U+0C` code points instead of raw bytes in strings.
  - **`[\s\S]` → `Codepoint`** — Complementary shorthand pairs (e.g., `[\s\S]`, `[\w\W]`) are detected and emitted as `Codepoint` (any character).
  - **.NET mixed group references** — When both named and unnamed capturing groups exist in .NET flavor, unnamed groups receive synthetic names (`_g1`, `_g3`) to avoid Pomsky's numeric reference restriction. (2 tests fixed)
  - **Unclosed char classes** — Incomplete regex like `[` now emits as `regex '['` pass-through instead of producing empty `[]` that Pomsky rejects. (1 test fixed)

## [0.15.0] - 2026-04-10

### Added

- **Regex Explanation Engine** — `Decompiler.explain(regex, flavor)` generates human-readable English descriptions from regex patterns. E.g., `^[A-Z]{2}\d{4}$` → "start of string, exactly 2 uppercase letters (A-Z), followed by exactly 4 digits (0-9), end of string".
- **Built-in Pattern Library** — 9 named patterns available as variables without `let` bindings: `email`, `semver`, `uuid`, `date_iso8601`, `time_24h`, `hex_color`, `ipv4`, `jira_ticket`, `conventional_commit`. Controllable via `CompileOptions(patternLibraryEnabled = true)`.
- **Regex Complexity Scorer** — `ComplexityScorer.score(ir)` rates patterns 1-10 for ReDoS risk with detailed factor breakdown (nested quantifiers, alternation branches, lookaround count, backreferences, recursion).
- **Regex Diff Tool** — `RegexDiff.diff(a, b)` compares two regex IR trees structurally, reporting features only in A, only in B, and structural differences with path tracking.
- **Test Generator** — `TestGenerator.generate(ir)` auto-generates matching and non-matching sample strings from regex IR. Configurable via `TestGeneratorOptions(maxExamples, coverAllBranches)`.
- **Capture Group Type Inference** — `CaptureInference.inferGroups(pomskySource)` analyzes named/unnamed captures and generates Kotlin `data class` + `Regex.extract*()` extension function source code.
- **Railroad Diagram Visualization** — `RailroadDiagram.render(ir)` produces ASCII railroad diagrams with horizontal chaining, vertical alternation branching, and quantifier annotations.
- **Pattern Fuzzer** — `PatternFuzzer.fuzz(patterns, flavors)` compiles patterns to multiple flavors and tests for cross-flavor match mismatches. All `commonMain` — fully cross-platform.
- **Performance Benchmarker** — `RegexBenchmark.benchmark(pattern)` measures regex matching throughput (ops/sec, avg latency) with warmup. Uses `kotlin.time.measureTime` — fully cross-platform.
- **Permutations Operation** — new `permute('a' 'b' 'c')` syntax that compiles to all orderings as alternation (`abc|acb|bac|bca|cab|cba`). Maximum 8 elements (40320 alternatives). (#78)
- **CLI Test Runner** — `pomsky test` command runs `test { match ...; reject ...; }` blocks against the compiled regex using the platform regex engine. Reports PASS/FAIL per case. (#112)
- **Native Shared Library (FFI)** — new `ffi` module producing `libpomsky.dylib`/`.so`/`.dll` with C-compatible API: `pomsky_compile`, `pomsky_compile_json`, `pomsky_decompile`, `pomsky_explain`, `pomsky_version`, `pomsky_free`. Auto-generated C header. (#62)
- **CLI Commands** — 7 new subcommands: `migrate` (regex to Pomsky), `explain` (human-readable), `complexity` (score 1-10), `test` (run test blocks), `visualize` (ASCII railroad), `fuzz` (cross-flavor), `benchmark` (throughput).

### Improved

- **PCRE `\w` Unicode Polyfill** — `[word]` in PCRE Unicode mode now polyfills to `[\p{Alphabetic}\pM\p{Nd}\p{Pc}]`, matching the existing JS/.NET/PythonRegex polyfills. (#113)
- **Python `\w` Warning** — compiler warning when Python `re` flavor uses `[word]` in Unicode mode, suggesting `python_regex` flavor for proper Unicode support. (#86)
- **.NET `\p{LC}` Polyfill** — `[LC]` (Cased_Letter) in .NET flavor now expands to `[\p{Lu}\p{Ll}\p{Lt}]`. (#83)
- **Char Class Intersection Polyfill** — `[a-z] & [d-p]` now works in .NET, Python, PythonRegex, RE2, and POSIX ERE by computing the intersection statically at compile time. Unicode property intersections still require native `&&` support.
- **Linter Rule 7** — warns on quantifiers applied to lookaround assertions (`(>> 'x')+`). (#10)
- **Feature Compatibility Matrix** — README now includes a comprehensive table showing feature support across all 10 flavors. (#55)
- **Conditionals** — now correctly documented as supported in all flavors with lookahead (not just PCRE), since they compile to lookahead assertions.

## [0.14.0] - 2026-04-07

### Added

- **Published `decompiler` module** — `ru.kode.pomsky-kt:decompiler` now available as a standalone Maven artifact for regex-to-Pomsky conversion.
- **Published `dsl` module** — `ru.kode.pomsky-kt:dsl` now available as a standalone Maven artifact for the type-safe Kotlin builder API.

## [0.13.0] - 2026-04-07

### Added

- **Regex Decompiler** — new `decompiler` module that converts regex strings back to Pomsky DSL. Supports all 8 regex flavors (Java, Rust, PCRE, JavaScript, Python, .NET, Ruby, RE2). Public API: `Decompiler.decompile(regex, flavor)`. (#49)
- **ReDoS Detection** — static analysis that detects nested unbounded quantifiers (e.g., `(a+)+`, `(a*)*`) which cause catastrophic backtracking. Emits a warning diagnostic without blocking compilation. (#99)
- **Mode Modifiers** — 6 new mode modifiers with `enable`/`disable` syntax and full lexical scoping. (#4)
  - `ignore_case` → `(?i:...)` — case-insensitive matching (all flavors)
  - `multiline` → `(?m:...)` — `^`/`$` match line boundaries (all flavors)
  - `single_line` → `(?s:...)` — `.` matches newlines (all flavors)
  - `extended` → `(?x:...)` — free-spacing mode (all flavors)
  - `reuse_groups` → `(?J:...)` — allow duplicate group names (PCRE only)
  - `ascii_line_breaks` → `(?d:...)` — ASCII-only line breaks (PCRE, Java)
- **Forward Reference Validation for Named Groups** — named forward references (e.g., `::name` before `:name(...)`) are now validated. Rust and RE2 flavors correctly reject them as unsupported. Previously only numbered forward references were validated. (#84)
- **Linter** — opt-in static analysis pass (`CompileOptions(lintEnabled = true)`) that detects common mistakes. (#50)
  - Unreachable alternative: `'ab' | 'abc'` — later alternative shadowed by prefix
  - Unnecessary repetition: `'x'{1}` — has no effect
  - Redundant nested repetition: `('x'?)?`, `('x'+)+` — can be simplified
  - Empty expression: `('')` in non-capturing group — has no effect
  - Unnecessary group: `('ab')` — parentheses around single non-alternation element
  - Quantifier on anchor: `^+`, `$*` — quantifying anchors is usually a mistake
- **Conditionals** — `if (>> 'condition') 'yes' else 'no'` syntax with lookaround conditions. Compiles to `(?=cond)yes|(?!cond)no`. Supports positive/negative lookahead, optional else branch. (#7)
- **Auto-Atomicization** — opt-in optimization (`CompileOptions(autoAtomize = true)`) that inserts atomic groups `(?>...)` around greedy unbounded repetitions when the following element has provably disjoint characters. Prevents unnecessary backtracking. Supports PCRE, Java, .NET.
- **Kotlin DSL** — new `dsl` module with type-safe builder API for constructing Pomsky expressions programmatically. `pomsky { start; literal("hello"); oneOrMore { digit }; end }` compiles to `^hello\d+$`. Supports anchors, literals, character classes, quantifiers, groups, captures, alternation, lookaround, and backreferences.
- **POSIX Extended Regex Flavor** — `RegexFlavor.PosixExtended` for `grep -E`, `awk`, `sed -E`. Uses POSIX bracket expressions (`[[:digit:]]`, `[_[:alnum:]]`, `[[:space:]]`), plain `(...)` groups. No lookaround, backreferences, or Unicode properties. (#11)
- **Auto-Formatter** — `PomskyFormatter.format(source)` parses Pomsky source and re-emits with consistent spacing, indentation, and style. Configurable via `FormatOptions(indentWidth, maxLineLength)`. (#48)
- **Python `regex` Module Flavor** — `RegexFlavor.PythonRegex` for the third-party `regex` module. Supports Unicode properties and atomic groups (unlike plain `Python` flavor). Same `(?P<name>...)` syntax and `\U` escapes. (#87)

### Optimized

- **Assertion optimizations** — removes redundant positive lookaheads when followed by the same expression, outlines boundary assertions from lookahead starts. (#56)
- **Alternation factoring** — extracts common literal prefixes from alternation branches. `'abc' | 'abd'` now compiles to `ab[cd]` instead of `abc|abd`.
- **Dead branch elimination** — removes duplicate and subsumed alternation branches. `'abc' | 'abc' | 'def'` → `'abc' | 'def'`; `['a'-'z'] | ['a'-'c']` → `['a'-'z']`.

### Fixed

- **Unicode `\w` for .NET** — `[word]` in Unicode mode now polyfills to `[\p{Alphabetic}\p{M}\p{Nd}\p{Pc}]` for .NET flavor, matching the existing JavaScript polyfill. Previously .NET's non-Unicode `\w` was used incorrectly. (#88)
- **Empty negated groups** — `![word digit space]` now correctly produces a compile error when the combined shorthands cover all code points, making the negation unmatchable. (#68)
- **.NET BMP limitation** — negating supplementary code points (above U+FFFF) in .NET flavor correctly produces an error, since .NET's UTF-16 surrogate pair encoding can't represent negated supplementary characters. (#89)

### Decompiler Details

The decompiler parses regex strings and produces idiomatic Pomsky DSL:
- Literal merging: `h`, `e`, `l`, `l`, `o` → `'hello'`
- Readable shorthands: `\d` → `[digit]`, `\w` → `[word]`, `\s` → `[space]`
- Proper Pomsky syntax: `^` → `Start`, `$` → `End`, `\b` → `%`
- Named groups: `(?<name>...)` → `:name(...)`
- Lookaround: `(?=...)` → `>> ...`, `(?!...)` → `!>> ...`
- Flavor-aware parsing: PCRE POSIX classes (`[[:alpha:]]`), .NET surrogate pairs, Python `(?P<name>...)` / `(?P=name)`, Ruby `\k'name'`, Rust `\<`/`\>` word boundaries

## [0.12.0] - 2025-11-08

Initial Kotlin Multiplatform port of Pomsky regex language compiler. Feature-complete with the Rust v0.12.0 baseline — all 381 test cases pass across all 8 regex flavors.
