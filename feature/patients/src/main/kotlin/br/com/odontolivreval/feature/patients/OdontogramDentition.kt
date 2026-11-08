package br.com.odontolivreval.feature.patients

internal enum class Dentition(
    val label: Int,
    val upper: List<IntProgression>,
    val lower: List<IntProgression>,
) {
    PERMANENT(R.string.odontogram_permanent, listOf(18 downTo 11, 21..28), listOf(48 downTo 41, 31..38)),
    PRIMARY(R.string.odontogram_primary, listOf(55 downTo 51, 61..65), listOf(85 downTo 81, 71..75));

    fun hasMarks(marks: Map<String, String>): Boolean =
        (upper + lower).any { group ->
            group.any { tooth ->
                toothKey(tooth) in marks || surfacesFor(tooth).any { toothKey(tooth, it) in marks }
            }
        }
}

internal enum class ToothSurface(val code: String, val label: Int) {
    MESIAL("M", R.string.odontogram_mesial),
    DISTAL("D", R.string.odontogram_distal),
    VESTIBULAR("V", R.string.odontogram_vestibular),
    LINGUAL("L", R.string.odontogram_lingual),
    INCISAL("I", R.string.odontogram_incisal),
    OCCLUSAL("O", R.string.odontogram_occlusal),
}

internal fun surfacesFor(tooth: Int): List<ToothSurface> = listOf(
    ToothSurface.MESIAL,
    ToothSurface.DISTAL,
    ToothSurface.VESTIBULAR,
    ToothSurface.LINGUAL,
    if (tooth % 10 <= 3) ToothSurface.INCISAL else ToothSurface.OCCLUSAL,
)

internal fun toothKey(tooth: Int, surface: ToothSurface? = null): String =
    if (surface == null) tooth.toString() else "$tooth:${surface.code}"
