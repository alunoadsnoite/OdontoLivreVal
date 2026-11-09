package br.com.odontolivreval.core.demo

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import android.net.Uri
import android.provider.OpenableColumns
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.AtomicFile
import android.util.Base64
import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.math.BigDecimal
import java.nio.charset.StandardCharsets
import java.security.GeneralSecurityException
import java.security.KeyStore
import java.security.ProviderException
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import org.json.JSONObject
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.comparisons.compareBy
import org.json.JSONException

data class DemoPatient(val id: Long, val name: String)

data class DemoAppointment(
    val id: Long,
    val patientId: Long,
    val patientName: String,
    val day: LocalDate,
    val time: LocalTime,
    val cancelled: Boolean,
    val durationMinutes: Int = defaultAppointmentMinutes,
    val returnOfId: Long? = null,
)

enum class AppointmentSaveResult {
    SAVED, TIME_OCCUPIED, INVALID_DURATION, RETURN_BEFORE_SOURCE, RETURN_PATIENT_MISMATCH,
}

class ClinicStorageException(cause: Exception) : Exception(cause)

class DemoClinicStore(context: Context) : ClinicStore {
    private val app = context.applicationContext
    private val legacyFile = app.noBackupFilesDir.resolve("demo-patients.db")
    private val snapshotFile = AtomicFile(app.noBackupFilesDir.resolve("demo-clinic.enc"))
    private val mutex = Mutex()
    private val attachments = AttachmentStorage(app.noBackupFilesDir.resolve("clinic-attachments"))
    private var cleanupPending = true

    override suspend fun listPatients(): List<DemoPatient> = access { load().patients }

    override suspend fun savePatient(id: Long?, name: String): DemoPatient = access {
        val snapshot = load()
        val patient = DemoPatient(id ?: nextId(snapshot.patients.map { it.id }), name)
        if (id != null && snapshot.patients.none { it.id == id }) throw IOException("Patient missing")
        persist(snapshot.copy(patients = snapshot.patients.filterNot { it.id == id } + patient))
        patient
    }

    override suspend fun listAppointments(day: LocalDate): List<DemoAppointment> = access {
        val snapshot = load()
        val patients = snapshot.patients.associateBy { it.id }
        snapshot.appointments.filter { it.day == day.toEpochDay() }
            .sortedWith(compareBy(AppointmentRecord::minute, AppointmentRecord::id))
            .map {
                DemoAppointment(
                    it.id, it.patientId, patients.getValue(it.patientId).name, day,
                    LocalTime.of(it.minute / 60, it.minute % 60), it.cancelled, it.durationMinutes, it.returnOfId,
                )
            }
    }

    override suspend fun countActiveAppointments(month: YearMonth): Map<LocalDate, Int> = access {
        val days = month.atDay(1).toEpochDay()..month.atEndOfMonth().toEpochDay()
        load().appointments.asSequence()
            .filter { !it.cancelled && it.day in days }
            .groupingBy { LocalDate.ofEpochDay(it.day) }
            .eachCount()
    }

    override suspend fun saveAppointment(
        id: Long?,
        patientId: Long,
        day: LocalDate,
        time: LocalTime,
        durationMinutes: Int,
        returnOfId: Long?,
    ): AppointmentSaveResult = access {
        val snapshot = load()
        val minute = time.hour * 60 + time.minute
        val epochDay = day.toEpochDay()
        val end = minute + durationMinutes
        if (durationMinutes !in 1..maxAppointmentMinutes || end > 1440) {
            return@access AppointmentSaveResult.INVALID_DURATION
        }
        requirePatient(snapshot, patientId)
        val existing = id?.let { target ->
            snapshot.appointments.find { it.id == target && !it.cancelled }
                ?: throw IOException("Active appointment missing")
        }
        val sourceId = if (existing == null) returnOfId else existing.returnOfId
        val source = sourceId?.let { target ->
            snapshot.appointments.find { it.id == target }
                ?: throw IOException("Return source missing")
        }
        if ((source != null && source.patientId != patientId) ||
            (id != null && snapshot.appointments.any { it.returnOfId == id && it.patientId != patientId })
        ) return@access AppointmentSaveResult.RETURN_PATIENT_MISMATCH

        val moved = existing == null || existing.day != epochDay || existing.minute != minute ||
            existing.durationMinutes != durationMinutes
        if (moved && source != null &&
            (epochDay < source.day || (epochDay == source.day && minute < source.endMinute))
        ) return@access AppointmentSaveResult.RETURN_BEFORE_SOURCE

        if (moved && id != null && snapshot.appointments.any {
                it.returnOfId == id && !it.cancelled &&
                    (it.day < epochDay || (it.day == epochDay && it.minute < end))
            }
        ) return@access AppointmentSaveResult.RETURN_BEFORE_SOURCE

        if (moved && snapshot.appointments.any {
                it.day == epochDay && !it.cancelled && it.id != id && it.overlaps(minute, end)
            }
        ) return@access AppointmentSaveResult.TIME_OCCUPIED

        val record = AppointmentRecord(
            id ?: nextId(snapshot.appointments.map { it.id }), patientId, epochDay, minute, false,
            durationMinutes, sourceId,
        )
        persist(snapshot.copy(appointments = snapshot.appointments.filterNot { it.id == id } + record))
        AppointmentSaveResult.SAVED
    }

    override suspend fun cancelAppointment(id: Long) = access {
        val snapshot = load()
        if (snapshot.appointments.none { it.id == id }) throw IOException("Appointment missing")
        persist(snapshot.copy(appointments = snapshot.appointments.map {
            if (it.id == id) it.copy(cancelled = true) else it
        }))
    }

    override suspend fun listUpcomingAppointments(from: LocalDate, days: Int): List<DemoAppointment> = access {
        if (days !in 1..MAX_UPCOMING_DAYS) throw IOException("Invalid upcoming window")
        val snapshot = load()
        val patients = snapshot.patients.associateBy { it.id }
        val start = from.toEpochDay()
        snapshot.appointments.filter { !it.cancelled && it.day in start until start + days }
            .sortedWith(
                compareBy<AppointmentRecord> { it.day }
                    .thenBy { it.minute }
                    .thenBy { it.id },
            )
            .map { appointment ->
                appointment.toDemo(
                    LocalDate.ofEpochDay(appointment.day),
                    patients.getValue(appointment.patientId).name,
                )
            }
    }

    override suspend fun getProfessionalProfile(): ProfessionalProfile = access {
        load().professional
    }

    override suspend fun saveProfessionalProfile(profile: ProfessionalProfile): ProfessionalProfile = access {
        val saved = profile.copy(
            name = profile.name.trim(),
            cro = profile.cro.trim(),
            specialty = profile.specialty.trim(),
            clinicName = profile.clinicName.trim(),
            address = profile.address.trim(),
            phone = profile.phone.trim(),
        )
        val snapshot = load()
        persist(snapshot.copy(professional = saved))
        saved
    }

    override suspend fun getPatientRecord(patientId: Long): PatientRecord? = access {
        load().patientRecords.find { it.patientId == patientId }
    }

    override suspend fun savePatientRecord(record: PatientRecord): PatientRecord = access {
        val snapshot = load()
        requirePatient(snapshot, record.patientId)
        val saved = withRecordId(snapshot, record)
        persist(snapshot.copy(patientRecords = replaceRecord(snapshot, saved)))
        saved
    }

    override suspend fun saveRecordAndVisit(record: PatientRecord, visit: VisitRecord): SavedRecord = access {
        val snapshot = load()
        requirePatient(snapshot, record.patientId)
        if (visit.patientId != record.patientId) throw IOException("Visit and record belong to different patients")
        val savedRecord = withRecordId(snapshot, record)
        val savedVisit = withVisitId(snapshot, visit)
        persist(
            snapshot.copy(
                patientRecords = replaceRecord(snapshot, savedRecord),
                visits = snapshot.visits.filterNot { it.id == savedVisit.id } + savedVisit,
            ),
        )
        SavedRecord(savedRecord, savedVisit)
    }

    override suspend fun deletePatient(id: Long) = access {
        val snapshot = load()
        if (snapshot.patients.none { it.id == id }) throw IOException("Patient missing")
        // Backup automático antes de operação destrutiva para permitir recuperação
        val backupFile = autoBackup()
        try {
            persist(snapshot.copy(
                patients = snapshot.patients.filterNot { it.id == id },
                appointments = snapshot.appointments.filter { it.patientId != id },
                patientRecords = snapshot.patientRecords.filterNot { it.patientId == id },
                visits = snapshot.visits.filterNot { it.patientId == id },
                radiographs = snapshot.radiographs.filterNot { it.file.patientId == id },
                prescriptions = snapshot.prescriptions.filterNot { it.file.patientId == id },
                documentPhotos = snapshot.documentPhotos.filterNot { it.file.patientId == id },
                digitalSignatures = snapshot.digitalSignatures.filterNot { it.file.patientId == id },
                medicalCertificates = snapshot.medicalCertificates.filterNot { it.file.patientId == id },
            ))
        } catch (error: Exception) {
            backupFile.deleteRecursively()
            throw error
        }
        backupFile.deleteRecursively()
        Unit
    }

    override suspend fun listMedicalCertificates(patientId: Long): List<MedicalCertificateFile> = access {
        load().medicalCertificates.map { it.file }.filter { it.patientId == patientId }
    }

    override suspend fun getMedicalCertificate(patientId: Long, id: Long): MedicalCertificate? = access {
        val stored = load().medicalCertificates.find { it.file.patientId == patientId && it.file.id == id }
            ?: return@access null
        val file = stored.file
        val content = try {
            readAttachment(stored.content)
        } catch (_: IOException) {
            return@access MedicalCertificate(
                id = file.id, patientId = file.patientId, type = file.type, status = file.status,
                title = file.title, content = "", serviceDate = file.serviceDate,
                leaveDays = file.leaveDays, version = file.version, originalId = file.originalId,
                placeOfIssue = "", issueDate = LocalDate.now(),
            )
        }
        val text = String(content, StandardCharsets.UTF_8)
        MedicalCertificate(
            id = file.id, patientId = file.patientId, type = file.type, status = file.status,
            title = file.title, content = text, serviceDate = file.serviceDate,
            leaveDays = file.leaveDays, version = file.version, originalId = file.originalId,
            placeOfIssue = "", issueDate = LocalDate.now(),
        )
    }

    override suspend fun saveMedicalCertificate(patientId: Long, certificate: MedicalCertificate): MedicalCertificate = access {
        val snapshot = load()
        requirePatient(snapshot, patientId)
        val existing = snapshot.medicalCertificates.find { it.file.patientId == patientId && it.file.id == certificate.id }
        val now = System.currentTimeMillis()
        val savedFile = when {
            existing != null -> {
                // Atualiza rascunho existente ou cria nova versão de finalizado
                val file = existing.file.copy(
                    type = certificate.type,
                    status = certificate.status,
                    title = certificate.title,
                    serviceDate = certificate.serviceDate,
                    leaveDays = certificate.leaveDays,
                    version = if (certificate.status == CertificateStatus.FINALIZED && existing.file.status == CertificateStatus.DRAFT)
                        existing.file.version + 1 else existing.file.version,
                    originalId = if (certificate.status == CertificateStatus.FINALIZED && existing.file.status == CertificateStatus.DRAFT)
                        existing.file.id else existing.file.originalId,
                )
                MedicalCertificateFile(
                    id = file.id, patientId = patientId, type = file.type, status = file.status,
                    title = file.title, serviceDate = file.serviceDate, leaveDays = file.leaveDays,
                    version = file.version, originalId = file.originalId, createdAt = now,
                )
            }
            else -> {
                val newId = nextId(snapshot.medicalCertificates.map { it.file.id })
                MedicalCertificateFile(
                    id = newId, patientId = patientId, type = certificate.type,
                    status = certificate.status, title = certificate.title,
                    serviceDate = certificate.serviceDate, leaveDays = certificate.leaveDays,
                    version = 1, originalId = null, createdAt = now,
                )
            }
        }
        val bytes = certificate.content.toByteArray(StandardCharsets.UTF_8)
        val content = writeNewAttachment(snapshot, bytes)
        persist(snapshot.copy(
            medicalCertificates = snapshot.medicalCertificates.filterNot {
                it.file.patientId == patientId && it.file.id == savedFile.id
            } + StoredMedicalCertificate(savedFile, content),
        ))
        MedicalCertificate(
            id = savedFile.id, patientId = patientId, type = savedFile.type, status = savedFile.status,
            title = savedFile.title, content = certificate.content, serviceDate = savedFile.serviceDate,
            leaveDays = savedFile.leaveDays, version = savedFile.version, originalId = savedFile.originalId,
            placeOfIssue = certificate.placeOfIssue, issueDate = savedFile.serviceDate,
        )
    }

    override suspend fun cancelMedicalCertificate(patientId: Long, id: Long, reason: String) = access {
        val snapshot = load()
        val existing = snapshot.medicalCertificates.find { it.file.patientId == patientId && it.file.id == id }
            ?: throw IOException("Certificate missing")
        if (existing.file.status != CertificateStatus.FINALIZED) throw IOException("Only finalized certificates can be cancelled")
        persist(snapshot.copy(
            medicalCertificates = snapshot.medicalCertificates.map {
                if (it.file.patientId == patientId && it.file.id == id) {
                    it.copy(file = it.file.copy(
                        status = CertificateStatus.CANCELLED,
                        cancelledReason = reason,
                        cancelledAt = LocalDate.now(),
                    ))
                } else it
            },
        ))
    }

    override suspend fun createMedicalCertificateVersion(patientId: Long, id: Long): MedicalCertificate = access {
        val snapshot = load()
        val existing = snapshot.medicalCertificates.find { it.file.patientId == patientId && it.file.id == id }
            ?: throw IOException("Certificate missing")
        if (existing.file.status != CertificateStatus.FINALIZED) throw IOException("Only finalized certificates can have a new version")
        val newFile = MedicalCertificateFile(
            id = nextId(snapshot.medicalCertificates.map { it.file.id }),
            patientId = patientId,
            type = existing.file.type,
            status = CertificateStatus.DRAFT,
            title = existing.file.title,
            serviceDate = existing.file.serviceDate,
            leaveDays = existing.file.leaveDays,
            version = existing.file.version + 1,
            originalId = existing.file.originalId ?: id,
            createdAt = System.currentTimeMillis(),
        )
        val existingStored = snapshot.medicalCertificates.find { it.file.patientId == patientId && it.file.id == id }
            ?: throw IOException("Certificate content missing")
        val existingContent = try {
            String(readAttachment(existingStored.content), StandardCharsets.UTF_8)
        } catch (_: IOException) {
            ""
        }
        persist(snapshot.copy(
            medicalCertificates = snapshot.medicalCertificates + StoredMedicalCertificate(
                newFile,
                writeNewAttachment(snapshot, existingContent.toByteArray(StandardCharsets.UTF_8)),
            ),
        ))
        MedicalCertificate(
            id = newFile.id, patientId = patientId, type = newFile.type, status = newFile.status,
            title = newFile.title, content = existingContent, serviceDate = newFile.serviceDate,
            leaveDays = newFile.leaveDays, version = newFile.version, originalId = newFile.originalId,
            placeOfIssue = "", issueDate = newFile.serviceDate,
        )
    }

    override suspend fun deleteMedicalCertificate(patientId: Long, id: Long) = access {
        val snapshot = load()
        val existing = snapshot.medicalCertificates.find { it.file.patientId == patientId && it.file.id == id }
            ?: throw IOException("Certificate missing")
        if (existing.file.status == CertificateStatus.FINALIZED) throw IOException("Cannot delete finalized certificate")
        persist(snapshot.copy(
            medicalCertificates = snapshot.medicalCertificates.filterNot {
                it.file.patientId == patientId && it.file.id == id
            },
        ))
    }

    // --- Planos de tratamento ---

    override suspend fun listTreatmentPlans(patientId: Long): List<TreatmentPlanFile> = access {
        load().treatmentPlans.map { it.file }.filter { it.patientId == patientId }
    }

    override suspend fun getTreatmentPlan(patientId: Long, id: Long): TreatmentPlan? = access {
        val stored = load().treatmentPlans.find { it.file.patientId == patientId && it.file.id == id }
            ?: return@access null
        val file = stored.file
        val content = try {
            String(readAttachment(stored.content), StandardCharsets.UTF_8)
        } catch (_: IOException) {
            ""
        }
        val obj = org.json.JSONObject(content)
        TreatmentPlan(
            id = file.id,
            patientId = file.patientId,
            title = file.title,
            description = obj.optString("description", ""),
            clinicalObjectives = obj.optString("clinicalObjectives", ""),
            estimatedSessions = obj.optInt("estimatedSessions", 1),
            startDate = if (obj.isNull("startDate")) null else LocalDate.ofEpochDay(obj.optLong("startDate")),
            observations = obj.optString("observations", ""),
            status = obj.optString("status", "active"),
            createdAt = file.createdAt,
            updatedAt = file.updatedAt,
        )
    }

    override suspend fun saveTreatmentPlan(patientId: Long, plan: TreatmentPlan): TreatmentPlan = access {
        val snapshot = load()
        requirePatient(snapshot, patientId)
        val now = System.currentTimeMillis()
        val existing = snapshot.treatmentPlans.find { it.file.patientId == patientId && it.file.id == plan.id }
        val savedFile = when {
            existing != null -> {
                val updatedFile = existing.file.copy(
                    title = plan.title,
                    description = plan.description,
                    estimatedSessions = plan.estimatedSessions,
                    status = plan.status,
                    updatedAt = now,
                )
                val updated = snapshot.treatmentPlans.map {
                    if (it.file.patientId == patientId && it.file.id == plan.id) {
                        it.copy(file = updatedFile)
                    } else it
                }
                persist(snapshot.copy(treatmentPlans = updated))
                updatedFile
            }
            else -> {
                val newId = nextId(snapshot.treatmentPlans.map { it.file.id })
                val newFile = TreatmentPlanFile(
                    id = newId,
                    patientId = patientId,
                    title = plan.title,
                    description = plan.description,
                    estimatedSessions = plan.estimatedSessions,
                    status = plan.status,
                    createdAt = now,
                    updatedAt = now,
                )
                val bytes = plan.toJsonBytes()
                val content = writeNewAttachment(snapshot, bytes)
                persist(snapshot.copy(
                    treatmentPlans = snapshot.treatmentPlans + StoredTreatmentPlan(newFile, content),
                ))
                newFile
            }
        }
        TreatmentPlan(
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

    override suspend fun deleteTreatmentPlan(patientId: Long, id: Long) = access {
        val snapshot = load()
        val existing = snapshot.treatmentPlans.find { it.file.patientId == patientId && it.file.id == id }
            ?: throw IOException("Treatment plan missing")
        // Remove procedimentos e orçamentos vinculados
        persist(snapshot.copy(
            treatmentPlans = snapshot.treatmentPlans.filterNot {
                it.file.patientId == patientId && it.file.id == id
            },
            treatmentProcedures = snapshot.treatmentProcedures.filterNot {
                it.file.planId == id
            },
            treatmentBudgets = snapshot.treatmentBudgets.filterNot {
                it.file.planId == id
            },
        ))
    }

    // --- Procedimentos ---

    override suspend fun listTreatmentProcedures(planId: Long): List<TreatmentProcedureFile> = access {
        load().treatmentProcedures.map { it.file }.filter { it.planId == planId }
            .sortedWith(compareBy({ it.treatmentStage }, { it.id }))
    }

    override suspend fun saveTreatmentProcedure(planId: Long, procedure: TreatmentProcedure): TreatmentProcedure = access {
        val snapshot = load()
        val planExists = snapshot.treatmentPlans.any { it.file.id == planId }
        if (!planExists) throw IOException("Treatment plan missing")
        val now = System.currentTimeMillis()
        val existing = snapshot.treatmentProcedures.find { it.file.id == procedure.id && it.file.planId == planId }
        val savedFile = when {
            existing != null -> {
                val updatedFile = existing.file.copy(
                    description = procedure.description,
                    tooth = procedure.tooth,
                    surface = procedure.surface,
                    quantity = procedure.quantity,
                    unitValue = procedure.unitValue,
                    totalValue = procedure.totalValue,
                    treatmentStage = procedure.treatmentStage,
                    status = procedure.status,
                )
                val updated = snapshot.treatmentProcedures.map {
                    if (it.file.id == procedure.id && it.file.planId == planId) {
                        it.copy(file = updatedFile)
                    } else it
                }
                persist(snapshot.copy(treatmentProcedures = updated))
                updatedFile
            }
            else -> {
                val newId = nextId(snapshot.treatmentProcedures.map { it.file.id })
                val newFile = TreatmentProcedureFile(
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
                val bytes = procedure.toJsonBytes()
                val content = writeNewAttachment(snapshot, bytes)
                persist(snapshot.copy(
                    treatmentProcedures = snapshot.treatmentProcedures + StoredTreatmentProcedure(newFile, content),
                ))
                newFile
            }
        }
        TreatmentProcedure(
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

    override suspend fun deleteTreatmentProcedure(planId: Long, procedureId: Long) = access {
        val snapshot = load()
        val existing = snapshot.treatmentProcedures.find {
            it.file.planId == planId && it.file.id == procedureId
        } ?: throw IOException("Procedure missing")
        persist(snapshot.copy(
            treatmentProcedures = snapshot.treatmentProcedures.filterNot {
                it.file.planId == planId && it.file.id == procedureId
            },
        ))
    }

    override suspend fun reorderTreatmentProcedures(planId: Long, procedureIds: List<Long>) = access {
        val snapshot = load()
        val procedures = snapshot.treatmentProcedures.filter { it.file.planId == planId }
        val ids = procedures.map { it.file.id }.toSet()
        if (procedureIds.toSet() != ids) throw IOException("Invalid procedure list")
        val updated = procedureIds.mapIndexed { index, id ->
            val proc = procedures.find { it.file.id == id }!!
            val newFile = proc.file.copy(treatmentStage = index + 1)
            StoredTreatmentProcedure(newFile, proc.content)
        }
        persist(snapshot.copy(
            treatmentProcedures = snapshot.treatmentProcedures.map {
                if (it.file.planId == planId) {
                    updated.find { it.file.id == it.file.id }!!
                } else it
            },
        ))
    }

    // --- Orçamentos ---

    override suspend fun listTreatmentBudgets(patientId: Long): List<TreatmentBudgetFile> = access {
        load().treatmentBudgets.map { it.file }.filter { it.patientId == patientId }
            .sortedWith(compareBy({ -it.createdAt }, { -it.id }))
    }

    override suspend fun getTreatmentBudget(patientId: Long, id: Long): TreatmentBudget? = access {
        val stored = load().treatmentBudgets.find { it.file.patientId == patientId && it.file.id == id }
            ?: return@access null
        val file = stored.file
        val content = try {
            String(readAttachment(stored.content), StandardCharsets.UTF_8)
        } catch (_: IOException) {
            ""
        }
        val obj = org.json.JSONObject(content)
        val itemsArray = obj.getJSONArray("items")
        val items = (0 until itemsArray.length()).map { i ->
            val row = itemsArray.getJSONArray(i)
            BudgetItem(
                procedureId = row.getLong(0),
                description = row.getString(1),
                tooth = if (row.isNull(2)) null else row.getString(2),
                quantity = row.getInt(3).toInt(),
                unitValue = BigDecimal(row.getString(4)),
                totalValue = BigDecimal(row.getString(5)),
            )
        }
        TreatmentBudget(
            id = file.id,
            patientId = file.patientId,
            planId = file.planId,
            budgetNumber = file.budgetNumber,
            status = file.status,
            items = items,
            discount = BigDecimal(obj.optString("discount", "0")),
            paymentConditions = obj.optString("paymentConditions", ""),
            validityDays = obj.optInt("validityDays", 30),
            observations = obj.optString("observations", ""),
            issueDate = file.issueDate,
            approvedAt = if (obj.isNull("approvedAt")) null else LocalDate.ofEpochDay(obj.optLong("approvedAt")),
            approvalObservations = if (obj.isNull("approvalObservations")) null else obj.optString("approvalObservations"),
            revision = file.revision,
            originalId = file.originalId,
        )
    }

    override suspend fun saveTreatmentBudget(patientId: Long, budget: TreatmentBudget): TreatmentBudget = access {
        val snapshot = load()
        requirePatient(snapshot, patientId)
        val now = System.currentTimeMillis()
        val existing = snapshot.treatmentBudgets.find { it.file.patientId == patientId && it.file.id == budget.id }
        val savedFile = when {
            existing != null -> {
                // Só permite editar se estiver em rascunho
                if (existing.file.status != BudgetStatus.DRAFT) throw IOException("Only draft budgets can be edited")
                val updatedFile = existing.file.copy(
                    budgetNumber = budget.budgetNumber,
                    validityDays = budget.validityDays,
                    updatedAt = now,
                )
                val bytes = budget.toJsonBytes()
                val content = writeNewAttachment(snapshot, bytes)
                val updated = snapshot.treatmentBudgets.map {
                    if (it.file.patientId == patientId && it.file.id == budget.id) {
                        it.copy(file = updatedFile, content = content)
                    } else it
                }
                persist(snapshot.copy(treatmentBudgets = updated))
                updatedFile
            }
            else -> {
                val newId = nextId(snapshot.treatmentBudgets.map { it.file.id })
                val newFile = TreatmentBudgetFile(
                    id = newId,
                    patientId = patientId,
                    planId = budget.planId,
                    budgetNumber = budget.budgetNumber,
                    status = budget.status,
                    revision = budget.revision,
                    originalId = budget.originalId,
                    issueDate = budget.issueDate,
                    validityDays = budget.validityDays,
                    createdAt = now,
                    updatedAt = now,
                )
                val bytes = budget.toJsonBytes()
                val content = writeNewAttachment(snapshot, bytes)
                persist(snapshot.copy(
                    treatmentBudgets = snapshot.treatmentBudgets + StoredTreatmentBudget(newFile, content),
                ))
                newFile
            }
        }
        TreatmentBudget(
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

    override suspend fun cancelTreatmentBudget(patientId: Long, id: Long, reason: String) = access {
        val snapshot = load()
        val existing = snapshot.treatmentBudgets.find { it.file.patientId == patientId && it.file.id == id }
            ?: throw IOException("Budget missing")
        if (existing.file.status == BudgetStatus.APPROVED) throw IOException("Cannot cancel approved budget")
        persist(snapshot.copy(
            treatmentBudgets = snapshot.treatmentBudgets.map {
                if (it.file.patientId == patientId && it.file.id == id) {
                    it.copy(file = it.file.copy(status = BudgetStatus.CANCELLED))
                } else it
            },
        ))
    }

    override suspend fun createTreatmentBudgetRevision(patientId: Long, id: Long): TreatmentBudget = access {
        val snapshot = load()
        val existing = snapshot.treatmentBudgets.find { it.file.patientId == patientId && it.file.id == id }
            ?: throw IOException("Budget missing")
        if (existing.file.status != BudgetStatus.PRESENTED && existing.file.status != BudgetStatus.REJECTED) {
            throw IOException("Only presented or rejected budgets can have a new revision")
        }
        val content = try {
            String(readAttachment(existing.content), StandardCharsets.UTF_8)
        } catch (_: IOException) {
            ""
        }
        val obj = org.json.JSONObject(content)
        val itemsArray = obj.getJSONArray("items")
        val items = (0 until itemsArray.length()).map { i ->
            val row = itemsArray.getJSONArray(i)
            BudgetItem(
                procedureId = row.getLong(0),
                description = row.getString(1),
                tooth = if (row.isNull(2)) null else row.getString(2),
                quantity = row.getInt(3).toInt(),
                unitValue = BigDecimal(row.getString(4)),
                totalValue = BigDecimal(row.getString(5)),
            )
        }
        val current = TreatmentBudget(
            id = existing.file.id,
            patientId = patientId,
            planId = existing.file.planId,
            budgetNumber = existing.file.budgetNumber,
            status = existing.file.status,
            items = items,
            discount = BigDecimal(obj.optString("discount", "0")),
            paymentConditions = obj.optString("paymentConditions", ""),
            validityDays = existing.file.validityDays,
            observations = obj.optString("observations", ""),
            issueDate = existing.file.issueDate,
            approvedAt = if (obj.isNull("approvedAt")) null else LocalDate.ofEpochDay(obj.optLong("approvedAt")),
            approvalObservations = if (obj.isNull("approvalObservations")) null else obj.optString("approvalObservations"),
            revision = existing.file.revision,
            originalId = existing.file.originalId,
        )
        val newFile = TreatmentBudgetFile(
            id = nextId(snapshot.treatmentBudgets.map { it.file.id }),
            patientId = patientId,
            planId = existing.file.planId,
            budgetNumber = existing.file.budgetNumber,
            status = BudgetStatus.DRAFT,
            revision = existing.file.revision + 1,
            originalId = existing.file.originalId ?: id,
            issueDate = LocalDate.now(),
            validityDays = existing.file.validityDays,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
        )
        val bytes = current.toJsonBytes()
        val contentRef = writeNewAttachment(snapshot, bytes)
        persist(snapshot.copy(
            treatmentBudgets = snapshot.treatmentBudgets + StoredTreatmentBudget(newFile, contentRef),
        ))
        current.copy(
            id = newFile.id,
            status = BudgetStatus.DRAFT,
            revision = newFile.revision,
            originalId = newFile.originalId,
        )
    }

    override suspend fun deleteTreatmentBudget(patientId: Long, id: Long) = access {
        val snapshot = load()
        val existing = snapshot.treatmentBudgets.find { it.file.patientId == patientId && it.file.id == id }
            ?: throw IOException("Budget missing")
        if (existing.file.status == BudgetStatus.APPROVED) throw IOException("Cannot delete approved budget")
        persist(snapshot.copy(
            treatmentBudgets = snapshot.treatmentBudgets.filterNot {
                it.file.patientId == patientId && it.file.id == id
            },
        ))
    }

    // --- Termos de consentimento (TCLE) ---

    override suspend fun listInformedConsents(patientId: Long): List<InformedConsentFile> = access {
        load().informedConsents.map { it.file }.filter { it.patientId == patientId }
    }

    override suspend fun getInformedConsent(patientId: Long, id: Long): InformedConsent? = access {
        val stored = load().informedConsents.find { it.file.patientId == patientId && it.file.id == id }
            ?: return@access null
        val file = stored.file
        val content = try {
            String(readAttachment(stored.content), StandardCharsets.UTF_8)
        } catch (_: IOException) {
            ""
        }
        val obj = org.json.JSONObject(content)
        val proofObj = obj.optJSONObject("consentProof")
        val proof = ConsentProof(
            patientDecision = proofObj?.optString("patientDecision") ?: null,
            recordedAt = if (proofObj?.isNull("recordedAt") == false)
                LocalDate.ofEpochDay(proofObj.optLong("recordedAt")) else null,
            proofMethod = proofObj?.optString("proofMethod") ?: null,
            hasSignedDocument = proofObj?.optBoolean("hasSignedDocument") ?: false,
        )
        val repObj = obj.optJSONObject("legalRepresentative")
        val representative = if (repObj != null) {
            LegalRepresentative(
                name = repObj.optString("name", ""),
                relationship = repObj.optString("relationship", ""),
                identification = repObj.optString("identification", ""),
            )
        } else null
        InformedConsent(
            id = file.id,
            patientId = file.patientId,
            type = file.type,
            status = file.status,
            title = file.title,
            content = obj.optString("content", ""),
            procedure = obj.optString("procedure", ""),
            objectives = obj.optString("objectives", ""),
            explanation = obj.optString("explanation", ""),
            benefits = obj.optString("benefits", ""),
            risks = obj.optString("risks", ""),
            alternatives = obj.optString("alternatives", ""),
            consequencesOfNotPerforming = obj.optString("consequencesOfNotPerforming", ""),
            careInstructions = obj.optString("careInstructions", ""),
            limitations = obj.optString("limitations", ""),
            patientQuestions = obj.optString("patientQuestions", ""),
            clarificationStatement = obj.optString("clarificationStatement", ""),
            consentProof = proof,
            legalRepresentative = representative,
            placeOfIssue = obj.optString("placeOfIssue", ""),
            issueDate = if (obj.isNull("issueDate")) file.issueDate
            else LocalDate.ofEpochDay(obj.optLong("issueDate")),
            professionalId = if (obj.isNull("professionalId")) null else obj.optLong("professionalId"),
            planId = file.planId,
            procedureId = file.procedureId,
            version = file.version,
            originalId = file.originalId,
            revokedReason = file.revokedReason,
            revokedAt = file.revokedAt,
        )
    }

    override suspend fun saveInformedConsent(patientId: Long, consent: InformedConsent): InformedConsent = access {
        val snapshot = load()
        requirePatient(snapshot, patientId)
        val now = System.currentTimeMillis()
        val existing = snapshot.informedConsents.find { it.file.patientId == patientId && it.file.id == consent.id }
        val savedFile = when {
            existing != null -> {
                val updatedFile = existing.file.copy(
                    type = consent.type,
                    status = consent.status,
                    title = consent.title,
                    procedure = consent.procedure,
                    version = if (consent.status == ConsentStatus.DRAFT && existing.file.status != ConsentStatus.DRAFT)
                        existing.file.version else existing.file.version,
                    planId = consent.planId,
                    procedureId = consent.procedureId,
                )
                val updated = snapshot.informedConsents.map {
                    if (it.file.patientId == patientId && it.file.id == consent.id) {
                        it.copy(file = updatedFile)
                    } else it
                }
                persist(snapshot.copy(informedConsents = updated))
                updatedFile
            }
            else -> {
                val newId = nextId(snapshot.informedConsents.map { it.file.id })
                val newFile = InformedConsentFile(
                    id = newId,
                    patientId = patientId,
                    type = consent.type,
                    status = consent.status,
                    title = consent.title,
                    procedure = consent.procedure,
                    version = 1,
                    originalId = null,
                    planId = consent.planId,
                    procedureId = consent.procedureId,
                    issueDate = consent.issueDate,
                    createdAt = now,
                )
                val bytes = consent.toJsonBytes()
                val content = writeNewAttachment(snapshot, bytes)
                persist(snapshot.copy(
                    informedConsents = snapshot.informedConsents + StoredInformedConsent(newFile, content),
                ))
                newFile
            }
        }
        InformedConsent(
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
            issueDate = savedFile.issueDate ?: consent.issueDate,
            professionalId = consent.professionalId,
            planId = savedFile.planId,
            procedureId = savedFile.procedureId,
            version = savedFile.version,
            originalId = savedFile.originalId,
            revokedReason = savedFile.revokedReason,
            revokedAt = savedFile.revokedAt,
        )
    }

    override suspend fun revokeInformedConsent(patientId: Long, id: Long, reason: String) = access {
        val snapshot = load()
        val existing = snapshot.informedConsents.find { it.file.patientId == patientId && it.file.id == id }
            ?: throw IOException("Consent missing")
        persist(snapshot.copy(
            informedConsents = snapshot.informedConsents.map {
                if (it.file.patientId == patientId && it.file.id == id) {
                    it.copy(file = it.file.copy(
                        status = ConsentStatus.REVOKED,
                        revokedReason = reason,
                        revokedAt = LocalDate.now(),
                    ))
                } else it
            },
        ))
    }

    override suspend fun createInformedConsentRevision(patientId: Long, id: Long): InformedConsent = access {
        val snapshot = load()
        val existing = snapshot.informedConsents.find { it.file.patientId == patientId && it.file.id == id }
            ?: throw IOException("Consent missing")
        if (existing.file.status != ConsentStatus.ACCEPTED &&
            existing.file.status != ConsentStatus.REJECTED &&
            existing.file.status != ConsentStatus.REVOKED) {
            throw IOException("Only accepted, rejected or revoked consents can have a new revision")
        }
        val content = try {
            String(readAttachment(existing.content), StandardCharsets.UTF_8)
        } catch (_: IOException) {
            ""
        }
        val obj = org.json.JSONObject(content)
        val proofObj = obj.optJSONObject("consentProof")
        val proof = ConsentProof(
            patientDecision = proofObj?.optString("patientDecision") ?: null,
            recordedAt = if (proofObj?.isNull("recordedAt") == false)
                LocalDate.ofEpochDay(proofObj.optLong("recordedAt")) else null,
            proofMethod = proofObj?.optString("proofMethod") ?: null,
            hasSignedDocument = proofObj?.optBoolean("hasSignedDocument") ?: false,
        )
        val repObj = obj.optJSONObject("legalRepresentative")
        val representative = if (repObj != null) {
            LegalRepresentative(
                name = repObj.optString("name", ""),
                relationship = repObj.optString("relationship", ""),
                identification = repObj.optString("identification", ""),
            )
        } else null
        val current = InformedConsent(
            id = existing.file.id,
            patientId = patientId,
            type = existing.file.type,
            status = existing.file.status,
            title = existing.file.title,
            content = obj.optString("content", ""),
            procedure = obj.optString("procedure", ""),
            objectives = obj.optString("objectives", ""),
            explanation = obj.optString("explanation", ""),
            benefits = obj.optString("benefits", ""),
            risks = obj.optString("risks", ""),
            alternatives = obj.optString("alternatives", ""),
            consequencesOfNotPerforming = obj.optString("consequencesOfNotPerforming", ""),
            careInstructions = obj.optString("careInstructions", ""),
            limitations = obj.optString("limitations", ""),
            patientQuestions = obj.optString("patientQuestions", ""),
            clarificationStatement = obj.optString("clarificationStatement", ""),
            consentProof = proof,
            legalRepresentative = representative,
            placeOfIssue = obj.optString("placeOfIssue", ""),
            issueDate = if (obj.isNull("issueDate")) existing.file.issueDate
            else LocalDate.ofEpochDay(obj.optLong("issueDate")),
            professionalId = if (obj.isNull("professionalId")) null else obj.optLong("professionalId"),
            planId = existing.file.planId,
            procedureId = existing.file.procedureId,
            version = existing.file.version,
            originalId = existing.file.originalId,
            revokedReason = existing.file.revokedReason,
            revokedAt = existing.file.revokedAt,
        )
        val newFile = InformedConsentFile(
            id = nextId(snapshot.informedConsents.map { it.file.id }),
            patientId = patientId,
            type = existing.file.type,
            status = ConsentStatus.DRAFT,
            title = existing.file.title,
            procedure = existing.file.procedure,
            version = existing.file.version + 1,
            originalId = existing.file.originalId ?: id,
            planId = existing.file.planId,
            procedureId = existing.file.procedureId,
            issueDate = existing.file.issueDate,
            createdAt = System.currentTimeMillis(),
        )
        val bytes = current.toJsonBytes()
        val contentRef = writeNewAttachment(snapshot, bytes)
        persist(snapshot.copy(
            informedConsents = snapshot.informedConsents + StoredInformedConsent(newFile, contentRef),
        ))
        current.copy(
            id = newFile.id,
            status = ConsentStatus.DRAFT,
            originalId = newFile.originalId,
        )
    }

    override suspend fun deleteInformedConsent(patientId: Long, id: Long) = access {
        val snapshot = load()
        val existing = snapshot.informedConsents.find { it.file.patientId == patientId && it.file.id == id }
            ?: throw IOException("Consent missing")
        persist(snapshot.copy(
            informedConsents = snapshot.informedConsents.filterNot {
                it.file.patientId == patientId && it.file.id == id
            },
        ))
    }

    // --- Assinaturas eletrônicas e digitais ---

    override suspend fun listDocumentSignatures(patientId: Long): List<DocumentSignatureFile> = access {
        load().documentSignatures.map { it.file }.filter { it.patientId == patientId }
    }

    override suspend fun getDocumentSignature(patientId: Long, id: Long): DocumentSignature? = access {
        val stored = load().documentSignatures.find { it.file.patientId == patientId && it.file.id == id }
            ?: return@access null
        val file = stored.file
        val content = try {
            String(readAttachment(stored.content), StandardCharsets.UTF_8)
        } catch (_: IOException) {
            ""
        }
        val obj = JSONObject(content)
        // Formato atual é plano; também aceitamos o formato aninhado para dados antigos.
        val proofObj = obj.optJSONObject("proof") ?: obj
        val modality = proofObj.optString("modality", file.modality.name)
        val status = proofObj.optString("status", file.status.name)
        val signedAt = proofObj.optLong("signedAt", file.signedAt)
        val identification = proofObj.optJSONObject("identification")?.let { rep ->
            SignerIdentification(
                role = rep.optString("role", ""),
                name = rep.optString("name", ""),
                idDocument = rep.optString("idDocument").takeIf { it.isNotBlank() },
                registrationNumber = rep.optString("registrationNumber").takeIf { it.isNotBlank() },
            )
        } ?: obj.optString("identificationRole").takeIf { it.isNotBlank() }?.let { role ->
            SignerIdentification(
                role = role,
                name = obj.optString("identificationName"),
                idDocument = obj.optString("identificationDocument")
                    .takeIf { it.isNotBlank() }
                    ?: obj.optString("identificationIdDocument").takeIf { it.isNotBlank() },
                registrationNumber = obj.optString("identificationRegistrationNumber")
                    .takeIf { it.isNotBlank() },
            )
        }
        val hasVisualRepresentation = proofObj.optBoolean("hasVisualRepresentation", false)
        val cryptographicValid = proofObj.optBoolean("cryptographicValid", false)
        val notes = proofObj.optString("notes").takeIf { it.isNotBlank() }
        val proof = SignatureProof(
            modality = SignatureModality.valueOf(modality),
            status = SignatureStatus.valueOf(status),
            signedAt = signedAt,
            identification = identification,
            hasVisualRepresentation = hasVisualRepresentation,
            cryptographicValid = cryptographicValid,
            notes = notes,
        )
        DocumentSignature(
            id = file.id,
            patientId = file.patientId,
            documentType = file.documentType,
            documentId = file.documentId,
            documentVersion = file.documentVersion,
            documentHash = obj.optString("documentHash")?.takeIf { it.isNotBlank() },
            proof = proof,
        )
    }

    override suspend fun saveDocumentSignature(patientId: Long, signature: DocumentSignature): DocumentSignature = access {
        val snapshot = load()
        requirePatient(snapshot, patientId)
        val now = System.currentTimeMillis()
        val existing = snapshot.documentSignatures.find { it.file.patientId == patientId && it.file.id == signature.id }
        val savedFile = when {
            existing != null -> {
                val updatedFile = existing.file.copy(
                    documentType = signature.documentType,
                    documentId = signature.documentId,
                    documentVersion = signature.documentVersion,
                    modality = signature.proof.modality,
                    status = signature.proof.status,
                    signedAt = signature.proof.signedAt,
                    identificationRole = signature.proof.identification?.role,
                    identificationName = signature.proof.identification?.name,
                    identificationDocument = signature.proof.identification?.idDocument,
                    identificationRegistrationNumber = signature.proof.identification?.registrationNumber,
                )
                val updated = snapshot.documentSignatures.map {
                    if (it.file.patientId == patientId && it.file.id == signature.id) {
                        it.copy(file = updatedFile)
                    } else it
                }
                persist(snapshot.copy(documentSignatures = updated))
                updatedFile
            }
            else -> {
                val newId = nextId(snapshot.documentSignatures.map { it.file.id })
                val newFile = DocumentSignatureFile(
                    id = newId,
                    patientId = patientId,
                    documentType = signature.documentType,
                    documentId = signature.documentId,
                    documentVersion = signature.documentVersion,
                    modality = signature.proof.modality,
                    status = signature.proof.status,
                    signedAt = signature.proof.signedAt,
                    identificationRole = signature.proof.identification?.role,
                    identificationName = signature.proof.identification?.name,
                    identificationDocument = signature.proof.identification?.idDocument,
                    identificationRegistrationNumber = signature.proof.identification?.registrationNumber,
                )
                val bytes = signature.toJsonBytes()
                val content = writeNewAttachment(snapshot, bytes)
                persist(snapshot.copy(
                    documentSignatures = snapshot.documentSignatures + StoredDocumentSignature(newFile, content),
                ))
                newFile
            }
        }
        DocumentSignature(
            id = savedFile.id,
            patientId = patientId,
            documentType = savedFile.documentType,
            documentId = savedFile.documentId,
            documentVersion = savedFile.documentVersion,
            documentHash = signature.documentHash,
            proof = signature.proof,
        )
    }

    override suspend fun deleteDocumentSignature(patientId: Long, id: Long) = access {
        val snapshot = load()
        val existing = snapshot.documentSignatures.find { it.file.patientId == patientId && it.file.id == id }
            ?: throw IOException("Signature missing")
        persist(snapshot.copy(
            documentSignatures = snapshot.documentSignatures.filterNot {
                it.file.patientId == patientId && it.file.id == id
            },
        ))
    }

    // --- Hashes de integridade documental ---

    override suspend fun listDocumentHashes(patientId: Long): List<DocumentHashFile> = access {
        load().documentHashes.map { it.file }.filter { it.patientId == patientId }
    }

    override suspend fun getDocumentHash(patientId: Long, id: Long): DocumentHash? = access {
        val stored = load().documentHashes.find { it.file.patientId == patientId && it.file.id == id }
            ?: return@access null
        val file = stored.file
        val content = try {
            String(readAttachment(stored.content), StandardCharsets.UTF_8)
        } catch (_: IOException) {
            ""
        }
        val obj = JSONObject(content)
        DocumentHash(
            id = file.id,
            patientId = file.patientId,
            documentType = file.documentType,
            documentId = file.documentId,
            documentVersion = file.documentVersion,
            hashAlgorithm = file.hashAlgorithm,
            hashValue = file.hashValue,
            calculatedAt = file.calculatedAt,
            integrityState = file.integrityState,
        )
    }

    override suspend fun saveDocumentHash(patientId: Long, hash: DocumentHash): DocumentHash = access {
        val snapshot = load()
        requirePatient(snapshot, patientId)
        val now = System.currentTimeMillis()
        val existing = snapshot.documentHashes.find { it.file.patientId == patientId && it.file.id == hash.id }
        val savedFile = when {
            existing != null -> {
                val updatedFile = existing.file.copy(
                    documentVersion = hash.documentVersion,
                    hashValue = hash.hashValue,
                    integrityState = hash.integrityState,
                )
                val updated = snapshot.documentHashes.map {
                    if (it.file.patientId == patientId && it.file.id == hash.id) {
                        it.copy(file = updatedFile)
                    } else it
                }
                persist(snapshot.copy(documentHashes = updated))
                updatedFile
            }
            else -> {
                val newId = nextId(snapshot.documentHashes.map { it.file.id })
                val newFile = DocumentHashFile(
                    id = newId,
                    patientId = patientId,
                    documentType = hash.documentType,
                    documentId = hash.documentId,
                    documentVersion = hash.documentVersion,
                    hashAlgorithm = hash.hashAlgorithm,
                    hashValue = hash.hashValue,
                    calculatedAt = now,
                    integrityState = hash.integrityState,
                )
                val bytes = hash.toJsonBytes()
                val content = writeNewAttachment(snapshot, bytes)
                persist(snapshot.copy(
                    documentHashes = snapshot.documentHashes + StoredDocumentHash(newFile, content),
                ))
                newFile
            }
        }
        DocumentHash(
            id = savedFile.id,
            patientId = patientId,
            documentType = savedFile.documentType,
            documentId = savedFile.documentId,
            documentVersion = savedFile.documentVersion,
            hashAlgorithm = savedFile.hashAlgorithm,
            hashValue = savedFile.hashValue,
            calculatedAt = savedFile.calculatedAt,
            integrityState = savedFile.integrityState,
        )
    }

    override suspend fun deleteDocumentHash(patientId: Long, id: Long) = access {
        val snapshot = load()
        val existing = snapshot.documentHashes.find { it.file.patientId == patientId && it.file.id == id }
            ?: throw IOException("Document hash missing")
        persist(snapshot.copy(
            documentHashes = snapshot.documentHashes.filterNot {
                it.file.patientId == patientId && it.file.id == id
            },
        ))
    }

    // --- Lembretes de consulta ---

    override suspend fun listReminders(patientId: Long): List<ReminderFile> = access {
        load().reminders.map { it.file }.filter { it.patientId == patientId }
    }

    override suspend fun getReminder(patientId: Long, id: Long): Reminder? = access {
        val stored = load().reminders.find { it.file.patientId == patientId && it.file.id == id }
            ?: return@access null
        val file = stored.file
        Reminder(
            id = file.id,
            appointmentId = file.appointmentId,
            type = file.type,
            customMinutes = file.customMinutes,
            enabled = file.enabled,
            sent = file.sent,
            sentAt = file.sentAt,
        )
    }

    override suspend fun saveReminder(patientId: Long, reminder: Reminder): Reminder = access {
        val snapshot = load()
        requirePatient(snapshot, patientId)
        val existing = snapshot.reminders.find { it.file.patientId == patientId && it.file.id == reminder.id }
        val savedFile = ReminderFile(
            id = existing?.file?.id ?: nextId(snapshot.reminders.map { it.file.id }),
            patientId = patientId,
            appointmentId = reminder.appointmentId,
            type = reminder.type,
            customMinutes = reminder.customMinutes,
            enabled = reminder.enabled,
            sent = reminder.sent,
            sentAt = reminder.sentAt,
        )
        val bytes = savedFile.toJsonBytes()
        val content = writeNewAttachment(snapshot, bytes)
        val updated = if (existing == null) {
            snapshot.reminders + StoredReminder(savedFile, content)
        } else {
            snapshot.reminders.map {
                if (it.file.patientId == patientId && it.file.id == savedFile.id) {
                    it.copy(file = savedFile, content = content)
                } else it
            }
        }
        persist(snapshot.copy(reminders = updated))
        Reminder(
            id = savedFile.id,
            appointmentId = savedFile.appointmentId,
            type = savedFile.type,
            customMinutes = savedFile.customMinutes,
            enabled = savedFile.enabled,
            sent = savedFile.sent,
            sentAt = savedFile.sentAt,
        )
    }

    override suspend fun deleteReminder(patientId: Long, id: Long) = access {
        val snapshot = load()
        val existing = snapshot.reminders.find { it.file.patientId == patientId && it.file.id == id }
            ?: throw IOException("Reminder missing")
        persist(snapshot.copy(
            reminders = snapshot.reminders.filterNot {
                it.file.patientId == patientId && it.file.id == id
            },
        ))
    }

    override suspend fun listReminderSettings(patientId: Long): ReminderSettingsFile? = access {
        load().reminderSettings.find { it.file.patientId == patientId }?.file
    }

    override suspend fun saveReminderSettings(patientId: Long, settings: ReminderSettings): ReminderSettingsFile = access {
        val snapshot = load()
        requirePatient(snapshot, patientId)
        val existing = snapshot.reminderSettings.find { it.file.patientId == patientId }
        val savedFile = ReminderSettingsFile(
            patientId = patientId,
            reminders24h = settings.reminders24h,
            reminders2h = settings.reminders2h,
            reminders30m = settings.reminders30m,
            customMinutes = settings.customMinutes,
            customEnabled = settings.customEnabled,
        )
        val bytes = savedFile.toJsonBytes()
        val content = writeNewAttachment(snapshot, bytes)
        val updated = if (existing == null) {
            snapshot.reminderSettings + StoredReminderSettings(savedFile, content)
        } else {
            snapshot.reminderSettings.map {
                if (it.file.patientId == patientId) it.copy(file = savedFile, content = content) else it
            }
        }
        persist(snapshot.copy(reminderSettings = updated))
        savedFile
    }

    override suspend fun deleteReminderSettings(patientId: Long) = access {
        val snapshot = load()
        persist(snapshot.copy(
            reminderSettings = snapshot.reminderSettings.filterNot {
                it.file.patientId == patientId
            },
        ))
    }

    override suspend fun updateReminderSent(patientId: Long, reminderId: Long, sentAt: Long) = access {
        val snapshot = load()
        val existing = snapshot.reminders.find { it.file.patientId == patientId && it.file.id == reminderId }
            ?: throw IOException("Reminder missing")
        val updated = existing.file.copy(sent = true, sentAt = sentAt)
        val content = writeNewAttachment(snapshot, updated.toJsonBytes())
        persist(snapshot.copy(
            reminders = snapshot.reminders.map {
                if (it.file.patientId == patientId && it.file.id == reminderId) {
                    it.copy(file = updated, content = content)
                } else it
            },
        ))
    }

    override suspend fun getRemindersForAppointment(patientId: Long, appointmentId: Long): List<ReminderFile> = access {
        load().reminders.map { it.file }.filter { it.patientId == patientId && it.appointmentId == appointmentId }
    }

    private fun autoBackup(): File {
        val directory = app.cacheDir.resolve("auto-backup")
        if (!directory.isDirectory && !directory.mkdirs()) throw IOException("Cannot create auto-backup directory")
        val file = File(directory, "auto-backup-${System.currentTimeMillis()}.odl")
        val output = app.contentResolver.openOutputStream(Uri.fromFile(file), "wt") ?: throw IOException("Cannot open auto-backup")
        output.use {
            BackupArchive.write(it, "auto-backup-temporary".toCharArray()) { writer ->
                val plaintext = load().encode()
                try {
                    writer.frame(plaintext, ProtectedPayload.maxDataSize)
                } finally {
                    plaintext.fill(0)
                }
                load().attachments().forEach { attachment ->
                    val bytes = readAttachment(attachment)
                    try {
                        writer.frame(bytes, maxAttachmentBytes)
                    } finally {
                        bytes.fill(0)
                    }
                }
            }
        }
        return file
    }

    override suspend fun listVisits(patientId: Long): List<VisitRecord> = access {
        load().visits.filter { it.patientId == patientId }
            .sortedWith(compareBy<VisitRecord>({ -it.visitDay }, { -it.visitMinute }, { -it.id }))
    }

    override suspend fun saveVisit(visit: VisitRecord): VisitRecord = access {
        val snapshot = load()
        requirePatient(snapshot, visit.patientId)
        val saved = withVisitId(snapshot, visit)
        persist(snapshot.copy(visits = snapshot.visits.filterNot { it.id == saved.id } + saved))
        saved
    }

    override suspend fun listRadiographs(patientId: Long): List<Radiograph> = access {
        load().radiographs.map { it.file }.filter { it.patientId == patientId }
    }

    override suspend fun previewRadiograph(patientId: Long, id: Long): Bitmap? = access {
        val record = load().radiographs.find { it.file.patientId == patientId && it.file.id == id }
            ?: throw IOException("Radiograph missing")
        val bytes = readAttachment(record.content)
        try {
            decodeOrientedRadiograph(bytes, 1600)
        } finally {
            bytes.fill(0)
        }
    }

    override suspend fun addRadiograph(patientId: Long, uri: Uri, fromCamera: Boolean) = access {
        val snapshot = load()
        requirePatient(snapshot, patientId)
        val input = app.contentResolver.openInputStream(uri) ?: throw IOException("Cannot read radiograph")
        val bytes = input.use { it.readBounded(if (fromCamera) 20 * 1024 * 1024 else maxAttachmentBytes) }
        val image = if (fromCamera) {
            val upright = decodeOrientedRadiograph(bytes, 2048) ?: throw IOException("Invalid camera image")
            try {
                ByteArrayOutputStream().use { output ->
                    if (!upright.compress(Bitmap.CompressFormat.JPEG, 85, output)) throw IOException("Cannot encode photo")
                    if (output.size() > maxAttachmentBytes) throw IOException("Camera image exceeds size limit")
                    output.toByteArray()
                }
            } finally {
                upright.recycle()
            }
        } else {
            val preview = decodeRadiograph(bytes, 256) ?: throw IOException("Invalid image")
            preview.recycle()
            bytes
        }
        val file = Radiograph(nextId(snapshot.radiographs.map { it.file.id }), patientId)
        val record = StoredRadiograph(file, writeNewAttachment(snapshot, image))
        persist(snapshot.copy(radiographs = snapshot.radiographs + record))
    }

    override suspend fun removeRadiograph(patientId: Long, id: Long) = access {
        val snapshot = load()
        if (snapshot.radiographs.none { it.file.patientId == patientId && it.file.id == id }) {
            throw IOException("Radiograph missing")
        }
        persist(snapshot.copy(radiographs = snapshot.radiographs.filterNot { it.file.id == id }))
    }

    override suspend fun listPrescriptions(patientId: Long, archivedDocuments: Boolean): List<PrescriptionFile> = access {
        load().prescriptions.map { it.file }.filter { it.patientId == patientId && it.archivedDocument == archivedDocuments }
    }

    override suspend fun addPrescription(patientId: Long, uri: Uri, archivedDocument: Boolean) = access {
        val snapshot = load()
        requirePatient(snapshot, patientId)
        val input = app.contentResolver.openInputStream(uri) ?: throw IOException("Cannot open prescription")
        val bytes = input.use { it.readBounded(maxAttachmentBytes) }
        val format = prescriptionFormat(bytes)
        if (archivedDocument && format != PrescriptionFormat.PDF) throw IOException("Archived document must be a PDF")
        val displayName = if (uri.scheme == ContentResolver.SCHEME_CONTENT) {
            app.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { if (it.moveToFirst()) it.getString(0) else null }
        } else uri.lastPathSegment
        val name = displayName.orEmpty().substringBeforeLast('.').filterNot { it.isISOControl() }.trim()
            .ifBlank { if (archivedDocument) "Documento" else "Receita" }.take(110) + "." + format.extension
        val id = nextId(snapshot.prescriptions.map { it.file.id })
        val file = StoredPrescription(PrescriptionFile(id, patientId, name, format, archivedDocument), writeNewAttachment(snapshot, bytes))
        persist(snapshot.copy(prescriptions = snapshot.prescriptions + file))
    }

    override suspend fun copyPrescription(patientId: Long, id: Long, uri: Uri) = access {
        val prescription = load().prescriptions.find { it.file.patientId == patientId && it.file.id == id }
            ?: throw IOException("Prescription missing")
        val bytes = readAttachment(prescription.content)
        try {
            val output = app.contentResolver.openOutputStream(uri, "wt") ?: throw IOException("Cannot write prescription")
            output.use { it.write(bytes) }
        } finally {
            bytes.fill(0)
        }
    }

    override suspend fun removePrescription(patientId: Long, id: Long) = access {
        val snapshot = load()
        val file = snapshot.prescriptions.find { it.file.patientId == patientId && it.file.id == id }
            ?: throw IOException("Prescription missing")
        persist(snapshot.copy(prescriptions = snapshot.prescriptions - file))
    }

    override suspend fun listDocumentPhotos(patientId: Long): List<DocumentPhoto> = access {
        load().documentPhotos.map { it.file }.filter { it.patientId == patientId }
    }

    override suspend fun previewDocumentPhoto(patientId: Long, id: Long): Bitmap? = access {
        val record = load().documentPhotos.find { it.file.patientId == patientId && it.file.id == id }
            ?: throw IOException("Document photo missing")
        val bytes = readAttachment(record.content)
        try {
            decodeOrientedDocumentPhoto(bytes, 1600)
        } finally {
            bytes.fill(0)
        }
    }

    override suspend fun addDocumentPhoto(patientId: Long, uri: Uri) = access {
        val snapshot = load()
        requirePatient(snapshot, patientId)
        val input = app.contentResolver.openInputStream(uri) ?: throw IOException("Cannot read document photo")
        val bytes = input.use { it.readBounded(maxAttachmentBytes) }
        val image = decodeOrientedDocumentPhoto(bytes, 2048) ?: throw IOException("Invalid camera image")
        try {
            ByteArrayOutputStream().use { output ->
                if (!image.compress(Bitmap.CompressFormat.JPEG, 85, output)) throw IOException("Cannot encode photo")
                if (output.size() > maxAttachmentBytes) throw IOException("Camera image exceeds size limit")
                val compressed = output.toByteArray()
                val file = DocumentPhoto(nextId(snapshot.documentPhotos.map { it.file.id }), patientId)
                val record = StoredDocumentPhoto(file, writeNewAttachment(snapshot, compressed))
                persist(snapshot.copy(documentPhotos = snapshot.documentPhotos + record))
            }
        } finally {
            image.recycle()
        }
    }

    override suspend fun removeDocumentPhoto(patientId: Long, id: Long) = access {
        val snapshot = load()
        if (snapshot.documentPhotos.none { it.file.patientId == patientId && it.file.id == id }) {
            throw IOException("Document photo missing")
        }
        persist(snapshot.copy(documentPhotos = snapshot.documentPhotos.filterNot { it.file.id == id }))
    }

    override suspend fun listDigitalSignatures(patientId: Long?): List<DigitalSignature> = access {
        load().digitalSignatures.map { it.file }
            .filter { patientId == null || it.patientId == patientId }
    }

    override suspend fun previewDigitalSignature(patientId: Long?, id: Long): Bitmap? = access {
        val record = load().digitalSignatures.find {
            it.file.id == id && (patientId == null || it.file.patientId == patientId)
        } ?: throw IOException("Digital signature missing")
        val bytes = readAttachment(record.content)
        try {
            decodeDigitalSignature(bytes, 1600)
        } finally {
            bytes.fill(0)
        }
    }

    override suspend fun saveDigitalSignature(patientId: Long?, documentType: String?, bitmap: Bitmap): DigitalSignature = access {
        val snapshot = load()
        if (patientId != null) requirePatient(snapshot, patientId)
        val bytes = encodeDigitalSignature(bitmap, 2048)
        val file = DigitalSignature(
            nextId(snapshot.digitalSignatures.map { it.file.id }),
            patientId,
            documentType,
        )
        val record = StoredDigitalSignature(file, writeNewAttachment(snapshot, bytes))
        persist(snapshot.copy(digitalSignatures = snapshot.digitalSignatures + record))
        file
    }

    override suspend fun removeDigitalSignature(patientId: Long?, id: Long) = access {
        val snapshot = load()
        if (snapshot.digitalSignatures.none {
            it.file.id == id && (patientId == null || it.file.patientId == patientId)
        }) {
            throw IOException("Digital signature missing")
        }
        persist(snapshot.copy(digitalSignatures = snapshot.digitalSignatures.filterNot { it.file.id == id }))
    }

    override suspend fun uploadProfessionalLogo(uri: Uri) = access {
        throw IOException("Not implemented")
    }

    override suspend fun previewProfessionalLogo(): Bitmap? = access {
        throw IOException("Not implemented")
    }

    override suspend fun removeProfessionalLogo() = access {
        throw IOException("Not implemented")
    }

    override suspend fun exportBackup(uri: Uri, password: CharArray) = access {
        if (password.size !in 12..128) throw IOException("Invalid backup password length")
        val snapshot = load()
        val output = app.contentResolver.openOutputStream(uri, "wt") ?: throw IOException("Cannot open backup")
        output.use {
            BackupArchive.write(it, password) { writer ->
                val plaintext = snapshot.encode()
                try {
                    writer.frame(plaintext, ProtectedPayload.maxDataSize)
                } finally {
                    plaintext.fill(0)
                }
                snapshot.attachments().forEach { attachment ->
                    val bytes = readAttachment(attachment)
                    try {
                        writer.frame(bytes, maxAttachmentBytes)
                    } finally {
                        bytes.fill(0)
                    }
                }
            }
        }
    }

    override suspend fun restoreBackup(uri: Uri, password: CharArray) = access {
        if (password.size !in 1..128) throw IOException("Invalid backup password length")
        val backupFile = autoBackup()
        try {
            val input = app.contentResolver.openInputStream(uri) ?: throw IOException("Cannot open backup")
            val snapshot = stageAttachments { staged ->
                BufferedInputStream(input).use { stream ->
                    if (BackupArchive.matches(stream)) {
                        BackupArchive.read(stream, password) { reader ->
                            decodeSnapshot(reader.frame(ProtectedPayload.maxDataSize)).mapAttachments { content ->
                                val reference = content as? AttachmentRef ?: throw IOException("Invalid backup index")
                                val bytes = reader.frame(reference.size)
                                try {
                                    if (bytes.size != reference.size) throw IOException("Invalid backup attachment size")
                                    writeAttachment(bytes).also(staged::add)
                                } finally {
                                    bytes.fill(0)
                                }
                            }
                        }
                    } else {
                        migrateAttachments(
                            decodeSnapshot(ProtectedPayload.decryptBackup(stream.readClinicFile(), password)),
                            staged,
                        )
                    }
                }
            }
            persist(snapshot)
            removeLegacy()
            backupFile.deleteRecursively()
        } catch (error: Exception) {
            backupFile.deleteRecursively()
            throw error
        }
        Unit
    }

    private fun decodeSnapshot(bytes: ByteArray): ClinicSnapshot = try {
        ClinicSnapshot.decode(bytes)
    } finally {
        bytes.fill(0)
    }

    private fun migrateAttachments(snapshot: ClinicSnapshot, staged: MutableList<AttachmentRef>): ClinicSnapshot =
        snapshot.mapAttachments { content ->
            val embedded = content as? EmbeddedAttachment ?: throw IOException("Legacy backup lacks attachment contents")
            val bytes = try {
                Base64.decode(embedded.encoded, Base64.NO_WRAP)
            } catch (error: IllegalArgumentException) {
                throw IOException("Invalid embedded attachment", error)
            }
            try {
                writeAttachment(bytes).also(staged::add)
            } finally {
                bytes.fill(0)
            }
        }

    private fun stageAttachments(action: (MutableList<AttachmentRef>) -> ClinicSnapshot): ClinicSnapshot {
        val staged = mutableListOf<AttachmentRef>()
        try {
            return action(staged)
        } catch (error: Exception) {
            attachments.discard(staged)
            throw error
        }
    }

    private fun writeNewAttachment(snapshot: ClinicSnapshot, bytes: ByteArray): AttachmentRef {
        val total = snapshot.attachments().filterIsInstance<AttachmentRef>().sumOf { it.size.toLong() }
        if (total + bytes.size > maxAttachmentTotal) throw IOException("Attachment storage is full")
        return writeAttachment(bytes)
    }

    private fun writeAttachment(bytes: ByteArray): AttachmentRef {
        cleanupPending = true
        return attachments.write(bytes, key(create = true))
    }

    private fun readAttachment(content: AttachmentContent): ByteArray {
        val reference = content as? AttachmentRef ?: throw IOException("Attachment has not been migrated")
        return attachments.read(reference, key(create = false))
    }

    private fun cleanAttachments(snapshot: ClinicSnapshot) {
        attachments.prune(snapshot.attachments().filterIsInstance<AttachmentRef>())
        cleanupPending = false
    }

    private fun load(): ClinicSnapshot {
        val encrypted = try {
            snapshotFile.openRead().use { it.readClinicFile() }
        } catch (error: FileNotFoundException) {
            if (snapshotFile.baseFile.exists()) throw error
            if (!legacyFile.exists()) return ClinicSnapshot().also { if (cleanupPending) cleanAttachments(it) }
            return LegacyClinic.read(legacyFile).also { persist(it); removeLegacy() }
        }
        val snapshot = decodeSnapshot(ProtectedPayload.decryptLocal(encrypted, key(create = false)))
        if (snapshot.attachments().any { it is EmbeddedAttachment }) {
            val migrated = stageAttachments { migrateAttachments(snapshot, it) }
            return migrated.also { persist(it); removeLegacy() }
        }
        if (cleanupPending) cleanAttachments(snapshot)
        removeLegacy()
        return snapshot
    }

    private fun persist(snapshot: ClinicSnapshot) {
        snapshot.validate()
        val bytes = snapshot.encode()
        val encrypted = try {
            ProtectedPayload.encryptLocal(bytes, key(create = true))
        } finally {
            bytes.fill(0)
        }
        val stream = snapshotFile.startWrite()
        try {
            stream.write(encrypted)
            stream.fd.sync()
        } catch (error: IOException) {
            snapshotFile.failWrite(stream)
            throw error
        }
        snapshotFile.finishWrite(stream)
        if (!snapshotFile.openRead().use { it.readClinicFile() }.contentEquals(encrypted)) {
            throw IOException("Snapshot commit could not be verified")
        }
        cleanAttachments(snapshot)
        compactSnapshotIfNeeded()
    }

    private fun compactSnapshotIfNeeded() {
        val file = snapshotFile.baseFile
        if (!file.exists()) return
        val size = file.length()
        if (size > maxSnapshotSize) {
            val snapshot = load()
            persist(snapshot)
        }
    }

    private fun removeLegacy() {
        if (legacyFile.exists() && !SQLiteDatabase.deleteDatabase(legacyFile)) {
            throw IOException("Could not remove legacy database")
        }
    }

    private fun key(create: Boolean): SecretKey {
        val provider = "AndroidKeyStore"
        val alias = "odontolivreval.demo.snapshot.v1"
        val store = KeyStore.getInstance(provider).apply { load(null) }
        (store.getKey(alias, null) as? SecretKey)?.let { return it }
        if (!create) throw IOException("Snapshot key unavailable")
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, provider).apply {
            init(KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setKeySize(256)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .build())
        }.generateKey()
    }

    private fun requirePatient(snapshot: ClinicSnapshot, patientId: Long) {
        if (snapshot.patients.none { it.id == patientId }) throw IOException("Patient missing")
    }

    private fun AppointmentRecord.toDemo(day: LocalDate, patientName: String) = DemoAppointment(
        id,
        patientId,
        patientName,
        day,
        LocalTime.of(minute / 60, minute % 60),
        cancelled,
        durationMinutes,
        returnOfId,
    )

    private companion object {
        const val MAX_UPCOMING_DAYS = 366
        const val maxSnapshotSize = 4 * 1024 * 1024 // 4 MiB: compacta o snapshot quando excede este limite
    }

    private fun replaceRecord(snapshot: ClinicSnapshot, record: PatientRecord): List<PatientRecord> =
        snapshot.patientRecords.filterNot { it.patientId == record.patientId } + record

    private fun withRecordId(snapshot: ClinicSnapshot, record: PatientRecord): PatientRecord {
        val existing = snapshot.patientRecords.find { it.patientId == record.patientId }
        val id = existing?.id?.takeIf { it > 0 } ?: nextId(snapshot.patientRecords.map { it.id })
        return record.copy(id = id)
    }

    private fun withVisitId(snapshot: ClinicSnapshot, visit: VisitRecord): VisitRecord {
        if (visit.id <= 0) {
            return visit.copy(id = nextId(snapshot.visits.map { it.id }))
        }
        val existing = snapshot.visits.find { it.id == visit.id }
            ?: throw IOException("Visit missing")
        if (existing.patientId != visit.patientId) throw IOException("Visit belongs to another patient")
        return visit
    }

    private fun nextId(ids: List<Long>): Long {
        val last = ids.maxOrNull() ?: 0
        if (last == Long.MAX_VALUE) throw IOException("Record limit reached")
        return last + 1
    }

    private suspend fun <T> access(action: () -> T): T = withContext(Dispatchers.IO) {
        mutex.withLock {
            try {
                action()
            } catch (error: Exception) {
                when (error) {
                    is IOException, is GeneralSecurityException, is ProviderException,
                    is JSONException, is SQLiteException, is SecurityException ->
                        throw ClinicStorageException(error)
                    else -> throw error
                }
            }
        }
    }
}
