package com.example.myspacepersonalhub.core.database.dao

import androidx.room.*
import com.example.myspacepersonalhub.core.database.entity.QuickAccessEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface QuickAccessDao {
    @Query("SELECT * FROM quick_access WHERE deletedAt IS NULL ORDER BY isPinned DESC, sortOrder ASC, createdAt DESC")
    fun getAllQuickAccessItems(): Flow<List<QuickAccessEntity>>

    @Query("SELECT * FROM quick_access WHERE deletedAt IS NULL AND isPinned = 1 ORDER BY sortOrder ASC")
    fun getPinnedQuickAccessItems(): Flow<List<QuickAccessEntity>>

    @Query("SELECT * FROM quick_access WHERE id = :id AND deletedAt IS NULL")
    fun getQuickAccessItemById(id: String): Flow<QuickAccessEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuickAccessItem(item: QuickAccessEntity)

    @Update
    suspend fun updateQuickAccessItem(item: QuickAccessEntity)

    @Query("UPDATE quick_access SET deletedAt = :timestamp, syncState = 'PENDING_DELETE' WHERE id = :id")
    suspend fun softDeleteQuickAccessItem(id: String, timestamp: Long = System.currentTimeMillis())

    @Query("SELECT * FROM quick_access WHERE id = :id")
    suspend fun getQuickAccessItemByIdDirect(id: String): QuickAccessEntity?

    @Query("SELECT * FROM quick_access WHERE syncState != 'SYNCED'")
    suspend fun getPendingSyncQuickAccessItems(): List<QuickAccessEntity>

    @Query("UPDATE quick_access SET syncState = 'PENDING_CREATE' WHERE syncState = 'SYNCED'")
    suspend fun markAllSyncedAsPending()

    @Query("DELETE FROM quick_access WHERE id = :id")
    suspend fun deleteQuickAccessItemDirect(id: String)

    @Delete
    suspend fun deleteQuickAccessItem(item: QuickAccessEntity)
}
