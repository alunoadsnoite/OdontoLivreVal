package br.com.odontolivreval.feature.signature

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import br.com.odontolivreval.core.demo.ClinicStore
import br.com.odontolivreval.core.demo.DemoPatient
import java.io.IOException
import java.security.MessageDigest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val MAX_IMPORTED_PDF_BYTES = 10L * 1024 * 1024

@Composable
fun SignatureImportScreen(
    store: ClinicStore,
    patient: DemoPatient,
    onClose: () -> Unit,
    onImportCompleted: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var importing by remember { mutableStateOf(false) }
    var importSuccess by remember { mutableStateOf(false) }
    var importError by remember { mutableStateOf(false) }
    var importedHash by remember { mutableStateOf<String?>(null) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null && !importing) {
            importing = true
            importError = false
            importSuccess = false
            importedHash = null
            scope.launch {
                try {
                    importedHash = withContext(Dispatchers.IO) {
                        val hash = sha256(context, uri)
                        store.addPrescription(patient.id, uri, archivedDocument = true)
                        hash
                    }
                    importSuccess = true
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    importError = true
                } finally {
                    importing = false
                }
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(R.string.signature_import_title),
            style = MaterialTheme.typography.headlineLarge,
        )
        Text(
            text = stringResource(R.string.signature_options_patient, patient.name),
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = stringResource(R.string.signature_import_unverified),
            style = MaterialTheme.typography.bodySmall,
        )

        Button(
            enabled = !importing,
            onClick = { launcher.launch(arrayOf("application/pdf")) },
        ) {
            Text(stringResource(R.string.signature_integrity_import_pdf))
        }

        if (importing) Text(stringResource(R.string.signature_integrity_importing))
        if (importError) {
            Text(
                text = stringResource(R.string.signature_integrity_import_error),
                color = MaterialTheme.colorScheme.error,
            )
        }
        if (importSuccess) {
            Text(
                text = stringResource(R.string.signature_integrity_import_success),
                color = MaterialTheme.colorScheme.primary,
            )
            importedHash?.let { hash ->
                Text("SHA-256: $hash", style = MaterialTheme.typography.bodySmall)
            }
            OutlinedButton(onClick = onImportCompleted) {
                Text(stringResource(R.string.signature_import_done))
            }
        }

        OutlinedButton(onClick = onClose, enabled = !importing) {
            Text(stringResource(R.string.signature_integrity_back))
        }
    }
}

private fun sha256(context: Context, uri: Uri): String {
    val input = context.contentResolver.openInputStream(uri) ?: throw IOException("Unable to read selected PDF")
    val digest = MessageDigest.getInstance("SHA-256")
    val buffer = ByteArray(8 * 1024)
    var total = 0L
    input.use {
        while (true) {
            val count = it.read(buffer)
            if (count < 0) break
            total += count
            if (total > MAX_IMPORTED_PDF_BYTES) throw IOException("PDF is too large")
            digest.update(buffer, 0, count)
        }
    }
    if (total == 0L) throw IOException("PDF is empty")
    return digest.digest().joinToString("") { byte -> "%02x".format(byte) }
}
