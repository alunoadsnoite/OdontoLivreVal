package br.com.odontolivreval.core.demo

import java.io.File
import java.io.IOException
import java.util.UUID
import javax.crypto.SecretKey

internal const val maxAttachmentBytes = 10 * 1024 * 1024
internal const val maxAttachmentTotal = 512L * 1024 * 1024
private const val maxLegacyEncoded = (2 * 1024 * 1024 + 2) / 3 * 4

internal sealed interface AttachmentContent {
    val valid: Boolean
}

internal data class EmbeddedAttachment(val encoded: String) : AttachmentContent {
    override val valid get() = encoded.length in 1..maxLegacyEncoded
}

internal data class AttachmentRef(val token: String, val size: Int) : AttachmentContent {
    override val valid get() = token.matches(attachmentToken) && size in 1..maxAttachmentBytes
}

private val attachmentToken = Regex("[a-f0-9]{32}")

internal class AttachmentStorage(private val folder: File) {
    fun write(bytes: ByteArray, key: SecretKey): AttachmentRef {
        if (bytes.size !in 1..maxAttachmentBytes) throw IOException("Invalid attachment size")
        if (!folder.isDirectory && !folder.mkdirs()) throw IOException("Cannot create attachment storage")
        val reference = AttachmentRef(UUID.randomUUID().toString().replace("-", ""), bytes.size)
        val file = fileFor(reference)
        if (!file.createNewFile()) throw IOException("Attachment file already exists")
        try {
            val encrypted = ProtectedPayload.encryptAttachment(bytes, key, reference.token)
            file.outputStream().use {
                it.write(encrypted)
                it.fd.sync()
            }
        } catch (error: Exception) {
            file.delete()
            throw error
        }
        return reference
    }

    fun read(reference: AttachmentRef, key: SecretKey): ByteArray {
        val encrypted = fileFor(reference).inputStream().use { it.readBounded(reference.size + 128) }
        val bytes = ProtectedPayload.decryptAttachment(encrypted, key, reference.token)
        if (bytes.size != reference.size) {
            bytes.fill(0)
            throw IOException("Attachment size does not match its index")
        }
        return bytes
    }

    fun discard(references: List<AttachmentRef>) {
        references.forEach { fileFor(it).delete() }
    }

    fun prune(references: List<AttachmentRef>) {
        val keep = references.mapTo(hashSetOf()) { "${it.token}.enc" }
        folder.listFiles()?.forEach {
            if (it.name.endsWith(".enc") && it.name !in keep) it.delete()
        }
    }

    private fun fileFor(reference: AttachmentRef): File {
        if (!reference.valid) throw IOException("Invalid attachment reference")
        return folder.resolve("${reference.token}.enc")
    }
}
