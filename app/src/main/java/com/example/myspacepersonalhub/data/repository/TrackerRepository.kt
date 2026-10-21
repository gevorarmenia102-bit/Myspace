package com.example.myspacepersonalhub.data.repository

import com.example.myspacepersonalhub.core.database.dao.TrackerDao
import com.example.myspacepersonalhub.core.database.entity.GoalEntity
import com.example.myspacepersonalhub.core.database.entity.HabitCompletionEntity
import com.example.myspacepersonalhub.core.database.entity.HabitEntity
import com.example.myspacepersonalhub.core.model.GoalStatus
import com.example.myspacepersonalhub.core.model.SyncState
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class TrackerRepository(
    private val trackerDao: TrackerDao,
    private val onDataChanged: (() -> Unit)? = null
) {

    // Habits
    fun getAllHabits(): Flow<List<HabitEntity>> = trackerDao.getAllHabits()

    suspend fun saveHabit(habit: HabitEntity): Result<Unit> {
        val trimmedName = habit.name.trim()
        if (trimmedName.isEmpty()) {
            return Result.failure(IllegalArgumentException("Habit name cannot be blank"))
        }
        val updated = habit.copy(
            name = trimmedName,
            updatedAt = System.currentTimeMillis(),
            syncState = if (habit.syncState == SyncState.SYNCED) SyncState.PENDING_UPDATE else habit.syncState
        )
        trackerDao.insertHabit(updated)
        onDataChanged?.invoke()
        return Result.success(Unit)
    }

    suspend fun deleteHabit(id: String) {
        val existing = trackerDao.getHabitByIdDirect(id)
        if (existing != null) {
            trackerDao.updateHabit(
                existing.copy(
                    deletedAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis(),
                    syncState = SyncState.PENDING_DELETE
                )
            )
        } else {
            trackerDao.softDeleteHabit(id)
        }
        onDataChanged?.invoke()
    }

    fun getHabitCompletions(habitId: String): Flow<List<HabitCompletionEntity>> =
        trackerDao.getHabitCompletions(habitId)

    fun getCompletionsForDate(date: String): Flow<List<HabitCompletionEntity>> =
        trackerDao.getCompletionsForDate(date)

    suspend fun toggleHabitCompletion(habitId: String, date: String): Boolean {
        val existing = trackerDao.getCompletion(habitId, date)
        return if (existing != null) {
            trackerDao.deleteHabitCompletion(habitId, date)
            onDataChanged?.invoke()
            false
        } else {
            val completion = HabitCompletionEntity(
                habitId = habitId,
                completionDate = date,
                isCompleted = true,
                updatedAt = System.currentTimeMillis()
            )
            trackerDao.insertHabitCompletion(completion)
            onDataChanged?.invoke()
            true
        }
    }

    fun calculateStreak(completions: List<HabitCompletionEntity>): Int {
        if (completions.isEmpty()) return 0
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val dates = completions.mapNotNull {
            try { sdf.parse(it.completionDate) } catch (_: Exception) { null }
        }.sortedDescending()

        if (dates.isEmpty()) return 0

        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val today = cal.time

        cal.add(Calendar.DAY_OF_YEAR, -1)
        val yesterday = cal.time

        // Streak must be active either today or yesterday
        val latest = dates.first()
        val latestCal = Calendar.getInstance().apply { time = latest }
        val isToday = sdf.format(latest) == sdf.format(today)
        val isYesterday = sdf.format(latest) == sdf.format(yesterday)

        if (!isToday && !isYesterday) return 0

        var streak = 1
        var previousCal = latestCal

        for (i in 1 until dates.size) {
            val currentCal = Calendar.getInstance().apply { time = dates[i] }
            val checkCal = (previousCal.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -1) }
            if (sdf.format(currentCal.time) == sdf.format(checkCal.time)) {
                streak++
                previousCal = currentCal
            } else if (sdf.format(currentCal.time) == sdf.format(previousCal.time)) {
                // Same day, continue
            } else {
                break
            }
        }
        return streak
    }

    // Goals
    fun getAllGoals(): Flow<List<GoalEntity>> = trackerDao.getAllGoals()

    suspend fun saveGoal(goal: GoalEntity): Result<Unit> {
        val trimmedTitle = goal.title.trim()
        if (trimmedTitle.isEmpty()) {
            return Result.failure(IllegalArgumentException("Goal title cannot be blank"))
        }
        if (goal.targetValue <= 0) {
            return Result.failure(IllegalArgumentException("Target value must be greater than zero"))
        }
        val updated = goal.copy(
            title = trimmedTitle,
            updatedAt = System.currentTimeMillis(),
            syncState = if (goal.syncState == SyncState.SYNCED) SyncState.PENDING_UPDATE else goal.syncState
        )
        trackerDao.insertGoal(updated)
        onDataChanged?.invoke()
        return Result.success(Unit)
    }

    suspend fun updateGoalProgress(id: String, progress: Double, target: Double) {
        val status = if (progress >= target) GoalStatus.ACHIEVED else GoalStatus.IN_PROGRESS
        val existing = trackerDao.getGoalByIdDirect(id)
        if (existing != null) {
            trackerDao.updateGoal(
                existing.copy(
                    currentProgress = progress,
                    status = status,
                    updatedAt = System.currentTimeMillis(),
                    syncState = SyncState.PENDING_UPDATE
                )
            )
        } else {
            trackerDao.updateGoalProgress(id, progress)
        }
        onDataChanged?.invoke()
    }

    suspend fun deleteGoal(id: String) {
        val existing = trackerDao.getGoalByIdDirect(id)
        if (existing != null) {
            trackerDao.updateGoal(
                existing.copy(
                    deletedAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis(),
                    syncState = SyncState.PENDING_DELETE
                )
            )
        } else {
            trackerDao.softDeleteGoal(id)
        }
        onDataChanged?.invoke()
    }
}
