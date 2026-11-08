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
            ))
        } catch (error: Exception) {
            backupFile.deleteRecursively()
            throw error
        }
        backupFile.deleteRecursively()
        Unit
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
        // Backup automático antes de restauração para permitir recuperação em caso de falha
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

    // Clean up only before the index commit starts. An interrupted commit may already
    // have published the index, so later cleanup reads the authoritative index first.
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

    /**
     * Mantém o ID de uma ficha existente do mesmo paciente e cria um novo ID
     * quando a ficha ainda não existe. Assim, fichas de pacientes diferentes
     * nunca compartilham o mesmo identificador.
     */
    private fun withRecordId(snapshot: ClinicSnapshot, record: PatientRecord): PatientRecord {
        val existing = snapshot.patientRecords.find { it.patientId == record.patientId }
        val id = existing?.id?.takeIf { it > 0 } ?: nextId(snapshot.patientRecords.map { it.id })
        return record.copy(id = id)
    }

    /**
     * Consultas novas recebem o próximo ID livre. Consultas existentes precisam
     * pertencer ao mesmo paciente para evitar sobrescrever dados de outro.
     */
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
