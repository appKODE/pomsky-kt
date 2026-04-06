package ru.kode.pomskykt.compile

import ru.kode.pomskykt.capturing.CapturingGroupIndex
import ru.kode.pomskykt.capturing.CapturingGroupsCollector
import ru.kode.pomskykt.diagnose.Diagnostic
import ru.kode.pomskykt.options.CompileOptions
import ru.kode.pomskykt.syntax.exprs.Rule

/**
 * Mutable state carried through compilation.
 *
 * Ported from pomsky-lib/src/compile/mod.rs.
 */
class CompileState(
    capturingGroups: CapturingGroupsCollector,
    initialVariables: List<Pair<String, Rule>> = emptyList(),
) {
    val variables: MutableList<Pair<String, Rule>> = initialVariables.toMutableList()
    var nextIdx: Int = 1
    val usedNamesVec = mutableListOf<String?>()
    val usedNames: MutableMap<String, CapturingGroupIndex> = capturingGroups.names.toMutableMap()
    var groupsCount: Int = capturingGroups.countNamed + capturingGroups.countNumbered
    var numberedGroupsCount: Int = capturingGroups.countNumbered
    var namedGroupsCount: Int = capturingGroups.countNamed
    var inLookbehind: Boolean = false
    val currentVars: MutableSet<Int> = mutableSetOf()
    val diagnostics: MutableList<Diagnostic> = mutableListOf()

    /** True when currently compiling the body of a `let` binding. */
    var inLetBinding: Boolean = false

    /** Nesting depth inside groups (for nested test detection). */
    var nestingDepth: Int = 0

    /** Names of capturing groups defined so far (for duplicate detection). */
    val definedGroupNames: MutableSet<String> = mutableSetOf()

    /**
     * Map from absolute group index to whether it is named.
     * Key = absolute index (1-based), value = true if named.
     */
    val groupIndexIsNamed: MutableMap<Int, Boolean> = mutableMapOf()

    fun hasNamedGroups(): Boolean = usedNames.isNotEmpty()
    fun hasNumberedGroups(): Boolean = numberedGroupsCount > 0

    fun pushVariable(name: String, rule: Rule) {
        variables.add(name to rule)
    }

    fun popVariable() {
        if (variables.isNotEmpty()) {
            variables.removeAt(variables.size - 1)
        }
    }
}

/**
 * Interface for AST nodes that can compile to regex IR.
 */
interface Compile {
    fun compile(options: CompileOptions, state: CompileState): ru.kode.pomskykt.regex.Regex
}
