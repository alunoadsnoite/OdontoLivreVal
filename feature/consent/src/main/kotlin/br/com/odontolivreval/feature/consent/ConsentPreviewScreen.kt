package br.com.odontolivreval.feature.consent

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
import br.com.odontolivreval.core.demo.ConsentType
import br.com.odontolivreval.core.demo.DemoPatient
import br.com.odontolivreval.core.demo.ClinicStore
import br.com.odontolivreval.core.demo.ProfessionalProfile
import br.com.odontolivreval.feature.documents.DocumentPdfRequest
import br.com.odontolivreval.feature.documents.DocumentType
import br.com.odontolivreval.feature.documents.ClinicPdf
import java.io.File
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ConsentPreviewScreen(
    store: ClinicStore,
    patient: DemoPatient,
    consentId: Long,
    onClose: () -> Unit,
    onShare: (Uri) -> Unit,
) {
    val model: ConsentViewModel = viewModel(factory = ConsentViewModel.factory(store, patient.id))
    val formState by model.formState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val resources = LocalResources.current
    val scope = rememberCoroutineScope()

    var pdfUri by remember { mutableStateOf<Uri?>(null) }
    var sharing by remember { mutableStateOf(false) }
    var shareFailed by remember { mutableStateOf(false) }

    val consent = formState.consent

    val shareTitle = stringResource(R.string.consents_share_title)
    val pdfNotice = stringResource(R.string.consents_pdf_notice)
    val previewTitle = stringResource(R.string.consents_preview_title)
    val sharingText = stringResource(R.string.consents_sharing)
    val shareText = stringResource(R.string.consents_share)
    val shareError = stringResource(R.string.consents_share_error)
    val notFound = stringResource(R.string.consents_not_found)
    val backText = stringResource(R.string.consents_back)
    val croLabel = stringResource(R.string.pdf_cro)
    val patientLabel = stringResource(R.string.pdf_patient)
    val birthLabel = stringResource(R.string.pdf_birth)
    val phoneLabel = stringResource(R.string.pdf_phone)
    val dateLabel = stringResource(R.string.pdf_date)

    LaunchedEffect(consentId) {
        model.loadConsent(consentId)
    }

    fun generateAndSharePdf() {
        val c = consent ?: return
        sharing = true
        shareFailed = false
        scope.launch {
            try {
                val file = withContext(Dispatchers.IO) {
                    val content = buildString {
                        appendLine(resources.getString(R.string.consent_export_termo_de_consentimento_livre_e_esclarecido))
                        appendLine(resources.getString(R.string.consent_export_tipo, resources.getString(typeLabel(c.type))))
                        appendLine(resources.getString(R.string.consent_export_status, resources.getString(c.status.labelResource)))
                        appendLine(resources.getString(R.string.consent_export_versao, c.version))
                        appendLine(resources.getString(R.string.consent_export_data_de_emissao, c.issueDate))
                        appendLine(resources.getString(R.string.consent_export_local, c.placeOfIssue))
                        appendLine("")
                        appendLine(resources.getString(R.string.consent_export_paciente, patient.name))
                        appendLine("")
                        appendLine(resources.getString(R.string.consent_export_procedimento, c.procedure))
                        appendLine(resources.getString(R.string.consent_export_objetivos, c.objectives))
                        appendLine(resources.getString(R.string.consent_export_explicacao, c.explanation))
                        appendLine(resources.getString(R.string.consent_export_beneficios, c.benefits))
                        appendLine(resources.getString(R.string.consent_export_riscos_e_complicacoes, c.risks))
                        appendLine(resources.getString(R.string.consent_export_alternativas, c.alternatives))
                        appendLine(resources.getString(R.string.consent_export_consequencias_da_nao_realizacao, c.consequencesOfNotPerforming))
                        appendLine(resources.getString(R.string.consent_export_cuidados_necessarios, c.careInstructions))
                        appendLine(resources.getString(R.string.consent_export_limitacoes_e_incertezas, c.limitations))
                        appendLine(resources.getString(R.string.consent_export_duvidas_do_paciente, c.patientQuestions))
                        appendLine(resources.getString(R.string.consent_export_declaracao_de_esclarecimento, c.clarificationStatement))
                        appendLine("")
                        appendLine(resources.getString(R.string.consent_export_decisao_do_paciente, c.consentProof.patientDecision.orEmpty()))
                        appendLine(resources.getString(R.string.consent_export_metodo_de_comprovacao, c.consentProof.proofMethod.orEmpty()))
                        appendLine(resources.getString(R.string.consent_export_documento_assinado, resources.getString(if (c.consentProof.hasSignedDocument) R.string.consent_export_yes else R.string.consent_export_no)))
                        appendLine("")
                        appendLine(resources.getString(R.string.consent_export_profissional, c.professionalId ?: resources.getString(R.string.consent_export_not_provided)))
                        appendLine(resources.getString(R.string.consent_export_cro_uf_preencha_no_perfil_profissional))
                        appendLine("")
                        appendLine(resources.getString(R.string.consent_export_local_e_data, c.placeOfIssue, c.issueDate))
                        appendLine("")
                        appendLine("_________________________")
                        appendLine(resources.getString(R.string.consent_export_assinatura_do_paciente_ou_representante_legal))
                        appendLine("_________________________")
                        appendLine(resources.getString(R.string.consent_export_assinatura_do_cirurgiao_dentista))
                        appendLine("_________________________")
                        appendLine(resources.getString(R.string.consent_export_cro_uf))
                    }
                    val request = DocumentPdfRequest(
                        type = DocumentType.CONSENT,
                        patient = patient,
                        record = null,
                        profile = ProfessionalProfile(),
                        content = content,
                        title = resources.getString(R.string.consent_export_document_title, c.title),
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

        if (consent == null) {
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
