package br.com.odontolivreval
import android.app.Application
import br.com.odontolivreval.feature.settings.DataStoreThemeRepository
class OdontoApplication : Application() {
    val themes by lazy { DataStoreThemeRepository(this) }
}
