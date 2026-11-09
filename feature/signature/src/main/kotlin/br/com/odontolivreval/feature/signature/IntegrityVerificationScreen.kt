package br.com.odontolivreval.feature.signature

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
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException

/**
 * Calcula o hash SHA-256 de uma string.
 */
private fun computeSha256(text: String): String? {
    return try {
        MessageDigest.getInstance("SHA-256").digest(text.toByteArray()).joinToString("") { "%02x".format(it) }
    } catch (_: NoSuchAlgorithmException) {
        null
    }
}

@Composable
fun IntegrityVerificationScreen(
    store: ClinicStore,
    patient: DemoPatient,
    documentType: String,
    documentId: Long,
    documentVersion: Int,
    documentContent: String,
    onClose: () -> Unit,
) {
    val model: SignatureViewModel = viewModel(factory = SignatureViewModel.factory(store, patient.id))
    val hashState by model.hashState.collectAsStateWithLifecycle()

    var documentContentToVerify by remember { mutableStateOf(documentContent) }

    Column(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(R.string.signature_integrity_title),
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

        // Conteúdo do documento para verificação
        androidx.compose.material3.OutlinedTextField(
            value = documentContentToVerify,
            onValueChange = { documentContentToVerify = it },
            label = { Text(stringResource(R.string.signature_integrity_document_content)) },
            minLines = 8,
            modifier = Modifier.fillMaxWidth(),
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Button(
                onClick = {
                    val hash = computeSha256(documentContentToVerify)
                    if (hash != null) {
                        model.verifyHash(documentType, documentId, documentVersion, hash)
                    }
                },
            ) {
                Text(stringResource(R.string.signature_integrity_verify))
            }
        }

        // Exibe o hash calculado localmente
        val currentHash = computeSha256(documentContentToVerify)
        if (currentHash != null) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "Hash SHA-256 calculado:",
                    style = MaterialTheme.typography.labelMedium,
                )
                Text(
                    text = currentHash,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        hashState.error?.let { error ->
            Text(
                text = when (error) {
                    SignatureError.HASH_ERROR -> stringResource(R.string.signature_integrity_error)
                    else -> stringResource(R.string.signature_integrity_error_unknown)
                },
                color = MaterialTheme.colorScheme.error,
            )
        }

        if (hashState.loading) {
            Text(text = stringResource(R.string.signature_integrity_verifying))
        } else if (hashState.integrityMatches) {
            Text(
                text = stringResource(R.string.signature_integrity_ok),
                color = MaterialTheme.colorScheme.primary,
            )
        } else if (hashState.storedHash != null) {
            Text(
                text = stringResource(R.string.signature_integrity_mismatch),
                color = MaterialTheme.colorScheme.error,
            )
        } else if (hashState.storedHash == null) {
            Text(
                text = stringResource(R.string.signature_integrity_no_hash),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        OutlinedButton(onClick = onClose) {
            Text(stringResource(R.string.signature_integrity_back))
        }
    }
}
