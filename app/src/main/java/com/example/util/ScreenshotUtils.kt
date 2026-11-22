package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import kotlin.math.max
import kotlin.math.roundToInt

object ScreenshotUtils {
    private const val MAX_DIMENSION_PX = 720
    private const val MAX_BYTES = 180_000

    suspend fun encodeUriToBase64(context: Context, uri: Uri): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val resolver = context.contentResolver
            val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            resolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, boundsOptions)
            }

            val srcW = boundsOptions.outWidth.coerceAtLeast(1)
            val srcH = boundsOptions.outHeight.coerceAtLeast(1)
            val maxSide = max(srcW, srcH)
            var sampleSize = 1
            while (maxSide / (sampleSize * 2) >= MAX_DIMENSION_PX) {
                sampleSize *= 2
            }

            val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sampleSize }
            val decoded = resolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, decodeOptions)
            } ?: error("Unable to read the selected screenshot image.")

            val scale = if (max(decoded.width, decoded.height) > MAX_DIMENSION_PX) {
                MAX_DIMENSION_PX.toFloat() / max(decoded.width, decoded.height).toFloat()
            } else {
                1f
            }

            val scaledBitmap = if (scale < 1f) {
                Bitmap.createScaledBitmap(
                    decoded,
                    (decoded.width * scale).roundToInt().coerceAtLeast(1),
                    (decoded.height * scale).roundToInt().coerceAtLeast(1),
                    true
                )
            } else {
                decoded
            }

            val out = ByteArrayOutputStream()
            var quality = 75
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
            while (out.size() > MAX_BYTES && quality > 30) {
                out.reset()
                quality -= 10
                scaledBitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
            }

            if (scaledBitmap !== decoded) {
                scaledBitmap.recycle()
            }
            decoded.recycle()

            val bytes = out.toByteArray()
            require(bytes.isNotEmpty() && bytes.size <= 400_000) {
                "Screenshot image is too large. Please pick a smaller image."
            }
            Base64.encodeToString(bytes, Base64.NO_WRAP)
        }
    }

    fun decodeBase64ToBitmap(base64: String): Bitmap? {
        if (base64.isBlank()) return null
        return runCatching {
            val bytes = Base64.decode(base64, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        }.getOrNull()
    }
}
