package br.com.odontolivreval

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Reagenda lembretes após reinicialização ou atualização do app.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in listOf(Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_MY_PACKAGE_REPLACED)) return
        val app = context.applicationContext as? OdontoApplication ?: return
        if (intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            AppointmentReminders.clearLegacyNotifications(app)
        }
        val pending = goAsync()
        AppointmentReminders.sync(app, app.demoClinic).invokeOnCompletion { pending.finish() }
    }
}
