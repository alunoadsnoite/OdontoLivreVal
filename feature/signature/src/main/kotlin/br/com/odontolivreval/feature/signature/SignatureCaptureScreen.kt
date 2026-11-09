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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.odontolivreval.core.demo.DemoPatient
import br.com.odontolivreval.core.demo.ClinicStore
import br.com.odontolivreval.core.demo.SignatureModality

@Composable
fun SignatureCaptureScreen(
    store: ClinicStore,
    patient: DemoPatient,
    documentType: String,
    documentId: Long,
    documentVersion: Int,
    documentTitle: String,
    initialModality: SignatureModality = SignatureModality.ELECTRONIC_ACKNOWLEDGEMENT,
    onClose: () -> Unit,
    onSignatureCreated: (String) -> Unit,
) {
    val model: SignatureViewModel = viewModel(factory = SignatureViewModel.factory(store, patient.id))
    val formState by model.formState.collectAsStateWithLifecycle()

    var signatureText by remember { mutableStateOf("") }
    var confirmed by remember { mutableStateOf(false) }
    LaunchedEffect(patient.id, documentType, documentId, documentVersion, initialModality) {
        model.initForm(documentType, documentId, documentVersion)
        model.updateModality(initialModality)
    }
    LaunchedEffect(formState.saved) {
        if (formState.saved) onSignatureCreated(signatureText)
    }

    Column(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(R.string.signature_capture_title),
            style = MaterialTheme.typography.headlineLarge,
        )
        Text(
            text = stringResource(R.string.signature_capture_patient, patient.name),
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = stringResource(R.string.signature_capture_document, documentTitle),
            style = MaterialTheme.typography.bodyMedium,
        )

        // Seletor de modalidade
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            androidx.compose.material3.RadioButton(
                selected = formState.modality == SignatureModality.ELECTRONIC_ACKNOWLEDGEMENT,
                onClick = { model.updateModality(SignatureModality.ELECTRONIC_ACKNOWLEDGEMENT) },
            )
            Text(stringResource(R.string.signature_capture_modality_digital))

            androidx.compose.material3.RadioButton(
                selected = formState.modality == SignatureModality.HANDWRITTEN_ON_PAPER,
                onClick = { model.updateModality(SignatureModality.HANDWRITTEN_ON_PAPER) },
            )
            Text(stringResource(R.string.signature_capture_modality_paper))
        }

        // Registra a confirmação nominal; não representa desenho nem assinatura certificada.
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(R.string.signature_capture_draw_here),
                style = MaterialTheme.typography.labelMedium,
            )

            androidx.compose.material3.OutlinedTextField(
                value = signatureText,
                onValueChange = { signatureText = it },
                label = { Text(stringResource(R.string.signature_capture_signature_text)) },
                singleLine = false,
                minLines = 4,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        // Configurações do signatário
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(R.string.signature_capture_signer),
                style = MaterialTheme.typography.titleMedium,
            )

            androidx.compose.material3.OutlinedTextField(
                value = formState.signerName,
                onValueChange = { model.updateSignerName(it) },
                label = { Text(stringResource(R.string.signature_capture_signer_name)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            androidx.compose.material3.OutlinedTextField(
                value = formState.signerIdDocument,
                onValueChange = { model.updateSignerIdDocument(it) },
                label = { Text(stringResource(R.string.signature_capture_signer_id)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            androidx.compose.material3.OutlinedTextField(
                value = formState.signerRegistrationNumber,
                onValueChange = { model.updateSignerRegistrationNumber(it) },
                label = { Text(stringResource(R.string.signature_capture_signer_reg)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

        }

        formState.error?.let { error ->
            Text(
                text = when (error) {
                    SignatureError.INCOMPLETE -> stringResource(R.string.signature_capture_error_incomplete)
                    SignatureError.SAVE -> stringResource(R.string.signature_capture_error_save)
                    else -> stringResource(R.string.signature_capture_error_unknown)
                },
                color = MaterialTheme.colorScheme.error,
            )
        }

        if (formState.saving) {
            Text(text = stringResource(R.string.signature_capture_saving))
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        ) {
            androidx.compose.material3.Checkbox(
                checked = confirmed,
                onCheckedChange = { confirmed = it },
                enabled = !formState.saving && !formState.saved,
            )
            Text(stringResource(R.string.signature_capture_confirmation))
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedButton(
                enabled = !formState.saving && !formState.saved && signatureText.isNotBlank() && confirmed,
                onClick = {
                    model.updateNotes(signatureText.trim())
                    model.saveSignature()
                },
            ) {
                Text(stringResource(R.string.signature_capture_save))
            }
        }

        OutlinedButton(onClick = onClose) {
            Text(stringResource(R.string.signature_capture_back))
        }
    }
}
