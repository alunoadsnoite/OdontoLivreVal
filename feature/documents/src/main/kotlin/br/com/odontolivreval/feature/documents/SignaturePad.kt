package br.com.odontolivreval.feature.documents

import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint as AndroidPaint
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Componente para capturar assinatura digital baseada em texto.
 * O usuário digita seu nome e o sistema gera um bitmap renderizado que é
 * embaralhado no PDF como assinatura digital.
 */
@Composable
fun SignaturePad(
    onSignatureReady: (Bitmap) -> Unit,
    onEmpty: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val signatureTextState = remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(stringResource(R.string.signature_draw_instructions), style = MaterialTheme.typography.bodySmall)

        OutlinedTextField(
            value = signatureTextState.value,
            onValueChange = { newValue ->
                signatureTextState.value = newValue
            },
            label = { Text(stringResource(R.string.signature_name)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        // Preview da assinatura
        if (signatureTextState.value.isNotBlank()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
                    .background(Color.White)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = signatureTextState.value,
                    style = MaterialTheme.typography.displaySmall.copy(
                        fontSize = 32.sp,
                    ),
                )
            }
        }

        TextButton(
            onClick = {
                signatureTextState.value = ""
                onEmpty()
            },
            enabled = signatureTextState.value.isNotBlank(),
        ) {
            Text(stringResource(R.string.signature_clear))
        }

        Button(
            onClick = {
                if (signatureTextState.value.isBlank()) {
                    onEmpty()
                    return@Button
                }
                val bitmap = textToBitmap(
                    text = signatureTextState.value,
                    width = 400,
                    height = 120,
                )
                onSignatureReady(bitmap)
            },
            enabled = signatureTextState.value.isNotBlank(),
        ) {
            Text(stringResource(R.string.signature_save))
        }
    }
}

/**
 * Renderiza o texto da assinatura em um android.graphics.Bitmap.
 */
private fun textToBitmap(text: String, width: Int, height: Int): Bitmap {
    val bitmap = Bitmap.createBitmap(width, height, android.graphics.Bitmap.Config.ARGB_8888)
    val canvas = AndroidCanvas(bitmap)
    canvas.drawColor(AndroidColor.WHITE)

    val paint = AndroidPaint().apply {
        color = AndroidColor.BLACK
        textSize = 44f
        isAntiAlias = true
        setFakeBoldText(true)
    }

    // Centraliza o texto
    val textWidth = paint.measureText(text)
    val x = (width - textWidth.toInt()) / 2
    val y = height / 2 + 20
    canvas.drawText(text, x.toFloat(), y.toFloat(), paint)

    return bitmap
}
