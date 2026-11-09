package br.com.odontolivreval.feature.consent

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import br.com.odontolivreval.core.demo.ConsentStatus
import br.com.odontolivreval.core.demo.ConsentType
import br.com.odontolivreval.core.demo.ClinicStorageException
import br.com.odontolivreval.core.demo.ClinicStore
import br.com.odontolivreval.core.demo.InformedConsent
import br.com.odontolivreval.core.demo.InformedConsentFile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException

internal enum class ConsentError { LOAD, SAVE, REVOKE, REVISION, NOT_FOUND, INCOMPLETE }

internal data class ConsentListState(
    val consents: List<InformedConsentFile> = emptyList(),
    val loading: Boolean = true,
    val error: ConsentError? = null,
)

internal data class ConsentFormState(
    val consent: InformedConsent? = null,
    val loading: Boolean = false,
    val saving: Boolean = false,
    val saved: Boolean = false,
    val error: ConsentError? = null,
)

internal class ConsentViewModel(private val store: ClinicStore, private val patientId: Long) : ViewModel() {

    // ---------- Lista de consentimentos ----------

    private val listMutable = MutableStateFlow(ConsentListState())
    val listState = listMutable.asStateFlow()

    fun loadConsents() {
        listMutable.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            try {
                val consents = store.listInformedConsents(patientId)
                listMutable.update { it.copy(consents = consents, loading = false) }
            } catch (_: ClinicStorageException) {
                listMutable.update { it.copy(loading = false, error = ConsentError.LOAD) }
            }
        }
    }

    fun revokeConsent(id: Long, reason: String) {
        if (reason.isBlank()) {
            listMutable.update { it.copy(error = ConsentError.INCOMPLETE) }
            return
        }
        viewModelScope.launch {
            try {
                store.revokeInformedConsent(patientId, id, reason)
                loadConsents()
            } catch (_: ClinicStorageException) {
                listMutable.update { it.copy(error = ConsentError.REVOKE) }
            }
        }
    }

    fun createRevision(id: Long) {
        viewModelScope.launch {
            try {
                val newConsent = store.createInformedConsentRevision(patientId, id)
                listMutable.update { it.copy(error = null) }
            } catch (_: ClinicStorageException) {
                listMutable.update { it.copy(error = ConsentError.REVISION) }
            }
        }
    }

    // ---------- Formulário de consentimento ----------

    private val formMutable = MutableStateFlow(ConsentFormState())
    val formState = formMutable.asStateFlow()

    fun loadConsent(consentId: Long) {
        formMutable.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            try {
                val consent = store.getInformedConsent(patientId, consentId)
                    ?: throw IOException("Consent not found")
                formMutable.update {
                    it.copy(
                        consent = consent,
                        loading = false,
                        saving = false,
                        saved = false,
                        error = null,
                    )
                }
            } catch (_: ClinicStorageException) {
                formMutable.update { it.copy(loading = false, error = ConsentError.LOAD) }
            } catch (_: IOException) {
                formMutable.update { it.copy(loading = false, error = ConsentError.NOT_FOUND) }
            }
        }
    }

    fun updateType(type: ConsentType) {
        formMutable.update { it.copy(
            consent = it.consent?.copy(type = type),
            error = null,
        )}
    }

    fun updateTitle(value: String) {
        formMutable.update { it.copy(
            consent = it.consent?.copy(title = value),
            error = null,
        )}
    }

    fun updateContent(value: String) {
        formMutable.update { it.copy(
            consent = it.consent?.copy(content = value),
            error = null,
        )}
    }

    fun updateProcedure(value: String) {
        formMutable.update { it.copy(
            consent = it.consent?.copy(procedure = value),
            error = null,
        )}
    }

    fun updateObjectives(value: String) {
        formMutable.update { it.copy(
            consent = it.consent?.copy(objectives = value),
            error = null,
        )}
    }

    fun updateExplanation(value: String) {
        formMutable.update { it.copy(
            consent = it.consent?.copy(explanation = value),
            error = null,
        )}
    }

    fun updateBenefits(value: String) {
        formMutable.update { it.copy(
            consent = it.consent?.copy(benefits = value),
            error = null,
        )}
    }

    fun updateRisks(value: String) {
        formMutable.update { it.copy(
            consent = it.consent?.copy(risks = value),
            error = null,
        )}
    }

    fun updateAlternatives(value: String) {
        formMutable.update { it.copy(
            consent = it.consent?.copy(alternatives = value),
            error = null,
        )}
    }

    fun updateConsequencesOfNotPerforming(value: String) {
        formMutable.update { it.copy(
            consent = it.consent?.copy(consequencesOfNotPerforming = value),
            error = null,
        )}
    }

    fun updateCareInstructions(value: String) {
        formMutable.update { it.copy(
            consent = it.consent?.copy(careInstructions = value),
            error = null,
        )}
    }

    fun updateLimitations(value: String) {
        formMutable.update { it.copy(
            consent = it.consent?.copy(limitations = value),
            error = null,
        )}
    }

    fun updatePatientQuestions(value: String) {
        formMutable.update { it.copy(
            consent = it.consent?.copy(patientQuestions = value),
            error = null,
        )}
    }

    fun updateClarificationStatement(value: String) {
        formMutable.update { it.copy(
            consent = it.consent?.copy(clarificationStatement = value),
            error = null,
        )}
    }

    fun updatePlaceOfIssue(value: String) {
        formMutable.update { it.copy(
            consent = it.consent?.copy(placeOfIssue = value),
            error = null,
        )}
    }

    fun updateDecision(decision: String, recordedAt: java.time.LocalDate, proofMethod: String, hasSignedDocument: Boolean) {
        formMutable.update { it.copy(
            consent = it.consent?.copy(
                consentProof = br.com.odontolivreval.core.demo.ConsentProof(
                    patientDecision = decision,
                    recordedAt = recordedAt,
                    proofMethod = proofMethod,
                    hasSignedDocument = hasSignedDocument,
                ),
            ),
            error = null,
        )}
    }

    fun updateLegalRepresentative(name: String, relationship: String, identification: String) {
        formMutable.update { it.copy(
            consent = it.consent?.copy(
                legalRepresentative = br.com.odontolivreval.core.demo.LegalRepresentative(
                    name = name,
                    relationship = relationship,
                    identification = identification,
                ),
            ),
            error = null,
        )}
    }

    fun validateForm(): Boolean {
        val current = formState.value
        val consent = current.consent
        val error = when {
            consent == null || consent.title.isBlank() -> ConsentError.INCOMPLETE
            consent.content.isBlank() -> ConsentError.INCOMPLETE
            consent.procedure.isBlank() -> ConsentError.INCOMPLETE
            else -> null
        }
        if (error != null) {
            formMutable.update { it.copy(error = error) }
            return false
        }
        formMutable.update { it.copy(error = null) }
        return true
    }

    fun saveConsent() {
        if (formState.value.saving) return
        if (!validateForm()) return

        val current = formState.value
        val consent = current.consent ?: return
        formMutable.update { it.copy(saving = true, error = null, saved = false) }

        viewModelScope.launch {
            try {
                val saved = store.saveInformedConsent(patientId, consent)
                formMutable.update {
                    it.copy(
                        consent = saved,
                        saving = false,
                        saved = true,
                        error = null,
                    )
                }
            } catch (_: ClinicStorageException) {
                formMutable.update { it.copy(saving = false, error = ConsentError.SAVE) }
            }
        }
    }

    fun presentConsent() {
        val current = formState.value.consent ?: return
        if (current.status != ConsentStatus.DRAFT) {
            formMutable.update { it.copy(error = ConsentError.INCOMPLETE) }
            return
        }
        formMutable.update { it.copy(saving = true, error = null) }

        viewModelScope.launch {
            try {
                val presented = current.copy(status = ConsentStatus.PRESENTED)
                val saved = store.saveInformedConsent(patientId, presented)
                formMutable.update {
                    it.copy(
                        consent = saved,
                        saving = false,
                        saved = true,
                        error = null,
                    )
                }
            } catch (_: ClinicStorageException) {
                formMutable.update { it.copy(saving = false, error = ConsentError.SAVE) }
            }
        }
    }

    fun acceptConsent(decision: String, recordedAt: java.time.LocalDate, proofMethod: String, hasSignedDocument: Boolean) {
        val current = formState.value.consent ?: return
        if (current.status != ConsentStatus.PRESENTED && current.status != ConsentStatus.PENDING) {
            formMutable.update { it.copy(error = ConsentError.INCOMPLETE) }
            return
        }
        formMutable.update { it.copy(saving = true, error = null) }

        viewModelScope.launch {
            try {
                val accepted = current.copy(
                    status = ConsentStatus.ACCEPTED,
                    consentProof = br.com.odontolivreval.core.demo.ConsentProof(
                        patientDecision = decision,
                        recordedAt = recordedAt,
                        proofMethod = proofMethod,
                        hasSignedDocument = hasSignedDocument,
                    ),
                )
                val saved = store.saveInformedConsent(patientId, accepted)
                formMutable.update {
                    it.copy(
                        consent = saved,
                        saving = false,
                        saved = true,
                        error = null,
                    )
                }
            } catch (_: ClinicStorageException) {
                formMutable.update { it.copy(saving = false, error = ConsentError.SAVE) }
            }
        }
    }

    fun rejectConsent(decision: String, recordedAt: java.time.LocalDate, proofMethod: String, hasSignedDocument: Boolean) {
        val current = formState.value.consent ?: return
        if (current.status != ConsentStatus.PRESENTED && current.status != ConsentStatus.PENDING) {
            formMutable.update { it.copy(error = ConsentError.INCOMPLETE) }
            return
        }
        formMutable.update { it.copy(saving = true, error = null) }

        viewModelScope.launch {
            try {
                val rejected = current.copy(
                    status = ConsentStatus.REJECTED,
                    consentProof = br.com.odontolivreval.core.demo.ConsentProof(
                        patientDecision = decision,
                        recordedAt = recordedAt,
                        proofMethod = proofMethod,
                        hasSignedDocument = hasSignedDocument,
                    ),
                )
                val saved = store.saveInformedConsent(patientId, rejected)
                formMutable.update {
                    it.copy(
                        consent = saved,
                        saving = false,
                        saved = true,
                        error = null,
                    )
                }
            } catch (_: ClinicStorageException) {
                formMutable.update { it.copy(saving = false, error = ConsentError.SAVE) }
            }
        }
    }

    fun clearForm() {
        formMutable.update { ConsentFormState() }
    }

    companion object {
        fun factory(store: ClinicStore, patientId: Long) = viewModelFactory {
            initializer { ConsentViewModel(store, patientId) }
        }
    }
}
