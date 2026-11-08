package br.com.odontolivreval.core.demo

import java.io.ByteArrayInputStream
import java.io.IOException
import java.util.zip.ZipInputStream

enum class PrescriptionFormat(val extension: String, val mimeType: String) {
    PDF("pdf", "application/pdf"),
    DOCX("docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"),
}

data class PrescriptionFile(
    val id: Long,
    val patientId: Long,
    val name: String,
    val format: PrescriptionFormat,
    val archivedDocument: Boolean = false,
)

internal data class StoredPrescription(val file: PrescriptionFile, val content: AttachmentContent)

internal fun prescriptionFormat(bytes: ByteArray): PrescriptionFormat {
    if (bytes.size >= 5 && bytes.copyOfRange(0, 5).decodeToString() == "%PDF-") {
        return PrescriptionFormat.PDF
    }
    if (bytes.size >= 4 && bytes[0] == 80.toByte() && bytes[1] == 75.toByte()) {
        ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
            val buffer = ByteArray(8192)
            var contents = false
            var document = false
            var expanded = 0
            repeat(512) {
                val entry = zip.nextEntry ?: return@use
                contents = contents || entry.name == "[Content_Types].xml"
                document = document || entry.name == "word/document.xml"
                while (true) {
                    val count = zip.read(buffer)
                    if (count < 0) break
                    expanded += count
                    if (expanded > 32 * 1024 * 1024) throw IOException("DOCX exceeds expansion limit")
                }
                if (contents && document) return PrescriptionFormat.DOCX
            }
        }
    }
    throw IOException("Unsupported prescription format")
}
