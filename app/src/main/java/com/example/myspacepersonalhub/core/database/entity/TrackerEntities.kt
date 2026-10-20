package com.example.myspacepersonalhub.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.myspacepersonalhub.core.model.GoalStatus
import com.example.myspacepersonalhub.core.model.SyncState
import java.util.UUID

@Entity(tableName = "habits")
data class HabitEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val description: String? = null,
    val scheduleDays: List<Int> = listOf(1, 2, 3, 4, 5, 6, 7), // 1=Mon, 7=Sun
    val startDate: String, // YYYY-MM-DD
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val deletedAt: Long? = null,
    val syncState: SyncState = SyncState.PENDING_CREATE,
    val ownerUid: String? = null
)

@Entity(
    tableName = "habit_completions",
    indices = [Index(value = ["habitId", "completionDate"], unique = true)]
)
data class HabitCompletionEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val habitId: String,
    val completionDate: String, // YYYY-MM-DD (unique per habitId + date)
    val isCompleted: Boolean = true,
    val note: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "goals")
data class GoalEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String,
    val description: String? = null,
    val targetValue: Double,
    val currentProgress: Double = 0.0,
    val unit: String = "%",
    val targetDate: String? = null, // YYYY-MM-DD
    val status: GoalStatus = GoalStatus.IN_PROGRESS,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val deletedAt: Long? = null,
    val syncState: SyncState = SyncState.PENDING_CREATE,
    val ownerUid: String? = null
)
