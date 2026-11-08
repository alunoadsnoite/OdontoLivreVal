package br.com.odontolivreval.core.demo

const val defaultAppointmentMinutes = 30
const val maxAppointmentMinutes = 240

internal fun List<AppointmentRecord>.withLegacyDurations(): List<AppointmentRecord> {
    val nextStartById = filterNot { it.cancelled }
        .groupBy { it.day }
        .values
        .flatMap { day ->
            day.sortedBy { it.minute }.zipWithNext { current, next -> current.id to next.minute }
        }
        .toMap()
    return map { appointment ->
        val nextStart = nextStartById[appointment.id] ?: 1440
        appointment.copy(
            durationMinutes = minOf(defaultAppointmentMinutes, (nextStart - appointment.minute).coerceAtLeast(1)),
        )
    }
}
