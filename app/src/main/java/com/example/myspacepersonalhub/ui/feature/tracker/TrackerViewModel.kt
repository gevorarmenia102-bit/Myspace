package com.example.myspacepersonalhub.ui.feature.tracker

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myspacepersonalhub.core.database.entity.GoalEntity
import com.example.myspacepersonalhub.core.database.entity.HabitCompletionEntity
import com.example.myspacepersonalhub.core.database.entity.HabitEntity
import com.example.myspacepersonalhub.data.repository.ContactRepository
import com.example.myspacepersonalhub.data.repository.TaskRepository
import com.example.myspacepersonalhub.data.repository.TrackerRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class HabitWithStreak(
    val habit: HabitEntity,
    val streak: Int,
    val isCompletedToday: Boolean
)

data class CalendarEventItem(
    val title: String,
    val subtitle: String,
    val dateStr: String,
    val type: String // "TASK", "BIRTHDAY", "HABIT"
)

data class TrackerUiState(
    val habits: List<HabitWithStreak> = emptyList(),
    val goals: List<GoalEntity> = emptyList(),
    val upcomingEvents: List<CalendarEventItem> = emptyList(),
    val selectedTab: Int = 0, // 0: Habits, 1: Goals, 2: Calendar
    val isLoading: Boolean = false
)

class TrackerViewModel(
    private val trackerRepository: TrackerRepository,
    private val taskRepository: TaskRepository,
    private val contactRepository: ContactRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TrackerUiState())
    val uiState: StateFlow<TrackerUiState> = _uiState.asStateFlow()

    private val todayDate = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            combine(
                trackerRepository.getAllHabits(),
                trackerRepository.getCompletionsForDate(todayDate),
                trackerRepository.getAllGoals(),
                taskRepository.getUpcomingTasks(),
                contactRepository.getAllContacts()
            ) { habits, todayCompletions, goals, tasks, contacts ->
                val todayCompletedHabitIds = todayCompletions.map { it.habitId }.toSet()

                val habitsWithStreak = habits.map { h ->
                    HabitWithStreak(
                        habit = h,
                        streak = 0, // Updated asynchronously or base
                        isCompletedToday = todayCompletedHabitIds.contains(h.id)
                    )
                }

                // Compile upcoming calendar events
                val events = mutableListOf<CalendarEventItem>()
                val sdf = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())

                tasks.forEach { t ->
                    if (t.dueAt != null) {
                        events.add(
                            CalendarEventItem(
                                title = t.title,
                                subtitle = "Task Due • Priority: ${t.priority.name}",
                                dateStr = sdf.format(Date(t.dueAt)),
                                type = "TASK"
                            )
                        )
                    }
                }

                contacts.forEach { c ->
                    if (!c.birthday.isNullOrBlank()) {
                        events.add(
                            CalendarEventItem(
                                title = "${c.displayName}'s Birthday",
                                subtitle = "Contact Birthday",
                                dateStr = c.birthday,
                                type = "BIRTHDAY"
                            )
                        )
                    }
                }

                Triple(habitsWithStreak, goals, events)
            }.collect { (habits, goals, events) ->
                _uiState.update {
                    it.copy(
                        habits = habits,
                        goals = goals,
                        upcomingEvents = events,
                        isLoading = false
                    )
                }
            }
        }
    }

    fun selectTab(index: Int) {
        _uiState.update { it.copy(selectedTab = index) }
    }

    fun toggleHabitToday(habitId: String) {
        viewModelScope.launch {
            trackerRepository.toggleHabitCompletion(habitId, todayDate)
        }
    }

    fun saveHabit(habit: HabitEntity, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            val result = trackerRepository.saveHabit(habit)
            if (result.isSuccess) onSuccess() else onError(result.exceptionOrNull()?.message ?: "Save error")
        }
    }

    fun deleteHabit(id: String) {
        _uiState.update { state ->
            state.copy(habits = state.habits.filterNot { it.habit.id == id })
        }
        viewModelScope.launch {
            trackerRepository.deleteHabit(id)
        }
    }

    fun saveGoal(goal: GoalEntity, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            val result = trackerRepository.saveGoal(goal)
            if (result.isSuccess) onSuccess() else onError(result.exceptionOrNull()?.message ?: "Save error")
        }
    }

    fun updateGoalProgress(goal: GoalEntity, newProgress: Double) {
        viewModelScope.launch {
            trackerRepository.updateGoalProgress(goal.id, newProgress, goal.targetValue)
        }
    }

    fun deleteGoal(id: String) {
        _uiState.update { state ->
            state.copy(goals = state.goals.filterNot { it.id == id })
        }
        viewModelScope.launch {
            trackerRepository.deleteGoal(id)
        }
    }
}
