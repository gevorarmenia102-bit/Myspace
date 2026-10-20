package com.example.myspacepersonalhub.core.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.myspacepersonalhub.core.database.MySpaceDatabase
import com.example.myspacepersonalhub.core.datastore.UserPreferencesRepository

class SyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val database = MySpaceDatabase.getDatabase(applicationContext)
        val preferences = UserPreferencesRepository(applicationContext)
        val syncManager = SyncManager(applicationContext, database, preferences)

        if (!syncManager.isFirebaseConfigured() || syncManager.getCurrentUserUid() == null) {
            return Result.success()
        }

        val result = syncManager.syncNow()
        return if (result.isSuccess) {
            Result.success()
        } else {
            Result.retry()
        }
    }
}
