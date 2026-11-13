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

object PrescriptionPdf {
    private const val PAGE_WIDTH = 595f
    private const val PAGE_HEIGHT = 842f
    private const val MARGIN = 48f
    private const val CONTENT_WIDTH = PAGE_WIDTH - 2 * MARGIN

    fun write(
        context: Context,
        patient: DemoPatient,
        patientRecord: br.com.odontolivreval.core.demo.PatientRecord?,
        professional: br.com.odontolivreval.core.demo.ProfessionalProfile,
        medications: List<PrescriptionDocument.MedicationItem>,
        additionalInstructions: String,
        validityDays: Int,
    ): File {
        val document = PdfDocument()
        val writer = Writer(document)
        try {
            val headerBold = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = 0xFF1A1A1A.toInt()
                textSize = 16f
                isFakeBoldText = true
            }
            val header = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = 0xFF1A1A1A.toInt()
                textSize = 11f
            }
            val body = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = 0xFF1A1A1A.toInt()
                textSize = 11f
            }

            // Cabeçalho
            if (professional.clinicName.isNotBlank()) {
                writer.paragraph(professional.clinicName, headerBold, 4f)
            }
            writer.paragraph(professional.name, headerBold, 2f)
            if (professional.cro.isNotBlank()) {
                val croLine = "CRO: ${professional.cro}" + (if (professional.croUF.isNotBlank()) "/${professional.croUF}" else "")
                writer.paragraph(croLine, header, 2f)
            }
            writer.separator()

            // Título
            val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = 0xFF1A1A1A.toInt()
                textSize = 18f
                isFakeBoldText = true
            }
            writer.paragraph(context.getString(R.string.documents_export_receita_odontologica), titlePaint, 10f)

            // Identificação do paciente
            writer.paragraph(context.getString(R.string.documents_export_paciente, patient.name), body, 2f)
            if (!patientRecord?.birthDate.isNullOrBlank()) {
                writer.paragraph(context.getString(R.string.documents_export_nascimento, patientRecord?.birthDate), body, 2f)
            }
            if (!patientRecord?.phone.isNullOrBlank()) {
                writer.paragraph(context.getString(R.string.documents_export_telefone, patientRecord?.phone), body, 2f)
            }

            // Médicamentos
            writer.paragraph(context.getString(R.string.documents_export_medicamentos), body, 6f)
            for (medication in medications) {
                writer.paragraph("• ${medication.name}", body, 2f)
                if (medication.presentation.isNotEmpty()) {
                    writer.paragraph("  " + context.getString(R.string.documents_export_apresentacao, medication.presentation), body, 2f)
                }
                if (medication.concentration.isNotEmpty()) {
                    writer.paragraph("  " + context.getString(R.string.documents_export_concentracao, medication.concentration), body, 2f)
                }
                if (medication.posology.isNotEmpty()) {
                    writer.paragraph("  " + context.getString(R.string.documents_export_posologia, medication.posology), body, 2f)
                }
                if (medication.route.isNotEmpty()) {
                    writer.paragraph("  " + context.getString(R.string.documents_export_via_de_administracao, medication.route), body, 2f)
                }
                if (medication.duration.isNotEmpty()) {
                    writer.paragraph("  " + context.getString(R.string.documents_export_duracao, medication.duration), body, 2f)
                }
                if (medication.quantity.isNotEmpty()) {
                    writer.paragraph("  " + context.getString(R.string.documents_export_quantidade, medication.quantity), body, 2f)
                }
                if (medication.observations.isNotEmpty()) {
                    writer.paragraph("  " + context.getString(R.string.documents_export_observacoes, medication.observations), body, 2f)
                }
                writer.gap(4f)
            }

            // Instruções adicionais
            if (additionalInstructions.isNotEmpty()) {
                writer.paragraph(context.getString(R.string.documents_export_orientacoes_adicionais), body, 6f)
                writer.paragraph(additionalInstructions, body, 12f)
            }

            // Data de emissão
            val dateFormat = DateTimeFormatter.ofPattern("dd/MM/uuuu")
            writer.paragraph(context.getString(R.string.documents_export_data_de_emissao, LocalDate.now().format(dateFormat)), body, 12f)

            // Validade
            writer.paragraph(context.getString(R.string.documents_export_validade, validityDays), body, 12f)

            // Rodapé com assinatura
            writer.ensureSpace(60f)
            writer.separator()
            writer.paragraph("______________________________________", body, 4f)
            writer.paragraph(professional.name, body, 2f)
            if (professional.cro.isNotBlank()) {
                writer.paragraph("CRO: ${professional.cro}" + (if (professional.croUF.isNotEmpty()) "/${professional.croUF}" else ""), body, 2f)
            }
            writer.paragraph(context.getString(R.string.documents_export_este_documento_nao_constitui_receita_de_controle_especial), body, 8f)
            writer.paragraph(context.getString(R.string.documents_export_a_assinatura_digital_deve_ser_realizada_em_sistema_proprio), body, 4f)

            writer.finish()

            val directory = File(context.cacheDir, "documents").apply { mkdirs() }
            val file = File(directory, "receita-${sanitize(patient.name)}-${System.currentTimeMillis()}.pdf")
            FileOutputStream(file).use { document.writeTo(it) }
            return file
        } finally {
            document.close()
        }
    }

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

        fun gap(amount: Float) {
            ensureSpace(amount)
            y += amount
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
