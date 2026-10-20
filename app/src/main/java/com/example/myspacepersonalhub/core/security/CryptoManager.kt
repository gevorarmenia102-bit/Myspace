package com.example.myspacepersonalhub.core.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.KeyStore
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class CryptoManager {

    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val VAULT_KEY_ALIAS = "MySpaceVaultKey_v1"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_IV_LENGTH_BYTES = 12
        private const val GCM_TAG_LENGTH_BITS = 128
    }

    private val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply {
        load(null)
    }

    private fun getOrCreateKey(): SecretKey {
        val existingKey = keyStore.getEntry(VAULT_KEY_ALIAS, null) as? KeyStore.SecretKeyEntry
        return existingKey?.secretKey ?: createKey()
    }

    private fun createKey(): SecretKey {
        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        val spec = KeyGenParameterSpec.Builder(
            VAULT_KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .setRandomizedEncryptionRequired(true)
            .build()

        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }

    data class EncryptionResult(
        val ciphertextBase64: String,
        val ivBase64: String,
        val version: Int = 1
    )

    fun encryptText(plainText: String): EncryptionResult {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        val key = getOrCreateKey()
        cipher.init(Cipher.ENCRYPT_MODE, key)

        val iv = cipher.iv
        val ciphertext = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))

        return EncryptionResult(
            ciphertextBase64 = Base64.getEncoder().encodeToString(ciphertext),
            ivBase64 = Base64.getEncoder().encodeToString(iv),
            version = 1
        )
    }

    fun decryptText(ciphertextBase64: String, ivBase64: String): String {
        val ciphertext = Base64.getDecoder().decode(ciphertextBase64)
        val iv = Base64.getDecoder().decode(ivBase64)

        val cipher = Cipher.getInstance(TRANSFORMATION)
        val spec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv)
        val key = getOrCreateKey()
        cipher.init(Cipher.DECRYPT_MODE, key, spec)

        val plainBytes = cipher.doFinal(ciphertext)
        return String(plainBytes, Charsets.UTF_8)
    }

    fun encryptFile(inputFile: File, outputFile: File): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        val key = getOrCreateKey()
        cipher.init(Cipher.ENCRYPT_MODE, key)
        val iv = cipher.iv

        FileInputStream(inputFile).use { fis ->
            FileOutputStream(outputFile).use { fos ->
                val inputBytes = fis.readBytes()
                val encryptedBytes = cipher.doFinal(inputBytes)
                fos.write(encryptedBytes)
            }
        }
        return iv
    }

    fun decryptFile(encryptedFile: File, outputFile: File, iv: ByteArray) {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        val spec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv)
        val key = getOrCreateKey()
        cipher.init(Cipher.DECRYPT_MODE, key, spec)

        FileInputStream(encryptedFile).use { fis ->
            FileOutputStream(outputFile).use { fos ->
                val encryptedBytes = fis.readBytes()
                val decryptedBytes = cipher.doFinal(encryptedBytes)
                fos.write(decryptedBytes)
            }
        }
    }
}
