package br.com.odontolivreval.feature.documents

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
import br.com.odontolivreval.core.demo.ClinicStore
import br.com.odontolivreval.core.demo.DemoPatient
import br.com.odontolivreval.core.demo.ProfessionalProfile
import java.io.File
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun CertificatePreviewScreen(
    store: ClinicStore,
    patient: DemoPatient,
    certificateId: Long,
    onClose: () -> Unit,
    onShare: (Uri) -> Unit,
    onCancel: (String) -> Unit,
    onNewVersion: () -> Unit,
) {
    val model: CertificateViewModel = viewModel(factory = CertificateViewModel.factory(store, patient.id))
    val listState by model.listState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var pdfUri by remember { mutableStateOf<Uri?>(null) }
    var sharing by remember { mutableStateOf(false) }
    var shareFailed by remember { mutableStateOf(false) }

    val certificate = listState.certificates.find { it.id == certificateId }

    // Strings resolvidas no contexto composable
    val shareTitle = stringResource(R.string.certificates_share_title)
    val pdfNotice = stringResource(R.string.certificates_pdf_notice)
    val previewTitle = stringResource(R.string.certificates_preview_title)
    val preparingPdf = stringResource(R.string.certificates_preparing_pdf)
    val sharingText = stringResource(R.string.certificates_sharing)
    val shareText = stringResource(R.string.certificates_share)
    val newVersionText = stringResource(R.string.certificates_new_version)
    val shareError = stringResource(R.string.certificates_share_error)
    val notFound = stringResource(R.string.certificates_not_found)
    val backText = stringResource(R.string.documents_back)
    val croLabel = stringResource(R.string.pdf_cro)
    val patientLabel = stringResource(R.string.pdf_patient)
    val birthLabel = stringResource(R.string.pdf_birth)
    val phoneLabel = stringResource(R.string.pdf_phone)
    val dateLabel = stringResource(R.string.pdf_date)

    LaunchedEffect(certificateId) {
        model.loadList()
    }

    fun generateAndSharePdf() {
        val cert = certificate ?: return
        sharing = true
        shareFailed = false
        scope.launch {
            try {
                val file = withContext(Dispatchers.IO) {
                    val request = DocumentPdfRequest(
                        type = DocumentType.CERTIFICATE,
                        patient = patient,
                        record = null,
                        profile = ProfessionalProfile(),
                        content = cert.title,
                        title = cert.title,
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

        if (certificate == null) {
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
                Button(
                    enabled = true,
                    onClick = { onNewVersion() },
                ) {
                    Text(newVersionText)
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
