package br.com.odontolivreval

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import br.com.odontolivreval.core.demo.ClinicStorageException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AppointmentReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getLongExtra(AppointmentReminders.EXTRA_ID, 0)
        if (id <= 0) return
        val day = java.time.LocalDate.ofEpochDay(intent.getLongExtra(AppointmentReminders.EXTRA_DAY, 0))
        val trigger = intent.getLongExtra(AppointmentReminders.EXTRA_TRIGGER, 0)
        if (trigger <= 0) return

        val pending = goAsync()
        val appContext = context.applicationContext
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val store = (appContext as? OdontoApplication)?.demoClinic
                val appointment = store?.listAppointments(day)
                    ?.firstOrNull { it.id == id && !it.cancelled }
                if (appointment != null) AppointmentReminders.notifyIfCurrent(appContext, appointment, trigger)
            } catch (_: ClinicStorageException) {
                // No notification without confirming that the appointment is still active.
            } finally {
                pending.finish()
            }
        }
    }
}
