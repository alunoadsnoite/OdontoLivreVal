package br.com.odontolivreval.core.demo

import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

internal object ProtectedPayload {
    private val local = "ODL-LOC1".toByteArray(Charsets.US_ASCII)
    private val backup = "ODL-BKP1".toByteArray(Charsets.US_ASCII)
    private const val saltSize = 16
    private const val ivSize = 12
    private const val tagBits = 128
    private const val iterations = 600_000
    const val maxDataSize = 4 * 1024 * 1024
    const val maxFileSize = maxDataSize + 64

    fun encryptLocal(data: ByteArray, key: SecretKey): ByteArray = encrypt(data, key, local)

    fun decryptLocal(data: ByteArray, key: SecretKey): ByteArray = decrypt(data, key, local)

    fun encryptBackup(data: ByteArray, password: CharArray): ByteArray {
        val salt = ByteArray(saltSize).also(SecureRandom()::nextBytes)
        return withBackupKey(password, salt) { key -> encrypt(data, key, backup + salt) }
    }

    fun decryptBackup(data: ByteArray, password: CharArray): ByteArray {
        if (data.size < backup.size + saltSize + ivSize + tagBits / 8 ||
            !data.copyOfRange(0, backup.size).contentEquals(backup)
        ) throw IOException("Unsupported backup format")
        val salt = data.copyOfRange(backup.size, backup.size + saltSize)
        return withBackupKey(password, salt) { key -> decrypt(data, key, backup + salt) }
    }

    private fun withBackupKey(password: CharArray, salt: ByteArray, block: (SecretKey) -> ByteArray): ByteArray {
        val spec = PBEKeySpec(password, salt, iterations, 256)
        try {
            val bytes = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
            try {
                return block(SecretKeySpec(bytes, "AES"))
            } finally {
                bytes.fill(0)
            }
        } finally {
            spec.clearPassword()
        }
    }

    private fun encrypt(data: ByteArray, key: SecretKey, header: ByteArray): ByteArray {
        if (data.size > maxDataSize) throw IOException("Snapshot is too large")
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key)
        val iv = cipher.iv
        if (iv.size != ivSize) throw IOException("Unsupported cipher IV size")
        cipher.updateAAD(header)
        return header + iv + cipher.doFinal(data)
    }

    private fun decrypt(data: ByteArray, key: SecretKey, header: ByteArray): ByteArray {
        val start = header.size + ivSize
        if (data.size !in (start + tagBits / 8)..(start + tagBits / 8 + maxDataSize) ||
            !data.copyOfRange(0, header.size).contentEquals(header)
        ) throw IOException("Invalid encrypted snapshot")
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(tagBits, data, header.size, ivSize))
        cipher.updateAAD(header)
        return cipher.doFinal(data, start, data.size - start)
    }
}

internal fun InputStream.readClinicFile(): ByteArray {
    val output = ByteArrayOutputStream()
    val buffer = ByteArray(8192)
    while (true) {
        val count = read(buffer)
        if (count < 0) return output.toByteArray()
        if (output.size() + count > ProtectedPayload.maxFileSize) throw IOException("File exceeds size limit")
        output.write(buffer, 0, count)
    }
}
