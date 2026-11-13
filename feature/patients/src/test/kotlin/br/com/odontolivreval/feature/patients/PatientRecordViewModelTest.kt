package br.com.odontolivreval.feature.patients

import br.com.odontolivreval.core.demo.DemoPatient
import br.com.odontolivreval.core.demo.VisitRecord
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class PatientRecordViewModelTest {
    private val patient = DemoPatient(1, "Ana")
    @Before
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun loadsNewestVisitAsActiveAndSavesChanges() = runTest {
        val store = FakeClinicStore().apply {
            patients += patient
            visits += VisitRecord(
                id = 1,
                patientId = 1,
                visitDay = LocalDate.of(2026, 1, 1).toEpochDay(),
                visitMinute = 600,
                diagnosis = "antigo",
                odontogram = mapOf("11" to "X"),
            )
            visits += VisitRecord(
                id = 2,
                patientId = 1,
                visitDay = LocalDate.of(2026, 2, 1).toEpochDay(),
                visitMinute = 700,
                diagnosis = "recente",
            )
        }
        val model = PatientRecordViewModel(store, patient)
        runCurrent()
        assertEquals(2L, model.state.value.activeVisitId)
        assertEquals("recente", model.state.value.diagnosis)
        assertTrue(model.state.value.odontogram.isEmpty())

        model.changeOdontogram(mapOf("11" to "C"))
        model.changeDiagnosis("novo diagnóstico")
        model.changeConduct("restauração")
        model.saveRecord()
        runCurrent()

        assertEquals(2L, model.state.value.activeVisitId)
        assertFalse(model.state.value.dirty)
        assertTrue(model.state.value.saved)
        val saved = store.visits.first { it.id == 2L }
        assertEquals("novo diagnóstico", saved.diagnosis)
        assertEquals("restauração", saved.conduct)
        assertEquals("C", saved.odontogram["11"])
        assertEquals(2, model.state.value.visits.size)
    }

    @Test
    fun newVisitStartsEmptyAndReceivesNextId() = runTest {
        val store = FakeClinicStore().apply {
            patients += patient
            visits += VisitRecord(
                id = 1,
                patientId = 1,
                visitDay = LocalDate.of(2026, 1, 1).toEpochDay(),
                visitMinute = 600,
                diagnosis = "primeira",
            )
        }
        val model = PatientRecordViewModel(store, patient)
        runCurrent()
        model.newVisit()
        assertNull(model.state.value.activeVisitId)
        assertTrue(model.state.value.diagnosis.isEmpty())
        assertTrue(model.state.value.odontogram.isEmpty())

        model.changeDiagnosis("segunda consulta")
        model.saveRecord()
        runCurrent()

        assertEquals(2, model.state.value.visits.size)
        assertEquals("segunda consulta", model.state.value.visits.first().diagnosis)
        assertEquals(2L, model.state.value.activeVisitId)
    }

    @Test
    fun invalidDateAndPhoneBlockSaving() = runTest {
        val store = FakeClinicStore().apply { patients += patient }
        val model = PatientRecordViewModel(store, patient)
        runCurrent()
        model.changeBirthDate("31022020")
        model.changePhone("123")
        model.saveRecord()
        runCurrent()
        assertTrue(model.state.value.birthDateError)
        assertTrue(model.state.value.phoneError)
        assertFalse(model.state.value.saving)
        assertEquals(0, store.saves)
    }

    @Test
    fun inputMasksFormatDateAndPhone() {
        assertEquals("31/02/2020", PatientRecordViewModel.formatDateInput("31022020"))
        assertEquals("(11) 91234-5678", PatientRecordViewModel.formatPhone("11912345678"))
        assertEquals("(11) 3456-7890", PatientRecordViewModel.formatPhone("1134567890"))
    }
}
