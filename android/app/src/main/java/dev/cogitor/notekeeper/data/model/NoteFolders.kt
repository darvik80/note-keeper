package dev.cogitor.notekeeper.data.model

object NoteFolders {
    const val ALL = "all"

    fun path(folder: String?, subfolder: String?): String {
        val top = folder?.trim().orEmpty().ifBlank { "default" }
        val sub = subfolder?.trim()?.takeIf { it.isNotBlank() }
        return if (sub == null) top else "$top/$sub"
    }

    fun parse(path: String): Pair<String, String?> {
        val trimmed = path.trim().trim('/')
        if (trimmed.isEmpty() || trimmed == ALL || trimmed == "root") {
            return "default" to null
        }
        val parts = trimmed.split('/').map { it.trim() }.filter { it.isNotBlank() }
        if (parts.isEmpty()) return "default" to null
        if (parts.size == 1) return parts[0] to null
        return parts[0] to parts.drop(1).joinToString("/")
    }

    fun label(path: String): String {
        val trimmed = path.trim()
        if (trimmed.isEmpty() || trimmed == ALL || trimmed == "root" || trimmed == "default") return "All"
        return trimmed
    }

    fun isDefault(folder: String?, subfolder: String?): Boolean {
        val top = folder?.trim().orEmpty().ifBlank { "default" }
        return top == "default" && subfolder.isNullOrBlank()
    }

    fun matches(note: Note, selectedPath: String): Boolean {
        if (selectedPath == ALL || selectedPath == "root") return true
        val notePath = path(note.folder, note.subfolder)
        return notePath == selectedPath || notePath.startsWith("$selectedPath/")
    }

    fun knownPaths(notes: List<Note>): List<String> =
        notes.map { path(it.folder, it.subfolder) }
            .filter { it.isNotBlank() && it != "default" }
            .distinct()
            .sorted()
}

fun VaultFolder.displayPath(all: List<VaultFolder>): String {
    val names = mutableListOf(name)
    var pid = parentId
    val seen = mutableSetOf(id)
    while (pid != null && pid !in seen) {
        seen += pid
        val parent = all.find { it.id == pid } ?: break
        names.add(0, parent.name)
        pid = parent.parentId
    }
    return names.joinToString(" / ")
}
