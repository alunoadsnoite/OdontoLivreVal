package br.com.odontolivreval.feature.patients

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OdontogramDentitionTest {
    @Test
    fun primaryChartAndSurfaceMarksUseTheirOwnToothKeys() {
        assertEquals((55 downTo 51).toList() + (61..65).toList(), Dentition.PRIMARY.upper.flatMap { it.toList() })
        assertEquals((85 downTo 81).toList() + (71..75).toList(), Dentition.PRIMARY.lower.flatMap { it.toList() })
        assertEquals(ToothSurface.INCISAL, surfacesFor(51).last())
        assertEquals(ToothSurface.OCCLUSAL, surfacesFor(55).last())
        val marks = mapOf(toothKey(55, ToothSurface.OCCLUSAL) to "R")
        assertEquals(setOf("55:O"), marks.keys)
        assertTrue(Dentition.PRIMARY.hasMarks(marks))
        assertFalse(Dentition.PERMANENT.hasMarks(marks))
        assertTrue(Dentition.PERMANENT.hasMarks(mapOf("11" to "E")))
    }
}
