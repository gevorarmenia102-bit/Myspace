package com.example.myspacepersonalhub.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.myspacepersonalhub.core.model.SyncState
import java.util.UUID

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String,
    val body: String,
    val category: String = "General",
    val tags: List<String> = emptyList(),
    val isJournal: Boolean = false,
    val journalMood: String? = null,
    val journalDate: String? = null,
    val isPinned: Boolean = false,
    val isFavorite: Boolean = false,
    val isArchived: Boolean = false,
    val inTrash: Boolean = false,
    val reminderAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val deletedAt: Long? = null,
    val syncState: SyncState = SyncState.PENDING_CREATE,
    val ownerUid: String? = null
)
