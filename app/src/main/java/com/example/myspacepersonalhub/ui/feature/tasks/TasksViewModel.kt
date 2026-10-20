package com.example.myspacepersonalhub.ui.feature.tasks

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myspacepersonalhub.core.database.entity.TaskEntity
import com.example.myspacepersonalhub.core.model.Priority
import com.example.myspacepersonalhub.core.model.TaskStatus
import com.example.myspacepersonalhub.data.repository.TaskRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class TaskViewFilter {
    ALL,
    TODAY,
    UPCOMING,
    COMPLETED
}

data class TasksUiState(
    val allTasks: List<TaskEntity> = emptyList(),
    val filteredTasks: List<TaskEntity> = emptyList(),
    val viewFilter: TaskViewFilter = TaskViewFilter.ALL,
    val selectedPriority: Priority? = null,
    val searchQuery: String = "",
    val isLoading: Boolean = false
)

class TasksViewModel(private val taskRepository: TaskRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(TasksUiState())
    val uiState: StateFlow<TasksUiState> = _uiState.asStateFlow()

    init {
        loadTasks()
    }

    private fun loadTasks() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            taskRepository.getAllTasks().collect { tasksList ->
                _uiState.update { state ->
                    val filtered = applyFilter(tasksList, state.viewFilter, state.selectedPriority, state.searchQuery)
                    state.copy(
                        allTasks = tasksList,
                        filteredTasks = filtered,
                        isLoading = false
                    )
                }
            }
        }
    }

    fun setViewFilter(filter: TaskViewFilter) {
        _uiState.update { state ->
            val filtered = applyFilter(state.allTasks, filter, state.selectedPriority, state.searchQuery)
            state.copy(viewFilter = filter, filteredTasks = filtered)
        }
    }

    fun setPriorityFilter(priority: Priority?) {
        _uiState.update { state ->
            val newP = if (state.selectedPriority == priority) null else priority
            val filtered = applyFilter(state.allTasks, state.viewFilter, newP, state.searchQuery)
            state.copy(selectedPriority = newP, filteredTasks = filtered)
        }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.update { state ->
            val filtered = applyFilter(state.allTasks, state.viewFilter, state.selectedPriority, query)
            state.copy(searchQuery = query, filteredTasks = filtered)
        }
    }

    fun toggleTaskComplete(task: TaskEntity, context: Context) {
        viewModelScope.launch {
            taskRepository.toggleTaskComplete(task, context)
        }
    }

    fun deleteTask(task: TaskEntity, context: Context) {
        viewModelScope.launch {
            taskRepository.deleteTask(task, context)
        }
    }

    fun getTaskById(id: String): Flow<TaskEntity?> = taskRepository.getTaskById(id)

    fun saveTask(
        task: TaskEntity,
        context: Context,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            val result = taskRepository.saveTask(task, context)
            if (result.isSuccess) {
                onSuccess()
            } else {
                onError(result.exceptionOrNull()?.message ?: "Save failed")
            }
        }
    }

    private fun applyFilter(
        tasks: List<TaskEntity>,
        filter: TaskViewFilter,
        priority: Priority?,
        query: String
    ): List<TaskEntity> {
        val now = System.currentTimeMillis()
        val calendar = java.util.Calendar.getInstance()
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 23)
        calendar.set(java.util.Calendar.MINUTE, 59)
        calendar.set(java.util.Calendar.SECOND, 59)
        val endOfToday = calendar.timeInMillis

        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
        calendar.set(java.util.Calendar.MINUTE, 0)
        calendar.set(java.util.Calendar.SECOND, 0)
        val startOfToday = calendar.timeInMillis

        var result = when (filter) {
            TaskViewFilter.ALL -> tasks.filter { it.status != TaskStatus.DONE }
            TaskViewFilter.TODAY -> tasks.filter {
                it.status != TaskStatus.DONE && it.dueAt != null && it.dueAt in startOfToday..endOfToday
            }
            TaskViewFilter.UPCOMING -> tasks.filter {
                it.status != TaskStatus.DONE && it.dueAt != null && it.dueAt > endOfToday
            }
            TaskViewFilter.COMPLETED -> tasks.filter { it.status == TaskStatus.DONE }
        }

        if (priority != null) {
            result = result.filter { it.priority == priority }
        }

        if (query.isNotBlank()) {
            val q = query.trim().lowercase()
            result = result.filter {
                it.title.lowercase().contains(q) ||
                (it.description?.lowercase()?.contains(q) == true) ||
                (it.category?.lowercase()?.contains(q) == true)
            }
        }

        return result
    }
}
