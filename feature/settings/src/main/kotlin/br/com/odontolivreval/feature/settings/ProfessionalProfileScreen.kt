package br.com.odontolivreval.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.odontolivreval.core.demo.ClinicStore
import br.com.odontolivreval.core.demo.ProfessionalProfile

@Composable
fun ProfessionalProfileScreen(store: ClinicStore, onClose: () -> Unit) {
    val model: ProfessionalProfileViewModel = viewModel(factory = ProfessionalProfileViewModel.factory(store))
    val state by model.state.collectAsStateWithLifecycle()

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.profile_title), style = MaterialTheme.typography.headlineLarge)
        Text(stringResource(R.string.profile_notice), style = MaterialTheme.typography.bodySmall)

        if (state.loading) {
            Text(stringResource(R.string.profile_loading))
            return@Column
        }

        if (state.error == ProfileError.LOAD) {
            Text(stringResource(R.string.profile_load_error), color = MaterialTheme.colorScheme.error)
            OutlinedButton(onClick = model::load) { Text(stringResource(R.string.profile_retry)) }
            return@Column
        }

        val form = state.profile
        ProfileField(
            value = form.name,
            onValueChange = { model.change(form.copy(name = it.take(ProfessionalProfile.MAX_NAME))) },
            label = stringResource(R.string.profile_name),
            enabled = !state.saving,
        )
        ProfileField(
            value = form.cro,
            onValueChange = { model.change(form.copy(cro = it.take(ProfessionalProfile.MAX_CRO))) },
            label = stringResource(R.string.profile_cro),
            enabled = !state.saving,
        )
        ProfileField(
            value = form.specialty,
            onValueChange = { model.change(form.copy(specialty = it.take(ProfessionalProfile.MAX_SPECIALTY))) },
            label = stringResource(R.string.profile_specialty),
            enabled = !state.saving,
        )
        ProfileField(
            value = form.clinicName,
            onValueChange = { model.change(form.copy(clinicName = it.take(ProfessionalProfile.MAX_CLINIC))) },
            label = stringResource(R.string.profile_clinic),
            enabled = !state.saving,
        )
        ProfileField(
            value = form.address,
            onValueChange = { model.change(form.copy(address = it.take(ProfessionalProfile.MAX_ADDRESS))) },
            label = stringResource(R.string.profile_address),
            enabled = !state.saving,
        )
        ProfileField(
            value = form.phone,
            onValueChange = { model.change(form.copy(phone = it.take(ProfessionalProfile.MAX_PHONE))) },
            label = stringResource(R.string.profile_phone),
            enabled = !state.saving,
            keyboardType = KeyboardType.Phone,
        )

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

        Button(onClick = model::save, enabled = !state.saving, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(if (state.saving) R.string.profile_saving else R.string.profile_save))
        }
        OutlinedButton(onClick = onClose, enabled = !state.saving) {
            Text(stringResource(R.string.profile_back))
        }
    }
}

@Composable
private fun ProfileField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    enabled: Boolean,
    keyboardType: KeyboardType = KeyboardType.Text,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        enabled = enabled,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        modifier = Modifier.fillMaxWidth(),
    )
}
