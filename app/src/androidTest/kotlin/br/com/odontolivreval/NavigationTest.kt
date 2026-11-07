package br.com.odontolivreval

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import java.util.UUID
import org.junit.Rule
import org.junit.Test

class NavigationTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private fun text(id: Int) = compose.activity.getString(id)
    @Test fun fiveDestinationsAndAboutRemainAvailableAfterRecreation() {
        compose.onNodeWithText(text(R.string.welcome)).assertIsDisplayed()
        compose.onAllNodesWithText(text(R.string.patients)).onFirst().performClick()
        compose.onNodeWithText("Pacientes de teste").assertIsDisplayed()
        compose.onAllNodesWithText(text(R.string.schedule)).onFirst().performClick()
        compose.onNodeWithText("Agenda de teste").assertIsDisplayed()
        compose.onAllNodesWithText(text(R.string.odontogram)).onFirst().performClick()
        compose.onNodeWithText(text(R.string.development)).assertIsDisplayed()
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

    @Test fun createsFindsAndEditsADemoPatient() {
        val suffix = UUID.randomUUID().toString().take(8)
        val original = "Paciente Ágata $suffix"
        val revised = "Paciente Ágata $suffix revisada"
        compose.onAllNodesWithText(text(R.string.patients)).onFirst().performClick()
        compose.waitUntil(5_000) {
            compose.onAllNodesWithText("Novo paciente").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("Novo paciente").performClick()
        compose.onNodeWithText("Salvar").assertIsNotEnabled()
        compose.onNodeWithText("Nome fictício").performTextInput(original)
        compose.onNodeWithText("Salvar").performClick()
        compose.waitUntil(5_000) {
            compose.onAllNodesWithText("Buscar pelo nome").fetchSemanticsNodes().isNotEmpty() &&
                compose.onAllNodesWithText("Salvar").fetchSemanticsNodes().isEmpty()
        }
        compose.onNodeWithText("Buscar pelo nome").performTextInput("agata $suffix")
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("Editar $original"))
        compose.onNodeWithText("Editar $original").performClick()
        compose.onNodeWithText("Nome fictício").performTextReplacement(revised)
        compose.onNodeWithText("Salvar").performClick()
        compose.waitUntil(5_000) {
            compose.onAllNodesWithText("Editar $revised").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("Editar $original").assertDoesNotExist()
        compose.activityRule.scenario.recreate()
        compose.onNodeWithText("Editar $revised").assertIsDisplayed()
    }
}
