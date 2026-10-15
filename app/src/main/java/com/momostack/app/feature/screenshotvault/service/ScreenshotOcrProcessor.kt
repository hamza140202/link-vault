package com.momostack.app.feature.screenshotvault.service

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.coroutines.resume

/**
 * On-Device Text Recognition Engine powered by Google ML Kit.
 * Runs completely locally without network access, zero server fees, and <100ms inference.
 */
object ScreenshotOcrProcessor {

    private val recognizer by lazy {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    /**
     * Extracts text from a local File safely on Dispatchers.IO.
     * Automatically downscales oversized screenshots (e.g. 4K/QHD) to avoid memory spikes.
     */
    suspend fun extractTextFromFile(file: File): String = withContext(Dispatchers.IO) {
        if (!file.exists() || file.length() == 0L) return@withContext ""
        try {
            // Check dimensions and downscale if max dimension > 1920
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.absolutePath, options)
            val maxDim = maxOf(options.outWidth, options.outHeight)

            var sampleSize = 1
            while (maxDim / sampleSize > 1920) {
                sampleSize *= 2
            }

            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            val bitmap = BitmapFactory.decodeFile(file.absolutePath, decodeOptions)
                ?: return@withContext ""

            val image = InputImage.fromBitmap(bitmap, 0)
            val result = extractTextFromInputImage(image)
            bitmap.recycle()
            result
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }

    /**
     * Extracts text from an InputImage using Kotlin Coroutines suspension.
     */
    private suspend fun extractTextFromInputImage(image: InputImage): String =
        suspendCancellableCoroutine { continuation ->
            recognizer.process(image)
                .addOnSuccessListener { visionText ->
                    val rawText = visionText.text.trim()
                    continuation.resume(rawText)
                }
                .addOnFailureListener { e ->
                    e.printStackTrace()
                    continuation.resume("")
                }
        }
}
