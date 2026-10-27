package dev.cogitor.notekeeper.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.FormatListBulleted
import androidx.compose.material.icons.outlined.CheckBox
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.FormatBold
import androidx.compose.material.icons.outlined.FormatItalic
import androidx.compose.material.icons.outlined.Title
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import kotlin.math.max
import kotlin.math.min

fun wrapMarkdown(value: TextFieldValue, before: String, after: String = before): TextFieldValue {
    val text = value.text
    val start = min(value.selection.start, value.selection.end).coerceIn(0, text.length)
    val end = max(value.selection.start, value.selection.end).coerceIn(0, text.length)
    val selected = text.substring(start, end)
    val insert = before + selected + after
    val newText = text.replaceRange(start, end, insert)
    val cursor = if (selected.isEmpty()) start + before.length else start + insert.length
    return TextFieldValue(newText, TextRange(cursor))
}

fun insertMarkdown(value: TextFieldValue, snippet: String): TextFieldValue {
    val text = value.text
    val start = min(value.selection.start, value.selection.end).coerceIn(0, text.length)
    val end = max(value.selection.start, value.selection.end).coerceIn(0, text.length)
    val newText = text.replaceRange(start, end, snippet)
    return TextFieldValue(newText, TextRange(start + snippet.length))
}

@Composable
fun NkGhostField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    placeholder: String,
    textStyle: TextStyle,
    modifier: Modifier = Modifier,
    singleLine: Boolean = false
) {
    val color = MaterialTheme.colorScheme.onBackground
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        textStyle = textStyle.copy(color = color),
        singleLine = singleLine,
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        decorationBox = { inner ->
            Box {
                if (value.text.isEmpty()) {
                    Text(
                        placeholder,
                        style = textStyle,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                }
                inner()
            }
        }
    )
}

@Composable
fun NkMarkdownBar(
    onWrap: (before: String, after: String) -> Unit,
    onInsert: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MdIcon(Icons.Outlined.FormatBold, "Bold") { onWrap("**", "**") }
            MdIcon(Icons.Outlined.FormatItalic, "Italic") { onWrap("_", "_") }
            MdIcon(Icons.Outlined.Title, "Heading") { onInsert("\n## ") }
            MdIcon(Icons.AutoMirrored.Outlined.FormatListBulleted, "List") { onInsert("\n- ") }
            MdIcon(Icons.Outlined.CheckBox, "Task") { onInsert("\n- [ ] ") }
            MdIcon(Icons.Outlined.Code, "Code") { onWrap("`", "`") }
        }
    }
}

@Composable
private fun MdIcon(icon: ImageVector, label: String, onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Icon(
            icon,
            contentDescription = label,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
