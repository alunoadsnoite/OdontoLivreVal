package br.com.odontolivreval.core.demo

import android.database.sqlite.SQLiteDatabase
import java.io.File
import java.io.IOException

internal object LegacyClinic {
    fun read(file: File): ClinicSnapshot = SQLiteDatabase.openDatabase(
        file.absolutePath, null, SQLiteDatabase.OPEN_READWRITE or SQLiteDatabase.NO_LOCALIZED_COLLATORS,
    ) { throw IOException("Legacy database is corrupt") }.use { db ->
        val version = db.version
        if (version !in 1..2) throw IOException("Unsupported legacy clinic version")
        val patients = db.rawQuery("SELECT id, name FROM patients ORDER BY id", null).use { rows ->
            buildList {
                while (rows.moveToNext()) add(DemoPatient(rows.getLong(0), rows.getString(1)))
            }
        }
        val appointments = if (version == 1) emptyList() else {
            db.rawQuery("SELECT id, patient_id, day, minute, cancelled FROM appointments", null).use { rows ->
                buildList {
                    while (rows.moveToNext()) {
                        add(AppointmentRecord(rows.getLong(0), rows.getLong(1), rows.getLong(2), rows.getInt(3), rows.getInt(4) != 0))
                    }
                }
            }
        }
        ClinicSnapshot(patients, appointments.withLegacyDurations())
    }
}
