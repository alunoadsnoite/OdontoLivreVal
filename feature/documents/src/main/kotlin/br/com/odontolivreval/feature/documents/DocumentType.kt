package br.com.odontolivreval.feature.documents

import androidx.annotation.StringRes

enum class DocumentType(
    @StringRes val label: Int,
    @StringRes val pdfTitle: Int,
    @StringRes val template: Int,
    val filePrefix: String,
) {
    PRESCRIPTION(R.string.doc_prescription, R.string.pdf_prescription_title, R.string.template_prescription, "receita"),
    CERTIFICATE(R.string.doc_certificate, R.string.pdf_certificate_title, R.string.template_certificate, "atestado"),
    BUDGET(R.string.doc_budget, R.string.pdf_budget_title, R.string.template_budget, "orcamento"),
    CONSENT(R.string.doc_consent, R.string.pdf_consent_title, R.string.template_consent, "consentimento"),
}
