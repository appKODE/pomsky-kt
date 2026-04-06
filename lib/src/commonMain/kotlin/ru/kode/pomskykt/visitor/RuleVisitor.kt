package ru.kode.pomskykt.visitor

import ru.kode.pomskykt.syntax.exprs.*

/**
 * Visitor for traversing a pomsky AST.
 *
 * Ported from pomsky-lib/src/visitor.rs.
 */
enum class NestingKind {
    Group, Alternation, Intersection, Repetition,
    Lookaround, StmtExpr, Let, Negation,
}

interface RuleVisitor {
    fun down(kind: NestingKind) {}
    fun up(kind: NestingKind) {}

    fun visitLiteral(literal: Literal) {}
    fun visitCharClass(charClass: CharClass) {}
    fun visitGroup(group: Group) {}
    fun visitAlternation(alternation: Alternation) {}
    fun visitIntersection(intersection: Intersection) {}
    fun visitRepetition(repetition: Repetition) {}
    fun visitBoundary(boundary: Boundary) {}
    fun visitLookaround(lookaround: Lookaround) {}
    fun visitVariable(variable: Variable) {}
    fun visitReference(reference: Reference) {}
    fun visitRange(range: Range) {}
    fun visitStatement(stmt: Stmt) {}
    fun visitNegation(negation: Negation) {}
    fun visitRegex(regex: Regex) {}
    fun visitRecursion(recursion: Recursion) {}
    fun visitGrapheme() {}
    fun visitCodepoint() {}
    fun visitDot() {}
}

/**
 * Walk the AST calling the visitor methods.
 */
fun walkRule(rule: Rule, visitor: RuleVisitor) {
    when (rule) {
        is Rule.Lit -> visitor.visitLiteral(rule.literal)
        is Rule.Class -> visitor.visitCharClass(rule.charClass)
        is Rule.Grp -> {
            visitor.visitGroup(rule.group)
            visitor.down(NestingKind.Group)
            rule.group.parts.forEach { walkRule(it, visitor) }
            visitor.up(NestingKind.Group)
        }
        is Rule.Alt -> {
            visitor.visitAlternation(rule.alternation)
            visitor.down(NestingKind.Alternation)
            rule.alternation.rules.forEach { walkRule(it, visitor) }
            visitor.up(NestingKind.Alternation)
        }
        is Rule.Inter -> {
            visitor.visitIntersection(rule.intersection)
            visitor.down(NestingKind.Intersection)
            rule.intersection.rules.forEach { walkRule(it, visitor) }
            visitor.up(NestingKind.Intersection)
        }
        is Rule.Rep -> {
            visitor.visitRepetition(rule.repetition)
            visitor.down(NestingKind.Repetition)
            walkRule(rule.repetition.rule, visitor)
            visitor.up(NestingKind.Repetition)
        }
        is Rule.Bound -> visitor.visitBoundary(rule.boundary)
        is Rule.Look -> {
            visitor.visitLookaround(rule.lookaround)
            visitor.down(NestingKind.Lookaround)
            walkRule(rule.lookaround.rule, visitor)
            visitor.up(NestingKind.Lookaround)
        }
        is Rule.Var -> visitor.visitVariable(rule.variable)
        is Rule.Ref -> visitor.visitReference(rule.reference)
        is Rule.Rng -> visitor.visitRange(rule.range)
        is Rule.StmtE -> {
            visitor.visitStatement(rule.stmtExpr.stmt)
            visitor.down(NestingKind.StmtExpr)
            if (rule.stmtExpr.stmt is Stmt.LetDecl) {
                visitor.down(NestingKind.Let)
                walkRule((rule.stmtExpr.stmt as Stmt.LetDecl).letBinding.rule, visitor)
                visitor.up(NestingKind.Let)
            }
            walkRule(rule.stmtExpr.rule, visitor)
            visitor.up(NestingKind.StmtExpr)
        }
        is Rule.Neg -> {
            visitor.visitNegation(rule.negation)
            visitor.down(NestingKind.Negation)
            walkRule(rule.negation.rule, visitor)
            visitor.up(NestingKind.Negation)
        }
        is Rule.Rgx -> visitor.visitRegex(rule.regex)
        is Rule.Recur -> visitor.visitRecursion(rule.recursion)
        Rule.Grapheme -> visitor.visitGrapheme()
        Rule.Codepoint -> visitor.visitCodepoint()
        Rule.Dot -> visitor.visitDot()
    }
}
