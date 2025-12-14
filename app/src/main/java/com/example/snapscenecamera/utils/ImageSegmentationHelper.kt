package com.example.snapscenecamera.utils

import android.graphics.Bitmap
import android.util.Log
import java.nio.ByteBuffer

/**
 * Image Segmentation Helper
 * Provides advanced mask processing for better segmentation quality
 */
object ImageSegmentationHelper {
    private const val TAG = "SegmentationHelper"
    
    /**
     * Smooth mask array to reduce noise using 3x3 neighborhood averaging
     */
    fun smoothMaskArray(confidences: FloatArray, width: Int, height: Int, iterations: Int = 1): FloatArray {
        var current = confidences
        
        repeat(iterations) {
            val smoothed = FloatArray(current.size)
            
            for (y in 0 until height) {
                for (x in 0 until width) {
                    val idx = y * width + x
                    
                    // 3x3 neighborhood averaging
                    var sum = 0f
                    var count = 0
                    
                    for (dy in -1..1) {
                        for (dx in -1..1) {
                            val nx = x + dx
                            val ny = y + dy
                            if (nx in 0 until width && ny in 0 until height) {
                                sum += current[ny * width + nx]
                                count++
                            }
                        }
                    }
                    
                    smoothed[idx] = sum / count
                }
            }
            
            current = smoothed
        }
        
        return current
    }
    
    /**
     * Extract confidence array from mask buffer
     */
    fun extractConfidenceArray(maskBuffer: ByteBuffer, width: Int, height: Int): FloatArray {
        val confidences = FloatArray(width * height)
        maskBuffer.rewind()
        
        for (i in confidences.indices) {
            confidences[i] = maskBuffer.float
        }
        
        maskBuffer.rewind()
        return confidences
    }
    
    /**
     * Calculate alpha value with improved edge feathering
     */
    fun calculateAlpha(confidence: Float): Int {
        return when {
            confidence > 0.7f -> 255  // Core area: fully opaque
            confidence > 0.5f -> {
                // Mid area: gradual transition
                ((confidence - 0.5f) / 0.2f * 255).toInt().coerceIn(200, 255)
            }
            confidence > 0.3f -> {
                // Edge area: soft feathering
                ((confidence - 0.3f) / 0.2f * 200).toInt().coerceIn(0, 200)
            }
            else -> 0  // Background: fully transparent
        }
    }
    
    /**
     * Apply morphological operations (erosion + dilation) to remove noise
     */
    fun morphologicalClean(confidences: FloatArray, width: Int, height: Int): FloatArray {
        // Erosion: remove small noise
        val eroded = erode(confidences, width, height, kernelSize = 2)
        // Dilation: restore original size
        val dilated = dilate(eroded, width, height, kernelSize = 2)
        return dilated
    }
    
    private fun erode(confidences: FloatArray, width: Int, height: Int, kernelSize: Int): FloatArray {
        val result = FloatArray(confidences.size)
        val radius = kernelSize / 2
        
        for (y in 0 until height) {
            for (x in 0 until width) {
                val idx = y * width + x
                var minVal = 1f
                
                for (dy in -radius..radius) {
                    for (dx in -radius..radius) {
                        val nx = (x + dx).coerceIn(0, width - 1)
                        val ny = (y + dy).coerceIn(0, height - 1)
                        minVal = minOf(minVal, confidences[ny * width + nx])
                    }
                }
                
                result[idx] = minVal
            }
        }
        
        return result
    }
    
    private fun dilate(confidences: FloatArray, width: Int, height: Int, kernelSize: Int): FloatArray {
        val result = FloatArray(confidences.size)
        val radius = kernelSize / 2
        
        for (y in 0 until height) {
            for (x in 0 until width) {
                val idx = y * width + x
                var maxVal = 0f
                
                for (dy in -radius..radius) {
                    for (dx in -radius..radius) {
                        val nx = (x + dx).coerceIn(0, width - 1)
                        val ny = (y + dy).coerceIn(0, height - 1)
                        maxVal = maxOf(maxVal, confidences[ny * width + nx])
                    }
                }
                
                result[idx] = maxVal
            }
        }
        
        return result
    }
}
