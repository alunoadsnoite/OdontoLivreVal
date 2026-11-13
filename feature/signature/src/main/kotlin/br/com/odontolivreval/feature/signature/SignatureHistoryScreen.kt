package br.com.odontolivreval.feature.signature

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
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.odontolivreval.core.demo.DemoPatient
import br.com.odontolivreval.core.demo.ClinicStore
import br.com.odontolivreval.core.demo.DocumentHashFile
import br.com.odontolivreval.core.demo.DocumentSignatureFile
import br.com.odontolivreval.core.demo.SignatureStatus

@Composable
fun SignatureHistoryScreen(
    store: ClinicStore,
    patient: DemoPatient,
    onClose: () -> Unit,
) {
    val model: SignatureViewModel = viewModel(factory = SignatureViewModel.factory(store, patient.id))
    val state by model.listState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(R.string.signature_history_title),
            style = MaterialTheme.typography.headlineLarge,
        )
        Text(
            text = stringResource(R.string.signature_history_patient, patient.name),
            style = MaterialTheme.typography.titleMedium,
        )

        when {
            state.loading -> {
                Text(text = stringResource(R.string.signature_history_loading))
            }
            state.error != null -> {
                Text(
                    text = stringResource(R.string.signature_history_error),
                    color = MaterialTheme.colorScheme.error,
                )
                OutlinedButton(onClick = model::loadSignaturesAndHashes) {
                    Text(stringResource(R.string.signature_history_retry))
                }
            }
            state.signatures.isEmpty() && state.hashes.isEmpty() -> {
                Text(text = stringResource(R.string.signature_history_empty), style = MaterialTheme.typography.bodyMedium)
            }
            else -> {
                if (state.signatures.isNotEmpty()) {
                    Text(
                        text = stringResource(R.string.signature_history_document),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    state.signatures.forEach { sig ->
                        SignatureRow(
                            signature = sig,
                        )
                    }
                }
                if (state.hashes.isNotEmpty()) {
                    Text(
                        text = stringResource(R.string.signature_history_document),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    state.hashes.forEach { hash ->
                        HashRow(
                            hash = hash,
                        )
                    }
                }
            }
        }

        OutlinedButton(onClick = onClose) {
            Text(stringResource(R.string.signature_history_back))
        }
    }
}

@Composable
private fun SignatureRow(
    signature: DocumentSignatureFile,
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
                text = "${stringResource(R.string.signature_options_modality, stringResource(signature.modality.labelResource))} · ${stringResource(R.string.signature_options_status, stringResource(signature.status.labelResource))}",
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun HashRow(
    hash: DocumentHashFile,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = hash.documentType,
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = "Hash: ${hash.hashValue.take(16)}…",
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}
