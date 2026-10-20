package com.example.myspacepersonalhub.data.repository

import com.example.myspacepersonalhub.core.database.dao.NoteDao
import com.example.myspacepersonalhub.core.database.entity.NoteEntity
import com.example.myspacepersonalhub.core.model.SyncState
import kotlinx.coroutines.flow.Flow

class NoteRepository(
    private val noteDao: NoteDao,
    private val onDataChanged: (() -> Unit)? = null
) {

    fun getAllNotes(): Flow<List<NoteEntity>> = noteDao.getAllNotes()

    fun getPinnedNotes(): Flow<List<NoteEntity>> = noteDao.getPinnedNotes()

    fun getJournalEntries(): Flow<List<NoteEntity>> = noteDao.getJournalEntries()

    fun getTrashNotes(): Flow<List<NoteEntity>> = noteDao.getTrashNotes()

    fun searchNotes(query: String): Flow<List<NoteEntity>> {
        val trimmed = query.trim()
        return if (trimmed.isEmpty()) noteDao.getAllNotes() else noteDao.searchNotes(trimmed)
    }

    fun getNoteById(id: String): Flow<NoteEntity?> = noteDao.getNoteById(id)

    suspend fun saveNote(note: NoteEntity): Result<Unit> {
        val trimmedTitle = note.title.trim()
        if (trimmedTitle.isEmpty() && note.body.trim().isEmpty()) {
            return Result.failure(IllegalArgumentException("Note must have a title or body"))
        }

        // Limit maximum body size to protect database
        val safeBody = if (note.body.length > 100_000) note.body.substring(0, 100_000) else note.body

        val updated = note.copy(
            title = if (trimmedTitle.isEmpty()) "Untitled Note" else trimmedTitle,
            body = safeBody,
            category = note.category.trim().ifBlank { "General" },
            updatedAt = System.currentTimeMillis(),
            syncState = if (note.syncState == SyncState.SYNCED) SyncState.PENDING_UPDATE else note.syncState
        )
        noteDao.insertNote(updated)
        onDataChanged?.invoke()
        return Result.success(Unit)
    }

    suspend fun moveToTrash(id: String) {
        noteDao.moveToTrash(id)
        onDataChanged?.invoke()
    }

    suspend fun restoreFromTrash(id: String) {
        noteDao.restoreFromTrash(id)
        onDataChanged?.invoke()
    }

    suspend fun deletePermanently(note: NoteEntity) {
        noteDao.softDeleteNote(note.id)
        onDataChanged?.invoke()
    }

    suspend fun emptyTrash() {
        noteDao.emptyTrash()
        onDataChanged?.invoke()
    }

    fun getNoteCount(): Flow<Int> = noteDao.getNoteCount()
}
