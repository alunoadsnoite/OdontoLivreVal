package br.com.odontolivreval.feature.settings

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import br.com.odontolivreval.core.demo.ClinicStore
import br.com.odontolivreval.core.model.ThemeMode

@Composable
fun SettingsScreen(store: ClinicStore, state: SettingsState, onTheme: (ThemeMode) -> Unit, onAbout: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.appearance), style = MaterialTheme.typography.headlineSmall)
        Column(Modifier.selectableGroup()) {
            ThemeMode.entries.forEach { mode ->
                val label = stringResource(when(mode) {
                    ThemeMode.SYSTEM -> R.string.system
                    ThemeMode.LIGHT -> R.string.light
                    ThemeMode.DARK -> R.string.dark
                })
                Row(Modifier.fillMaxWidth().selectable(state.theme == mode, role = Role.RadioButton, onClick = { onTheme(mode) }).padding(12.dp)) {
                    RadioButton(selected = state.theme == mode, onClick = null)
                    Spacer(Modifier.width(12.dp))
                    Text(label)
                }
            }
        }
        if (state.writeFailed) Text(stringResource(R.string.save_failed), color = MaterialTheme.colorScheme.error)
        OutlinedButton(onClick = onAbout) { Text(stringResource(R.string.about)) }
        Text(stringResource(R.string.contact_title), style = MaterialTheme.typography.titleMedium)
        val context = LocalContext.current
        val contactEmail = stringResource(R.string.contact_email)
        val sendEmail = {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = "mailto:$contactEmail".toUri()
            }
            runCatching { context.startActivity(intent) }
            Unit
        }
        Text(
            text = stringResource(R.string.contact_developer),
            modifier = Modifier.clickable(onClick = sendEmail),
        )
        OutlinedButton(onClick = sendEmail) { Text(stringResource(R.string.contact_send_email)) }
    }
}
