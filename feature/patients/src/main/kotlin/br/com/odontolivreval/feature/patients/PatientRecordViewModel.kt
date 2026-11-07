package br.com.odontolivreval.feature.patients

import androidx.lifecycle.ViewModel
import br.com.odontolivreval.core.demo.DemoClinicStore
import br.com.odontolivreval.core.demo.DemoPatient
import br.com.odontolivreval.core.demo.PatientRecord
import br.com.odontolivreval.core.demo.VisitRecord
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.initializer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDateTime

/**
 * ViewModel do prontuário do paciente.
 *
 * Regras:
 * - O odontograma é persistido junto com a ficha.
 * - Cada paciente tem um "rasgo" (draftVisitId) de ficha ativa; salvar atualiza a
 *   última consulta ativa em vez de criar uma duplicata a cada clique.
 */
internal class PatientRecordViewModel(
    private val store: DemoClinicStore,
    private val patient: DemoPatient,
) : ViewModel() {

    private val mutableState = MutableStateFlow(PatientRecordState())
    val state = mutableState.asStateFlow()

    internal data class PatientRecordState(
        val record: PatientRecord = PatientRecord(id = 0, patientId = 0),
        val visits: List<VisitRecord> = emptyList(),
        val loading: Boolean = true,
        val loadFailed: Boolean = false,
        val saveFailed: Boolean = false,
        val draftVisitId: Long? = null,
    )

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
                val record = store.getPatientRecord(patient.id) ?: PatientRecord(id = 0, patientId = patient.id)
                val visits = store.listVisits(patient.id)
                // A ficha "ativa" é a última consulta não cancelada; seu id serve de ancla de edição.
                val draftId = visits.lastOrNull { it.id > 0 }?.id ?: null
                        mutableState.update {
                    it.copy(record = record, visits = visits, loading = false, draftVisitId = draftId)
                }
            } catch (_: Exception) {
                mutableState.update { it.copy(loading = false, loadFailed = true) }
            }
        }
    }

    fun changeBirthDate(value: String) {
        updateRecord { it.copy(birthDate = value) }
    }

    fun changePhone(value: String) {
        updateRecord { it.copy(phone = value) }
    }

    fun changeComplaint(value: String) {
        updateRecord { it.copy(complaint = value) }
    }

    fun changeHistory(value: String) {
        updateRecord { it.copy(history = value) }
    }

    fun changeObservations(value: String) {
        updateRecord { it.copy(observations = value) }
    }

    fun changeOdontogram(odontogram: Map<String, String>) {
        val visits = state.value.visits
        val lastVisit = visits.lastOrNull()
        if (lastVisit != null) {
            val updated = lastVisit.copy(odontogram = odontogram)
            mutableState.update { it.copy(visits = visits.dropLast(1) + updated) }
        }
    }

    private fun updateRecord(transform: (PatientRecord) -> PatientRecord) {
        val record = state.value.record
        mutableState.update { it.copy(record = transform(record), saveFailed = false) }
    }

    fun saveRecord() {
        val record = state.value.record
        val draftId = state.value.draftVisitId ?: patient.id
        val now = LocalDateTime.now()
        val visit = VisitRecord(
            id = draftId,
            patientId = record.patientId,
            visitDay = now.toLocalDate().toEpochDay(),
            visitMinute = now.toLocalTime().hour * 60 + now.toLocalTime().minute,
            complaint = record.complaint,
            anamnesis = record.history,
            diagnosis = "",
            conduct = "",
            observations = record.observations,
            odontogram = state.value.visits.firstOrNull { it.id == draftId }?.odontogram ?: emptyMap(),
        )
        viewModelScope.launch {
            try {
                store.saveVisit(visit)
                store.savePatientRecord(record)
                mutableState.update { it.copy(saveFailed = false) }
            } catch (_: Exception) {
                mutableState.update { it.copy(saveFailed = true) }
            }
        }
    }

    companion object {
        fun factory(store: DemoClinicStore, patient: DemoPatient) = androidx.lifecycle.viewmodel.compose.initializer {
            PatientRecordViewModel(store, patient)
        }
    }
}
