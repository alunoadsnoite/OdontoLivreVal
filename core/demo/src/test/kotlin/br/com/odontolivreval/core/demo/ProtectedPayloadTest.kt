package br.com.odontolivreval.core.demo

import java.security.GeneralSecurityException
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Test

class ProtectedPayloadTest {
    private val data = "Paciente fictício".toByteArray()

    @Test
    fun backupRestoresWithCorrectPasswordAndRejectsWrongPasswordAndTampering() {
        val password = "senha-de-teste-longa".toCharArray()
        val backup = ProtectedPayload.encryptBackup(data, password)
        assertArrayEquals(data, ProtectedPayload.decryptBackup(backup, password))
        assertThrows(GeneralSecurityException::class.java) {
            ProtectedPayload.decryptBackup(backup, "senha-incorreta".toCharArray())
        }
        backup[backup.lastIndex] = (backup.last().toInt() xor 1).toByte()
        assertThrows(GeneralSecurityException::class.java) {
            ProtectedPayload.decryptBackup(backup, password)
        }
    }

    @Test
    fun backupFormatStoresIterationsAndRestoresLegacyV1Files() {
        val password = "senha-de-teste-longa".toCharArray()
        val backup = ProtectedPayload.encryptBackup(data, password)
        assertArrayEquals("ODL-BKP2".toByteArray(Charsets.US_ASCII), backup.copyOfRange(0, 8))
        assertArrayEquals(data, ProtectedPayload.decryptBackup(backup, password))

        val legacy = legacyBackup(data, password)
        assertArrayEquals(data, ProtectedPayload.decryptBackup(legacy, password))
    }

    @Test
    fun localEncryptionUsesFreshNonce() {
        val key = SecretKeySpec(ByteArray(32), "AES")
        val first = ProtectedPayload.encryptLocal(data, key)
        val second = ProtectedPayload.encryptLocal(data, key)
        assertFalse(first.contentEquals(second))
        assertArrayEquals(data, ProtectedPayload.decryptLocal(first, key))
        assertArrayEquals(data, ProtectedPayload.decryptLocal(second, key))
    }

    /** Reproduz o formato 1 para garantir que backups antigos continuem abrindo. */
    private fun legacyBackup(data: ByteArray, password: CharArray): ByteArray {
        val header = "ODL-BKP1".toByteArray(Charsets.US_ASCII)
        val salt = ByteArray(16).also(SecureRandom()::nextBytes)
        val spec = PBEKeySpec(password, salt, 600_000, 256)
        try {
            val keyBytes = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                .generateSecret(spec).encoded
            val key = SecretKeySpec(keyBytes, "AES")
            val aad = header + salt
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, key)
            cipher.updateAAD(aad)
            return aad + cipher.iv + cipher.doFinal(data)
        } finally {
            spec.clearPassword()
        }
    }
}
