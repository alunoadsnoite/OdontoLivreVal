package br.com.odontolivreval.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.odontolivreval.core.demo.ProfessionalProfile
import br.com.odontolivreval.core.demo.ClinicStore

@Composable
fun ProfessionalProfileScreen(store: ClinicStore, onClose: () -> Unit) {
    val model: ProfessionalProfileViewModel = viewModel(factory = ProfessionalProfileViewModel.factory(store))
    val state by model.state.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier.fillMaxWidth().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(R.string.profile_title),
            style = MaterialTheme.typography.headlineLarge,
        )
        Text(
            text = stringResource(R.string.profile_notice),
            style = MaterialTheme.typography.bodySmall,
        )

        if (state.loading) {
            Text(stringResource(R.string.profile_loading))
        } else if (state.error == ProfileError.LOAD) {
            Text(stringResource(R.string.profile_load_error), color = MaterialTheme.colorScheme.error)
            OutlinedButton(onClick = model::load) {
                Text(stringResource(R.string.profile_retry))
            }
        } else {
            val p = state.profile

            OutlinedTextField(
                value = p.name,
                onValueChange = { model.change(p.copy(name = it)) },
                label = { Text(stringResource(R.string.profile_name)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.saving,
            )

            OutlinedTextField(
                value = p.cro,
                onValueChange = { model.change(p.copy(cro = it)) },
                label = { Text(stringResource(R.string.profile_cro)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.saving,
            )

            OutlinedTextField(
                value = p.croUF,
                onValueChange = { model.change(p.copy(croUF = it.uppercase())) },
                label = { Text(stringResource(R.string.profile_cro_uf)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.saving,
            )

            OutlinedTextField(
                value = p.cpf,
                onValueChange = { model.change(p.copy(cpf = it)) },
                label = { Text(stringResource(R.string.profile_cpf)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.saving,
            )

            OutlinedTextField(
                value = p.specialty,
                onValueChange = { model.change(p.copy(specialty = it)) },
                label = { Text(stringResource(R.string.profile_specialty)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.saving,
            )

            OutlinedTextField(
                value = p.clinicName,
                onValueChange = { model.change(p.copy(clinicName = it)) },
                label = { Text(stringResource(R.string.profile_clinic)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.saving,
            )

            OutlinedTextField(
                value = p.address,
                onValueChange = { model.change(p.copy(address = it)) },
                label = { Text(stringResource(R.string.profile_address)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.saving,
            )

            OutlinedTextField(
                value = p.phone,
                onValueChange = { model.change(p.copy(phone = it)) },
                label = { Text(stringResource(R.string.profile_phone)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.saving,
            )

            OutlinedTextField(
                value = p.email,
                onValueChange = { model.change(p.copy(email = it)) },
                label = { Text(stringResource(R.string.profile_email)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.saving,
            )
        }

        when (state.error) {
            ProfileError.INCOMPLETE -> Text(
                stringResource(R.string.profile_incomplete),
                color = MaterialTheme.colorScheme.error,
            )
            ProfileError.SAVE -> Text(
                stringResource(R.string.profile_save_error),
                color = MaterialTheme.colorScheme.error,
            )
            else -> Unit
        }

        if (state.saved) {
            Text(stringResource(R.string.profile_saved), color = MaterialTheme.colorScheme.primary)
        }

        Button(
            onClick = model::save,
            enabled = !state.saving,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(if (state.saving) R.string.profile_saving else R.string.profile_save))
        }

        OutlinedButton(onClick = onClose, enabled = !state.saving) {
            Text(stringResource(R.string.profile_back))
        }
    }
}
