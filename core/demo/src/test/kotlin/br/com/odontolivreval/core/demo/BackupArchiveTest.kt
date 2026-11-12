package br.com.odontolivreval.core.demo

import java.io.ByteArrayOutputStream
import java.io.IOException
import java.security.GeneralSecurityException
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupArchiveTest {
    private val password = "senha-de-backup-de-teste".toCharArray()

    @Test
    fun roundTripsAttachmentsBiggerThanTheOldSnapshotAndRejectsInvalidCopies() {
        val index = byteArrayOf(1, 2, 3)
        val attachment = ByteArray(5 * 1024 * 1024) { (it % 253).toByte() }
        val output = ByteArrayOutputStream()
        BackupArchive.write(output, password) { writer ->
            writer.frame(index, ProtectedPayload.maxDataSize)
            writer.frame(attachment, maxAttachmentBytes)
        }
        val archive = output.toByteArray()

        fun read(bytes: ByteArray) {
            bytes.inputStream().buffered().use { input ->
                assertTrue(BackupArchive.matches(input))
                BackupArchive.read(input, password) { reader ->
                    assertArrayEquals(index, reader.frame(ProtectedPayload.maxDataSize))
                    assertArrayEquals(attachment, reader.frame(maxAttachmentBytes))
                }
            }
        }

        read(archive)
        assertThrows(IOException::class.java) { read(archive.copyOf(archive.size - 1)) }
        assertThrows(IOException::class.java) { read(archive + 0.toByte()) }
        archive[archive.lastIndex] = (archive.last().toInt() xor 1).toByte()
        assertThrows(GeneralSecurityException::class.java) { read(archive) }
    }

    @Test
    fun backupRejectsIncorrectPasswordBeforeReturningSnapshot() {
        val snapshot = byteArrayOf(1, 2, 3)
        val output = ByteArrayOutputStream()
        BackupArchive.write(output, password) { writer ->
            writer.frame(snapshot, ProtectedPayload.maxDataSize)
        }
        val archive = output.toByteArray()

        assertThrows(GeneralSecurityException::class.java) {
            archive.inputStream().buffered().use { input ->
                BackupArchive.read(input, "senha-de-teste-incorreta".toCharArray()) { reader ->
                    reader.frame(ProtectedPayload.maxDataSize)
                }
            }
        }
    }
}
