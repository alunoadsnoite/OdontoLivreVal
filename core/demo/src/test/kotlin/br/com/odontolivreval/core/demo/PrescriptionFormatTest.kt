package br.com.odontolivreval.core.demo

import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class PrescriptionFormatTest {
    @Test
    fun identifiesPdfAndWordAndRejectsOrdinaryZip() {
        assertEquals(PrescriptionFormat.PDF, prescriptionFormat("%PDF-1.7\n".encodeToByteArray()))
        assertEquals(PrescriptionFormat.DOCX, prescriptionFormat(zip("[Content_Types].xml", "word/document.xml")))
        assertThrows(IOException::class.java) { prescriptionFormat(zip("notes.txt")) }
    }

    private fun zip(vararg names: String): ByteArray {
        val bytes = ByteArrayOutputStream()
        ZipOutputStream(bytes).use { archive ->
            names.forEach {
                archive.putNextEntry(ZipEntry(it))
                archive.write("teste".encodeToByteArray())
                archive.closeEntry()
            }
        }
        return bytes.toByteArray()
    }
}
