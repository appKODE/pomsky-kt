package ru.kode.pomskykt

import ru.kode.pomskykt.capturing.CapturingGroupsCollector
import ru.kode.pomskykt.compile.CompileException
import ru.kode.pomskykt.compile.CompileState
import ru.kode.pomskykt.compile.compileRule
import ru.kode.pomskykt.diagnose.CompileErrorKind
import ru.kode.pomskykt.diagnose.Diagnostic
import ru.kode.pomskykt.diagnose.DiagnosticKind
import ru.kode.pomskykt.diagnose.Severity
import ru.kode.pomskykt.diagnose.getCompileHelp
import ru.kode.pomskykt.diagnose.getParseHelp
import ru.kode.pomskykt.diagnose.toMessage
import ru.kode.pomskykt.options.CompileOptions
import ru.kode.pomskykt.patterns.PatternLibrary
import ru.kode.pomskykt.options.RegexFlavor
import ru.kode.pomskykt.regex.autoAtomize
import ru.kode.pomskykt.regex.codegen
import ru.kode.pomskykt.regex.eliminateDeadBranches
import ru.kode.pomskykt.regex.factorAlternations
import ru.kode.pomskykt.regex.optimize
import ru.kode.pomskykt.regex.optimizeAssertions
import ru.kode.pomskykt.syntax.diagnose.ParseDiagnosticKind
import ru.kode.pomskykt.syntax.diagnose.toMessage
import ru.kode.pomskykt.syntax.Span
import ru.kode.pomskykt.syntax.exprs.*
import ru.kode.pomskykt.validation.Linter
import ru.kode.pomskykt.validation.Validator
import ru.kode.pomskykt.visitor.walkRule

/**
 * Public entry point for the Pomsky compiler.
 *
 * Wraps a parsed [Rule] and provides [compile] and [parseAndCompile].
 */
class Expr(val rule: Rule) {

    companion object {
        const val VERSION = "0.14.0"

        /**
         * Parse a pomsky expression.
         *
         * @return Pair of (Expr or null, diagnostics)
         */
        fun parse(input: String): Pair<Expr?, List<Diagnostic>> {
            val (rule, parseDiags) = ru.kode.pomskykt.syntax.parse.parse(input)
            val diagnostics = parseDiags.map { diag ->
                Diagnostic(
                    severity = when (diag.kind) {
                        is ParseDiagnosticKind.Error -> Severity.Error
                        is ParseDiagnosticKind.Warning -> Severity.Warning
                    },
                    msg = diag.kind.toMessage(),
                    span = diag.span,
                    help = getParseHelp(diag.kind, input, diag.span),
                )
            }
            return if (rule != null) Expr(rule) to diagnostics
            else null to diagnostics
        }

        /**
         * Parse and compile in one step.
         *
         * @return Triple of (regex string or null, diagnostics, tests)
         */
        fun parseAndCompile(
            input: String,
            options: CompileOptions = CompileOptions(),
        ): Triple<String?, List<Diagnostic>, List<Test>> {
            val (expr, diagnostics) = parse(input)
            if (expr == null) return Triple(null, diagnostics, emptyList())
            if (diagnostics.any { it.severity == Severity.Error }) {
                return Triple(null, diagnostics, emptyList())
            }
            val (result, compileDiags) = expr.compile(input, options)
            val tests = expr.extractTests()
            return Triple(result, diagnostics + compileDiags, tests)
        }
    }

    /**
     * Compile the parsed expression to a regex string.
     *
     * Pipeline: validate → collect groups → compile to IR → optimize → codegen
     */
    fun compile(
        input: String,
        options: CompileOptions = CompileOptions(),
    ): Pair<String?, List<Diagnostic>> {
        val diagnostics = mutableListOf<Diagnostic>()

        // 1. Validate
        val validator = Validator(options)
        walkRule(rule, validator)
        // Separate warnings (like ReDoS) from hard errors
        for (err in validator.compileErrors) {
            val severity = when (err.kind) {
                is CompileErrorKind.NestedQuantifiers -> Severity.Warning
                is CompileErrorKind.PythonWordUnicodeHint -> Severity.Warning
                else -> Severity.Error
            }
            diagnostics.add(Diagnostic(
                severity = severity,
                msg = err.kind.toMessage(),
                span = err.span,
                kind = DiagnosticKind.Compat,
                help = getCompileHelp(err.kind, err.span, input),
            ))
            // Short-circuit on first hard error (matching Rust behavior)
            if (severity == Severity.Error) break
        }
        if (diagnostics.any { it.severity == Severity.Error }) {
            return null to diagnostics
        }

        // 1b. Lint pass (warnings only, never blocks compilation)
        if (options.lintEnabled) {
            val linter = Linter()
            walkRule(rule, linter)
            diagnostics.addAll(linter.warnings)
        }

        // 2. Collect capturing groups
        val collector = CapturingGroupsCollector()
        walkRule(rule, collector)

        // 3. Extract variables from let bindings and add built-in variables
        val noSpan = Span.EMPTY
        val start = Rule.Bound(Boundary(BoundaryKind.Start, true, noSpan))
        val end = Rule.Bound(Boundary(BoundaryKind.End, true, noSpan))
        val grapheme = Rule.Grapheme
        val codepoint = Rule.Codepoint

        val builtins = listOf<Pair<String, Rule>>(
            "Start" to start,
            "End" to end,
            "Grapheme" to grapheme,
            "G" to grapheme,
            "Codepoint" to codepoint,
            "C" to codepoint,
        )
        val library = if (options.patternLibraryEnabled) PatternLibrary.patterns else emptyList()
        val variables = builtins + library + extractVariables(rule)

        // 4. Compile to regex IR
        val state = CompileState(collector, variables)
        val regexIR = try {
            compileRule(rule, options, state)
        } catch (e: CompileException) {
            diagnostics.add(Diagnostic(
                severity = Severity.Error,
                msg = e.kind.toMessage(),
                span = e.span,
                kind = DiagnosticKind.Resolve,
                help = getCompileHelp(e.kind, e.span, input),
            ))
            return null to diagnostics
        }

        // 5. Optimize
        val optimized = regexIR.optimize() ?: regexIR

        // 5a. Factor common prefixes from alternations
        val factored = optimized.factorAlternations()

        // 5b. Eliminate dead (redundant) alternation branches
        val pruned = factored.eliminateDeadBranches()

        // 5c. Optimize lookaround assertions
        val assertionOpt = pruned.optimizeAssertions()

        // 5d. Auto-atomize (opt-in, only for flavors supporting atomic groups)
        val atomized = if (options.autoAtomize && flavorSupportsAtomicGroups(options.flavor)) {
            assertionOpt.autoAtomize()
        } else {
            assertionOpt
        }

        // 6. Codegen
        val result = atomized.codegen(options.flavor)

        // Add any diagnostics from compilation state
        diagnostics.addAll(state.diagnostics)

        return result to diagnostics
    }

    /** Extract test blocks from the AST. */
    fun extractTests(): List<Test> {
        val tests = mutableListOf<Test>()
        collectTests(rule, tests)
        return tests
    }

    private fun collectTests(rule: Rule, tests: MutableList<Test>) {
        if (rule is Rule.StmtE) {
            val stmt = rule.stmtExpr.stmt
            if (stmt is Stmt.TestDecl) {
                tests.add(stmt.test)
            }
            collectTests(rule.stmtExpr.rule, tests)
        }
    }

    /** Extract let-bound variables as (name, rule) pairs. */
    private fun extractVariables(rule: Rule): List<Pair<String, Rule>> {
        val vars = mutableListOf<Pair<String, Rule>>()
        collectVariables(rule, vars)
        return vars
    }

    private fun collectVariables(rule: Rule, vars: MutableList<Pair<String, Rule>>) {
        if (rule is Rule.StmtE) {
            val stmt = rule.stmtExpr.stmt
            if (stmt is Stmt.LetDecl) {
                vars.add(stmt.letBinding.name to stmt.letBinding.rule)
            }
            collectVariables(rule.stmtExpr.rule, vars)
        }
    }
}

/**
 * Returns true if the given flavor supports atomic groups `(?>...)`.
 * Supported: PCRE, Java, .NET. Not supported: JavaScript, Python, RE2, Ruby, Rust.
 */
private fun flavorSupportsAtomicGroups(flavor: RegexFlavor): Boolean = when (flavor) {
    RegexFlavor.Pcre, RegexFlavor.Java, RegexFlavor.DotNet, RegexFlavor.PythonRegex -> true
    else -> false
}
