package br.com.odontolivreval.feature.treatment

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.odontolivreval.core.demo.BudgetItem
import br.com.odontolivreval.core.demo.BudgetStatus
import br.com.odontolivreval.core.demo.ClinicStore
import br.com.odontolivreval.core.demo.DemoPatient
import br.com.odontolivreval.core.demo.ProfessionalProfile
import br.com.odontolivreval.feature.documents.ClinicPdf
import br.com.odontolivreval.feature.documents.DocumentPdfRequest
import br.com.odontolivreval.feature.documents.DocumentType
import java.io.File
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun TreatmentBudgetPreviewScreen(
    store: ClinicStore,
    patient: DemoPatient,
    budgetId: Long,
    planId: Long,
    onClose: () -> Unit,
    onShare: (Uri) -> Unit,
) {
    val model: TreatmentBudgetViewModel = viewModel(factory = TreatmentBudgetViewModel.factory(store, patient.id))
    val formState by model.formState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val resources = LocalResources.current
    val scope = rememberCoroutineScope()

    var pdfUri by remember { mutableStateOf<Uri?>(null) }
    var sharing by remember { mutableStateOf(false) }
    var shareFailed by remember { mutableStateOf(false) }

    val budget = formState.budget

    val shareTitle = stringResource(R.string.treatment_budget_share_title)
    val pdfNotice = stringResource(R.string.treatment_budget_pdf_notice)
    val previewTitle = stringResource(R.string.treatment_budget_preview_title)
    val sharingText = stringResource(R.string.treatment_budget_sharing)
    val shareText = stringResource(R.string.treatment_budget_share)
    val shareError = stringResource(R.string.treatment_budget_share_error)
    val notFound = stringResource(R.string.treatment_budget_not_found)
    val backText = stringResource(R.string.treatment_plans_back)
    val croLabel = stringResource(R.string.pdf_cro)
    val patientLabel = stringResource(R.string.pdf_patient)
    val birthLabel = stringResource(R.string.pdf_birth)
    val phoneLabel = stringResource(R.string.pdf_phone)
    val dateLabel = stringResource(R.string.pdf_date)

    LaunchedEffect(budgetId) {
        model.loadBudget(budgetId)
    }

    fun generateAndSharePdf() {
        val b = budget ?: return
        sharing = true
        shareFailed = false
        scope.launch {
            try {
                val file = withContext(Dispatchers.IO) {
                    val content = buildString {
                        appendLine(resources.getString(R.string.treatment_export_orcamento_odontologico))
                        appendLine(resources.getString(R.string.treatment_export_numero, b.budgetNumber))
                        appendLine(resources.getString(R.string.treatment_export_revisao, b.revision))
                        appendLine(resources.getString(R.string.treatment_export_data_de_emissao, b.issueDate))
                        appendLine(resources.getString(R.string.treatment_export_validade, b.validityDays))
                        appendLine("")
                        appendLine(resources.getString(R.string.treatment_export_paciente, patient.name))
                        appendLine(resources.getString(R.string.treatment_export_plano_de_tratamento_id, b.planId))
                        appendLine("")
                        appendLine(resources.getString(R.string.treatment_export_procedimentos))
                        b.items.forEachIndexed { index, item ->
                            appendLine("${index + 1}. ${item.description}")
                            if (item.tooth != null) appendLine("   " + resources.getString(R.string.treatment_export_dente, item.tooth))
                            appendLine("   " + resources.getString(R.string.treatment_export_quantidade, item.quantity))
                            appendLine("   " + resources.getString(R.string.treatment_export_valor_unitario, item.unitValue.toPlainString()))
                            appendLine("   " + resources.getString(R.string.treatment_export_subtotal, item.totalValue.toPlainString()))
                            appendLine("")
                        }
                        appendLine(resources.getString(R.string.treatment_export_total_bruto, b.items.sumOf { it.totalValue }.toPlainString()))
                        appendLine(resources.getString(R.string.treatment_export_desconto, b.discount.toPlainString()))
                        appendLine(resources.getString(R.string.treatment_export_total_final, (b.items.sumOf { it.totalValue } - b.discount).toPlainString()))
                        appendLine("")
                        appendLine(resources.getString(R.string.treatment_export_condicoes_de_pagamento, b.paymentConditions))
                        appendLine(resources.getString(R.string.treatment_export_observacoes, b.observations))
                        appendLine("")
                        appendLine(resources.getString(R.string.treatment_export_status, resources.getString(b.status.labelResource)))
                    }
                    val request = DocumentPdfRequest(
                        type = DocumentType.BUDGET,
                        patient = patient,
                        record = null,
                        profile = ProfessionalProfile(),
                        content = content,
                        title = resources.getString(R.string.treatment_export_document_title, b.budgetNumber),
                        labels = ClinicPdf.PdfLabels(
                            cro = croLabel,
                            patient = patientLabel,
                            birth = birthLabel,
                            phone = phoneLabel,
                            date = dateLabel,
                        ),
                    )
                    ClinicPdf.shareFile(context.applicationContext, request)
                }
                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file,
                )
                pdfUri = uri
                onShare(uri)
            } catch (_: IOException) {
                shareFailed = true
            } finally {
                sharing = false
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = previewTitle,
            style = MaterialTheme.typography.headlineLarge,
        )

        if (budget == null) {
            Text(text = notFound, style = MaterialTheme.typography.bodyMedium)
        } else {
            Text(
                text = pdfNotice,
                style = MaterialTheme.typography.bodySmall,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(
                    enabled = !sharing,
                    onClick = { generateAndSharePdf() },
                ) {
                    Text(if (sharing) sharingText else shareText)
                }
            }

            if (shareFailed) {
                Text(
                    text = shareError,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }

        OutlinedButton(onClick = onClose) {
            Text(backText)
        }
    }
}
