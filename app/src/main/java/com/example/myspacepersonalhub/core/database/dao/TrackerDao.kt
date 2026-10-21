package com.example.myspacepersonalhub.core.database.dao

import androidx.room.*
import com.example.myspacepersonalhub.core.database.entity.GoalEntity
import com.example.myspacepersonalhub.core.database.entity.HabitCompletionEntity
import com.example.myspacepersonalhub.core.database.entity.HabitEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TrackerDao {
    // Habits
    @Query("SELECT * FROM habits WHERE deletedAt IS NULL ORDER BY isActive DESC, createdAt DESC")
    fun getAllHabits(): Flow<List<HabitEntity>>

    @Query("SELECT * FROM habits WHERE id = :id AND deletedAt IS NULL")
    fun getHabitById(id: String): Flow<HabitEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHabit(habit: HabitEntity)

    @Update
    suspend fun updateHabit(habit: HabitEntity)

    @Query("UPDATE habits SET deletedAt = :timestamp, syncState = 'PENDING_DELETE' WHERE id = :id")
    suspend fun softDeleteHabit(id: String, timestamp: Long = System.currentTimeMillis())

    @Query("SELECT * FROM habits WHERE syncState != 'SYNCED'")
    suspend fun getPendingSyncHabits(): List<HabitEntity>

    @Query("SELECT * FROM habits WHERE id = :id")
    suspend fun getHabitByIdDirect(id: String): HabitEntity?

    @Query("UPDATE habits SET syncState = 'PENDING_CREATE' WHERE syncState = 'SYNCED'")
    suspend fun markAllHabitsSyncedAsPending()

    @Query("DELETE FROM habits WHERE id = :id")
    suspend fun deleteHabitDirect(id: String)

    // Habit Completions
    @Query("SELECT * FROM habit_completions WHERE habitId = :habitId ORDER BY completionDate DESC")
    fun getHabitCompletions(habitId: String): Flow<List<HabitCompletionEntity>>

    @Query("SELECT * FROM habit_completions WHERE completionDate = :date")
    fun getCompletionsForDate(date: String): Flow<List<HabitCompletionEntity>>

    @Query("SELECT * FROM habit_completions WHERE habitId = :habitId AND completionDate = :date LIMIT 1")
    suspend fun getCompletion(habitId: String, date: String): HabitCompletionEntity?

    @Query("SELECT * FROM habit_completions")
    suspend fun getAllHabitCompletionsDirect(): List<HabitCompletionEntity>

    @Query("SELECT * FROM habit_completions WHERE id = :id")
    suspend fun getCompletionByIdDirect(id: String): HabitCompletionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHabitCompletion(completion: HabitCompletionEntity)

    @Query("DELETE FROM habit_completions WHERE habitId = :habitId AND completionDate = :date")
    suspend fun deleteHabitCompletion(habitId: String, date: String)

    @Query("DELETE FROM habit_completions WHERE id = :id")
    suspend fun deleteHabitCompletionById(id: String)

    // Goals
    @Query("SELECT * FROM goals WHERE deletedAt IS NULL ORDER BY CASE status WHEN 'IN_PROGRESS' THEN 1 WHEN 'ACHIEVED' THEN 2 ELSE 3 END, createdAt DESC")
    fun getAllGoals(): Flow<List<GoalEntity>>

    @Query("SELECT * FROM goals WHERE id = :id AND deletedAt IS NULL")
    fun getGoalById(id: String): Flow<GoalEntity?>

    @Query("SELECT * FROM goals WHERE id = :id")
    suspend fun getGoalByIdDirect(id: String): GoalEntity?

    @Query("SELECT * FROM goals WHERE syncState != 'SYNCED'")
    suspend fun getPendingSyncGoals(): List<GoalEntity>

    @Query("UPDATE goals SET syncState = 'PENDING_CREATE' WHERE syncState = 'SYNCED'")
    suspend fun markAllGoalsSyncedAsPending()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoal(goal: GoalEntity)

    @Update
    suspend fun updateGoal(goal: GoalEntity)

    @Query("UPDATE goals SET currentProgress = :progress, updatedAt = :timestamp WHERE id = :id")
    suspend fun updateGoalProgress(id: String, progress: Double, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE goals SET deletedAt = :timestamp, syncState = 'PENDING_DELETE' WHERE id = :id")
    suspend fun softDeleteGoal(id: String, timestamp: Long = System.currentTimeMillis())

    @Query("DELETE FROM goals WHERE id = :id")
    suspend fun deleteGoalDirect(id: String)

    @Delete
    suspend fun deleteGoal(goal: GoalEntity)
}
