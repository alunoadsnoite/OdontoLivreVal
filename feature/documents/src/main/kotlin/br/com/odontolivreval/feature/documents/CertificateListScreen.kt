package br.com.odontolivreval.feature.documents

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.odontolivreval.core.demo.CertificateStatus
import br.com.odontolivreval.core.demo.ClinicStore
import br.com.odontolivreval.core.demo.DemoPatient

@Composable
fun CertificateListScreen(
    store: ClinicStore,
    patient: DemoPatient,
    onClose: () -> Unit,
    onCreateCertificate: () -> Unit,
    onViewCertificate: (Long) -> Unit,
    onCancelCertificate: (Long, String) -> Unit,
    onCreateVersion: (Long) -> Unit,
) {
    val model: CertificateViewModel = viewModel(factory = CertificateViewModel.factory(store, patient.id))
    val state by model.listState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var cancelDialogOpen by remember { mutableStateOf(false) }
    var cancelTargetId by remember { mutableStateOf(-1L) }
    var cancelReason by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(R.string.certificates_title),
            style = MaterialTheme.typography.headlineLarge,
        )
        Text(
            text = stringResource(R.string.certificates_patient, patient.name),
            style = MaterialTheme.typography.titleMedium,
        )

        Button(
            onClick = onCreateCertificate,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.certificates_create))
        }

        when {
            state.loading -> {
                Text(text = stringResource(R.string.certificates_loading))
            }
            state.error != null -> {
                Text(
                    text = stringResource(R.string.certificates_load_error),
                    color = MaterialTheme.colorScheme.error,
                )
                OutlinedButton(onClick = model::loadList) {
                    Text(stringResource(R.string.documents_retry))
                }
            }
            state.certificates.isEmpty() -> {
                Text(text = stringResource(R.string.certificates_empty), style = MaterialTheme.typography.bodyMedium)
            }
            else -> {
                Text(
                    text = stringResource(R.string.certificates_history),
                    style = MaterialTheme.typography.titleMedium,
                )
                state.certificates.forEach { cert ->
                    CertificateRow(
                        certificate = cert,
                        onClick = { onViewCertificate(cert.id) },
                        onCancel = {
                            cancelTargetId = cert.id
                            cancelReason = ""
                            cancelDialogOpen = true
                        },
                        onNewVersion = { onCreateVersion(cert.id) },
                    )
                }
            }
        }

        if (cancelDialogOpen && cancelTargetId > 0) {
            CancelDialog(
                isOpen = cancelDialogOpen,
                onDismiss = { cancelDialogOpen = false },
                onConfirm = { reason ->
                    onCancelCertificate(cancelTargetId, reason)
                    cancelDialogOpen = false
                    cancelTargetId = -1
                },
                onReasonChange = { cancelReason = it },
                reason = cancelReason,
            )
        }

        OutlinedButton(onClick = onClose) {
            Text(stringResource(R.string.documents_back))
        }
    }
}

@Composable
private fun CancelDialog(
    isOpen: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
    onReasonChange: (String) -> Unit,
    reason: String,
) {
    if (!isOpen) return
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
    ) {
        Text(
            text = stringResource(R.string.certificates_cancel_title),
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = stringResource(R.string.certificates_cancel_reason),
            style = MaterialTheme.typography.bodyMedium,
        )
        OutlinedTextField(
            value = reason,
            onValueChange = onReasonChange,
            label = { Text(stringResource(R.string.certificates_cancel_reason_field)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.certificates_cancel_dismiss))
            }
            Button(onClick = { onConfirm(reason) }) {
                Text(stringResource(R.string.certificates_cancel_confirm))
            }
        }
    }
}

@Composable
private fun CertificateRow(
    certificate: br.com.odontolivreval.core.demo.MedicalCertificateFile,
    onClick: () -> Unit,
    onCancel: () -> Unit,
    onNewVersion: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = certificate.title,
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = "${stringResource(R.string.certificates_type, certificate.type.name)} · ${stringResource(R.string.certificates_status, certificate.status.name)}",
                style = MaterialTheme.typography.bodySmall,
            )
        }
        if (certificate.status == CertificateStatus.DRAFT) {
            TextButton(onClick = onClick) {
                Text(stringResource(R.string.certificates_edit))
            }
            TextButton(onClick = onNewVersion) {
                Text(stringResource(R.string.certificates_new_version))
            }
        } else {
            TextButton(onClick = onClick) {
                Text(stringResource(R.string.certificates_view))
            }
        }
        if (certificate.status == CertificateStatus.FINALIZED) {
            TextButton(onClick = onCancel) {
                Text(stringResource(R.string.certificates_cancel))
            }
        }
    }
}
