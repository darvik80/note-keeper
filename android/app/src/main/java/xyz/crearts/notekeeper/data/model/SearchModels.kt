package xyz.crearts.notekeeper.data.model

data class SearchResult(
    val notes: List<NoteResponse>? = emptyList(),
    val todos: List<TodoResponse>? = emptyList()
)

data class SavedQuery(
    val id: String,
    val name: String,
    val query: String,
    val filters: SavedQueryFilters? = null,
    val createdAt: String? = null
)

data class SavedQueryFilters(
    val type: String? = null,
    val tags: List<String>? = emptyList(),
    val priority: String? = null,
    val folder: String? = null
)

data class SavedQueryInput(
    val name: String,
    val query: String,
    val filters: SavedQueryFilters? = null
)
