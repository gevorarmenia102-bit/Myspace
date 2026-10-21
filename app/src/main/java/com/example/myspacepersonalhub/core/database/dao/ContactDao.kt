package com.example.myspacepersonalhub.core.database.dao

import androidx.room.*
import com.example.myspacepersonalhub.core.database.entity.ContactEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ContactDao {
    @Query("SELECT * FROM contacts WHERE deletedAt IS NULL ORDER BY isPinned DESC, displayName COLLATE NOCASE ASC")
    fun getAllContacts(): Flow<List<ContactEntity>>

    @Query("SELECT * FROM contacts WHERE deletedAt IS NULL AND isFavorite = 1 ORDER BY isPinned DESC, displayName COLLATE NOCASE ASC")
    fun getFavoriteContacts(): Flow<List<ContactEntity>>

    @Query("""
        SELECT * FROM contacts 
        WHERE deletedAt IS NULL AND (
            displayName LIKE '%' || :query || '%' OR 
            nickname LIKE '%' || :query || '%' OR 
            email LIKE '%' || :query || '%' OR 
            company LIKE '%' || :query || '%' OR 
            personalNotes LIKE '%' || :query || '%' OR 
            phoneNumbers LIKE '%' || :query || '%'
        )
        ORDER BY isPinned DESC, displayName COLLATE NOCASE ASC
    """)
    fun searchContacts(query: String): Flow<List<ContactEntity>>

    @Query("SELECT * FROM contacts WHERE id = :id AND deletedAt IS NULL")
    fun getContactById(id: String): Flow<ContactEntity?>

    @Query("SELECT * FROM contacts WHERE id = :id")
    suspend fun getContactByIdDirect(id: String): ContactEntity?

    @Query("SELECT * FROM contacts WHERE syncState != 'SYNCED' OR (deletedAt IS NULL AND profileImageLocalPath IS NOT NULL AND (profileImageStoragePath IS NULL OR profileImageStoragePath = ''))")
    suspend fun getPendingSyncContacts(): List<ContactEntity>

    @Query("SELECT COUNT(*) FROM contacts WHERE deletedAt IS NULL")
    fun getContactCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContact(contact: ContactEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContacts(contacts: List<ContactEntity>)

    @Update
    suspend fun updateContact(contact: ContactEntity)

    @Query("UPDATE contacts SET deletedAt = :timestamp, syncState = 'PENDING_DELETE' WHERE id = :id")
    suspend fun softDeleteContact(id: String, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE contacts SET syncState = 'PENDING_UPDATE' WHERE deletedAt IS NULL AND syncState = 'SYNCED'")
    suspend fun markAllSyncedAsPending()

    @Delete
    suspend fun deleteContact(contact: ContactEntity)
}
