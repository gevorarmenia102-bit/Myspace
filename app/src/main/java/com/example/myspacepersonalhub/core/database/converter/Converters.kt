package com.example.myspacepersonalhub.core.database.converter

import androidx.room.TypeConverter
import com.example.myspacepersonalhub.core.model.GoalStatus
import com.example.myspacepersonalhub.core.model.PhoneNumber
import com.example.myspacepersonalhub.core.model.Priority
import com.example.myspacepersonalhub.core.model.QuickAccessType
import com.example.myspacepersonalhub.core.model.SyncState
import com.example.myspacepersonalhub.core.model.TaskStatus
import com.example.myspacepersonalhub.core.model.TransactionType
import com.example.myspacepersonalhub.core.model.VaultItemType
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class Converters {
    private val json = Json { ignoreUnknownKeys = true }

    @TypeConverter
    fun fromStringList(value: List<String>?): String {
        return value?.let { json.encodeToString(it) } ?: "[]"
    }

    @TypeConverter
    fun toStringList(value: String?): List<String> {
        if (value.isNullOrBlank()) return emptyList()
        return try {
            json.decodeFromString(value)
        } catch (_: Exception) {
            emptyList()
        }
    }

    @TypeConverter
    fun fromIntList(value: List<Int>?): String {
        return value?.let { json.encodeToString(it) } ?: "[]"
    }

    @TypeConverter
    fun toIntList(value: String?): List<Int> {
        if (value.isNullOrBlank()) return emptyList()
        return try {
            json.decodeFromString(value)
        } catch (_: Exception) {
            emptyList()
        }
    }

    @TypeConverter
    fun fromPhoneList(value: List<PhoneNumber>?): String {
        return value?.let { json.encodeToString(it) } ?: "[]"
    }

    @TypeConverter
    fun toPhoneList(value: String?): List<PhoneNumber> {
        if (value.isNullOrBlank()) return emptyList()
        return try {
            json.decodeFromString(value)
        } catch (_: Exception) {
            emptyList()
        }
    }

    @TypeConverter
    fun fromSyncState(value: SyncState): String = value.name

    @TypeConverter
    fun toSyncState(value: String?): SyncState {
        return try {
            if (value != null) SyncState.valueOf(value) else SyncState.SYNCED
        } catch (_: Exception) {
            SyncState.SYNCED
        }
    }

    @TypeConverter
    fun fromPriority(value: Priority): String = value.name

    @TypeConverter
    fun toPriority(value: String?): Priority {
        return try {
            if (value != null) Priority.valueOf(value) else Priority.MEDIUM
        } catch (_: Exception) {
            Priority.MEDIUM
        }
    }

    @TypeConverter
    fun fromTaskStatus(value: TaskStatus): String = value.name

    @TypeConverter
    fun toTaskStatus(value: String?): TaskStatus {
        return try {
            if (value != null) TaskStatus.valueOf(value) else TaskStatus.TODO
        } catch (_: Exception) {
            TaskStatus.TODO
        }
    }

    @TypeConverter
    fun fromTransactionType(value: TransactionType): String = value.name

    @TypeConverter
    fun toTransactionType(value: String?): TransactionType {
        return try {
            if (value != null) TransactionType.valueOf(value) else TransactionType.EXPENSE
        } catch (_: Exception) {
            TransactionType.EXPENSE
        }
    }

    @TypeConverter
    fun fromVaultItemType(value: VaultItemType): String = value.name

    @TypeConverter
    fun toVaultItemType(value: String?): VaultItemType {
        return try {
            if (value != null) VaultItemType.valueOf(value) else VaultItemType.SECURE_NOTE
        } catch (_: Exception) {
            VaultItemType.SECURE_NOTE
        }
    }

    @TypeConverter
    fun fromGoalStatus(value: GoalStatus): String = value.name

    @TypeConverter
    fun toGoalStatus(value: String?): GoalStatus {
        return try {
            if (value != null) GoalStatus.valueOf(value) else GoalStatus.IN_PROGRESS
        } catch (_: Exception) {
            GoalStatus.IN_PROGRESS
        }
    }

    @TypeConverter
    fun fromQuickAccessType(value: QuickAccessType): String = value.name

    @TypeConverter
    fun toQuickAccessType(value: String?): QuickAccessType {
        return try {
            if (value != null) QuickAccessType.valueOf(value) else QuickAccessType.TEXT
        } catch (_: Exception) {
            QuickAccessType.TEXT
        }
    }
}
