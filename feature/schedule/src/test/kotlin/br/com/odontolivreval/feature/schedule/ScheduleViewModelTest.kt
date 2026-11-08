package br.com.odontolivreval.feature.schedule

import java.time.LocalDate
import java.time.LocalTime
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
}
