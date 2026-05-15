package xyz.crearts.notekeeper.data.model

data class Attachment(
    val id: String,
    val parentId: String,
    val parentType: String,
    val name: String,
    val size: Long,
    val type: String,
    val url: String,
    val uploadedAt: String
)
