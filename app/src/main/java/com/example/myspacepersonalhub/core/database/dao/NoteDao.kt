package com.example.myspacepersonalhub.core.database.dao

import androidx.room.*
import com.example.myspacepersonalhub.core.database.entity.NoteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes WHERE deletedAt IS NULL AND inTrash = 0 ORDER BY isPinned DESC, updatedAt DESC")
    fun getAllNotes(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE deletedAt IS NULL AND inTrash = 1 ORDER BY updatedAt DESC")
    fun getTrashNotes(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE deletedAt IS NULL AND inTrash = 0 AND isPinned = 1 ORDER BY updatedAt DESC")
    fun getPinnedNotes(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE deletedAt IS NULL AND inTrash = 0 AND isJournal = 1 ORDER BY journalDate DESC, updatedAt DESC")
    fun getJournalEntries(): Flow<List<NoteEntity>>

    @Query("""
        SELECT * FROM notes 
        WHERE deletedAt IS NULL AND inTrash = 0 AND (
            title LIKE '%' || :query || '%' OR 
            body LIKE '%' || :query || '%' OR 
            category LIKE '%' || :query || '%'
        )
        ORDER BY isPinned DESC, updatedAt DESC
    """)
    fun searchNotes(query: String): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE id = :id AND deletedAt IS NULL")
    fun getNoteById(id: String): Flow<NoteEntity?>

    @Query("SELECT * FROM notes WHERE id = :id")
    suspend fun getNoteByIdDirect(id: String): NoteEntity?

    @Query("SELECT * FROM notes WHERE syncState != 'SYNCED'")
    suspend fun getPendingSyncNotes(): List<NoteEntity>

    @Query("SELECT COUNT(*) FROM notes WHERE deletedAt IS NULL AND inTrash = 0")
    fun getNoteCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: NoteEntity)

    @Update
    suspend fun updateNote(note: NoteEntity)

    @Query("UPDATE notes SET inTrash = 1, updatedAt = :timestamp, syncState = 'PENDING_UPDATE' WHERE id = :id")
    suspend fun moveToTrash(id: String, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE notes SET inTrash = 0, updatedAt = :timestamp, syncState = 'PENDING_UPDATE' WHERE id = :id")
    suspend fun restoreFromTrash(id: String, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE notes SET deletedAt = :timestamp, syncState = 'PENDING_DELETE' WHERE id = :id")
    suspend fun softDeleteNote(id: String, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE notes SET deletedAt = :timestamp, syncState = 'PENDING_DELETE' WHERE inTrash = 1")
    suspend fun emptyTrash(timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE notes SET syncState = 'PENDING_UPDATE' WHERE deletedAt IS NULL AND syncState = 'SYNCED'")
    suspend fun markAllSyncedAsPending()

    @Delete
    suspend fun deleteNote(note: NoteEntity)
}
