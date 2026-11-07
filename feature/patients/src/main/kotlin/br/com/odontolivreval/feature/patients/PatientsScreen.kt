package br.com.odontolivreval.feature.patients

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.odontolivreval.core.demo.DemoClinicStore
import br.com.odontolivreval.core.demo.DemoPatient

@Composable
fun PatientsScreen(
    store: DemoClinicStore,
    onPatientClick: (DemoPatient) -> Unit = { },
) {
    val model: PatientsViewModel = viewModel(factory = PatientsViewModel.factory(store))
    val state by model.state.collectAsStateWithLifecycle()
    var search by rememberSaveable { mutableStateOf("") }
    val query = search.patientSearchKey()
    val matches = state.patients.filter { it.name.patientSearchKey().contains(query) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(stringResource(R.string.demo_patients_title), style = MaterialTheme.typography.headlineLarge)
        }
        item {
            Text(stringResource(R.string.demo_patients_notice))
        }
        when {
            state.loading -> item {
                Text(stringResource(R.string.demo_patients_loading))
            }
            state.loadFailed -> item {
                Text(stringResource(R.string.demo_patients_load_failed), color = MaterialTheme.colorScheme.error)
                TextButton(onClick = model::retry) {
                    Text(stringResource(R.string.demo_patients_retry))
                }
            }
            else -> {
                item {
                    Button(onClick = { model.openForm() }) {
                        Text(stringResource(R.string.demo_patients_new))
                    }
                }
                item {
                    OutlinedTextField(
                        value = search,
                        onValueChange = { search = it.take(MAX_NAME_LENGTH) },
                        label = { Text(stringResource(R.string.demo_patients_search)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                if (matches.isEmpty()) {
                    item {
                        val message = if (state.patients.isEmpty()) {
                            R.string.demo_patients_empty
                        } else {
                            R.string.demo_patients_no_results
                        }
                        Text(stringResource(message))
                    }
                }
                items(matches, key = { it.id }) { patient ->
                    Column(Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = patient.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.clickable(onClick = { onPatientClick(patient) }),
                                )
                                OutlinedButton(
                                    onClick = { model.openForm(patient) },
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Text(stringResource(R.string.demo_patients_edit_named, patient.name))
                                }
                            }
                            OutlinedButton(
                                onClick = { model.deletePatient(patient.id) },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(stringResource(R.string.demo_patients_delete))
                            }
                        }
                    }
                }
            }
        }
    }

    state.form?.let { form ->
        AlertDialog(
            onDismissRequest = model::closeForm,
            title = {
                val title = if (form.id == null) R.string.demo_patients_new else R.string.demo_patients_edit
                Text(stringResource(title))
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = form.name,
                        onValueChange = model::changeName,
                        label = { Text(stringResource(R.string.demo_patients_name)) },
                        singleLine = true,
                        enabled = !form.saving,
                    )
                    if (form.saveFailed) {
                        Text(stringResource(R.string.demo_patients_save_failed), color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = model::save,
                    enabled = !form.saving && form.name.isNotBlank(),
                ) {
                    Text(stringResource(R.string.demo_patients_save))
                }
            },
            dismissButton = {
                TextButton(onClick = model::closeForm, enabled = !form.saving) {
                    Text(stringResource(R.string.demo_patients_cancel))
                }
            },
        )
    }

    state.deleteTarget?.let { id ->
        AlertDialog(
            onDismissRequest = { model.cancelDelete() },
            title = { Text(stringResource(R.string.demo_patients_delete_title)) },
            text = { Text(stringResource(R.string.demo_patients_delete_confirm)) },
            confirmButton = {
                TextButton(onClick = { model.confirmDelete() }) {
                    Text(stringResource(R.string.demo_patients_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { model.cancelDelete() }) {
                    Text(stringResource(R.string.demo_patients_cancel))
                }
            },
        )
    }

    if (state.deleteFailed) {
        Text(stringResource(R.string.demo_patients_save_failed), color = MaterialTheme.colorScheme.error)
    }
}
