package br.com.odontolivreval.feature.documents

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import br.com.odontolivreval.core.demo.ClinicStore
import br.com.odontolivreval.core.demo.DemoPatient
import br.com.odontolivreval.core.demo.PatientRecord
import br.com.odontolivreval.core.demo.ProfessionalProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal data class PrescriptionState(
    val patient: DemoPatient? = null,
    val patientRecord: PatientRecord? = null,
    val professional: ProfessionalProfile? = null,
    val medications: List<PrescriptionDocument.MedicationItem> = emptyList(),
    val additionalInstructions: String = "",
    val validityDays: Int = 30,
    val loading: Boolean = false,
    val saving: Boolean = false,
    val saved: Boolean = false,
    val error: String? = null,
)

internal class PrescriptionViewModel(private val store: ClinicStore) : ViewModel() {
    private val mutableState = MutableStateFlow(PrescriptionState())
    val state = mutableState.asStateFlow()

    fun load(patient: DemoPatient, patientRecord: PatientRecord?, professional: ProfessionalProfile) {
        mutableState.update {
            it.copy(patient = patient, patientRecord = patientRecord, professional = professional)
        }
    }

    fun addMedication(medication: PrescriptionDocument.MedicationItem) {
        mutableState.update {
            it.copy(medications = it.medications + medication)
        }
    }

    fun updateMedication(index: Int, medication: PrescriptionDocument.MedicationItem) {
        if (index >= 0 && index < mutableState.value.medications.size) {
            mutableState.update {
                it.copy(medications = it.medications.mapIndexed { i, m ->
                    if (i == index) medication else m
                })
            }
        }
    }

    fun removeMedication(index: Int) {
        if (index >= 0 && index < mutableState.value.medications.size) {
            mutableState.update {
                it.copy(medications = it.medications.filterIndexed { i, _ -> i != index })
            }
        }
    }

    fun updateAdditionalInstructions(text: String) {
        mutableState.update { it.copy(additionalInstructions = text) }
    }

    fun updateValidityDays(days: Int) {
        mutableState.update { it.copy(validityDays = days) }
    }

    fun validate(): Boolean {
        val current = state.value
        return current.patient != null &&
            current.professional != null &&
            current.professional.name.isNotBlank() &&
            current.professional.cro.isNotBlank() &&
            current.medications.isNotEmpty() &&
            current.medications.all { m ->
                m.name.isNotBlank() && m.quantity.isNotBlank()
            }
    }

    fun clear() {
        mutableState.update {
            it.copy(
                patient = null,
                patientRecord = null,
                professional = null,
                medications = emptyList(),
                additionalInstructions = "",
                validityDays = 30,
                error = null,
            )
        }
    }

    companion object {
        fun factory(store: ClinicStore) = viewModelFactory {
            PrescriptionViewModel(store)
        }
    }
}
