package br.com.odontolivreval

import br.com.odontolivreval.core.model.AppLanguage
import org.junit.Assert.assertEquals
import org.junit.Test

class AppLanguageTest {
    @Test
    fun missingOrUnknownLanguageUsesBrazilianPortuguese() {
        assertEquals(AppLanguage.PORTUGUESE, AppLanguage.fromTag(null))
        assertEquals(AppLanguage.PORTUGUESE, AppLanguage.fromTag("de-DE"))
    }

    @Test
    fun supportedLanguageTagsRoundTrip() {
        AppLanguage.entries.forEach { language ->
            assertEquals(language, AppLanguage.fromTag(language.tag))
        }
    }
}
