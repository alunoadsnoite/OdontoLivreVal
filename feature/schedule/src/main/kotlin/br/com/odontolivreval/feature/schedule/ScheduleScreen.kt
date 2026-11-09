package br.com.odontolivreval.feature.schedule

import android.app.DatePickerDialog
import android.app.Dialog
import android.app.TimePickerDialog
import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.odontolivreval.core.demo.ClinicStore
import br.com.odontolivreval.core.demo.DemoPatient
import br.com.odontolivreval.core.demo.maxAppointmentMinutes
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

private val dateFormat = DateTimeFormatter.ofPattern("dd/MM/uuuu")
private val timeFormat = DateTimeFormatter.ofPattern("HH:mm")

@Composable
fun ScheduleScreen(
    store: ClinicStore,
    onPatientClick: (DemoPatient) -> Unit = { },
    onPatients: () -> Unit = {},
) {
    val model: ScheduleViewModel = viewModel(factory = ScheduleViewModel.factory(store))
    val state by model.state.collectAsStateWithLifecycle()
    var pickingDay by rememberSaveable { mutableStateOf(false) }
    var confirmCancel by rememberSaveable { mutableStateOf<Long?>(null) }
    LaunchedEffect(model) { model.refresh() }
    val month = state.month

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(stringResource(R.string.schedule_title), style = MaterialTheme.typography.headlineLarge)
        }
        item { Text(stringResource(R.string.schedule_demo_notice)) }
        item {
            OutlinedButton(
                onClick = { if (month == null) model.showMonth() else model.showDay() },
                enabled = !state.writing,
            ) {
                Text(stringResource(if (month == null) R.string.schedule_month_view else R.string.schedule_day_view))
            }
        }
        if (month != null) {
            item { MonthNavigation(month, model::showMonth) }
        } else item {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = { model.showDay(state.day.minusDays(1)) }, enabled = !state.writing) {
                    Text(stringResource(R.string.schedule_previous))
                }
                OutlinedButton(
                    onClick = { pickingDay = true },
                    enabled = !state.writing,
                ) { Text(state.day.format(dateFormat)) }
                TextButton(onClick = { model.showDay(state.day.plusDays(1)) }, enabled = !state.writing) {
                    Text(stringResource(R.string.schedule_next))
                }
            }
        }
        when {
            state.loading -> item { Text(stringResource(R.string.schedule_loading)) }
            state.error == ScheduleError.LOAD -> item {
                ScheduleErrorText(R.string.schedule_load_error)
                TextButton(onClick = model::refresh) { Text(stringResource(R.string.schedule_retry)) }
            }
            month != null -> item {
                Text(stringResource(R.string.schedule_month_hint), Modifier.padding(bottom = 8.dp))
                MonthCalendar(month, state.day, state.activeCounts, model::showDay)
            }
            else -> {
                if (state.error == ScheduleError.CANCEL) {
                    item { ScheduleErrorText(R.string.schedule_cancel_error) }
                }
                item {
                    if (state.patients.isEmpty()) {
                        Text(stringResource(R.string.schedule_need_patient))
                        TextButton(onClick = onPatients) { Text(stringResource(R.string.schedule_open_patients)) }
                    } else {
                        Button(onClick = { model.open() }, enabled = !state.writing) {
                            Text(stringResource(R.string.schedule_new))
                        }
                    }
                }
                if (state.appointments.isEmpty()) {
                    item { Text(stringResource(R.string.schedule_empty)) }
                }
                items(state.appointments, key = { it.id }) { appointment ->
                    Column {
                        val end = appointment.time.plusMinutes(appointment.durationMinutes.toLong())
                        Text(
                            stringResource(
                                R.string.schedule_entry,
                                appointment.time.format(timeFormat),
                                if (end == LocalTime.MIDNIGHT) "24:00" else end.format(timeFormat),
                                appointment.patientName,
                            ),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        if (appointment.returnOfId != null) Text(stringResource(R.string.schedule_return_label))
                        if (appointment.cancelled) {
                            Text(stringResource(R.string.schedule_cancelled))
                        } else {
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TextButton(onClick = { model.open(appointment) }, enabled = !state.writing) {
                                    Text(stringResource(R.string.schedule_edit))
                                }
                                TextButton(
                                    onClick = { model.open(appointment, asReturn = true) },
                                    enabled = !state.writing,
                                ) { Text(stringResource(R.string.schedule_book_return)) }
                                val patient = state.patients.firstOrNull { it.id == appointment.patientId }
                                TextButton(
                                    onClick = { patient?.let(onPatientClick) },
                                    enabled = !state.writing && patient != null,
                                ) {
                                    Text(stringResource(R.string.schedule_view_record))
                                }
                                TextButton(onClick = { confirmCancel = appointment.id }, enabled = !state.writing) {
                                    Text(stringResource(R.string.schedule_cancel_appointment))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    if (pickingDay) {
        PlatformPicker(onDismiss = { pickingDay = false }) { context ->
            datePicker(context, state.day, model::showDay)
        }
    }
    if (state.form != null) AppointmentDialog(state, model)
    confirmCancel?.let { id ->
        AlertDialog(
            onDismissRequest = { confirmCancel = null },
            title = { Text(stringResource(R.string.schedule_confirm_cancel)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmCancel = null
                    model.cancel(id)
                }) { Text(stringResource(R.string.schedule_cancel_appointment)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmCancel = null }) { Text(stringResource(R.string.schedule_back)) }
            },
        )
    }
}

@Composable
private fun AppointmentDialog(state: ScheduleState, model: ScheduleViewModel) {
    val form = state.form ?: return
    var pickingDate by rememberSaveable { mutableStateOf(false) }
    var pickingTime by rememberSaveable { mutableStateOf(false) }
    var choosingPatient by rememberSaveable { mutableStateOf(false) }
    val selectedPatient = state.patients.firstOrNull { it.id == form.patientId }
    if (choosingPatient) {
        AlertDialog(
            onDismissRequest = { choosingPatient = false },
            title = { Text(stringResource(R.string.schedule_choose_patient)) },
            text = {
                LazyColumn(Modifier.heightIn(max = 300.dp)) {
                    items(state.patients, key = { it.id }) { patient ->
                        TextButton(
                            onClick = {
                                model.changeForm(form.copy(patientId = patient.id))
                                choosingPatient = false
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text(patient.name) }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { choosingPatient = false }) { Text(stringResource(R.string.schedule_back)) }
            },
        )
        return
    }
    AlertDialog(
        onDismissRequest = model::close,
        title = {
            val title = when {
                form.id != null -> R.string.schedule_edit
                form.returnOfId != null -> R.string.schedule_book_return
                else -> R.string.schedule_new
            }
            Text(stringResource(title))
        },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (form.returnOfId == null) {
                    OutlinedButton(onClick = { choosingPatient = true }, enabled = !state.writing) {
                        Text(selectedPatient?.name ?: stringResource(R.string.schedule_choose_patient))
                    }
                } else {
                    Text(selectedPatient?.name.orEmpty())
                }
                OutlinedButton(
                    onClick = { pickingDate = true },
                    enabled = !state.writing,
                ) { Text(stringResource(R.string.schedule_date, form.day.format(dateFormat))) }
                OutlinedButton(
                    onClick = { pickingTime = true },
                    enabled = !state.writing,
                ) { Text(stringResource(R.string.schedule_time, form.time.format(timeFormat))) }
                OutlinedTextField(
                    value = form.duration,
                    onValueChange = { value ->
                        model.changeForm(form.copy(duration = value.filter { it in '0'..'9' }.take(3)))
                    },
                    label = { Text(stringResource(R.string.schedule_duration)) },
                    supportingText = { Text(pluralStringResource(R.plurals.schedule_duration_hint, maxAppointmentMinutes, maxAppointmentMinutes)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    enabled = !state.writing,
                    isError = state.error == ScheduleError.INVALID_DURATION,
                    modifier = Modifier.fillMaxWidth(),
                )
                when (state.error) {
                    ScheduleError.OCCUPIED -> ScheduleErrorText(R.string.schedule_occupied)
                    ScheduleError.SAVE -> ScheduleErrorText(R.string.schedule_save_error)
                    ScheduleError.INVALID_DURATION -> ScheduleErrorText(R.string.schedule_invalid_duration)
                    ScheduleError.RETURN_BEFORE_SOURCE -> ScheduleErrorText(R.string.schedule_return_before_source)
                    ScheduleError.RETURN_PATIENT_MISMATCH -> ScheduleErrorText(R.string.schedule_return_patient_mismatch)
                    else -> Unit
                }
            }
        },
        confirmButton = {
            TextButton(onClick = model::save, enabled = form.patientId != null && !state.writing) {
                Text(stringResource(R.string.schedule_save))
            }
        },
        dismissButton = {
            TextButton(onClick = model::close, enabled = !state.writing) {
                Text(stringResource(R.string.schedule_back))
            }
        },
    )
    if (pickingDate) {
        PlatformPicker(onDismiss = { pickingDate = false }) { context ->
            datePicker(context, form.day) { model.changeForm(form.copy(day = it)) }
        }
    }
    if (pickingTime) {
        PlatformPicker(onDismiss = { pickingTime = false }) { context ->
            TimePickerDialog(context, { _, hour, minute ->
                model.changeForm(form.copy(time = LocalTime.of(hour, minute)))
            }, form.time.hour, form.time.minute, true)
        }
    }
}

@Composable
private fun ScheduleErrorText(resource: Int) {
    Text(stringResource(resource), color = MaterialTheme.colorScheme.error)
}

@Composable
private fun PlatformPicker(onDismiss: () -> Unit, create: (Context) -> Dialog) {
    val context = LocalContext.current
    DisposableEffect(context) {
        val dialog = create(context)
        dialog.setOnDismissListener { onDismiss() }
        dialog.show()
        onDispose {
            dialog.setOnDismissListener(null)
            dialog.dismiss()
        }
    }
}

private fun datePicker(context: Context, date: LocalDate, onSelected: (LocalDate) -> Unit) =
    DatePickerDialog(context, { _, year, month, day ->
        onSelected(LocalDate.of(year, month + 1, day))
    }, date.year, date.monthValue - 1, date.dayOfMonth)
