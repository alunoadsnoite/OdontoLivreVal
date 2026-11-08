package br.com.odontolivreval

import android.app.Notification
import androidx.test.platform.app.InstrumentationRegistry
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class AppointmentRemindersTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun alarmCarriesOnlySchedulingMetadata() {
        val day = LocalDate.of(2026, 11, 9)
        val trigger = 1_794_218_400_000L
        val intent = AppointmentReminders.alarmIntent(context, 17, day, trigger)

        val extras = requireNotNull(intent.extras)
        assertEquals(setOf("appointment_id", "appointment_day", "appointment_trigger"), extras.keySet())
        assertEquals(17L, extras.getLong("appointment_id"))
        assertEquals(day.toEpochDay(), extras.getLong("appointment_day"))
        assertEquals(trigger, extras.getLong("appointment_trigger"))
    }

    @Test
    fun notificationUsesGenericContentAndPrivateVisibility() {
        val notification = AppointmentReminders.notification(context)

        assertEquals("Abra o aplicativo para consultar a agenda.", notification.extras.getCharSequence(Notification.EXTRA_TEXT))
        assertEquals(context.getString(R.string.reminder_title), notification.extras.getCharSequence(Notification.EXTRA_TITLE))
        assertEquals(Notification.VISIBILITY_PRIVATE, notification.visibility)
    }
}
