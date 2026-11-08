package br.com.odontolivreval.feature.schedule

import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class ScheduleViewModelTest {
    private val day = LocalDate.of(2026, 11, 9)

    @Test
    fun savesRejectsDuplicateSlotAndCancels() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val store = FakeScheduleStore()
            val model = ScheduleViewModel(store)
            model.showDay(day)
            runCurrent()
            assertFalse(model.state.value.loading)
            assertEquals(1, model.state.value.patients.size)

            model.open()
            model.changeForm(model.state.value.form!!.copy(time = LocalTime.of(10, 0)))
            model.save()
            runCurrent()
            assertEquals(1, store.appointments[day].orEmpty().size)
            assertNull(model.state.value.form)
            val appointment = model.state.value.appointments.single()

            model.open()
            model.changeForm(model.state.value.form!!.copy(time = LocalTime.of(10, 0)))
            model.save()
            runCurrent()
            assertEquals(ScheduleError.OCCUPIED, model.state.value.error)
            assertEquals(1, store.appointments[day].orEmpty().count { !it.cancelled })

            model.close()
            model.cancel(appointment.id)
            runCurrent()
            assertTrue(store.appointments[day].orEmpty().single().cancelled)
            assertNull(model.state.value.error)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun monthCountsOnlyActiveVisitsAndSelectingDayReturnsToDailySchedule() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val store = FakeScheduleStore()
            store.saveAppointment(null, 1, day, LocalTime.of(9, 0))
            store.saveAppointment(null, 1, day, LocalTime.of(10, 0))
            store.cancelAppointment(store.listAppointments(day).last().id)
            val nextMonth = day.plusMonths(1)
            store.saveAppointment(null, 1, nextMonth, LocalTime.of(9, 0))

            val model = ScheduleViewModel(store)
            model.showMonth(YearMonth.from(day))
            runCurrent()
            assertEquals(mapOf(day to 1), model.state.value.activeCounts)

            model.showMonth(YearMonth.from(nextMonth))
            runCurrent()
            assertEquals(mapOf(nextMonth to 1), model.state.value.activeCounts)
            model.showDay(nextMonth)
            runCurrent()
            assertNull(model.state.value.month)
            assertEquals(nextMonth, model.state.value.day)
            assertEquals(1, model.state.value.appointments.size)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun returnUsesSamePatientAndKeepsLinkWhenRescheduled() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val store = FakeScheduleStore()
            val model = ScheduleViewModel(store)
            store.saveAppointment(null, 1, day, LocalTime.of(10, 0), durationMinutes = 45)
            model.showDay(day)
            runCurrent()
            val source = model.state.value.appointments.single()

            model.open(source, asReturn = true)
            val form = requireNotNull(model.state.value.form)
            assertNull(form.id)
            assertEquals(source.id, form.returnOfId)
            assertEquals(source.patientId, form.patientId)
            assertEquals("45", form.duration)
            assertEquals(maxOf(day.plusDays(7), LocalDate.now()), form.day)
            model.save()
            runCurrent()

            val scheduled = model.state.value.appointments.single()
            assertEquals(source.id, scheduled.returnOfId)
            assertEquals(45, scheduled.durationMinutes)
            model.open(scheduled)
            val edit = requireNotNull(model.state.value.form)
            model.changeForm(edit.copy(day = edit.day.plusDays(1), duration = "20"))
            model.save()
            runCurrent()
            assertEquals(source.id, model.state.value.appointments.single().returnOfId)
            assertEquals(20, model.state.value.appointments.single().durationMinutes)
            assertTrue(store.listAppointments(form.day).isEmpty())
        } finally {
            Dispatchers.resetMain()
        }
    }
}
