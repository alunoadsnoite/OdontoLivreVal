package br.com.odontolivreval.feature.patients

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import br.com.odontolivreval.core.demo.ClinicStorageException
import br.com.odontolivreval.core.demo.ClinicStore
import br.com.odontolivreval.core.demo.PrescriptionFile

@Composable
fun PatientPrescriptions(
    store: ClinicStore,
    patientId: Long,
    revision: Int,
    busy: Boolean,
    archivedDocuments: Boolean = false,
    transferFailed: Boolean,
    copied: Boolean,
    onImport: () -> Unit,
    onCopy: (PrescriptionFile) -> Unit,
) {
    var files by remember(patientId) { mutableStateOf<List<PrescriptionFile>?>(null) }
    var error by remember(patientId) { mutableStateOf(false) }
    LaunchedEffect(store, patientId, archivedDocuments, revision) {
        try {
            files = store.listPrescriptions(patientId, archivedDocuments)
            error = false
        } catch (_: ClinicStorageException) {
            error = true
        }
    }

    Text(stringResource(if (archivedDocuments) R.string.archive_title else R.string.prescriptions_title), style = MaterialTheme.typography.titleMedium)
    Text(stringResource(if (archivedDocuments) R.string.archive_hint else R.string.prescriptions_hint), style = MaterialTheme.typography.bodySmall)
    OutlinedButton(onClick = onImport, enabled = !busy) {
        Text(stringResource(if (archivedDocuments) R.string.archive_import else R.string.prescriptions_import))
    }
    if (copied) Text(stringResource(if (archivedDocuments) R.string.archive_copied else R.string.prescriptions_copied))
    if (busy) Text(stringResource(if (archivedDocuments) R.string.archive_working else R.string.prescriptions_working))
    if (transferFailed || error) {
        Text(stringResource(if (archivedDocuments) R.string.archive_error else R.string.prescriptions_error), color = MaterialTheme.colorScheme.error)
    }
    if (files?.isEmpty() == true) Text(stringResource(if (archivedDocuments) R.string.archive_empty else R.string.prescriptions_empty))
    files.orEmpty().forEach { file ->
        Text(file.name, style = MaterialTheme.typography.bodyMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = { onCopy(file) }, enabled = !busy) {
                Text(stringResource(R.string.prescriptions_copy))
            }
        }
    }
    if (files?.isNotEmpty() == true) {
        Text(stringResource(R.string.clinical_attachments_retained), style = MaterialTheme.typography.bodySmall)
    }
}
