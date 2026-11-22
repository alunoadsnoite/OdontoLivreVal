package br.com.odontolivreval

import br.com.odontolivreval.core.demo.ClinicStore
import br.com.odontolivreval.core.demo.DemoPatient
import br.com.odontolivreval.core.demo.ProfessionalProfile
import br.com.odontolivreval.core.demo.VisitRecord
import java.lang.reflect.Proxy
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ClinicalPdfTest {
    private val patient = DemoPatient(42, "Paciente fictício")
    private val original = VisitRecord(5, patient.id, 20_000, 540, diagnosis = "Diagnóstico fictício")
    private val revision = original.copy(
        id = 6, previousVisitId = original.id, professionalName = "Profissional fictício",
        professionalCro = "12345", professionalCroUf = "CE", diagnosis = "Correção fictícia",
    )
    private val store = Proxy.newProxyInstance(
        ClinicStore::class.java.classLoader, arrayOf(ClinicStore::class.java),
    ) { _, method, args ->
        when (method.name) {
            "getPatientRecord" -> null
            "listVisits" -> {
                check(args[0] == patient.id)
                listOf(revision, original)
            }
            "getProfessionalProfile" -> ProfessionalProfile(name = "Profissional fictício", cro = "12345", croUF = "CE")
            else -> error("Unexpected method: ${method.name}")
        }
    } as ClinicStore

    @Test
    fun exportsTheSelectedRevisionAndKeepsLegacyAuthorshipUnknown() = runBlocking {
        val selected = ClinicalPdf.text(store, patient, ClinicalExport.Evolution(revision.id))
        assertTrue(selected.contains("Revisão da evolução nº 5"))
        assertTrue(selected.contains("CRO-CE 12345"))
        assertFalse(selected.contains("Diagnóstico fictício"))

        val copy = ClinicalPdf.text(store, patient, ClinicalExport.RecordCopy)
        assertTrue(copy.contains("Registro antigo sem profissional identificado"))
        assertTrue(copy.indexOf("Evolução nº 5") < copy.indexOf("Evolução nº 6"))
    }

    @Test
    fun receiptIdentifiesRecipientInventoryAndBothSignatureFields() = runBlocking {
        val receipt = ClinicalPdf.text(store, patient,
            ClinicalExport.Receipt("Pessoa fictícia, representante", "Radiografia original e cópia da ficha"))
        assertTrue(receipt.contains("Pessoa fictícia, representante"))
        assertTrue(receipt.contains("Radiografia original e cópia da ficha"))
        assertTrue(receipt.contains("Assinatura do destinatário"))
        assertTrue(receipt.contains("Assinatura do profissional"))
        assertFalse(receipt.contains("Diagnóstico fictício"))
    }
}
