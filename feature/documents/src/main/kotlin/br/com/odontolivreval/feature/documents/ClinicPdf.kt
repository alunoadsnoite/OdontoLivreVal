package br.com.odontolivreval.feature.documents

import android.content.Context
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import br.com.odontolivreval.core.demo.DemoPatient
import br.com.odontolivreval.core.demo.PatientRecord
import br.com.odontolivreval.core.demo.ProfessionalProfile
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Gera o PDF de um documento clínico simples, com cabeçalho do profissional,
 * identificação do paciente, corpo livre e bloco de assinatura.
 */
object ClinicPdf {
    private const val PAGE_WIDTH = 595f
    private const val PAGE_HEIGHT = 842f
    private const val MARGIN = 48f
    private const val CONTENT_WIDTH = PAGE_WIDTH - 2 * MARGIN
    private val dateFormat = DateTimeFormatter.ofPattern("dd/MM/uuuu")

    fun write(
        context: Context,
        type: DocumentType,
        patient: DemoPatient,
        record: PatientRecord?,
        profile: ProfessionalProfile,
        content: String,
        title: String,
        labels: PdfLabels,
    ): File {
        val document = PdfDocument()
        val writer = Writer(document)
        try {
            val headerBold = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = 0xFF1A1A1A.toInt()
                textSize = 15f
                isFakeBoldText = true
            }
            val header = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = 0xFF1A1A1A.toInt()
                textSize = 11f
            }
            if (profile.clinicName.isNotBlank()) writer.paragraph(profile.clinicName, headerBold, 4f)
            writer.paragraph(profile.name, headerBold, 2f)
            if (profile.cro.isNotBlank()) writer.paragraph("${labels.cro}: ${profile.cro}", header, 2f)
            if (profile.specialty.isNotBlank()) writer.paragraph(profile.specialty, header, 2f)
            if (profile.address.isNotBlank()) writer.paragraph(profile.address, header, 2f)
            if (profile.phone.isNotBlank()) writer.paragraph(profile.phone, header, 2f)
            writer.separator()

            val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = 0xFF1A1A1A.toInt()
                textSize = 18f
                isFakeBoldText = true
            }
            writer.paragraph(title, titlePaint, 10f)

            val body = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = 0xFF1A1A1A.toInt()
                textSize = 11f
            }
            writer.paragraph("${labels.patient}: ${patient.name}", body, 2f)
            if (!record?.birthDate.isNullOrBlank()) {
                writer.paragraph("${labels.birth}: ${record.birthDate}", body, 2f)
            }
            if (!record?.phone.isNullOrBlank()) {
                writer.paragraph("${labels.phone}: ${record.phone}", body, 2f)
            }
            writer.paragraph("${labels.date}: ${LocalDate.now().format(dateFormat)}", body, 12f)

            val contentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = 0xFF1A1A1A.toInt()
                textSize = 12f
            }
            writer.paragraph(content.trim().ifBlank { " " }, contentPaint, 24f)

            writer.ensureSpace(80f)
            writer.paragraph("______________________________________", body, 4f)
            writer.paragraph(profile.name, body, 2f)
            if (profile.cro.isNotBlank()) {
                writer.paragraph("${labels.cro}: ${profile.cro}", body, 2f)
            }
            writer.finish()

            val directory = File(context.cacheDir, "documents").apply { mkdirs() }
            val file = File(directory, "${type.filePrefix}-${sanitize(patient.name)}-${System.currentTimeMillis()}.pdf")
            FileOutputStream(file).use { document.writeTo(it) }
            return file
        } finally {
            document.close()
        }
    }

    data class PdfLabels(
        val cro: String,
        val patient: String,
        val birth: String,
        val phone: String,
        val date: String,
    )

    private fun sanitize(value: String): String = value
        .lowercase()
        .map { if (it.isLetterOrDigit()) it else '-' }
        .joinToString("")
        .trim('-')
        .take(40)
        .ifBlank { "documento" }

    private class Writer(private val document: PdfDocument) {
        private var pageNumber = 1
        private var page = document.startPage(info(pageNumber))
        private var canvas = page.canvas
        private var y = MARGIN

        fun ensureSpace(height: Float) {
            if (y + height > PAGE_HEIGHT - MARGIN) {
                document.finishPage(page)
                pageNumber += 1
                page = document.startPage(info(pageNumber))
                canvas = page.canvas
                y = MARGIN
            }
        }

        fun paragraph(text: String, paint: Paint, gapAfter: Float) {
            val lineHeight = paint.textSize * 1.45f
            text.split("\n").forEach { rawLine ->
                val words = rawLine.trim().split(" ").filter { it.isNotEmpty() }
                if (words.isEmpty()) {
                    ensureSpace(lineHeight)
                    y += lineHeight
                    return@forEach
                }
                var line = words.first()
                for (word in words.drop(1)) {
                    val candidate = "$line $word"
                    if (paint.measureText(candidate) <= CONTENT_WIDTH) {
                        line = candidate
                    } else {
                        drawLine(line, paint, lineHeight)
                        line = word
                    }
                }
                drawLine(line, paint, lineHeight)
            }
            y += gapAfter
        }

        fun separator() {
            ensureSpace(18f)
            val paint = Paint().apply { color = 0xFF9A9A9A.toInt(); strokeWidth = 0.8f }
            canvas.drawLine(MARGIN, y + 4f, PAGE_WIDTH - MARGIN, y + 4f, paint)
            y += 16f
        }

        fun finish() {
            document.finishPage(page)
        }

        private fun drawLine(text: String, paint: Paint, lineHeight: Float) {
            ensureSpace(lineHeight)
            canvas.drawText(text, MARGIN, y + paint.textSize, paint)
            y += lineHeight
        }

        private fun info(number: Int) = PdfDocument.PageInfo.Builder(PAGE_WIDTH.toInt(), PAGE_HEIGHT.toInt(), number).create()
    }
}
