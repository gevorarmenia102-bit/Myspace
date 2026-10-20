package com.example.myspacepersonalhub.data.repository

import com.example.myspacepersonalhub.core.database.MySpaceDatabase
import com.example.myspacepersonalhub.core.database.entity.*
import com.example.myspacepersonalhub.core.model.*
import com.example.myspacepersonalhub.core.security.BackupCrypto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class BackupContactDto(
    val id: String,
    val displayName: String,
    val email: String? = null,
    val phoneNumbers: List<PhoneNumber> = emptyList(),
    val company: String? = null,
    val jobTitle: String? = null,
    val address: String? = null,
    val birthday: String? = null,
    val tags: List<String> = emptyList(),
    val notes: String? = null
)

@Serializable
data class BackupNoteDto(
    val id: String,
    val title: String,
    val body: String,
    val category: String,
    val tags: List<String> = emptyList(),
    val isJournal: Boolean = false,
    val journalMood: String? = null,
    val journalDate: String? = null,
    val isPinned: Boolean = false,
    val isFavorite: Boolean = false
)

@Serializable
data class BackupTaskDto(
    val id: String,
    val title: String,
    val description: String? = null,
    val dueAt: Long? = null,
    val priority: String = "MEDIUM",
    val status: String = "TODO",
    val recurrenceRule: String? = null
)

@Serializable
data class BackupTransactionDto(
    val id: String,
    val type: String,
    val amountMinorUnits: Long,
    val currencyCode: String,
    val categoryId: String,
    val title: String,
    val transactionDate: Long,
    val paymentMethod: String? = null,
    val notes: String? = null
)

@Serializable
data class BackupBudgetDto(
    val id: String,
    val name: String,
    val currencyCode: String,
    val limitAmountMinorUnits: Long,
    val period: String
)

@Serializable
data class BackupHabitDto(
    val id: String,
    val name: String,
    val description: String? = null,
    val startDate: String
)

@Serializable
data class BackupGoalDto(
    val id: String,
    val title: String,
    val description: String? = null,
    val targetValue: Double,
    val currentProgress: Double,
    val unit: String
)

@Serializable
data class BackupQuickAccessDto(
    val id: String,
    val title: String,
    val itemType: String,
    val value: String,
    val category: String
)

@Serializable
data class AppBackupData(
    val version: Int = 1,
    val timestamp: Long = System.currentTimeMillis(),
    val contacts: List<BackupContactDto> = emptyList(),
    val notes: List<BackupNoteDto> = emptyList(),
    val tasks: List<BackupTaskDto> = emptyList(),
    val transactions: List<BackupTransactionDto> = emptyList(),
    val budgets: List<BackupBudgetDto> = emptyList(),
    val habits: List<BackupHabitDto> = emptyList(),
    val goals: List<BackupGoalDto> = emptyList(),
    val quickAccess: List<BackupQuickAccessDto> = emptyList()
)

class BackupRepository(private val database: MySpaceDatabase) {
    private val json = Json { prettyPrint = false; ignoreUnknownKeys = true }

    suspend fun createEncryptedBackup(passphrase: CharArray): BackupCrypto.EncryptedBackup = withContext(Dispatchers.IO) {
        val contacts = database.contactDao().getAllContacts().first().map {
            BackupContactDto(
                id = it.id,
                displayName = it.displayName,
                email = it.email,
                phoneNumbers = it.phoneNumbers,
                company = it.company,
                jobTitle = it.jobTitle,
                address = it.addressLine1,
                birthday = it.birthday,
                tags = it.tags,
                notes = it.personalNotes
            )
        }

        val notes = database.noteDao().getAllNotes().first().map {
            BackupNoteDto(
                id = it.id,
                title = it.title,
                body = it.body,
                category = it.category,
                tags = it.tags,
                isJournal = it.isJournal,
                journalMood = it.journalMood,
                journalDate = it.journalDate,
                isPinned = it.isPinned,
                isFavorite = it.isFavorite
            )
        }

        val tasks = database.taskDao().getAllTasks().first().map {
            BackupTaskDto(
                id = it.id,
                title = it.title,
                description = it.description,
                dueAt = it.dueAt,
                priority = it.priority.name,
                status = it.status.name,
                recurrenceRule = it.recurrenceRule
            )
        }

        val txs = database.moneyDao().getAllTransactions().first().map {
            BackupTransactionDto(
                id = it.id,
                type = it.type.name,
                amountMinorUnits = it.amountMinorUnits,
                currencyCode = it.currencyCode,
                categoryId = it.categoryId,
                title = it.title,
                transactionDate = it.transactionDate,
                paymentMethod = it.paymentMethod,
                notes = it.notes
            )
        }

        val budgets = database.moneyDao().getAllBudgets().first().map {
            BackupBudgetDto(
                id = it.id,
                name = it.name,
                currencyCode = it.currencyCode,
                limitAmountMinorUnits = it.limitAmountMinorUnits,
                period = it.period
            )
        }

        val habits = database.trackerDao().getAllHabits().first().map {
            BackupHabitDto(
                id = it.id,
                name = it.name,
                description = it.description,
                startDate = it.startDate
            )
        }

        val goals = database.trackerDao().getAllGoals().first().map {
            BackupGoalDto(
                id = it.id,
                title = it.title,
                description = it.description,
                targetValue = it.targetValue,
                currentProgress = it.currentProgress,
                unit = it.unit
            )
        }

        val quick = database.quickAccessDao().getAllQuickAccessItems().first().map {
            BackupQuickAccessDto(
                id = it.id,
                title = it.title,
                itemType = it.itemType.name,
                value = it.value,
                category = it.category
            )
        }

        val backupData = AppBackupData(
            contacts = contacts,
            notes = notes,
            tasks = tasks,
            transactions = txs,
            budgets = budgets,
            habits = habits,
            goals = goals,
            quickAccess = quick
        )

        val jsonString = json.encodeToString(backupData)
        BackupCrypto.encrypt(jsonString, passphrase)
    }

    suspend fun restoreEncryptedBackup(
        backup: BackupCrypto.EncryptedBackup,
        passphrase: CharArray
    ): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val decryptedJson = BackupCrypto.decrypt(backup, passphrase)
            val backupData = json.decodeFromString<AppBackupData>(decryptedJson)

            val contacts = backupData.contacts.map {
                ContactEntity(
                    id = it.id,
                    displayName = it.displayName,
                    email = it.email,
                    phoneNumbers = it.phoneNumbers,
                    company = it.company,
                    jobTitle = it.jobTitle,
                    addressLine1 = it.address,
                    birthday = it.birthday,
                    tags = it.tags,
                    personalNotes = it.notes
                )
            }
            database.contactDao().insertContacts(contacts)

            backupData.notes.forEach {
                database.noteDao().insertNote(
                    NoteEntity(
                        id = it.id,
                        title = it.title,
                        body = it.body,
                        category = it.category,
                        tags = it.tags,
                        isJournal = it.isJournal,
                        journalMood = it.journalMood,
                        journalDate = it.journalDate,
                        isPinned = it.isPinned,
                        isFavorite = it.isFavorite
                    )
                )
            }

            backupData.tasks.forEach {
                database.taskDao().insertTask(
                    TaskEntity(
                        id = it.id,
                        title = it.title,
                        description = it.description,
                        dueAt = it.dueAt,
                        priority = try { Priority.valueOf(it.priority) } catch (_: Exception) { Priority.MEDIUM },
                        status = try { TaskStatus.valueOf(it.status) } catch (_: Exception) { TaskStatus.TODO },
                        recurrenceRule = it.recurrenceRule
                    )
                )
            }

            backupData.transactions.forEach {
                database.moneyDao().insertTransaction(
                    TransactionEntity(
                        id = it.id,
                        type = try { TransactionType.valueOf(it.type) } catch (_: Exception) { TransactionType.EXPENSE },
                        amountMinorUnits = it.amountMinorUnits,
                        currencyCode = it.currencyCode,
                        categoryId = it.categoryId,
                        title = it.title,
                        transactionDate = it.transactionDate,
                        paymentMethod = it.paymentMethod,
                        notes = it.notes
                    )
                )
            }

            val totalRestored = backupData.contacts.size + backupData.notes.size + backupData.tasks.size
            Result.success(totalRestored)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
