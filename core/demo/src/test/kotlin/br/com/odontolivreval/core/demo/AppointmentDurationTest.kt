package br.com.odontolivreval.core.demo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppointmentDurationTest {
    @Test
    fun adjacentIntervalsCanShareTheBoundaryButNotTheSameMinutes() {
        val appointment = AppointmentRecord(1, 1, 20_000, 600, false, durationMinutes = 45)
        assertFalse(appointment.overlaps(570, 600))
        assertFalse(appointment.overlaps(645, 660))
        assertTrue(appointment.overlaps(590, 610))
        assertTrue(appointment.overlaps(644, 660))
    }

    @Test
    fun legacyDurationsPreserveStartsAndIgnoreCancelledAppointments() {
        val appointments = listOf(
            AppointmentRecord(1, 1, 20_000, 600, false),
            AppointmentRecord(2, 1, 20_000, 605, true),
            AppointmentRecord(3, 1, 20_000, 620, false),
            AppointmentRecord(4, 1, 20_000, 1435, false),
        )
        val migrated = appointments.withLegacyDurations()
        assertEquals(appointments.map { it.minute }, migrated.map { it.minute })
        assertEquals(listOf(20, 30, 30, 5), migrated.map { it.durationMinutes })
    }
}
