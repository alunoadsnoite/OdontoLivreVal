package br.com.odontolivreval.feature.schedule

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import br.com.odontolivreval.core.demo.AppointmentSaveResult
import br.com.odontolivreval.core.demo.DemoAppointment
import br.com.odontolivreval.core.demo.DemoClinicStore
import br.com.odontolivreval.core.demo.ClinicStorageException
import br.com.odontolivreval.core.demo.DemoPatient
import java.time.LocalDate
import java.time.LocalTime
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
)

internal enum class ScheduleError { LOAD, SAVE, OCCUPIED, CANCEL }

internal data class ScheduleState(
    val day: LocalDate = LocalDate.now(),
    val patients: List<DemoPatient> = emptyList(),
    val appointments: List<DemoAppointment> = emptyList(),
    val loading: Boolean = true,
    val writing: Boolean = false,
    val error: ScheduleError? = null,
    val form: AppointmentForm? = null,
)

internal class ScheduleViewModel(private val store: DemoClinicStore) : ViewModel() {
    private val mutableState = MutableStateFlow(ScheduleState())
    val state = mutableState.asStateFlow()
    private var loadJob: Job? = null

    fun showDay(day: LocalDate = state.value.day) {
        if (state.value.writing) return
        loadJob?.cancel()
        mutableState.update { it.copy(day = day, loading = true, error = null) }
        loadJob = viewModelScope.launch {
            try {
                val patients = store.listPatients().sortedBy { it.name.lowercase() }
                val appointments = store.listAppointments(day)
                mutableState.update {
                    it.copy(patients = patients, appointments = appointments, loading = false)
                }
            } catch (_: ClinicStorageException) {
                mutableState.update { it.copy(error = ScheduleError.LOAD, loading = false) }
            }
        }
    }

    fun open(appointment: DemoAppointment? = null) {
        val current = state.value
        if (current.loading || current.writing || current.form != null) return
        mutableState.update {
            it.copy(
                error = null,
                form = AppointmentForm(
                    appointment?.id,
                    appointment?.patientId ?: current.patients.singleOrNull()?.id,
                    current.day,
                    appointment?.time ?: LocalTime.of(9, 0),
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
        loadJob?.cancel()
        mutableState.update { it.copy(writing = true, error = null) }
        viewModelScope.launch {
            try {
                if (store.saveAppointment(form.id, patientId, form.day, form.time) == AppointmentSaveResult.TIME_OCCUPIED) {
                    mutableState.update { it.copy(writing = false, error = ScheduleError.OCCUPIED) }
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
        fun factory(store: DemoClinicStore) = viewModelFactory {
            initializer { ScheduleViewModel(store) }
        }
    }
}
