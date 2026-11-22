package br.com.odontolivreval.feature.patients

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import br.com.odontolivreval.core.demo.ClinicStorageException
import br.com.odontolivreval.core.demo.ClinicStore
import br.com.odontolivreval.core.demo.Radiograph

@Composable
fun PatientRadiographs(
    store: ClinicStore,
    patientId: Long,
    importRevision: Int,
    importing: Boolean,
    importFailed: Boolean,
    onPick: () -> Unit,
    onCamera: () -> Unit,
) {
    var images by remember(patientId) { mutableStateOf<List<Radiograph>?>(null) }
    var viewing by remember(patientId) { mutableStateOf<Radiograph?>(null) }
    var error by remember(patientId) { mutableStateOf(false) }
    LaunchedEffect(store, patientId, importRevision) {
        try {
            images = store.listRadiographs(patientId)
            error = false
        } catch (_: ClinicStorageException) {
            error = true
        }
    }

    Text(stringResource(R.string.radiographs_title), style = MaterialTheme.typography.titleMedium)
    Text(stringResource(R.string.radiographs_hint), style = MaterialTheme.typography.bodySmall)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = onPick, enabled = !importing) {
            Text(stringResource(R.string.radiographs_import))
        }
        OutlinedButton(onClick = onCamera, enabled = !importing) {
            Text(stringResource(R.string.radiographs_camera))
        }
    }
    if (importing) Text(stringResource(R.string.radiographs_importing))
    if (importFailed) {
        Text(stringResource(R.string.radiographs_import_failed), color = MaterialTheme.colorScheme.error)
    }
    if (error) Text(stringResource(R.string.radiographs_error), color = MaterialTheme.colorScheme.error)
    if (images?.isEmpty() == true) Text(stringResource(R.string.radiographs_empty))
    images.orEmpty().forEachIndexed { index, image ->
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = { viewing = image }) {
                Text(stringResource(R.string.radiographs_view, index + 1))
            }
        }
    }
    viewing?.let { image -> RadiographPreview(store, image) { viewing = null } }
    if (images?.isNotEmpty() == true) {
        Text(stringResource(R.string.clinical_attachments_retained), style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun RadiographPreview(store: ClinicStore, image: Radiograph, onClose: () -> Unit) {
    var loaded by remember(image.id) { mutableStateOf(false) }
    val bitmap by produceState<ImageBitmap?>(null, image) {
        value = try {
            store.previewRadiograph(image.patientId, image.id)?.asImageBitmap()
        } catch (_: ClinicStorageException) {
            null
        }
        loaded = true
    }
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text(stringResource(R.string.radiographs_title)) },
        text = {
            val preview = bitmap
            if (preview != null) {
                Image(
                    preview, stringResource(R.string.radiographs_description),
                    Modifier.fillMaxWidth().heightIn(max = 420.dp),
                )
            } else {
                Text(stringResource(if (loaded) R.string.radiographs_error else R.string.demo_patients_loading))
            }
        },
        confirmButton = { TextButton(onClick = onClose) { Text(stringResource(R.string.back)) } },
    )
}
