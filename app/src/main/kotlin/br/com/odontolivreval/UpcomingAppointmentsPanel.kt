package br.com.odontolivreval

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.odontolivreval.core.demo.ClinicStore
import br.com.odontolivreval.core.demo.DemoAppointment
import br.com.odontolivreval.core.demo.DemoPatient
import java.time.format.DateTimeFormatter

private val upcomingDayFormat = DateTimeFormatter.ofPattern("dd/MM")
private val upcomingTimeFormat = DateTimeFormatter.ofPattern("HH:mm")

@Composable
fun UpcomingAppointmentsPanel(store: ClinicStore, onPatientClick: (DemoPatient) -> Unit) {
    val model: HomeViewModel = viewModel(factory = HomeViewModel.factory(store))
    val state by model.state.collectAsStateWithLifecycle()

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.home_upcoming_title), style = MaterialTheme.typography.titleLarge)
        when {
            state.loading -> Text(stringResource(R.string.home_upcoming_loading))
            state.failed -> {
                Text(stringResource(R.string.home_upcoming_error), color = MaterialTheme.colorScheme.error)
                TextButton(onClick = model::load) { Text(stringResource(R.string.home_upcoming_retry)) }
            }
            state.upcoming.isEmpty() -> Text(
                stringResource(R.string.home_upcoming_empty),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            else -> state.upcoming.forEach { appointment ->
                Card(Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            stringResource(
                                R.string.home_upcoming_entry,
                                appointment.day.format(upcomingDayFormat),
                                appointment.time.format(upcomingTimeFormat),
                                appointment.patientName,
                            ),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                stringResource(R.string.home_upcoming_duration, appointment.durationMinutes),
                                style = MaterialTheme.typography.bodySmall,
                            )
                            if (appointment.returnOfId != null) {
                                Text(
                                    stringResource(R.string.home_upcoming_return),
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                        }
                        TextButton(
                            onClick = {
                                onPatientClick(DemoPatient(appointment.patientId, appointment.patientName))
                            },
                        ) { Text(stringResource(R.string.home_upcoming_open)) }
                    }
                }
            }
        }
    }
}
