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
                        appendLine("TERMO DE CONSENTIMENTO LIVRE E ESCLARECIDO")
                        appendLine("Tipo: ${c.type.name}")
                        appendLine("Status: ${c.status.name}")
                        appendLine("Versão: ${c.version}")
                        appendLine("Data de emissão: ${c.issueDate}")
                        appendLine("Local: ${c.placeOfIssue}")
                        appendLine("")
                        appendLine("Paciente: ${patient.name}")
                        appendLine("")
                        appendLine("PROCEDIMENTO: ${c.procedure}")
                        appendLine("OBJETIVOS: ${c.objectives}")
                        appendLine("EXPLICAÇÃO: ${c.explanation}")
                        appendLine("BENEFÍCIOS: ${c.benefits}")
                        appendLine("RISCOS E COMPLICAÇÕES: ${c.risks}")
                        appendLine("ALTERNATIVAS: ${c.alternatives}")
                        appendLine("CONSEQUÊNCIAS DA NÃO REALIZAÇÃO: ${c.consequencesOfNotPerforming}")
                        appendLine("CUIDADOS NECESSÁRIOS: ${c.careInstructions}")
                        appendLine("LIMITAÇÕES E INCERTEZAS: ${c.limitations}")
                        appendLine("DÚVIDAS DO PACIENTE: ${c.patientQuestions}")
                        appendLine("DECLARAÇÃO DE ESCLARECIMENTO: ${c.clarificationStatement}")
                        appendLine("")
                        appendLine("DECISÃO DO PACIENTE: ${c.consentProof.patientDecision.orEmpty()}")
                        appendLine("MÉTODO DE COMPROVAÇÃO: ${c.consentProof.proofMethod.orEmpty()}")
                        appendLine("DOCUMENTO ASSINADO: ${if (c.consentProof.hasSignedDocument) "Sim" else "Não"}")
                        appendLine("")
                        appendLine("Profissional: ${c.professionalId ?: "Não informado"}")
                        appendLine("CRO/UF: Preencha no perfil profissional")
                        appendLine("")
                        appendLine("Local e data: ${c.placeOfIssue} - ${c.issueDate}")
                        appendLine("")
                        appendLine("_________________________")
                        appendLine("Assinatura do paciente ou representante legal")
                        appendLine("_________________________")
                        appendLine("Assinatura do cirurgião-dentista")
                        appendLine("_________________________")
                        appendLine("CRO/UF")
                    }
                    val request = DocumentPdfRequest(
                        type = DocumentType.CONSENT,
                        patient = patient,
                        record = null,
                        profile = ProfessionalProfile(),
                        content = content,
                        title = "TCLE - ${c.title}",
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
