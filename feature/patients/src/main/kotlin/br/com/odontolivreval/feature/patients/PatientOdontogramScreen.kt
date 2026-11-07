package br.com.odontolivreval.feature.patients

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun PatientOdontogramScreen(
    odontogram: Map<String, String> = emptyMap(),
    onOdontogramChange: (Map<String, String>) -> Unit = { },
) {
    val currentOdontogram = remember { mutableStateOf<Map<String, String>>(odontogram) }

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

        Card(
            modifier = Modifier.fillMaxWidth().height(150.dp),
        ) {
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
                    text = "Raio-x simulado: dentes 11–18 e 21–28",
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    text = stringResource(R.string.odontogram_xray_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Text(
            text = stringResource(R.string.odontogram_upper),
            style = MaterialTheme.typography.titleMedium,
        )
        Row(
            modifier = Modifier.fillMaxWidth().height(50.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            (11..18).forEach { number ->
                ToothBox(
                    number = number.toString(),
                    status = currentOdontogram.value[number.toString()] ?: "",
                    onStatusChange = { status ->
                        val updated = currentOdontogram.value + (number.toString() to status)
                        currentOdontogram.value = updated
                        onOdontogramChange(updated)
                    },
                )
            }
            (21..28).forEach { number ->
                ToothBox(
                    number = number.toString(),
                    status = currentOdontogram.value[number.toString()] ?: "",
                    onStatusChange = { status ->
                        val updated = currentOdontogram.value + (number.toString() to status)
                        currentOdontogram.value = updated
                        onOdontogramChange(updated)
                    },
                )
            }
        }

        Text(
            text = stringResource(R.string.odontogram_lower),
            style = MaterialTheme.typography.titleMedium,
        )
        Row(
            modifier = Modifier.fillMaxWidth().height(50.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            (31..38).forEach { number ->
                ToothBox(
                    number = number.toString(),
                    status = currentOdontogram.value[number.toString()] ?: "",
                    onStatusChange = { status ->
                        val updated = currentOdontogram.value + (number.toString() to status)
                        currentOdontogram.value = updated
                        onOdontogramChange(updated)
                    },
                )
            }
            (41..48).forEach { number ->
                ToothBox(
                    number = number.toString(),
                    status = currentOdontogram.value[number.toString()] ?: "",
                    onStatusChange = { status ->
                        val updated = currentOdontogram.value + (number.toString() to status)
                        currentOdontogram.value = updated
                        onOdontogramChange(updated)
                    },
                )
            }
        }
    }
}

@Composable
private fun ToothBox(
    number: String,
    status: String,
    onStatusChange: (String) -> Unit,
) {
    val color = when (status) {
        "C" -> MaterialTheme.colorScheme.error // Caria
        "R" -> MaterialTheme.colorScheme.primary // Restauracao
        "X" -> MaterialTheme.colorScheme.secondary // Ausente/Extraido
        "I" -> MaterialTheme.colorScheme.tertiary // Implante
        else -> MaterialTheme.colorScheme.surface
    }
    Column(
        modifier = Modifier.size(40.dp, 52.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Box(
            modifier = Modifier
                .size(32.dp, 40.dp)
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(4.dp))
                .background(color),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = number,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        Text(
            text = when (status) {
                "C" -> "Caria"
                "R" -> "Rest."
                "X" -> "Ausente"
                "I" -> "Impl."
                else -> ""
            },
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
