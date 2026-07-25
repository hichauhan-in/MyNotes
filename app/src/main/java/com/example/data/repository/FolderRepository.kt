package com.example.data.repository

import com.example.data.local.FolderDao
import com.example.data.local.FolderEntity
import com.example.data.local.NoteDao
import com.example.domain.model.Folder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * Manages "books" (nestable folders) and moving notes between them. Folder names are
 * not sensitive, so they are stored in plain text (unlike note titles / bodies).
 */
class FolderRepository(
    private val folderDao: FolderDao,
    private val noteDao: NoteDao,
) {
    val allFolders: Flow<List<Folder>> = folderDao.getAllFolders().map { list ->
        list.map { it.toFolder() }
    }

    suspend fun createFolder(name: String, parentId: String?): String = withContext(Dispatchers.IO) {
        val folder = FolderEntity(name = name.trim().ifBlank { "Untitled book" }, parentId = parentId)
        folderDao.insertFolder(folder)
        folder.id
    }

    suspend fun renameFolder(id: String, name: String) = withContext(Dispatchers.IO) {
        folderDao.renameFolder(id, name.trim().ifBlank { "Untitled book" })
    }

    /** Move a book under a new parent (null = top level). */
    suspend fun moveFolder(id: String, newParentId: String?) = withContext(Dispatchers.IO) {
        if (id != newParentId) folderDao.setFolderParent(id, newParentId)
    }

    /**
     * Delete a book. Its notes and sub-books are moved up to the book's own parent so
     * nothing is lost, then the book itself is removed.
     */
    suspend fun deleteFolder(id: String) = withContext(Dispatchers.IO) {
        val parentId = folderDao.getFolderById(id)?.parentId
        noteDao.reparentNotes(id, parentId)
        folderDao.reparentChildFolders(id, parentId)
        folderDao.deleteFolderById(id)
    }

    suspend fun moveNoteToFolder(noteId: String, folderId: String?) = withContext(Dispatchers.IO) {
        noteDao.setFolder(noteId, folderId, System.currentTimeMillis())
    }

    suspend fun moveNotesToFolder(noteIds: Collection<String>, folderId: String?) =
        withContext(Dispatchers.IO) {
            val now = System.currentTimeMillis()
            noteIds.forEach { noteDao.setFolder(it, folderId, now) }
        }

    private fun FolderEntity.toFolder() = Folder(
        id = id,
        name = name,
        colorArgb = colorArgb,
        parentId = parentId,
    )
}
