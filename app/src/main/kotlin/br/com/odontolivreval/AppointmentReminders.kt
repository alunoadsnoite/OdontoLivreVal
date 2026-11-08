package br.com.odontolivreval

import android.Manifest
import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import br.com.odontolivreval.core.demo.ClinicStorageException
import br.com.odontolivreval.core.demo.ClinicStore
import br.com.odontolivreval.core.demo.DemoAppointment
import java.time.LocalDate
import java.time.ZoneId
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
    const val EXTRA_TRIGGER = "appointment_trigger"

    private const val LEAD_MILLIS = 60L * 60 * 1000
    private const val SYNC_DAYS = 60

    fun ensureChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.reminder_channel),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = context.getString(R.string.reminder_channel_description)
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    fun sync(context: Context, store: ClinicStore) =
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            val upcoming = try {
                store.listUpcomingAppointments(LocalDate.now(), SYNC_DAYS)
            } catch (_: ClinicStorageException) {
                return@launch
            }
            upcoming.forEach { schedule(context.applicationContext, it) }
        }

    fun clearLegacyNotifications(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.activeNotifications
            .filter { it.tag == null && it.notification.channelId == CHANNEL_ID }
            .forEach { manager.cancel(it.id) }
    }

    fun schedule(context: Context, appointment: DemoAppointment) {
        val trigger = startMillis(appointment) - LEAD_MILLIS
        if (trigger <= System.currentTimeMillis()) return
        val alarm = context.getSystemService(AlarmManager::class.java) ?: return
        alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pending(context, appointment, trigger))
    }

    fun notifyIfCurrent(context: Context, appointment: DemoAppointment, scheduledTrigger: Long) {
        val start = startMillis(appointment)
        val now = System.currentTimeMillis()
        if (appointment.cancelled || scheduledTrigger != start - LEAD_MILLIS || now !in scheduledTrigger until start) return
        post(context, appointment.id)
    }

    private fun startMillis(appointment: DemoAppointment): Long =
        appointment.day.atTime(appointment.time).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

    private fun post(context: Context, id: Long) {
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
        context.getSystemService(NotificationManager::class.java).notify(id.toString(), 0, notification(context, open))
    }

    internal fun notification(context: Context, open: PendingIntent? = null): Notification =
        NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(context.getString(R.string.reminder_title))
            .setContentText(context.getString(R.string.reminder_text))
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setAutoCancel(true)
            .setContentIntent(open)
            .build()

    internal fun alarmIntent(context: Context, id: Long, day: LocalDate, trigger: Long): Intent =
        Intent(context, AppointmentReminderReceiver::class.java)
            .setData(Uri.parse("odontolivreval://reminder/$id"))
            .putExtra(EXTRA_ID, id)
            .putExtra(EXTRA_DAY, day.toEpochDay())
            .putExtra(EXTRA_TRIGGER, trigger)

    private fun pending(context: Context, appointment: DemoAppointment, trigger: Long): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            appointment.id.hashCode(),
            alarmIntent(context, appointment.id, appointment.day, trigger),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
}
