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

class DemoClinicStore(context: Context) {
    private val app = context.applicationContext
    private val legacyFile = app.noBackupFilesDir.resolve("demo-patients.db")
    private val snapshotFile = AtomicFile(app.noBackupFilesDir.resolve("demo-clinic.enc"))
    private val mutex = Mutex()

    suspend fun listPatients(): List<DemoPatient> = access { load().patients }

    suspend fun savePatient(id: Long?, name: String): DemoPatient = access {
        val snapshot = load()
        val patient = DemoPatient(id ?: nextId(snapshot.patients.map { it.id }), name)
        if (id != null && snapshot.patients.none { it.id == id }) throw IOException("Patient missing")
        persist(snapshot.copy(patients = snapshot.patients.filterNot { it.id == id } + patient))
        patient
    }

    suspend fun listAppointments(day: LocalDate): List<DemoAppointment> = access {
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

    suspend fun saveAppointment(
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

    suspend fun cancelAppointment(id: Long) = access {
        val snapshot = load()
        if (snapshot.appointments.none { it.id == id }) throw IOException("Appointment missing")
        persist(snapshot.copy(appointments = snapshot.appointments.map {
            if (it.id == id) it.copy(cancelled = true) else it
        }))
    }

    suspend fun getPatientRecord(patientId: Long): PatientRecord? = access {
        load().patientRecords.find { it.patientId == patientId }
    }

    suspend fun savePatientRecord(record: PatientRecord) = access {
        val snapshot = load()
        if (snapshot.patients.none { it.id == record.patientId }) throw IOException("Patient missing")
        val updated = snapshot.patientRecords.filterNot { it.patientId == record.patientId } + record
        persist(snapshot.copy(patientRecords = updated))
    }

    suspend fun deletePatient(id: Long) = access {
        val snapshot = load()
        if (snapshot.patients.none { it.id == id }) throw IOException("Patient missing")
        persist(snapshot.copy(
            patients = snapshot.patients.filterNot { it.id == id },
            appointments = snapshot.appointments.filter { it.patientId != id },
            patientRecords = snapshot.patientRecords.filterNot { it.patientId == id },
            visits = snapshot.visits.filterNot { it.patientId == id },
        ))
    }

    suspend fun listVisits(patientId: Long): List<VisitRecord> = access {
        load().visits.filter { it.patientId == patientId }
            .sortedWith(compareBy<VisitRecord>({ -it.visitDay }, { -it.visitMinute }, { it.id }))
    }

    suspend fun saveVisit(visit: VisitRecord) = access {
        val snapshot = load()
        if (snapshot.patients.none { it.id == visit.patientId }) throw IOException("Patient missing")
        val updated = snapshot.visits.filterNot { it.id == visit.id } + visit
        persist(snapshot.copy(visits = updated))
    }

    suspend fun exportBackup(uri: Uri, password: CharArray) = access {
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

    suspend fun restoreBackup(uri: Uri, password: CharArray) = access {
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
