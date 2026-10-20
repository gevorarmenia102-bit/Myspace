package com.example.myspacepersonalhub.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.myspacepersonalhub.core.model.QuickAccessType
import com.example.myspacepersonalhub.core.model.SyncState
import java.util.UUID

@Entity(tableName = "quick_access")
data class QuickAccessEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String,
    val itemType: QuickAccessType,
    val value: String,
    val description: String? = null,
    val category: String = "Favorites",
    val isPinned: Boolean = false,
    val sortOrder: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val deletedAt: Long? = null,
    val syncState: SyncState = SyncState.PENDING_CREATE,
    val ownerUid: String? = null
)
