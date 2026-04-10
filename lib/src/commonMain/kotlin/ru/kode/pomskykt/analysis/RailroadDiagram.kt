package ru.kode.pomskykt.analysis

import ru.kode.pomskykt.regex.Regex
import ru.kode.pomskykt.regex.RegexCharSetItem
import ru.kode.pomskykt.regex.RegexGroupKind
import ru.kode.pomskykt.regex.RegexReference
import ru.kode.pomskykt.regex.RegexShorthand
import ru.kode.pomskykt.syntax.exprs.BoundaryKind
import ru.kode.pomskykt.syntax.exprs.LookaroundKind

/**
 * ASCII railroad diagram renderer for the [Regex] IR tree.
 *
 * Walks the IR and produces a text-based visualization where each node
 * becomes a labeled block connected by track lines (`──`). Alternations
 * are rendered as vertical branches with box-drawing characters.
 */
object RailroadDiagram {

    /**
     * Render the given [regex] IR tree as an ASCII railroad diagram string.
     */
    fun render(regex: Regex): String {
        val block = toBlock(regex)
        return block.lines.joinToString("\n")
    }

    // ── Block model ──────────────────────────────────────────────────

    private data class Block(
        val lines: List<String>,
        val entryRow: Int,
        val width: Int,
    )

    // ── IR → Block dispatch ──────────────────────────────────────────

    private fun toBlock(regex: Regex): Block = when (regex) {
        is Regex.Literal -> label("\"${regex.content}\"")
        is Regex.Unescaped -> label("raw:${regex.content}")
        is Regex.Dot -> label(".")
        is Regex.Grapheme -> label("Grapheme")
        is Regex.Recursion -> label("(?R)")

        is Regex.Bound -> label(boundLabel(regex.kind))

        is Regex.CharSet -> {
            val prefix = if (regex.set.negative) "^" else ""
            val items = regex.set.items.joinToString(", ") { charSetItemLabel(it) }
            label("[$prefix$items]")
        }

        is Regex.CompoundCharSet -> label("[compound]")

        is Regex.Sequence -> {
            val blocks = regex.parts.map { toBlock(it) }
            if (blocks.isEmpty()) label("(empty)") else horizontal(blocks)
        }

        is Regex.Alt -> {
            val blocks = regex.alternation.alternatives.map { toBlock(it) }
            if (blocks.isEmpty()) label("(empty)") else vertical(blocks)
        }

        is Regex.Rep -> {
            val inner = toBlock(regex.repetition.inner)
            val q = quantifier(regex.repetition.lower, regex.repetition.upper)
            val suffix = if (regex.repetition.greedy) q else "$q lazy"
            appendSuffix(inner, suffix)
        }

        is Regex.Group -> {
            val inner = groupInner(regex.group.parts)
            when (regex.group.kind) {
                is RegexGroupKind.NonCapturing -> inner
                is RegexGroupKind.Numbered -> wrap("(:${regex.group.kind.index}", inner, ")")
                is RegexGroupKind.Named -> wrap("(:${regex.group.kind.name}", inner, ")")
                is RegexGroupKind.Atomic -> wrap("(atomic", inner, ")")
            }
        }

        is Regex.Look -> {
            val inner = toBlock(regex.lookaround.inner)
            val prefix = when (regex.lookaround.kind) {
                LookaroundKind.Ahead -> "(?="
                LookaroundKind.AheadNegative -> "(?!"
                LookaroundKind.Behind -> "(?<="
                LookaroundKind.BehindNegative -> "(?<!"
            }
            wrap(prefix, inner, ")")
        }

        is Regex.Ref -> when (val ref = regex.reference) {
            is RegexReference.Named -> label("\\k<${ref.name}>")
            is RegexReference.Numbered -> label("\\${ref.index}")
        }

        is Regex.ModeGroup -> toBlock(regex.inner)
    }

    // ── Helpers ──────────────────────────────────────────────────────

    private fun boundLabel(kind: BoundaryKind): String = when (kind) {
        BoundaryKind.Start -> "START"
        BoundaryKind.End -> "END"
        BoundaryKind.Word -> "\\b"
        BoundaryKind.NotWord -> "\\B"
        BoundaryKind.WordStart -> "\\b<"
        BoundaryKind.WordEnd -> "\\b>"
    }

    private fun charSetItemLabel(item: RegexCharSetItem): String = when (item) {
        is RegexCharSetItem.Char -> item.char.toString()
        is RegexCharSetItem.Range -> "${item.first}-${item.last}"
        is RegexCharSetItem.Shorthand -> item.shorthand.str
        is RegexCharSetItem.Property -> {
            val name = when (val p = item.property) {
                is ru.kode.pomskykt.regex.RegexProperty.CategoryProp -> p.category.name
                is ru.kode.pomskykt.regex.RegexProperty.ScriptProp -> p.script.name
                is ru.kode.pomskykt.regex.RegexProperty.BlockProp -> p.block.name
                is ru.kode.pomskykt.regex.RegexProperty.OtherProp -> p.property.name
            }
            if (item.negative) "!$name" else name
        }
        is RegexCharSetItem.Literal -> item.content
        is RegexCharSetItem.CodePoint -> "U+${item.codePoint.toString(16).uppercase().padStart(4, '0')}"
    }

    private fun quantifier(lower: Int, upper: Int?): String = when {
        lower == 0 && upper == null -> "*"
        lower == 1 && upper == null -> "+"
        lower == 0 && upper == 1 -> "?"
        upper == null -> "{$lower,}"
        lower == upper -> "{$lower}"
        else -> "{$lower,$upper}"
    }

    // ── Block constructors ───────────────────────────────────────────

    /**
     * A simple single-line labeled block: `-- label --`
     */
    private fun label(text: String): Block {
        val line = "\u2500\u2500 $text \u2500\u2500"
        return Block(lines = listOf(line), entryRow = 0, width = line.length)
    }

    /**
     * Chain blocks horizontally, connecting entry rows with track lines.
     */
    private fun horizontal(blocks: List<Block>): Block {
        if (blocks.size == 1) return blocks[0]

        // Find the maximum height and unify entry rows by aligning them.
        val maxEntry = blocks.maxOf { it.entryRow }
        val maxBelow = blocks.maxOf { it.lines.size - 1 - it.entryRow }
        val totalHeight = maxEntry + maxBelow + 1

        // Pad each block to totalHeight, centered on the aligned entry row.
        val padded = blocks.map { b ->
            val topPad = maxEntry - b.entryRow
            val bottomPad = totalHeight - topPad - b.lines.size
            val paddedLines = List(topPad) { " ".repeat(b.width) } +
                b.lines +
                List(bottomPad) { " ".repeat(b.width) }
            Block(paddedLines, maxEntry, b.width)
        }

        // Join horizontally with connector on entry row.
        val connector = "\u2500\u2500"
        val resultLines = (0 until totalHeight).map { row ->
            padded.mapIndexed { idx, b ->
                val segment = b.lines[row]
                if (idx < padded.size - 1) {
                    if (row == maxEntry) "$segment$connector" else "$segment  "
                } else {
                    segment
                }
            }.joinToString("")
        }

        val totalWidth = padded.sumOf { it.width } + (padded.size - 1) * connector.length
        return Block(resultLines, maxEntry, totalWidth)
    }

    /**
     * Stack blocks vertically with branch lines for alternation.
     */
    private fun vertical(blocks: List<Block>): Block {
        if (blocks.size == 1) return blocks[0]

        val maxContentWidth = blocks.maxOf { it.width }

        val resultLines = mutableListOf<String>()
        var entryRow = 0

        blocks.forEachIndexed { idx, block ->
            val isFirst = idx == 0
            val isLast = idx == blocks.size - 1

            // Determine branch characters for this alternative.
            val leftChar = when {
                isFirst -> "\u250c"  // ┌
                isLast -> "\u2514"   // └
                else -> "\u251c"     // ├
            }
            val rightChar = when {
                isFirst -> "\u2510"  // ┐
                isLast -> "\u2518"   // ┘
                else -> "\u2524"     // ┤
            }
            val verticalChar = "\u2502" // │

            block.lines.forEachIndexed { lineIdx, line ->
                val isEntryLine = lineIdx == block.entryRow
                val paddedLine = line + " ".repeat(maxContentWidth - block.width)

                val left = if (isEntryLine) "$leftChar\u2500\u2500 " else "$verticalChar   "
                val right = if (isEntryLine) " $rightChar" else " $verticalChar"

                resultLines.add("$left$paddedLine$right")
            }

            if (isFirst) {
                entryRow = block.entryRow
            }

            // Add vertical connector between alternatives (except after last).
            if (!isLast) {
                val connectorLeft = verticalChar
                val connectorRight = verticalChar
                resultLines.add("$connectorLeft   ${" ".repeat(maxContentWidth)} $connectorRight")
            }
        }

        val totalWidth = maxContentWidth + 6 // left prefix (4 chars) + right suffix (2 chars)
        return Block(resultLines, entryRow, totalWidth)
    }

    /**
     * Wrap inner block with a prefix and suffix on the entry line.
     */
    private fun wrap(prefix: String, inner: Block, suffix: String): Block {
        val resultLines = inner.lines.mapIndexed { idx, line ->
            if (idx == inner.entryRow) {
                "\u2500\u2500 $prefix $line $suffix \u2500\u2500"
            } else {
                "   ${" ".repeat(prefix.length)} $line ${" ".repeat(suffix.length)}   "
            }
        }
        val width = resultLines.maxOf { it.length }
        val paddedLines = resultLines.map { it.padEnd(width) }
        return Block(paddedLines, inner.entryRow, width)
    }

    /**
     * Append a quantifier suffix to a block's entry line.
     */
    private fun appendSuffix(block: Block, suffix: String): Block {
        val resultLines = block.lines.mapIndexed { idx, line ->
            if (idx == block.entryRow) "$line $suffix" else "$line${" ".repeat(suffix.length + 1)}"
        }
        val width = resultLines.maxOf { it.length }
        val paddedLines = resultLines.map { it.padEnd(width) }
        return Block(paddedLines, block.entryRow, width)
    }

    /**
     * Convert a list of group parts to a single block (sequence or single element).
     */
    private fun groupInner(parts: List<Regex>): Block = when {
        parts.isEmpty() -> label("(empty)")
        parts.size == 1 -> toBlock(parts[0])
        else -> horizontal(parts.map { toBlock(it) })
    }
}
