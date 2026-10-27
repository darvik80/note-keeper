package dev.cogitor.notekeeper.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.InputChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import dev.cogitor.notekeeper.data.repository.TagCatalog

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NkTagInput(
    tags: List<String>,
    onTagsChange: (List<String>) -> Unit,
    draft: String,
    onDraftChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var catalog by remember { mutableStateOf(emptyList<String>()) }
    LaunchedEffect(Unit) {
        catalog = TagCatalog.load(context)
    }

    fun normalize(raw: String): String = raw.trim().trimStart('#').trimEnd(',', ' ', '\n')

    fun commit(raw: String = draft) {
        val next = normalize(raw)
        if (next.isNotBlank() && tags.none { it.equals(next, ignoreCase = true) }) {
            onTagsChange(tags + next)
        }
        onDraftChange("")
    }

    val needle = normalize(draft).lowercase()
    val suggestions = remember(needle, catalog, tags) {
        if (needle.isEmpty()) emptyList()
        else (catalog + tags)
            .distinctBy { it.lowercase() }
            .filter { tag ->
                tag.lowercase().contains(needle) &&
                    tags.none { it.equals(tag, ignoreCase = true) }
            }
            .take(8)
    }

    Column(modifier.fillMaxWidth()) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            tags.forEach { tag ->
                InputChip(
                    selected = false,
                    onClick = { onTagsChange(tags.filterNot { it == tag }) },
                    label = { Text("#$tag") },
                    trailingIcon = {
                        Icon(
                            Icons.Outlined.Close,
                            contentDescription = "Remove",
                            modifier = Modifier.size(14.dp)
                        )
                    }
                )
            }
            BasicTextField(
                value = draft,
                onValueChange = { value ->
                    if (value.endsWith(",") || value.endsWith(" ") || value.endsWith("\n")) {
                        commit(value)
                    } else {
                        onDraftChange(value)
                    }
                },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onBackground
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { commit() }),
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .weight(1f, fill = false),
                decorationBox = { inner ->
                    Box {
                        if (draft.isEmpty() && tags.isEmpty()) {
                            Text(
                                "tags",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                            )
                        } else if (draft.isEmpty()) {
                            Text(
                                "add tag",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                            )
                        }
                        inner()
                    }
                }
            )
        }
        if (suggestions.isNotEmpty()) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 8.dp),
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surface
            ) {
                Column {
                    suggestions.forEach { tag ->
                        Text(
                            "#$tag",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { commit(tag) }
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                        )
                    }
                }
            }
        }
    }
}
