package br.com.odontolivreval.core.demo

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ClinicSnapshotRetentionTest {
    @Test
    fun protectsEachPatientWithAVisitIndependently() {
        val snapshot = ClinicSnapshot(
            patients = listOf(DemoPatient(1, "Paciente fictício A"), DemoPatient(2, "Paciente fictício B")),
            visits = listOf(VisitRecord(id = 1, patientId = 2, visitDay = 0, visitMinute = 0)),
        )

        assertFalse(snapshot.hasPatientHistory(1))
        assertTrue(snapshot.hasPatientHistory(2))
    }
}
