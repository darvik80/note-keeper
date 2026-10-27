package dev.cogitor.notekeeper.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NkFolderField(
    path: String,
    onPathChange: (String) -> Unit,
    knownPaths: List<String>,
    modifier: Modifier = Modifier,
    label: String = "Folder"
) {
    var expanded by remember { mutableStateOf(false) }
    val suggestions = remember(path, knownPaths) {
        val needle = path.trim().lowercase()
        knownPaths.filter { candidate ->
            needle.isEmpty() || candidate.lowercase().contains(needle)
        }.take(8)
    }
    ExposedDropdownMenuBox(
        expanded = expanded && suggestions.isNotEmpty(),
        onExpandedChange = { expanded = it },
        modifier = modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = path,
            onValueChange = {
                onPathChange(it)
                expanded = true
            },
            label = { Text(label) },
            placeholder = { Text("Work/Q1") },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable),
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded && suggestions.isNotEmpty()) },
            singleLine = true
        )
        ExposedDropdownMenu(
            expanded = expanded && suggestions.isNotEmpty(),
            onDismissRequest = { expanded = false }
        ) {
            suggestions.forEach { folder ->
                DropdownMenuItem(
                    text = { Text(folder) },
                    onClick = {
                        onPathChange(folder)
                        expanded = false
                    }
                )
            }
        }
    }
}
