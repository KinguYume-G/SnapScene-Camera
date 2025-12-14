package com.example.snapscenecamera.engine

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.min

/**
 * Powerful Filter Engine with 12 Professional Filters
 * 
 * Filters:
 * 1. Original (No filter)
 * 2. Black & White
 * 3. Retro/Sepia
 * 4. Cool Tone (Blue shift)
 * 5. Warm Tone (Yellow/Orange shift)
 * 6. Vivid (Saturation boost)
 * 7. Soft (Saturation reduce)
 * 8. High Contrast
 * 9. Japanese (Overexpose + desaturate)
 * 10. Film
 * 11. Cyberpunk (Blue/Purple tone)
 * 12. Sunset (Orange/Red gradient)
 * 
 * @author SnapScene Camera Team
 */
class FilterEngine {
    
    companion object {
        private const val TAG = "FilterEngine"
    }
    
    enum class FilterType {
        ORIGINAL,
        BLACK_WHITE,
        RETRO,
        COOL,
        WARM,
        VIVID,
        SOFT,
        HIGH_CONTRAST,
        JAPANESE,
        FILM,
        CYBERPUNK,
        SUNSET
    }
    
    /**
     * Apply filter to bitmap
     */
    suspend fun applyFilter(bitmap: Bitmap, filterType: FilterType): Bitmap = 
        withContext(Dispatchers.Default) {
            Log.d(TAG, "Applying filter: $filterType")
            
            when (filterType) {
                FilterType.ORIGINAL -> bitmap.copy(Bitmap.Config.ARGB_8888, true)
                FilterType.BLACK_WHITE -> applyBlackAndWhite(bitmap)
                FilterType.RETRO -> applyRetro(bitmap)
                FilterType.COOL -> applyCoolTone(bitmap)
                FilterType.WARM -> applyWarmTone(bitmap)
                FilterType.VIVID -> applyVivid(bitmap)
                FilterType.SOFT -> applySoft(bitmap)
                FilterType.HIGH_CONTRAST -> applyHighContrast(bitmap)
                FilterType.JAPANESE -> applyJapanese(bitmap)
                FilterType.FILM -> applyFilm(bitmap)
                FilterType.CYBERPUNK -> applyCyberpunk(bitmap)
                FilterType.SUNSET -> applySunset(bitmap)
            }
        }
    
    /**
     * 1. Black & White Filter
     */
    private fun applyBlackAndWhite(source: Bitmap): Bitmap {
        val width = source.width
        val height = source.height
        val result = Bitmap.createBitmap(width, height, source.config ?: Bitmap.Config.ARGB_8888)
        
        for (y in 0 until height) {
            for (x in 0 until width) {
                val pixel = source.getPixel(x, y)
                val r = Color.red(pixel)
                val g = Color.green(pixel)
                val b = Color.blue(pixel)
                val a = Color.alpha(pixel)
                
                // Weighted grayscale conversion
                val gray = (0.299 * r + 0.587 * g + 0.114 * b).toInt()
                
                result.setPixel(x, y, Color.argb(a, gray, gray, gray))
            }
        }
        
        return result
    }
    
    /**
     * 2. Retro/Sepia Filter
     */
    private fun applyRetro(source: Bitmap): Bitmap {
        val width = source.width
        val height = source.height
        val result = Bitmap.createBitmap(width, height, source.config ?: Bitmap.Config.ARGB_8888)
        
        for (y in 0 until height) {
            for (x in 0 until width) {
                val pixel = source.getPixel(x, y)
                val r = Color.red(pixel)
                val g = Color.green(pixel)
                val b = Color.blue(pixel)
                val a = Color.alpha(pixel)
                
                val tr = (0.393 * r + 0.769 * g + 0.189 * b).toInt().coerceIn(0, 255)
                val tg = (0.349 * r + 0.686 * g + 0.168 * b).toInt().coerceIn(0, 255)
                val tb = (0.272 * r + 0.534 * g + 0.131 * b).toInt().coerceIn(0, 255)
                
                result.setPixel(x, y, Color.argb(a, tr, tg, tb))
            }
        }
        
        return result
    }
    
    /**
     * 3. Cool Tone (Blue shift)
     */
    private fun applyCoolTone(source: Bitmap): Bitmap {
        return adjustColorTemperature(source, -0.15f)
    }
    
    /**
     * 4. Warm Tone (Yellow/Orange shift)
     */
    private fun applyWarmTone(source: Bitmap): Bitmap {
        return adjustColorTemperature(source, 0.15f)
    }
    
    /**
     * 5. Vivid (Saturation +30%)
     */
    private fun applyVivid(source: Bitmap): Bitmap {
        return adjustSaturation(source, 1.3f)
    }
    
    /**
     * 6. Soft (Saturation -20%)
     */
    private fun applySoft(source: Bitmap): Bitmap {
        return adjustSaturation(source, 0.8f)
    }
    
    /**
     * 7. High Contrast
     */
    private fun applyHighContrast(source: Bitmap): Bitmap {
        val width = source.width
        val height = source.height
        val result = Bitmap.createBitmap(width, height, source.config ?: Bitmap.Config.ARGB_8888)
        
        val contrast = 1.5f
        val factor = (259f * (contrast + 255f)) / (255f * (259f - contrast))
        
        for (y in 0 until height) {
            for (x in 0 until width) {
                val pixel = source.getPixel(x, y)
                val r = Color.red(pixel)
                val g = Color.green(pixel)
                val b = Color.blue(pixel)
                val a = Color.alpha(pixel)
                
                val nr = (factor * (r - 128) + 128).toInt().coerceIn(0, 255)
                val ng = (factor * (g - 128) + 128).toInt().coerceIn(0, 255)
                val nb = (factor * (b - 128) + 128).toInt().coerceIn(0, 255)
                
                result.setPixel(x, y, Color.argb(a, nr, ng, nb))
            }
        }
        
        return result
    }
    
    /**
     * 8. Japanese (Overexpose + Desaturate)
     */
    private fun applyJapanese(source: Bitmap): Bitmap {
        val width = source.width
        val height = source.height
        val result = Bitmap.createBitmap(width, height, source.config ?: Bitmap.Config.ARGB_8888)
        
        for (y in 0 until height) {
            for (x in 0 until width) {
                val pixel = source.getPixel(x, y)
                var r = Color.red(pixel)
                var g = Color.green(pixel)
                var b = Color.blue(pixel)
                val a = Color.alpha(pixel)
                
                // Overexposure
                r = (r + 25).coerceIn(0, 255)
                g = (g + 25).coerceIn(0, 255)
                b = (b + 25).coerceIn(0, 255)
                
                // Desaturate
                val gray = (0.299 * r + 0.587 * g + 0.114 * b).toInt()
                r = (r * 0.85f + gray * 0.15f).toInt()
                g = (g * 0.85f + gray * 0.15f).toInt()
                b = (b * 0.85f + gray * 0.15f).toInt()
                
                result.setPixel(x, y, Color.argb(a, r, g, b))
            }
        }
        
        return result
    }
    
    /**
     * 9. Film Filter
     */
    private fun applyFilm(source: Bitmap): Bitmap {
        val width = source.width
        val height = source.height
        val result = Bitmap.createBitmap(width, height, source.config ?: Bitmap.Config.ARGB_8888)
        
        for (y in 0 until height) {
            for (x in 0 until width) {
                val pixel = source.getPixel(x, y)
                val r = Color.red(pixel)
                val g = Color.green(pixel)
                val b = Color.blue(pixel)
                val a = Color.alpha(pixel)
                
                // Film-like color grading
                val nr = (r * 1.1f - 10).toInt().coerceIn(0, 255)
                val ng = (g * 0.95f).toInt().coerceIn(0, 255)
                val nb = (b * 1.05f + 5).toInt().coerceIn(0, 255)
                
                result.setPixel(x, y, Color.argb(a, nr, ng, nb))
            }
        }
        
        return result
    }
    
    /**
     * 10. Cyberpunk (Blue/Purple tone)
     */
    private fun applyCyberpunk(source: Bitmap): Bitmap {
        val width = source.width
        val height = source.height
        val result = Bitmap.createBitmap(width, height, source.config ?: Bitmap.Config.ARGB_8888)
        
        for (y in 0 until height) {
            for (x in 0 until width) {
                val pixel = source.getPixel(x, y)
                val r = Color.red(pixel)
                val g = Color.green(pixel)
                val b = Color.blue(pixel)
                val a = Color.alpha(pixel)
                
                // Cyberpunk cyber color scheme
                val nr = (r * 0.7f + 30).toInt().coerceIn(0, 255)
                val ng = (g * 0.8f + 20).toInt().coerceIn(0, 255)
                val nb = (b * 1.3f + 40).toInt().coerceIn(0, 255)
                
                result.setPixel(x, y, Color.argb(a, nr, ng, nb))
            }
        }
        
        return result
    }
    
    /**
     * 11. Sunset (Orange/Red gradient)
     */
    private fun applySunset(source: Bitmap): Bitmap {
        val width = source.width
        val height = source.height
        val result = Bitmap.createBitmap(width, height, source.config ?: Bitmap.Config.ARGB_8888)
        
        for (y in 0 until height) {
            for (x in 0 until width) {
                val pixel = source.getPixel(x, y)
                val r = Color.red(pixel)
                val g = Color.green(pixel)
                val b = Color.blue(pixel)
                val a = Color.alpha(pixel)
                
                // Sunset warm tones
                val nr = (r * 1.2f + 20).toInt().coerceIn(0, 255)
                val ng = (g * 0.9f + 10).toInt().coerceIn(0, 255)
                val nb = (b * 0.7f - 10).toInt().coerceIn(0, 255)
                
                result.setPixel(x, y, Color.argb(a, nr, ng, nb))
            }
        }
        
        return result
    }
    
    // Helper functions
    
    private fun adjustColorTemperature(source: Bitmap, temp: Float): Bitmap {
        val width = source.width
        val height = source.height
        val result = Bitmap.createBitmap(width, height, source.config ?: Bitmap.Config.ARGB_8888)
        
        for (y in 0 until height) {
            for (x in 0 until width) {
                val pixel = source.getPixel(x, y)
                var r = Color.red(pixel)
                var g = Color.green(pixel)
                var b = Color.blue(pixel)
                val a = Color.alpha(pixel)
                
                if (temp > 0) {
                    // Warm
                    r = (r + temp * 50).toInt().coerceIn(0, 255)
                    g = (g + temp * 30).toInt().coerceIn(0, 255)
                } else {
                    // Cool
                    b = (b - temp * 50).toInt().coerceIn(0, 255)
                    g = (g - temp * 20).toInt().coerceIn(0, 255)
                }
                
                result.setPixel(x, y, Color.argb(a, r, g, b))
            }
        }
        
        return result
    }
    
    private fun adjustSaturation(source: Bitmap, saturation: Float): Bitmap {
        val width = source.width
        val height = source.height
        val result = Bitmap.createBitmap(width, height, source.config ?: Bitmap.Config.ARGB_8888)
        
        for (y in 0 until height) {
            for (x in 0 until width) {
                val pixel = source.getPixel(x, y)
                val r = Color.red(pixel)
                val g = Color.green(pixel)
                val b = Color.blue(pixel)
                val a = Color.alpha(pixel)
                
                val gray = (0.299 * r + 0.587 * g + 0.114 * b).toInt()
                
                val nr = (gray + saturation * (r - gray)).toInt().coerceIn(0, 255)
                val ng = (gray + saturation * (g - gray)).toInt().coerceIn(0, 255)
                val nb = (gray + saturation * (b - gray)).toInt().coerceIn(0, 255)
                
                result.setPixel(x, y, Color.argb(a, nr, ng, nb))
            }
        }
        
        return result
    }
}
