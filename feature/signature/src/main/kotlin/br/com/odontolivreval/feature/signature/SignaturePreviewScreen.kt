package br.com.odontolivreval.feature.signature

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
import br.com.odontolivreval.core.demo.DemoPatient
import br.com.odontolivreval.core.demo.ClinicStore
import br.com.odontolivreval.core.demo.DocumentHash
import br.com.odontolivreval.core.demo.DocumentSignature
import br.com.odontolivreval.core.demo.DocumentSignatureFile
import br.com.odontolivreval.core.demo.ProfessionalProfile
import br.com.odontolivreval.feature.documents.DocumentPdfRequest
import br.com.odontolivreval.feature.documents.DocumentType
import br.com.odontolivreval.feature.documents.ClinicPdf
import java.io.File
import java.io.IOException
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Calcula o hash SHA-256 de uma string.
 */
private fun computeSha256(text: String): String? {
    return try {
        MessageDigest.getInstance("SHA-256").digest(text.toByteArray()).joinToString("") { "%02x".format(it) }
    } catch (_: NoSuchAlgorithmException) {
        null
    }
}

@Composable
fun SignaturePreviewScreen(
    store: ClinicStore,
    patient: DemoPatient,
    documentType: String,
    documentId: Long,
    documentVersion: Int,
    documentTitle: String,
    documentContent: String,
    onClose: () -> Unit,
    onSignatureCreated: () -> Unit,
    onShareSigned: (Uri) -> Unit,
) {
    val model: SignatureViewModel = viewModel(factory = SignatureViewModel.factory(store, patient.id))
    val formState by model.formState.collectAsStateWithLifecycle()
    val hashState by model.hashState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val resources = LocalResources.current
    val scope = rememberCoroutineScope()

    var pdfUri by remember { mutableStateOf<Uri?>(null) }
    var sharing by remember { mutableStateOf(false) }
    var shareFailed by remember { mutableStateOf(false) }

    val shareTitle = stringResource(R.string.signature_preview_share_title)
    val pdfNotice = stringResource(R.string.signature_preview_pdf_notice)
    val previewTitle = stringResource(R.string.signature_preview_title)
    val sharingText = stringResource(R.string.signature_preview_sharing)
    val shareText = stringResource(R.string.signature_preview_share)
    val shareError = stringResource(R.string.signature_preview_share_error)
    val notFound = stringResource(R.string.signature_preview_not_found)
    val backText = stringResource(R.string.signature_preview_back)
    val croLabel = stringResource(R.string.pdf_cro)
    val patientLabel = stringResource(R.string.pdf_patient)
    val birthLabel = stringResource(R.string.pdf_birth)
    val phoneLabel = stringResource(R.string.pdf_phone)
    val dateLabel = stringResource(R.string.pdf_date)

    LaunchedEffect(documentId) {
        // Carregar dados existentes se houver
        model.loadSignaturesAndHashes()
    }

    fun generateAndSharePdf() {
        sharing = true
        shareFailed = false
        scope.launch {
            try {
                val file = withContext(Dispatchers.IO) {
                    val content = buildString {
                        appendLine(resources.getString(R.string.signature_export_termo_documento, documentTitle))
                        appendLine(resources.getString(R.string.signature_export_tipo, documentType))
                        appendLine(resources.getString(R.string.signature_export_versao, documentVersion))
                        appendLine(resources.getString(R.string.signature_export_paciente, patient.name))
                        appendLine("")
                        appendLine(documentContent)
                        appendLine("")
                        appendLine("---")
                        appendLine(resources.getString(R.string.signature_export_assinatura_do_responsavel))
                        appendLine(resources.getString(R.string.signature_export_nome, formState.signerName))
                        appendLine(resources.getString(R.string.signature_export_funcao, formState.signerRole))
                        appendLine(resources.getString(R.string.signature_export_data_hora, java.time.LocalDateTime.now()))
                        appendLine("")
                        appendLine(resources.getString(R.string.signature_export_este_documento_foi_preparado_para_assinatura))
                        appendLine(resources.getString(R.string.signature_export_a_assinatura_manuscrita_capturada_e_uma_representacao_visual))
                        appendLine(resources.getString(R.string.signature_export_nao_constitui_assinatura_digital_certificada_icp_brasil))
                    }
                    val request = DocumentPdfRequest(
                        type = DocumentType.CONSENT,
                        patient = patient,
                        record = null,
                        profile = ProfessionalProfile(),
                        content = content,
                        title = resources.getString(R.string.signature_export_document_title, documentTitle),
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
                onShareSigned(uri)
            } catch (_: IOException) {
                shareFailed = true
            } finally {
                sharing = false
            }
        }
    }

    fun verifyIntegrity() {
        val currentHash = computeSha256(documentContent)
        if (currentHash != null) {
            model.verifyHash(documentType, documentId, documentVersion, currentHash)
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

        if (formState.saving) {
            Text(text = stringResource(R.string.signature_preview_saving))
        }

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

        // Verificação de integridade
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(R.string.signature_preview_integrity),
                style = MaterialTheme.typography.titleMedium,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(
                    enabled = !hashState.loading,
                    onClick = { verifyIntegrity() },
                ) {
                    Text(stringResource(R.string.signature_preview_verify_integrity))
                }
            }

            if (hashState.loading) {
                Text(text = stringResource(R.string.signature_preview_verifying))
            } else if (hashState.integrityMatches) {
                Text(
                    text = stringResource(R.string.signature_preview_integrity_ok),
                    color = MaterialTheme.colorScheme.primary,
                )
            } else if (hashState.storedHash != null) {
                Text(
                    text = stringResource(R.string.signature_preview_integrity_mismatch),
                    color = MaterialTheme.colorScheme.error,
                )
            } else {
                Text(
                    text = stringResource(R.string.signature_preview_integrity_no_hash),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        OutlinedButton(onClick = onClose) {
            Text(backText)
        }
    }
}
