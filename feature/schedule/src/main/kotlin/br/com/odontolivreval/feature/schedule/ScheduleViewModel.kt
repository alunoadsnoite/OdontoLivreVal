package br.com.odontolivreval.feature.schedule

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import br.com.odontolivreval.core.demo.AppointmentSaveResult
import br.com.odontolivreval.core.demo.ClinicStorageException
import br.com.odontolivreval.core.demo.ClinicStore
import br.com.odontolivreval.core.demo.DemoAppointment
import br.com.odontolivreval.core.demo.DemoPatient
import br.com.odontolivreval.core.demo.clinicSearchKey
import br.com.odontolivreval.core.demo.defaultAppointmentMinutes
import br.com.odontolivreval.core.demo.maxAppointmentMinutes
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal data class AppointmentForm(
    val id: Long?,
    val patientId: Long?,
    val day: LocalDate,
    val time: LocalTime,
    val duration: String,
    val returnOfId: Long? = null,
)

internal enum class ScheduleError {
    LOAD, SAVE, OCCUPIED, CANCEL, INVALID_DURATION, RETURN_BEFORE_SOURCE, RETURN_PATIENT_MISMATCH,
}

internal data class ScheduleState(
    val day: LocalDate = LocalDate.now(),
    val month: YearMonth? = null,
    val activeCounts: Map<LocalDate, Int> = emptyMap(),
    val patients: List<DemoPatient> = emptyList(),
    val appointments: List<DemoAppointment> = emptyList(),
    val loading: Boolean = true,
    val writing: Boolean = false,
    val error: ScheduleError? = null,
    val form: AppointmentForm? = null,
)

internal class ScheduleViewModel(private val store: ClinicStore) : ViewModel() {
    private val mutableState = MutableStateFlow(ScheduleState())
    val state = mutableState.asStateFlow()
    private var loadJob: Job? = null

    fun refresh() {
        val month = state.value.month
        if (month == null) showDay() else showMonth(month)
    }

    fun showMonth(month: YearMonth = YearMonth.from(state.value.day)) {
        if (state.value.writing || state.value.form != null) return
        loadJob?.cancel()
        mutableState.update { it.copy(month = month, activeCounts = emptyMap(), loading = true, error = null) }
        loadJob = viewModelScope.launch {
            try {
                val counts = store.countActiveAppointments(month)
                mutableState.update { it.copy(activeCounts = counts, loading = false) }
            } catch (_: ClinicStorageException) {
                mutableState.update { it.copy(error = ScheduleError.LOAD, loading = false) }
            }
        }
    }

    fun showDay(day: LocalDate = state.value.day) {
        if (state.value.writing) return
        loadJob?.cancel()
        mutableState.update { it.copy(day = day, month = null, activeCounts = emptyMap(), loading = true, error = null) }
        loadJob = viewModelScope.launch {
            try {
                val patients = store.listPatients().sortedBy { it.name.clinicSearchKey() }
                val appointments = store.listAppointments(day)
                mutableState.update {
                    it.copy(patients = patients, appointments = appointments, loading = false)
                }
            } catch (_: ClinicStorageException) {
                mutableState.update { it.copy(error = ScheduleError.LOAD, loading = false) }
            }
        }
    }

    fun open(appointment: DemoAppointment? = null, asReturn: Boolean = false) {
        val current = state.value
        if (current.loading || current.writing || current.form != null) return
        if (asReturn && (appointment == null || appointment.cancelled)) return
        val day = if (asReturn) maxOf(current.day.plusDays(7), LocalDate.now()) else current.day
        mutableState.update {
            it.copy(
                error = null,
                form = AppointmentForm(
                    id = if (asReturn) null else appointment?.id,
                    patientId = appointment?.patientId ?: current.patients.singleOrNull()?.id,
                    day = day,
                    time = appointment?.time ?: LocalTime.of(9, 0),
                    duration = (appointment?.durationMinutes ?: defaultAppointmentMinutes).toString(),
                    returnOfId = if (asReturn) appointment?.id else appointment?.returnOfId,
                ),
            )
        }
    }

    fun changeForm(form: AppointmentForm) {
        if (!state.value.writing) mutableState.update { it.copy(form = form, error = null) }
    }

    fun close() {
        if (!state.value.writing) mutableState.update { it.copy(form = null, error = null) }
    }

    fun save() {
        val current = state.value
        val form = current.form ?: return
        val patientId = form.patientId ?: return
        if (current.writing) return
        val duration = form.duration.toIntOrNull()
        if (duration == null || duration !in 1..maxAppointmentMinutes) {
            mutableState.update { it.copy(error = ScheduleError.INVALID_DURATION) }
            return
        }
        loadJob?.cancel()
        mutableState.update { it.copy(writing = true, error = null) }
        viewModelScope.launch {
            try {
                val result = store.saveAppointment(form.id, patientId, form.day, form.time, duration, form.returnOfId)
                val error = when (result) {
                    AppointmentSaveResult.SAVED -> null
                    AppointmentSaveResult.TIME_OCCUPIED -> ScheduleError.OCCUPIED
                    AppointmentSaveResult.INVALID_DURATION -> ScheduleError.INVALID_DURATION
                    AppointmentSaveResult.RETURN_BEFORE_SOURCE -> ScheduleError.RETURN_BEFORE_SOURCE
                    AppointmentSaveResult.RETURN_PATIENT_MISMATCH -> ScheduleError.RETURN_PATIENT_MISMATCH
                }
                if (error != null) {
                    mutableState.update { it.copy(writing = false, error = error) }
                    return@launch
                }
                mutableState.update { it.copy(writing = false, form = null) }
                showDay(form.day)
            } catch (_: ClinicStorageException) {
                mutableState.update { it.copy(writing = false, error = ScheduleError.SAVE) }
            }
        }
    }

    fun cancel(id: Long) {
        if (state.value.writing || state.value.loading) return
        mutableState.update { it.copy(writing = true, error = null) }
        viewModelScope.launch {
            try {
                store.cancelAppointment(id)
                mutableState.update { it.copy(writing = false) }
                showDay()
            } catch (_: ClinicStorageException) {
                mutableState.update { it.copy(writing = false, error = ScheduleError.CANCEL) }
            }
        }
    }

    companion object {
        fun factory(store: ClinicStore) = viewModelFactory {
            initializer { ScheduleViewModel(store) }
        }
    }
}
