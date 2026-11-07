package br.com.odontolivreval.feature.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import br.com.odontolivreval.core.model.ThemeMode
import java.io.IOException
import kotlinx.coroutines.flow.*

private val Context.preferences by preferencesDataStore(name = "appearance")
interface ThemeRepository {
    val theme: Flow<ThemeMode>
    suspend fun setTheme(mode: ThemeMode)
}
class DataStoreThemeRepository(private val store: DataStore<Preferences>) : ThemeRepository {
    constructor(context: Context) : this(context.applicationContext.preferences)
    private val themeKey = stringPreferencesKey("theme")
    override val theme = store.data.catch { error ->
        if (error is IOException) emit(emptyPreferences()) else throw error
    }.map { ThemeMode.fromKey(it[themeKey]) }
    override suspend fun setTheme(mode: ThemeMode) { store.edit { it[themeKey] = mode.key } }
}
