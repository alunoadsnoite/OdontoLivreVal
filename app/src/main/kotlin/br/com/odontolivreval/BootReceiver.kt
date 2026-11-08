package br.com.odontolivreval

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Reagenda os lembretes depois que o aparelho reinicia, já que os alarmes são
 * perdidos nesse caso.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val app = context.applicationContext as? OdontoApplication ?: return
        AppointmentReminders.sync(app, app.demoClinic)
    }
}
