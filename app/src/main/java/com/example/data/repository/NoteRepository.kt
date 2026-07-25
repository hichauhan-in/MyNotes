package com.example.data.repository

import com.example.data.local.NoteDao
import com.example.data.local.NoteEntity
import com.example.data.security.EncryptionManager
import com.example.domain.model.Note
import com.example.domain.model.NoteType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * Single source of truth for notes. Encryption / decryption happens here so the rest
 * of the app only ever deals with plaintext [Note] models held in memory.
 */
class NoteRepository(private val noteDao: NoteDao) {

    val allNotes: Flow<List<Note>> = noteDao.getAllNotes().map { entities ->
        entities.map { it.toNote() }
    }

    suspend fun getNoteById(id: String): Note? = withContext(Dispatchers.IO) {
        noteDao.getNoteById(id)?.toNote()
    }

    suspend fun saveNote(note: Note): Unit = withContext(Dispatchers.IO) {
        val existing = noteDao.getNoteById(note.id)
        val entity = NoteEntity(
            id = note.id.ifBlank { UUID.randomUUID().toString() },
            encryptedTitle = EncryptionManager.encrypt(note.title),
            encryptedContent = EncryptionManager.encrypt(note.content),
            createdAt = existing?.createdAt ?: note.createdAt,
            updatedAt = System.currentTimeMillis(),
            isPinned = note.isPinned,
            isFavorite = note.isFavorite,
            // Archive / trash state is managed by dedicated actions, never the editor,
            // so preserve whatever is already persisted for an existing note.
            isArchived = existing?.isArchived ?: note.isArchived,
            isTrashed = existing?.isTrashed ?: note.isTrashed,
            folderId = note.folderId,
            tags = note.tags.joinToString(","),
            colorArgb = note.colorArgb,
            type = note.type.name,
            attachments = note.attachments.joinToString(","),
        )
        noteDao.insertNote(entity)
    }

    suspend fun setPinned(id: String, value: Boolean) = withContext(Dispatchers.IO) {
        noteDao.setPinned(id, value, System.currentTimeMillis())
    }

    suspend fun setFavorite(id: String, value: Boolean) = withContext(Dispatchers.IO) {
        noteDao.setFavorite(id, value, System.currentTimeMillis())
    }

    suspend fun setArchived(id: String, value: Boolean) = withContext(Dispatchers.IO) {
        noteDao.setArchived(id, value, System.currentTimeMillis())
    }

    suspend fun setTrashed(id: String, value: Boolean) = withContext(Dispatchers.IO) {
        noteDao.setTrashed(id, value, System.currentTimeMillis())
    }

    suspend fun setColor(id: String, colorArgb: Int) = withContext(Dispatchers.IO) {
        noteDao.setColor(id, colorArgb, System.currentTimeMillis())
    }

    suspend fun deletePermanently(id: String) = withContext(Dispatchers.IO) {
        noteDao.deleteNoteById(id)
    }

    suspend fun emptyTrash() = withContext(Dispatchers.IO) {
        noteDao.emptyTrash()
    }

    /** Permanently deletes trashed notes last touched before [retentionDays] ago. No-op if 0. */
    suspend fun purgeTrashOlderThan(retentionDays: Int) = withContext(Dispatchers.IO) {
        if (retentionDays <= 0) return@withContext
        val cutoff = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(retentionDays.toLong())
        noteDao.purgeTrashedBefore(cutoff)
    }

    private fun NoteEntity.toNote() = Note(
        id = id,
        title = EncryptionManager.decrypt(encryptedTitle),
        content = EncryptionManager.decrypt(encryptedContent),
        createdAt = createdAt,
        updatedAt = updatedAt,
        isPinned = isPinned,
        isFavorite = isFavorite,
        isArchived = isArchived,
        isTrashed = isTrashed,
        folderId = folderId,
        tags = tags.split(",").filter { it.isNotBlank() },
        colorArgb = colorArgb,
        type = runCatching { NoteType.valueOf(type) }.getOrDefault(NoteType.TEXT),
        attachments = attachments.split(",").filter { it.isNotBlank() },
    )
}
