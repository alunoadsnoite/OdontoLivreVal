package br.com.odontolivreval.core.demo

import android.graphics.Bitmap
import android.net.Uri
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth

/**
 * Contrato de persistência da clínica de demonstração.
 *
 * A interface permite testar os ViewModels sem Android e deixa explícito o
 * conjunto de operações que a camada de dados precisa oferecer.
 */
interface ClinicStore {
    suspend fun listPatients(): List<DemoPatient>

    suspend fun savePatient(id: Long?, name: String): DemoPatient

    suspend fun listAppointments(day: LocalDate): List<DemoAppointment>

    suspend fun countActiveAppointments(month: YearMonth): Map<LocalDate, Int>

    suspend fun saveAppointment(
        id: Long?,
        patientId: Long,
        day: LocalDate,
        time: LocalTime,
        durationMinutes: Int = minOf(defaultAppointmentMinutes, 1440 - time.hour * 60 - time.minute),
        returnOfId: Long? = null,
    ): AppointmentSaveResult

    suspend fun cancelAppointment(id: Long)

    /**
     * Consultas ativas a partir de [from], somando [days] dias, em ordem
     * cronológica. Usada pelo painel de próximas consultas do Início.
     */
    suspend fun listUpcomingAppointments(from: LocalDate, days: Int): List<DemoAppointment>

    suspend fun getProfessionalProfile(): ProfessionalProfile

    suspend fun saveProfessionalProfile(profile: ProfessionalProfile): ProfessionalProfile

    suspend fun getPatientRecord(patientId: Long): PatientRecord?

    suspend fun savePatientRecord(record: PatientRecord): PatientRecord

    /**
     * Grava a ficha clínica e a consulta em uma única transação, atribuindo
     * identificadores novos quando necessário.
     */
    suspend fun saveRecordAndVisit(record: PatientRecord, visit: VisitRecord): SavedRecord

    suspend fun deletePatient(id: Long)

    suspend fun listVisits(patientId: Long): List<VisitRecord>

    suspend fun saveVisit(visit: VisitRecord): VisitRecord

    suspend fun listRadiographs(patientId: Long): List<Radiograph>

    suspend fun previewRadiograph(patientId: Long, id: Long): Bitmap?

    suspend fun addRadiograph(patientId: Long, uri: Uri, fromCamera: Boolean)

    suspend fun removeRadiograph(patientId: Long, id: Long)

    suspend fun listPrescriptions(patientId: Long, archivedDocuments: Boolean = false): List<PrescriptionFile>

    suspend fun addPrescription(patientId: Long, uri: Uri, archivedDocument: Boolean = false)

    suspend fun copyPrescription(patientId: Long, id: Long, uri: Uri)

    suspend fun removePrescription(patientId: Long, id: Long)

    suspend fun listDocumentPhotos(patientId: Long): List<DocumentPhoto>

    suspend fun previewDocumentPhoto(patientId: Long, id: Long): Bitmap?

    suspend fun addDocumentPhoto(patientId: Long, uri: Uri)

    suspend fun removeDocumentPhoto(patientId: Long, id: Long)

    suspend fun listDigitalSignatures(patientId: Long? = null): List<DigitalSignature>

    suspend fun previewDigitalSignature(patientId: Long?, id: Long): Bitmap?

    suspend fun saveDigitalSignature(patientId: Long?, documentType: String?, bitmap: Bitmap): DigitalSignature

    suspend fun removeDigitalSignature(patientId: Long?, id: Long)

    suspend fun uploadProfessionalLogo(uri: Uri)

    suspend fun previewProfessionalLogo(): Bitmap?

    suspend fun removeProfessionalLogo()

    suspend fun exportBackup(uri: Uri, password: CharArray)

    suspend fun restoreBackup(uri: Uri, password: CharArray)

    // --- Atestados e declarações ---

    suspend fun listMedicalCertificates(patientId: Long): List<MedicalCertificateFile>

    suspend fun getMedicalCertificate(patientId: Long, id: Long): MedicalCertificate?

    suspend fun saveMedicalCertificate(patientId: Long, certificate: MedicalCertificate): MedicalCertificate

    suspend fun cancelMedicalCertificate(patientId: Long, id: Long, reason: String)

    suspend fun createMedicalCertificateVersion(patientId: Long, id: Long): MedicalCertificate

    suspend fun deleteMedicalCertificate(patientId: Long, id: Long)

    // --- Planos de tratamento e orçamentos ---

    suspend fun listTreatmentPlans(patientId: Long): List<TreatmentPlanFile>

    suspend fun getTreatmentPlan(patientId: Long, id: Long): TreatmentPlan?

    suspend fun saveTreatmentPlan(patientId: Long, plan: TreatmentPlan): TreatmentPlan

    suspend fun deleteTreatmentPlan(patientId: Long, id: Long)

    suspend fun listTreatmentProcedures(planId: Long): List<TreatmentProcedureFile>

    suspend fun saveTreatmentProcedure(planId: Long, procedure: TreatmentProcedure): TreatmentProcedure

    suspend fun deleteTreatmentProcedure(planId: Long, procedureId: Long)

    suspend fun reorderTreatmentProcedures(planId: Long, procedureIds: List<Long>)

    suspend fun listTreatmentBudgets(patientId: Long): List<TreatmentBudgetFile>

    suspend fun getTreatmentBudget(patientId: Long, id: Long): TreatmentBudget?

    suspend fun saveTreatmentBudget(patientId: Long, budget: TreatmentBudget): TreatmentBudget

    suspend fun cancelTreatmentBudget(patientId: Long, id: Long, reason: String)

    suspend fun createTreatmentBudgetRevision(patientId: Long, id: Long): TreatmentBudget

    suspend fun deleteTreatmentBudget(patientId: Long, id: Long)

    // --- Termos de consentimento (TCLE) ---

    suspend fun listInformedConsents(patientId: Long): List<InformedConsentFile>

    suspend fun getInformedConsent(patientId: Long, id: Long): InformedConsent?

    suspend fun saveInformedConsent(patientId: Long, consent: InformedConsent): InformedConsent

    suspend fun deleteInformedConsent(patientId: Long, id: Long)

    suspend fun revokeInformedConsent(patientId: Long, id: Long, reason: String)

    suspend fun createInformedConsentRevision(patientId: Long, id: Long): InformedConsent
}

data class SavedRecord(val record: PatientRecord, val visit: VisitRecord)
