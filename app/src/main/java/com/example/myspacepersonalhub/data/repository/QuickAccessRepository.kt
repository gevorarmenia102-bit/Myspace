package com.example.myspacepersonalhub.data.repository

import android.net.Uri
import com.example.myspacepersonalhub.core.database.dao.QuickAccessDao
import com.example.myspacepersonalhub.core.database.entity.QuickAccessEntity
import com.example.myspacepersonalhub.core.model.QuickAccessType
import kotlinx.coroutines.flow.Flow

class QuickAccessRepository(private val quickAccessDao: QuickAccessDao) {

    fun getAllItems(): Flow<List<QuickAccessEntity>> = quickAccessDao.getAllQuickAccessItems()

    fun getPinnedItems(): Flow<List<QuickAccessEntity>> = quickAccessDao.getPinnedQuickAccessItems()

    suspend fun saveItem(item: QuickAccessEntity): Result<Unit> {
        val trimmedTitle = item.title.trim()
        val trimmedVal = item.value.trim()
        if (trimmedTitle.isEmpty()) {
            return Result.failure(IllegalArgumentException("Title cannot be blank"))
        }
        if (trimmedVal.isEmpty()) {
            return Result.failure(IllegalArgumentException("Value cannot be blank"))
        }

        // Validate URL schemes
        if (item.itemType == QuickAccessType.URL) {
            val uri = Uri.parse(trimmedVal)
            val scheme = uri.scheme?.lowercase()
            if (scheme != "http" && scheme != "https") {
                return Result.failure(IllegalArgumentException("Only http:// or https:// URLs are allowed"))
            }
        }

        quickAccessDao.insertQuickAccessItem(
            item.copy(
                title = trimmedTitle,
                value = trimmedVal,
                updatedAt = System.currentTimeMillis()
            )
        )
        return Result.success(Unit)
    }

    suspend fun deleteItem(id: String) = quickAccessDao.softDeleteQuickAccessItem(id)
}
