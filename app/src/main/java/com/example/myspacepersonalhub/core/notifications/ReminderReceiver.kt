package com.example.myspacepersonalhub.core.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getStringExtra(NotificationHelper.EXTRA_ITEM_ID) ?: return
        val title = intent.getStringExtra(NotificationHelper.EXTRA_TITLE) ?: "Reminder"
        val message = intent.getStringExtra(NotificationHelper.EXTRA_MESSAGE) ?: "You have a scheduled reminder."

        NotificationHelper.showReminderNotification(
            context = context,
            notificationId = id.hashCode(),
            title = title,
            message = message
        )
    }
}
