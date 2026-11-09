package br.com.odontolivreval.feature.documents

import br.com.odontolivreval.core.demo.DemoPatient
import br.com.odontolivreval.core.demo.PatientRecord
import br.com.odontolivreval.core.demo.ProfessionalProfile

data class DocumentPdfRequest(
    val type: DocumentType,
    val patient: DemoPatient,
    val record: PatientRecord?,
    val profile: ProfessionalProfile,
    val content: String,
    val title: String,
    val labels: ClinicPdf.PdfLabels,
) {
    val filename: String get() = "${type.filePrefix}.pdf"
}
