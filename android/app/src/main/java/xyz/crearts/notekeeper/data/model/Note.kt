package xyz.crearts.notekeeper.data.model

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
    @SerializedName("isFavorite")
    val isFavorite: Boolean = false,
    @SerializedName("isEncrypted")
    val isEncrypted: Boolean = false,
    @SerializedName("isArchived")
    val isArchived: Boolean = false,
    @SerializedName("isDeleted")
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

