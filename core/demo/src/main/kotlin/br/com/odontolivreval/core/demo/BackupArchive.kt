package br.com.odontolivreval.core.demo

import java.io.BufferedInputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.io.OutputStream
import java.nio.ByteBuffer
import java.security.SecureRandom
import javax.crypto.SecretKey

/** The authenticated snapshot lists the ordered files and exact sizes that must follow it. */
internal object BackupArchive {
    private val magic = "ODL-BKP3".encodeToByteArray()
    private const val frameOverheadAllowance = 128

    fun matches(input: BufferedInputStream): Boolean {
        input.mark(magic.size)
        return try {
            magic.all { input.read() == it.toInt() }
        } finally {
            input.reset()
        }
    }

    fun write(output: OutputStream, password: CharArray, frames: (Writer) -> Unit) {
        val salt = ByteArray(ProtectedPayload.saltSize).also(SecureRandom()::nextBytes)
        val stream = DataOutputStream(output)
        stream.write(magic)
        stream.writeInt(ProtectedPayload.iterations)
        stream.write(salt)
        ProtectedPayload.withBackupKey(password, salt, ProtectedPayload.iterations) { key ->
            frames(Writer(stream, key, archiveHeader(ProtectedPayload.iterations, salt)))
        }
        stream.flush()
    }

    fun <T> read(input: BufferedInputStream, password: CharArray, frames: (Reader) -> T): T {
        val stream = DataInputStream(input)
        val prefix = ByteArray(magic.size).also(stream::readFully)
        if (!prefix.contentEquals(magic)) throw IOException("Invalid backup header")
        val iterations = stream.readInt()
        val salt = ByteArray(ProtectedPayload.saltSize).also(stream::readFully)
        val result = ProtectedPayload.withBackupKey(password, salt, iterations) { key ->
            frames(Reader(stream, key, archiveHeader(iterations, salt)))
        }
        if (stream.read() != -1) throw IOException("Unexpected data after backup")
        return result
    }

    class Writer(private val stream: DataOutputStream, private val key: SecretKey, private val header: ByteArray) {
        private var index = 0

        fun frame(bytes: ByteArray, limit: Int) {
            val encrypted = ProtectedPayload.encrypt(bytes, key, frameHeader(header, index++), limit)
            stream.writeInt(encrypted.size)
            stream.write(encrypted)
        }
    }

    class Reader(private val stream: DataInputStream, private val key: SecretKey, private val header: ByteArray) {
        private var index = 0

        fun frame(limit: Int): ByteArray {
            val size = stream.readInt()
            if (size !in 1..(limit + frameOverheadAllowance)) throw IOException("Invalid backup frame size")
            val encrypted = ByteArray(size).also(stream::readFully)
            return ProtectedPayload.decrypt(encrypted, key, frameHeader(header, index++), limit)
        }
    }

    private fun archiveHeader(iterations: Int, salt: ByteArray): ByteArray =
        magic + ByteBuffer.allocate(Int.SIZE_BYTES).putInt(iterations).array() + salt

    private fun frameHeader(header: ByteArray, index: Int): ByteArray =
        header + ByteBuffer.allocate(Int.SIZE_BYTES).putInt(index).array()
}
