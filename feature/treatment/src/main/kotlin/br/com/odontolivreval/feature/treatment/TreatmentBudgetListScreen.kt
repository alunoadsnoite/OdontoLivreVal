package br.com.odontolivreval.feature.treatment

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.odontolivreval.core.demo.BudgetStatus
import br.com.odontolivreval.core.demo.DemoPatient
import br.com.odontolivreval.core.demo.ClinicStore

@Composable
fun TreatmentBudgetListScreen(
    store: ClinicStore,
    patient: DemoPatient,
    onClose: () -> Unit,
    onCreateBudget: (Long) -> Unit,
    onOpenBudget: (Long) -> Unit,
) {
    val model: TreatmentBudgetViewModel = viewModel(factory = TreatmentBudgetViewModel.factory(store, patient.id))
    val state by model.listState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(R.string.treatment_budgets_title),
            style = MaterialTheme.typography.headlineLarge,
        )
        Text(
            text = stringResource(R.string.treatment_budgets_patient, patient.name),
            style = MaterialTheme.typography.titleMedium,
        )

        when {
            state.loading -> {
                Text(text = stringResource(R.string.treatment_budgets_loading))
            }
            state.error != null -> {
                Text(
                    text = stringResource(R.string.treatment_budgets_error),
                    color = MaterialTheme.colorScheme.error,
                )
                OutlinedButton(onClick = model::loadBudgets) {
                    Text(stringResource(R.string.treatment_budgets_retry))
                }
            }
            state.budgets.isEmpty() -> {
                Text(text = stringResource(R.string.treatment_budgets_empty), style = MaterialTheme.typography.bodyMedium)
            }
            else -> {
                Text(
                    text = stringResource(R.string.treatment_budgets_history),
                    style = MaterialTheme.typography.titleMedium,
                )
                state.budgets.forEach { budget ->
                    BudgetRow(
                        budget = budget,
                        onClick = { onOpenBudget(budget.id) },
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
private fun BudgetRow(
    budget: br.com.odontolivreval.core.demo.TreatmentBudgetFile,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = budget.budgetNumber,
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = "${stringResource(R.string.treatment_budget_revision, budget.revision)} · ${budget.status.name}",
                style = MaterialTheme.typography.bodySmall,
            )
        }
        androidx.compose.material3.TextButton(onClick = onClick) {
            Text(stringResource(R.string.treatment_budget_view))
        }
    }
}
