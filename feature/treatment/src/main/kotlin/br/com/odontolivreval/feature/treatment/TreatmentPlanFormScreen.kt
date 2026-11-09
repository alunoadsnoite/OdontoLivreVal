package br.com.odontolivreval.feature.treatment

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.material3.Button
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
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
fun TreatmentPlanFormScreen(
    store: ClinicStore,
    patient: DemoPatient,
    planId: Long?,
    onClose: () -> Unit,
    onSaveComplete: () -> Unit,
) {
    val model: TreatmentPlanViewModel = viewModel(factory = TreatmentPlanViewModel.factory(store, patient.id))
    val formState by model.formState.collectAsStateWithLifecycle()

    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var estimatedSessions by remember { mutableStateOf("1") }

    LaunchedEffect(planId, formState.plan) {
        if (planId != null) {
            model.loadPlan(planId)
            title = formState.plan?.title ?: ""
            description = formState.plan?.description ?: ""
            estimatedSessions = formState.plan?.estimatedSessions.toString() ?: "1"
        } else {
            model.clearForm()
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = if (planId == null) stringResource(R.string.treatment_plan_new_title) else stringResource(R.string.treatment_plan_edit_title),
            style = MaterialTheme.typography.headlineLarge,
        )
        Text(
            text = stringResource(R.string.treatment_plans_patient, patient.name),
            style = MaterialTheme.typography.titleMedium,
        )

        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text(stringResource(R.string.treatment_plan_title)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text(stringResource(R.string.treatment_plan_description)) },
            minLines = 3,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = estimatedSessions,
            onValueChange = { estimatedSessions = it },
            label = { Text(stringResource(R.string.treatment_plan_sessions)) },
            singleLine = true,
            modifier = Modifier.requiredWidth(120.dp),
        )

        formState.error?.let { error ->
            Text(
                text = when (error) {
                    TreatmentError.INCOMPLETE -> stringResource(R.string.treatment_plan_error_incomplete)
                    TreatmentError.SAVE -> stringResource(R.string.treatment_plan_error_save)
                    TreatmentError.NOT_FOUND -> stringResource(R.string.treatment_plan_error_not_found)
                    else -> stringResource(R.string.treatment_plan_error_unknown)
                },
                color = MaterialTheme.colorScheme.error,
            )
        }

        if (formState.saving) {
            Text(text = stringResource(R.string.treatment_plan_saving))
        }

        // Procedimentos
        if (formState.plan != null) {
            Text(
                text = stringResource(R.string.treatment_procedures_title),
                style = MaterialTheme.typography.titleMedium,
            )

            formState.procedures.forEachIndexed { index, proc ->
                ProcedureRow(
                    procedure = proc,
                    index = index,
                    onDelete = { model.deleteProcedure(proc.id) },
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedButton(
                enabled = !formState.saving,
                onClick = {
                    val sessions = estimatedSessions.toIntOrNull() ?: 1
                    model.savePlan(title, description, sessions)
                    if (model.formState.value.saved) onSaveComplete()
                },
            ) {
                Text(stringResource(R.string.treatment_plan_save))
            }
            if (formState.plan != null) {
                Button(
                    enabled = !formState.saving && formState.saved,
                    onClick = { onSaveComplete() },
                ) {
                    Text(stringResource(R.string.treatment_plan_generate_budget))
                }
            }
        }

        OutlinedButton(onClick = onClose) {
            Text(stringResource(R.string.treatment_plans_back))
        }
    }
}

@Composable
private fun ProcedureRow(
    procedure: br.com.odontolivreval.core.demo.TreatmentProcedureFile,
    index: Int,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "${index + 1}. ${procedure.description}",
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = "${procedure.tooth ?: ""} ${procedure.surface ?: ""} · Qtd: ${procedure.quantity} · ${procedure.unitValue.toPlainString()} · ${procedure.status}",
                style = MaterialTheme.typography.bodySmall,
            )
        }
        TextButton(onClick = onDelete) {
            Text(stringResource(R.string.treatment_procedure_delete))
        }
    }
}
