package dev.cogitor.notekeeper.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import dev.cogitor.notekeeper.data.model.VaultItemInput
import dev.cogitor.notekeeper.ui.components.nkTopBarColors
import dev.cogitor.notekeeper.ui.viewmodel.VaultViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultDetailScreen(
    viewModel: VaultViewModel,
    itemId: String?,
    onBack: () -> Unit
) {
    val existing by viewModel.item.collectAsState()
    val context = LocalContext.current
    var title by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var host by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var privateKey by remember { mutableStateOf("") }
    var showSecret by remember { mutableStateOf(false) }
    var type by remember { mutableStateOf("password") }

    LaunchedEffect(itemId) {
        if (itemId != null) viewModel.loadItem(itemId)
    }
    LaunchedEffect(existing) {
        existing?.let {
            title = it.title
            username = it.username.orEmpty()
            host = it.host.orEmpty()
            password = it.password ?: it.secret.orEmpty()
            privateKey = it.privateKey.orEmpty()
            type = it.type
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(if (itemId == null) "New item" else "Vault item") },
                colors = nkTopBarColors(),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (itemId != null) {
                        IconButton(onClick = {
                            viewModel.delete(itemId)
                            onBack()
                        }) {
                            Icon(Icons.Outlined.Delete, contentDescription = "Delete")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            OutlinedTextField(
                title,
                { title = it },
                label = { Text("Title") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                username,
                { username = it },
                label = { Text("Username") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                host,
                { host = it },
                label = { Text("Host") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                password,
                { password = it },
                label = { Text("Password") },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = if (showSecret) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { showSecret = !showSecret }) {
                        Icon(
                            if (showSecret) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                            contentDescription = if (showSecret) "Hide" else "Show"
                        )
                    }
                }
            )
            TextButton(onClick = { copy(context, password) }) {
                Icon(Icons.Outlined.ContentCopy, contentDescription = null)
                Text("Copy password")
            }
            if (type == "ssh_key" || privateKey.isNotBlank()) {
                OutlinedTextField(
                    privateKey,
                    { privateKey = it },
                    label = { Text("Private key") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 6
                )
            }
            Button(
                onClick = {
                    viewModel.save(
                        VaultItemInput(
                            title = title,
                            type = if (privateKey.isNotBlank()) "ssh_key" else "password",
                            username = username,
                            host = host,
                            password = password,
                            privateKey = privateKey.ifBlank { null }
                        ),
                        itemId,
                        existing?.revision
                    )
                    onBack()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
            ) { Text("Save") }
        }
    }
}

private fun copy(context: Context, value: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.primaryClip = ClipData.newPlainText("vault", value)
}
