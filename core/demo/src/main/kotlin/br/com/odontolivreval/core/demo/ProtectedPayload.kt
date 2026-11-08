package br.com.odontolivreval.core.demo

import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.nio.ByteBuffer
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

internal object ProtectedPayload {
    private const val localHeader = "ODL-LOC1"
    private const val backupV1Header = "ODL-BKP1"
    private const val backupV2Header = "ODL-BKP2"
    private const val saltSize = 16
    private const val ivSize = 12
    private const val tagBits = 128
    private const val iterations = 600_000
    private const val minIterations = 100_000
    private const val maxIterations = 10_000_000
    private const val iterationsSize = Int.SIZE_BYTES
    private val local = localHeader.toByteArray(Charsets.US_ASCII)
    private val legacyBackup = backupV1Header.toByteArray(Charsets.US_ASCII)
    private val backup = backupV2Header.toByteArray(Charsets.US_ASCII)
    const val maxDataSize = 4 * 1024 * 1024
    const val maxFileSize = maxDataSize + 128

    fun encryptLocal(data: ByteArray, key: SecretKey): ByteArray = encrypt(data, key, local)

    fun decryptLocal(data: ByteArray, key: SecretKey): ByteArray = decrypt(data, key, local)

    /**
     * Formato 2: magic (8) + iterações do PBKDF2 (4) + sal (16) + IV (12) +
     * conteúdo autenticado. Guardar as iterações permite aumentar o custo no
     * futuro sem invalidar backups antigos.
     */
    fun encryptBackup(data: ByteArray, password: CharArray): ByteArray {
        val salt = ByteArray(saltSize).also(SecureRandom()::nextBytes)
        val header = backup + ByteBuffer.allocate(iterationsSize).putInt(iterations).array() + salt
        return withBackupKey(password, salt, iterations) { key -> encrypt(data, key, header) }
    }

    fun decryptBackup(data: ByteArray, password: CharArray): ByteArray = when {
        data.startsWith(legacyBackup) -> decryptLegacyBackup(data, password)
        data.startsWith(backup) -> decryptBackupV2(data, password)
        else -> throw IOException("Unsupported backup format")
    }

    private fun decryptLegacyBackup(data: ByteArray, password: CharArray): ByteArray {
        val headerSize = legacyBackup.size + saltSize
        if (data.size < headerSize + ivSize + tagBits / 8) throw IOException("Invalid backup")
        val salt = data.copyOfRange(legacyBackup.size, headerSize)
        val header = data.copyOfRange(0, headerSize)
        return withBackupKey(password, salt, iterations) { key -> decrypt(data, key, header) }
    }

    private fun decryptBackupV2(data: ByteArray, password: CharArray): ByteArray {
        val iterationsStart = backup.size
        val saltStart = iterationsStart + iterationsSize
        val headerSize = saltStart + saltSize
        if (data.size < headerSize + ivSize + tagBits / 8) throw IOException("Invalid backup")
        val storedIterations = ByteBuffer.wrap(data, iterationsStart, iterationsSize).int
        if (storedIterations !in minIterations..maxIterations) throw IOException("Unsupported backup cost")
        val salt = data.copyOfRange(saltStart, headerSize)
        val header = data.copyOfRange(0, headerSize)
        return withBackupKey(password, salt, storedIterations) { key -> decrypt(data, key, header) }
    }

    private fun withBackupKey(
        password: CharArray,
        salt: ByteArray,
        iterations: Int,
        block: (SecretKey) -> ByteArray,
    ): ByteArray {
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

    private fun ByteArray.startsWith(prefix: ByteArray): Boolean =
        size >= prefix.size && copyOfRange(0, prefix.size).contentEquals(prefix)
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
