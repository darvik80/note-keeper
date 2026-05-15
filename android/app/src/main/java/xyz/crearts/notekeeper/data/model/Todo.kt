package xyz.crearts.notekeeper.data.model

data class Todo(
    val localId: Long = 0,
    val id: String,
    val title: String,
    val description: String? = "",
    val completed: Boolean = false,
    val tags: List<String>? = emptyList(),
    val priority: String? = "medium",
    val isFavorite: Boolean = false,
    val isArchived: Boolean = false,
    val isDeleted: Boolean = false,
    val deletedAt: String? = null,
    val dueDate: String? = null,
    val reminder: String? = null,
    val notifiedAt: String? = null,
    val notificationChannels: String? = null,
    val locationLat: Double? = null,
    val locationLng: Double? = null,
    val locationAddress: String? = null,
    val scheduleRepeat: String? = "none",
    val scheduleEndDate: String? = null,
    val ownerId: String? = null,
    val sharedWith: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null,
    val attachments: List<Attachment>? = emptyList(),
    val syncStatus: SyncStatus = SyncStatus.SYNCED
)

