package br.com.odontolivreval.core.demo

import java.security.GeneralSecurityException
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
    fun localEncryptionUsesFreshNonce() {
        val key = SecretKeySpec(ByteArray(32), "AES")
        val first = ProtectedPayload.encryptLocal(data, key)
        val second = ProtectedPayload.encryptLocal(data, key)
        assertFalse(first.contentEquals(second))
        assertArrayEquals(data, ProtectedPayload.decryptLocal(first, key))
        assertArrayEquals(data, ProtectedPayload.decryptLocal(second, key))
    }
}
