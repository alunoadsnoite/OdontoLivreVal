package br.com.odontolivreval

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.Rule
import org.junit.Test

class NavigationTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private fun text(id: Int) = compose.activity.getString(id)
    @Test fun fiveDestinationsAndAboutRemainAvailableAfterRecreation() {
        compose.onNodeWithText(text(R.string.welcome)).assertIsDisplayed()
        for (id in listOf(R.string.patients, R.string.schedule, R.string.odontogram)) {
            compose.onAllNodesWithText(text(id)).onFirst().performClick()
            compose.onNodeWithText(text(R.string.development)).assertIsDisplayed()
        }
        compose.onAllNodesWithText(text(R.string.settings)).onFirst().performClick()
        compose.onNodeWithText(compose.activity.getString(br.com.odontolivreval.feature.settings.R.string.dark)).performClick()
        compose.onNodeWithText(text(R.string.about)).performClick()
        compose.onNodeWithText(text(R.string.license)).assertIsDisplayed()
        compose.activityRule.scenario.recreate()
        compose.onNodeWithText(text(R.string.license)).assertIsDisplayed()
        compose.onNodeWithText(text(R.string.back)).performClick()
        compose.onNodeWithText(compose.activity.getString(br.com.odontolivreval.feature.settings.R.string.dark)).assertIsSelected()
        compose.onAllNodesWithText(text(R.string.home)).onFirst().performClick()
        compose.onNodeWithText(text(R.string.welcome)).assertIsDisplayed()
    }
}
