package br.com.odontolivreval.feature.patients

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import br.com.odontolivreval.core.demo.ClinicStorageException
import br.com.odontolivreval.core.demo.ClinicStore
import br.com.odontolivreval.core.demo.DemoPatient
import br.com.odontolivreval.core.demo.clinicSearchKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

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
    val deleting: Boolean = false,
    val deleteFailed: Boolean = false,
)

internal class PatientsViewModel(private val store: ClinicStore) : ViewModel() {
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

    /** Abre a confirmação de exclusão. A exclusão só ocorre em [confirmDelete]. */
    fun requestDelete(id: Long) {
        if (state.value.deleting || state.value.deleteTarget != null) return
        mutableState.update { it.copy(deleteTarget = id, deleteFailed = false) }
    }

    fun confirmDelete() {
        val target = state.value.deleteTarget ?: return
        if (state.value.deleting) return
        mutableState.update { it.copy(deleting = true, deleteFailed = false) }
        viewModelScope.launch {
            try {
                store.deletePatient(target)
                mutableState.update {
                    it.copy(deleteTarget = null, deleting = false, deleteFailed = false)
                }
                load()
            } catch (_: ClinicStorageException) {
                mutableState.update { it.copy(deleting = false, deleteFailed = true) }
            }
        }
    }

    fun cancelDelete() {
        if (state.value.deleting) return
        mutableState.update { it.copy(deleteTarget = null, deleteFailed = false) }
    }

    companion object {
        fun factory(store: ClinicStore) = viewModelFactory {
            initializer { PatientsViewModel(store) }
        }
    }
}

private fun List<DemoPatient>.sortedByName() = sortedBy { it.name.clinicSearchKey() }
