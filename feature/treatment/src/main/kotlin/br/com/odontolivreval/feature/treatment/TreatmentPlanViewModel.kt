package br.com.odontolivreval.feature.treatment

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import br.com.odontolivreval.core.demo.ClinicStorageException
import br.com.odontolivreval.core.demo.ClinicStore
import br.com.odontolivreval.core.demo.TreatmentPlan
import br.com.odontolivreval.core.demo.TreatmentPlanFile
import br.com.odontolivreval.core.demo.TreatmentProcedure
import br.com.odontolivreval.core.demo.TreatmentProcedureFile
import java.io.IOException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal enum class TreatmentError { LOAD, SAVE, DELETE, NOT_FOUND, INCOMPLETE }

internal data class PlanListState(
    val plans: List<TreatmentPlanFile> = emptyList(),
    val loading: Boolean = true,
    val error: TreatmentError? = null,
)

internal data class PlanFormState(
    val plan: TreatmentPlan? = null,
    val procedures: List<TreatmentProcedureFile> = emptyList(),
    val loading: Boolean = false,
    val saving: Boolean = false,
    val saved: Boolean = false,
    val error: TreatmentError? = null,
)

internal class TreatmentPlanViewModel(private val store: ClinicStore, private val patientId: Long) : ViewModel() {

    // ---------- Lista de planos ----------

    private val listMutable = MutableStateFlow(PlanListState())
    val listState = listMutable.asStateFlow()

    fun loadPlans() {
        listMutable.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            try {
                val plans = store.listTreatmentPlans(patientId)
                listMutable.update { it.copy(plans = plans, loading = false) }
            } catch (_: ClinicStorageException) {
                listMutable.update { it.copy(loading = false, error = TreatmentError.LOAD) }
            }
        }
    }

    fun deletePlan(id: Long) {
        viewModelScope.launch {
            try {
                store.deleteTreatmentPlan(patientId, id)
                loadPlans()
            } catch (_: ClinicStorageException) {
                listMutable.update { it.copy(error = TreatmentError.DELETE) }
            }
        }
    }

    // ---------- Formulário de plano + procedimentos ----------

    private val formMutable = MutableStateFlow(PlanFormState())
    val formState = formMutable.asStateFlow()

    fun loadPlan(planId: Long) {
        formMutable.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            try {
                val plan = store.getTreatmentPlan(patientId, planId)
                    ?: throw IOException("Plan not found")
                val procedures = store.listTreatmentProcedures(planId)
                formMutable.update {
                    it.copy(
                        plan = plan,
                        procedures = procedures,
                        loading = false,
                        saving = false,
                        saved = false,
                        error = null,
                    )
                }
            } catch (_: ClinicStorageException) {
                formMutable.update { it.copy(loading = false, error = TreatmentError.LOAD) }
            } catch (_: IOException) {
                formMutable.update { it.copy(loading = false, error = TreatmentError.NOT_FOUND) }
            }
        }
    }

    fun savePlan(title: String, description: String, estimatedSessions: Int) {
        if (formState.value.saving) return
        if (title.isBlank()) {
            formMutable.update { it.copy(error = TreatmentError.INCOMPLETE) }
            return
        }
        formMutable.update { it.copy(saving = true, error = null, saved = false) }

        val currentPlan = formState.value.plan
        viewModelScope.launch {
            try {
                val plan = TreatmentPlan(
                    id = currentPlan?.id ?: 0,
                    patientId = patientId,
                    title = title,
                    description = description,
                    estimatedSessions = estimatedSessions,
                    status = "active",
                )
                val saved = store.saveTreatmentPlan(patientId, plan)
                val procedures = store.listTreatmentProcedures(saved.id)
                formMutable.update {
                    it.copy(
                        plan = saved,
                        procedures = procedures,
                        saving = false,
                        saved = true,
                        error = null,
                    )
                }
            } catch (_: ClinicStorageException) {
                formMutable.update { it.copy(saving = false, error = TreatmentError.SAVE) }
            }
        }
    }

    fun saveProcedure(procedure: TreatmentProcedure) {
        if (formState.value.saving) return
        if (procedure.description.isBlank()) {
            formMutable.update { it.copy(error = TreatmentError.INCOMPLETE) }
            return
        }
        val planId = formState.value.plan?.id ?: return
        formMutable.update { it.copy(saving = true, error = null) }

        viewModelScope.launch {
            try {
                val saved = store.saveTreatmentProcedure(planId, procedure)
                val procedures = store.listTreatmentProcedures(planId)
                formMutable.update {
                    it.copy(
                        procedures = procedures,
                        saving = false,
                        saved = true,
                        error = null,
                    )
                }
            } catch (_: ClinicStorageException) {
                formMutable.update { it.copy(saving = false, error = TreatmentError.SAVE) }
            }
        }
    }

    fun deleteProcedure(procedureId: Long) {
        val planId = formState.value.plan?.id ?: return
        viewModelScope.launch {
            try {
                store.deleteTreatmentProcedure(planId, procedureId)
                val procedures = store.listTreatmentProcedures(planId)
                formMutable.update { it.copy(procedures = procedures) }
            } catch (_: ClinicStorageException) {
                formMutable.update { it.copy(error = TreatmentError.DELETE) }
            }
        }
    }

    fun reorderProcedures(procedureIds: List<Long>) {
        val planId = formState.value.plan?.id ?: return
        viewModelScope.launch {
            try {
                store.reorderTreatmentProcedures(planId, procedureIds)
                val procedures = store.listTreatmentProcedures(planId)
                formMutable.update { it.copy(procedures = procedures) }
            } catch (_: ClinicStorageException) {
                formMutable.update { it.copy(error = TreatmentError.SAVE) }
            }
        }
    }

    fun clearForm() {
        formMutable.update { PlanFormState() }
    }

    companion object {
        fun factory(store: ClinicStore, patientId: Long) = viewModelFactory {
            initializer { TreatmentPlanViewModel(store, patientId) }
        }
    }
}
