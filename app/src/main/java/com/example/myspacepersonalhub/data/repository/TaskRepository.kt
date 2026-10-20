package com.example.myspacepersonalhub.data.repository

import android.content.Context
import com.example.myspacepersonalhub.core.database.dao.TaskDao
import com.example.myspacepersonalhub.core.database.entity.TaskEntity
import com.example.myspacepersonalhub.core.model.SyncState
import com.example.myspacepersonalhub.core.model.TaskStatus
import com.example.myspacepersonalhub.core.notifications.NotificationHelper
import kotlinx.coroutines.flow.Flow
import java.util.Calendar

class TaskRepository(
    private val taskDao: TaskDao,
    private val onDataChanged: (() -> Unit)? = null
) {

    fun getAllTasks(): Flow<List<TaskEntity>> = taskDao.getAllTasks()

    fun getTodayTasks(): Flow<List<TaskEntity>> {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startOfDay = calendar.timeInMillis
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        val endOfDay = calendar.timeInMillis
        return taskDao.getTodayTasks(startOfDay, endOfDay)
    }

    fun getUpcomingTasks(): Flow<List<TaskEntity>> {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        val endOfToday = calendar.timeInMillis
        return taskDao.getUpcomingTasks(endOfToday)
    }

    fun getCompletedTasks(): Flow<List<TaskEntity>> = taskDao.getCompletedTasks()

    fun getTaskById(id: String): Flow<TaskEntity?> = taskDao.getTaskById(id)

    suspend fun saveTask(task: TaskEntity, context: Context): Result<Unit> {
        val trimmedTitle = task.title.trim()
        if (trimmedTitle.isEmpty()) {
            return Result.failure(IllegalArgumentException("Task title cannot be blank"))
        }

        val updated = task.copy(
            title = trimmedTitle,
            description = task.description?.trim()?.ifBlank { null },
            category = task.category?.trim()?.ifBlank { "Personal" },
            updatedAt = System.currentTimeMillis(),
            syncState = if (task.syncState == SyncState.SYNCED) SyncState.PENDING_UPDATE else task.syncState
        )
        taskDao.insertTask(updated)

        // Manage Reminder
        val reminder = updated.reminderAt ?: updated.dueAt
        if (reminder != null && reminder > System.currentTimeMillis() && updated.status != TaskStatus.DONE) {
            NotificationHelper.scheduleReminder(
                context = context,
                id = updated.id,
                title = "Task: ${updated.title}",
                message = updated.description ?: "Scheduled task is due.",
                triggerAtMillis = reminder
            )
        } else {
            NotificationHelper.cancelReminder(context, updated.id)
        }

        onDataChanged?.invoke()
        return Result.success(Unit)
    }

    suspend fun toggleTaskComplete(task: TaskEntity, context: Context) {
        val isNowDone = task.status != TaskStatus.DONE
        val newStatus = if (isNowDone) TaskStatus.DONE else TaskStatus.TODO
        val completedAt = if (isNowDone) System.currentTimeMillis() else null

        taskDao.updateTaskStatus(
            id = task.id,
            status = newStatus.name,
            completedAt = completedAt,
            updatedAt = System.currentTimeMillis()
        )

        if (isNowDone) {
            NotificationHelper.cancelReminder(context, task.id)
            // Handle recurrence
            if (!task.recurrenceRule.isNullOrBlank() && task.dueAt != null) {
                val calendar = Calendar.getInstance().apply { timeInMillis = task.dueAt }
                when (task.recurrenceRule.uppercase()) {
                    "DAILY" -> calendar.add(Calendar.DAY_OF_YEAR, 1)
                    "WEEKLY" -> calendar.add(Calendar.WEEK_OF_YEAR, 1)
                    "MONTHLY" -> calendar.add(Calendar.MONTH, 1)
                }
                val nextDue = calendar.timeInMillis
                val nextTask = task.copy(
                    id = java.util.UUID.randomUUID().toString(),
                    status = TaskStatus.TODO,
                    dueAt = nextDue,
                    reminderAt = if (task.reminderAt != null) nextDue else null,
                    completedAt = null,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis(),
                    syncState = SyncState.PENDING_CREATE
                )
                taskDao.insertTask(nextTask)
            }
        }
        onDataChanged?.invoke()
    }

    suspend fun deleteTask(task: TaskEntity, context: Context) {
        NotificationHelper.cancelReminder(context, task.id)
        taskDao.softDeleteTask(task.id)
        onDataChanged?.invoke()
    }

    fun getActiveTasksCount(): Flow<Int> = taskDao.getActiveTasksCount()
}
