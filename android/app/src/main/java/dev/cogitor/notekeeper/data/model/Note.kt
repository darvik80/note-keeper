package dev.cogitor.notekeeper.data.model

import com.google.gson.annotations.SerializedName

data class Note(
    val localId: Long = 0,
    val id: String,
    val title: String,
    val content: String? = "",
    val tags: List<String>? = emptyList(),
    val folder: String? = "default",
    val subfolder: String? = null,
    val priority: String? = "medium",
    @SerializedName(value = "isFavorite", alternate = ["favorite"])
    val isFavorite: Boolean = false,
    @SerializedName(value = "isEncrypted", alternate = ["encrypted"])
    val isEncrypted: Boolean = false,
    @SerializedName(value = "isArchived", alternate = ["archived"])
    val isArchived: Boolean = false,
    @SerializedName(value = "isDeleted", alternate = ["deleted"])
    val isDeleted: Boolean = false,
    val deletedAt: String? = null,
    val reminder: String? = null,
    val templateId: String? = null,
    val ownerId: String? = null,
    val sharedWith: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null,
    val attachments: List<Attachment>? = emptyList(),
    val syncStatus: SyncStatus = SyncStatus.SYNCED
)

