package br.com.odontolivreval.feature.patients

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.text.keyboard.KeyboardOptions
import androidx.compose.foundation.text.keyboard.TextInputType
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.fillMaxSize
import br.com.odontolivreval.core.demo.DemoClinicStore
import br.com.odontolivreval.core.demo.DemoPatient
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.odontolivreval.core.demo.VisitRecord

@Composable
fun PatientRecordScreen(
    store: DemoClinicStore,
    patient: DemoPatient,
    onClose: () -> Unit,
) {
    val model: PatientRecordViewModel = viewModel(factory = PatientRecordViewModel.factory(store, patient))
    val state = model.state.collectAsStateWithLifecycle()

    if (state.value.loading) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(text = stringResource(R.string.demo_patients_loading))
        }
        return
    }

    if (state.value.loadFailed) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
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

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(R.string.demo_patient_record_title),
                style = MaterialTheme.typography.headlineLarge,
            )
            Text(
                text = patient.name,
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                text = stringResource(R.string.demo_patient_record_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        OutlinedTextField(
            value = state.value.record.birthDate,
            onValueChange = { model.changeBirthDate(it) },
            label = { Text(stringResource(R.string.demo_patient_birth_date)) },
            keyboardOptions = KeyboardOptions(keyboardType = TextInputType.Date),
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = state.value.record.phone,
            onValueChange = { model.changePhone(it) },
            label = { Text(stringResource(R.string.demo_patient_phone)) },
            keyboardOptions = KeyboardOptions(keyboardType = TextInputType.Phone),
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = state.value.record.complaint,
            onValueChange = { model.changeComplaint(it) },
            label = { Text(stringResource(R.string.demo_patient_complaint)) },
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = state.value.record.history,
            onValueChange = { model.changeHistory(it) },
            label = { Text(stringResource(R.string.demo_patient_history)) },
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = state.value.record.observations,
            onValueChange = { model.changeObservations(it) },
            label = { Text(stringResource(R.string.demo_patient_observations)) },
            modifier = Modifier.fillMaxWidth(),
        )

        PatientOdontogramScreen(state.value.visits.lastOrNull()?.odontogram ?: emptyMap(), model::changeOdontogram)

        Text(
            text = stringResource(R.string.demo_patient_visits),
            style = MaterialTheme.typography.titleMedium,
        )
        if (state.value.visits.isEmpty()) {
            Text(
                text = stringResource(R.string.demo_patient_no_visits),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            state.value.visits.forEach { visit ->
                VisitCard(visit = visit)
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Button(onClick = onClose) {
                Text(stringResource(R.string.demo_patients_cancel))
            }
            Button(
                onClick = { model.saveRecord() },
                modifier = Modifier.weight(1f),
            ) {
                Text(stringResource(R.string.demo_patient_save_record))
            }
            if (state.value.saveFailed) {
                Text(
                    text = stringResource(R.string.demo_patient_save_failed),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
private fun VisitCard(visit: VisitRecord) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            val date = java.time.LocalDate.ofEpochDay(visit.visitDay)
            val time = java.time.LocalTime.of(visit.visitMinute / 60, visit.visitMinute % 60)
            Text(
                text = "$date ${time.toString().take(5)}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = visit.complaint.take(60).ifBlank { "- Sem queixa -" },
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
        }
    }
}
