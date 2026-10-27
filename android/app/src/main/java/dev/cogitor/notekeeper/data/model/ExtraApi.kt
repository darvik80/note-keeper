package dev.cogitor.notekeeper.data.model

data class UserSettings(
    val id: String? = null,
    val telegramBotToken: String? = null,
    val telegramChatId: String? = null,
    val telegramWebhookSecret: String? = null,
    val dingtalkWebhook: String? = null,
    val dingtalkSecret: String? = null,
    val dailyReportEnabled: Boolean = false,
    val dailyReportTime: String? = null,
    val dailyReportChannels: String? = null,
    val dailyReportTemplate: String? = null,
    val dailyReportItemTemplate: String? = null,
    val dailyReportLastSent: String? = null,
    val updatedAt: String? = null
)

data class NoteTemplate(
    val id: String,
    val name: String,
    val content: String? = "",
    val tags: List<String>? = emptyList(),
    val category: String? = null
)

data class NoteTemplateInput(
    val name: String,
    val content: String,
    val tags: List<String>? = emptyList(),
    val category: String = "general"
)

data class IntegrationRequest(
    val message: String,
    val subject: String? = null,
    val botToken: String? = null,
    val chatId: String? = null,
    val webhook: String? = null,
    val secret: String? = null
)

data class IntegrationResponse(
    val success: Boolean = false,
    val message: String? = null
)

data class ApiToken(
    val id: String,
    val name: String,
    val prefix: String? = null,
    val scopes: List<String>? = emptyList(),
    val expiresAt: String? = null,
    val lastUsedAt: String? = null,
    val createdAt: String? = null,
    val token: String? = null
)

data class ApiTokenCreateRequest(
    val name: String,
    val scopes: List<String> = listOf("notes", "todos"),
    val expiresAt: String? = null
)

data class TodoCompletionLog(
    val id: String? = null,
    val todoId: String? = null,
    val completedAt: String? = null,
    val occurrenceReminder: String? = null,
    val occurrenceDueDate: String? = null
)

data class BackupInfo(
    val filename: String? = null,
    val size: String? = null,
    val createdAt: String? = null
)

data class BackupSettings(
    val enabled: Boolean = false,
    val cron: String? = "0 0 2 * * *",
    val retentionDays: Int = 30
)

data class StatusMessage(
    val status: String? = null,
    val message: String? = null
)
