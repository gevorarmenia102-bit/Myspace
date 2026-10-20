package com.example.myspacepersonalhub.core.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.myspacepersonalhub.core.database.MySpaceDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = MySpaceDatabase.getDatabase(context)
                    val now = System.currentTimeMillis()

                    // Reschedule future task reminders
                    val tasks = db.taskDao().getAllTasks().first()
                    tasks.forEach { task ->
                        val reminder = task.reminderAt ?: task.dueAt
                        if (reminder != null && reminder > now && task.status.name != "DONE") {
                            NotificationHelper.scheduleReminder(
                                context = context,
                                id = task.id,
                                title = "Task Due: ${task.title}",
                                message = task.description ?: "Scheduled task is due now.",
                                triggerAtMillis = reminder
                            )
                        }
                    }

                    // Reschedule future note reminders
                    val notes = db.noteDao().getAllNotes().first()
                    notes.forEach { note ->
                        val reminder = note.reminderAt
                        if (reminder != null && reminder > now) {
                            NotificationHelper.scheduleReminder(
                                context = context,
                                id = note.id,
                                title = "Note Reminder: ${note.title}",
                                message = note.body.take(100),
                                triggerAtMillis = reminder
                            )
                        }
                    }
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
