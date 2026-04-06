package ru.kode.pomskykt

import ru.kode.pomskykt.capturing.CapturingGroupsCollector
import ru.kode.pomskykt.compile.CompileException
import ru.kode.pomskykt.compile.CompileState
import ru.kode.pomskykt.compile.compileRule
import ru.kode.pomskykt.diagnose.Diagnostic
import ru.kode.pomskykt.diagnose.DiagnosticKind
import ru.kode.pomskykt.diagnose.Severity
import ru.kode.pomskykt.diagnose.getCompileHelp
import ru.kode.pomskykt.diagnose.getParseHelp
import ru.kode.pomskykt.diagnose.toMessage
import ru.kode.pomskykt.options.CompileOptions
import ru.kode.pomskykt.regex.codegen
import ru.kode.pomskykt.regex.optimize
import ru.kode.pomskykt.syntax.diagnose.ParseDiagnosticKind
import ru.kode.pomskykt.syntax.diagnose.toMessage
import ru.kode.pomskykt.syntax.Span
import ru.kode.pomskykt.syntax.exprs.*
import ru.kode.pomskykt.validation.Validator
import ru.kode.pomskykt.visitor.walkRule

/**
 * Public entry point for the Pomsky compiler.
 *
 * Wraps a parsed [Rule] and provides [compile] and [parseAndCompile].
 */
class Expr(val rule: Rule) {

    companion object {
        const val VERSION = "0.12.0"

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
        // Rust's compiler short-circuits on first compile error (via `?`), so only report one.
        validator.compileErrors.firstOrNull()?.let { err ->
            diagnostics.add(Diagnostic(
                severity = Severity.Error,
                msg = err.kind.toMessage(),
                span = err.span,
                kind = DiagnosticKind.Compat,
                help = getCompileHelp(err.kind, err.span, input),
            ))
        }
        if (diagnostics.any { it.severity == Severity.Error }) {
            return null to diagnostics
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
        val variables = builtins + extractVariables(rule)

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

        // 6. Codegen
        val result = optimized.codegen(options.flavor)

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
