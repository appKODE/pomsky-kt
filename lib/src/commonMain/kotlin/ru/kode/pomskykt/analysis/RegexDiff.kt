package ru.kode.pomskykt.analysis

import ru.kode.pomskykt.regex.Regex
import ru.kode.pomskykt.regex.RegexCharSetItem
import ru.kode.pomskykt.regex.RegexGroupKind
import ru.kode.pomskykt.syntax.exprs.BoundaryKind
import ru.kode.pomskykt.syntax.exprs.LookaroundKind

data class DiffReport(
    val onlyInA: List<DiffItem>,
    val onlyInB: List<DiffItem>,
    val common: List<DiffItem>,
    val structuralDiffs: List<String>,
)

data class DiffItem(
    val description: String,
    val category: Category,
) {
    enum class Category { CHAR_SET, QUANTIFIER, ANCHOR, GROUP, LOOKAROUND, OTHER }
}

object RegexDiff {

    fun diff(a: Regex, b: Regex): DiffReport {
        val featuresA = FeatureSet()
        val featuresB = FeatureSet()
        collectFeatures(a, featuresA)
        collectFeatures(b, featuresB)

        val itemsA = featuresA.toDiffItems()
        val itemsB = featuresB.toDiffItems()

        val mapA = itemsA.groupBy { it.description to it.category }.mapValues { it.value.size }.toMutableMap()
        val mapB = itemsB.groupBy { it.description to it.category }.mapValues { it.value.size }.toMutableMap()

        val allKeys = (mapA.keys + mapB.keys).toSet()

        val onlyInA = mutableListOf<DiffItem>()
        val onlyInB = mutableListOf<DiffItem>()
        val common = mutableListOf<DiffItem>()

        for (key in allKeys) {
            val countA = mapA[key] ?: 0
            val countB = mapB[key] ?: 0
            val (desc, cat) = key
            val minCount = minOf(countA, countB)
            repeat(minCount) {
                common.add(DiffItem(desc, cat))
            }
            repeat(countA - minCount) {
                onlyInA.add(DiffItem(desc, cat))
            }
            repeat(countB - minCount) {
                onlyInB.add(DiffItem(desc, cat))
            }
        }

        val structuralDiffs = compareStructure(a, b)

        return DiffReport(
            onlyInA = onlyInA,
            onlyInB = onlyInB,
            common = common,
            structuralDiffs = structuralDiffs,
        )
    }

    private data class FeatureSet(
        val charSets: MutableList<String> = mutableListOf(),
        val anchors: MutableList<String> = mutableListOf(),
        val quantifiers: MutableList<String> = mutableListOf(),
        val groups: MutableList<String> = mutableListOf(),
        val lookarounds: MutableList<String> = mutableListOf(),
        val literals: MutableList<String> = mutableListOf(),
        val other: MutableList<String> = mutableListOf(),
    ) {
        fun toDiffItems(): List<DiffItem> {
            val items = mutableListOf<DiffItem>()
            charSets.mapTo(items) { DiffItem(it, DiffItem.Category.CHAR_SET) }
            anchors.mapTo(items) { DiffItem(it, DiffItem.Category.ANCHOR) }
            quantifiers.mapTo(items) { DiffItem(it, DiffItem.Category.QUANTIFIER) }
            groups.mapTo(items) { DiffItem(it, DiffItem.Category.GROUP) }
            lookarounds.mapTo(items) { DiffItem(it, DiffItem.Category.LOOKAROUND) }
            literals.mapTo(items) { DiffItem("literal: $it", DiffItem.Category.OTHER) }
            other.mapTo(items) { DiffItem(it, DiffItem.Category.OTHER) }
            return items
        }
    }

    private fun collectFeatures(regex: Regex, features: FeatureSet) {
        when (regex) {
            is Regex.Literal -> features.literals.add(regex.content)
            is Regex.CharSet -> {
                for (item in regex.set.items) {
                    features.charSets.add(describeCharSetItem(item, regex.set.negative))
                }
            }
            is Regex.CompoundCharSet -> {
                features.charSets.add(
                    "compound character set" + if (regex.set.negative) " (negated)" else ""
                )
            }
            is Regex.Sequence -> regex.parts.forEach { collectFeatures(it, features) }
            is Regex.Alt -> regex.alternation.alternatives.forEach { collectFeatures(it, features) }
            is Regex.Rep -> {
                val rep = regex.repetition
                val desc = describeQuantifier(rep.lower, rep.upper, rep.greedy)
                features.quantifiers.add(desc)
                collectFeatures(rep.inner, features)
            }
            is Regex.Bound -> features.anchors.add(describeBoundary(regex.kind))
            is Regex.Group -> {
                val kind = regex.group.kind
                val desc = when (kind) {
                    is RegexGroupKind.Numbered -> "capturing group #${kind.index}"
                    is RegexGroupKind.Named -> "named group '${kind.name}'"
                    is RegexGroupKind.NonCapturing -> "non-capturing group"
                    is RegexGroupKind.Atomic -> "atomic group"
                }
                features.groups.add(desc)
                regex.group.parts.forEach { collectFeatures(it, features) }
            }
            is Regex.Look -> {
                features.lookarounds.add(describeLookaround(regex.lookaround.kind))
                collectFeatures(regex.lookaround.inner, features)
            }
            is Regex.ModeGroup -> collectFeatures(regex.inner, features)
            is Regex.Recursion -> features.other.add("recursion")
            is Regex.Dot -> features.charSets.add("any character (.)")
            is Regex.Grapheme -> features.charSets.add("grapheme cluster")
            is Regex.Ref -> {
                val desc = when (val ref = regex.reference) {
                    is ru.kode.pomskykt.regex.RegexReference.Named -> "backreference to '${ref.name}'"
                    is ru.kode.pomskykt.regex.RegexReference.Numbered -> "backreference to #${ref.index}"
                }
                features.other.add(desc)
            }
            is Regex.Unescaped -> features.other.add("raw regex: ${regex.content}")
        }
    }

    private fun describeCharSetItem(item: RegexCharSetItem, negative: Boolean): String {
        val prefix = if (negative) "negated " else ""
        return prefix + when (item) {
            is RegexCharSetItem.Char -> "char '${item.char}'"
            is RegexCharSetItem.Range -> "range '${item.first}'-'${item.last}'"
            is RegexCharSetItem.Shorthand -> "shorthand ${item.shorthand.name}"
            is RegexCharSetItem.Property -> {
                val negStr = if (item.negative) "negated " else ""
                "${negStr}property ${item.property}"
            }
            is RegexCharSetItem.Literal -> "literal '${item.content}'"
            is RegexCharSetItem.CodePoint -> "code point U+${item.codePoint.toString(16).uppercase().padStart(4, '0')}"
        }
    }

    private fun describeBoundary(kind: BoundaryKind): String {
        return when (kind) {
            BoundaryKind.Start -> "start of string"
            BoundaryKind.End -> "end of string"
            BoundaryKind.Word -> "word boundary"
            BoundaryKind.NotWord -> "not word boundary"
            BoundaryKind.WordStart -> "start of word"
            BoundaryKind.WordEnd -> "end of word"
        }
    }

    private fun describeLookaround(kind: LookaroundKind): String {
        return when (kind) {
            LookaroundKind.Ahead -> "positive lookahead"
            LookaroundKind.Behind -> "positive lookbehind"
            LookaroundKind.AheadNegative -> "negative lookahead"
            LookaroundKind.BehindNegative -> "negative lookbehind"
        }
    }

    private fun describeQuantifier(lower: Int, upper: Int?, greedy: Boolean): String {
        val kind = when {
            lower == 0 && upper == null -> "zero or more"
            lower == 1 && upper == null -> "one or more"
            lower == 0 && upper == 1 -> "optional"
            lower == upper -> "exactly $lower"
            upper == null -> "$lower or more"
            else -> "$lower to $upper"
        }
        val mode = if (greedy) "(greedy)" else "(lazy)"
        return "$kind $mode"
    }

    private fun compareStructure(a: Regex, b: Regex): List<String> {
        val diffs = mutableListOf<String>()
        compareStructureImpl(a, b, "root", diffs)
        return diffs
    }

    private fun compareStructureImpl(a: Regex, b: Regex, path: String, diffs: MutableList<String>) {
        if (a::class != b::class) {
            diffs.add("$path: node type differs — ${nodeTypeName(a)} vs ${nodeTypeName(b)}")
            return
        }
        when (a) {
            is Regex.Sequence -> {
                val bSeq = b as Regex.Sequence
                if (a.parts.size != bSeq.parts.size) {
                    diffs.add("$path: sequence length differs — ${a.parts.size} vs ${bSeq.parts.size}")
                }
                val minLen = minOf(a.parts.size, bSeq.parts.size)
                for (i in 0 until minLen) {
                    compareStructureImpl(a.parts[i], bSeq.parts[i], "$path[$i]", diffs)
                }
            }
            is Regex.Alt -> {
                val bAlt = b as Regex.Alt
                if (a.alternation.alternatives.size != bAlt.alternation.alternatives.size) {
                    diffs.add(
                        "$path: alternation branch count differs — " +
                            "${a.alternation.alternatives.size} vs ${bAlt.alternation.alternatives.size}"
                    )
                }
                val minLen = minOf(a.alternation.alternatives.size, bAlt.alternation.alternatives.size)
                for (i in 0 until minLen) {
                    compareStructureImpl(
                        a.alternation.alternatives[i],
                        bAlt.alternation.alternatives[i],
                        "$path|$i",
                        diffs,
                    )
                }
            }
            is Regex.Group -> {
                val bGroup = b as Regex.Group
                if (a.group.kind != bGroup.group.kind) {
                    diffs.add("$path: group kind differs — ${a.group.kind} vs ${bGroup.group.kind}")
                }
                if (a.group.parts.size != bGroup.group.parts.size) {
                    diffs.add("$path: group content length differs — ${a.group.parts.size} vs ${bGroup.group.parts.size}")
                }
                val minLen = minOf(a.group.parts.size, bGroup.group.parts.size)
                for (i in 0 until minLen) {
                    compareStructureImpl(a.group.parts[i], bGroup.group.parts[i], "$path:$i", diffs)
                }
            }
            is Regex.Rep -> {
                val bRep = b as Regex.Rep
                if (a.repetition.lower != bRep.repetition.lower || a.repetition.upper != bRep.repetition.upper) {
                    diffs.add(
                        "$path: repetition bounds differ — " +
                            "{${a.repetition.lower},${a.repetition.upper ?: ""}} vs " +
                            "{${bRep.repetition.lower},${bRep.repetition.upper ?: ""}}"
                    )
                }
                if (a.repetition.greedy != bRep.repetition.greedy) {
                    diffs.add("$path: greediness differs — ${if (a.repetition.greedy) "greedy" else "lazy"} vs ${if (bRep.repetition.greedy) "greedy" else "lazy"}")
                }
                compareStructureImpl(a.repetition.inner, bRep.repetition.inner, "$path/rep", diffs)
            }
            is Regex.Look -> {
                val bLook = b as Regex.Look
                if (a.lookaround.kind != bLook.lookaround.kind) {
                    diffs.add("$path: lookaround kind differs — ${a.lookaround.kind} vs ${bLook.lookaround.kind}")
                }
                compareStructureImpl(a.lookaround.inner, bLook.lookaround.inner, "$path/look", diffs)
            }
            is Regex.ModeGroup -> {
                val bMode = b as Regex.ModeGroup
                if (a.flags != bMode.flags) {
                    diffs.add("$path: mode flags differ — ${a.flags} vs ${bMode.flags}")
                }
                compareStructureImpl(a.inner, bMode.inner, "$path/mode", diffs)
            }
            is Regex.Literal -> {
                val bLit = b as Regex.Literal
                if (a.content != bLit.content) {
                    diffs.add("$path: literal differs — \"${a.content}\" vs \"${bLit.content}\"")
                }
            }
            is Regex.CharSet -> {
                val bSet = b as Regex.CharSet
                if (a.set != bSet.set) {
                    diffs.add("$path: character set differs")
                }
            }
            is Regex.Bound -> {
                val bBound = b as Regex.Bound
                if (a.kind != bBound.kind) {
                    diffs.add("$path: boundary kind differs — ${a.kind} vs ${bBound.kind}")
                }
            }
            is Regex.Ref -> {
                val bRef = b as Regex.Ref
                if (a.reference != bRef.reference) {
                    diffs.add("$path: backreference differs — ${a.reference} vs ${bRef.reference}")
                }
            }
            is Regex.Unescaped -> {
                val bUn = b as Regex.Unescaped
                if (a.content != bUn.content) {
                    diffs.add("$path: raw regex differs — \"${a.content}\" vs \"${bUn.content}\"")
                }
            }
            is Regex.CompoundCharSet -> {
                val bComp = b as Regex.CompoundCharSet
                if (a.set != bComp.set) {
                    diffs.add("$path: compound character set differs")
                }
            }
            // Leaf singletons: Dot, Grapheme, Recursion — identical if same type
            is Regex.Dot -> {}
            is Regex.Grapheme -> {}
            is Regex.Recursion -> {}
        }
    }

    private fun nodeTypeName(regex: Regex): String {
        return when (regex) {
            is Regex.Literal -> "Literal"
            is Regex.Unescaped -> "Unescaped"
            is Regex.CharSet -> "CharSet"
            is Regex.CompoundCharSet -> "CompoundCharSet"
            is Regex.Grapheme -> "Grapheme"
            is Regex.Dot -> "Dot"
            is Regex.Group -> "Group"
            is Regex.Sequence -> "Sequence"
            is Regex.Alt -> "Alt"
            is Regex.Rep -> "Rep"
            is Regex.Bound -> "Bound"
            is Regex.Look -> "Look"
            is Regex.Ref -> "Ref"
            is Regex.Recursion -> "Recursion"
            is Regex.ModeGroup -> "ModeGroup"
        }
    }
}
