package com.example.myspacepersonalhub.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.myspacepersonalhub.core.model.SyncState
import com.example.myspacepersonalhub.core.model.VaultItemType
import java.util.UUID

@Entity(tableName = "vault_items")
data class VaultItemEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String,
    val itemType: VaultItemType,
    val encryptedLocalPath: String? = null,
    val encryptedSecretText: String? = null, // Ciphertext in Base64
    val ivBase64: String, // 12-byte AES-GCM IV in Base64
    val encryptionVersion: Int = 1,
    val mimeType: String? = null,
    val fileSizeBytes: Long = 0L,
    val storageObjectPath: String? = null,
    val isSynced: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val deletedAt: Long? = null,
    val syncState: SyncState = SyncState.PENDING_CREATE,
    val ownerUid: String? = null
)
