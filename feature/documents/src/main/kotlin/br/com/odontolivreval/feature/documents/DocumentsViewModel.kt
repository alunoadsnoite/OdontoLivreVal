package br.com.odontolivreval.feature.documents

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import br.com.odontolivreval.core.demo.ClinicStorageException
import br.com.odontolivreval.core.demo.ClinicStore
import br.com.odontolivreval.core.demo.PatientRecord
import br.com.odontolivreval.core.demo.ProfessionalProfile
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal enum class DocumentsError { EMPTY_CONTENT, PROFILE_MISSING }

internal data class DocumentsState(
    val loading: Boolean = true,
    val failed: Boolean = false,
    val profile: ProfessionalProfile = ProfessionalProfile(),
    val record: PatientRecord? = null,
    val type: DocumentType = DocumentType.PRESCRIPTION,
    val contents: Map<DocumentType, String> = emptyMap(),
    val error: DocumentsError? = null,
    val shared: Boolean = false,
) {
    val content: String get() = contents[type].orEmpty()
    val profileReady: Boolean get() = profile.name.isNotBlank() && profile.cro.isNotBlank()
}

internal class DocumentsViewModel(
    private val store: ClinicStore,
    private val patientId: Long,
) : ViewModel() {
    private val mutableState = MutableStateFlow(DocumentsState())
    val state = mutableState.asStateFlow()

    private var loadJob: Job? = null

    init {
        load()
    }

    fun load() {
        loadJob?.cancel()
        mutableState.update { it.copy(loading = true, failed = false) }
        loadJob = viewModelScope.launch {
            try {
                val profile = store.getProfessionalProfile()
                val record = store.getPatientRecord(patientId)
                mutableState.update {
                    it.copy(loading = false, profile = profile, record = record)
                }
            } catch (_: ClinicStorageException) {
                mutableState.update { it.copy(loading = false, failed = true) }
            }
        }
    }

    fun selectType(type: DocumentType) {
        if (type != state.value.type) {
            mutableState.update { it.copy(type = type, error = null, shared = false) }
        }
    }

    fun ensureTemplate(type: DocumentType, template: String) {
        if (state.value.contents[type].isNullOrBlank()) {
            mutableState.update { it.copy(contents = it.contents + (type to template)) }
        }
    }

    fun changeContent(value: String) {
        mutableState.update {
            it.copy(contents = it.contents + (it.type to value), error = null, shared = false)
        }
    }

    fun validate(): Boolean {
        val current = state.value
        val error = when {
            !current.profileReady -> DocumentsError.PROFILE_MISSING
            current.content.isBlank() -> DocumentsError.EMPTY_CONTENT
            else -> null
        }
        mutableState.update { it.copy(error = error) }
        return error == null
    }

    fun markShared() {
        mutableState.update { it.copy(shared = true) }
    }

    companion object {
        fun factory(store: ClinicStore, patientId: Long) = viewModelFactory {
            initializer { DocumentsViewModel(store, patientId) }
        }
    }
}
