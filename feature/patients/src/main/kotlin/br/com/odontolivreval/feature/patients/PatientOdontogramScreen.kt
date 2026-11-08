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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

internal data class ToothStatus(
    val code: String,
    val label: Int,
    val color: @Composable () -> Color,
)

@Composable
fun PatientOdontogramScreen(
    odontogram: Map<String, String> = emptyMap(),
    onOdontogramChange: (Map<String, String>) -> Unit = { },
) {
    var dentition by rememberSaveable { mutableStateOf(Dentition.PERMANENT) }
    var editingTooth by rememberSaveable { mutableStateOf<Int?>(null) }

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

        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Dentition.entries.forEach { option ->
                FilterChip(
                    selected = dentition == option,
                    onClick = { dentition = option },
                    label = { Text(stringResource(option.label)) },
                )
            }
        }
        ToothRow(
            label = stringResource(R.string.odontogram_upper),
            groups = dentition.upper,
            odontogram = odontogram,
            onEdit = { editingTooth = it },
        )
        ToothRow(
            label = stringResource(R.string.odontogram_lower),
            groups = dentition.lower,
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
        ToothEditor(tooth, odontogram, onOdontogramChange) { editingTooth = null }
    }
}

@Composable
private fun ToothEditor(
    tooth: Int,
    odontogram: Map<String, String>,
    onChange: (Map<String, String>) -> Unit,
    onClose: () -> Unit,
) {
    var surface by rememberSaveable(tooth) { mutableStateOf<ToothSurface?>(null) }
    val key = toothKey(tooth, surface)
    val current = odontogram[key].orEmpty()
    val options = if (surface == null) statuses else surfaceStatuses
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text(stringResource(R.string.odontogram_change_title, tooth.toString())) },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(
                        selected = surface == null,
                        onClick = { surface = null },
                        label = { Text(stringResource(R.string.odontogram_whole)) },
                    )
                    surfacesFor(tooth).forEach { option ->
                        val label = stringResource(option.label)
                        FilterChip(
                            selected = surface == option,
                            onClick = { surface = option },
                            label = { Text(if (toothKey(tooth, option) in odontogram) "$label •" else label) },
                        )
                    }
                }
                options.forEach { status ->
                    TextButton(
                        onClick = {
                            onChange(if (status.code.isEmpty()) odontogram - key else odontogram + (key to status.code))
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        val label = stringResource(status.label)
                        Text(if (status.code == current) "$label ✓" else label)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onClose) { Text(stringResource(R.string.odontogram_done)) }
        },
    )
}

@Composable
private fun ToothRow(
    label: String,
    groups: List<IntProgression>,
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
                        status = odontogram[toothKey(number)].orEmpty(),
                        surfaceCount = surfacesFor(number).count { toothKey(number, it) in odontogram },
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
    surfaceCount: Int,
    onClick: () -> Unit,
) {
    val definition = statuses.firstOrNull { it.code == status } ?: statuses.first()
    val color = if (status.isEmpty() && surfaceCount > 0) {
        MaterialTheme.colorScheme.secondaryContainer
    } else {
        definition.color()
    }
    val contentColor = when (status) {
        "C" -> MaterialTheme.colorScheme.onError
        "R" -> MaterialTheme.colorScheme.onPrimary
        "X" -> MaterialTheme.colorScheme.onSecondary
        "I" -> MaterialTheme.colorScheme.onTertiary
        else -> MaterialTheme.colorScheme.onSurface
    }
    val surfaceLabel = pluralStringResource(R.plurals.odontogram_surface_count, surfaceCount, surfaceCount)
    val statusLabel = stringResource(definition.label)
    val summary = when {
        surfaceCount == 0 -> statusLabel
        status.isEmpty() -> surfaceLabel
        else -> "$statusLabel. $surfaceLabel"
    }
    val accessibleLabel = stringResource(R.string.odontogram_tooth_description, number.toString(), summary)
    Column(
        modifier = Modifier
            .size(46.dp, 60.dp)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = accessibleLabel },
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
            text = when {
                surfaceCount > 0 -> pluralStringResource(R.plurals.odontogram_surface_count_short, surfaceCount, surfaceCount)
                status.isNotEmpty() -> stringResource(definition.label)
                else -> ""
            },
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
    }
}

internal val statuses = listOf(
    ToothStatus("", R.string.odontogram_status_none) { MaterialTheme.colorScheme.surface },
    ToothStatus("C", R.string.odontogram_status_caries) { MaterialTheme.colorScheme.error },
    ToothStatus("E", R.string.odontogram_status_canal) { MaterialTheme.colorScheme.primaryContainer },
    ToothStatus("R", R.string.odontogram_status_restoration) { MaterialTheme.colorScheme.primary },
    ToothStatus("X", R.string.odontogram_status_missing) { MaterialTheme.colorScheme.secondary },
    ToothStatus("I", R.string.odontogram_status_implant) { MaterialTheme.colorScheme.tertiary },
)

private val surfaceStatuses = statuses.filter { it.code in setOf("", "C", "R") }
