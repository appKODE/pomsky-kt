package ru.kode.pomskykt.syntax.exprs

import ru.kode.pomskykt.syntax.Span

/** A statement followed by an expression: `let x = 'a'; x+`. */
data class StmtExpr(
    val stmt: Stmt,
    val rule: Rule,
    val span: Span,
)

/** A statement (enable/disable/let/test). */
sealed class Stmt {
    data class Enable(val setting: BooleanSetting, val span: Span) : Stmt()
    data class Disable(val setting: BooleanSetting, val span: Span) : Stmt()
    data class LetDecl(val letBinding: Let) : Stmt()
    data class TestDecl(val test: Test) : Stmt()
}

/** A boolean setting that can be enabled or disabled. */
enum class BooleanSetting {
    Lazy,
    Unicode,
}

/** A variable binding: `let name = rule`. */
data class Let(
    val name: String,
    val rule: Rule,
    val nameSpan: Span,
)

/** A test block containing test cases. */
data class Test(
    val cases: List<TestCase>,
    val span: Span,
)

/** A test case. */
sealed class TestCase {
    data class Match(val testCaseMatch: TestCaseMatch) : TestCase()
    data class MatchAll(val testCaseMatchAll: TestCaseMatchAll) : TestCase()
    data class Reject(val testCaseReject: TestCaseReject) : TestCase()
}

/** A test case that expects a match. */
data class TestCaseMatch(
    val literal: Literal,
    val captures: List<TestCapture>,
    val span: Span,
)

/** A test case that expects all matches. */
data class TestCaseMatchAll(
    val literal: Literal,
    val matches: List<TestCaseMatch>,
)

/** A test case that expects rejection. */
data class TestCaseReject(
    val literal: Literal,
    val asSubstring: Boolean,
)

/** A capture assertion in a test case. */
data class TestCapture(
    val ident: CaptureIdent,
    val identSpan: Span,
    val literal: Literal,
)

/** Identifier for a capture group in a test. */
sealed class CaptureIdent {
    data class Name(val name: String) : CaptureIdent()
    data class Index(val index: Int) : CaptureIdent()
}
