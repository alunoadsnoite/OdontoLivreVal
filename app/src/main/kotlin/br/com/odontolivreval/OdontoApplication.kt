package br.com.odontolivreval

import android.app.Application
import br.com.odontolivreval.core.demo.DemoClinicStore
import br.com.odontolivreval.feature.settings.DataStoreThemeRepository

class OdontoApplication : Application() {
    val themes by lazy { DataStoreThemeRepository(this) }
    val demoClinic by lazy { DemoClinicStore(this) }

    override fun onCreate() {
        super.onCreate()
        AppointmentReminders.ensureChannel(this)
        AppointmentReminders.sync(this, demoClinic)
    }
}
