# Changelog

All notable changes to pomsky-kt will be documented in this file.

## [0.13.0]

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

### Fixed

- **Unicode `\w` for .NET** — `[word]` in Unicode mode now polyfills to `[\p{Alphabetic}\p{M}\p{Nd}\p{Pc}]` for .NET flavor, matching the existing JavaScript polyfill. Previously .NET's non-Unicode `\w` was used incorrectly. (#88)

### Decompiler Details

The decompiler parses regex strings and produces idiomatic Pomsky DSL:
- Literal merging: `h`, `e`, `l`, `l`, `o` → `'hello'`
- Readable shorthands: `\d` → `[digit]`, `\w` → `[word]`, `\s` → `[space]`
- Proper Pomsky syntax: `^` → `Start`, `$` → `End`, `\b` → `%`
- Named groups: `(?<name>...)` → `:name(...)`
- Lookaround: `(?=...)` → `>> ...`, `(?!...)` → `!>> ...`
- Flavor-aware parsing: PCRE POSIX classes (`[[:alpha:]]`), .NET surrogate pairs, Python `(?P<name>...)` / `(?P=name)`, Ruby `\k'name'`, Rust `\<`/`\>` word boundaries

### Changed

- Removed `linuxArm64` from default build targets.

## [0.12.0] - 2025-11-08

Initial Kotlin Multiplatform port of Pomsky regex language compiler. Feature-complete with the Rust v0.12.0 baseline — all 381 test cases pass across all 8 regex flavors.
