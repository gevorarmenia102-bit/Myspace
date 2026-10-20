package com.example.myspacepersonalhub

import com.example.myspacepersonalhub.core.database.dao.TrackerDao
import com.example.myspacepersonalhub.core.database.entity.HabitCompletionEntity
import com.example.myspacepersonalhub.data.repository.TrackerRepository
import org.junit.Assert.*
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class TrackerCalculationTest {

    private class FakeTrackerDao : TrackerDao {
        override fun getAllHabits() = kotlinx.coroutines.flow.flowOf(emptyList<com.example.myspacepersonalhub.core.database.entity.HabitEntity>())
        override fun getHabitById(id: String) = kotlinx.coroutines.flow.flowOf(null)
        override suspend fun insertHabit(habit: com.example.myspacepersonalhub.core.database.entity.HabitEntity) {}
        override suspend fun updateHabit(habit: com.example.myspacepersonalhub.core.database.entity.HabitEntity) {}
        override suspend fun softDeleteHabit(id: String, timestamp: Long) {}
        override fun getHabitCompletions(habitId: String) = kotlinx.coroutines.flow.flowOf(emptyList<HabitCompletionEntity>())
        override fun getCompletionsForDate(date: String) = kotlinx.coroutines.flow.flowOf(emptyList<HabitCompletionEntity>())
        override suspend fun getCompletion(habitId: String, date: String) = null
        override suspend fun insertHabitCompletion(completion: HabitCompletionEntity) {}
        override suspend fun deleteHabitCompletion(habitId: String, date: String) {}
        override fun getAllGoals() = kotlinx.coroutines.flow.flowOf(emptyList<com.example.myspacepersonalhub.core.database.entity.GoalEntity>())
        override fun getGoalById(id: String) = kotlinx.coroutines.flow.flowOf(null)
        override suspend fun insertGoal(goal: com.example.myspacepersonalhub.core.database.entity.GoalEntity) {}
        override suspend fun updateGoal(goal: com.example.myspacepersonalhub.core.database.entity.GoalEntity) {}
        override suspend fun updateGoalProgress(id: String, progress: Double, timestamp: Long) {}
        override suspend fun softDeleteGoal(id: String, timestamp: Long) {}
        override suspend fun deleteGoal(goal: com.example.myspacepersonalhub.core.database.entity.GoalEntity) {}
    }

    @Test
    fun testStreakCalculationConsecutiveDays() {
        val repo = TrackerRepository(FakeTrackerDao())
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)

        val cal = Calendar.getInstance()
        val todayStr = sdf.format(cal.time)

        cal.add(Calendar.DAY_OF_YEAR, -1)
        val yesterdayStr = sdf.format(cal.time)

        cal.add(Calendar.DAY_OF_YEAR, -1)
        val twoDaysAgoStr = sdf.format(cal.time)

        val completions = listOf(
            HabitCompletionEntity(habitId = "h1", completionDate = todayStr),
            HabitCompletionEntity(habitId = "h1", completionDate = yesterdayStr),
            HabitCompletionEntity(habitId = "h1", completionDate = twoDaysAgoStr)
        )

        val streak = repo.calculateStreak(completions)
        assertEquals(3, streak)
    }

    @Test
    fun testStreakBrokenWhenDaysMissed() {
        val repo = TrackerRepository(FakeTrackerDao())
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)

        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -5)
        val fiveDaysAgoStr = sdf.format(cal.time)

        val completions = listOf(
            HabitCompletionEntity(habitId = "h1", completionDate = fiveDaysAgoStr)
        )

        val streak = repo.calculateStreak(completions)
        assertEquals(0, streak)
    }
}
