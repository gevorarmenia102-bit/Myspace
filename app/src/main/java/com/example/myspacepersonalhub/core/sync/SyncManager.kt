package com.example.myspacepersonalhub.core.sync

import android.content.Context
import android.provider.Settings
import android.util.Log
import com.example.myspacepersonalhub.core.database.MySpaceDatabase
import com.example.myspacepersonalhub.core.database.entity.ContactEntity
import com.example.myspacepersonalhub.core.database.entity.NoteEntity
import com.example.myspacepersonalhub.core.database.entity.TaskEntity
import com.example.myspacepersonalhub.core.database.entity.TransactionEntity
import com.example.myspacepersonalhub.core.database.entity.BudgetEntity
import com.example.myspacepersonalhub.core.database.entity.VaultItemEntity
import com.example.myspacepersonalhub.core.database.entity.HabitEntity
import com.example.myspacepersonalhub.core.database.entity.HabitCompletionEntity
import com.example.myspacepersonalhub.core.database.entity.GoalEntity
import com.example.myspacepersonalhub.core.database.entity.QuickAccessEntity
import com.example.myspacepersonalhub.core.datastore.UserPreferencesRepository
import com.example.myspacepersonalhub.core.model.*
import com.example.myspacepersonalhub.core.storage.GitHubRepoInfo
import com.example.myspacepersonalhub.core.storage.GitHubStorageManager
import com.example.myspacepersonalhub.data.repository.BackupRepository
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.util.UUID

sealed class CloudSyncStatus {
    object Unconfigured : CloudSyncStatus()
    object NotSignedIn : CloudSyncStatus()
    data class Syncing(val message: String = "Syncing in progress...") : CloudSyncStatus()
    data class Synced(val lastSyncTimestamp: Long) : CloudSyncStatus()
    data class Error(val errorMessage: String) : CloudSyncStatus()
    object Offline : CloudSyncStatus()
}

class SyncManager(
    private val context: Context,
    private val database: MySpaceDatabase,
    private val preferencesRepository: UserPreferencesRepository,
    private val gitHubStorageManager: GitHubStorageManager = GitHubStorageManager(context)
) {
    private val TAG = "SyncManager"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val backupRepository = BackupRepository(database)
    private val json = Json { ignoreUnknownKeys = true }

    private val _syncStatus = MutableStateFlow<CloudSyncStatus>(CloudSyncStatus.Unconfigured)
    val syncStatus: StateFlow<CloudSyncStatus> = _syncStatus.asStateFlow()

    // Realtime listeners map to allow clean detach
    private val activeListeners = mutableMapOf<DatabaseReference, ChildEventListener>()
    private var isRealtimeActive = false

    fun isFirebaseConfigured(): Boolean = true

    suspend fun getEffectiveDatabase(): FirebaseDatabase? = withContext(Dispatchers.IO) {
        val prefs = preferencesRepository.userPreferencesFlow.first()
        val dbUrl = prefs.firebaseDatabaseUrl?.ifBlank { null } ?: DefaultCloudConfig.DEFAULT_FIREBASE_URL
        try {
            if (FirebaseApp.getApps(context).isEmpty()) {
                val options = com.google.firebase.FirebaseOptions.Builder()
                    .setApplicationId(DefaultCloudConfig.DEFAULT_FIREBASE_APP_ID)
                    .setApiKey(DefaultCloudConfig.DEFAULT_FIREBASE_API_KEY)
                    .setProjectId(DefaultCloudConfig.DEFAULT_FIREBASE_PROJECT_ID)
                    .setGcmSenderId(DefaultCloudConfig.DEFAULT_FIREBASE_GCM_SENDER_ID)
                    .setDatabaseUrl(dbUrl)
                    .build()
                FirebaseApp.initializeApp(context, options)
            }
            FirebaseDatabase.getInstance(dbUrl)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get FirebaseDatabase instance", e)
            null
        }
    }

    suspend fun getCurrentUserUid(): String = withContext(Dispatchers.IO) {
        val prefs = preferencesRepository.userPreferencesFlow.first()
        try {
            val user = FirebaseAuth.getInstance().currentUser
            if (user != null) return@withContext user.uid
        } catch (_: Exception) {
        }

        // Multi-device shared Personal Hub:
        // Use the shared Hub Owner / GitHub Username (e.g. "gevorarmenia102-bit")
        // so that all personal phones with the app automatically link to the same real-time user space.
        val effectiveOwner = prefs.githubOwner?.ifBlank { null } ?: DefaultCloudConfig.DEFAULT_GITHUB_OWNER
        if (!effectiveOwner.isNullOrBlank()) {
            return@withContext effectiveOwner.trim().replace(Regex("[.#$\\[\\]]"), "_")
        }

        val androidId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
        androidId ?: "gevorarmenia102-bit"
    }

    suspend fun checkStatus() = withContext(Dispatchers.IO) {
        val db = getEffectiveDatabase()
        if (db == null) {
            _syncStatus.value = CloudSyncStatus.Unconfigured
            return@withContext
        }

        val uid = getCurrentUserUid()
        if (uid == null) {
            _syncStatus.value = CloudSyncStatus.NotSignedIn
        } else {
            val prefs = preferencesRepository.userPreferencesFlow.first()
            _syncStatus.value = if (prefs.lastSyncTimestamp > 0) {
                CloudSyncStatus.Synced(prefs.lastSyncTimestamp)
            } else {
                CloudSyncStatus.Synced(System.currentTimeMillis())
            }
        }
    }

    /**
     * Test connection to GitHub repository
     */
    suspend fun testGitHubConnection(): Result<GitHubRepoInfo> = withContext(Dispatchers.IO) {
        val prefs = preferencesRepository.userPreferencesFlow.first()
        val token = prefs.githubToken
        val owner = prefs.githubOwner
        val repo = prefs.githubRepo

        if (token.isNullOrBlank() || owner.isNullOrBlank() || repo.isNullOrBlank()) {
            return@withContext Result.failure(IllegalArgumentException("GitHub Token, Owner, and Repo must not be empty."))
        }

        gitHubStorageManager.testConnection(token, owner, repo)
    }

    private var debouncePushJob: Job? = null

    /**
     * Trigger immediate asynchronous cloud synchronization for local additions,
     * updates, or deletions. Debounced by 300ms to batch rapid changes.
     */
    fun triggerRealtimePush() {
        scope.launch {
            debouncePushJob?.cancel()
            debouncePushJob = launch {
                delay(300)
                try {
                    syncNow()
                } catch (e: Exception) {
                    Log.w(TAG, "Background realtime push failed: ${e.message}")
                }
            }
        }
    }

    /**
     * Start Realtime bi-directional synchronization via Firebase WebSockets.
     * Propagates changes in under 200ms across all connected devices.
     */
    fun startRealtimeSync() {
        if (isRealtimeActive) return
        scope.launch {
            val db = getEffectiveDatabase() ?: return@launch
            val uid = getCurrentUserUid()
            val userRef = db.reference.child("users").child(uid)

            isRealtimeActive = true
            Log.d(TAG, "Starting Realtime Firebase sync for user $uid")

            // 1. Listen for Realtime Contacts updates
            val contactsRef = userRef.child("contacts")
            val contactListener = object : ChildEventListener {
                override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                    handleRemoteContactChange(snapshot)
                }

                override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                    handleRemoteContactChange(snapshot)
                }

                override fun onChildRemoved(snapshot: DataSnapshot) {
                    val id = snapshot.key ?: return
                    scope.launch {
                        val local = database.contactDao().getContactByIdDirect(id)
                        if (local != null) database.contactDao().deleteContact(local)
                    }
                }

                override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
                override fun onCancelled(error: DatabaseError) {
                    Log.w(TAG, "Contacts realtime listener cancelled: ${error.message}")
                }
            }
            contactsRef.addChildEventListener(contactListener)
            activeListeners[contactsRef] = contactListener

            // 2. Listen for Realtime Notes updates
            val notesRef = userRef.child("notes")
            val noteListener = object : ChildEventListener {
                override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                    handleRemoteNoteChange(snapshot)
                }

                override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                    handleRemoteNoteChange(snapshot)
                }

                override fun onChildRemoved(snapshot: DataSnapshot) {
                    val id = snapshot.key ?: return
                    scope.launch {
                        val local = database.noteDao().getNoteByIdDirect(id)
                        if (local != null) database.noteDao().deleteNote(local)
                    }
                }

                override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
                override fun onCancelled(error: DatabaseError) {
                    Log.w(TAG, "Notes realtime listener cancelled: ${error.message}")
                }
            }
            notesRef.addChildEventListener(noteListener)
            activeListeners[notesRef] = noteListener

            // 3. Listen for Realtime Tasks updates
            val tasksRef = userRef.child("tasks")
            val taskListener = object : ChildEventListener {
                override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                    handleRemoteTaskChange(snapshot)
                }

                override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                    handleRemoteTaskChange(snapshot)
                }

                override fun onChildRemoved(snapshot: DataSnapshot) {
                    val id = snapshot.key ?: return
                    scope.launch {
                        val local = database.taskDao().getTaskByIdDirect(id)
                        if (local != null) database.taskDao().deleteTask(local)
                    }
                }

                override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
                override fun onCancelled(error: DatabaseError) {
                    Log.w(TAG, "Tasks realtime listener cancelled: ${error.message}")
                }
            }
            tasksRef.addChildEventListener(taskListener)
            activeListeners[tasksRef] = taskListener

            // 4. Listen for Realtime Transactions updates
            val txRef = userRef.child("transactions")
            val txListener = object : ChildEventListener {
                override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                    handleRemoteTransactionChange(snapshot)
                }

                override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                    handleRemoteTransactionChange(snapshot)
                }

                override fun onChildRemoved(snapshot: DataSnapshot) {
                    val id = snapshot.key ?: return
                    scope.launch {
                        val local = database.moneyDao().getAllTransactions().first().find { it.id == id }
                        if (local != null) database.moneyDao().deleteTransaction(local)
                    }
                }

                override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
                override fun onCancelled(error: DatabaseError) {
                    Log.w(TAG, "Transactions realtime listener cancelled: ${error.message}")
                }
            }
            txRef.addChildEventListener(txListener)
            activeListeners[txRef] = txListener

            // 5. Listen for Realtime Vault updates
            val vaultRef = userRef.child("vault")
            val vaultListener = object : ChildEventListener {
                override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                    handleRemoteVaultChange(snapshot)
                }
                override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                    handleRemoteVaultChange(snapshot)
                }
                override fun onChildRemoved(snapshot: DataSnapshot) {
                    val id = snapshot.key ?: return
                    scope.launch {
                        database.vaultDao().deleteVaultItemDirect(id)
                    }
                }
                override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
                override fun onCancelled(error: DatabaseError) {
                    Log.w(TAG, "Vault realtime listener cancelled: ${error.message}")
                }
            }
            vaultRef.addChildEventListener(vaultListener)
            activeListeners[vaultRef] = vaultListener

            // 6. Listen for Realtime Habits updates
            val habitsRef = userRef.child("habits")
            val habitListener = object : ChildEventListener {
                override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                    handleRemoteHabitChange(snapshot)
                }
                override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                    handleRemoteHabitChange(snapshot)
                }
                override fun onChildRemoved(snapshot: DataSnapshot) {
                    val id = snapshot.key ?: return
                    scope.launch {
                        database.trackerDao().deleteHabitDirect(id)
                    }
                }
                override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
                override fun onCancelled(error: DatabaseError) {
                    Log.w(TAG, "Habits realtime listener cancelled: ${error.message}")
                }
            }
            habitsRef.addChildEventListener(habitListener)
            activeListeners[habitsRef] = habitListener

            // 7. Listen for Realtime Habit Completions updates
            val completionsRef = userRef.child("habit_completions")
            val completionListener = object : ChildEventListener {
                override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                    handleRemoteHabitCompletionChange(snapshot)
                }
                override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                    handleRemoteHabitCompletionChange(snapshot)
                }
                override fun onChildRemoved(snapshot: DataSnapshot) {
                    val id = snapshot.key ?: return
                    scope.launch {
                        database.trackerDao().deleteHabitCompletionById(id)
                    }
                }
                override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
                override fun onCancelled(error: DatabaseError) {
                    Log.w(TAG, "Completions realtime listener cancelled: ${error.message}")
                }
            }
            completionsRef.addChildEventListener(completionListener)
            activeListeners[completionsRef] = completionListener

            // 8. Listen for Realtime Goals updates
            val goalsRef = userRef.child("goals")
            val goalListener = object : ChildEventListener {
                override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                    handleRemoteGoalChange(snapshot)
                }
                override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                    handleRemoteGoalChange(snapshot)
                }
                override fun onChildRemoved(snapshot: DataSnapshot) {
                    val id = snapshot.key ?: return
                    scope.launch {
                        database.trackerDao().deleteGoalDirect(id)
                    }
                }
                override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
                override fun onCancelled(error: DatabaseError) {
                    Log.w(TAG, "Goals realtime listener cancelled: ${error.message}")
                }
            }
            goalsRef.addChildEventListener(goalListener)
            activeListeners[goalsRef] = goalListener

            // 9. Listen for Realtime Quick Access updates
            val quickAccessRef = userRef.child("quick_access")
            val quickAccessListener = object : ChildEventListener {
                override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                    handleRemoteQuickAccessChange(snapshot)
                }
                override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                    handleRemoteQuickAccessChange(snapshot)
                }
                override fun onChildRemoved(snapshot: DataSnapshot) {
                    val id = snapshot.key ?: return
                    scope.launch {
                        database.quickAccessDao().deleteQuickAccessItemDirect(id)
                    }
                }
                override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
                override fun onCancelled(error: DatabaseError) {
                    Log.w(TAG, "Quick Access realtime listener cancelled: ${error.message}")
                }
            }
            quickAccessRef.addChildEventListener(quickAccessListener)
            activeListeners[quickAccessRef] = quickAccessListener
        }
    }

    fun stopRealtimeSync() {
        for ((ref, listener) in activeListeners) {
            ref.removeEventListener(listener)
        }
        activeListeners.clear()
        isRealtimeActive = false
        Log.d(TAG, "Stopped Realtime Firebase sync")
    }

    private suspend fun processRemoteContact(snapshot: DataSnapshot) = withContext(Dispatchers.IO) {
        try {
            val id = snapshot.key ?: return@withContext
            val remoteUpdatedAt = snapshot.child("updatedAt").getValue(Long::class.java) ?: 0L
            val local = database.contactDao().getContactByIdDirect(id)

            // If local is newer and pending sync, skip remote overwrite
            if (local != null && local.updatedAt >= remoteUpdatedAt && local.syncState != SyncState.SYNCED) {
                return@withContext
            }

            val displayName = snapshot.child("displayName").getValue(String::class.java) ?: "Unnamed"
            val email = snapshot.child("email").getValue(String::class.java)
            val company = snapshot.child("company").getValue(String::class.java)
            val jobTitle = snapshot.child("jobTitle").getValue(String::class.java)
            val birthday = snapshot.child("birthday").getValue(String::class.java)
            val personalNotes = snapshot.child("personalNotes").getValue(String::class.java)
            val isFavorite = snapshot.child("isFavorite").getValue(Boolean::class.java) ?: false
            val isPinned = snapshot.child("isPinned").getValue(Boolean::class.java) ?: false
            val profileImageStoragePath = snapshot.child("profileImageStoragePath").getValue(String::class.java)
            val addressLine1 = snapshot.child("addressLine1").getValue(String::class.java)
            val addressLine2 = snapshot.child("addressLine2").getValue(String::class.java)
            val city = snapshot.child("city").getValue(String::class.java)
            val region = snapshot.child("region").getValue(String::class.java)
            val postalCode = snapshot.child("postalCode").getValue(String::class.java)
            val country = snapshot.child("country").getValue(String::class.java)

            // Parse phone numbers
            val phoneNumbers = mutableListOf<PhoneNumber>()
            val phonesSnapshot = snapshot.child("phoneNumbers")
            if (phonesSnapshot.exists()) {
                for (phoneSnap in phonesSnapshot.children) {
                    val num = phoneSnap.child("number").getValue(String::class.java)
                    if (!num.isNullOrBlank()) {
                        val label = phoneSnap.child("label").getValue(String::class.java) ?: "Mobile"
                        val isPrimary = phoneSnap.child("isPrimary").getValue(Boolean::class.java) ?: false
                        phoneNumbers.add(PhoneNumber(id = phoneSnap.key ?: UUID.randomUUID().toString(), number = num, label = label, isPrimary = isPrimary))
                    }
                }
            }

            var localAvatarPath = local?.profileImageLocalPath
            val avatarFile = File(File(context.filesDir, "avatars"), "avatar_${id}.jpg")
            if (localAvatarPath.isNullOrBlank() && avatarFile.exists() && avatarFile.length() > 0L) {
                localAvatarPath = avatarFile.absolutePath
            }

            // 1. Immediately insert/update Contact in Room so user sees it instantly
            val updatedContact = ContactEntity(
                id = id,
                displayName = displayName,
                email = email,
                company = company,
                jobTitle = jobTitle,
                birthday = birthday,
                personalNotes = personalNotes,
                isFavorite = isFavorite,
                isPinned = isPinned,
                profileImageLocalPath = localAvatarPath,
                profileImageStoragePath = profileImageStoragePath,
                addressLine1 = addressLine1,
                addressLine2 = addressLine2,
                city = city,
                region = region,
                postalCode = postalCode,
                country = country,
                phoneNumbers = phoneNumbers,
                updatedAt = remoteUpdatedAt,
                syncState = SyncState.SYNCED
            )
            database.contactDao().insertContact(updatedContact)

            // 2. Asynchronously download avatar from GitHub without delaying contact display
            if (!profileImageStoragePath.isNullOrBlank() && (localAvatarPath.isNullOrBlank() || !File(localAvatarPath).exists())) {
                scope.launch(Dispatchers.IO) {
                    try {
                        val prefs = preferencesRepository.userPreferencesFlow.first()
                        val dlToken = prefs.githubToken?.ifBlank { null } ?: DefaultCloudConfig.getDefaultGitHubToken()
                        val dlOwner = prefs.githubOwner?.ifBlank { null } ?: DefaultCloudConfig.DEFAULT_GITHUB_OWNER
                        val dlRepo = prefs.githubRepo?.ifBlank { null } ?: DefaultCloudConfig.DEFAULT_GITHUB_REPO
                        val dlBranch = prefs.githubBranch.ifBlank { DefaultCloudConfig.DEFAULT_GITHUB_BRANCH }
                        val dlResult = gitHubStorageManager.downloadFile(
                            urlOrPath = profileImageStoragePath,
                            destinationFile = avatarFile,
                            token = dlToken,
                            owner = dlOwner,
                            repo = dlRepo,
                            branch = dlBranch
                        )
                        if (dlResult.isSuccess && avatarFile.exists() && avatarFile.length() > 0L) {
                            database.contactDao().insertContact(
                                updatedContact.copy(profileImageLocalPath = avatarFile.absolutePath)
                            )
                        }
                    } catch (ex: Exception) {
                        Log.w(TAG, "Background avatar download failed for contact $id: ${ex.message}")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling remote contact", e)
        }
    }

    private fun handleRemoteContactChange(snapshot: DataSnapshot) {
        scope.launch {
            processRemoteContact(snapshot)
        }
    }

    private suspend fun processRemoteNote(snapshot: DataSnapshot) = withContext(Dispatchers.IO) {
        try {
            val id = snapshot.key ?: return@withContext
            val remoteUpdatedAt = snapshot.child("updatedAt").getValue(Long::class.java) ?: 0L
            val local = database.noteDao().getNoteByIdDirect(id)

            if (local != null && local.updatedAt >= remoteUpdatedAt && local.syncState != SyncState.SYNCED) {
                return@withContext
            }

            val title = snapshot.child("title").getValue(String::class.java) ?: ""
            val body = snapshot.child("body").getValue(String::class.java) ?: ""
            val category = snapshot.child("category").getValue(String::class.java) ?: "General"
            val isJournal = snapshot.child("isJournal").getValue(Boolean::class.java) ?: false
            val journalMood = snapshot.child("journalMood").getValue(String::class.java)
            val journalDate = snapshot.child("journalDate").getValue(String::class.java)
            val isPinned = snapshot.child("isPinned").getValue(Boolean::class.java) ?: false
            val isFavorite = snapshot.child("isFavorite").getValue(Boolean::class.java) ?: false
            val inTrash = snapshot.child("inTrash").getValue(Boolean::class.java) ?: false

            val note = NoteEntity(
                id = id,
                title = title,
                body = body,
                category = category,
                isJournal = isJournal,
                journalMood = journalMood,
                journalDate = journalDate,
                isPinned = isPinned,
                isFavorite = isFavorite,
                inTrash = inTrash,
                updatedAt = remoteUpdatedAt,
                syncState = SyncState.SYNCED
            )
            database.noteDao().insertNote(note)
        } catch (e: Exception) {
            Log.e(TAG, "Error handling remote note", e)
        }
    }

    private fun handleRemoteNoteChange(snapshot: DataSnapshot) {
        scope.launch {
            processRemoteNote(snapshot)
        }
    }

    private suspend fun processRemoteTask(snapshot: DataSnapshot) = withContext(Dispatchers.IO) {
        try {
            val id = snapshot.key ?: return@withContext
            val remoteUpdatedAt = snapshot.child("updatedAt").getValue(Long::class.java) ?: 0L
            val local = database.taskDao().getTaskByIdDirect(id)

            if (local != null && local.updatedAt >= remoteUpdatedAt && local.syncState != SyncState.SYNCED) {
                return@withContext
            }

            val title = snapshot.child("title").getValue(String::class.java) ?: ""
            val description = snapshot.child("description").getValue(String::class.java)
            val dueAt = snapshot.child("dueAt").getValue(Long::class.java)
            val priorityStr = snapshot.child("priority").getValue(String::class.java) ?: "MEDIUM"
            val statusStr = snapshot.child("status").getValue(String::class.java) ?: "TODO"
            val completedAt = snapshot.child("completedAt").getValue(Long::class.java)
            val recurrenceRule = snapshot.child("recurrenceRule").getValue(String::class.java)

            val task = TaskEntity(
                id = id,
                title = title,
                description = description,
                dueAt = if (dueAt != null && dueAt > 0) dueAt else null,
                priority = try { Priority.valueOf(priorityStr) } catch (_: Exception) { Priority.MEDIUM },
                status = try { TaskStatus.valueOf(statusStr) } catch (_: Exception) { TaskStatus.TODO },
                completedAt = if (completedAt != null && completedAt > 0) completedAt else null,
                recurrenceRule = recurrenceRule,
                updatedAt = remoteUpdatedAt,
                syncState = SyncState.SYNCED
            )
            database.taskDao().insertTask(task)
        } catch (e: Exception) {
            Log.e(TAG, "Error handling remote task", e)
        }
    }

    private fun handleRemoteTaskChange(snapshot: DataSnapshot) {
        scope.launch {
            processRemoteTask(snapshot)
        }
    }

    private suspend fun processRemoteTransaction(snapshot: DataSnapshot) = withContext(Dispatchers.IO) {
        try {
            val id = snapshot.key ?: return@withContext
            val remoteUpdatedAt = snapshot.child("updatedAt").getValue(Long::class.java) ?: 0L
            val local = database.moneyDao().getAllTransactions().first().find { it.id == id }

            if (local != null && local.updatedAt >= remoteUpdatedAt && local.syncState != SyncState.SYNCED) {
                return@withContext
            }

            val typeStr = snapshot.child("type").getValue(String::class.java) ?: "EXPENSE"
            val amountMinorUnits = snapshot.child("amountMinorUnits").getValue(Long::class.java) ?: 0L
            val currencyCode = snapshot.child("currencyCode").getValue(String::class.java) ?: "USD"
            val categoryId = snapshot.child("categoryId").getValue(String::class.java) ?: "General"
            val title = snapshot.child("title").getValue(String::class.java) ?: ""
            val transactionDate = snapshot.child("transactionDate").getValue(Long::class.java) ?: System.currentTimeMillis()
            val paymentMethod = snapshot.child("paymentMethod").getValue(String::class.java)
            val notes = snapshot.child("notes").getValue(String::class.java)

            val tx = TransactionEntity(
                id = id,
                type = try { TransactionType.valueOf(typeStr) } catch (_: Exception) { TransactionType.EXPENSE },
                amountMinorUnits = amountMinorUnits,
                currencyCode = currencyCode,
                categoryId = categoryId,
                title = title,
                transactionDate = transactionDate,
                paymentMethod = paymentMethod,
                notes = notes,
                updatedAt = remoteUpdatedAt,
                syncState = SyncState.SYNCED
            )
            database.moneyDao().insertTransaction(tx)
        } catch (e: Exception) {
            Log.e(TAG, "Error handling remote transaction", e)
        }
    }

    private fun handleRemoteTransactionChange(snapshot: DataSnapshot) {
        scope.launch {
            processRemoteTransaction(snapshot)
        }
    }

    private suspend fun processRemoteVaultItem(snapshot: DataSnapshot) = withContext(Dispatchers.IO) {
        try {
            val id = snapshot.key ?: return@withContext
            val remoteUpdatedAt = snapshot.child("updatedAt").getValue(Long::class.java) ?: 0L
            val local = database.vaultDao().getVaultItemByIdDirect(id)

            if (local != null && local.updatedAt >= remoteUpdatedAt && local.syncState != SyncState.SYNCED) {
                return@withContext
            }

            val title = snapshot.child("title").getValue(String::class.java) ?: "Encrypted Item"
            val typeStr = snapshot.child("itemType").getValue(String::class.java) ?: VaultItemType.SECURE_NOTE.name
            val itemType = try { VaultItemType.valueOf(typeStr) } catch (_: Exception) { VaultItemType.SECURE_NOTE }
            val secretText = snapshot.child("encryptedSecretText").getValue(String::class.java)?.ifBlank { null }
            val ivBase64 = snapshot.child("ivBase64").getValue(String::class.java) ?: ""
            val encVer = snapshot.child("encryptionVersion").getValue(Int::class.java) ?: 1
            val mimeType = snapshot.child("mimeType").getValue(String::class.java)?.ifBlank { null }
            val fileSizeBytes = snapshot.child("fileSizeBytes").getValue(Long::class.java) ?: 0L
            val storageObjectPath = snapshot.child("storageObjectPath").getValue(String::class.java)?.ifBlank { null }

            val entity = VaultItemEntity(
                id = id,
                title = title,
                itemType = itemType,
                encryptedLocalPath = local?.encryptedLocalPath,
                encryptedSecretText = secretText,
                ivBase64 = ivBase64,
                encryptionVersion = encVer,
                mimeType = mimeType,
                fileSizeBytes = fileSizeBytes,
                storageObjectPath = storageObjectPath,
                isSynced = true,
                updatedAt = remoteUpdatedAt,
                syncState = SyncState.SYNCED
            )
            database.vaultDao().insertVaultItem(entity)
        } catch (e: Exception) {
            Log.e(TAG, "Error handling remote vault item", e)
        }
    }

    private fun handleRemoteVaultChange(snapshot: DataSnapshot) {
        scope.launch {
            processRemoteVaultItem(snapshot)
        }
    }

    private suspend fun processRemoteHabit(snapshot: DataSnapshot) = withContext(Dispatchers.IO) {
        try {
            val id = snapshot.key ?: return@withContext
            val remoteUpdatedAt = snapshot.child("updatedAt").getValue(Long::class.java) ?: 0L
            val local = database.trackerDao().getHabitByIdDirect(id)

            if (local != null && local.updatedAt >= remoteUpdatedAt && local.syncState != SyncState.SYNCED) {
                return@withContext
            }

            val name = snapshot.child("name").getValue(String::class.java) ?: ""
            val description = snapshot.child("description").getValue(String::class.java)?.ifBlank { null }
            val scheduleDaysSnap = snapshot.child("scheduleDays")
            val scheduleDays = if (scheduleDaysSnap.exists()) {
                scheduleDaysSnap.children.mapNotNull { it.getValue(Long::class.java)?.toInt() }
            } else listOf(1, 2, 3, 4, 5, 6, 7)
            val startDate = snapshot.child("startDate").getValue(String::class.java) ?: ""
            val isActive = snapshot.child("isActive").getValue(Boolean::class.java) ?: true

            val habit = HabitEntity(
                id = id,
                name = name,
                description = description,
                scheduleDays = if (scheduleDays.isNotEmpty()) scheduleDays else listOf(1, 2, 3, 4, 5, 6, 7),
                startDate = startDate,
                isActive = isActive,
                updatedAt = remoteUpdatedAt,
                syncState = SyncState.SYNCED
            )
            database.trackerDao().insertHabit(habit)
        } catch (e: Exception) {
            Log.e(TAG, "Error handling remote habit", e)
        }
    }

    private fun handleRemoteHabitChange(snapshot: DataSnapshot) {
        scope.launch {
            processRemoteHabit(snapshot)
        }
    }

    private suspend fun processRemoteHabitCompletion(snapshot: DataSnapshot) = withContext(Dispatchers.IO) {
        try {
            val id = snapshot.key ?: return@withContext
            val habitId = snapshot.child("habitId").getValue(String::class.java) ?: return@withContext
            val completionDate = snapshot.child("completionDate").getValue(String::class.java) ?: return@withContext
            val isCompleted = snapshot.child("isCompleted").getValue(Boolean::class.java) ?: true
            val note = snapshot.child("note").getValue(String::class.java)?.ifBlank { null }
            val updatedAt = snapshot.child("updatedAt").getValue(Long::class.java) ?: System.currentTimeMillis()

            val completion = HabitCompletionEntity(
                id = id,
                habitId = habitId,
                completionDate = completionDate,
                isCompleted = isCompleted,
                note = note,
                updatedAt = updatedAt
            )
            database.trackerDao().insertHabitCompletion(completion)
        } catch (e: Exception) {
            Log.e(TAG, "Error handling remote habit completion", e)
        }
    }

    private fun handleRemoteHabitCompletionChange(snapshot: DataSnapshot) {
        scope.launch {
            processRemoteHabitCompletion(snapshot)
        }
    }

    private suspend fun processRemoteGoal(snapshot: DataSnapshot) = withContext(Dispatchers.IO) {
        try {
            val id = snapshot.key ?: return@withContext
            val remoteUpdatedAt = snapshot.child("updatedAt").getValue(Long::class.java) ?: 0L
            val local = database.trackerDao().getGoalByIdDirect(id)

            if (local != null && local.updatedAt >= remoteUpdatedAt && local.syncState != SyncState.SYNCED) {
                return@withContext
            }

            val title = snapshot.child("title").getValue(String::class.java) ?: ""
            val description = snapshot.child("description").getValue(String::class.java)?.ifBlank { null }
            val targetValue = snapshot.child("targetValue").getValue(Double::class.java) ?: 100.0
            val currentProgress = snapshot.child("currentProgress").getValue(Double::class.java) ?: 0.0
            val unit = snapshot.child("unit").getValue(String::class.java) ?: "%"
            val targetDate = snapshot.child("targetDate").getValue(String::class.java)?.ifBlank { null }
            val statusStr = snapshot.child("status").getValue(String::class.java) ?: GoalStatus.IN_PROGRESS.name
            val status = try { GoalStatus.valueOf(statusStr) } catch (_: Exception) { GoalStatus.IN_PROGRESS }

            val goal = GoalEntity(
                id = id,
                title = title,
                description = description,
                targetValue = targetValue,
                currentProgress = currentProgress,
                unit = unit,
                targetDate = targetDate,
                status = status,
                updatedAt = remoteUpdatedAt,
                syncState = SyncState.SYNCED
            )
            database.trackerDao().insertGoal(goal)
        } catch (e: Exception) {
            Log.e(TAG, "Error handling remote goal", e)
        }
    }

    private fun handleRemoteGoalChange(snapshot: DataSnapshot) {
        scope.launch {
            processRemoteGoal(snapshot)
        }
    }

    private suspend fun processRemoteQuickAccess(snapshot: DataSnapshot) = withContext(Dispatchers.IO) {
        try {
            val id = snapshot.key ?: return@withContext
            val remoteUpdatedAt = snapshot.child("updatedAt").getValue(Long::class.java) ?: 0L
            val local = database.quickAccessDao().getQuickAccessItemByIdDirect(id)

            if (local != null && local.updatedAt >= remoteUpdatedAt && local.syncState != SyncState.SYNCED) {
                return@withContext
            }

            val title = snapshot.child("title").getValue(String::class.java) ?: ""
            val typeStr = snapshot.child("itemType").getValue(String::class.java) ?: QuickAccessType.URL.name
            val itemType = try { QuickAccessType.valueOf(typeStr) } catch (_: Exception) { QuickAccessType.URL }
            val value = snapshot.child("value").getValue(String::class.java) ?: ""
            val description = snapshot.child("description").getValue(String::class.java)?.ifBlank { null }
            val category = snapshot.child("category").getValue(String::class.java) ?: "Favorites"
            val isPinned = snapshot.child("isPinned").getValue(Boolean::class.java) ?: false
            val sortOrder = snapshot.child("sortOrder").getValue(Int::class.java) ?: 0

            val entity = QuickAccessEntity(
                id = id,
                title = title,
                itemType = itemType,
                value = value,
                description = description,
                category = category,
                isPinned = isPinned,
                sortOrder = sortOrder,
                updatedAt = remoteUpdatedAt,
                syncState = SyncState.SYNCED
            )
            database.quickAccessDao().insertQuickAccessItem(entity)
        } catch (e: Exception) {
            Log.e(TAG, "Error handling remote quick access item", e)
        }
    }

    private fun handleRemoteQuickAccessChange(snapshot: DataSnapshot) {
        scope.launch {
            processRemoteQuickAccess(snapshot)
        }
    }

    /**
     * Upload an authenticated PBKDF2 + AES-GCM encrypted database backup directly
     * to GitHub Private Repository, and save its cloud pointer in Firebase Realtime Database.
     */
    suspend fun uploadEncryptedBackupToGitHub(passphrase: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val prefs = preferencesRepository.userPreferencesFlow.first()
            val token = prefs.githubToken
            val owner = prefs.githubOwner
            val repo = prefs.githubRepo
            val branch = prefs.githubBranch

            if (token.isNullOrBlank() || owner.isNullOrBlank() || repo.isNullOrBlank()) {
                return@withContext Result.failure(IllegalStateException("GitHub credentials not configured. Please fill in Token, Owner, and Repo in Settings."))
            }

            val backup = backupRepository.createEncryptedBackup(passphrase.toCharArray())
            val backupJson = json.encodeToString(backup)
            val timestamp = System.currentTimeMillis()
            val filename = "backups/myspace_backup_${timestamp}.enc.json"

            val uploadResult = gitHubStorageManager.uploadFile(
                path = filename,
                bytes = backupJson.toByteArray(Charsets.UTF_8),
                commitMessage = "Automated Encrypted Backup: $timestamp",
                token = token,
                owner = owner,
                repo = repo,
                branch = branch
            ).getOrThrow()

            // Register in Firebase Realtime Database if connected
            val db = getEffectiveDatabase()
            val uid = getCurrentUserUid()
            if (db != null) {
                val backupMeta = mapOf(
                    "timestamp" to timestamp,
                    "filename" to filename,
                    "sha" to uploadResult.sha,
                    "downloadUrl" to uploadResult.downloadUrl,
                    "rawUrl" to uploadResult.rawUrl,
                    "sizeBytes" to backupJson.toByteArray().size
                )
                db.reference.child("users").child(uid).child("backups").child(timestamp.toString()).setValue(backupMeta).await()
            }

            Result.success("Backup successfully uploaded to GitHub repo: $filename")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Executes bidirectional sync:
     * 1. Large files (avatars) are pushed to GitHub repository.
     * 2. Small text records are synced with Firebase Realtime Database.
     */
    suspend fun syncNow(): Result<Unit> = withContext(Dispatchers.IO) {
        val db = getEffectiveDatabase()
        if (db == null) {
            _syncStatus.value = CloudSyncStatus.Unconfigured
            return@withContext Result.failure(IllegalStateException("Firebase is not configured. Add google-services.json or enter Realtime Database URL in Settings."))
        }

        val uid = getCurrentUserUid()

        try {
            _syncStatus.value = CloudSyncStatus.Syncing("Synchronizing Personal Hub...")
            val prefs = preferencesRepository.userPreferencesFlow.first()
            val effectiveToken = prefs.githubToken?.ifBlank { null } ?: DefaultCloudConfig.getDefaultGitHubToken()
            val effectiveOwner = prefs.githubOwner?.ifBlank { null } ?: DefaultCloudConfig.DEFAULT_GITHUB_OWNER
            val effectiveRepo = prefs.githubRepo?.ifBlank { null } ?: DefaultCloudConfig.DEFAULT_GITHUB_REPO
            val effectiveBranch = prefs.githubBranch.ifBlank { DefaultCloudConfig.DEFAULT_GITHUB_BRANCH }
            val userNode = db.reference.child("users").child(uid)

            // 0. Pull all remote records from Firebase into Room DB
            try {
                val remoteContactsSnap = userNode.child("contacts").get().await()
                for (child in remoteContactsSnap.children) {
                    processRemoteContact(child)
                }

                val remoteNotesSnap = userNode.child("notes").get().await()
                for (child in remoteNotesSnap.children) {
                    processRemoteNote(child)
                }

                val remoteTasksSnap = userNode.child("tasks").get().await()
                for (child in remoteTasksSnap.children) {
                    processRemoteTask(child)
                }

                val remoteTxSnap = userNode.child("transactions").get().await()
                for (child in remoteTxSnap.children) {
                    processRemoteTransaction(child)
                }

                val remoteVaultSnap = userNode.child("vault").get().await()
                for (child in remoteVaultSnap.children) {
                    processRemoteVaultItem(child)
                }

                val remoteHabitsSnap = userNode.child("habits").get().await()
                for (child in remoteHabitsSnap.children) {
                    processRemoteHabit(child)
                }

                val remoteCompletionsSnap = userNode.child("habit_completions").get().await()
                for (child in remoteCompletionsSnap.children) {
                    processRemoteHabitCompletion(child)
                }

                val remoteGoalsSnap = userNode.child("goals").get().await()
                for (child in remoteGoalsSnap.children) {
                    processRemoteGoal(child)
                }

                val remoteQASnap = userNode.child("quick_access").get().await()
                for (child in remoteQASnap.children) {
                    processRemoteQuickAccess(child)
                }
            } catch (pullEx: Exception) {
                Log.w(TAG, "Pulling remote cloud records failed: ${pullEx.message}")
            }

            // 1. Sync Pending Contacts (Large files -> GitHub, Text -> Firebase)
            val pendingContacts = database.contactDao().getPendingSyncContacts()
            for (contact in pendingContacts) {
                if (contact.deletedAt != null) {
                    userNode.child("contacts").child(contact.id).removeValue().await()
                    // Delete avatar from GitHub if configured
                    if (!contact.profileImageStoragePath.isNullOrBlank() && !effectiveToken.isNullOrBlank()) {
                        gitHubStorageManager.deleteFile(
                            path = "media/avatars/avatar_${contact.id}.jpg",
                            commitMessage = "Delete avatar for contact ${contact.id}",
                            token = effectiveToken,
                            owner = effectiveOwner,
                            repo = effectiveRepo,
                            branch = effectiveBranch
                        )
                    }
                    database.contactDao().deleteContact(contact)
                } else {
                    var updatedStoragePath = contact.profileImageStoragePath

                    // If contact has a local avatar not yet in GitHub and GitHub is configured:
                    if (!contact.profileImageLocalPath.isNullOrBlank() && updatedStoragePath.isNullOrBlank() && !effectiveToken.isNullOrBlank()) {
                        val localFile = File(contact.profileImageLocalPath)
                        if (localFile.exists() && localFile.length() > 0L) {
                            val uploadRes = gitHubStorageManager.uploadFile(
                                path = "media/avatars/avatar_${contact.id}.jpg",
                                bytes = localFile.readBytes(),
                                commitMessage = "Upload avatar for ${contact.displayName}",
                                token = effectiveToken,
                                owner = effectiveOwner,
                                repo = effectiveRepo,
                                branch = effectiveBranch
                            )
                            if (uploadRes.isSuccess) {
                                val res = uploadRes.getOrNull()
                                updatedStoragePath = res?.downloadUrl?.ifBlank { null } ?: res?.rawUrl
                            } else {
                                Log.w(TAG, "Failed to upload avatar for contact ${contact.id}: ${uploadRes.exceptionOrNull()?.message}")
                            }
                        }
                    }

                    val phoneListMap = contact.phoneNumbers.map {
                        mapOf(
                            "number" to it.number,
                            "label" to it.label,
                            "isPrimary" to it.isPrimary
                        )
                    }

                    val map = mapOf(
                        "id" to contact.id,
                        "displayName" to contact.displayName,
                        "email" to (contact.email ?: ""),
                        "company" to (contact.company ?: ""),
                        "jobTitle" to (contact.jobTitle ?: ""),
                        "birthday" to (contact.birthday ?: ""),
                        "personalNotes" to (contact.personalNotes ?: ""),
                        "isFavorite" to contact.isFavorite,
                        "isPinned" to contact.isPinned,
                        "profileImageStoragePath" to (updatedStoragePath ?: ""),
                        "addressLine1" to (contact.addressLine1 ?: ""),
                        "addressLine2" to (contact.addressLine2 ?: ""),
                        "city" to (contact.city ?: ""),
                        "region" to (contact.region ?: ""),
                        "postalCode" to (contact.postalCode ?: ""),
                        "country" to (contact.country ?: ""),
                        "phoneNumbers" to phoneListMap,
                        "updatedAt" to contact.updatedAt
                    )
                    userNode.child("contacts").child(contact.id).setValue(map).await()

                    val isAvatarSynced = contact.profileImageLocalPath.isNullOrBlank() || !updatedStoragePath.isNullOrBlank()
                    database.contactDao().updateContact(
                        contact.copy(
                            profileImageStoragePath = updatedStoragePath,
                            syncState = if (isAvatarSynced) SyncState.SYNCED else SyncState.PENDING_UPDATE
                        )
                    )
                }
            }

            // 2. Sync Pending Notes
            val pendingNotes = database.noteDao().getPendingSyncNotes()
            for (note in pendingNotes) {
                if (note.deletedAt != null) {
                    userNode.child("notes").child(note.id).removeValue().await()
                    database.noteDao().deleteNote(note)
                } else {
                    val map = mapOf(
                        "id" to note.id,
                        "title" to note.title,
                        "body" to note.body,
                        "category" to note.category,
                        "isJournal" to note.isJournal,
                        "journalMood" to (note.journalMood ?: ""),
                        "journalDate" to (note.journalDate ?: ""),
                        "isPinned" to note.isPinned,
                        "isFavorite" to note.isFavorite,
                        "inTrash" to note.inTrash,
                        "updatedAt" to note.updatedAt
                    )
                    userNode.child("notes").child(note.id).setValue(map).await()
                    database.noteDao().updateNote(note.copy(syncState = SyncState.SYNCED))
                }
            }

            // 3. Sync Pending Tasks
            val pendingTasks = database.taskDao().getPendingSyncTasks()
            for (task in pendingTasks) {
                if (task.deletedAt != null) {
                    userNode.child("tasks").child(task.id).removeValue().await()
                    database.taskDao().deleteTask(task)
                } else {
                    val map = mapOf(
                        "id" to task.id,
                        "title" to task.title,
                        "description" to (task.description ?: ""),
                        "dueAt" to (task.dueAt ?: 0L),
                        "priority" to task.priority.name,
                        "status" to task.status.name,
                        "completedAt" to (task.completedAt ?: 0L),
                        "recurrenceRule" to (task.recurrenceRule ?: ""),
                        "updatedAt" to task.updatedAt
                    )
                    userNode.child("tasks").child(task.id).setValue(map).await()
                    database.taskDao().updateTask(task.copy(syncState = SyncState.SYNCED))
                }
            }

            // 4. Sync Pending Transactions
            val pendingTxs = database.moneyDao().getPendingSyncTransactions()
            for (tx in pendingTxs) {
                if (tx.deletedAt != null) {
                    userNode.child("transactions").child(tx.id).removeValue().await()
                    database.moneyDao().deleteTransaction(tx)
                } else {
                    val map = mapOf(
                        "id" to tx.id,
                        "type" to tx.type.name,
                        "amountMinorUnits" to tx.amountMinorUnits,
                        "currencyCode" to tx.currencyCode,
                        "categoryId" to tx.categoryId,
                        "title" to tx.title,
                        "transactionDate" to tx.transactionDate,
                        "paymentMethod" to (tx.paymentMethod ?: ""),
                        "notes" to (tx.notes ?: ""),
                        "updatedAt" to tx.updatedAt
                    )
                    userNode.child("transactions").child(tx.id).setValue(map).await()
                    database.moneyDao().updateTransaction(tx.copy(syncState = SyncState.SYNCED))
                }
            }

            // 5. Sync Pending Vault Items
            val pendingVault = database.vaultDao().getPendingSyncVaultItems()
            for (item in pendingVault) {
                if (item.deletedAt != null) {
                    userNode.child("vault").child(item.id).removeValue().await()
                    database.vaultDao().deleteVaultItemDirect(item.id)
                } else {
                    val map = mapOf(
                        "id" to item.id,
                        "title" to item.title,
                        "itemType" to item.itemType.name,
                        "encryptedSecretText" to (item.encryptedSecretText ?: ""),
                        "ivBase64" to item.ivBase64,
                        "encryptionVersion" to item.encryptionVersion,
                        "mimeType" to (item.mimeType ?: ""),
                        "fileSizeBytes" to item.fileSizeBytes,
                        "storageObjectPath" to (item.storageObjectPath ?: ""),
                        "updatedAt" to item.updatedAt
                    )
                    userNode.child("vault").child(item.id).setValue(map).await()
                    database.vaultDao().updateVaultItem(item.copy(syncState = SyncState.SYNCED, isSynced = true))
                }
            }

            // 6. Sync Pending Habits
            val pendingHabits = database.trackerDao().getPendingSyncHabits()
            for (habit in pendingHabits) {
                if (habit.deletedAt != null) {
                    userNode.child("habits").child(habit.id).removeValue().await()
                    database.trackerDao().deleteHabitDirect(habit.id)
                } else {
                    val map = mapOf(
                        "id" to habit.id,
                        "name" to habit.name,
                        "description" to (habit.description ?: ""),
                        "scheduleDays" to habit.scheduleDays,
                        "startDate" to habit.startDate,
                        "isActive" to habit.isActive,
                        "updatedAt" to habit.updatedAt
                    )
                    userNode.child("habits").child(habit.id).setValue(map).await()
                    database.trackerDao().updateHabit(habit.copy(syncState = SyncState.SYNCED))
                }
            }

            // 7. Sync All Habit Completions
            val allCompletions = database.trackerDao().getAllHabitCompletionsDirect()
            for (comp in allCompletions) {
                val map = mapOf(
                    "id" to comp.id,
                    "habitId" to comp.habitId,
                    "completionDate" to comp.completionDate,
                    "isCompleted" to comp.isCompleted,
                    "note" to (comp.note ?: ""),
                    "updatedAt" to comp.updatedAt
                )
                userNode.child("habit_completions").child(comp.id).setValue(map).await()
            }

            // 8. Sync Pending Goals
            val pendingGoals = database.trackerDao().getPendingSyncGoals()
            for (goal in pendingGoals) {
                if (goal.deletedAt != null) {
                    userNode.child("goals").child(goal.id).removeValue().await()
                    database.trackerDao().deleteGoalDirect(goal.id)
                } else {
                    val map = mapOf(
                        "id" to goal.id,
                        "title" to goal.title,
                        "description" to (goal.description ?: ""),
                        "targetValue" to goal.targetValue,
                        "currentProgress" to goal.currentProgress,
                        "unit" to goal.unit,
                        "targetDate" to (goal.targetDate ?: ""),
                        "status" to goal.status.name,
                        "updatedAt" to goal.updatedAt
                    )
                    userNode.child("goals").child(goal.id).setValue(map).await()
                    database.trackerDao().updateGoal(goal.copy(syncState = SyncState.SYNCED))
                }
            }

            // 9. Sync Pending Quick Access Items
            val pendingQA = database.quickAccessDao().getPendingSyncQuickAccessItems()
            for (qa in pendingQA) {
                if (qa.deletedAt != null) {
                    userNode.child("quick_access").child(qa.id).removeValue().await()
                    database.quickAccessDao().deleteQuickAccessItemDirect(qa.id)
                } else {
                    val map = mapOf(
                        "id" to qa.id,
                        "title" to qa.title,
                        "itemType" to qa.itemType.name,
                        "value" to qa.value,
                        "description" to (qa.description ?: ""),
                        "category" to qa.category,
                        "isPinned" to qa.isPinned,
                        "sortOrder" to qa.sortOrder,
                        "updatedAt" to qa.updatedAt
                    )
                    userNode.child("quick_access").child(qa.id).setValue(map).await()
                    database.quickAccessDao().updateQuickAccessItem(qa.copy(syncState = SyncState.SYNCED))
                }
            }

            // 10. Update sync timestamp in preferences
            val now = System.currentTimeMillis()
            preferencesRepository.setLastSyncTimestamp(now)
            _syncStatus.value = CloudSyncStatus.Synced(now)

            // Start realtime listeners if enabled
            if (prefs.realtimeSyncEnabled && !isRealtimeActive) {
                startRealtimeSync()
            }

            Result.success(Unit)
        } catch (e: Exception) {
            _syncStatus.value = CloudSyncStatus.Error(e.message ?: "Unknown sync error")
            Result.failure(e)
        }
    }
}
