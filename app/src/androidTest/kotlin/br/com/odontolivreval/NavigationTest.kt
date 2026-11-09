package br.com.odontolivreval

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import java.util.UUID
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import org.junit.Rule
import org.junit.Test

class NavigationTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private fun text(id: Int) = compose.activity.getString(id)

    @Test
    fun destinationsAndAboutRemainAvailableAfterRecreation() {
        val app = compose.activity.application as OdontoApplication
        val previousTheme = runBlocking { app.themes.theme.first() }
        try {
        compose.onNodeWithText(text(R.string.welcome)).assertIsDisplayed()
        compose.onAllNodesWithText(text(R.string.patients)).onFirst().performClick()
        compose.onNodeWithText("Cadastro de pacientes").assertIsDisplayed()
        compose.onAllNodesWithText(text(R.string.schedule)).onFirst().performClick()
        compose.onNodeWithText("Agenda de consultas").assertIsDisplayed()
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
        } finally {
            runBlocking { app.themes.setTheme(previousTheme) }
        }
    }

    @Test
    fun createsFindsAndEditsADemoPatient() {
        val suffix = UUID.randomUUID().toString().take(8)
        val original = "Paciente Ágata $suffix"
        val revised = "Paciente Ágata $suffix revisada"
        val store = (compose.activity.application as OdontoApplication).demoClinic
        try {
        compose.onAllNodesWithText(text(R.string.patients)).onFirst().performClick()
        compose.waitUntil(5_000) {
            compose.onAllNodesWithText("Novo paciente").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("Novo paciente").performClick()
        compose.onNodeWithText("Salvar").assertIsNotEnabled()
        compose.onNodeWithText("Nome do paciente").performTextInput(original)
        compose.onNodeWithText("Salvar").performClick()
        compose.waitUntil(5_000) {
            compose.onAllNodesWithText("Buscar pelo nome").fetchSemanticsNodes().isNotEmpty() &&
                compose.onAllNodesWithText("Salvar").fetchSemanticsNodes().isEmpty()
        }
        compose.onNodeWithText("Buscar pelo nome").performTextInput("agata $suffix")
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("Editar $original"))
        compose.onNodeWithText("Editar $original").performClick()
        compose.onNodeWithText("Nome do paciente").performTextReplacement(revised)
        compose.onNodeWithText("Salvar").performClick()
        compose.waitUntil(5_000) {
            compose.onAllNodesWithText("Editar $revised").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("Editar $original").assertDoesNotExist()
        compose.activityRule.scenario.recreate()
        compose.onNodeWithText("Editar $revised").assertIsDisplayed()
        } finally {
            runBlocking {
                store.listPatients().filter { suffix in it.name }.forEach { store.deletePatient(it.id) }
            }
        }
    }

    @Test
    fun opensRecordAndMarksATooth() {
        val suffix = UUID.randomUUID().toString().take(8)
        val name = "Paciente Dente $suffix"
        val store = (compose.activity.application as OdontoApplication).demoClinic
        try {
        compose.onAllNodesWithText(text(R.string.patients)).onFirst().performClick()
        compose.waitUntil(5_000) {
            compose.onAllNodesWithText("Novo paciente").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("Novo paciente").performClick()
        compose.onNodeWithText("Nome do paciente").performTextInput(name)
        compose.onNodeWithText("Salvar").performClick()
        compose.waitUntil(5_000) {
            compose.onAllNodesWithText(name).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText(name).performClick()
        compose.waitUntil(5_000) {
            compose.onAllNodesWithText("Odontograma").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithContentDescription("Dente 11: Sem marcação").performScrollTo().performClick()
        compose.onAllNodesWithText("Canal").onLast().performClick()
        compose.onNodeWithText("Mesial").performClick()
        compose.onAllNodesWithText("Cárie").onLast().performClick()
        compose.onNodeWithText("Concluir").performClick()
        compose.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        compose.onNodeWithText("Descartar alterações?").assertIsDisplayed()
        compose.onNodeWithText("Continuar editando").performClick()
        compose.onNodeWithText("Salvar ficha").performScrollTo().performClick()
        compose.waitUntil(5_000) {
            compose.onAllNodesWithText("Consulta ativa").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithContentDescription("Dente 11: Canal. 1 superfície marcada").assertExists()
        } finally {
            runBlocking {
                store.listPatients().filter { suffix in it.name }.forEach { store.deletePatient(it.id) }
            }
        }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun documentsKeepsThePatientAfterRecreationAndProfileNavigation() {
        val name = "Paciente Documento ${UUID.randomUUID().toString().take(8)}"
        val store = (compose.activity.application as OdontoApplication).demoClinic
        val patient = runBlocking { store.savePatient(null, name) }
        try {
            compose.onNodeWithText(text(R.string.welcome)).assertIsDisplayed()
            compose.onAllNodesWithText(text(R.string.patients)).onFirst().performClick()
            compose.waitUntilExactlyOneExists(hasText(name), timeoutMillis = 5_000)
            compose.onNodeWithText(name).performClick()
            compose.waitUntilExactlyOneExists(hasText("Documentos"), timeoutMillis = 5_000)
            compose.onNodeWithText("Documentos").performScrollTo().performClick()

            val patientLabel = "Paciente: $name"
            compose.onNodeWithText(patientLabel).assertIsDisplayed()
            compose.activityRule.scenario.recreate()
            compose.onNodeWithText(patientLabel).assertIsDisplayed()

            compose.waitUntilExactlyOneExists(hasText("Abrir perfil profissional"), timeoutMillis = 5_000)
            compose.onNodeWithText("Abrir perfil profissional").performScrollTo().performClick()
            compose.onNodeWithText("Perfil profissional").assertIsDisplayed()
            compose.waitUntilExactlyOneExists(hasText("Voltar"), timeoutMillis = 5_000)
            compose.onNodeWithText("Voltar").performScrollTo().performClick()

            compose.onNodeWithText(patientLabel).assertIsDisplayed()
            compose.waitUntilExactlyOneExists(hasText("Salvar PDF sem assinatura"), timeoutMillis = 5_000)
            compose.onNodeWithText("Salvar PDF sem assinatura").performScrollTo().assertIsDisplayed()
            compose.onNodeWithText("Compartilhar PDF sem assinatura").performScrollTo().assertIsDisplayed()
            compose.onNodeWithText("Documento assinado").assertDoesNotExist()
        } finally {
            runBlocking { store.deletePatient(patient.id) }
        }
    }
}
