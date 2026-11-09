package br.com.odontolivreval.feature.documents

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.odontolivreval.core.demo.ClinicStore
import br.com.odontolivreval.core.demo.DemoPatient

import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun DocumentsScreen(
    store: ClinicStore,
    patient: DemoPatient,
    onClose: () -> Unit,
    onOpenProfile: () -> Unit = {},
    onSaveDocument: (DocumentPdfRequest) -> Unit,
    exportStatus: String? = null,
    archive: @Composable () -> Unit,
) {
    val model: DocumentsViewModel = viewModel(factory = DocumentsViewModel.factory(store, patient.id))
    val state by model.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val croLabel = stringResource(R.string.pdf_cro)
    val patientLabel = stringResource(R.string.pdf_patient)
    val birthLabel = stringResource(R.string.pdf_birth)
    val phoneLabel = stringResource(R.string.pdf_phone)
    val dateLabel = stringResource(R.string.pdf_date)
    val shareTitle = stringResource(R.string.documents_share_title)
    val pdfTitle = stringResource(state.type.pdfTitle)

    val scope = rememberCoroutineScope()
    var shareFailed by remember { mutableStateOf(false) }
    var portalFailed by remember { mutableStateOf(false) }
    var sharing by remember { mutableStateOf(false) }
    var encryptPdf by remember { mutableStateOf(false) }
    var pdfPassword by remember { mutableStateOf("") }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { model.load() }

    fun request(): DocumentPdfRequest = DocumentPdfRequest(
        state.type, patient, state.record, state.profile, state.content, pdfTitle,
        ClinicPdf.PdfLabels(croLabel, patientLabel, birthLabel, phoneLabel, dateLabel),
    )

    fun openPortal(url: String) {
        portalFailed = try {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            false
        } catch (_: ActivityNotFoundException) {
            true
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.documents_title), style = MaterialTheme.typography.headlineLarge)
        Text(stringResource(R.string.documents_patient, patient.name))
        Text(stringResource(R.string.documents_notice), style = MaterialTheme.typography.bodySmall)

        when {
            state.loading -> Text(stringResource(R.string.documents_loading))
            state.failed -> {
                Text(stringResource(R.string.documents_load_error), color = MaterialTheme.colorScheme.error)
                OutlinedButton(onClick = model::load) { Text(stringResource(R.string.documents_retry)) }
                OutlinedButton(onClick = onClose) { Text(stringResource(R.string.documents_back)) }
            }
            else -> {
                if (state.profileReady) {
                    Text(
                        stringResource(R.string.documents_professional, state.profile.name, state.profile.cro),
                        style = MaterialTheme.typography.titleMedium,
                    )
                } else {
                    Text(
                        stringResource(R.string.documents_profile_missing),
                        color = MaterialTheme.colorScheme.error,
                    )
                    OutlinedButton(onClick = onOpenProfile) {
                        Text(stringResource(R.string.documents_open_profile))
                    }
                }

                Text(stringResource(R.string.documents_type), style = MaterialTheme.typography.titleSmall)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DocumentType.entries.forEach { type ->
                        FilterChip(
                            selected = state.type == type,
                            onClick = { model.selectType(type) },
                            label = { Text(stringResource(type.label)) },
                        )
                    }
                }

                val template = stringResource(state.type.template, patient.name)
                LaunchedEffect(state.type, state.loading) {
                    if (!state.loading) model.ensureTemplate(state.type, template)
                }

                OutlinedTextField(
                    value = state.content,
                    onValueChange = model::changeContent,
                    label = { Text(stringResource(R.string.documents_content)) },
                    minLines = 8,
                    modifier = Modifier.fillMaxWidth(),
                )

                when (state.error) {
                    DocumentsError.EMPTY_CONTENT -> Text(
                        stringResource(R.string.documents_empty_content),
                        color = MaterialTheme.colorScheme.error,
                    )
                    DocumentsError.PROFILE_MISSING -> Text(
                        stringResource(R.string.documents_profile_missing),
                        color = MaterialTheme.colorScheme.error,
                    )
                    else -> Unit
                }
                if (state.shared) {
                    Text(stringResource(R.string.documents_shared), color = MaterialTheme.colorScheme.primary)
                }

                // Opção de criptografar PDF antes de compartilhar
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    androidx.compose.foundation.layout.Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                    ) {
                        androidx.compose.material3.Checkbox(
                            checked = encryptPdf,
                            onCheckedChange = { encryptPdf = it },
                        )
                        Text(stringResource(R.string.documents_encrypt_pdf))
                    }
                    if (encryptPdf) {
                        OutlinedTextField(
                            value = pdfPassword,
                            onValueChange = { pdfPassword = it },
                            label = { Text(stringResource(R.string.documents_pdf_password)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }

                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { if (model.validate()) onSaveDocument(request()) }) {
                        Text(stringResource(R.string.documents_save_pdf))
                    }
                    val buttonText = if (encryptPdf) stringResource(R.string.documents_share_encrypted) else stringResource(R.string.documents_generate)
                    OutlinedButton(
                        enabled = !sharing,
                        onClick = {
                            if (model.validate()) {
                                val draft = request()
                                val shouldEncrypt = encryptPdf
                                val passwordValue = pdfPassword
                                sharing = true
                                shareFailed = false
                                scope.launch {
                                    try {
                                        val file = withContext(Dispatchers.IO) {
                                            ClinicPdf.shareFile(context.applicationContext, draft)
                                        }
                                        val fileToShare = if (shouldEncrypt && passwordValue.isNotBlank()) {
                                            encryptPdf(file, passwordValue.toCharArray())
                                        } else {
                                            file
                                        }
                                        sharePdf(context, fileToShare, shareTitle)
                                        model.markShared()
                                    } catch (_: IOException) {
                                        shareFailed = true
                                    } catch (_: ActivityNotFoundException) {
                                        shareFailed = true
                                    } finally {
                                        sharing = false
                                    }
                                }
                            }
                        },
                    ) { Text(buttonText) }                }
                exportStatus?.let { Text(it) }
                if (shareFailed) Text(stringResource(R.string.documents_pdf_error), color = MaterialTheme.colorScheme.error)

                Text(stringResource(R.string.signature_title), style = MaterialTheme.typography.titleMedium)
                Text(stringResource(R.string.signature_flow))
                Text(stringResource(R.string.signature_provider_notice), style = MaterialTheme.typography.bodySmall)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { openPortal("https://assinador.iti.br/") }) {
                        Text(stringResource(R.string.signature_gov))
                    }
                    OutlinedButton(onClick = { openPortal("https://prescricao.cfo.org.br/index") }) {
                        Text(stringResource(R.string.signature_cfo))
                    }
                    OutlinedButton(onClick = { openPortal("https://validar.iti.gov.br/") }) {
                        Text(stringResource(R.string.signature_validate))
                    }
                }
                if (portalFailed) Text(stringResource(R.string.signature_portal_error), color = MaterialTheme.colorScheme.error)
                archive()

                OutlinedButton(onClick = onClose) { Text(stringResource(R.string.documents_back)) }
            }
        }
    }
}

private fun sharePdf(context: Context, file: java.io.File, title: String) {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "application/pdf"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, title))
}
