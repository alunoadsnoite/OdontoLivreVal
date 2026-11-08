package br.com.odontolivreval.feature.patients

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import br.com.odontolivreval.core.demo.ClinicStorageException
import br.com.odontolivreval.core.demo.ClinicStore
import br.com.odontolivreval.core.demo.PrescriptionFile
import kotlinx.coroutines.launch

@Composable
fun PatientPrescriptions(
    store: ClinicStore,
    patientId: Long,
    revision: Int,
    busy: Boolean,
    transferFailed: Boolean,
    copied: Boolean,
    onImport: () -> Unit,
    onCopy: (PrescriptionFile) -> Unit,
) {
    var files by remember(patientId) { mutableStateOf<List<PrescriptionFile>?>(null) }
    var removing by remember(patientId) { mutableStateOf<PrescriptionFile?>(null) }
    var deleting by remember(patientId) { mutableStateOf(false) }
    var error by remember(patientId) { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(store, patientId, revision) {
        try {
            files = store.listPrescriptions(patientId)
            error = false
        } catch (_: ClinicStorageException) {
            error = true
        }
    }

    Text(stringResource(R.string.prescriptions_title), style = MaterialTheme.typography.titleMedium)
    Text(stringResource(R.string.prescriptions_hint), style = MaterialTheme.typography.bodySmall)
    OutlinedButton(onClick = onImport, enabled = !busy && !deleting) {
        Text(stringResource(R.string.prescriptions_import))
    }
    if (copied) Text(stringResource(R.string.prescriptions_copied))
    if (busy) Text(stringResource(R.string.prescriptions_working))
    if (transferFailed || error) {
        Text(stringResource(R.string.prescriptions_error), color = MaterialTheme.colorScheme.error)
    }
    if (files?.isEmpty() == true) Text(stringResource(R.string.prescriptions_empty))
    files.orEmpty().forEach { file ->
        Text(file.name, style = MaterialTheme.typography.bodyMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = { onCopy(file) }, enabled = !busy && !deleting) {
                Text(stringResource(R.string.prescriptions_copy))
            }
            TextButton(onClick = { removing = file }, enabled = !busy && !deleting) {
                Text(stringResource(R.string.prescriptions_remove))
            }
        }
    }
    removing?.let { file ->
        AlertDialog(
            onDismissRequest = { removing = null },
            title = { Text(stringResource(R.string.prescriptions_confirm_remove)) },
            text = { Text(file.name) },
            confirmButton = {
                TextButton(onClick = {
                    removing = null
                    deleting = true
                    scope.launch {
                        try {
                            store.removePrescription(patientId, file.id)
                            files = files.orEmpty().filterNot { it.id == file.id }
                            error = false
                        } catch (_: ClinicStorageException) {
                            error = true
                        } finally {
                            deleting = false
                        }
                    }
                }) { Text(stringResource(R.string.prescriptions_remove)) }
            },
            dismissButton = {
                TextButton(onClick = { removing = null }) { Text(stringResource(R.string.demo_patients_cancel)) }
            },
        )
    }
}
