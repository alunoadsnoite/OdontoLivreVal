package br.com.odontolivreval.feature.treatment

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import br.com.odontolivreval.core.demo.BudgetStatus
import br.com.odontolivreval.core.demo.BudgetItem
import br.com.odontolivreval.core.demo.ClinicStorageException
import br.com.odontolivreval.core.demo.ClinicStore
import br.com.odontolivreval.core.demo.TreatmentBudget
import br.com.odontolivreval.core.demo.TreatmentBudgetFile
import java.io.IOException
import java.math.BigDecimal
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal enum class BudgetError { LOAD, SAVE, CANCEL, REVISION, NOT_FOUND, INCOMPLETE }

internal data class BudgetListState(
    val budgets: List<TreatmentBudgetFile> = emptyList(),
    val loading: Boolean = true,
    val error: BudgetError? = null,
)

internal data class BudgetFormState(
    val budget: TreatmentBudget? = null,
    val loading: Boolean = false,
    val saving: Boolean = false,
    val saved: Boolean = false,
    val error: BudgetError? = null,
)

internal class TreatmentBudgetViewModel(private val store: ClinicStore, private val patientId: Long) : ViewModel() {

    // ---------- Lista de orçamentos ----------

    private val listMutable = MutableStateFlow(BudgetListState())
    val listState = listMutable.asStateFlow()

    fun loadBudgets() {
        listMutable.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            try {
                val budgets = store.listTreatmentBudgets(patientId)
                listMutable.update { it.copy(budgets = budgets, loading = false) }
            } catch (_: ClinicStorageException) {
                listMutable.update { it.copy(loading = false, error = BudgetError.LOAD) }
            }
        }
    }

    fun cancelBudget(id: Long, reason: String) {
        if (reason.isBlank()) {
            listMutable.update { it.copy(error = BudgetError.INCOMPLETE) }
            return
        }
        viewModelScope.launch {
            try {
                store.cancelTreatmentBudget(patientId, id, reason)
                loadBudgets()
            } catch (_: ClinicStorageException) {
                listMutable.update { it.copy(error = BudgetError.CANCEL) }
            }
        }
    }

    fun deleteBudget(id: Long) {
        viewModelScope.launch {
            try {
                store.deleteTreatmentBudget(patientId, id)
                loadBudgets()
            } catch (_: ClinicStorageException) {
                listMutable.update { it.copy(error = BudgetError.CANCEL) }
            }
        }
    }

    fun createRevision(id: Long) {
        viewModelScope.launch {
            try {
                val newBudget = store.createTreatmentBudgetRevision(patientId, id)
                listMutable.update { it.copy(error = null) }
                // Carregar o novo orçamento no formulário
                loadBudget(newBudget.id)
            } catch (_: ClinicStorageException) {
                listMutable.update { it.copy(error = BudgetError.REVISION) }
            }
        }
    }

    // ---------- Formulário de orçamento ----------

    private val formMutable = MutableStateFlow(BudgetFormState())
    val formState = formMutable.asStateFlow()

    fun loadBudget(budgetId: Long) {
        formMutable.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            try {
                val budget = store.getTreatmentBudget(patientId, budgetId)
                    ?: throw IOException("Budget not found")
                formMutable.update {
                    it.copy(budget = budget, loading = false, saving = false, saved = false, error = null)
                }
            } catch (_: ClinicStorageException) {
                formMutable.update { it.copy(loading = false, error = BudgetError.LOAD) }
            } catch (_: IOException) {
                formMutable.update { it.copy(loading = false, error = BudgetError.NOT_FOUND) }
            }
        }
    }

    fun updateBudgetNumber(value: String) {
        formMutable.update { it.copy(
            budget = it.budget?.copy(budgetNumber = value),
            error = null,
        )}
    }

    fun updatePaymentConditions(value: String) {
        formMutable.update { it.copy(
            budget = it.budget?.copy(paymentConditions = value),
            error = null,
        )}
    }

    fun updateValidityDays(value: Int) {
        formMutable.update { it.copy(
            budget = it.budget?.copy(validityDays = value),
            error = null,
        )}
    }

    fun updateObservations(value: String) {
        formMutable.update { it.copy(
            budget = it.budget?.copy(observations = value),
            error = null,
        )}
    }

    fun updateDiscount(value: BigDecimal) {
        formMutable.update { it.copy(
            budget = it.budget?.copy(discount = value),
            error = null,
        )}
    }

    fun updateItems(items: List<br.com.odontolivreval.core.demo.BudgetItem>) {
        formMutable.update { it.copy(
            budget = it.budget?.copy(items = items),
            error = null,
        )}
    }

    fun approveBudget(approvalObservations: String? = null) {
        val current = formState.value.budget ?: return
        if (current.status != BudgetStatus.DRAFT && current.status != BudgetStatus.PRESENTED) {
            formMutable.update { it.copy(error = BudgetError.INCOMPLETE) }
            return
        }
        formMutable.update { it.copy(saving = true, error = null) }

        viewModelScope.launch {
            try {
                val approved = current.copy(
                    status = BudgetStatus.APPROVED,
                    approvedAt = LocalDate.now(),
                    approvalObservations = approvalObservations,
                )
                val saved = store.saveTreatmentBudget(patientId, approved)
                formMutable.update {
                    it.copy(
                        budget = saved,
                        saving = false,
                        saved = true,
                        error = null,
                    )
                }
            } catch (_: ClinicStorageException) {
                formMutable.update { it.copy(saving = false, error = BudgetError.SAVE) }
            }
        }
    }

    fun saveBudget() {
        val current = formState.value.budget ?: return
        if (current.budgetNumber.isBlank()) {
            formMutable.update { it.copy(error = BudgetError.INCOMPLETE) }
            return
        }
        if (current.items.isEmpty()) {
            formMutable.update { it.copy(error = BudgetError.INCOMPLETE) }
            return
        }
        formMutable.update { it.copy(saving = true, error = null, saved = false) }

        viewModelScope.launch {
            try {
                val saved = store.saveTreatmentBudget(patientId, current)
                formMutable.update {
                    it.copy(
                        budget = saved,
                        saving = false,
                        saved = true,
                        error = null,
                    )
                }
            } catch (_: ClinicStorageException) {
                formMutable.update { it.copy(saving = false, error = BudgetError.SAVE) }
            }
        }
    }

    fun presentBudget() {
        val current = formState.value.budget ?: return
        if (current.status != BudgetStatus.DRAFT) {
            formMutable.update { it.copy(error = BudgetError.INCOMPLETE) }
            return
        }
        formMutable.update { it.copy(saving = true, error = null) }

        viewModelScope.launch {
            try {
                val presented = current.copy(status = BudgetStatus.PRESENTED)
                val saved = store.saveTreatmentBudget(patientId, presented)
                formMutable.update {
                    it.copy(
                        budget = saved,
                        saving = false,
                        saved = true,
                        error = null,
                    )
                }
            } catch (_: ClinicStorageException) {
                formMutable.update { it.copy(saving = false, error = BudgetError.SAVE) }
            }
        }
    }

    fun clearForm() {
        formMutable.update { BudgetFormState() }
    }

    companion object {
        fun factory(store: ClinicStore, patientId: Long) = viewModelFactory {
            initializer { TreatmentBudgetViewModel(store, patientId) }
        }
    }
}
