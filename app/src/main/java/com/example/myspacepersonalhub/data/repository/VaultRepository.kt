package com.example.myspacepersonalhub.data.repository

import android.content.Context
import android.util.Base64
import com.example.myspacepersonalhub.core.database.dao.VaultDao
import com.example.myspacepersonalhub.core.database.entity.VaultItemEntity
import com.example.myspacepersonalhub.core.model.VaultItemType
import com.example.myspacepersonalhub.core.security.CryptoManager
import kotlinx.coroutines.flow.Flow
import java.io.File
import java.util.UUID

class VaultRepository(
    private val vaultDao: VaultDao,
    private val cryptoManager: CryptoManager
) {

    fun getAllVaultItems(): Flow<List<VaultItemEntity>> = vaultDao.getAllVaultItems()

    fun getVaultItemById(id: String): Flow<VaultItemEntity?> = vaultDao.getVaultItemById(id)

    suspend fun saveSecureNote(title: String, secretText: String): Result<Unit> {
        val trimmedTitle = title.trim()
        if (trimmedTitle.isEmpty()) {
            return Result.failure(IllegalArgumentException("Title cannot be blank"))
        }

        val encrypted = cryptoManager.encryptText(secretText)
        val entity = VaultItemEntity(
            title = trimmedTitle,
            itemType = VaultItemType.SECURE_NOTE,
            encryptedSecretText = encrypted.ciphertextBase64,
            ivBase64 = encrypted.ivBase64,
            encryptionVersion = encrypted.version,
            fileSizeBytes = secretText.toByteArray(Charsets.UTF_8).size.toLong()
        )
        vaultDao.insertVaultItem(entity)
        return Result.success(Unit)
    }

    suspend fun updateSecureNote(id: String, title: String, secretText: String): Result<Unit> {
        val trimmedTitle = title.trim()
        if (trimmedTitle.isEmpty()) {
            return Result.failure(IllegalArgumentException("Title cannot be blank"))
        }

        val encrypted = cryptoManager.encryptText(secretText)
        val existing = vaultDao.getVaultItemByIdDirect(id) ?: return Result.failure(IllegalArgumentException("Item not found"))
        val updated = existing.copy(
            title = trimmedTitle,
            encryptedSecretText = encrypted.ciphertextBase64,
            ivBase64 = encrypted.ivBase64,
            encryptionVersion = encrypted.version,
            fileSizeBytes = secretText.toByteArray(Charsets.UTF_8).size.toLong(),
            updatedAt = System.currentTimeMillis()
        )
        vaultDao.updateVaultItem(updated)
        return Result.success(Unit)
    }

    fun decryptSecureNote(item: VaultItemEntity): String {
        val ciphertext = item.encryptedSecretText ?: return ""
        return cryptoManager.decryptText(ciphertext, item.ivBase64)
    }

    suspend fun saveEncryptedFile(
        context: Context,
        title: String,
        sourceFile: File,
        mimeType: String,
        itemType: VaultItemType
    ): Result<Unit> {
        val trimmedTitle = title.trim()
        if (trimmedTitle.isEmpty()) {
            return Result.failure(IllegalArgumentException("Title cannot be blank"))
        }

        val vaultDir = File(context.filesDir, "vault_secure").apply { mkdirs() }
        val secureFileName = "${UUID.randomUUID()}.enc"
        val destFile = File(vaultDir, secureFileName)

        val iv = cryptoManager.encryptFile(sourceFile, destFile)
        val ivBase64 = Base64.encodeToString(iv, Base64.NO_WRAP)

        val entity = VaultItemEntity(
            title = trimmedTitle,
            itemType = itemType,
            encryptedLocalPath = destFile.absolutePath,
            ivBase64 = ivBase64,
            encryptionVersion = 1,
            mimeType = mimeType,
            fileSizeBytes = sourceFile.length()
        )
        vaultDao.insertVaultItem(entity)
        return Result.success(Unit)
    }

    fun decryptVaultFile(item: VaultItemEntity, destFile: File) {
        val encPath = item.encryptedLocalPath ?: return
        val encFile = File(encPath)
        if (!encFile.exists()) return
        val iv = Base64.decode(item.ivBase64, Base64.NO_WRAP)
        cryptoManager.decryptFile(encFile, destFile, iv)
    }

    suspend fun deleteVaultItem(item: VaultItemEntity) {
        if (!item.encryptedLocalPath.isNullOrBlank()) {
            try {
                File(item.encryptedLocalPath).delete()
            } catch (_: Exception) {}
        }
        vaultDao.deleteVaultItem(item)
    }

    fun getVaultItemCount(): Flow<Int> = vaultDao.getVaultItemCount()
}
