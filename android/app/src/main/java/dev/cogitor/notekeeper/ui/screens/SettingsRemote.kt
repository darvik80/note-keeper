package dev.cogitor.notekeeper.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import dev.cogitor.notekeeper.data.model.ApiToken
import dev.cogitor.notekeeper.data.model.ApiTokenCreateRequest
import dev.cogitor.notekeeper.data.model.BackupSettings
import dev.cogitor.notekeeper.data.model.IntegrationRequest
import dev.cogitor.notekeeper.data.model.UserSettings
import dev.cogitor.notekeeper.data.remote.RetrofitClient
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset

private val TOKEN_SCOPES = listOf("notes", "todos", "vault", "mcp")

@Composable
fun SettingsRemoteBlocks(snackbar: SnackbarHostState) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboardManager.current
    val api = RetrofitClient.accountApiService
    val backupApi = RetrofitClient.backupApiService

    var telegramToken by remember { mutableStateOf("") }
    var telegramChatId by remember { mutableStateOf("") }
    var dingtalkWebhook by remember { mutableStateOf("") }
    var dingtalkSecret by remember { mutableStateOf("") }
    var reportEnabled by remember { mutableStateOf(false) }
    var reportTime by remember { mutableStateOf("09:00") }
    var channelTelegram by remember { mutableStateOf(true) }
    var channelDingtalk by remember { mutableStateOf(false) }
    var backupEnabled by remember { mutableStateOf(false) }
    var backupRetention by remember { mutableStateOf("30") }
    var tokens by remember { mutableStateOf(listOf<ApiToken>()) }
    var tokenName by remember { mutableStateOf("") }
    var tokenScopes by remember { mutableStateOf(setOf("notes", "todos", "vault", "mcp")) }
    var createdToken by remember { mutableStateOf<String?>(null) }

    fun toast(msg: String) {
        scope.launch { snackbar.showSnackbar(msg) }
    }

    LaunchedEffect(Unit) {
        runCatching { api.getSettings().body() }.getOrNull()?.let { settings ->
            telegramToken = settings.telegramBotToken.orEmpty()
            telegramChatId = settings.telegramChatId.orEmpty()
            dingtalkWebhook = settings.dingtalkWebhook.orEmpty()
            dingtalkSecret = settings.dingtalkSecret.orEmpty()
            reportEnabled = settings.dailyReportEnabled
            reportTime = utcToLocal(settings.dailyReportTime ?: "06:00")
            val channels = settings.dailyReportChannels.orEmpty().split(",")
                .map { it.trim() }.filter { it.isNotBlank() }
            if (channels.isNotEmpty()) {
                channelTelegram = "telegram" in channels
                channelDingtalk = "dingtalk" in channels
            }
        }
        runCatching { backupApi.getSettings().body() }.getOrNull()?.let {
            backupEnabled = it.enabled
            backupRetention = it.retentionDays.toString()
        }
        tokens = runCatching { api.listTokens().body().orEmpty() }.getOrElse { emptyList() }
    }

    val importPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri ?: return@rememberLauncherForActivityResult
        scope.launch {
            val name = "import.json"
            val file = File(context.cacheDir, name)
            context.contentResolver.openInputStream(uri)?.use { input ->
                file.outputStream().use { output -> input.copyTo(output) }
            }
            val part = MultipartBody.Part.createFormData(
                "file",
                file.name,
                file.asRequestBody("application/json".toMediaTypeOrNull())
            )
            val response = runCatching { backupApi.importData(part) }.getOrNull()
            toast(response?.body()?.message ?: if (response?.isSuccessful == true) "Imported" else "Import failed")
        }
    }

    fun currentSettings() = UserSettings(
        telegramBotToken = telegramToken.ifBlank { null },
        telegramChatId = telegramChatId.ifBlank { null },
        dingtalkWebhook = dingtalkWebhook.ifBlank { null },
        dingtalkSecret = dingtalkSecret.ifBlank { null },
        dailyReportEnabled = reportEnabled,
        dailyReportTime = localToUtc(reportTime),
        dailyReportChannels = listOfNotNull(
            "telegram".takeIf { channelTelegram },
            "dingtalk".takeIf { channelDingtalk }
        ).joinToString(",").ifBlank { "telegram" }
    )

    SettingsCard("Telegram") {
        OutlinedTextField(
            value = telegramToken,
            onValueChange = { telegramToken = it },
            label = { Text("Bot token") },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = telegramChatId,
            onValueChange = { telegramChatId = it },
            label = { Text("Chat ID") },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true
        )
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = {
                scope.launch {
                    val ok = runCatching { api.saveSettings(currentSettings()) }.getOrNull()?.isSuccessful == true
                    toast(if (ok) "Saved" else "Save failed")
                }
            }) { Text("Save") }
            OutlinedButton(onClick = {
                scope.launch {
                    val response = runCatching {
                        api.sendTelegram(
                            IntegrationRequest(
                                message = "Test from NoteKeeper Android",
                                subject = "Test",
                                botToken = telegramToken.ifBlank { null },
                                chatId = telegramChatId.ifBlank { null }
                            )
                        )
                    }.getOrNull()
                    toast(response?.body()?.message ?: if (response?.body()?.success == true) "Sent" else "Telegram failed")
                }
            }) { Text("Test") }
            OutlinedButton(onClick = {
                scope.launch {
                    val response = runCatching { api.testTelegramTodo() }.getOrNull()
                    toast(response?.body()?.message ?: if (response?.body()?.success == true) "Todo sent" else "Todo test failed")
                }
            }) { Text("Test todo") }
        }
    }

    Spacer(Modifier.height(16.dp))
    SettingsCard("DingTalk") {
        OutlinedTextField(
            value = dingtalkWebhook,
            onValueChange = { dingtalkWebhook = it },
            label = { Text("Webhook") },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = dingtalkSecret,
            onValueChange = { dingtalkSecret = it },
            label = { Text("Secret") },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true
        )
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = {
                scope.launch {
                    val ok = runCatching { api.saveSettings(currentSettings()) }.getOrNull()?.isSuccessful == true
                    toast(if (ok) "Saved" else "Save failed")
                }
            }) { Text("Save") }
            OutlinedButton(onClick = {
                scope.launch {
                    val response = runCatching {
                        api.sendDingtalk(
                            IntegrationRequest(
                                message = "Test from NoteKeeper Android",
                                subject = "Test",
                                webhook = dingtalkWebhook.ifBlank { null },
                                secret = dingtalkSecret.ifBlank { null }
                            )
                        )
                    }.getOrNull()
                    toast(response?.body()?.message ?: if (response?.body()?.success == true) "Sent" else "DingTalk failed")
                }
            }) { Text("Test") }
        }
    }

    Spacer(Modifier.height(16.dp))
    SettingsCard("Daily report") {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text("Enabled", modifier = Modifier.weight(1f))
            Switch(checked = reportEnabled, onCheckedChange = { reportEnabled = it })
        }
        OutlinedTextField(
            value = reportTime,
            onValueChange = { reportTime = it },
            label = { Text("Local time HH:mm") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = channelTelegram,
                onClick = { channelTelegram = !channelTelegram },
                label = { Text("Telegram") }
            )
            FilterChip(
                selected = channelDingtalk,
                onClick = { channelDingtalk = !channelDingtalk },
                label = { Text("DingTalk") }
            )
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = {
                scope.launch {
                    val ok = runCatching { api.saveSettings(currentSettings()) }.getOrNull()?.isSuccessful == true
                    toast(if (ok) "Saved" else "Save failed")
                }
            }) { Text("Save") }
            OutlinedButton(onClick = {
                scope.launch {
                    val ok = runCatching { api.testDailyReport() }.getOrNull()?.isSuccessful == true
                    toast(if (ok) "Report sent" else "Report failed")
                }
            }) { Text("Send test") }
        }
    }

    Spacer(Modifier.height(16.dp))
    SettingsCard("Backup") {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text("Scheduled backup", modifier = Modifier.weight(1f))
            Switch(checked = backupEnabled, onCheckedChange = { backupEnabled = it })
        }
        OutlinedTextField(
            value = backupRetention,
            onValueChange = { backupRetention = it.filter { ch -> ch.isDigit() } },
            label = { Text("Retention days") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = {
                scope.launch {
                    val settings = BackupSettings(
                        enabled = backupEnabled,
                        retentionDays = backupRetention.toIntOrNull() ?: 30
                    )
                    val ok = runCatching { backupApi.saveSettings(settings) }.getOrNull()?.isSuccessful == true
                    toast(if (ok) "Backup settings saved" else "Save failed")
                }
            }) { Text("Save") }
            OutlinedButton(onClick = {
                scope.launch {
                    val body = runCatching { backupApi.export().body() }.getOrNull()
                    if (body == null) {
                        toast("Export failed")
                        return@launch
                    }
                    val dest = File(context.cacheDir, "backup_${System.currentTimeMillis()}.json")
                    body.byteStream().use { input -> dest.outputStream().use { input.copyTo(it) } }
                    toast("Saved ${dest.name}")
                }
            }) { Text("Export") }
            OutlinedButton(onClick = { importPicker.launch(arrayOf("application/json", "*/*")) }) {
                Text("Import")
            }
        }
    }

    Spacer(Modifier.height(16.dp))
    SettingsCard("API tokens") {
        createdToken?.let { token ->
            Text(
                "Copy now. Token never shown again.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                token,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 6.dp, bottom = 8.dp)
            )
            TextButton(onClick = {
                clipboard.setText(AnnotatedString(token))
                toast("Copied")
            }) { Text("Copy") }
        }
        OutlinedTextField(
            value = tokenName,
            onValueChange = { tokenName = it },
            label = { Text("Name") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TOKEN_SCOPES.forEach { scopeName ->
                FilterChip(
                    selected = scopeName in tokenScopes,
                    onClick = {
                        tokenScopes = if (scopeName in tokenScopes) tokenScopes - scopeName else tokenScopes + scopeName
                    },
                    label = { Text(scopeName) }
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = {
                scope.launch {
                    val created = runCatching {
                        api.createToken(
                            ApiTokenCreateRequest(tokenName.trim(), tokenScopes.toList())
                        ).body()
                    }.getOrNull()
                    if (created == null) {
                        toast("Create failed")
                    } else {
                        createdToken = created.token
                        tokenName = ""
                        tokens = runCatching { api.listTokens().body().orEmpty() }.getOrElse { tokens }
                    }
                }
            },
            enabled = tokenName.isNotBlank() && tokenScopes.isNotEmpty()
        ) { Text("Create token") }
        Spacer(Modifier.height(12.dp))
        if (tokens.isEmpty()) {
            Text("No tokens yet", style = MaterialTheme.typography.bodySmall)
        } else {
            tokens.forEach { token ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(token.name, style = MaterialTheme.typography.bodyMedium)
                        Text(
                            listOfNotNull(token.prefix, token.scopes.orEmpty().joinToString(", "))
                                .joinToString(" · "),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    TextButton(onClick = {
                        scope.launch {
                            runCatching { api.revokeToken(token.id) }
                            tokens = tokens.filter { it.id != token.id }
                        }
                    }) { Text("Revoke") }
                }
            }
        }
    }
}

@Composable
private fun SettingsCard(title: String, content: @Composable () -> Unit) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

private fun localToUtc(hhmm: String): String {
    val parts = hhmm.split(":")
    if (parts.size < 2) return "06:00"
    val hour = parts[0].toIntOrNull() ?: return "06:00"
    val minute = parts[1].toIntOrNull() ?: return "06:00"
    val utc = LocalDateTime.of(LocalDate.of(2000, 1, 1), LocalTime.of(hour.coerceIn(0, 23), minute.coerceIn(0, 59)))
        .atZone(ZoneId.systemDefault())
        .withZoneSameInstant(ZoneOffset.UTC)
        .toLocalTime()
    return "%02d:%02d".format(utc.hour, utc.minute)
}

private fun utcToLocal(hhmm: String): String {
    val parts = hhmm.split(":")
    if (parts.size < 2) return "09:00"
    val hour = parts[0].toIntOrNull() ?: return "09:00"
    val minute = parts[1].toIntOrNull() ?: return "09:00"
    val local = LocalDateTime.of(LocalDate.of(2000, 1, 1), LocalTime.of(hour.coerceIn(0, 23), minute.coerceIn(0, 59)))
        .atZone(ZoneOffset.UTC)
        .withZoneSameInstant(ZoneId.systemDefault())
        .toLocalTime()
    return "%02d:%02d".format(local.hour, local.minute)
}
