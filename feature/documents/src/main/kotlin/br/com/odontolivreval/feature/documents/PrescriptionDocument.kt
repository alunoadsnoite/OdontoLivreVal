package br.com.odontolivreval.feature.documents

import br.com.odontolivreval.core.demo.DemoPatient
import br.com.odontolivreval.core.demo.PatientRecord
import br.com.odontolivreval.core.demo.ProfessionalProfile
import java.time.LocalDate

data class PrescriptionDocument(
    val patient: DemoPatient,
    val patientRecord: PatientRecord?,
    val professional: ProfessionalProfile,
    val medications: List<MedicationItem>,
    val additionalInstructions: String = "",
    val issueDate: LocalDate = LocalDate.now(),
    val validityDays: Int = 30,
) {
    data class MedicationItem(
        val name: String,
        val presentation: String,
        val concentration: String,
        val posology: String,
        val route: String,
        val duration: String,
        val quantity: String,
        val observations: String = "",
    )
}
