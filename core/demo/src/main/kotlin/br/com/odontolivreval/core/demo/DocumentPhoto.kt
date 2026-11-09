package br.com.odontolivreval.core.demo

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
import java.io.ByteArrayInputStream
import java.io.IOException

data class DocumentPhoto(val id: Long, val patientId: Long)

internal data class StoredDocumentPhoto(val file: DocumentPhoto, val content: AttachmentContent)

internal fun decodeDocumentPhoto(data: ByteArray, maxEdge: Int): Bitmap? {
    val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(data, 0, data.size, options)
    if (options.outWidth <= 0 || options.outHeight <= 0 ||
        (options.outMimeType != "image/jpeg" && options.outMimeType != "image/png")
    ) return null
    var sample = 1
    val edge = maxOf(options.outWidth, options.outHeight)
    while (edge / sample > maxEdge) sample *= 2
    options.inSampleSize = sample
    options.inJustDecodeBounds = false
    return BitmapFactory.decodeByteArray(data, 0, data.size, options)
}

internal fun decodeOrientedDocumentPhoto(data: ByteArray, maxEdge: Int): Bitmap? {
    val decoded = decodeDocumentPhoto(data, maxEdge) ?: return null
    val orientation = try {
        ExifInterface(ByteArrayInputStream(data))
            .getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
    } catch (_: IOException) {
        ExifInterface.ORIENTATION_NORMAL
    }
    val matrix = Matrix()
    when (orientation) {
        ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.setScale(-1f, 1f)
        ExifInterface.ORIENTATION_ROTATE_180 -> matrix.setRotate(180f)
        ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.setScale(1f, -1f)
        ExifInterface.ORIENTATION_TRANSPOSE -> { matrix.setRotate(90f); matrix.postScale(-1f, 1f) }
        ExifInterface.ORIENTATION_ROTATE_90 -> matrix.setRotate(90f)
        ExifInterface.ORIENTATION_TRANSVERSE -> { matrix.setRotate(-90f); matrix.postScale(-1f, 1f) }
        ExifInterface.ORIENTATION_ROTATE_270 -> matrix.setRotate(-90f)
        else -> return decoded
    }
    val upright = Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
    if (upright !== decoded) decoded.recycle()
    return upright
}
