package br.com.odontolivreval.feature.patients

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.odontolivreval.core.demo.ClinicStore
import br.com.odontolivreval.core.demo.DemoPatient
import br.com.odontolivreval.core.demo.clinicSearchKey

@Composable
fun PatientsScreen(
    store: ClinicStore,
    onPatientClick: (DemoPatient) -> Unit = { },
) {
    val model: PatientsViewModel = viewModel(factory = PatientsViewModel.factory(store))
    val state by model.state.collectAsStateWithLifecycle()
    var search by rememberSaveable { mutableStateOf("") }
    val query = search.clinicSearchKey()
    val matches = state.patients.filter { it.name.clinicSearchKey().contains(query) }

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
                if (state.deleteFailed) {
                    item {
                        Text(
                            stringResource(R.string.demo_patients_save_failed),
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
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
                    PatientCard(
                        patient = patient,
                        onOpen = { onPatientClick(patient) },
                        onEdit = { model.openForm(patient) },
                        onDelete = { model.requestDelete(patient.id) },
                    )
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
                        modifier = Modifier.fillMaxWidth(),
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

    if (state.deleteTarget != null) {
        AlertDialog(
            onDismissRequest = model::cancelDelete,
            title = { Text(stringResource(R.string.demo_patients_delete_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.demo_patients_delete_confirm))
                    if (state.deleteFailed) {
                        Text(
                            stringResource(R.string.demo_patients_save_failed),
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = model::confirmDelete, enabled = !state.deleting) {
                    Text(stringResource(R.string.demo_patients_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = model::cancelDelete, enabled = !state.deleting) {
                    Text(stringResource(R.string.demo_patients_cancel))
                }
            },
        )
    }
}

@Composable
private fun PatientCard(
    patient: DemoPatient,
    onOpen: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = patient.name,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        role = Role.Button,
                        onClickLabel = stringResource(R.string.demo_patient_open_record),
                        onClick = onOpen,
                    )
                    .padding(vertical = 8.dp),
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onEdit) {
                    Text(stringResource(R.string.demo_patients_edit_named, patient.name))
                }
                OutlinedButton(onClick = onDelete) {
                    Text(stringResource(R.string.demo_patients_delete))
                }
            }
        }
    }
}
