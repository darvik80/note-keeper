package xyz.crearts.notekeeper.data.model

data class NoteInput(
    val title: String,
    val content: String? = null,
    val tags: List<String>? = null,
    val folder: String? = null,
    val subfolder: String? = null,
    val priority: String? = null,
    val isFavorite: Boolean? = null,
    val isEncrypted: Boolean? = null,
    val reminder: String? = null,
    val attachments: List<AttachmentInput>? = null,
    val templateId: String? = null
)

data class AttachmentInput(
    val id: String? = null,
    val name: String,
    val url: String,
    val size: Long? = null,
    val mimeType: String? = null,
    val uploadedAt: String? = null
)
