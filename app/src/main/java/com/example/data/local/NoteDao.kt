package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes ORDER BY isPinned DESC, updatedAt DESC")
    fun getAllNotes(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE id = :id")
    suspend fun getNoteById(id: String): NoteEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: NoteEntity)

    @Query("UPDATE notes SET isPinned = :value, updatedAt = :updatedAt WHERE id = :id")
    suspend fun setPinned(id: String, value: Boolean, updatedAt: Long)

    @Query("UPDATE notes SET isFavorite = :value, updatedAt = :updatedAt WHERE id = :id")
    suspend fun setFavorite(id: String, value: Boolean, updatedAt: Long)

    @Query("UPDATE notes SET isArchived = :value, updatedAt = :updatedAt WHERE id = :id")
    suspend fun setArchived(id: String, value: Boolean, updatedAt: Long)

    @Query("UPDATE notes SET isTrashed = :value, isPinned = 0, updatedAt = :updatedAt WHERE id = :id")
    suspend fun setTrashed(id: String, value: Boolean, updatedAt: Long)

    @Query("UPDATE notes SET colorArgb = :colorArgb, updatedAt = :updatedAt WHERE id = :id")
    suspend fun setColor(id: String, colorArgb: Int, updatedAt: Long)

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun deleteNoteById(id: String)

    @Query("DELETE FROM notes WHERE isTrashed = 1")
    suspend fun emptyTrash()
}

@Dao
interface FolderDao {
    @Query("SELECT * FROM folders ORDER BY name ASC")
    fun getAllFolders(): Flow<List<FolderEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFolder(folder: FolderEntity)

    @Query("DELETE FROM folders WHERE id = :id")
    suspend fun deleteFolderById(id: String)
}
