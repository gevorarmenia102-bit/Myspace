package com.example.myspacepersonalhub

import android.app.Application
import androidx.work.*
import com.example.myspacepersonalhub.core.database.MySpaceDatabase
import com.example.myspacepersonalhub.core.datastore.UserPreferencesRepository
import com.example.myspacepersonalhub.core.notifications.NotificationHelper
import com.example.myspacepersonalhub.core.security.CryptoManager
import com.example.myspacepersonalhub.core.sync.SyncManager
import com.example.myspacepersonalhub.core.sync.SyncWorker
import com.example.myspacepersonalhub.data.repository.*
import com.google.firebase.FirebaseApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class MySpaceApplication : Application() {

    lateinit var database: MySpaceDatabase
        private set

    lateinit var preferencesRepository: UserPreferencesRepository
        private set

    lateinit var cryptoManager: CryptoManager
        private set

    lateinit var syncManager: SyncManager
        private set

    lateinit var contactRepository: ContactRepository
        private set

    lateinit var noteRepository: NoteRepository
        private set

    lateinit var taskRepository: TaskRepository
        private set

    lateinit var moneyRepository: MoneyRepository
        private set

    lateinit var vaultRepository: VaultRepository
        private set

    lateinit var trackerRepository: TrackerRepository
        private set

    lateinit var quickAccessRepository: QuickAccessRepository
        private set

    lateinit var backupRepository: BackupRepository
        private set

    override fun onCreate() {
        super.onCreate()

        // 1. Initialize Notification Channels
        NotificationHelper.createNotificationChannels(this)

        // 2. Safe Firebase initialization
        try {
            if (FirebaseApp.getApps(this).isEmpty()) {
                val options = com.google.firebase.FirebaseOptions.Builder()
                    .setApplicationId(com.example.myspacepersonalhub.core.sync.DefaultCloudConfig.DEFAULT_FIREBASE_APP_ID)
                    .setApiKey(com.example.myspacepersonalhub.core.sync.DefaultCloudConfig.DEFAULT_FIREBASE_API_KEY)
                    .setProjectId(com.example.myspacepersonalhub.core.sync.DefaultCloudConfig.DEFAULT_FIREBASE_PROJECT_ID)
                    .setGcmSenderId(com.example.myspacepersonalhub.core.sync.DefaultCloudConfig.DEFAULT_FIREBASE_GCM_SENDER_ID)
                    .setDatabaseUrl(com.example.myspacepersonalhub.core.sync.DefaultCloudConfig.DEFAULT_FIREBASE_URL)
                    .build()
                FirebaseApp.initializeApp(this, options)
            }
        } catch (_: Exception) {
            try { FirebaseApp.initializeApp(this) } catch (_: Exception) {}
        }

        // 3. Initialize Core Singletons & Repositories
        database = MySpaceDatabase.getDatabase(this)
        preferencesRepository = UserPreferencesRepository(this)
        cryptoManager = CryptoManager()
        syncManager = SyncManager(this, database, preferencesRepository)

        contactRepository = ContactRepository(database.contactDao(), onDataChanged = { syncManager.triggerRealtimePush() })
        noteRepository = NoteRepository(database.noteDao(), onDataChanged = { syncManager.triggerRealtimePush() })
        taskRepository = TaskRepository(database.taskDao(), onDataChanged = { syncManager.triggerRealtimePush() })
        moneyRepository = MoneyRepository(database.moneyDao(), onDataChanged = { syncManager.triggerRealtimePush() })
        vaultRepository = VaultRepository(database.vaultDao(), cryptoManager)
        trackerRepository = TrackerRepository(database.trackerDao())
        quickAccessRepository = QuickAccessRepository(database.quickAccessDao())
        backupRepository = BackupRepository(database)

        // 4. Schedule Background Periodic Sync via WorkManager
        schedulePeriodicSync()

        // 5. Start Realtime Cloud Sync and pull/push on startup
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            val prefs = preferencesRepository.userPreferencesFlow.first()
            if (prefs.firebaseDatabaseUrl.isNullOrBlank()) {
                preferencesRepository.setFirebaseDatabaseUrl(com.example.myspacepersonalhub.core.sync.DefaultCloudConfig.DEFAULT_FIREBASE_URL)
            }
            val currentToken = prefs.githubToken?.ifBlank { null } ?: com.example.myspacepersonalhub.core.sync.DefaultCloudConfig.getDefaultGitHubToken()
            val currentOwner = prefs.githubOwner?.ifBlank { null } ?: com.example.myspacepersonalhub.core.sync.DefaultCloudConfig.DEFAULT_GITHUB_OWNER
            val currentRepo = prefs.githubRepo?.ifBlank { null } ?: com.example.myspacepersonalhub.core.sync.DefaultCloudConfig.DEFAULT_GITHUB_REPO
            val currentBranch = prefs.githubBranch.ifBlank { com.example.myspacepersonalhub.core.sync.DefaultCloudConfig.DEFAULT_GITHUB_BRANCH }
            if (prefs.githubToken.isNullOrBlank() || prefs.githubOwner.isNullOrBlank() || prefs.githubRepo.isNullOrBlank()) {
                preferencesRepository.setGitHubConfig(
                    token = currentToken,
                    owner = currentOwner,
                    repo = currentRepo,
                    branch = currentBranch
                )
            }

            // One-time local data migration to unified hub
            if (!prefs.hubMigrationV2) {
                try {
                    database.contactDao().markAllSyncedAsPending()
                    database.noteDao().markAllSyncedAsPending()
                    database.taskDao().markAllSyncedAsPending()
                    database.moneyDao().markAllSyncedAsPending()
                    preferencesRepository.setHubMigrationV2Done(true)
                } catch (e: Exception) {
                    android.util.Log.w("MySpaceApp", "Migration mark failed: ${e.message}")
                }
            }

            if (prefs.realtimeSyncEnabled) {
                syncManager.startRealtimeSync()
            }

            // Immediately pull any new remote changes and push any pending local items
            try {
                syncManager.syncNow()
            } catch (e: Exception) {
                android.util.Log.w("MySpaceApp", "Startup syncNow failed: ${e.message}")
            }
        }
    }

    private fun schedulePeriodicSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncWorkRequest = PeriodicWorkRequestBuilder<SyncWorker>(6, TimeUnit.HOURS)
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.MINUTES)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "MySpacePeriodicSync",
            ExistingPeriodicWorkPolicy.KEEP,
            syncWorkRequest
        )
    }
}
