package xyz.crearts.notekeeper.data.model

data class NoteHistory(
    val id: String,
    val noteId: String,
    val content: String? = null,
    val timestamp: String? = null,
    val action: String? = null
)
