package br.com.odontolivreval

import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import br.com.odontolivreval.core.model.AppLanguage
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class LanguageSelectionTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test
    fun changesLanguageAndKeepsChoiceAfterActivityRecreation() {
        val context = compose.activity.applicationContext
        val original = LanguagePreferences.selected(context)
        try {
            compose.onAllNodesWithText(compose.activity.getString(R.string.settings)).onFirst().performClick()
            compose.onNodeWithText(AppLanguage.ENGLISH.label).performClick()
            compose.onNodeWithText("Language").assertExists()
            compose.onNodeWithText(AppLanguage.ENGLISH.label).assertIsSelected()

            compose.onNodeWithText(AppLanguage.SPANISH.label).performClick()
            compose.activityRule.scenario.recreate()
            compose.onNodeWithText("Idioma").assertExists()
            compose.onNodeWithText(AppLanguage.SPANISH.label).assertIsSelected()
            assertEquals(AppLanguage.SPANISH, LanguagePreferences.selected(context))

            compose.onNodeWithText(AppLanguage.PORTUGUESE.label).performClick()
            compose.onNodeWithText(AppLanguage.PORTUGUESE.label).assertIsSelected()
            assertEquals("Configurações", compose.activity.getString(R.string.settings))
        } finally {
            LanguagePreferences.select(context, original)
        }
    }
}
