package br.com.odontolivreval

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import br.com.odontolivreval.core.demo.ClinicStore
import br.com.odontolivreval.core.demo.DemoAppointment
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Agenda um alarme local para cada consulta futura. O lembrete é disparado
 * aproximadamente uma hora antes do horário marcado.
 */
object AppointmentReminders {
    const val CHANNEL_ID = "appointment_reminders"
    const val EXTRA_ID = "appointment_id"
    const val EXTRA_DAY = "appointment_day"
    const val EXTRA_TIME = "appointment_time"
    const val EXTRA_PATIENT = "appointment_patient"

    private const val LEAD_MILLIS = 60L * 60 * 1000
    private const val SYNC_DAYS = 60
    private val timeFormat = DateTimeFormatter.ofPattern("HH:mm")

    fun ensureChannel(context: Context) {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.reminder_channel),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = context.getString(R.string.reminder_channel_description)
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    fun sync(context: Context, store: ClinicStore) {
        val appContext = context.applicationContext
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            val upcoming = try {
                store.listUpcomingAppointments(LocalDate.now(), SYNC_DAYS)
            } catch (_: Exception) {
                return@launch
            }
            upcoming.forEach { schedule(appContext, it) }
        }
    }

    fun schedule(context: Context, appointment: DemoAppointment) {
        val trigger = appointment.day.atTime(appointment.time)
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli() - LEAD_MILLIS
        if (trigger <= System.currentTimeMillis()) return
        val alarm = context.getSystemService(AlarmManager::class.java) ?: return
        alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pending(context, appointment))
    }

    fun notify(context: Context, appointment: DemoAppointment) {
        post(
            context,
            appointment.id,
            context.getString(
                R.string.reminder_text,
                appointment.time.format(timeFormat),
                appointment.patientName,
            ),
        )
    }

    fun notifyFallback(context: Context, id: Long, time: String, patient: String) {
        if (time.isBlank() && patient.isBlank()) return
        post(context, id, context.getString(R.string.reminder_text, time, patient))
    }

    private fun post(context: Context, id: Long, text: String) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        ensureChannel(context)
        val open = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(context.getString(R.string.reminder_title))
            .setContentText(text)
            .setAutoCancel(true)
            .setContentIntent(open)
            .build()
        context.getSystemService(NotificationManager::class.java).notify(id.hashCode(), notification)
    }

    private fun pending(context: Context, appointment: DemoAppointment): PendingIntent {
        val intent = Intent(context, AppointmentReminderReceiver::class.java)
            .putExtra(EXTRA_ID, appointment.id)
            .putExtra(EXTRA_DAY, appointment.day.toEpochDay())
            .putExtra(EXTRA_TIME, appointment.time.format(timeFormat))
            .putExtra(EXTRA_PATIENT, appointment.patientName)
        return PendingIntent.getBroadcast(
            context,
            appointment.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
