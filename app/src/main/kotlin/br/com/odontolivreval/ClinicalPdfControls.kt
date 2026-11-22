package br.com.odontolivreval

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

@Composable
internal fun ClinicalPdfControls(
    visitId: Long?,
    saving: Boolean,
    export: (ClinicalExport, String) -> Unit,
) {
    var showReceipt by remember { mutableStateOf(false) }

    OutlinedButton(enabled = visitId != null && !saving, onClick = {
        visitId?.let { export(ClinicalExport.Evolution(it), "Evolucao-$it.pdf") }
    }) { Text("Exportar evolução para assinatura") }
    Text(
        "Confira o PDF e assine em um assinador externo. Use VALIDAR para conferir a assinatura e arquive o PDF final em Assinaturas. O app guarda o arquivo importado.",
        style = MaterialTheme.typography.bodySmall,
    )

    OutlinedButton(enabled = !saving, onClick = {
        export(ClinicalExport.RecordCopy, "Copia-ficha-e-evolucoes.pdf")
    }) { Text("Exportar cópia da ficha e das evoluções") }
    Text(
        "Confira a identidade do destinatário e sua autorização antes de entregar. Inclua separadamente os anexos e documentos solicitados. O PDF salvo fica fora do armazenamento protegido do app.",
        style = MaterialTheme.typography.bodySmall,
    )
    OutlinedButton(enabled = !saving, onClick = { showReceipt = true }) {
        Text("Preparar recibo de entrega")
    }

    if (showReceipt) {
        var recipient by remember { mutableStateOf("") }
        var inventory by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showReceipt = false },
            title = { Text("Recibo de entrega") },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    Text("Liste apenas os documentos a entregar. Confira com o destinatário, preencha a data da entrega e colha as duas assinaturas.")
                    OutlinedTextField(
                        value = recipient,
                        onValueChange = { recipient = it.take(240) },
                        label = { Text("Nome e vínculo com o paciente") },
                    )
                    OutlinedTextField(
                        value = inventory,
                        onValueChange = { inventory = it.take(4000) },
                        label = { Text("Documentos e indicação de original ou cópia") },
                        minLines = 3,
                    )
                }
            },
            confirmButton = {
                TextButton(enabled = recipient.isNotBlank() && inventory.isNotBlank(), onClick = {
                    showReceipt = false
                    export(ClinicalExport.Receipt(recipient.trim(), inventory.trim()), "Recibo-entrega.pdf")
                }) { Text("Salvar recibo em PDF") }
            },
            dismissButton = {
                TextButton(onClick = { showReceipt = false }) { Text("Cancelar") }
            },
        )
    }
}
