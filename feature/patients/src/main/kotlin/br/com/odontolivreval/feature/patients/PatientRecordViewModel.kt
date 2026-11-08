package br.com.odontolivreval.feature.patients

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import br.com.odontolivreval.core.demo.ClinicStorageException
import br.com.odontolivreval.core.demo.ClinicStore
import br.com.odontolivreval.core.demo.DemoPatient
import br.com.odontolivreval.core.demo.PatientRecord
import br.com.odontolivreval.core.demo.VisitRecord
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.format.ResolverStyle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel do prontuário do paciente.
 *
 * A ficha (dados permanentes) e a evolução clínica (consulta) são salvas juntas.
 * A consulta mais recente é carregada como rascunho ativo; "Nova consulta"
 * inicia uma evolução em branco. O odontograma pertence à consulta ativa.
 */
internal class PatientRecordViewModel(
    private val store: ClinicStore,
    private val patient: DemoPatient,
) : ViewModel() {

    private val mutableState = MutableStateFlow(PatientRecordState())
    val state = mutableState.asStateFlow()

    internal data class PatientRecordState(
        val record: PatientRecord = PatientRecord(id = 0, patientId = 0),
        val visits: List<VisitRecord> = emptyList(),
        val activeVisitId: Long? = null,
        val visitDay: Long? = null,
        val visitMinute: Int? = null,
        val diagnosis: String = "",
        val conduct: String = "",
        val odontogram: Map<String, String> = emptyMap(),
        val loading: Boolean = true,
        val loadFailed: Boolean = false,
        val saving: Boolean = false,
        val saveFailed: Boolean = false,
        val saved: Boolean = false,
        val dirty: Boolean = false,
        val birthDateError: Boolean = false,
        val phoneError: Boolean = false,
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
                val record = store.getPatientRecord(patient.id)
                    ?: PatientRecord(id = 0, patientId = patient.id)
                val visits = store.listVisits(patient.id)
                val active = visits.firstOrNull()
                mutableState.update {
                    it.copy(
                        record = record,
                        visits = visits,
                        activeVisitId = active?.id,
                        visitDay = active?.visitDay,
                        visitMinute = active?.visitMinute,
                        diagnosis = active?.diagnosis.orEmpty(),
                        conduct = active?.conduct.orEmpty(),
                        odontogram = active?.odontogram.orEmpty(),
                        loading = false,
                        dirty = false,
                        saved = false,
                    )
                }
            } catch (_: ClinicStorageException) {
                mutableState.update { it.copy(loading = false, loadFailed = true) }
            }
        }
    }

    fun changeBirthDate(value: String) {
        updateRecord { it.copy(birthDate = formatDateInput(value)) }
        mutableState.update { it.copy(birthDateError = false) }
    }

    fun changePhone(value: String) {
        updateRecord { it.copy(phone = formatPhone(value)) }
        mutableState.update { it.copy(phoneError = false) }
    }

    fun changeComplaint(value: String) = updateRecord { it.copy(complaint = value) }

    fun changeHistory(value: String) = updateRecord { it.copy(history = value) }

    fun changeObservations(value: String) = updateRecord { it.copy(observations = value) }

    fun changeDiagnosis(value: String) {
        if (state.value.saving) return
        mutableState.update { it.copy(diagnosis = value, dirty = true, saved = false) }
    }

    fun changeConduct(value: String) {
        if (state.value.saving) return
        mutableState.update { it.copy(conduct = value, dirty = true, saved = false) }
    }

    fun changeOdontogram(odontogram: Map<String, String>) {
        if (state.value.saving) return
        mutableState.update { it.copy(odontogram = odontogram, dirty = true, saved = false) }
    }

    /** Inicia uma evolução em branco. A ficha permanente é mantida. */
    fun newVisit() {
        if (state.value.saving) return
        mutableState.update {
            it.copy(
                activeVisitId = null,
                visitDay = null,
                visitMinute = null,
                diagnosis = "",
                conduct = "",
                odontogram = emptyMap(),
                dirty = true,
                saved = false,
                saveFailed = false,
            )
        }
    }

    private fun updateRecord(transform: (PatientRecord) -> PatientRecord) {
        if (state.value.saving) return
        val record = state.value.record
        mutableState.update {
            it.copy(record = transform(record), dirty = true, saved = false, saveFailed = false)
        }
    }

    fun saveRecord() {
        val current = state.value
        if (current.saving || current.loading) return
        val birthDateError = current.record.birthDate.isNotBlank() &&
            (parseBirthDate(current.record.birthDate)?.let { !it.isAfter(LocalDate.now()) } != true)
        val phoneDigits = current.record.phone.filter(Char::isDigit)
        val phoneError = current.record.phone.isNotBlank() && phoneDigits.length !in 10..11
        if (birthDateError || phoneError) {
            mutableState.update { it.copy(birthDateError = birthDateError, phoneError = phoneError) }
            return
        }

        val now = LocalDateTime.now()
        val visit = VisitRecord(
            id = current.activeVisitId ?: 0,
            patientId = patient.id,
            visitDay = current.visitDay ?: now.toLocalDate().toEpochDay(),
            visitMinute = current.visitMinute ?: (now.hour * 60 + now.minute),
            complaint = current.record.complaint,
            anamnesis = current.record.history,
            diagnosis = current.diagnosis,
            conduct = current.conduct,
            observations = current.record.observations,
            odontogram = current.odontogram,
        )
        mutableState.update { it.copy(saving = true, saveFailed = false, saved = false) }
        viewModelScope.launch {
            try {
                val saved = store.saveRecordAndVisit(current.record, visit)
                val visits = store.listVisits(patient.id)
                mutableState.update {
                    it.copy(
                        record = saved.record,
                        visits = visits,
                        activeVisitId = saved.visit.id,
                        visitDay = saved.visit.visitDay,
                        visitMinute = saved.visit.visitMinute,
                        diagnosis = saved.visit.diagnosis,
                        conduct = saved.visit.conduct,
                        odontogram = saved.visit.odontogram,
                        saving = false,
                        saveFailed = false,
                        saved = true,
                        dirty = false,
                        birthDateError = false,
                        phoneError = false,
                    )
                }
            } catch (_: ClinicStorageException) {
                mutableState.update { it.copy(saving = false, saveFailed = true) }
            }
        }
    }

    companion object {
        private val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/uuuu")
            .withResolverStyle(ResolverStyle.STRICT)

        fun factory(store: ClinicStore, patient: DemoPatient) = viewModelFactory {
            initializer { PatientRecordViewModel(store, patient) }
        }

        internal fun parseBirthDate(value: String): LocalDate? = try {
            LocalDate.parse(value, dateFormatter)
        } catch (_: DateTimeParseException) {
            null
        }

        /** Aplica máscara dd/MM/aaaa enquanto o usuário digita. */
        internal fun formatDateInput(value: String): String {
            val digits = value.filter(Char::isDigit).take(8)
            return buildString {
                append(digits.take(2))
                if (digits.length > 2) append('/').append(digits.substring(2, minOf(4, digits.length)))
                if (digits.length > 4) append('/').append(digits.substring(4))
            }
        }

        /** Aplica máscara (dd) ddddd-dddd ou (dd) dddd-dddd. */
        internal fun formatPhone(value: String): String {
            val digits = value.filter(Char::isDigit).take(11)
            if (digits.isEmpty()) return ""
            return buildString {
                append('(').append(digits.take(2))
                if (digits.length > 2) {
                    append(") ")
                    val rest = digits.substring(2)
                    if (rest.length <= 4) {
                        append(rest)
                    } else if (digits.length <= 10) {
                        append(rest.take(4)).append('-').append(rest.drop(4))
                    } else {
                        append(rest.take(5)).append('-').append(rest.drop(5))
                    }
                }
            }
        }
    }
}
