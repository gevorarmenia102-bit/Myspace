package com.example.myspacepersonalhub

import com.example.myspacepersonalhub.core.security.BackupCrypto
import org.junit.Assert.*
import org.junit.Test
import javax.crypto.AEADBadTagException

class BackupCryptoTest {

    @Test
    fun testEncryptDecryptRoundTrip() {
        val plainText = """{"notes":[{"title":"My Secret","body":"Classified data"}],"contacts":[]}"""
        val passphrase = "MyStrongSecurePassphrase123!".toCharArray()

        val encrypted = BackupCrypto.encrypt(plainText, passphrase)
        assertNotNull(encrypted)
        assertNotNull(encrypted.saltBase64)
        assertNotNull(encrypted.ivBase64)
        assertNotNull(encrypted.ciphertextBase64)

        val decrypted = BackupCrypto.decrypt(encrypted, passphrase)
        assertEquals(plainText, decrypted)
    }

    @Test(expected = Exception::class)
    fun testDecryptWithWrongPassphraseFailsSafely() {
        val plainText = "Sensitive financial information"
        val correctPass = "CorrectPassphrase2026".toCharArray()
        val wrongPass = "WrongPassphrase9999".toCharArray()

        val encrypted = BackupCrypto.encrypt(plainText, correctPass)
        // Must throw an AEADBadTagException or Exception
        BackupCrypto.decrypt(encrypted, wrongPass)
    }

    @Test(expected = Exception::class)
    fun testDecryptWithTamperedCiphertextFailsSafely() {
        val plainText = "Original untampered data"
        val passphrase = "SafePassphrase2026".toCharArray()

        val encrypted = BackupCrypto.encrypt(plainText, passphrase)
        // Corrupt the ciphertext
        val tamperedCiphertext = "AAAA" + encrypted.ciphertextBase64.drop(4)
        val tampered = encrypted.copy(ciphertextBase64 = tamperedCiphertext)

        BackupCrypto.decrypt(tampered, passphrase)
    }
}
