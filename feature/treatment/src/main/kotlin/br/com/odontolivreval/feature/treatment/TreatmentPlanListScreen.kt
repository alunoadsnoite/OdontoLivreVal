package br.com.odontolivreval.feature.treatment

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.odontolivreval.core.demo.DemoPatient
import br.com.odontolivreval.core.demo.ClinicStore

@Composable
fun TreatmentPlanListScreen(
    store: ClinicStore,
    patient: DemoPatient,
    onClose: () -> Unit,
    onCreatePlan: () -> Unit,
    onOpenPlan: (Long) -> Unit,
) {
    val model: TreatmentPlanViewModel = viewModel(factory = TreatmentPlanViewModel.factory(store, patient.id))
    val state by model.listState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(R.string.treatment_plans_title),
            style = MaterialTheme.typography.headlineLarge,
        )
        Text(
            text = stringResource(R.string.treatment_plans_patient, patient.name),
            style = MaterialTheme.typography.titleMedium,
        )

        Button(
            onClick = onCreatePlan,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.treatment_plans_create))
        }

        when {
            state.loading -> {
                Text(text = stringResource(R.string.treatment_plans_loading))
            }
            state.error != null -> {
                Text(
                    text = stringResource(R.string.treatment_plans_error),
                    color = MaterialTheme.colorScheme.error,
                )
                OutlinedButton(onClick = model::loadPlans) {
                    Text(stringResource(R.string.treatment_plans_retry))
                }
            }
            state.plans.isEmpty() -> {
                Text(text = stringResource(R.string.treatment_plans_empty), style = MaterialTheme.typography.bodyMedium)
            }
            else -> {
                Text(
                    text = stringResource(R.string.treatment_plans_history),
                    style = MaterialTheme.typography.titleMedium,
                )
                state.plans.forEach { plan ->
                    PlanRow(
                        plan = plan,
                        onClick = { onOpenPlan(plan.id) },
                    )
                }
            }
        }

        OutlinedButton(onClick = onClose) {
            Text(stringResource(R.string.treatment_plans_back))
        }
    }
}

@Composable
private fun PlanRow(
    plan: br.com.odontolivreval.core.demo.TreatmentPlanFile,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = plan.title,
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = "${pluralStringResource(R.plurals.treatment_plans_sessions, plan.estimatedSessions, plan.estimatedSessions)} · ${plan.status}",
                style = MaterialTheme.typography.bodySmall,
            )
        }
        FilledTonalButton(onClick = onClick) {
            Text(stringResource(R.string.treatment_plans_open))
        }
    }
}
