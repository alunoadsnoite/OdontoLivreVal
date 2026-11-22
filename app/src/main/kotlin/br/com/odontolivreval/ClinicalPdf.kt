package br.com.odontolivreval

import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.text.StaticLayout
import android.text.TextPaint
import br.com.odontolivreval.core.demo.ClinicStore
import br.com.odontolivreval.core.demo.DemoPatient
import br.com.odontolivreval.core.demo.VisitRecord
import java.io.IOException
import java.io.OutputStream
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

internal sealed interface ClinicalExport {
    data class Evolution(val visitId: Long) : ClinicalExport
    data object RecordCopy : ClinicalExport
    data class Receipt(val recipient: String, val inventory: String) : ClinicalExport
}

internal object ClinicalPdf {
    private val dateTime = DateTimeFormatter.ofPattern("dd/MM/uuuu HH:mm")

    suspend fun text(store: ClinicStore, patient: DemoPatient, request: ClinicalExport): String {
        val header = "Paciente: ${patient.name}\n"
        return when (request) {
            is ClinicalExport.Evolution -> {
                val visit = store.listVisits(patient.id).firstOrNull { it.id == request.visitId }
                    ?: throw IOException("Visit missing")
                "EVOLUÇÃO ODONTOLÓGICA\n\n$header\n${evolution(visit)}"
            }
            ClinicalExport.RecordCopy -> {
                val record = store.getPatientRecord(patient.id)
                val visits = store.listVisits(patient.id).asReversed()
                buildString {
                    appendLine("CÓPIA DA FICHA CLÍNICA E DAS EVOLUÇÕES")
                    appendLine()
                    appendLine(header)
                    appendLine("Recorte do prontuário. Anexos e outros documentos são entregues separadamente.")
                    appendLine()
                    if (record != null) {
                        appendLine("Nascimento: ${record.birthDate}")
                        appendLine("Telefone: ${record.phone}")
                        appendLine("Queixa: ${record.complaint}")
                        appendLine("Histórico: ${record.history}")
                        appendLine("Observações: ${record.observations}")
                    }
                    if (visits.isEmpty()) appendLine("Sem evoluções salvas.")
                    visits.forEach { appendLine(); appendLine(evolution(it)) }
                }
            }
            is ClinicalExport.Receipt -> {
                val profile = store.getProfessionalProfile()
                if (profile.name.isBlank() || profile.cro.isBlank() || profile.croUF.isBlank()) {
                    throw IOException("Incomplete professional profile")
                }
                buildString {
                    appendLine("RECIBO DE ENTREGA DE DOCUMENTOS")
                    appendLine()
                    appendLine(header)
                    appendLine("Profissional: ${profile.name} · CRO-${profile.croUF} ${profile.cro}")
                    appendLine("Destinatário e vínculo: ${request.recipient}")
                    appendLine("Documentos entregues, com indicação de original ou cópia:")
                    appendLine(request.inventory)
                    appendLine()
                    appendLine("Data e hora da entrega: __________________________________")
                    appendLine()
                    appendLine("Assinatura do destinatário: _______________________________")
                    appendLine()
                    appendLine("Assinatura do profissional: ________________________________")
                }
            }
        }
    }

    private fun evolution(visit: VisitRecord): String = buildString {
        val day = LocalDate.ofEpochDay(visit.visitDay)
        val time = LocalTime.of(visit.visitMinute / 60, visit.visitMinute % 60)
        appendLine("Evolução nº ${visit.id} · Atendimento: ${dateTime.format(LocalDateTime.of(day, time))}")
        visit.previousVisitId?.let { appendLine("Revisão da evolução nº $it") }
        visit.recordedAtMillis?.let {
            appendLine("Registro: ${dateTime.format(Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()))}")
        }
        appendLine(if (visit.professionalName.isBlank()) "Registro antigo sem profissional identificado."
            else "Profissional: ${visit.professionalName} · CRO-${visit.professionalCroUf} ${visit.professionalCro}")
        appendLine("Queixa: ${visit.complaint}")
        appendLine("Anamnese: ${visit.anamnesis}")
        appendLine("Diagnóstico: ${visit.diagnosis}")
        appendLine("Conduta: ${visit.conduct}")
        appendLine("Observações: ${visit.observations}")
        if (visit.odontogram.isNotEmpty()) {
            appendLine("Marcações do odontograma:")
            visit.odontogram.toSortedMap().forEach { (position, code) -> appendLine("$position: $code") }
        }
    }

    fun write(text: String, output: OutputStream) {
        val margin = 42
        val width = 595
        val height = 842
        val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 12f; color = Color.BLACK }
        val layout = StaticLayout.Builder.obtain(text, 0, text.length, paint, width - margin * 2)
            .setIncludePad(false).build()
        val pdf = PdfDocument()
        try {
            var first = 0
            var pageNumber = 1
            while (first < layout.lineCount) {
                val top = layout.getLineTop(first)
                var end = first + 1
                while (end < layout.lineCount && layout.getLineTop(end + 1) - top <= height - margin * 2) end++
                val page = pdf.startPage(PdfDocument.PageInfo.Builder(width, height, pageNumber++).create())
                val canvas = page.canvas
                val saved = canvas.save()
                try {
                    canvas.clipRect(margin, margin, width - margin, margin + layout.getLineTop(end) - top)
                    canvas.translate(margin.toFloat(), (margin - top).toFloat())
                    layout.draw(canvas)
                } finally {
                    canvas.restoreToCount(saved)
                    pdf.finishPage(page)
                }
                first = end
            }
            pdf.writeTo(output)
        } finally {
            pdf.close()
        }
    }
}
