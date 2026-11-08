package br.com.odontolivreval.feature.patients

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import androidx.core.graphics.withTranslation
import br.com.odontolivreval.core.demo.ProfessionalProfile
import br.com.odontolivreval.core.demo.VisitRecord
import java.io.OutputStream
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

object OdontogramPdf {
    private const val pageWidth = 595
    private const val pageHeight = 842
    private const val margin = 42f
    private const val purple = 0xff62368c.toInt()
    private const val ink = 0xff241b2d.toInt()
    private const val muted = 0xff51475a.toInt()

    fun write(
        context: Context,
        patientName: String,
        visit: VisitRecord,
        output: OutputStream,
        profile: ProfessionalProfile? = null,
    ) {
        val document = PdfDocument()
        try {
            val dentitions = Dentition.entries.filter { it.hasMarks(visit.odontogram) }
                .ifEmpty { listOf(Dentition.PERMANENT) }
            dentitions.forEachIndexed { index, dentition ->
                val pageNumber = index + 1
                val page = document.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
                try {
                    drawPage(context, page.canvas, patientName, visit, dentition, pageNumber, dentitions.size, profile)
                } finally {
                    document.finishPage(page)
                }
            }
            document.writeTo(output)
        } finally {
            document.close()
        }
    }

    private fun drawPage(
        context: Context,
        canvas: Canvas,
        patientName: String,
        visit: VisitRecord,
        dentition: Dentition,
        pageNumber: Int,
        pageCount: Int,
        profile: ProfessionalProfile?,
    ) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        canvas.drawColor(Color.WHITE)
        paint.color = purple
        canvas.drawRect(margin, 48f, pageWidth - margin, 52f, paint)
        canvas.label(context.getString(R.string.pdf_odontogram_header), margin, 78f, 12f, purple, true)
        canvas.label(context.getString(R.string.odontogram), margin, 115f, 30f, ink, true)

        val name = context.getString(R.string.pdf_patient, patientName)
        val namePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 14f; color = ink }
        val nameLayout = StaticLayout.Builder.obtain(name, 0, name.length, namePaint, (pageWidth - 2 * margin).toInt())
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setIncludePad(false)
            .setMaxLines(3)
            .setEllipsize(TextUtils.TruncateAt.END)
            .build()
        canvas.withTranslation(margin, 144f) { nameLayout.draw(this) }

        val date = LocalDate.ofEpochDay(visit.visitDay)
        val time = LocalTime.of(visit.visitMinute / 60, visit.visitMinute % 60)
        val dateTime = date.atTime(time).format(DateTimeFormatter.ofPattern("dd/MM/uuuu HH:mm"))
        canvas.label(context.getString(R.string.pdf_visit_date, dateTime), margin, 160f + nameLayout.height, 12f, muted)

        val dentitionName = context.getString(dentition.label).lowercase()
        canvas.label(context.getString(R.string.pdf_dentition, dentitionName), margin, 222f, 12f, purple, true)
        canvas.arch(context.getString(R.string.odontogram_upper), dentition.upper, visit.odontogram, 252f)
        canvas.arch(context.getString(R.string.odontogram_lower), dentition.lower, visit.odontogram, 395f)

        canvas.label(context.getString(R.string.odontogram_legend), margin, 544f, 15f, ink, true)
        statuses.forEachIndexed { index, status ->
            val x = margin + (index % 2) * 250f
            val y = 568f + (index / 2) * 20f
            canvas.label(status.code.ifEmpty { "-" }, x, y, 11f, purple, true)
            canvas.label(context.getString(status.label), x + 24f, y, 11f, ink)
        }
        canvas.label(context.getString(R.string.pdf_surface_legend), margin, 646f, 12f, ink, true)
        ToothSurface.entries.forEachIndexed { index, surface ->
            val x = margin + (index % 3) * 167f
            val y = 667f + (index / 3) * 20f
            canvas.label(surface.code, x, y, 10f, purple, true)
            canvas.label(context.getString(surface.label), x + 20f, y, 10f, ink)
        }
        if (profile != null && profile.name.isNotBlank() && profile.cro.isNotBlank()) {
            val professional = context.getString(R.string.pdf_professional, profile.name, profile.cro)
            val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 10f; color = muted }
            val layout = StaticLayout.Builder.obtain(
                professional, 0, professional.length, textPaint, (pageWidth - 2 * margin).toInt(),
            ).setIncludePad(false).setMaxLines(2).setEllipsize(TextUtils.TruncateAt.END).build()
            canvas.withTranslation(margin, 712f) { layout.draw(this) }
        }
        paint.color = 0xffddd2e4.toInt()
        canvas.drawRect(margin, 747f, pageWidth - margin, 748f, paint)
        canvas.label(context.getString(R.string.pdf_page_footer, pageNumber, pageCount), margin, 772f, 10f, muted)
    }

    private fun Canvas.arch(label: String, groups: List<IntProgression>, marks: Map<String, String>, y: Float) {
        label(label, margin, y, 15f, ink, true)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val knownCodes = statuses.map { it.code }.toSet()
        val gap = 12f
        val toothCount = groups.sumOf { it.count() }
        val cellWidth = (pageWidth - margin * 2 - gap) / toothCount
        var x = margin
        groups.forEach { group ->
            group.forEach { tooth ->
                val whole = marks[toothKey(tooth)].orEmpty()
                val surfaces = surfacesFor(tooth).mapNotNull { surface ->
                    marks[toothKey(tooth, surface)]?.let { surface.code + ":" + if (it in knownCodes) it else "?" }
                }
                val display = if (whole in knownCodes) whole.ifEmpty { "-" } else "?"
                paint.color = if (whole.isEmpty() && surfaces.isEmpty()) 0xfff7f4f9.toInt() else 0xffedddfa.toInt()
                drawRect(x, y + 12f, x + cellWidth - 2, y + 122f, paint)
                label(tooth.toString(), x + 5f, y + 30f, 11f, ink, true)
                label(display, x + 5f, y + 48f, 11f, purple, true)
                surfaces.forEachIndexed { index, surface ->
                    label(surface, x + 5f, y + 62f + index * 12f, 9f, ink)
                }
                x += cellWidth
            }
            x += gap
        }
    }

    private fun Canvas.label(text: String, x: Float, y: Float, size: Float, tint: Int, bold: Boolean = false) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = tint
            textSize = size
            typeface = if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
        }
        drawText(text, x, y, paint)
    }
}
