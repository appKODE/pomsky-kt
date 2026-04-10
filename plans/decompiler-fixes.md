# Decompiler Fixes Plan — Make All 50 Failing Round-Trip Tests Green

## Current State (all fixes complete)
- 182 round-trip tests: **182 pass, 0 fail, 0 skip**
- 84 unit tests: all pass

## Previous State (before fixes)
- 182 round-trip tests: 142 pass, 0 fail, 40 skip
- 46 unit tests: all pass

## Fix Categories (by root cause)

### Fix 1: Non-capturing group loss (15 tests)
**Files:** ranges/*, regex/in_group_3, variables/var_used_twice
**Root cause:** Parser strips `(?:...)` non-capturing groups, emitter doesn't re-add them. When the decompiled Pomsky is recompiled, alternations inside non-capturing groups lose their grouping, changing regex semantics.

**Example:**
```
Input:   (?:a|b|c)d
Decompiled: 'a' | 'b' | 'c' 'd'     # WRONG: alternation scope lost
Expected:   ('a' | 'b' | 'c') 'd'   # Need explicit grouping
```

**Fix in `RegexParser.kt`:**
- When parsing a non-capturing group `(?:...)`, keep it as `Regex.Group(NonCapturing)` instead of unwrapping
- The emitter already handles `NonCapturing` groups — it emits `(...)` when needed

**Fix in `PomskyEmitter.kt`:**
- For `NonCapturing` groups, emit parentheses when the group content contains alternation or when group is followed by a quantifier
- Current code unwraps single-part non-capturing groups — only do this when the content is NOT an alternation

### Fix 2: Lookaround not emitted as separate statements (5 tests)
**Files:** repetitions/lookaround_*, variables/nested_scopes_shadowing, groups/opt_in_lookaround
**Root cause:** Multiple consecutive lookarounds like `(?=a)(?<=b)` are parsed as a sequence, but emitted as `>> a << b` which Pomsky interprets as `>> (a << b)` (nested). Each lookaround needs to be a separate statement.

**Example:**
```
Input:   (?=test)*(?<!test){3}
Decompiled: >> 'test'* !<< 'test'{3}
Recompiled: (?=(?:test)*(?<!(?:test){3}))   # WRONG: nested
Expected:   (>> 'test')* (!<< 'test'){3}     # Need grouping around lookaround
```

**Fix in `PomskyEmitter.kt`:**
- When emitting `Regex.Look` inside a sequence (Context.SEQUENCE), wrap in `(...)` so it doesn't consume subsequent nodes
- `>> 'test'` becomes `(>> 'test')` when in sequence context

### Fix 3: Backslash escape handling (4 tests)
**Files:** strings/escapes, strings/special_chars_in_strings, classes/escapes_single, regex/literal, regex/in_group, regex/in_group_2
**Root cause:** Lexer treats `\\` as escaped backslash and produces `Char('\\')`, but emitter puts `\\` inside a Pomsky string literal `'\\'` which Pomsky sees as escaped backslash — but then recompiles to `\\\\` (double-escaped).

**Example:**
```
Input:   test\\test\\ " \.
Decompiled: 'test\\test\\ " .'     # Pomsky sees \\ as single backslash
Recompiled: test\\\\test\\\\ " \.  # Double-escaped
```

**Fix in `RegexLexer.kt`:**
- `\\` (escaped backslash) should produce a single `\` char: `RegexToken.Char('\\')`  — this is already correct
- The problem is in the emitter: when emitting a backslash character in a Pomsky string, we need `'\\'` (escaped in Pomsky), but the forward compiler then produces `\\` — which is correct! The real issue is the test comparison: the original regex has `\\` (escaped backslash = one literal backslash), and the recompiled regex also has `\\`. Let me re-examine...

Actually the issue is that `\` in the original regex is an escape prefix, so `\\` means literal `\`. The lexer correctly produces `Char('\\')`. The emitter writes `'\\'` in Pomsky. Pomsky compiles `'\\'` to `\\` in regex. That should match. But the test shows `\\\\` — meaning the emitter is writing `'\\\\'` or the literal content has two backslashes.

**Real fix in `RegexLexer.kt`:**
- When encountering `\\`, produce `RegexToken.Char('\\')` — already done
- But the emitter's `emitLiteral` escapes `\\` to `\\\\` in Pomsky strings, then Pomsky sees `\\\\` as two backslashes

**Fix in `PomskyEmitter.emitLiteral`:**
- The char `\` in content should be emitted as `'\\'` in Pomsky (single backslash escaped in Pomsky string)
- Current code: `'\\' -> buf.append("\\\\")` — this outputs 4 chars `\\\\` which Pomsky reads as TWO backslashes
- Should be: `'\\' -> buf.append("\\")` — Pomsky string uses `'` delimiters, and `\` only needs escaping before `'` and `\` itself

Wait — in Pomsky strings delimited by `'`, the escape rules are: `\'` for quote, `\\` for backslash. So a single `\` character should be emitted as `'\\'`. The current `buf.append("\\\\")` emits `\\\\` inside the quotes, which would be `'\\\\'` = two backslashes. 

**Fix:** Change `buf.append("\\\\")` to `buf.append("\\")`

### Fix 4: Caret `^` lost in char classes (3 tests)
**Files:** classes/escapes, classes/escapes_js, classes/escapes_pcre
**Root cause:** `[*+?^]` — the `^` after other items in a char class is consumed as `CaretInClass` token (negation marker) instead of as a literal char.

**Example:**
```
Input:   [*+?^]
Decompiled: ['*' '+' '?']     # Missing '^'
```

**Fix in `RegexLexer.kt`:**
- `^` should only be treated as `CaretInClass` (negation) when it's the FIRST character after `[`
- Track position: if we've already seen items in the current char class, emit `Char('^')` instead

### Fix 5: Hex/control char ranges in char classes (3 tests)
**Files:** classes/ascii_shorthands, classes/char_set_codepoints, classes/char_set_codepoints_rust, classes/shorthands_dep_fixed
**Root cause:** Ranges like `[\x00-\x7F]` — the lexer produces `CodePoint(0x00)`, `Hyphen`, `CodePoint(0x7F)` but the parser doesn't construct a `Range` from `CodePoint` tokens, only from `Char` tokens.

**Fix in `RegexParser.parseCharClass`:**
- When processing `CodePoint` tokens followed by `Hyphen`, construct a `Range` the same way as for `Char` tokens
- For code points <= 0xFFFF, convert to char for range bounds

### Fix 6: `\r\n` special escape in literals (2 tests)
**Files:** basics/explicit_crlf, basics/windows_crlf
**Root cause:** `\n` in the regex is lexed as `CodePoint('\n'.code)` which creates `Regex.Literal("\n")`. The emitter puts this inside a Pomsky string as `'\n'` literal. But Pomsky treats `'\n'` as two characters `\` and `n`, not as newline.

**Fix in `PomskyEmitter.emitLiteral`:**
- For control chars `\n`, `\r`, `\t`, emit them as separate Pomsky expressions using the `\n` escape: split the literal and emit `'before' '\n' 'after'`
- Actually in Pomsky strings, `\n` IS a valid escape. Check: does Pomsky support `'\n'` inside single-quoted strings? If yes, the fix is elsewhere.

**Alt fix:** The issue might be that the CodePoint token creates individual Literal nodes that aren't merged. Need to check if `\n` inside Pomsky `'...'` strings works. If not, emit as `U+0A` code point syntax.

### Fix 7: Word boundary patterns (3 tests)
**Files:** boundaries/word_start_end_rust, boundaries/word_start_end_pcre, boundaries/js_ascii_word_start_end
**Root cause:** `\<` and `\>` (Rust word boundaries), `[[:>:]]` (PCRE POSIX classes), and `(?<!\w)(?=\w)` (JS word-start emulation) aren't recognized as word boundary patterns.

**Fix in `RegexLexer.kt`:**
- `\<` → `RegexToken.WordBoundary` with a new `WordStart` variant (or map to the existing token)
- `\>` → `RegexToken.WordBoundary` with `WordEnd` variant

**Fix in `RegexParser.kt`:**
- Map `WordStart`/`WordEnd` tokens to `Regex.Bound(BoundaryKind.WordStart)` / `Regex.Bound(BoundaryKind.WordEnd)`

**For PCRE `[[:>:]]`:** This requires POSIX class parsing in the lexer — recognize `[[:` as start of POSIX class, read until `:]]`.

**For JS `(?<!\w)(?=\w)`:** This is a pattern emitted by codegen for word-start on JS. Recognizing this pattern during decompilation is complex (would need pattern matching on IR tree). Skip for now — emit as lookaround (functionally equivalent but not identical regex string).

### Fix 8: Char class intersection `&&` (2 tests)
**Files:** intersections/ranges_java, intersections/ranges_rs
**Root cause:** The parser sees `&&` as `ClassIntersection` token but just skips it, merging items into a single char set instead of creating `RegexCompoundCharSet`.

**Fix in `RegexParser.parseCharClass`:**
- When encountering `ClassIntersection` token, start a new char set group
- After parsing the full class, if there were intersections, return `Regex.CompoundCharSet` instead of `Regex.CharSet`

### Fix 9: Unicode property/block name resolution (8 tests)
**Files:** prefixes/blk_dotnet, prefixes/blk_ruby, prefixes/script_pcre, prefixes/scx_pcre, prefixes/scx_rs, props/blocks_dotnet, props/blocks_java, props/blocks_ruby, props/scripts_rs
**Root cause:** Property names with prefixes like `Is`, `In`, `sc=`, `scx=` aren't properly resolved to the correct Unicode property type. Block names like `IsGreekandCoptic`, `InBasic_Latin` should map to `BlockProp`, not `CategoryProp`.

**Fix in `RegexParser.parsePropertyName`:**
- Strip `Is` prefix for .NET block names → look up in CodeBlock
- Strip `In` prefix for Java/Ruby block names → look up in CodeBlock
- `sc=X` → ScriptProp with ScriptExtension.No
- `scx=X` → ScriptProp with ScriptExtension.Yes
- Preserve the extension type so recompilation produces the same prefix

### Fix 10: Named group flavor mismatch (1 test)
**Files:** groups/named_capturing_groups
**Root cause:** The test's default flavor is Rust (which uses `(?P<name>...)`), but the decompiler defaults to Java. When recompiled for Rust, the named group uses `(?P<...>)` syntax which doesn't match because the round-trip runner uses Java as default.

**Fix in `RoundTripTestCaseRunner`:**
- Use the test case's specified flavor for decompilation, not hardcoded Java
- Default to Rust (matching the original test runner) when no flavor specified

### Fix 11: Regex literal pass-through (4 tests)
**Files:** regex/in_alt, regex/in_group, regex/in_group_2, regex/literal
**Root cause:** These test cases use Pomsky's `regex '...'` pass-through. The expected regex output contains raw regex that when decompiled and recompiled may get optimized differently (e.g., single-char alternations merged into char sets).

**Example:**
```
Input:   a|bc|d
Decompiled: 'a' | 'bc' | 'd'
Recompiled: [ad]|bc          # Optimizer merges single-char alternations
```

**Fix:** This is an optimization difference, not a semantic error. The recompiled regex is semantically equivalent. These tests need special handling — either:
- Accept semantic equivalence (regex matches same strings) instead of string equality
- Or mark these as known optimization differences

## Implementation Order

1. **Fix 10** (flavor mismatch) — trivial, 1 line
2. **Fix 4** (caret in char class) — easy, lexer change
3. **Fix 3** (backslash escaping) — easy, emitter change  
4. **Fix 6** (control chars) — easy, emitter change
5. **Fix 1** (non-capturing groups) — medium, parser + emitter
6. **Fix 2** (lookaround grouping) — medium, emitter
7. **Fix 5** (hex ranges) — medium, parser
8. **Fix 8** (intersection) — medium, parser
9. **Fix 9** (property names) — medium, parser
10. **Fix 7** (word boundaries) — medium, lexer + parser
11. **Fix 11** (optimization diffs) — skip or semantic comparison

## Expected Result After All Fixes
- Fixes 1-10 should turn ~45 of the 50 failures green
- Fix 11 (5 tests) requires either semantic regex comparison or accepting known optimization diffs
- Target: 0 failures (with optimization-diff tests using `assumeTrue` skip if semantically equivalent)
