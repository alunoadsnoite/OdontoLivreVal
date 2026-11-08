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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import br.com.odontolivreval.core.demo.ClinicStorageException
import br.com.odontolivreval.core.demo.ClinicStore
import br.com.odontolivreval.core.demo.Radiograph
import kotlinx.coroutines.launch

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
    var removing by remember(patientId) { mutableStateOf<Radiograph?>(null) }
    var error by remember(patientId) { mutableStateOf(false) }
    var deleting by remember(patientId) { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
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
        OutlinedButton(onClick = onPick, enabled = !importing && !deleting) {
            Text(stringResource(R.string.radiographs_import))
        }
        OutlinedButton(onClick = onCamera, enabled = !importing && !deleting) {
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
            TextButton(onClick = { removing = image }, enabled = !importing && !deleting) {
                Text(stringResource(R.string.radiographs_remove))
            }
        }
    }
    viewing?.let { image -> RadiographPreview(store, image) { viewing = null } }
    removing?.let { image ->
        AlertDialog(
            onDismissRequest = { removing = null },
            title = { Text(stringResource(R.string.radiographs_confirm_remove)) },
            confirmButton = {
                TextButton(onClick = {
                    removing = null
                    deleting = true
                    scope.launch {
                        try {
                            store.removeRadiograph(patientId, image.id)
                            images = images.orEmpty().filterNot { it.id == image.id }
                            error = false
                        } catch (_: ClinicStorageException) {
                            error = true
                        } finally {
                            deleting = false
                        }
                    }
                }) { Text(stringResource(R.string.radiographs_remove)) }
            },
            dismissButton = {
                TextButton(onClick = { removing = null }) { Text(stringResource(R.string.demo_patients_cancel)) }
            },
        )
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
