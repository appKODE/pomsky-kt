package ru.kode.pomskykt.analysis

import ru.kode.pomskykt.Expr
import ru.kode.pomskykt.capturing.CapturingGroupsCollector
import ru.kode.pomskykt.options.CompileOptions
import ru.kode.pomskykt.visitor.walkRule

data class CaptureGroupInfo(
    val name: String?,
    val index: Int,
    val propertyName: String,
)

data class GeneratedDataClass(
    val className: String,
    val code: String,
    val groups: List<CaptureGroupInfo>,
)

object CaptureInference {
    fun inferGroups(
        pomskySource: String,
        options: CompileOptions = CompileOptions(),
        className: String = "MatchGroups",
    ): GeneratedDataClass? {
        // 1. Parse and compile to verify the source is valid
        val (result, _, _) = Expr.parseAndCompile(pomskySource, options)
        if (result == null) return null

        // 2. Parse the Pomsky source to get AST, then collect capturing groups
        val (rule, _) = ru.kode.pomskykt.syntax.parse.parse(pomskySource)
        if (rule == null) return null

        val collector = CapturingGroupsCollector()
        walkRule(rule, collector)

        if (collector.countNamed + collector.countNumbered == 0) return null

        // 3. Build group info list
        val groups = mutableListOf<CaptureGroupInfo>()

        // Named groups (from collector.names)
        for ((name, idx) in collector.names) {
            groups.add(
                CaptureGroupInfo(
                    name = name,
                    index = idx.absolute,
                    propertyName = toCamelCase(name),
                )
            )
        }

        // Numbered (unnamed) groups
        for (i in 1..collector.countNumbered) {
            val absoluteIndex = collector.countNamed + i
            groups.add(
                CaptureGroupInfo(
                    name = null,
                    index = absoluteIndex,
                    propertyName = "group$absoluteIndex",
                )
            )
        }

        groups.sortBy { it.index }

        // 4. Generate Kotlin code
        val code = generateCode(className, groups, result)
        return GeneratedDataClass(className, code, groups)
    }

    private fun generateCode(
        className: String,
        groups: List<CaptureGroupInfo>,
        compiledRegex: String,
    ): String {
        val sb = StringBuilder()

        // Data class
        sb.appendLine("data class $className(")
        groups.forEachIndexed { i, group ->
            val comma = if (i < groups.size - 1) "," else ""
            val comment = if (group.name != null) {
                " // group ${group.index}: \"${group.name}\""
            } else {
                " // group ${group.index}"
            }
            sb.appendLine("    val ${group.propertyName}: String$comma$comment")
        }
        sb.appendLine(")")
        sb.appendLine()

        // Extension function
        sb.appendLine("fun Regex.extract$className(input: String): $className? {")
        sb.appendLine("    val match = find(input) ?: return null")
        sb.appendLine("    return $className(")
        groups.forEachIndexed { i, group ->
            val comma = if (i < groups.size - 1) "," else ""
            sb.appendLine("        ${group.propertyName} = match.groupValues[${group.index}]$comma")
        }
        sb.appendLine("    )")
        sb.appendLine("}")

        return sb.toString()
    }

    private fun toCamelCase(name: String): String {
        return name.split('_', '-').mapIndexed { i, part ->
            if (i == 0) part.lowercase()
            else part.replaceFirstChar { it.uppercase() }
        }.joinToString("")
    }
}
