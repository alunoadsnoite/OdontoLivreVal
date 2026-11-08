package br.com.odontolivreval.core.demo

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.ByteArrayOutputStream
import java.io.IOException

data class DigitalSignature(
    val id: Long,
    val patientId: Long? = null,
    val documentType: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
)

internal data class StoredDigitalSignature(val file: DigitalSignature, val content: AttachmentContent)

internal fun decodeDigitalSignature(data: ByteArray, maxEdge: Int): Bitmap? {
    val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(data, 0, data.size, options)
    if (options.outWidth <= 0 || options.outHeight <= 0 ||
        (options.outMimeType != "image/png" && options.outMimeType != "image/jpeg")
    ) return null
    var sample = 1
    val edge = maxOf(options.outWidth, options.outHeight)
    while (edge / sample > maxEdge) sample *= 2
    options.inSampleSize = sample
    options.inJustDecodeBounds = false
    return BitmapFactory.decodeByteArray(data, 0, data.size, options)
}

internal fun encodeDigitalSignature(bitmap: Bitmap, maxEdge: Int): ByteArray {
    var working = bitmap
    if (maxOf(working.width, working.height) > maxEdge) {
        val scale = maxEdge.toFloat() / maxOf(working.width, working.height)
        working = Bitmap.createScaledBitmap(
            working,
            (working.width * scale).toInt(),
            (working.height * scale).toInt(),
            true,
        )
    }
    val output = ByteArrayOutputStream()
    try {
        if (!working.compress(Bitmap.CompressFormat.PNG, 92, output)) {
            throw IOException("Cannot encode signature")
        }
        return output.toByteArray()
    } finally {
        output.close()
        if (working !== bitmap) working.recycle()
    }
}
