package ru.kode.pomskykt.syntax.parse

import ru.kode.pomskykt.syntax.exprs.*
import ru.kode.pomskykt.syntax.diagnose.ParseDiagnosticKind
import ru.kode.pomskykt.syntax.diagnose.ParseErrorKind
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ParserTest {

    private fun parseOk(input: String): Rule {
        val (rule, diagnostics) = parse(input)
        val errors = diagnostics.filter { it.kind is ParseDiagnosticKind.Error }
        assertTrue(errors.isEmpty(), "Expected no errors, got: $errors")
        assertNotNull(rule, "Expected a rule")
        return rule
    }

    private fun parseErr(input: String): ParseErrorKind {
        val (_, diagnostics) = parse(input)
        val errors = diagnostics.filter { it.kind is ParseDiagnosticKind.Error }
        assertTrue(errors.isNotEmpty(), "Expected errors for: $input")
        return (errors.first().kind as ParseDiagnosticKind.Error).error
    }

    // --- Literals ---

    @Test
    fun singleQuotedLiteral() {
        val rule = parseOk("'hello'")
        assertIs<Rule.Lit>(rule)
        assertEquals("hello", rule.literal.content)
    }

    @Test
    fun doubleQuotedLiteral() {
        val rule = parseOk("\"world\"")
        assertIs<Rule.Lit>(rule)
        assertEquals("world", rule.literal.content)
    }

    @Test
    fun escapedDoubleQuote() {
        val rule = parseOk("\"hello\\\"world\"")
        assertIs<Rule.Lit>(rule)
        assertEquals("hello\"world", rule.literal.content)
    }

    // --- Alternation ---

    @Test
    fun alternation() {
        val rule = parseOk("'a' | 'b' | 'c'")
        assertIs<Rule.Alt>(rule)
        assertEquals(3, rule.alternation.rules.size)
    }

    @Test
    fun leadingPipe() {
        val rule = parseOk("| 'a' | 'b'")
        assertIs<Rule.Alt>(rule)
        assertEquals(2, rule.alternation.rules.size)
    }

    // --- Sequence ---

    @Test
    fun sequence() {
        val rule = parseOk("'a' 'b' 'c'")
        assertIs<Rule.Grp>(rule)
        assertEquals(GroupKind.Implicit, rule.group.kind)
        assertEquals(3, rule.group.parts.size)
    }

    // --- Repetition ---

    @Test
    fun starRepetition() {
        val rule = parseOk("'a'*")
        assertIs<Rule.Rep>(rule)
        assertEquals(RepetitionKind.zeroInf(), rule.repetition.kind)
    }

    @Test
    fun plusRepetition() {
        val rule = parseOk("'a'+")
        assertIs<Rule.Rep>(rule)
        assertEquals(RepetitionKind.oneInf(), rule.repetition.kind)
    }

    @Test
    fun questionRepetition() {
        val rule = parseOk("'a'?")
        assertIs<Rule.Rep>(rule)
        assertEquals(RepetitionKind.zeroOne(), rule.repetition.kind)
    }

    @Test
    fun braceRepetitionFixed() {
        val rule = parseOk("'a'{3}")
        assertIs<Rule.Rep>(rule)
        assertEquals(RepetitionKind.fixed(3), rule.repetition.kind)
    }

    @Test
    fun braceRepetitionRange() {
        val rule = parseOk("'a'{2,5}")
        assertIs<Rule.Rep>(rule)
        assertEquals(RepetitionKind(2, 5), rule.repetition.kind)
    }

    @Test
    fun braceRepetitionUnbounded() {
        val rule = parseOk("'a'{2,}")
        assertIs<Rule.Rep>(rule)
        assertEquals(RepetitionKind(2, null), rule.repetition.kind)
    }

    @Test
    fun lazyRepetition() {
        val rule = parseOk("'a'+ lazy")
        assertIs<Rule.Rep>(rule)
        assertEquals(Quantifier.Lazy, rule.repetition.quantifier)
    }

    @Test
    fun greedyRepetition() {
        val rule = parseOk("'a'+ greedy")
        assertIs<Rule.Rep>(rule)
        assertEquals(Quantifier.Greedy, rule.repetition.quantifier)
    }

    // --- Groups ---

    @Test
    fun normalGroup() {
        val rule = parseOk("('a' 'b')")
        assertIs<Rule.Grp>(rule)
        assertEquals(GroupKind.Normal, rule.group.kind)
        assertEquals(2, rule.group.parts.size)
    }

    @Test
    fun capturingGroup() {
        val rule = parseOk(":('a')")
        assertIs<Rule.Grp>(rule)
        assertIs<GroupKind.Capturing>(rule.group.kind)
        assertNull((rule.group.kind as GroupKind.Capturing).capture.name)
    }

    @Test
    fun namedCapturingGroup() {
        val rule = parseOk(":name('test')")
        assertIs<Rule.Grp>(rule)
        assertIs<GroupKind.Capturing>(rule.group.kind)
        assertEquals("name", (rule.group.kind as GroupKind.Capturing).capture.name)
    }

    @Test
    fun atomicGroup() {
        val rule = parseOk("atomic('a')")
        assertIs<Rule.Grp>(rule)
        assertEquals(GroupKind.Atomic, rule.group.kind)
    }

    // --- Boundaries ---

    @Test
    fun boundaries() {
        assertIs<Rule.Bound>(parseOk("^"))
        assertIs<Rule.Bound>(parseOk("$"))
        assertIs<Rule.Bound>(parseOk("%"))
    }

    @Test
    fun boundaryKinds() {
        assertEquals(BoundaryKind.Start, (parseOk("^") as Rule.Bound).boundary.kind)
        assertEquals(BoundaryKind.End, (parseOk("$") as Rule.Bound).boundary.kind)
        assertEquals(BoundaryKind.Word, (parseOk("%") as Rule.Bound).boundary.kind)
    }

    // --- Dot ---

    @Test
    fun dot() {
        assertEquals(Rule.Dot, parseOk("."))
    }

    // --- Negation ---

    @Test
    fun negation() {
        val rule = parseOk("!'a'")
        assertIs<Rule.Neg>(rule)
    }

    // --- Lookaround ---

    @Test
    fun lookahead() {
        val rule = parseOk(">> 'a'")
        assertIs<Rule.Look>(rule)
        assertEquals(LookaroundKind.Ahead, rule.lookaround.kind)
    }

    @Test
    fun lookbehind() {
        val rule = parseOk("<< 'a'")
        assertIs<Rule.Look>(rule)
        assertEquals(LookaroundKind.Behind, rule.lookaround.kind)
    }

    @Test
    fun negativeLookahead() {
        val rule = parseOk("!>> 'a'")
        assertIs<Rule.Neg>(rule)
        assertIs<Rule.Look>((rule as Rule.Neg).negation.rule)
    }

    // --- Reference ---

    @Test
    fun namedReference() {
        val rule = parseOk("::name")
        assertIs<Rule.Ref>(rule)
        assertIs<ReferenceTarget.Named>(rule.reference.target)
        assertEquals("name", (rule.reference.target as ReferenceTarget.Named).name)
    }

    @Test
    fun numberedReference() {
        val rule = parseOk("::1")
        assertIs<Rule.Ref>(rule)
        assertIs<ReferenceTarget.Number>(rule.reference.target)
        assertEquals(1, (rule.reference.target as ReferenceTarget.Number).number)
    }

    // --- Let bindings ---

    @Test
    fun letBinding() {
        val rule = parseOk("let x = 'test'; x")
        assertIs<Rule.StmtE>(rule)
        assertIs<Stmt.LetDecl>(rule.stmtExpr.stmt)
        assertEquals("test", ((rule.stmtExpr.stmt as Stmt.LetDecl).letBinding.rule as Rule.Lit).literal.content)
    }

    // --- Mode modifiers ---

    @Test
    fun enableLazy() {
        val rule = parseOk("enable lazy; 'a'+")
        assertIs<Rule.StmtE>(rule)
        assertIs<Stmt.Enable>(rule.stmtExpr.stmt)
    }

    @Test
    fun disableUnicode() {
        val rule = parseOk("disable unicode; 'a'")
        assertIs<Rule.StmtE>(rule)
        assertIs<Stmt.Disable>(rule.stmtExpr.stmt)
    }

    // --- Character classes ---

    @Test
    fun charClassShorthand() {
        val rule = parseOk("[w]")
        assertIs<Rule.Class>(rule)
        assertEquals(1, rule.charClass.inner.size)
        assertIs<GroupItem.Named>(rule.charClass.inner[0])
    }

    @Test
    fun charClassRange() {
        val rule = parseOk("['a'-'z']")
        assertIs<Rule.Class>(rule)
        assertEquals(1, rule.charClass.inner.size)
        assertIs<GroupItem.CharRange>(rule.charClass.inner[0])
    }

    @Test
    fun charClassNegated() {
        val rule = parseOk("[!w]")
        assertIs<Rule.Class>(rule)
        val item = rule.charClass.inner[0] as GroupItem.Named
        assertTrue(item.negative)
    }

    // --- Variable ---

    @Test
    fun variable() {
        val rule = parseOk("let x = 'a'; x")
        assertIs<Rule.StmtE>(rule)
        assertIs<Rule.Var>(rule.stmtExpr.rule)
    }

    // --- Regex ---

    @Test
    fun regexLiteral() {
        val rule = parseOk("regex '[a-z]+'")
        assertIs<Rule.Rgx>(rule)
        assertEquals("[a-z]+", rule.regex.content)
    }

    // --- Range ---

    @Test
    fun rangeExpression() {
        val rule = parseOk("range '0'-'255'")
        assertIs<Rule.Rng>(rule)
    }

    // --- Recursion ---

    @Test
    fun recursion() {
        val rule = parseOk("recursion")
        assertIs<Rule.Recur>(rule)
    }

    // --- Intersection ---

    @Test
    fun intersection() {
        val rule = parseOk("[Letter] & [Latin]")
        assertIs<Rule.Inter>(rule)
        assertEquals(2, rule.intersection.rules.size)
    }

    // --- Errors ---

    @Test
    fun unclosedString() {
        val err = parseErr("'hello")
        assertIs<ParseErrorKind.LexError>(err)
    }

    @Test
    fun emptyCharClass() {
        val err = parseErr("[]")
        assertIs<ParseErrorKind.CharClassError>(err)
    }

    // --- Complex expressions ---

    @Test
    fun complexExpression() {
        val rule = parseOk("let digit = [d]; ^ digit+ ('.' digit+)? $")
        assertNotNull(rule)
    }

    @Test
    fun pomskySample() {
        val rule = parseOk("'hello' | 'world'")
        assertIs<Rule.Alt>(rule)
        assertEquals(2, rule.alternation.rules.size)
    }
}
