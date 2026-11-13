package br.com.odontolivreval

import android.content.Context
import android.content.res.Configuration
import android.os.LocaleList
import androidx.core.content.edit
import br.com.odontolivreval.core.model.AppLanguage
import java.util.Locale

internal object LanguagePreferences {
    private const val KEY = "language"

    private fun preferences(context: Context) =
        context.getSharedPreferences("app_language", Context.MODE_PRIVATE)

    fun selected(context: Context): AppLanguage = AppLanguage.fromTag(preferences(context).getString(KEY, null))

    fun select(context: Context, language: AppLanguage): Boolean {
        if (language == selected(context)) return false
        preferences(context).edit { putString(KEY, language.tag) }
        return true
    }

    fun localizedContext(context: Context): Context {
        val locale = Locale.forLanguageTag(selected(context).tag)
        val config = Configuration(context.resources.configuration).apply {
            setLocales(LocaleList(locale))
        }
        return context.createConfigurationContext(config)
    }
}
