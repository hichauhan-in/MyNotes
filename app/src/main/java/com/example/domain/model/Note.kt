package com.example.domain.model

/**
 * Decrypted, in-memory representation of a note. Instances of this class only ever
 * exist in RAM — everything written to disk is encrypted (see NoteEntity).
 */
data class Note(
    val id: String,
    val title: String,
    val content: String,
    val createdAt: Long,
    val updatedAt: Long,
    val isPinned: Boolean = false,
    val isFavorite: Boolean = false,
    val isArchived: Boolean = false,
    val isTrashed: Boolean = false,
    val folderId: String? = null,
    val tags: List<String> = emptyList(),
    val colorArgb: Int = 0,
) {
    val preview: String
        get() = content.trim().replace(Regex("\\s+"), " ")

    val wordCount: Int
        get() = content.trim().let { if (it.isEmpty()) 0 else it.split(Regex("\\s+")).size }
}

data class Folder(
    val id: String,
    val name: String,
    val colorArgb: Int = 0,
)
