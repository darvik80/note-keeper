package dev.cogitor.notekeeper.data.model

import com.google.gson.annotations.JsonAdapter

data class NoteHistory(
    val id: String = "",
    val noteId: String? = null,
    val content: String? = null,
    @JsonAdapter(FlexibleTimestampDeserializer::class)
    val timestamp: String? = null,
    val action: String? = null
)
