package xyz.crearts.notekeeper.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey(autoGenerate = true)
    val localId: Long = 0,
    val id: String,
    val title: String,
    val content: String,
    val tags: String = "[]",
    val folder: String = "default",
    val subfolder: String? = null,
    val priority: String = "medium",
    val isFavorite: Boolean = false,
    val isEncrypted: Boolean = false,
    val isArchived: Boolean = false,
    val isDeleted: Boolean = false,
    val deletedAt: String? = null,
    val reminder: String? = null,
    val templateId: String? = null,
    val ownerId: String? = null,
    val sharedWith: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null,
    val attachments: String = "[]",
    val syncStatus: String = "SYNCED"
)

