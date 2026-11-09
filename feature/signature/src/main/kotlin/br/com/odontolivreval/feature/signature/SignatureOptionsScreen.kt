package br.com.odontolivreval.feature.signature

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.odontolivreval.core.demo.DemoPatient
import br.com.odontolivreval.core.demo.ClinicStore
import br.com.odontolivreval.core.demo.DocumentSignatureFile
import br.com.odontolivreval.core.demo.DocumentHashFile
import br.com.odontolivreval.core.demo.SignatureStatus
import br.com.odontolivreval.core.demo.SignatureModality

@Composable
fun SignatureOptionsScreen(
    store: ClinicStore,
    patient: DemoPatient,
    documentType: String,
    documentId: Long,
    documentVersion: Int,
    documentTitle: String,
    onClose: () -> Unit,
    onCapture: (SignatureModality) -> Unit,
    onImportExternal: () -> Unit,
) {
    val model: SignatureViewModel = viewModel(factory = SignatureViewModel.factory(store, patient.id))
    val state by model.listState.collectAsStateWithLifecycle()
    LaunchedEffect(model) { model.loadSignaturesAndHashes() }

    Column(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(R.string.signature_options_title),
            style = MaterialTheme.typography.headlineLarge,
        )
        Text(
            text = stringResource(R.string.signature_options_patient, patient.name),
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = stringResource(R.string.signature_options_document, documentTitle),
            style = MaterialTheme.typography.bodyMedium,
        )

        Text(
            text = stringResource(R.string.signature_options_choose_modality),
            style = MaterialTheme.typography.titleMedium,
        )
        OutlinedButton(onClick = onImportExternal) {
            Text(stringResource(R.string.signature_integrity_import_pdf))
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Button(
                onClick = { onCapture(SignatureModality.ELECTRONIC_ACKNOWLEDGEMENT) },
            ) {
                Text(stringResource(R.string.signature_options_handwritten_digital))
            }
            Button(
                onClick = { onCapture(SignatureModality.HANDWRITTEN_ON_PAPER) },
            ) {
                Text(stringResource(R.string.signature_options_handwritten_paper))
            }
            Button(enabled = false, onClick = {}) {
                Text(stringResource(R.string.signature_options_cryptographic))
            }
        }

        Text(
            text = stringResource(R.string.signature_options_history),
            style = MaterialTheme.typography.titleMedium,
        )

        when {
            state.loading -> {
                Text(text = stringResource(R.string.signature_options_loading))
            }
            state.error != null -> {
                Text(
                    text = stringResource(R.string.signature_options_error),
                    color = MaterialTheme.colorScheme.error,
                )
                OutlinedButton(onClick = model::loadSignaturesAndHashes) {
                    Text(stringResource(R.string.signature_options_retry))
                }
            }
            state.signatures.isEmpty() -> {
                Text(text = stringResource(R.string.signature_options_empty), style = MaterialTheme.typography.bodyMedium)
            }
            else -> {
                state.signatures.forEach { sig ->
                    SignatureRow(
                        signature = sig,
                    )
                }
            }
        }

        OutlinedButton(onClick = onClose) {
            Text(stringResource(R.string.signature_options_back))
        }
    }
}

@Composable
private fun SignatureRow(
    signature: br.com.odontolivreval.core.demo.DocumentSignatureFile,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = signature.documentType,
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = "${stringResource(R.string.signature_options_modality, signature.modality.name)} · ${stringResource(R.string.signature_options_status, signature.status.name)}",
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}
