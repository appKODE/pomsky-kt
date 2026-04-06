package ru.kode.pomskykt.capturing

import ru.kode.pomskykt.diagnose.CompileError
import ru.kode.pomskykt.diagnose.CompileErrorKind
import ru.kode.pomskykt.syntax.exprs.*
import ru.kode.pomskykt.visitor.NestingKind
import ru.kode.pomskykt.visitor.RuleVisitor

/**
 * Collects capturing group information from the AST before compilation.
 *
 * Ported from pomsky-lib/src/capturing_groups.rs.
 */
data class CapturingGroupIndex(
    val fromNamed: Int,
    val absolute: Int,
)

class CapturingGroupsCollector : RuleVisitor {
    var countNamed: Int = 0
        private set
    var countNumbered: Int = 0
        private set
    val names: MutableMap<String, CapturingGroupIndex> = mutableMapOf()
    private var variableNesting: Int = 0

    override fun down(kind: NestingKind) {
        if (kind == NestingKind.Let) variableNesting++
    }

    override fun up(kind: NestingKind) {
        if (kind == NestingKind.Let) variableNesting--
    }

    override fun visitGroup(group: Group) {
        if (variableNesting > 0) return
        val kind = group.kind
        if (kind is GroupKind.Capturing) {
            val name = kind.capture.name
            if (name != null) {
                // Groups are 1-indexed: first group is 1
                val idx = CapturingGroupIndex(
                    fromNamed = countNamed + 1,
                    absolute = countNamed + countNumbered + 1,
                )
                names[name] = idx
                countNamed++
            } else {
                countNumbered++
            }
        }
    }

    override fun visitReference(reference: Reference) {
        // Validation happens elsewhere
    }
}
