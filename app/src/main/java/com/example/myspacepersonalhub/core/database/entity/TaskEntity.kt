package com.example.myspacepersonalhub.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.myspacepersonalhub.core.model.Priority
import com.example.myspacepersonalhub.core.model.SyncState
import com.example.myspacepersonalhub.core.model.TaskStatus
import java.util.UUID

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String,
    val description: String? = null,
    val dueAt: Long? = null,
    val reminderAt: Long? = null,
    val priority: Priority = Priority.MEDIUM,
    val status: TaskStatus = TaskStatus.TODO,
    val recurrenceRule: String? = null, // NONE, DAILY, WEEKLY, MONTHLY
    val category: String? = "Personal",
    val tags: List<String> = emptyList(),
    val completedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val deletedAt: Long? = null,
    val syncState: SyncState = SyncState.PENDING_CREATE,
    val ownerUid: String? = null
)
