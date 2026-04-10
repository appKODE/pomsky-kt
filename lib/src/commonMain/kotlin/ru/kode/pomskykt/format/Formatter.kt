package ru.kode.pomskykt.format

import ru.kode.pomskykt.Expr
import ru.kode.pomskykt.diagnose.Severity
import ru.kode.pomskykt.syntax.exprs.*

/**
 * Options for formatting Pomsky source code.
 */
data class FormatOptions(
    val indentWidth: Int = 2,
    val maxLineLength: Int = 80,
)

/**
 * Formats Pomsky source code with consistent style.
 *
 * Parses the source to AST, then re-emits it with consistent spacing and indentation.
 */
object PomskyFormatter {

    /**
     * Format the given Pomsky source string.
     *
     * @return the formatted source, or null if parsing fails.
     */
    fun format(source: String, options: FormatOptions = FormatOptions()): String? {
        val (expr, diags) = Expr.parse(source)
        if (expr == null || diags.any { it.severity == Severity.Error }) return null
        val buf = StringBuilder()
        emitRule(expr.rule, 0, options, buf)
        return buf.toString()
    }

    private fun emitRule(rule: Rule, indent: Int, options: FormatOptions, buf: StringBuilder) {
        when (rule) {
            is Rule.Lit -> emitLiteral(rule.literal, buf)
            is Rule.Alt -> emitAlternation(rule.alternation, indent, options, buf)
            is Rule.Class -> emitCharClass(rule.charClass, buf)
            is Rule.Grp -> emitGroup(rule.group, indent, options, buf)
            is Rule.Rep -> emitRepetition(rule.repetition, indent, options, buf)
            is Rule.Bound -> emitBoundary(rule.boundary, buf)
            is Rule.Look -> emitLookaround(rule.lookaround, indent, options, buf)
            is Rule.Var -> buf.append(rule.variable.name)
            is Rule.Ref -> emitReference(rule.reference, buf)
            is Rule.Neg -> emitNegation(rule.negation, indent, options, buf)
            is Rule.StmtE -> emitStmtExpr(rule.stmtExpr, indent, options, buf)
            is Rule.Rgx -> emitRegexLiteral(rule.regex, buf)
            is Rule.Recur -> buf.append("recursion")
            is Rule.Rng -> emitRange(rule.range, buf)
            is Rule.Inter -> emitIntersection(rule.intersection, indent, options, buf)
            is Rule.Cond -> emitConditional(rule.conditional, indent, options, buf)
            is Rule.Perm -> {
                buf.append("permute(")
                rule.permutation.rules.forEachIndexed { i, r ->
                    if (i > 0) buf.append(' ')
                    emitRule(r, indent, options, buf)
                }
                buf.append(')')
            }
            Rule.Grapheme -> buf.append("Grapheme")
            Rule.Codepoint -> buf.append("Codepoint")
            Rule.Dot -> buf.append('.')
        }
    }

    private fun emitLiteral(literal: Literal, buf: StringBuilder) {
        buf.append('\'')
        buf.append(literal.content.replace("\\", "\\\\").replace("'", "\\'"))
        buf.append('\'')
    }

    private fun emitAlternation(alt: Alternation, indent: Int, options: FormatOptions, buf: StringBuilder) {
        alt.rules.forEachIndexed { i, rule ->
            if (i > 0) buf.append(" | ")
            emitRule(rule, indent, options, buf)
        }
    }

    private fun emitIntersection(inter: Intersection, indent: Int, options: FormatOptions, buf: StringBuilder) {
        inter.rules.forEachIndexed { i, rule ->
            if (i > 0) buf.append(" & ")
            emitRule(rule, indent, options, buf)
        }
    }

    private fun emitCharClass(charClass: CharClass, buf: StringBuilder) {
        buf.append('[')
        charClass.inner.forEachIndexed { i, item ->
            if (i > 0) buf.append(' ')
            emitGroupItem(item, buf)
        }
        buf.append(']')
    }

    private fun emitGroupItem(item: GroupItem, buf: StringBuilder) {
        when (item) {
            is GroupItem.Char -> {
                buf.append('\'')
                buf.append(escapeChar(item.char))
                buf.append('\'')
            }
            is GroupItem.CodePoint -> {
                buf.append("U+")
                buf.append(item.codePoint.toString(16).uppercase().padStart(4, '0'))
            }
            is GroupItem.CharRange -> {
                buf.append('\'')
                buf.append(escapeChar(item.first))
                buf.append("'-'")
                buf.append(escapeChar(item.last))
                buf.append('\'')
            }
            is GroupItem.Named -> {
                if (item.negative) buf.append('!')
                buf.append(groupNameToString(item.name))
            }
        }
    }

    private fun escapeChar(c: Char): String = when (c) {
        '\'' -> "\\'"
        '\\' -> "\\\\"
        '\n' -> "\\n"
        '\r' -> "\\r"
        '\t' -> "\\t"
        else -> c.toString()
    }

    private fun groupNameToString(name: GroupName): String = when (name) {
        GroupName.Word -> "word"
        GroupName.Digit -> "digit"
        GroupName.Space -> "space"
        GroupName.HorizSpace -> "horiz_space"
        GroupName.VertSpace -> "vert_space"
        is GroupName.CategoryName -> name.category.fullName
        is GroupName.ScriptName -> name.script.fullName
        is GroupName.CodeBlockName -> name.block.fullName
        is GroupName.OtherPropertyName -> name.property.fullName
    }

    private fun emitGroup(group: Group, indent: Int, options: FormatOptions, buf: StringBuilder) {
        val isImplicit = group.kind is GroupKind.Implicit
        when (val kind = group.kind) {
            is GroupKind.Capturing -> {
                val name = kind.capture.name
                if (name != null) {
                    buf.append(":$name(")
                } else {
                    buf.append(":(")
                }
            }
            is GroupKind.Atomic -> buf.append("atomic(")
            is GroupKind.Normal -> buf.append('(')
            is GroupKind.Implicit -> {} // no parens for implicit groups
        }
        group.parts.forEachIndexed { i, part ->
            if (i > 0) buf.append(' ')
            emitRule(part, indent, options, buf)
        }
        if (!isImplicit) buf.append(')')
    }

    private fun emitRepetition(rep: Repetition, indent: Int, options: FormatOptions, buf: StringBuilder) {
        emitRule(rep.rule, indent, options, buf)
        when {
            rep.kind.lowerBound == 0 && rep.kind.upperBound == null -> buf.append('*')
            rep.kind.lowerBound == 1 && rep.kind.upperBound == null -> buf.append('+')
            rep.kind.lowerBound == 0 && rep.kind.upperBound == 1 -> buf.append('?')
            rep.kind.lowerBound == rep.kind.upperBound -> buf.append("{${rep.kind.lowerBound}}")
            rep.kind.upperBound == null -> buf.append("{${rep.kind.lowerBound},}")
            else -> buf.append("{${rep.kind.lowerBound},${rep.kind.upperBound}}")
        }
        if (rep.quantifier == Quantifier.Lazy || rep.quantifier == Quantifier.DefaultLazy) {
            buf.append(" lazy")
        }
    }

    private fun emitBoundary(boundary: Boundary, buf: StringBuilder) {
        when (boundary.kind) {
            BoundaryKind.Start -> buf.append('^')
            BoundaryKind.End -> buf.append('$')
            BoundaryKind.Word -> buf.append('%')
            BoundaryKind.NotWord -> buf.append("!%")
            BoundaryKind.WordStart -> buf.append('<')
            BoundaryKind.WordEnd -> buf.append('>')
        }
    }

    private fun emitLookaround(look: Lookaround, indent: Int, options: FormatOptions, buf: StringBuilder) {
        when (look.kind) {
            LookaroundKind.Ahead -> buf.append(">> ")
            LookaroundKind.Behind -> buf.append("<< ")
            LookaroundKind.AheadNegative -> buf.append("!>> ")
            LookaroundKind.BehindNegative -> buf.append("!<< ")
        }
        emitRule(look.rule, indent, options, buf)
    }

    private fun emitReference(ref: Reference, buf: StringBuilder) {
        when (val target = ref.target) {
            is ReferenceTarget.Named -> buf.append("::${target.name}")
            is ReferenceTarget.Number -> buf.append("::${target.number}")
            is ReferenceTarget.Relative -> {
                if (target.offset >= 0) buf.append("::+${target.offset}")
                else buf.append("::${target.offset}")
            }
        }
    }

    private fun emitNegation(neg: Negation, indent: Int, options: FormatOptions, buf: StringBuilder) {
        buf.append('!')
        emitRule(neg.rule, indent, options, buf)
    }

    private fun emitStmtExpr(stmtExpr: StmtExpr, indent: Int, options: FormatOptions, buf: StringBuilder) {
        when (val stmt = stmtExpr.stmt) {
            is Stmt.LetDecl -> {
                buf.append("let ")
                buf.append(stmt.letBinding.name)
                buf.append(" = ")
                emitRule(stmt.letBinding.rule, indent, options, buf)
                buf.append(";\n")
            }
            is Stmt.Enable -> {
                buf.append("enable ")
                buf.append(boolSettingName(stmt.setting))
                buf.append(";\n")
            }
            is Stmt.Disable -> {
                buf.append("disable ")
                buf.append(boolSettingName(stmt.setting))
                buf.append(";\n")
            }
            is Stmt.TestDecl -> {
                // Preserve test declarations as-is (simplified)
                buf.append("test ")
                buf.append("{ /* ... */ }")
                buf.append('\n')
            }
        }
        emitRule(stmtExpr.rule, indent, options, buf)
    }

    private fun boolSettingName(setting: BooleanSetting): String = when (setting) {
        BooleanSetting.Lazy -> "lazy"
        BooleanSetting.Unicode -> "unicode"
        BooleanSetting.IgnoreCase -> "ignore_case"
        BooleanSetting.Multiline -> "multiline"
        BooleanSetting.SingleLine -> "single_line"
        BooleanSetting.Extended -> "extended"
        BooleanSetting.ReuseGroups -> "reuse_groups"
        BooleanSetting.AsciiLineBreaks -> "ascii_line_breaks"
    }

    private fun emitRegexLiteral(regex: ru.kode.pomskykt.syntax.exprs.Regex, buf: StringBuilder) {
        buf.append("regex '")
        buf.append(regex.content)
        buf.append('\'')
    }

    private fun emitRange(range: Range, buf: StringBuilder) {
        val start = range.start.joinToString("") { it.toInt().and(0xFF).toString(range.radix) }
        val end = range.end.joinToString("") { it.toInt().and(0xFF).toString(range.radix) }
        buf.append("range '")
        buf.append(start)
        buf.append("'-'")
        buf.append(end)
        buf.append('\'')
    }

    private fun emitConditional(cond: Conditional, indent: Int, options: FormatOptions, buf: StringBuilder) {
        buf.append("if ")
        emitRule(cond.condition, indent, options, buf)
        buf.append(' ')
        emitRule(cond.thenBranch, indent, options, buf)
        val elseBranch = cond.elseBranch
        if (elseBranch != null) {
            buf.append(" else ")
            emitRule(elseBranch, indent, options, buf)
        }
    }
}
