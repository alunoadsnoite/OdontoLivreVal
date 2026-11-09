package br.com.odontolivreval.feature.consent

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
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.odontolivreval.core.demo.ConsentStatus
import br.com.odontolivreval.core.demo.ConsentType
import br.com.odontolivreval.core.demo.DemoPatient
import br.com.odontolivreval.core.demo.ClinicStore

@Composable
fun ConsentListScreen(
    store: ClinicStore,
    patient: DemoPatient,
    onClose: () -> Unit,
    onCreateConsent: () -> Unit,
    onOpenConsent: (Long) -> Unit,
    onRevokeConsent: (Long, String) -> Unit,
    onCreateRevision: (Long) -> Unit,
) {
    val model: ConsentViewModel = viewModel(factory = ConsentViewModel.factory(store, patient.id))
    val state by model.listState.collectAsStateWithLifecycle()

    var revokeDialogOpen by remember { mutableStateOf(false) }
    var revokeTargetId by remember { mutableLongStateOf(-1L) }
    var revokeReason by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(R.string.consents_title),
            style = MaterialTheme.typography.headlineLarge,
        )
        Text(
            text = stringResource(R.string.consents_patient, patient.name),
            style = MaterialTheme.typography.titleMedium,
        )

        Button(
            onClick = onCreateConsent,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.consents_create))
        }

        when {
            state.loading -> {
                Text(text = stringResource(R.string.consents_loading))
            }
            state.error != null -> {
                Text(
                    text = stringResource(R.string.consents_error),
                    color = MaterialTheme.colorScheme.error,
                )
                OutlinedButton(onClick = model::loadConsents) {
                    Text(stringResource(R.string.consents_retry))
                }
            }
            state.consents.isEmpty() -> {
                Text(text = stringResource(R.string.consents_empty), style = MaterialTheme.typography.bodyMedium)
            }
            else -> {
                Text(
                    text = stringResource(R.string.consents_history),
                    style = MaterialTheme.typography.titleMedium,
                )
                state.consents.forEach { consent ->
                    ConsentRow(
                        consent = consent,
                        onClick = { onOpenConsent(consent.id) },
                        onRevoke = {
                            revokeTargetId = consent.id
                            revokeReason = ""
                            revokeDialogOpen = true
                        },
                        onNewRevision = { onCreateRevision(consent.id) },
                    )
                }
            }
        }

        if (revokeDialogOpen && revokeTargetId > 0) {
            RevokeDialog(
                isOpen = revokeDialogOpen,
                onDismiss = { revokeDialogOpen = false },
                onConfirm = { reason ->
                    model.revokeConsent(revokeTargetId, reason)
                    revokeDialogOpen = false
                    revokeTargetId = -1
                },
                onReasonChange = { revokeReason = it },
                reason = revokeReason,
            )
        }

        OutlinedButton(onClick = onClose) {
            Text(stringResource(R.string.consents_back))
        }
    }
}

@Composable
private fun RevokeDialog(
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
            text = stringResource(R.string.consents_revoke_title),
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = stringResource(R.string.consents_revoke_reason),
            style = MaterialTheme.typography.bodyMedium,
        )
        OutlinedTextField(
            value = reason,
            onValueChange = onReasonChange,
            label = { Text(stringResource(R.string.consents_revoke_reason_field)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.consents_revoke_dismiss))
            }
            Button(onClick = { onConfirm(reason) }) {
                Text(stringResource(R.string.consents_revoke_confirm))
            }
        }
    }
}

@Composable
private fun ConsentRow(
    consent: br.com.odontolivreval.core.demo.InformedConsentFile,
    onClick: () -> Unit,
    onRevoke: () -> Unit,
    onNewRevision: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = consent.title,
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = "${stringResource(R.string.consents_type, consent.type.name)} · ${stringResource(R.string.consents_status, consent.status.name)}",
                style = MaterialTheme.typography.bodySmall,
            )
        }
        TextButton(onClick = onClick) {
            Text(stringResource(R.string.consents_view))
        }
        if (consent.status == ConsentStatus.ACCEPTED || consent.status == ConsentStatus.REJECTED) {
            TextButton(onClick = onNewRevision) {
                Text(stringResource(R.string.consents_new_revision))
            }
        }
        if (consent.status == ConsentStatus.ACCEPTED) {
            TextButton(onClick = onRevoke) {
                Text(stringResource(R.string.consents_revoke))
            }
        }
    }
}
