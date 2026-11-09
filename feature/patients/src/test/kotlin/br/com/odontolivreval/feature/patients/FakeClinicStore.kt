package br.com.odontolivreval.feature.patients

import android.graphics.Bitmap
import android.net.Uri
import br.com.odontolivreval.core.demo.AppointmentSaveResult
import br.com.odontolivreval.core.demo.BudgetStatus
import br.com.odontolivreval.core.demo.CertificateStatus
import br.com.odontolivreval.core.demo.CertificateType
import br.com.odontolivreval.core.demo.ClinicStore
import br.com.odontolivreval.core.demo.DemoAppointment
import br.com.odontolivreval.core.demo.DemoPatient
import br.com.odontolivreval.core.demo.MedicalCertificate
import br.com.odontolivreval.core.demo.MedicalCertificateFile
import br.com.odontolivreval.core.demo.PatientRecord
import br.com.odontolivreval.core.demo.Radiograph
import br.com.odontolivreval.core.demo.PrescriptionFile
import br.com.odontolivreval.core.demo.ProfessionalProfile
import br.com.odontolivreval.core.demo.DocumentPhoto
import br.com.odontolivreval.core.demo.DigitalSignature
import br.com.odontolivreval.core.demo.SavedRecord
import br.com.odontolivreval.core.demo.TreatmentBudget
import br.com.odontolivreval.core.demo.TreatmentBudgetFile
import br.com.odontolivreval.core.demo.ConsentProof
import br.com.odontolivreval.core.demo.ConsentStatus
import br.com.odontolivreval.core.demo.ConsentType
import br.com.odontolivreval.core.demo.InformedConsent
import br.com.odontolivreval.core.demo.InformedConsentFile
import br.com.odontolivreval.core.demo.LegalRepresentative
import br.com.odontolivreval.core.demo.TreatmentPlan
import br.com.odontolivreval.core.demo.TreatmentPlanFile
import br.com.odontolivreval.core.demo.TreatmentProcedure
import br.com.odontolivreval.core.demo.TreatmentProcedureFile
import br.com.odontolivreval.core.demo.VisitRecord
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import kotlin.comparisons.compareBy

internal class FakeClinicStore : ClinicStore {
    val patients = mutableListOf<DemoPatient>()
    val records = mutableListOf<PatientRecord>()
    val visits = mutableListOf<VisitRecord>()
    val medicalCertificates = mutableListOf<MedicalCertificateFile>()
    val informedConsents = mutableListOf<InformedConsentFile>()
    var saves = 0
        private set
    private var nextRecordId = 1L

    override suspend fun listPatients(): List<DemoPatient> = patients.toList()

    override suspend fun savePatient(id: Long?, name: String): DemoPatient {
        val saved = DemoPatient(id ?: ((patients.maxOfOrNull { it.id } ?: 0) + 1), name)
        patients.removeAll { it.id == saved.id }
        patients.add(saved)
        return saved
    }

    override suspend fun getPatientRecord(patientId: Long): PatientRecord? =
        records.find { it.patientId == patientId }

    override suspend fun savePatientRecord(record: PatientRecord): PatientRecord {
        val saved = record.copy(id = record.id.takeIf { it > 0 } ?: nextRecordId++)
        records.removeAll { it.patientId == saved.patientId }
        records.add(saved)
        saves++
        return saved
    }

    override suspend fun saveRecordAndVisit(record: PatientRecord, visit: VisitRecord): SavedRecord {
        val savedRecord = savePatientRecord(record)
        val savedVisit = saveVisit(visit)
        return SavedRecord(savedRecord, savedVisit)
    }

    override suspend fun listVisits(patientId: Long): List<VisitRecord> = visits
        .filter { it.patientId == patientId }
        .sortedWith(compareBy<VisitRecord>({ -it.visitDay }, { -it.visitMinute }, { -it.id }))

    override suspend fun saveVisit(visit: VisitRecord): VisitRecord {
        val id = if (visit.id > 0) visit.id else ((visits.maxOfOrNull { it.id } ?: 0) + 1)
        val saved = visit.copy(id = id)
        visits.removeAll { it.id == id }
        visits.add(saved)
        saves++
        return saved
    }

    override suspend fun deletePatient(id: Long) {
        patients.removeAll { it.id == id }
        records.removeAll { it.patientId == id }
        visits.removeAll { it.patientId == id }
    }

    override suspend fun listAppointments(day: LocalDate): List<DemoAppointment> = emptyList()

    override suspend fun countActiveAppointments(month: YearMonth): Map<LocalDate, Int> = error("Not used")

    override suspend fun saveAppointment(
        id: Long?,
        patientId: Long,
        day: LocalDate,
        time: LocalTime,
        durationMinutes: Int,
        returnOfId: Long?,
    ): AppointmentSaveResult = error("Not used in patients tests")

    override suspend fun cancelAppointment(id: Long) = error("Not used in patients tests")

    override suspend fun listUpcomingAppointments(from: LocalDate, days: Int): List<DemoAppointment> = emptyList()

    override suspend fun getProfessionalProfile(): ProfessionalProfile = ProfessionalProfile()

    override suspend fun saveProfessionalProfile(profile: ProfessionalProfile): ProfessionalProfile = profile

    override suspend fun listRadiographs(patientId: Long): List<Radiograph> = error("Not used")

    override suspend fun previewRadiograph(patientId: Long, id: Long): Bitmap? = error("Not used")

    override suspend fun addRadiograph(patientId: Long, uri: Uri, fromCamera: Boolean) = error("Not used")

    override suspend fun removeRadiograph(patientId: Long, id: Long) = error("Not used")

    override suspend fun listPrescriptions(patientId: Long, archivedDocuments: Boolean): List<PrescriptionFile> = error("Not used")

    override suspend fun addPrescription(patientId: Long, uri: Uri, archivedDocument: Boolean) = error("Not used")

    override suspend fun copyPrescription(patientId: Long, id: Long, uri: Uri) = error("Not used")

    override suspend fun removePrescription(patientId: Long, id: Long) = error("Not used")

    override suspend fun listDocumentPhotos(patientId: Long): List<DocumentPhoto> = error("Not used")

    override suspend fun previewDocumentPhoto(patientId: Long, id: Long): Bitmap? = error("Not used")

    override suspend fun addDocumentPhoto(patientId: Long, uri: Uri) = error("Not used")

    override suspend fun removeDocumentPhoto(patientId: Long, id: Long) = error("Not used")

    override suspend fun listDigitalSignatures(patientId: Long?): List<DigitalSignature> = emptyList()

    override suspend fun previewDigitalSignature(patientId: Long?, id: Long): Bitmap? = error("Not used")

    override suspend fun saveDigitalSignature(patientId: Long?, documentType: String?, bitmap: Bitmap): DigitalSignature = error("Not used")

    override suspend fun removeDigitalSignature(patientId: Long?, id: Long) = error("Not used")

    override suspend fun uploadProfessionalLogo(uri: Uri) = error("Not used")

    override suspend fun previewProfessionalLogo(): Bitmap? = error("Not used")

    override suspend fun removeProfessionalLogo() = error("Not used")

    override suspend fun exportBackup(uri: Uri, password: CharArray) = error("Not used in patients tests")

    override suspend fun restoreBackup(uri: Uri, password: CharArray) = error("Not used in patients tests")

    // --- Atestados e declarações (fake para testes) ---

    override suspend fun listMedicalCertificates(patientId: Long): List<MedicalCertificateFile> =
        medicalCertificates.filter { it.patientId == patientId }

    override suspend fun getMedicalCertificate(patientId: Long, id: Long): MedicalCertificate? =
        medicalCertificates.find { it.patientId == patientId && it.id == id }
            ?.let { file ->
                MedicalCertificate(
                    id = file.id,
                    patientId = file.patientId,
                    type = file.type,
                    status = file.status,
                    title = file.title,
                    content = "",
                    serviceDate = file.serviceDate,
                    leaveDays = file.leaveDays,
                    version = file.version,
                    originalId = file.originalId,
                    placeOfIssue = "",
                    issueDate = file.serviceDate,
                )
            }

    override suspend fun saveMedicalCertificate(patientId: Long, certificate: MedicalCertificate): MedicalCertificate {
        val existing = medicalCertificates.find { it.patientId == patientId && it.id == certificate.id }
        val savedFile = if (existing != null) {
            val updated = medicalCertificates.map {
                if (it.patientId == patientId && it.id == certificate.id) {
                    it.copy(
                        type = certificate.type,
                        status = certificate.status,
                        title = certificate.title,
                        serviceDate = certificate.serviceDate,
                        leaveDays = certificate.leaveDays,
                        version = certificate.version,
                        originalId = certificate.originalId,
                    )
                } else it
            }
            medicalCertificates.clear()
            medicalCertificates.addAll(updated)
            existing
        } else {
            val newId = (medicalCertificates.maxOfOrNull { it.id } ?: 0) + 1
            val file = MedicalCertificateFile(
                id = newId,
                patientId = patientId,
                type = certificate.type,
                status = certificate.status,
                title = certificate.title,
                serviceDate = certificate.serviceDate,
                leaveDays = certificate.leaveDays,
                version = certificate.version,
                originalId = certificate.originalId,
            )
            medicalCertificates.add(file)
            file
        }
        saves++
        return MedicalCertificate(
            id = savedFile.id,
            patientId = patientId,
            type = savedFile.type,
            status = savedFile.status,
            title = savedFile.title,
            content = certificate.content,
            serviceDate = savedFile.serviceDate,
            leaveDays = savedFile.leaveDays,
            version = savedFile.version,
            originalId = savedFile.originalId,
            placeOfIssue = certificate.placeOfIssue,
            issueDate = savedFile.serviceDate,
        )
    }

    override suspend fun cancelMedicalCertificate(patientId: Long, id: Long, reason: String) {
        val idx = medicalCertificates.indexOfFirst { it.patientId == patientId && it.id == id }
        if (idx >= 0) {
            medicalCertificates[idx] = medicalCertificates[idx].copy(status = CertificateStatus.CANCELLED)
        }
    }

    override suspend fun createMedicalCertificateVersion(patientId: Long, id: Long): MedicalCertificate {
        val existing = medicalCertificates.find { it.patientId == patientId && it.id == id }
            ?: throw IllegalStateException("Certificate not found")
        val newId = (medicalCertificates.maxOfOrNull { it.id } ?: 0) + 1
        val newFile = MedicalCertificateFile(
            id = newId,
            patientId = patientId,
            type = existing.type,
            status = CertificateStatus.DRAFT,
            title = existing.title,
            serviceDate = existing.serviceDate,
            leaveDays = existing.leaveDays,
            version = existing.version + 1,
            originalId = existing.originalId ?: id,
        )
        medicalCertificates.add(newFile)
        return MedicalCertificate(
            id = newId,
            patientId = patientId,
            type = newFile.type,
            status = newFile.status,
            title = newFile.title,
            content = "",
            serviceDate = newFile.serviceDate,
            leaveDays = newFile.leaveDays,
            version = newFile.version,
            originalId = newFile.originalId,
            placeOfIssue = "",
            issueDate = newFile.serviceDate,
        )
    }

    override suspend fun deleteMedicalCertificate(patientId: Long, id: Long) {
        medicalCertificates.removeAll { it.patientId == patientId && it.id == id }
    }

    // --- Planos de tratamento e orçamentos (fake para testes) ---

    val treatmentPlans = mutableListOf<TreatmentPlanFile>()
    val treatmentProcedures = mutableListOf<TreatmentProcedureFile>()
    val treatmentBudgets = mutableListOf<TreatmentBudgetFile>()

    override suspend fun listTreatmentPlans(patientId: Long): List<TreatmentPlanFile> =
        treatmentPlans.filter { it.patientId == patientId }

    override suspend fun getTreatmentPlan(patientId: Long, id: Long): TreatmentPlan? =
        treatmentPlans.find { it.patientId == patientId && it.id == id }
            ?.let { file ->
                TreatmentPlan(
                    id = file.id,
                    patientId = file.patientId,
                    title = file.title,
                    description = file.description,
                    clinicalObjectives = "",
                    estimatedSessions = file.estimatedSessions,
                    startDate = null,
                    observations = "",
                    status = file.status,
                    createdAt = file.createdAt,
                    updatedAt = file.updatedAt,
                )
            }

    override suspend fun saveTreatmentPlan(patientId: Long, plan: TreatmentPlan): TreatmentPlan {
        val existing = treatmentPlans.find { it.patientId == patientId && it.id == plan.id }
        val savedFile = if (existing != null) {
            val updated = treatmentPlans.map {
                if (it.patientId == patientId && it.id == plan.id) {
                    it.copy(
                        title = plan.title,
                        description = plan.description,
                        estimatedSessions = plan.estimatedSessions,
                        status = plan.status,
                    )
                } else it
            }
            treatmentPlans.clear()
            treatmentPlans.addAll(updated)
            existing
        } else {
            val newId = (treatmentPlans.maxOfOrNull { it.id } ?: 0) + 1
            val file = TreatmentPlanFile(
                id = newId,
                patientId = patientId,
                title = plan.title,
                description = plan.description,
                estimatedSessions = plan.estimatedSessions,
                status = plan.status,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis(),
            )
            treatmentPlans.add(file)
            file
        }
        saves++
        return TreatmentPlan(
            id = savedFile.id,
            patientId = patientId,
            title = savedFile.title,
            description = savedFile.description,
            clinicalObjectives = plan.clinicalObjectives,
            estimatedSessions = savedFile.estimatedSessions,
            startDate = plan.startDate,
            observations = plan.observations,
            status = savedFile.status,
            createdAt = savedFile.createdAt,
            updatedAt = savedFile.updatedAt,
        )
    }

    override suspend fun deleteTreatmentPlan(patientId: Long, id: Long) {
        treatmentPlans.removeAll { it.patientId == patientId && it.id == id }
        treatmentProcedures.removeAll { it.planId == id }
        treatmentBudgets.removeAll { it.planId == id }
    }

    override suspend fun listTreatmentProcedures(planId: Long): List<TreatmentProcedureFile> =
        treatmentProcedures.filter { it.planId == planId }
            .sortedWith(compareBy({ it.treatmentStage }, { it.id }))

    override suspend fun saveTreatmentProcedure(planId: Long, procedure: TreatmentProcedure): TreatmentProcedure {
        val existing = treatmentProcedures.find { it.planId == planId && it.id == procedure.id }
        val savedFile = if (existing != null) {
            val updated = treatmentProcedures.map {
                if (it.planId == planId && it.id == procedure.id) {
                    it.copy(
                        description = procedure.description,
                        tooth = procedure.tooth,
                        surface = procedure.surface,
                        quantity = procedure.quantity,
                        unitValue = procedure.unitValue,
                        totalValue = procedure.totalValue,
                        treatmentStage = procedure.treatmentStage,
                        status = procedure.status,
                    )
                } else it
            }
            treatmentProcedures.clear()
            treatmentProcedures.addAll(updated)
            existing
        } else {
            val newId = (treatmentProcedures.maxOfOrNull { it.id } ?: 0) + 1
            val file = TreatmentProcedureFile(
                id = newId,
                planId = planId,
                description = procedure.description,
                tooth = procedure.tooth,
                surface = procedure.surface,
                quantity = procedure.quantity,
                unitValue = procedure.unitValue,
                totalValue = procedure.totalValue,
                treatmentStage = procedure.treatmentStage,
                status = procedure.status,
            )
            treatmentProcedures.add(file)
            file
        }
        return TreatmentProcedure(
            id = savedFile.id,
            planId = planId,
            description = savedFile.description,
            tooth = savedFile.tooth,
            surface = savedFile.surface,
            quantity = savedFile.quantity,
            unitValue = savedFile.unitValue,
            totalValue = savedFile.totalValue,
            treatmentStage = savedFile.treatmentStage,
            status = savedFile.status,
            observations = procedure.observations,
        )
    }

    override suspend fun deleteTreatmentProcedure(planId: Long, procedureId: Long) {
        treatmentProcedures.removeAll { it.planId == planId && it.id == procedureId }
    }

    override suspend fun reorderTreatmentProcedures(planId: Long, procedureIds: List<Long>) {
        val procedures = treatmentProcedures.filter { it.planId == planId }
        val ids = procedures.map { it.id }.toSet()
        if (procedureIds.toSet() != ids) throw IllegalStateException("Invalid procedure list")
        procedureIds.mapIndexed { index, id ->
            val proc = procedures.find { it.id == id }!!
            val newFile = proc.copy(treatmentStage = index + 1)
            treatmentProcedures[treatmentProcedures.indexOfFirst { it.id == id }] = newFile
        }
    }

    override suspend fun listTreatmentBudgets(patientId: Long): List<TreatmentBudgetFile> =
        treatmentBudgets.filter { it.patientId == patientId }

    override suspend fun getTreatmentBudget(patientId: Long, id: Long): TreatmentBudget? =
        treatmentBudgets.find { it.patientId == patientId && it.id == id }
            ?.let { file ->
                TreatmentBudget(
                    id = file.id,
                    patientId = file.patientId,
                    planId = file.planId,
                    budgetNumber = file.budgetNumber,
                    status = file.status,
                    items = emptyList(),
                    discount = BigDecimal.ZERO,
                    paymentConditions = "",
                    validityDays = file.validityDays,
                    observations = "",
                    issueDate = file.issueDate,
                    approvedAt = null,
                    approvalObservations = null,
                    revision = file.revision,
                    originalId = file.originalId,
                )
            }

    override suspend fun saveTreatmentBudget(patientId: Long, budget: TreatmentBudget): TreatmentBudget {
        val existing = treatmentBudgets.find { it.patientId == patientId && it.id == budget.id }
        val savedFile = if (existing != null) {
            val updated = treatmentBudgets.map {
                if (it.patientId == patientId && it.id == budget.id) {
                    it.copy(
                        budgetNumber = budget.budgetNumber,
                        validityDays = budget.validityDays,
                    )
                } else it
            }
            treatmentBudgets.clear()
            treatmentBudgets.addAll(updated)
            existing
        } else {
            val newId = (treatmentBudgets.maxOfOrNull { it.id } ?: 0) + 1
            val file = TreatmentBudgetFile(
                id = newId,
                patientId = patientId,
                planId = budget.planId,
                budgetNumber = budget.budgetNumber,
                status = budget.status,
                revision = budget.revision,
                originalId = budget.originalId,
                issueDate = budget.issueDate,
                validityDays = budget.validityDays,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis(),
            )
            treatmentBudgets.add(file)
            file
        }
        return TreatmentBudget(
            id = savedFile.id,
            patientId = patientId,
            planId = savedFile.planId,
            budgetNumber = savedFile.budgetNumber,
            status = savedFile.status,
            items = budget.items,
            discount = budget.discount,
            paymentConditions = budget.paymentConditions,
            validityDays = savedFile.validityDays,
            observations = budget.observations,
            issueDate = savedFile.issueDate,
            approvedAt = budget.approvedAt,
            approvalObservations = budget.approvalObservations,
            revision = savedFile.revision,
            originalId = savedFile.originalId,
        )
    }

    override suspend fun cancelTreatmentBudget(patientId: Long, id: Long, reason: String) {
        val idx = treatmentBudgets.indexOfFirst { it.patientId == patientId && it.id == id }
        if (idx >= 0) {
            treatmentBudgets[idx] = treatmentBudgets[idx].copy(status = BudgetStatus.CANCELLED)
        }
    }

    override suspend fun createTreatmentBudgetRevision(patientId: Long, id: Long): TreatmentBudget {
        val existing = treatmentBudgets.find { it.patientId == patientId && it.id == id }
            ?: throw IllegalStateException("Budget not found")
        val newId = (treatmentBudgets.maxOfOrNull { it.id } ?: 0) + 1
        val newFile = TreatmentBudgetFile(
            id = newId,
            patientId = patientId,
            planId = existing.planId,
            budgetNumber = existing.budgetNumber,
            status = BudgetStatus.DRAFT,
            revision = existing.revision + 1,
            originalId = existing.originalId ?: id,
            issueDate = java.time.LocalDate.now(),
            validityDays = existing.validityDays,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
        )
        treatmentBudgets.add(newFile)
        return TreatmentBudget(
            id = newId,
            patientId = patientId,
            planId = newFile.planId,
            budgetNumber = newFile.budgetNumber,
            status = BudgetStatus.DRAFT,
            items = emptyList(),
            discount = BigDecimal.ZERO,
            paymentConditions = "",
            validityDays = newFile.validityDays,
            observations = "",
            issueDate = newFile.issueDate,
            approvedAt = null,
            approvalObservations = null,
            revision = newFile.revision,
            originalId = newFile.originalId,
        )
    }

    override suspend fun deleteTreatmentBudget(patientId: Long, id: Long) {
        treatmentBudgets.removeAll { it.patientId == patientId && it.id == id }
    }

    // --- Termos de consentimento (fake para testes) ---

    override suspend fun listInformedConsents(patientId: Long): List<InformedConsentFile> =
        informedConsents.filter { it.patientId == patientId }

    override suspend fun getInformedConsent(patientId: Long, id: Long): InformedConsent? =
        informedConsents.find { it.patientId == patientId && it.id == id }
            ?.let { file ->
                InformedConsent(
                    id = file.id,
                    patientId = file.patientId,
                    type = file.type,
                    status = file.status,
                    title = file.title,
                    content = "",
                    procedure = file.procedure,
                    objectives = "",
                    explanation = "",
                    benefits = "",
                    risks = "",
                    alternatives = "",
                    consequencesOfNotPerforming = "",
                    careInstructions = "",
                    limitations = "",
                    patientQuestions = "",
                    clarificationStatement = "",
                    consentProof = ConsentProof(),
                    legalRepresentative = null,
                    placeOfIssue = "",
                    issueDate = file.issueDate,
                    professionalId = null,
                    planId = file.planId,
                    procedureId = file.procedureId,
                    version = file.version,
                    originalId = file.originalId,
                    revokedReason = file.revokedReason,
                    revokedAt = file.revokedAt,
                )
            }

    override suspend fun saveInformedConsent(patientId: Long, consent: InformedConsent): InformedConsent {
        val existing = informedConsents.find { it.patientId == patientId && it.id == consent.id }
        val savedFile = if (existing != null) {
            val updated = informedConsents.map {
                if (it.patientId == patientId && it.id == consent.id) {
                    it.copy(
                        type = consent.type,
                        status = consent.status,
                        title = consent.title,
                        procedure = consent.procedure,
                        version = consent.version,
                        planId = consent.planId,
                        procedureId = consent.procedureId,
                        issueDate = consent.issueDate,
                    )
                } else it
            }
            informedConsents.clear()
            informedConsents.addAll(updated)
            existing
        } else {
            val newId = (informedConsents.maxOfOrNull { it.id } ?: 0) + 1
            val file = InformedConsentFile(
                id = newId,
                patientId = patientId,
                type = consent.type,
                status = consent.status,
                title = consent.title,
                procedure = consent.procedure,
                version = consent.version,
                originalId = consent.originalId,
                planId = consent.planId,
                procedureId = consent.procedureId,
                issueDate = consent.issueDate,
                createdAt = System.currentTimeMillis(),
            )
            informedConsents.add(file)
            file
        }
        saves++
        return InformedConsent(
            id = savedFile.id,
            patientId = patientId,
            type = savedFile.type,
            status = savedFile.status,
            title = savedFile.title,
            content = consent.content,
            procedure = savedFile.procedure,
            objectives = consent.objectives,
            explanation = consent.explanation,
            benefits = consent.benefits,
            risks = consent.risks,
            alternatives = consent.alternatives,
            consequencesOfNotPerforming = consent.consequencesOfNotPerforming,
            careInstructions = consent.careInstructions,
            limitations = consent.limitations,
            patientQuestions = consent.patientQuestions,
            clarificationStatement = consent.clarificationStatement,
            consentProof = consent.consentProof,
            legalRepresentative = consent.legalRepresentative,
            placeOfIssue = consent.placeOfIssue,
            issueDate = savedFile.issueDate,
            professionalId = consent.professionalId,
            planId = savedFile.planId,
            procedureId = savedFile.procedureId,
            version = savedFile.version,
            originalId = savedFile.originalId,
            revokedReason = savedFile.revokedReason,
            revokedAt = savedFile.revokedAt,
        )
    }

    override suspend fun deleteInformedConsent(patientId: Long, id: Long) {
        informedConsents.removeAll { it.patientId == patientId && it.id == id }
    }

    override suspend fun revokeInformedConsent(patientId: Long, id: Long, reason: String) {
        val idx = informedConsents.indexOfFirst { it.patientId == patientId && it.id == id }
        if (idx >= 0) {
            informedConsents[idx] = informedConsents[idx].copy(
                status = ConsentStatus.REVOKED,
                revokedReason = reason,
                revokedAt = LocalDate.now(),
            )
        }
    }

    override suspend fun createInformedConsentRevision(patientId: Long, id: Long): InformedConsent {
        val existing = informedConsents.find { it.patientId == patientId && it.id == id }
            ?: throw IllegalStateException("Consent not found")
        val newId = (informedConsents.maxOfOrNull { it.id } ?: 0) + 1
        val newFile = InformedConsentFile(
            id = newId,
            patientId = patientId,
            type = existing.type,
            status = ConsentStatus.DRAFT,
            title = existing.title,
            procedure = existing.procedure,
            version = existing.version + 1,
            originalId = existing.originalId ?: id,
            planId = existing.planId,
            procedureId = existing.procedureId,
            issueDate = existing.issueDate,
            createdAt = System.currentTimeMillis(),
        )
        informedConsents.add(newFile)
        return InformedConsent(
            id = newId,
            patientId = patientId,
            type = newFile.type,
            status = newFile.status,
            title = newFile.title,
            content = "",
            procedure = newFile.procedure,
            objectives = "",
            explanation = "",
            benefits = "",
            risks = "",
            alternatives = "",
            consequencesOfNotPerforming = "",
            careInstructions = "",
            limitations = "",
            patientQuestions = "",
            clarificationStatement = "",
            consentProof = ConsentProof(),
            legalRepresentative = null,
            placeOfIssue = "",
            issueDate = newFile.issueDate,
            professionalId = null,
            planId = newFile.planId,
            procedureId = newFile.procedureId,
            version = newFile.version,
            originalId = newFile.originalId,
            revokedReason = null,
            revokedAt = null,
        )
    }
}
