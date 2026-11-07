package br.com.odontolivreval.feature.patients

import androidx.lifecycle.ViewModel
import br.com.odontolivreval.core.demo.ClinicStorageException
import br.com.odontolivreval.core.demo.DemoClinicStore
import br.com.odontolivreval.core.demo.DemoPatient
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.Normalizer

internal const val MAX_NAME_LENGTH = 100

internal data class PatientForm(
    val id: Long? = null,
    val name: String = "",
    val saving: Boolean = false,
    val saveFailed: Boolean = false,
)

internal data class PatientsState(
    val patients: List<DemoPatient> = emptyList(),
    val loading: Boolean = true,
    val loadFailed: Boolean = false,
    val form: PatientForm? = null,
    val deleteTarget: Long? = null,
    val deleteFailed: Boolean = false,
)

internal class PatientsViewModel(private val store: DemoClinicStore) : ViewModel() {
    private val mutableState = MutableStateFlow(PatientsState())
    val state = mutableState.asStateFlow()

    init {
        load()
    }

    fun retry() {
        if (state.value.loadFailed) load()
    }

    private fun load() {
        mutableState.update { it.copy(loading = true, loadFailed = false) }
        viewModelScope.launch {
            try {
                val patients = store.listPatients().sortedByName()
                mutableState.update { it.copy(patients = patients, loading = false) }
            } catch (_: ClinicStorageException) {
                mutableState.update { it.copy(loading = false, loadFailed = true) }
            }
        }
    }

    fun openForm(patient: DemoPatient? = null) {
        if (state.value.form != null) return
        mutableState.update { it.copy(form = PatientForm(patient?.id, patient?.name.orEmpty())) }
    }

    fun changeName(name: String) {
        val form = state.value.form ?: return
        if (!form.saving && name.length <= MAX_NAME_LENGTH) {
            mutableState.update { it.copy(form = form.copy(name = name, saveFailed = false)) }
        }
    }

    fun closeForm() {
        if (state.value.form?.saving != true) mutableState.update { it.copy(form = null) }
    }

    fun save() {
        val form = state.value.form ?: return
        val name = form.name.trim()
        if (form.saving || name.isEmpty()) return
        mutableState.update { it.copy(form = form.copy(saving = true, saveFailed = false)) }
        viewModelScope.launch {
            try {
                val saved = store.savePatient(form.id, name)
                mutableState.update {
                    val patients = (it.patients.filterNot { patient -> patient.id == saved.id } + saved)
                        .sortedByName()
                    it.copy(patients = patients, form = null)
                }
            } catch (_: ClinicStorageException) {
                mutableState.update { it.copy(form = form.copy(saveFailed = true)) }
            }
        }
    }

    fun deletePatient(id: Long) {
        if (state.value.deleteTarget == null) {
            mutableState.update { it.copy(deleteTarget = id, deleteFailed = false) }
        } else {
            val target = state.value.deleteTarget ?: return
            performDelete(target)
        }
    }

    fun confirmDelete() {
        val target = state.value.deleteTarget ?: return
        performDelete(target)
    }

    private fun performDelete(id: Long) {
        viewModelScope.launch {
            try {
                store.deletePatient(id)
                mutableState.update { it.copy(deleteTarget = null, deleteFailed = false) }
                load()
            } catch (_: ClinicStorageException) {
                mutableState.update { it.copy(deleteFailed = true) }
            }
        }
    }

    fun cancelDelete() {
        mutableState.update { it.copy(deleteTarget = null, deleteFailed = false) }
    }

    companion object {
        fun factory(store: DemoClinicStore) = viewModelFactory {
            initializer { PatientsViewModel(store) }
        }
    }
}

private val combiningMarks = Regex("\\p{M}+")

internal fun String.patientSearchKey(): String =
    Normalizer.normalize(trim(), Normalizer.Form.NFD).replace(combiningMarks, "").lowercase()

private fun List<DemoPatient>.sortedByName() = sortedBy { it.name.patientSearchKey() }
