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
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
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

        val createLabel = stringResource(R.string.consents_create)
        Button(
            onClick = onCreateConsent,
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = createLabel },
        ) {
            Text(createLabel)
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
                val retryLabel = stringResource(R.string.consents_retry)
                OutlinedButton(
                    onClick = model::loadConsents,
                    modifier = Modifier.semantics { contentDescription = retryLabel },
                ) {
                    Text(retryLabel)
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

        val backLabel = stringResource(R.string.consents_back)
        OutlinedButton(
            onClick = onClose,
            modifier = Modifier.semantics { contentDescription = backLabel },
        ) {
            Text(backLabel)
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
            val dismissLabel = stringResource(R.string.consents_revoke_dismiss)
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.semantics { contentDescription = dismissLabel },
            ) {
                Text(dismissLabel)
            }
            val confirmLabel = stringResource(R.string.consents_revoke_confirm)
            Button(
                onClick = { onConfirm(reason) },
                modifier = Modifier.semantics { contentDescription = confirmLabel },
            ) {
                Text(confirmLabel)
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
                text = "${stringResource(R.string.consents_type, stringResource(typeLabel(consent.type)))} · ${stringResource(R.string.consents_status, stringResource(consent.status.labelResource))}",
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
