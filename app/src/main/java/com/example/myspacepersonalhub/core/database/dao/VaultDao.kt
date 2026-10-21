package com.example.myspacepersonalhub.core.database.dao

import androidx.room.*
import com.example.myspacepersonalhub.core.database.entity.VaultItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface VaultDao {
    @Query("SELECT * FROM vault_items WHERE deletedAt IS NULL ORDER BY updatedAt DESC")
    fun getAllVaultItems(): Flow<List<VaultItemEntity>>

    @Query("SELECT * FROM vault_items WHERE id = :id AND deletedAt IS NULL")
    fun getVaultItemById(id: String): Flow<VaultItemEntity?>

    @Query("SELECT * FROM vault_items WHERE id = :id")
    suspend fun getVaultItemByIdDirect(id: String): VaultItemEntity?

    @Query("SELECT * FROM vault_items WHERE syncState != 'SYNCED'")
    suspend fun getPendingSyncVaultItems(): List<VaultItemEntity>

    @Query("SELECT COUNT(*) FROM vault_items WHERE deletedAt IS NULL")
    fun getVaultItemCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVaultItem(item: VaultItemEntity)

    @Update
    suspend fun updateVaultItem(item: VaultItemEntity)

    @Query("UPDATE vault_items SET deletedAt = :timestamp, syncState = 'PENDING_DELETE' WHERE id = :id")
    suspend fun softDeleteVaultItem(id: String, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE vault_items SET syncState = 'PENDING_CREATE' WHERE syncState = 'SYNCED'")
    suspend fun markAllSyncedAsPending()

    @Query("DELETE FROM vault_items WHERE id = :id")
    suspend fun deleteVaultItemDirect(id: String)

    @Delete
    suspend fun deleteVaultItem(item: VaultItemEntity)
}
