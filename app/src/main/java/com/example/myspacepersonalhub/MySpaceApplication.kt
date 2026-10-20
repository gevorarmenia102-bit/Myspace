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
                try {
                    val inputStream = assets.open("google-services.json")
                    val jsonStr = inputStream.bufferedReader().use { it.readText() }
                    val jsonObj = org.json.JSONObject(jsonStr)
                    val projectInfo = jsonObj.getJSONObject("project_info")
                    val clientObj = jsonObj.getJSONArray("client").getJSONObject(0)
                    val apiKey = clientObj.getJSONArray("api_key").getJSONObject(0).getString("current_key")
                    val appId = clientObj.getJSONObject("client_info").getString("mobilesdk_app_id")
                    val dbUrl = projectInfo.optString("firebase_url")
                    val projectId = projectInfo.getString("project_id")
                    val storageBucket = projectInfo.optString("storage_bucket")

                    val options = com.google.firebase.FirebaseOptions.Builder()
                        .setApplicationId(appId)
                        .setApiKey(apiKey)
                        .setProjectId(projectId)
                        .apply {
                            if (dbUrl.isNotBlank()) setDatabaseUrl(dbUrl)
                            if (storageBucket.isNotBlank()) setStorageBucket(storageBucket)
                        }
                        .build()

                    FirebaseApp.initializeApp(this, options)
                } catch (_: Exception) {
                    FirebaseApp.initializeApp(this)
                }
            }
        } catch (_: Exception) {
            // Safe fallback when google-services.json is not configured
        }

        // 3. Initialize Core Singletons & Repositories
        database = MySpaceDatabase.getDatabase(this)
        preferencesRepository = UserPreferencesRepository(this)
        cryptoManager = CryptoManager()
        syncManager = SyncManager(this, database, preferencesRepository)

        contactRepository = ContactRepository(database.contactDao())
        noteRepository = NoteRepository(database.noteDao())
        taskRepository = TaskRepository(database.taskDao())
        moneyRepository = MoneyRepository(database.moneyDao())
        vaultRepository = VaultRepository(database.vaultDao(), cryptoManager)
        trackerRepository = TrackerRepository(database.trackerDao())
        quickAccessRepository = QuickAccessRepository(database.quickAccessDao())
        backupRepository = BackupRepository(database)

        // 4. Schedule Background Periodic Sync via WorkManager
        schedulePeriodicSync()

        // 5. Start Realtime Cloud Sync if enabled
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            val prefs = preferencesRepository.userPreferencesFlow.first()
            if (prefs.firebaseDatabaseUrl.isNullOrBlank()) {
                preferencesRepository.setFirebaseDatabaseUrl("https://myspace-e0362-default-rtdb.firebaseio.com")
            }
            if (prefs.githubOwner.isNullOrBlank() || prefs.githubRepo.isNullOrBlank()) {
                preferencesRepository.setGitHubConfig(
                    token = prefs.githubToken ?: "",
                    owner = "gevorarmenia102-bit",
                    repo = "Myspace",
                    branch = "main"
                )
            }
            if (prefs.realtimeSyncEnabled) {
                syncManager.startRealtimeSync()
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
