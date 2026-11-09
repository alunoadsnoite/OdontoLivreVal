package br.com.odontolivreval.feature.signature

import android.app.Activity
import android.content.Intent
import android.net.Uri
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.odontolivreval.core.demo.DemoPatient
import br.com.odontolivreval.core.demo.ClinicStore

@Composable
fun SignatureImportScreen(
    store: ClinicStore,
    patient: DemoPatient,
    documentType: String,
    documentId: Long,
    documentVersion: Int,
    documentTitle: String,
    onClose: () -> Unit,
    onImportCompleted: () -> Unit,
) {
    val model: SignatureViewModel = viewModel(factory = SignatureViewModel.factory(store, patient.id))

    var importing by remember { mutableStateOf(false) }
    var importSuccess by remember { mutableStateOf(false) }
    var importError by remember { mutableStateOf(false) }
    var importedHash by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(R.string.signature_import_title),
            style = MaterialTheme.typography.headlineLarge,
        )
        Text(
            text = stringResource(R.string.signature_integrity_patient, patient.name),
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = stringResource(R.string.signature_integrity_document, documentType),
            style = MaterialTheme.typography.bodyMedium,
        )

        androidx.compose.material3.Text(
            text = stringResource(R.string.signature_integrity_import_pdf),
            style = MaterialTheme.typography.bodySmall,
        )

        Button(
            onClick = {
                // Abrir seletor de arquivo para importar PDF
                // Isso requer ActivityResultLauncher que seria configurado na Activity
                importing = true
                importError = false
                // Simular importação
                importing = false
                importSuccess = true
                importedHash = "abc123..."
            },
        ) {
            Text(stringResource(R.string.signature_integrity_import_pdf))
        }

        if (importing) {
            Text(text = stringResource(R.string.signature_integrity_importing))
        }

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
                Text(
                    text = "Hash do documento: $hash",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }

        OutlinedButton(onClick = onClose) {
            Text(stringResource(R.string.signature_integrity_back))
        }
    }
}
