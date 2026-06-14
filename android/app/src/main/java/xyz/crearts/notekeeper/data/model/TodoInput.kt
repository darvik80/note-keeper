package xyz.crearts.notekeeper.data.model

data class TodoInput(
    val title: String,
    val description: String? = null,
    val tags: List<String>? = null,
    val priority: String? = null,
    val isFavorite: Boolean? = null,
    val completed: Boolean? = null,
    val dueDate: String? = null,
    val reminder: String? = null,
    val notificationChannels: String? = null,
    val location: String? = null,
    val schedule: ScheduleInput? = null
)

data class ScheduleInput(
    val repeat: String,
    val endDate: String? = null
)
