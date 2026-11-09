package br.com.odontolivreval.feature.treatment

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
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
import br.com.odontolivreval.core.demo.BudgetItem
import br.com.odontolivreval.core.demo.BudgetStatus
import br.com.odontolivreval.core.demo.DemoPatient
import br.com.odontolivreval.core.demo.ClinicStore
import br.com.odontolivreval.feature.treatment.BudgetError
import java.math.BigDecimal

@Composable
fun TreatmentBudgetFormScreen(
    store: ClinicStore,
    patient: DemoPatient,
    planId: Long,
    budgetId: Long?,
    onClose: () -> Unit,
    onSaveComplete: () -> Unit,
) {
    val model: TreatmentBudgetViewModel = viewModel(factory = TreatmentBudgetViewModel.factory(store, patient.id))
    val formState by model.formState.collectAsStateWithLifecycle()

    var budgetNumber by remember { mutableStateOf("") }
    var paymentConditions by remember { mutableStateOf("") }
    var validityDays by remember { mutableStateOf("30") }
    var observations by remember { mutableStateOf("") }
    var discount by remember { mutableStateOf(BigDecimal.ZERO) }

    var items by remember { mutableStateOf(emptyList<BudgetItem>()) }

    LaunchedEffect(budgetId, formState.budget) {
        if (budgetId != null) {
            model.loadBudget(budgetId)
            val b = formState.budget
            if (b != null) {
                budgetNumber = b.budgetNumber
                paymentConditions = b.paymentConditions
                validityDays = b.validityDays.toString()
                observations = b.observations
                discount = b.discount
                items = b.items
            }
        }
    }

    fun totalValue(): BigDecimal {
        return items.sumOf { it.totalValue } - discount
    }

    Column(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = if (budgetId == null) stringResource(R.string.treatment_budget_new_title) else stringResource(R.string.treatment_budget_edit_title),
            style = MaterialTheme.typography.headlineLarge,
        )
        Text(
            text = stringResource(R.string.treatment_budgets_patient, patient.name),
            style = MaterialTheme.typography.titleMedium,
        )

        OutlinedTextField(
            value = budgetNumber,
            onValueChange = { budgetNumber = it; model.updateBudgetNumber(it) },
            label = { Text(stringResource(R.string.treatment_budget_number)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = paymentConditions,
            onValueChange = { paymentConditions = it; model.updatePaymentConditions(it) },
            label = { Text(stringResource(R.string.treatment_budget_payment_conditions)) },
            minLines = 2,
            modifier = Modifier.fillMaxWidth(),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = validityDays,
                onValueChange = { validityDays = it; model.updateValidityDays(it.toIntOrNull() ?: 30) },
                label = { Text(stringResource(R.string.treatment_budget_validity_days)) },
                singleLine = true,
                modifier = Modifier.requiredWidth(120.dp),
            )
            OutlinedTextField(
                value = discount.toPlainString(),
                onValueChange = {
                    val newValue = it.trim()
                    discount = if (newValue.isNotBlank()) BigDecimal(newValue) else BigDecimal.ZERO
                },
                label = { Text(stringResource(R.string.treatment_budget_discount)) },
                singleLine = true,
                modifier = Modifier.requiredWidth(120.dp),
            )
        }
        OutlinedTextField(
            value = observations,
            onValueChange = { observations = it; model.updateObservations(it) },
            label = { Text(stringResource(R.string.treatment_budget_observations)) },
            minLines = 3,
            modifier = Modifier.fillMaxWidth(),
        )

        Text(
            text = stringResource(R.string.treatment_budget_items),
            style = MaterialTheme.typography.titleMedium,
        )
        if (items.isEmpty()) {
            Text(stringResource(R.string.treatment_budget_items_empty), style = MaterialTheme.typography.bodySmall)
        } else {
            items.forEachIndexed { index, item ->
                BudgetItemRow(
                    item = item,
                    index = index,
                )
            }
        }

        formState.error?.let { error ->
            Text(
                text = when (error) {
                    BudgetError.INCOMPLETE -> stringResource(R.string.treatment_budget_error_incomplete)
                    BudgetError.SAVE -> stringResource(R.string.treatment_budget_error_save)
                    BudgetError.NOT_FOUND -> stringResource(R.string.treatment_budget_error_not_found)
                    else -> stringResource(R.string.treatment_budget_error_unknown)
                },
                color = MaterialTheme.colorScheme.error,
            )
        }

        if (formState.saving) {
            Text(text = stringResource(R.string.treatment_budget_saving))
        }

        Text(
            text = stringResource(R.string.treatment_budget_total, totalValue().toPlainString()),
            style = MaterialTheme.typography.titleLarge,
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedButton(
                enabled = !formState.saving,
                onClick = {
                    val itemsList = items
                    model.updateItems(itemsList)
                    if (budgetNumber.isBlank() || itemsList.isEmpty()) return@OutlinedButton
                    model.saveBudget()
                    if (formState.saved) onSaveComplete()
                },
            ) {
                Text(stringResource(R.string.treatment_budget_save))
            }
            if (formState.budget?.status == BudgetStatus.DRAFT) {
                Button(
                    enabled = !formState.saving && budgetNumber.isNotBlank() && items.isNotEmpty(),
                    onClick = {
                        val itemsList = items
                        model.updateItems(itemsList)
                        model.presentBudget()
                        if (formState.saved) onSaveComplete()
                    },
                ) {
                    Text(stringResource(R.string.treatment_budget_present))
                }
            }
            if (formState.budget?.status == BudgetStatus.PRESENTED) {
                Button(
                    enabled = !formState.saving,
                    onClick = { model.approveBudget() },
                ) {
                    Text(stringResource(R.string.treatment_budget_approve))
                }
            }
        }

        OutlinedButton(onClick = onClose) {
            Text(stringResource(R.string.treatment_plans_back))
        }
    }
}

@Composable
private fun BudgetItemRow(
    item: BudgetItem,
    index: Int,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "${index + 1}. ${item.description}",
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = "${item.tooth ?: ""} · Qtd: ${item.quantity} · ${item.unitValue.toPlainString()}",
                style = MaterialTheme.typography.bodySmall,
            )
        }
        Text(
            text = item.totalValue.toPlainString(),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.requiredWidth(80.dp),
        )
    }
}
