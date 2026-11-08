package br.com.odontolivreval.core.demo

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import android.net.Uri
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.AtomicFile
import java.io.FileNotFoundException
import java.io.IOException
import java.security.GeneralSecurityException
import java.security.KeyStore
import java.security.ProviderException
import java.time.LocalDate
import java.time.LocalTime
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
    val time: LocalTime,
    val cancelled: Boolean,
)

enum class AppointmentSaveResult { SAVED, TIME_OCCUPIED }

class ClinicStorageException(cause: Exception) : Exception(cause)

class DemoClinicStore(context: Context) : ClinicStore {
    private val app = context.applicationContext
    private val legacyFile = app.noBackupFilesDir.resolve("demo-patients.db")
    private val snapshotFile = AtomicFile(app.noBackupFilesDir.resolve("demo-clinic.enc"))
    private val mutex = Mutex()

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
                    it.id, it.patientId, patients.getValue(it.patientId).name,
                    LocalTime.of(it.minute / 60, it.minute % 60), it.cancelled,
                )
            }
    }

    override suspend fun saveAppointment(
        id: Long?,
        patientId: Long,
        day: LocalDate,
        time: LocalTime,
    ): AppointmentSaveResult = access {
        val snapshot = load()
        val minute = time.hour * 60 + time.minute
        val epochDay = day.toEpochDay()
        if (snapshot.patients.none { it.id == patientId }) throw IOException("Patient missing")
        if (id != null && snapshot.appointments.none { it.id == id && !it.cancelled }) {
            throw IOException("Active appointment missing")
        }
        if (snapshot.appointments.any { it.day == epochDay && it.minute == minute && !it.cancelled && it.id != id }) {
            return@access AppointmentSaveResult.TIME_OCCUPIED
        }
        val record = AppointmentRecord(id ?: nextId(snapshot.appointments.map { it.id }), patientId, epochDay, minute, false)
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
        persist(snapshot.copy(
            patients = snapshot.patients.filterNot { it.id == id },
            appointments = snapshot.appointments.filter { it.patientId != id },
            patientRecords = snapshot.patientRecords.filterNot { it.patientId == id },
            visits = snapshot.visits.filterNot { it.patientId == id },
        ))
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

    override suspend fun exportBackup(uri: Uri, password: CharArray) = access {
        if (password.size !in 12..128) throw IOException("Invalid backup password length")
        val plaintext = load().encode()
        val protected = try {
            ProtectedPayload.encryptBackup(plaintext, password)
        } finally {
            plaintext.fill(0)
        }
        val stream = app.contentResolver.openOutputStream(uri, "wt") ?: throw IOException("Cannot open backup")
        stream.use { it.write(protected) }
    }

    override suspend fun restoreBackup(uri: Uri, password: CharArray) = access {
        if (password.size !in 1..128) throw IOException("Invalid backup password length")
        val input = app.contentResolver.openInputStream(uri) ?: throw IOException("Cannot open backup")
        val encrypted = input.use { it.readClinicFile() }
        val plaintext = ProtectedPayload.decryptBackup(encrypted, password)
        val snapshot = try {
            ClinicSnapshot.decode(plaintext)
        } finally {
            plaintext.fill(0)
        }
        persist(snapshot)
        removeLegacy()
    }

    private fun load(): ClinicSnapshot {
        val encrypted = try {
            snapshotFile.openRead().use { it.readClinicFile() }
        } catch (error: FileNotFoundException) {
            if (snapshotFile.baseFile.exists()) throw error
            if (!legacyFile.exists()) return ClinicSnapshot()
            return LegacyClinic.read(legacyFile).also { persist(it); removeLegacy() }
        }
        val bytes = ProtectedPayload.decryptLocal(encrypted, key(create = false))
        try {
            return ClinicSnapshot.decode(bytes).also { removeLegacy() }
        } finally {
            bytes.fill(0)
        }
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
