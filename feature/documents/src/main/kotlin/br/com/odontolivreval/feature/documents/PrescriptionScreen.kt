package br.com.odontolivreval.feature.documents

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import br.com.odontolivreval.core.demo.ClinicStore
import br.com.odontolivreval.core.demo.DemoPatient
import br.com.odontolivreval.core.demo.ProfessionalProfile

@Composable
fun PrescriptionScreen(
    store: ClinicStore,
    patient: DemoPatient,
    onClose: () -> Unit,
    onSave: () -> Unit = {},
) {
    val model: PrescriptionViewModel = viewModel(factory = PrescriptionViewModel.factory(store))
    val context = LocalContext.current
    val state by model.state.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier.fillMaxWidth().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(text = stringResource(R.string.prescription_title), style = MaterialTheme.typography.headlineMedium)

        Text(text = patient.name, style = MaterialTheme.typography.titleMedium)

        Text(text = stringResource(R.string.documents_type), style = MaterialTheme.typography.titleSmall)

        if (state.medications.isEmpty()) {
            Text(text = stringResource(R.string.prescription_no_medications), style = MaterialTheme.typography.bodySmall)
        } else {
            state.medications.forEachIndexed { index, medication ->
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(text = "${index + 1}. ${medication.name}", style = MaterialTheme.typography.bodyMedium)
                    if (medication.presentation.isNotEmpty()) {
                        Text(text = "${stringResource(R.string.prescription_presentation)}: ${medication.presentation}", style = MaterialTheme.typography.bodySmall)
                    }
                    if (medication.concentration.isNotEmpty()) {
                        Text(text = "${stringResource(R.string.prescription_concentration)}: ${medication.concentration}", style = MaterialTheme.typography.bodySmall)
                    }
                    if (medication.posology.isNotEmpty()) {
                        Text(text = "${stringResource(R.string.prescription_posology)}: ${medication.posology}", style = MaterialTheme.typography.bodySmall)
                    }
                    if (medication.route.isNotEmpty()) {
                        Text(text = "${stringResource(R.string.prescription_route)}: ${medication.route}", style = MaterialTheme.typography.bodySmall)
                    }
                    if (medication.duration.isNotEmpty()) {
                        Text(text = "${stringResource(R.string.prescription_duration)}: ${medication.duration}", style = MaterialTheme.typography.bodySmall)
                    }
                    if (medication.quantity.isNotEmpty()) {
                        Text(text = "${stringResource(R.string.prescription_quantity)}: ${medication.quantity}", style = MaterialTheme.typography.bodySmall)
                    }
                    if (medication.observations.isNotEmpty()) {
                        Text(text = "${stringResource(R.string.prescription_observations)}: ${medication.observations}", style = MaterialTheme.typography.bodySmall)
                    }
                }
                TextButton(onClick = { model.removeMedication(index) }) {
                    Text(stringResource(R.string.prescription_remove_medication))
                }
            }
        }

        Button(
            onClick = {
                if (!state.saving) {
                    model.addMedication(PrescriptionDocument.MedicationItem(
                        name = "",
                        presentation = "",
                        concentration = "",
                        posology = "",
                        route = "",
                        duration = "",
                        quantity = "",
                    )
                    )
                }
            },
        ) {
            Text(stringResource(R.string.prescription_add_medication))
        }

        OutlinedTextField(
            value = state.additionalInstructions,
            onValueChange = { model.updateAdditionalInstructions(it) },
            label = { Text(stringResource(R.string.prescription_additional_instructions)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = stringResource(R.string.pdf_date) + ": ${java.time.LocalDate.now()}")
            OutlinedTextField(
                value = state.validityDays.toString(),
                onValueChange = { model.updateValidityDays(it.toIntOrNull() ?: 30) },
                label = { Text(stringResource(R.string.prescription_validity_days)) },
                singleLine = true,
                modifier = Modifier.requiredWidth(100.dp),
            )
        }

        Button(
            onClick = {
                if (model.validate()) {
                    // TODO: Gerar PDF
                }
            },
            enabled = !state.saving,
        ) {
            Text(stringResource(R.string.prescription_generate_pdf))
        }

        OutlinedButton(onClick = onClose, enabled = true) {
            Text(stringResource(R.string.documents_back))
        }
    }
}
