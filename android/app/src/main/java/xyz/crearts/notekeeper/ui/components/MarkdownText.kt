package xyz.crearts.notekeeper.ui.components

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
    Text(
        text = parseAllBlocks(markdown, primaryColor, onSurfaceVariantColor),
        style = style,
        modifier = modifier.padding(vertical = 4.dp)
    )
}

private fun parseAllBlocks(
    markdown: String,
    primaryColor: Color,
    onSurfaceVariantColor: Color
): AnnotatedString = buildAnnotatedString {
    val blocks = parseBlocks(markdown)
    blocks.forEachIndexed { blockIndex, block ->
        when (block) {
            is Block.TextBlock -> {
                append(parseMarkdownText(block.lines, primaryColor, onSurfaceVariantColor))
            }
            is Block.TableBlock -> {
                appendTable(block.rows)
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
            // Header row
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
}

private fun parseBlocks(input: String): List<Block> {
    val lines = input.lines()
    val blocks = mutableListOf<Block>()
    var currentBlock = mutableListOf<String>()

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
        if (line.isBlank()) {
            flushTextBlock()
        } else {
            currentBlock.add(line)
        }
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

private fun parseMarkdownText(
    lines: List<String>,
    primaryColor: Color,
    onSurfaceVariantColor: Color
): AnnotatedString = buildAnnotatedString {
    lines.forEachIndexed { index, rawLine ->
        val line = rawLine.trimEnd()
        when {
            line.startsWith("### ") -> {
                withStyle(
                    SpanStyle(
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = primaryColor
                    )
                ) {
                    appendInlineMarkdown(line.removePrefix("### "), primaryColor)
                }
            }
            line.startsWith("## ") -> {
                withStyle(
                    SpanStyle(
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = primaryColor
                    )
                ) {
                    appendInlineMarkdown(line.removePrefix("## "), primaryColor)
                }
            }
            line.startsWith("# ") -> {
                withStyle(
                    SpanStyle(
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = primaryColor
                    )
                ) {
                    appendInlineMarkdown(line.removePrefix("# "), primaryColor)
                }
            }
            line.startsWith("> ") -> {
                withStyle(
                    SpanStyle(
                        fontStyle = FontStyle.Italic,
                        color = onSurfaceVariantColor
                    )
                ) {
                    appendInlineMarkdown(line.removePrefix("> "), primaryColor)
                }
            }
            line.startsWith("- ") || line.startsWith("* ") -> {
                append("\u2022 ")
                appendInlineMarkdown(line.substring(2), primaryColor)
            }
            else -> {
                appendInlineMarkdown(line, primaryColor)
            }
        }
        if (index < lines.lastIndex) {
            append("\n")
        }
    }
}

private fun AnnotatedString.Builder.appendInlineMarkdown(text: String, primaryColor: Color) {
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
                withStyle(
                    SpanStyle(
                        fontWeight = FontWeight.Bold,
                        fontStyle = FontStyle.Italic
                    )
                ) {
                    appendInlineMarkdown(match.content, primaryColor)
                }
            }
            InlineType.BOLD -> {
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                    appendInlineMarkdown(match.content, primaryColor)
                }
            }
            InlineType.ITALIC -> {
                withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                    appendInlineMarkdown(match.content, primaryColor)
                }
            }
            InlineType.CODE -> {
                withStyle(
                    SpanStyle(
                        fontFamily = FontFamily.Monospace,
                        background = Color.LightGray.copy(alpha = 0.4f)
                    )
                ) {
                    append(match.content)
                }
            }
            InlineType.LINK -> {
                withStyle(
                    SpanStyle(
                        color = primaryColor,
                        textDecoration = TextDecoration.Underline
                    )
                ) {
                    append(match.content)
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
    val linkRegex = Regex("""\[([^\]]+)\]\(([^)]+)\)""")
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
    BOLD_ITALIC, BOLD, ITALIC, CODE, LINK
}
