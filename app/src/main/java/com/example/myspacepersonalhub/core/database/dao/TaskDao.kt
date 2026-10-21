package com.example.myspacepersonalhub.core.database.dao

import androidx.room.*
import com.example.myspacepersonalhub.core.database.entity.TaskEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks WHERE deletedAt IS NULL ORDER BY CASE status WHEN 'TODO' THEN 1 WHEN 'IN_PROGRESS' THEN 2 WHEN 'DONE' THEN 3 ELSE 4 END, dueAt ASC, priority DESC")
    fun getAllTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE deletedAt IS NULL AND dueAt >= :startOfDay AND dueAt <= :endOfDay ORDER BY priority DESC, dueAt ASC")
    fun getTodayTasks(startOfDay: Long, endOfDay: Long): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE deletedAt IS NULL AND dueAt > :startOfTomorrow ORDER BY dueAt ASC, priority DESC")
    fun getUpcomingTasks(startOfTomorrow: Long): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE deletedAt IS NULL AND status = 'DONE' ORDER BY completedAt DESC")
    fun getCompletedTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE id = :id AND deletedAt IS NULL")
    fun getTaskById(id: String): Flow<TaskEntity?>

    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun getTaskByIdDirect(id: String): TaskEntity?

    @Query("SELECT * FROM tasks WHERE syncState != 'SYNCED'")
    suspend fun getPendingSyncTasks(): List<TaskEntity>

    @Query("SELECT COUNT(*) FROM tasks WHERE deletedAt IS NULL AND status != 'DONE'")
    fun getActiveTasksCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity)

    @Update
    suspend fun updateTask(task: TaskEntity)

    @Query("UPDATE tasks SET status = :status, completedAt = :completedAt, updatedAt = :updatedAt, syncState = 'PENDING_UPDATE' WHERE id = :id")
    suspend fun updateTaskStatus(id: String, status: String, completedAt: Long?, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE tasks SET deletedAt = :timestamp, syncState = 'PENDING_DELETE' WHERE id = :id")
    suspend fun softDeleteTask(id: String, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE tasks SET syncState = 'PENDING_UPDATE' WHERE deletedAt IS NULL AND syncState = 'SYNCED'")
    suspend fun markAllSyncedAsPending()

    @Delete
    suspend fun deleteTask(task: TaskEntity)
}
