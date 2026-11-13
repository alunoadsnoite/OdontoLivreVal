package br.com.odontolivreval.feature.consent

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.odontolivreval.core.demo.ConsentStatus
import br.com.odontolivreval.core.demo.ConsentType
import br.com.odontolivreval.core.demo.ConsentProof
import br.com.odontolivreval.core.demo.DemoPatient
import br.com.odontolivreval.core.demo.ClinicStore
import br.com.odontolivreval.core.demo.LegalRepresentative
import java.io.IOException
import java.time.LocalDate

@Composable
fun ConsentFormScreen(
    store: ClinicStore,
    patient: DemoPatient,
    consentId: Long?,
    onClose: () -> Unit,
    onSaveComplete: () -> Unit,
) {
    val model: ConsentViewModel = viewModel(factory = ConsentViewModel.factory(store, patient.id))
    val formState by model.formState.collectAsStateWithLifecycle()

    var title by remember { mutableStateOf("") }
    var procedure by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var objectives by remember { mutableStateOf("") }
    var explanation by remember { mutableStateOf("") }
    var benefits by remember { mutableStateOf("") }
    var risks by remember { mutableStateOf("") }
    var alternatives by remember { mutableStateOf("") }
    var consequencesOfNotPerforming by remember { mutableStateOf("") }
    var careInstructions by remember { mutableStateOf("") }
    var limitations by remember { mutableStateOf("") }
    var patientQuestions by remember { mutableStateOf("") }
    var clarificationStatement by remember { mutableStateOf("") }
    var placeOfIssue by remember { mutableStateOf("") }
    var patientDecision by remember { mutableStateOf("") }
    var proofMethod by remember { mutableStateOf("") }
    var hasSignedDocument by remember { mutableStateOf(false) }
    var repName by remember { mutableStateOf("") }
    var repRelationship by remember { mutableStateOf("") }
    var repIdentification by remember { mutableStateOf("") }

    LaunchedEffect(consentId, formState.consent) {
        if (consentId != null) {
            model.loadConsent(consentId)
            val c = formState.consent
            if (c != null) {
                title = c.title
                procedure = c.procedure
                content = c.content
                objectives = c.objectives
                explanation = c.explanation
                benefits = c.benefits
                risks = c.risks
                alternatives = c.alternatives
                consequencesOfNotPerforming = c.consequencesOfNotPerforming
                careInstructions = c.careInstructions
                limitations = c.limitations
                patientQuestions = c.patientQuestions
                clarificationStatement = c.clarificationStatement
                placeOfIssue = c.placeOfIssue
                patientDecision = c.consentProof.patientDecision.orEmpty()
                proofMethod = c.consentProof.proofMethod.orEmpty()
                hasSignedDocument = c.consentProof.hasSignedDocument
                repName = c.legalRepresentative?.name.orEmpty()
                repRelationship = c.legalRepresentative?.relationship.orEmpty()
                repIdentification = c.legalRepresentative?.identification.orEmpty()
            }
        } else {
            model.clearForm()
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = if (consentId == null) stringResource(R.string.consent_form_new_title) else stringResource(R.string.consent_form_edit_title),
            style = MaterialTheme.typography.headlineLarge,
        )
        Text(
            text = stringResource(R.string.consents_patient, patient.name),
            style = MaterialTheme.typography.titleMedium,
        )

        Text(text = stringResource(R.string.consent_form_type), style = MaterialTheme.typography.titleSmall)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ConsentType.entries.forEach { type ->
                FilterChip(
                    selected = formState.consent?.type == type,
                    onClick = { model.updateType(type) },
                    label = { Text(stringResource(typeLabel(type))) },
                )
            }
        }

        OutlinedTextField(
            value = title,
            onValueChange = { title = it; model.updateTitle(it) },
            label = { Text(stringResource(R.string.consent_form_title)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = procedure,
            onValueChange = { procedure = it; model.updateProcedure(it) },
            label = { Text(stringResource(R.string.consent_form_procedure)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = content,
            onValueChange = { content = it; model.updateContent(it) },
            label = { Text(stringResource(R.string.consent_form_content)) },
            minLines = 6,
            modifier = Modifier.fillMaxWidth(),
        )

        SectionField(
            label = stringResource(R.string.consent_form_objectives),
            value = objectives,
            onChange = { objectives = it; model.updateObjectives(it) },
        )
        SectionField(
            label = stringResource(R.string.consent_form_explanation),
            value = explanation,
            onChange = { explanation = it; model.updateExplanation(it) },
        )
        SectionField(
            label = stringResource(R.string.consent_form_benefits),
            value = benefits,
            onChange = { benefits = it; model.updateBenefits(it) },
        )
        SectionField(
            label = stringResource(R.string.consent_form_risks),
            value = risks,
            onChange = { risks = it; model.updateRisks(it) },
        )
        SectionField(
            label = stringResource(R.string.consent_form_alternatives),
            value = alternatives,
            onChange = { alternatives = it; model.updateAlternatives(it) },
        )
        SectionField(
            label = stringResource(R.string.consent_form_consequences),
            value = consequencesOfNotPerforming,
            onChange = { consequencesOfNotPerforming = it; model.updateConsequencesOfNotPerforming(it) },
        )
        SectionField(
            label = stringResource(R.string.consent_form_care),
            value = careInstructions,
            onChange = { careInstructions = it; model.updateCareInstructions(it) },
        )
        SectionField(
            label = stringResource(R.string.consent_form_limitations),
            value = limitations,
            onChange = { limitations = it; model.updateLimitations(it) },
        )
        SectionField(
            label = stringResource(R.string.consent_form_questions),
            value = patientQuestions,
            onChange = { patientQuestions = it; model.updatePatientQuestions(it) },
        )
        SectionField(
            label = stringResource(R.string.consent_form_clarification),
            value = clarificationStatement,
            onChange = { clarificationStatement = it; model.updateClarificationStatement(it) },
        )

        OutlinedTextField(
            value = placeOfIssue,
            onValueChange = { placeOfIssue = it; model.updatePlaceOfIssue(it) },
            label = { Text(stringResource(R.string.consent_form_place)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        Text(text = stringResource(R.string.consent_form_decision), style = MaterialTheme.typography.titleSmall)
        Row(
            modifier = Modifier.fillMaxWidth(),
        ) {
            Checkbox(
                checked = hasSignedDocument,
                onCheckedChange = { hasSignedDocument = it },
            )
            TextButton(onClick = { hasSignedDocument = !hasSignedDocument }) {
                Text(stringResource(R.string.consent_form_signed_document))
            }
        }
        OutlinedTextField(
            value = patientDecision,
            onValueChange = { patientDecision = it },
            label = { Text(stringResource(R.string.consent_form_patient_decision)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = proofMethod,
            onValueChange = { proofMethod = it },
            label = { Text(stringResource(R.string.consent_form_proof_method)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        Text(text = stringResource(R.string.consent_form_representative), style = MaterialTheme.typography.titleSmall)
        OutlinedTextField(
            value = repName,
            onValueChange = { repName = it },
            label = { Text(stringResource(R.string.consent_form_rep_name)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = repRelationship,
            onValueChange = { repRelationship = it },
            label = { Text(stringResource(R.string.consent_form_rep_relationship)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = repIdentification,
            onValueChange = { repIdentification = it },
            label = { Text(stringResource(R.string.consent_form_rep_id)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        formState.error?.let { error ->
            Text(
                text = when (error) {
                    ConsentError.INCOMPLETE -> stringResource(R.string.consent_form_error_incomplete)
                    ConsentError.SAVE -> stringResource(R.string.consent_form_error_save)
                    ConsentError.NOT_FOUND -> stringResource(R.string.consent_form_error_not_found)
                    else -> stringResource(R.string.consent_form_error_unknown)
                },
                color = MaterialTheme.colorScheme.error,
            )
        }

        if (formState.saving) {
            Text(text = stringResource(R.string.consent_form_saving))
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedButton(
                enabled = !formState.saving,
                onClick = {
                    if (model.validateForm()) {
                        model.saveConsent()
                        if (formState.saved) onSaveComplete()
                    }
                },
            ) {
                Text(stringResource(R.string.consent_form_save_draft))
            }
            if (formState.consent?.status == ConsentStatus.DRAFT) {
                Button(
                    enabled = !formState.saving && formState.saved,
                    onClick = { model.presentConsent() },
                ) {
                    Text(stringResource(R.string.consent_form_present))
                }
            }
            if (formState.consent?.status == ConsentStatus.PRESENTED ||
                formState.consent?.status == ConsentStatus.PENDING
            ) {
                Button(
                    enabled = !formState.saving,
                    onClick = {
                        model.acceptConsent(
                            patientDecision,
                            LocalDate.now(),
                            proofMethod,
                            hasSignedDocument,
                        )
                    },
                ) {
                    Text(stringResource(R.string.consent_form_accept))
                }
                Button(
                    enabled = !formState.saving,
                    onClick = {
                        model.rejectConsent(
                            patientDecision,
                            LocalDate.now(),
                            proofMethod,
                            hasSignedDocument,
                        )
                    },
                ) {
                    Text(stringResource(R.string.consent_form_reject))
                }
            }
        }

        OutlinedButton(onClick = onClose) {
            Text(stringResource(R.string.consents_back))
        }
    }
}

@Composable
private fun SectionField(
    label: String,
    value: String,
    onChange: (String) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(text = label, style = MaterialTheme.typography.labelMedium)
        OutlinedTextField(
            value = value,
            onValueChange = onChange,
            label = { Text("") },
            minLines = 3,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

internal fun typeLabel(type: ConsentType): Int = when (type) {
    ConsentType.GENERAL_TREATMENT -> R.string.consent_type_general
    ConsentType.TOOTH_EXTRACTION -> R.string.consent_type_extraction
    ConsentType.ENDODONTIC_TREATMENT -> R.string.consent_type_endodontic
    ConsentType.PERIODONTAL_TREATMENT -> R.string.consent_type_periodontal
    ConsentType.RESTORATIVE_PROCEDURE -> R.string.consent_type_restorative
    ConsentType.PROSTHETIC_TREATMENT -> R.string.consent_type_prosthetic
    ConsentType.DENTAL_IMPLANT -> R.string.consent_type_implant
    ConsentType.ORTHODONTIC_TREATMENT -> R.string.consent_type_orthodontic
    ConsentType.COSMETIC_PROCEDURE -> R.string.consent_type_cosmetic
}
