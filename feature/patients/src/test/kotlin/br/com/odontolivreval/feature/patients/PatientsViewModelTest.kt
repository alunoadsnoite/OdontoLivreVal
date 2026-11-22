package br.com.odontolivreval.feature.patients

import br.com.odontolivreval.core.demo.DemoPatient
import br.com.odontolivreval.core.demo.clinicSearchKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.test.resetMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class PatientsViewModelTest {
    private val testScheduler = StandardTestDispatcher()
    @Test
    fun loadsSavesAndDeletesOnlyAfterConfirmation() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val store = FakeClinicStore().apply {
                patients += DemoPatient(1, "Ágata")
            }
            val model = PatientsViewModel(store)
            runCurrent()
            assertEquals(listOf("Ágata"), model.state.value.patients.map { it.name })
            assertFalse(model.state.value.loading)

            model.openForm()
            model.changeName("Zeca")
            model.save()
            runCurrent()
            assertEquals(listOf("Ágata", "Zeca"), model.state.value.patients.map { it.name })
            assertNull(model.state.value.form)

            model.requestDelete(1)
            assertNotNull(model.state.value.deleteTarget)
            assertEquals(2, model.state.value.patients.size)
            model.confirmDelete()
            runCurrent()
            assertEquals(listOf("Zeca"), model.state.value.patients.map { it.name })
            assertNull(model.state.value.deleteTarget)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun searchKeyIgnoresCaseAndAccents() {
        assertTrue("Paciente Ágata".clinicSearchKey().contains("agata"))
    }

    @Test
    fun clearRemovesPatientsAndForm() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val store = FakeClinicStore().apply {
                patients += DemoPatient(1, "Ágata")
            }
            val model = PatientsViewModel(store)
            runCurrent()
            assertEquals(1, model.state.value.patients.size)

            model.openForm()
            model.changeName("Zeca")
            model.save()
            runCurrent()
            assertEquals(2, model.state.value.patients.size)
            // Após salvar com sucesso, o form é limpo automaticamente
            assertNull(model.state.value.form)

            // Reabre o form para testar o clear
            model.openForm(DemoPatient(1, "Ágata"))
            runCurrent()
            assertNotNull(model.state.value.form)

            model.clear()
            runCurrent()
            assertTrue(model.state.value.patients.isEmpty())
            assertNull(model.state.value.form)
            assertNull(model.state.value.deleteTarget)
        } finally {
            Dispatchers.resetMain()
        }
    }
}
