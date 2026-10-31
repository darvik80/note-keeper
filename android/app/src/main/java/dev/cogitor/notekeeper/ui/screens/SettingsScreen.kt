package dev.cogitor.notekeeper.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import dev.cogitor.notekeeper.data.local.SettingsDataStore
import dev.cogitor.notekeeper.data.notification.NotificationHelper
import dev.cogitor.notekeeper.ui.components.nkTopBarColors
import dev.cogitor.notekeeper.ui.theme.AppThemeId
import dev.cogitor.notekeeper.ui.theme.palette
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val settingsDataStore = remember { SettingsDataStore(context) }
    val defaultChannel by settingsDataStore.defaultNotificationChannel.collectAsState(initial = SettingsDataStore.DEFAULT_CHANNEL)
    val themeIdStr by settingsDataStore.themeId.collectAsState(initial = SettingsDataStore.DEFAULT_THEME)
    val selectedTheme = AppThemeId.from(themeIdStr)
    var channelExpanded by remember { mutableStateOf(false) }
    var confirmLogout by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = nkTopBarColors()
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("Theme", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Matches web palettes. Cogitor is the default.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(14.dp))
                    AppThemeId.entries.chunked(4).forEach { row ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(bottom = 10.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            row.forEach { theme ->
                                ThemeSwatch(
                                    theme = theme,
                                    selected = theme == selectedTheme,
                                    onClick = { scope.launch { settingsDataStore.saveThemeId(theme.id) } },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("Notifications", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(12.dp))
                    ExposedDropdownMenuBox(
                        expanded = channelExpanded,
                        onExpandedChange = { channelExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = NotificationHelper.getChannelName(defaultChannel),
                            onValueChange = {},
                            readOnly = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                            leadingIcon = {
                                Icon(
                                    Icons.Outlined.Notifications,
                                    contentDescription = null
                                )
                            },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = channelExpanded) }
                        )
                        ExposedDropdownMenu(
                            expanded = channelExpanded,
                            onDismissRequest = { channelExpanded = false }
                        ) {
                            NotificationHelper.channels.forEach { ch ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(
                                                ch.name,
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                            Text(
                                                ch.description,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.outline
                                            )
                                        }
                                    },
                                    onClick = {
                                        scope.launch {
                                            settingsDataStore.saveDefaultNotificationChannel(
                                                ch.id
                                            )
                                        }
                                        channelExpanded = false
                                    },
                                    leadingIcon = if (defaultChannel == ch.id) {
                                        { Icon(Icons.Outlined.Check, contentDescription = null) }
                                    } else null
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            SettingsRemoteBlocks(snackbarHostState)

            Spacer(Modifier.height(16.dp))
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("NoteKeeper", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Android 2.0.3",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "Offline-first notes, todos and vault. Google and Yandex sign-in.",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 8.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
            Button(
                onClick = { confirmLogout = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                ),
                shape = MaterialTheme.shapes.medium
            ) {
                Icon(Icons.AutoMirrored.Outlined.Logout, contentDescription = null)
                Spacer(Modifier.size(8.dp))
                Text("Log out")
            }
        }
    }

    if (confirmLogout) {
        AlertDialog(
            onDismissRequest = { confirmLogout = false },
            title = { Text("Log out?") },
            text = { Text("You'll need to sign in again. Notes already on this device stay in the local cache.") },
            confirmButton = {
                TextButton(onClick = onLogout) { Text("Log out") }
            },
            dismissButton = {
                TextButton(onClick = { confirmLogout = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun ThemeSwatch(
    theme: AppThemeId,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = theme.palette
    Column(
        modifier = modifier.clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(palette.background)
                .border(
                    width = if (selected) 2.dp else 1.dp,
                    color = if (selected) palette.primary else palette.border,
                    shape = RoundedCornerShape(14.dp)
                )
                .padding(8.dp)
        ) {
            Row(
                modifier = Modifier.align(Alignment.BottomStart),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(palette.primary))
                Box(Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(palette.secondary))
                Box(Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(palette.surface))
            }
        }
        Text(
            theme.label,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}
