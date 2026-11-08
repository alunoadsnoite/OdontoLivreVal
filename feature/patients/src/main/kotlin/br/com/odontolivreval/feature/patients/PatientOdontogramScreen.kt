package br.com.odontolivreval.feature.patients

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val upperRight = 11..18
private val upperLeft = 21..28
private val lowerRight = 31..38
private val lowerLeft = 41..48

private data class ToothStatus(
    val code: String,
    val label: Int,
    val color: @Composable () -> Color,
)

@Composable
fun PatientOdontogramScreen(
    odontogram: Map<String, String> = emptyMap(),
    onOdontogramChange: (Map<String, String>) -> Unit = { },
) {
    var editingTooth by remember { mutableStateOf<Int?>(null) }

    Column(
        modifier = Modifier.padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(R.string.odontogram),
            style = MaterialTheme.typography.headlineLarge,
        )
        Text(
            text = stringResource(R.string.odontogram_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(R.string.odontogram_xray),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = stringResource(R.string.odontogram_xray_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        ToothRow(
            label = stringResource(R.string.odontogram_upper),
            groups = listOf(upperRight, upperLeft),
            odontogram = odontogram,
            onEdit = { editingTooth = it },
        )
        ToothRow(
            label = stringResource(R.string.odontogram_lower),
            groups = listOf(lowerRight, lowerLeft),
            odontogram = odontogram,
            onEdit = { editingTooth = it },
        )

        Text(
            text = stringResource(R.string.odontogram_legend),
            style = MaterialTheme.typography.titleSmall,
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            statuses.forEach { status ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .background(status.color())
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(3.dp)),
                    )
                    Text(
                        text = stringResource(status.label),
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(start = 4.dp),
                    )
                }
            }
        }
    }

    editingTooth?.let { tooth ->
        val current = odontogram[tooth.toString()].orEmpty()
        AlertDialog(
            onDismissRequest = { editingTooth = null },
            title = { Text(stringResource(R.string.odontogram_change_title, tooth.toString())) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    statuses.forEach { status ->
                        TextButton(
                            onClick = {
                                val updated = if (status.code.isEmpty()) {
                                    odontogram - tooth.toString()
                                } else {
                                    odontogram + (tooth.toString() to status.code)
                                }
                                onOdontogramChange(updated)
                                editingTooth = null
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                text = stringResource(status.label) +
                                    if (status.code == current) " ✓" else "",
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { editingTooth = null }) {
                    Text(stringResource(R.string.demo_patients_cancel))
                }
            },
        )
    }
}

@Composable
private fun ToothRow(
    label: String,
    groups: List<IntRange>,
    odontogram: Map<String, String>,
    onEdit: (Int) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(text = label, style = MaterialTheme.typography.titleMedium)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .height(64.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            groups.forEachIndexed { index, range ->
                if (index > 0) {
                    Text(
                        text = "•",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 2.dp),
                    )
                }
                range.forEach { number ->
                    ToothBox(
                        number = number,
                        status = odontogram[number.toString()].orEmpty(),
                        onClick = { onEdit(number) },
                    )
                }
            }
        }
    }
}

@Composable
private fun ToothBox(
    number: Int,
    status: String,
    onClick: () -> Unit,
) {
    val definition = statuses.firstOrNull { it.code == status } ?: statuses.first()
    val color = definition.color()
    val contentColor = when (status) {
        "C" -> MaterialTheme.colorScheme.onError
        "R" -> MaterialTheme.colorScheme.onPrimary
        "X" -> MaterialTheme.colorScheme.onSecondary
        "I" -> MaterialTheme.colorScheme.onTertiary
        else -> MaterialTheme.colorScheme.onSurface
    }
    val description = stringResource(
        R.string.odontogram_tooth_description,
        number.toString(),
        stringResource(definition.label),
    )
    Column(
        modifier = Modifier
            .size(46.dp, 60.dp)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Box(
            modifier = Modifier
                .size(40.dp, 42.dp)
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(4.dp))
                .background(color),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = number.toString(),
                style = MaterialTheme.typography.labelMedium,
                color = contentColor,
            )
        }
        Text(
            text = if (status.isEmpty()) "" else stringResource(definition.label),
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
    }
}

private val statuses = listOf(
    ToothStatus("", R.string.odontogram_status_none) { MaterialTheme.colorScheme.surface },
    ToothStatus("C", R.string.odontogram_status_caries) { MaterialTheme.colorScheme.error },
    ToothStatus("R", R.string.odontogram_status_restoration) { MaterialTheme.colorScheme.primary },
    ToothStatus("X", R.string.odontogram_status_missing) { MaterialTheme.colorScheme.secondary },
    ToothStatus("I", R.string.odontogram_status_implant) { MaterialTheme.colorScheme.tertiary },
)
