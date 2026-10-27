package dev.cogitor.notekeeper.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun MarkdownText(
    markdown: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodyLarge
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val onSurfaceVariantColor = MaterialTheme.colorScheme.onSurfaceVariant
    val codeBg = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
    Text(
        text = parseAllBlocks(markdown, primaryColor, onSurfaceVariantColor, codeBg),
        style = style,
        modifier = modifier.padding(vertical = 4.dp)
    )
}

private fun parseAllBlocks(
    markdown: String,
    primaryColor: Color,
    onSurfaceVariantColor: Color,
    codeBg: Color
): AnnotatedString = buildAnnotatedString {
    val blocks = parseBlocks(markdown)
    blocks.forEachIndexed { blockIndex, block ->
        when (block) {
            is Block.TextBlock -> {
                append(parseMarkdownText(block.lines, primaryColor, onSurfaceVariantColor, codeBg))
            }

            is Block.TableBlock -> {
                appendTable(block.rows)
            }

            is Block.CodeBlock -> {
                withStyle(
                    SpanStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        background = codeBg,
                        color = onSurfaceVariantColor
                    )
                ) {
                    append(block.text.ifBlank { " " })
                }
            }
        }
        if (blockIndex < blocks.size - 1) {
            append("\n")
        }
    }
}

private fun AnnotatedString.Builder.appendTable(rows: List<List<String>>) {
    if (rows.isEmpty()) return
    val colCount = rows.maxOf { it.size }
    val colWidths = (0 until colCount).map { colIndex ->
        rows.maxOf { it.getOrNull(colIndex)?.length ?: 0 }
    }
    rows.forEachIndexed { rowIndex, cells ->
        if (rowIndex == 0) {
            withStyle(
                SpanStyle(
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = Color.Unspecified
                )
            ) {
                append(
                    cells.mapIndexed { colIndex, cell ->
                        cell.padEnd(colWidths[colIndex])
                    }.joinToString(" | ")
                )
            }
        } else {
            withStyle(SpanStyle(fontFamily = FontFamily.Monospace)) {
                append(
                    cells.mapIndexed { colIndex, cell ->
                        cell.padEnd(colWidths[colIndex])
                    }.joinToString(" | ")
                )
            }
        }
        if (rowIndex < rows.lastIndex) {
            append("\n")
        }
    }
}

private sealed class Block {
    data class TextBlock(val lines: List<String>) : Block()
    data class TableBlock(val rows: List<List<String>>) : Block()
    data class CodeBlock(val text: String) : Block()
}

private fun parseBlocks(input: String): List<Block> {
    val lines = input.lines()
    val blocks = mutableListOf<Block>()
    var currentBlock = mutableListOf<String>()
    var fence: MutableList<String>? = null

    fun flushTextBlock() {
        if (currentBlock.isNotEmpty()) {
            val table = tryParseTable(currentBlock)
            if (table != null) {
                blocks.add(Block.TableBlock(table))
            } else {
                blocks.add(Block.TextBlock(currentBlock.toList()))
            }
            currentBlock.clear()
        }
    }

    for (line in lines) {
        val trimmed = line.trimStart()
        if (fence != null) {
            if (trimmed.startsWith("```")) {
                blocks.add(Block.CodeBlock(fence.joinToString("\n")))
                fence = null
            } else {
                fence.add(line)
            }
            continue
        }
        if (trimmed.startsWith("```")) {
            flushTextBlock()
            fence = mutableListOf()
            continue
        }
        if (line.isBlank()) {
            flushTextBlock()
        } else {
            currentBlock.add(line)
        }
    }
    if (fence != null) {
        blocks.add(Block.CodeBlock(fence.joinToString("\n")))
    }
    flushTextBlock()
    return blocks
}

private fun tryParseTable(lines: List<String>): List<List<String>>? {
    if (lines.size < 2) return null
    if (!lines.all { it.trim().startsWith("|") }) return null
    val separator = lines[1].trim()
    if (!separator.replace("|", "").all { it == '-' || it == ':' || it.isWhitespace() }) return null
    return lines.filterIndexed { index, _ -> index != 1 }.map { line ->
        line.trim().removePrefix("|").removeSuffix("|").split("|").map { it.trim() }
    }
}

private val numberedList = Regex("""^\d+\.\s+""")
private val taskList = Regex("""^[-*]\s+\[([ xX])]\s+(.*)""")

private fun parseMarkdownText(
    lines: List<String>,
    primaryColor: Color,
    onSurfaceVariantColor: Color,
    codeBg: Color
): AnnotatedString = buildAnnotatedString {
    lines.forEachIndexed { index, rawLine ->
        val line = rawLine.trimEnd()
        val trimmed = line.trimStart()
        when {
            trimmed == "---" || trimmed == "***" || trimmed == "___" -> {
                append("────────")
            }

            trimmed.startsWith("#### ") -> {
                withStyle(SpanStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold, color = primaryColor)) {
                    appendInlineMarkdown(trimmed.removePrefix("#### "), primaryColor, codeBg)
                }
            }

            trimmed.startsWith("### ") -> {
                withStyle(SpanStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold, color = primaryColor)) {
                    appendInlineMarkdown(trimmed.removePrefix("### "), primaryColor, codeBg)
                }
            }

            trimmed.startsWith("## ") -> {
                withStyle(SpanStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold, color = primaryColor)) {
                    appendInlineMarkdown(trimmed.removePrefix("## "), primaryColor, codeBg)
                }
            }

            trimmed.startsWith("# ") -> {
                withStyle(SpanStyle(fontSize = 28.sp, fontWeight = FontWeight.Bold, color = primaryColor)) {
                    appendInlineMarkdown(trimmed.removePrefix("# "), primaryColor, codeBg)
                }
            }

            trimmed.startsWith("> ") -> {
                withStyle(SpanStyle(fontStyle = FontStyle.Italic, color = onSurfaceVariantColor)) {
                    appendInlineMarkdown(trimmed.removePrefix("> "), primaryColor, codeBg)
                }
            }

            taskList.matches(trimmed) -> {
                val match = taskList.matchEntire(trimmed)!!
                val checked = match.groupValues[1].equals("x", ignoreCase = true)
                append(if (checked) "☑  " else "☐  ")
                appendInlineMarkdown(match.groupValues[2], primaryColor, codeBg)
            }

            trimmed.startsWith("- ") || trimmed.startsWith("* ") -> {
                append("• ")
                appendInlineMarkdown(trimmed.substring(2), primaryColor, codeBg)
            }

            numberedList.containsMatchIn(trimmed) -> {
                val prefix = numberedList.find(trimmed)!!.value
                append(prefix)
                appendInlineMarkdown(trimmed.substring(prefix.length), primaryColor, codeBg)
            }

            else -> {
                appendInlineMarkdown(line, primaryColor, codeBg)
            }
        }
        if (index < lines.lastIndex) {
            append("\n")
        }
    }
}

private fun AnnotatedString.Builder.appendInlineMarkdown(
    text: String,
    primaryColor: Color,
    codeBg: Color
) {
    var remaining = text
    while (remaining.isNotEmpty()) {
        val match = findNextInline(remaining)
        if (match == null) {
            append(remaining)
            break
        }
        append(remaining.substring(0, match.startIndex))
        when (match.type) {
            InlineType.BOLD_ITALIC -> {
                withStyle(SpanStyle(fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic)) {
                    appendInlineMarkdown(match.content, primaryColor, codeBg)
                }
            }

            InlineType.BOLD -> {
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                    appendInlineMarkdown(match.content, primaryColor, codeBg)
                }
            }

            InlineType.ITALIC -> {
                withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                    appendInlineMarkdown(match.content, primaryColor, codeBg)
                }
            }

            InlineType.STRIKE -> {
                withStyle(SpanStyle(textDecoration = TextDecoration.LineThrough)) {
                    appendInlineMarkdown(match.content, primaryColor, codeBg)
                }
            }

            InlineType.CODE -> {
                withStyle(
                    SpanStyle(
                        fontFamily = FontFamily.Monospace,
                        background = codeBg
                    )
                ) {
                    append(match.content)
                }
            }

            InlineType.LINK, InlineType.IMAGE -> {
                withStyle(
                    SpanStyle(
                        color = primaryColor,
                        textDecoration = TextDecoration.Underline
                    )
                ) {
                    append(if (match.type == InlineType.IMAGE) "🖼 ${match.content}" else match.content)
                }
            }
        }
        remaining = remaining.substring(match.endIndex)
    }
}

private fun findNextInline(text: String): InlineMatch? {
    val patterns = listOf(
        Triple("***", InlineType.BOLD_ITALIC, "***"),
        Triple("**", InlineType.BOLD, "**"),
        Triple("~~", InlineType.STRIKE, "~~"),
        Triple("*", InlineType.ITALIC, "*"),
        Triple("_", InlineType.ITALIC, "_"),
        Triple("`", InlineType.CODE, "`")
    )
    var best: InlineMatch? = null
    for ((open, type, close) in patterns) {
        val start = text.indexOf(open)
        if (start == -1) continue
        val contentStart = start + open.length
        val end = text.indexOf(close, contentStart)
        if (end == -1) continue
        if (best == null || start < best.startIndex) {
            best = InlineMatch(
                startIndex = start,
                endIndex = end + close.length,
                type = type,
                content = text.substring(contentStart, end)
            )
        }
    }
    val imageRegex = Regex("""!\[([^\]]*)]\(([^)]+)\)""")
    val imageMatch = imageRegex.find(text)
    if (imageMatch != null) {
        val imageStart = imageMatch.range.first
        if (best == null || imageStart < best.startIndex) {
            best = InlineMatch(
                startIndex = imageStart,
                endIndex = imageMatch.range.last + 1,
                type = InlineType.IMAGE,
                content = imageMatch.groupValues[1].ifBlank { "image" },
                url = imageMatch.groupValues[2]
            )
        }
    }
    val linkRegex = Regex("""\[([^\]]+)]\(([^)]+)\)""")
    val linkMatch = linkRegex.find(text)
    if (linkMatch != null) {
        val linkStart = linkMatch.range.first
        if (best == null || linkStart < best.startIndex) {
            best = InlineMatch(
                startIndex = linkStart,
                endIndex = linkMatch.range.last + 1,
                type = InlineType.LINK,
                content = linkMatch.groupValues[1],
                url = linkMatch.groupValues[2]
            )
        }
    }
    return best
}

private data class InlineMatch(
    val startIndex: Int,
    val endIndex: Int,
    val type: InlineType,
    val content: String,
    val url: String? = null
)

private enum class InlineType {
    BOLD_ITALIC, BOLD, ITALIC, STRIKE, CODE, LINK, IMAGE
}
