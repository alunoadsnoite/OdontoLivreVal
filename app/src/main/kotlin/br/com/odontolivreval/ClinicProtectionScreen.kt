package br.com.odontolivreval

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

@Composable
internal fun ClinicProtectionScreen(
    lock: DeviceLock,
    backup: BackupState,
    onExport: () -> Unit,
    onRestore: () -> Unit,
    onPassword: (CharArray) -> Unit,
    onDismiss: () -> Unit,
) {
    Text(stringResource(R.string.security_title), style = MaterialTheme.typography.headlineSmall)
    Text(stringResource(R.string.security_storage))
    Text(stringResource(if (lock.enabled) R.string.security_enabled else R.string.security_disabled))
    if (lock.secure) {
        OutlinedButton(onClick = { lock.changeEnabled(!lock.enabled) }, enabled = !backup.busy) {
            Text(stringResource(if (lock.enabled) R.string.security_disable else R.string.security_enable))
        }
    } else {
        Text(stringResource(R.string.security_setup))
    }
    Text(stringResource(R.string.security_backup_info))
    Button(onClick = onExport, enabled = !backup.busy) { Text(stringResource(R.string.security_export)) }
    OutlinedButton(onClick = onRestore, enabled = !backup.busy) { Text(stringResource(R.string.security_restore)) }
    if (backup.request == null) backup.result?.let { Text(stringResource(it.message())) }
    backup.request?.let { request ->
        BackupPasswordDialog(request, backup.busy, backup.result, onPassword, onDismiss)
    }
}

@Composable
private fun BackupPasswordDialog(
    request: BackupRequest,
    busy: Boolean,
    result: BackupResult?,
    onPassword: (CharArray) -> Unit,
    onDismiss: () -> Unit,
) {
    var password by remember(request) { mutableStateOf("") }
    var confirmation by remember(request) { mutableStateOf("") }
    val exporting = request.action == BackupAction.EXPORT
    val valid = if (exporting) password.length >= 12 && password == confirmation else password.isNotEmpty()
    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        title = { Text(stringResource(if (exporting) R.string.security_export else R.string.security_restore)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(if (exporting) R.string.security_password_help else R.string.security_replace_warning))
                BackupPasswordField(password, { password = it }, R.string.security_password, !busy)
                if (exporting) {
                    BackupPasswordField(confirmation, { confirmation = it }, R.string.security_password_confirm, !busy)
                }
                if (busy) Text(stringResource(R.string.security_working))
                result?.let { Text(stringResource(it.message()), color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val secret = password.toCharArray()
                    password = ""
                    confirmation = ""
                    onPassword(secret)
                },
                enabled = valid && !busy,
            ) {
                Text(stringResource(if (exporting) R.string.security_export else R.string.security_replace))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !busy) { Text(stringResource(R.string.security_cancel)) }
        },
    )
}

@Composable
private fun BackupPasswordField(value: String, update: (String) -> Unit, label: Int, enabled: Boolean) {
    OutlinedTextField(
        value = value,
        onValueChange = { update(it.take(128)) },
        label = { Text(stringResource(label)) },
        singleLine = true,
        enabled = enabled,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        visualTransformation = PasswordVisualTransformation(),
        modifier = Modifier.fillMaxWidth(),
    )
}

private fun BackupResult.message(): Int = when (this) {
    BackupResult.EXPORTED -> R.string.security_exported
    BackupResult.RESTORED -> R.string.security_restored
    BackupResult.EXPORT_FAILED -> R.string.security_export_failed
    BackupResult.RESTORE_FAILED -> R.string.security_restore_failed
}
