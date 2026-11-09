package br.com.odontolivreval.feature.patients

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.odontolivreval.core.demo.ClinicStore
import br.com.odontolivreval.core.demo.DemoPatient
import br.com.odontolivreval.core.demo.VisitRecord
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

private val visitFormatter = DateTimeFormatter.ofPattern("dd/MM/uuuu HH:mm")

private enum class PendingAction { BACK, NEW_VISIT }

@Composable
fun PatientRecordScreen(
    store: ClinicStore,
    patient: DemoPatient,
    radiographs: @Composable () -> Unit = {},
    prescriptions: @Composable () -> Unit = {},
    exportOdontogram: @Composable (Long?) -> Unit = {},
    documentPhotos: @Composable (Long) -> Unit = {},
    onDocuments: () -> Unit = {},
    onCertificates: () -> Unit = {},
    onClose: () -> Unit,
) {
    val model: PatientRecordViewModel = viewModel(factory = PatientRecordViewModel.factory(store, patient))
    val collected = model.state.collectAsStateWithLifecycle()
    val state = collected.value

    if (state.loading) {
        PatientRecordMessage(stringResource(R.string.demo_patients_loading))
        return
    }

    if (state.loadFailed) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(R.string.demo_patients_load_failed),
                color = MaterialTheme.colorScheme.error,
            )
            Button(onClick = model::retry) {
                Text(stringResource(R.string.demo_patients_retry))
            }
        }
        return
    }

    var pendingAction by remember { mutableStateOf<PendingAction?>(null) }
    val requestBack = {
        if (state.saving) Unit else if (state.dirty) pendingAction = PendingAction.BACK else onClose()
    }
    BackHandler(enabled = state.dirty || state.saving, onBack = requestBack)
    val requestNewVisit = {
        if (state.saving) Unit
        else if (state.dirty) pendingAction = PendingAction.NEW_VISIT
        else model.newVisit()
    }

    Column(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(R.string.demo_patient_record_title),
                style = MaterialTheme.typography.headlineLarge,
            )
            Text(text = patient.name, style = MaterialTheme.typography.titleLarge)
            Text(
                text = stringResource(R.string.demo_patient_record_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        OutlinedTextField(
            value = state.record.birthDate,
            onValueChange = model::changeBirthDate,
            label = { Text(stringResource(R.string.demo_patient_birth_date)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            isError = state.birthDateError,
            supportingText = if (state.birthDateError) {
                { Text(stringResource(R.string.demo_patient_invalid_date)) }
            } else {
                null
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = state.record.phone,
            onValueChange = model::changePhone,
            label = { Text(stringResource(R.string.demo_patient_phone)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            isError = state.phoneError,
            supportingText = if (state.phoneError) {
                { Text(stringResource(R.string.demo_patient_invalid_phone)) }
            } else {
                null
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = state.record.complaint,
            onValueChange = model::changeComplaint,
            label = { Text(stringResource(R.string.demo_patient_complaint)) },
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = state.record.history,
            onValueChange = model::changeHistory,
            label = { Text(stringResource(R.string.demo_patient_history)) },
            minLines = 2,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = state.record.observations,
            onValueChange = model::changeObservations,
            label = { Text(stringResource(R.string.demo_patient_observations)) },
            minLines = 2,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = state.diagnosis,
            onValueChange = model::changeDiagnosis,
            label = { Text(stringResource(R.string.demo_patient_diagnosis)) },
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = state.conduct,
            onValueChange = model::changeConduct,
            label = { Text(stringResource(R.string.demo_patient_conduct)) },
            modifier = Modifier.fillMaxWidth(),
        )

        PatientOdontogramScreen(state.odontogram, model::changeOdontogram)
        exportOdontogram(state.activeVisitId.takeUnless { state.dirty || state.saving })
        radiographs()
        prescriptions()
        documentPhotos(patient.id)
        OutlinedButton(onClick = onDocuments, enabled = !state.saving) {
            Text(stringResource(R.string.demo_patient_documents))
        }
        OutlinedButton(onClick = onCertificates, enabled = !state.saving) {
            Text(stringResource(R.string.demo_patient_certificates))
        }

        Text(
            text = stringResource(R.string.demo_patient_visits),
            style = MaterialTheme.typography.titleMedium,
        )
        if (state.visits.isEmpty()) {
            Text(
                text = stringResource(R.string.demo_patient_no_visits),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            state.visits.forEach { visit ->
                VisitCard(visit = visit, active = visit.id == state.activeVisitId)
            }
        }

        if (state.saved) {
            Text(stringResource(R.string.demo_patient_record_saved), color = MaterialTheme.colorScheme.primary)
        }
        if (state.saveFailed) {
            Text(stringResource(R.string.demo_patient_save_failed), color = MaterialTheme.colorScheme.error)
        }
        if (state.saving) {
            Text(stringResource(R.string.demo_patient_saving), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedButton(onClick = requestBack, enabled = !state.saving) {
                Text(stringResource(R.string.back))
            }
            OutlinedButton(onClick = requestNewVisit, enabled = !state.saving) {
                Text(stringResource(R.string.demo_patient_new_visit))
            }
            Button(onClick = model::saveRecord, enabled = !state.saving) {
                Text(stringResource(R.string.demo_patient_save_record))
            }
        }
    }

    pendingAction?.let { action ->
        AlertDialog(
            onDismissRequest = { pendingAction = null },
            title = { Text(stringResource(R.string.demo_patient_discard_title)) },
            text = { Text(stringResource(R.string.demo_patient_discard_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingAction = null
                        when (action) {
                            PendingAction.BACK -> onClose()
                            PendingAction.NEW_VISIT -> model.newVisit()
                        }
                    },
                ) {
                    Text(stringResource(R.string.demo_patient_discard))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingAction = null }) {
                    Text(stringResource(R.string.demo_patient_keep_editing))
                }
            },
        )
    }
}

@Composable
private fun PatientRecordMessage(message: String) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(message)
    }
}

@Composable
private fun VisitCard(visit: VisitRecord, active: Boolean) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            val date = LocalDate.ofEpochDay(visit.visitDay)
            val time = LocalTime.of(visit.visitMinute / 60, visit.visitMinute % 60)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = visitFormatter.format(date.atTime(time)),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (active) {
                    Text(
                        text = stringResource(R.string.demo_patient_active_visit),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            Text(
                text = visit.complaint.ifBlank { stringResource(R.string.demo_patient_no_complaint) },
                style = MaterialTheme.typography.bodyMedium,
            )
            if (visit.diagnosis.isNotBlank()) {
                Text(
                    text = "${stringResource(R.string.demo_patient_diagnosis)}: ${visit.diagnosis}",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            if (visit.conduct.isNotBlank()) {
                Text(
                    text = "${stringResource(R.string.demo_patient_conduct)}: ${visit.conduct}",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            if (visit.odontogram.isNotEmpty()) {
                val teethMarked = visit.odontogram.keys.map { it.substringBefore(':') }.toSet().size
                Text(
                    text = pluralStringResource(
                        R.plurals.demo_patient_odontogram_marks_plural,
                        teethMarked,
                        teethMarked,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
