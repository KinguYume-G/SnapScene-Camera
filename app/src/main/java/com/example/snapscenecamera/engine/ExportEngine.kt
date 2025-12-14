package com.example.snapscenecamera.engine

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

/**
 * Export Engine for Multiple Format Support
 * 
 * Supports:
 * - JPEG (High/Standard/Compressed quality)
 * - PNG (Transparent background support)
 * - WebP (Modern format, small size)
 * 
 * Features:
 * - Android 10-14 compatible
 * - MediaStore API with IS_PENDING
 * - Automatic folder creation
 * - Error handling and rollback
 * 
 * @author SnapScene Camera Team
 */
class ExportEngine(private val context: Context) {
    
    companion object {
        private const val TAG = "ExportEngine"
        private const val ALBUM_NAME = "SnapScene Camera"
    }
    
    enum class ExportFormat {
        JPEG_HIGH,      // 95% quality
        JPEG_STANDARD,  // 85% quality
        JPEG_COMPRESSED, // 70% quality
        PNG,            // Lossless with transparency
        WEBP            // Modern format
    }
    
    /**
     * Export image to gallery
     * @return URI of saved image, or null if failed
     */
    suspend fun exportImage(
        bitmap: Bitmap,
        format: ExportFormat = ExportFormat.JPEG_HIGH,
        filename: String? = null
    ): Uri? = withContext(Dispatchers.IO) {
        try {
            val (compressFormat, quality, extension) = when (format) {
                ExportFormat.JPEG_HIGH -> Triple(Bitmap.CompressFormat.JPEG, 95, "jpg")
                ExportFormat.JPEG_STANDARD -> Triple(Bitmap.CompressFormat.JPEG, 85, "jpg")
                ExportFormat.JPEG_COMPRESSED -> Triple(Bitmap.CompressFormat.JPEG, 70, "jpg")
                ExportFormat.PNG -> Triple(Bitmap.CompressFormat.PNG, 100, "png")
                ExportFormat.WEBP -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        Triple(Bitmap.CompressFormat.WEBP_LOSSY, 90, "webp")
                    } else {
                        @Suppress("DEPRECATION")
                        Triple(Bitmap.CompressFormat.WEBP, 90, "webp")
                    }
                }
            }
            
            val displayName = filename ?: "IMG_${System.currentTimeMillis()}.$extension"
            
            Log.d(TAG, "Exporting image: $displayName, format: $format, quality: $quality")
            
            val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
                put(MediaStore.Images.Media.MIME_TYPE, getMimeType(extension))
                put(MediaStore.Images.Media.RELATIVE_PATH, 
                    "${Environment.DIRECTORY_PICTURES}/$ALBUM_NAME")
                
                // ⭐ Critical: Prevent other apps from reading during write
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
            }
            
            val uri = context.contentResolver.insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues
            ) ?: run {
                Log.e(TAG, "Failed to create MediaStore entry")
                return@withContext null
            }
            
            try {
                // Write image data
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    val success = bitmap.compress(compressFormat, quality, outputStream)
                    if (!success) {
                        throw IOException("Failed to compress bitmap")
                    }
                    outputStream.flush()
                }
                
                // ⭐ Mark as completed
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    contentValues.clear()
                    contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                    context.contentResolver.update(uri, contentValues, null, null)
                }
                
                Log.d(TAG, "✅ Image exported successfully: $uri")
                uri
                
            } catch (e: Exception) {
                // Rollback: Delete the failed entry
                context.contentResolver.delete(uri, null, null)
                Log.e(TAG, "Export failed, entry deleted", e)
                throw e
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Export failed", e)
            null
        }
    }
    
    /**
     * Export with original + edited combo (Save Original feature)
     */
    suspend fun exportWithOriginal(
        editedBitmap: Bitmap,
        originalBitmap: Bitmap,
        format: ExportFormat = ExportFormat.JPEG_HIGH
    ): Pair<Uri?, Uri?> {
        val timestamp = System.currentTimeMillis()
        
        val editedUri = exportImage(
            bitmap = editedBitmap,
            format = format,
            filename = "EDITED_$timestamp.${getExtension(format)}"
        )
        
        val originalUri = exportImage(
            bitmap = originalBitmap,
            format = format,
            filename = "ORIGINAL_$timestamp.${getExtension(format)}"
        )
        
        return Pair(editedUri, originalUri)
    }
    
    private fun getMimeType(extension: String): String {
        return when (extension.lowercase()) {
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "webp" -> "image/webp"
            else -> "image/jpeg"
        }
    }
    
    private fun getExtension(format: ExportFormat): String {
        return when (format) {
            ExportFormat.JPEG_HIGH,
            ExportFormat.JPEG_STANDARD,
            ExportFormat.JPEG_COMPRESSED -> "jpg"
            ExportFormat.PNG -> "png"
            ExportFormat.WEBP -> "webp"
        }
    }
}
