package br.com.odontolivreval.feature.documents

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.odontolivreval.core.demo.ClinicStore
import br.com.odontolivreval.core.demo.DemoPatient

@Composable
fun DocumentsScreen(
    store: ClinicStore,
    patient: DemoPatient,
    onClose: () -> Unit,
    onOpenProfile: () -> Unit = {},
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

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.documents_title), style = MaterialTheme.typography.headlineLarge)
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
                        stringResource(R.string.documents_signed_by, state.profile.name, state.profile.cro),
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

                Button(
                    onClick = {
                        if (model.validate()) {
                            val labels = ClinicPdf.PdfLabels(
                                cro = croLabel,
                                patient = patientLabel,
                                birth = birthLabel,
                                phone = phoneLabel,
                                date = dateLabel,
                            )
                            val file = ClinicPdf.write(
                                context,
                                state.type,
                                patient,
                                state.record,
                                state.profile,
                                state.content,
                                pdfTitle,
                                labels,
                            )
                            sharePdf(context, file, shareTitle)
                            model.markShared()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(stringResource(R.string.documents_generate)) }

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
