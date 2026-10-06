package com.xraiassistant.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.util.Base64
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Longest edge a list thumbnail needs; screenshots are downsampled to roughly this. */
private const val THUMBNAIL_TARGET_PX = 320

/**
 * Decodes a base64 screenshot (with or without a data: URL prefix) off the main
 * thread, downsampled for a list thumbnail. Null until decoded, and null if the
 * data is missing or unreadable.
 */
@Composable
fun rememberBase64Thumbnail(base64: String?): State<ImageBitmap?> =
    produceState<ImageBitmap?>(initialValue = null, base64) {
        if (base64.isNullOrEmpty()) {
            value = null
            return@produceState
        }
        value = withContext(Dispatchers.Default) { decodeThumbnail(base64) }
    }

/**
 * True when the image is essentially one flat dark colour. A scene captured before
 * its canvas drew anything comes back black; the placeholder reads better.
 */
private fun Bitmap.isBlank(): Boolean {
    val steps = 6
    var brightest = 0
    for (i in 1 until steps) for (j in 1 until steps) {
        val pixel = getPixel(width * i / steps, height * j / steps)
        val luma = (299 * Color.red(pixel) + 587 * Color.green(pixel) + 114 * Color.blue(pixel)) / 1000
        if (luma > brightest) brightest = luma
    }
    return brightest < 12
}

private fun decodeThumbnail(base64: String): ImageBitmap? = try {
    val bytes = Base64.decode(base64.substringAfter("base64,"), Base64.DEFAULT)

    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)

    var sampleSize = 1
    while (maxOf(bounds.outWidth, bounds.outHeight) / (sampleSize * 2) >= THUMBNAIL_TARGET_PX) {
        sampleSize *= 2
    }

    val options = BitmapFactory.Options().apply { inSampleSize = sampleSize }
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
        ?.takeUnless { it.isBlank() }
        ?.asImageBitmap()
} catch (e: Exception) {
    println("⚠️ Failed to decode thumbnail: ${e.message}")
    null
}
