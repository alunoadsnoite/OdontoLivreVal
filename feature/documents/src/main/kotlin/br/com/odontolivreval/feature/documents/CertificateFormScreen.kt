package br.com.odontolivreval.feature.documents

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.odontolivreval.core.demo.ClinicStore
import br.com.odontolivreval.core.demo.CertificateType
import br.com.odontolivreval.core.demo.DemoPatient
import br.com.odontolivreval.feature.documents.CertificateError

@Composable
fun CertificateFormScreen(
    store: ClinicStore,
    patient: DemoPatient,
    certificate: br.com.odontolivreval.core.demo.MedicalCertificate?,
    onClose: () -> Unit,
    onSave: () -> Unit,
    onFinalize: () -> Unit,
) {
    val model: CertificateViewModel = viewModel(factory = CertificateViewModel.factory(store, patient.id))
    val formState by model.formState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var isDraftOpen by remember { mutableStateOf(false) }

    LaunchedEffect(certificate) {
        if (certificate != null) {
            model.loadForm(certificate)
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = if (certificate == null) stringResource(R.string.certificates_create_title) else stringResource(R.string.certificates_edit_title),
            style = MaterialTheme.typography.headlineLarge,
        )
        Text(
            text = stringResource(R.string.certificates_patient, patient.name),
            style = MaterialTheme.typography.titleMedium,
        )

        Text(text = stringResource(R.string.certificates_type), style = MaterialTheme.typography.titleSmall)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            CertificateType.entries.forEach { type ->
                FilterChip(
                    selected = formState.type == type,
                    onClick = { model.setType(type) },
                    label = { Text(stringResource(typeLabel(type))) },
                )
            }
        }

        OutlinedTextField(
            value = formState.title,
            onValueChange = model::updateTitle,
            label = { Text(stringResource(R.string.certificates_title_field)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        OutlinedTextField(
            value = formState.content,
            onValueChange = model::updateContent,
            label = { Text(stringResource(R.string.certificates_content)) },
            minLines = 6,
            modifier = Modifier.fillMaxWidth(),
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = formState.serviceDate,
                onValueChange = model::updateServiceDate,
                label = { Text(stringResource(R.string.certificates_service_date)) },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            OutlinedTextField(
                value = formState.placeOfIssue,
                onValueChange = model::updatePlaceOfIssue,
                label = { Text(stringResource(R.string.certificates_place_of_issue)) },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
        }

        when (formState.type) {
            CertificateType.MEDICAL_CERTIFICATE -> {
                OutlinedTextField(
                    value = formState.leaveDays,
                    onValueChange = model::updateLeaveDays,
                    label = { Text(stringResource(R.string.certificates_leave_days)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    text = stringResource(R.string.certificates_leave_days_notice),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            CertificateType.ATTENDANCE_DECLARATION -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedTextField(
                        value = formState.entryTime,
                        onValueChange = model::updateEntryTime,
                        label = { Text(stringResource(R.string.certificates_entry_time)) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = formState.exitTime,
                        onValueChange = model::updateExitTime,
                        label = { Text(stringResource(R.string.certificates_exit_time)) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            CertificateType.COMPANION_DECLARATION -> {
                OutlinedTextField(
                    value = formState.companionName,
                    onValueChange = model::updateCompanionName,
                    label = { Text(stringResource(R.string.certificates_companion_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        ) {
            Checkbox(
                checked = formState.cidAuthorized,
                onCheckedChange = { model.toggleCidAuthorized() },
            )
            TextButton(onClick = { model.toggleCidAuthorized() }) {
                Text(stringResource(R.string.certificates_cid_authorize))
            }
        }
        if (formState.cidAuthorized) {
            OutlinedTextField(
                value = formState.cid,
                onValueChange = model::updateCid,
                label = { Text(stringResource(R.string.certificates_cid_field)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                text = stringResource(R.string.certificates_cid_notice),
                style = MaterialTheme.typography.bodySmall,
            )
        }

        formState.error?.let { error ->
            Text(
                text = when (error) {
                    CertificateError.INCOMPLETE -> stringResource(R.string.certificates_error_incomplete)
                    CertificateError.SAVE -> stringResource(R.string.certificates_error_save)
                    else -> stringResource(R.string.certificates_error_unknown)
                },
                color = MaterialTheme.colorScheme.error,
            )
        }

        if (formState.saving) {
            Text(text = stringResource(R.string.certificates_saving))
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedButton(
                enabled = !formState.saving,
                onClick = {
                    if (model.validateForm()) {
                        model.saveCertificate()
                        onSave()
                    }
                },
            ) {
                Text(stringResource(R.string.certificates_save_draft))
            }
            Button(
                enabled = !formState.saving && formState.saved,
                onClick = {
                    if (model.validateForm()) {
                        model.saveCertificate()
                        onFinalize()
                    }
                },
            ) {
                Text(stringResource(R.string.certificates_finalize))
            }
        }

        OutlinedButton(onClick = onClose) {
            Text(stringResource(R.string.documents_back))
        }
    }
}

private fun typeLabel(type: CertificateType): Int = when (type) {
    CertificateType.MEDICAL_CERTIFICATE -> R.string.certificates_type_medical
    CertificateType.ATTENDANCE_DECLARATION -> R.string.certificates_type_attendance
    CertificateType.COMPANION_DECLARATION -> R.string.certificates_type_companion
}
