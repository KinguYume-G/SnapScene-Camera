package com.example.snapscenecamera.utils

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Canvas
import android.graphics.Paint
import kotlin.math.max
import kotlin.math.min

object ColorCorrectionHelper {

    /**
     * Applies automatic white balance using the Gray World assumption.
     * This assumes the average color of the scene should be neutral gray.
     */
    fun autoWhiteBalance(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        var rSum: Long = 0
        var gSum: Long = 0
        var bSum: Long = 0

        for (pixel in pixels) {
            rSum += Color.red(pixel)
            gSum += Color.green(pixel)
            bSum += Color.blue(pixel)
        }

        val pixelCount = pixels.size
        val rAvg = rSum / pixelCount.toDouble()
        val gAvg = gSum / pixelCount.toDouble()
        val bAvg = bSum / pixelCount.toDouble()

        // Avoid division by zero
        if (rAvg == 0.0 || gAvg == 0.0 || bAvg == 0.0) return bitmap

        val maxAvg = max(rAvg, max(gAvg, bAvg))
        val rGain = (maxAvg / rAvg).toFloat()
        val gGain = (maxAvg / gAvg).toFloat()
        val bGain = (maxAvg / bAvg).toFloat()

        val colorMatrix = ColorMatrix().apply {
            setScale(rGain, gGain, bGain, 1f)
        }

        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        val paint = Paint().apply {
            colorFilter = ColorMatrixColorFilter(colorMatrix)
        }
        canvas.drawBitmap(bitmap, 0f, 0f, paint)

        return result
    }

    /**
     * Optimizes skin tones by slightly boosting warm colors (red/yellow) and saturation.
     * This is a simple global adjustment. For more advanced results, face detection masks should be used.
     */
    fun optimizeSkinTone(bitmap: Bitmap): Bitmap {
        // 1. Boost saturation slightly
        val saturationMatrix = ColorMatrix().apply {
            setSaturation(1.1f)
        }

        // 2. Warmth filter (boost Red, slightly reduce Blue)
        val warmthMatrix = ColorMatrix().apply {
            set(floatArrayOf(
                1.05f, 0f, 0f, 0f, 0f,  // Red
                0f, 1.02f, 0f, 0f, 0f,  // Green
                0f, 0f, 0.95f, 0f, 0f,  // Blue
                0f, 0f, 0f, 1f, 0f
            ))
        }

        // Combine matrices
        val combinedMatrix = ColorMatrix()
        combinedMatrix.postConcat(saturationMatrix)
        combinedMatrix.postConcat(warmthMatrix)

        val result = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        val paint = Paint().apply {
            colorFilter = ColorMatrixColorFilter(combinedMatrix)
        }
        canvas.drawBitmap(bitmap, 0f, 0f, paint)

        return result
    }

    /**
     * Applies both Auto White Balance and Skin Tone Optimization.
     */
    fun autoEnhance(bitmap: Bitmap): Bitmap {
        val balanced = autoWhiteBalance(bitmap)
        val enhanced = optimizeSkinTone(balanced)
        if (balanced != bitmap) balanced.recycle()
        return enhanced
    }
}
