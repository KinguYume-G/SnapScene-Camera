package com.example.snapscenecamera

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.snapscenecamera.databinding.ActivityEditBinding
import com.example.snapscenecamera.engine.ExportEngine
import com.example.snapscenecamera.utils.ImageSegmentationHelper
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.segmentation.Segmentation
import com.google.mlkit.vision.segmentation.SegmentationMask
import com.google.mlkit.vision.segmentation.selfie.SelfieSegmenterOptions
import com.yalantis.ucrop.UCrop
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class EditActivity : AppCompatActivity() {
    private lateinit var binding: ActivityEditBinding
    
    private var currentImageUri: Uri? = null
    
    // Image processing
    private var originalBitmap: Bitmap? = null
    private var foregroundBitmap: Bitmap? = null
    private var customBackgroundBitmap: Bitmap? = null
    private var enhancedBitmap: Bitmap? = null
    private var currentMask: SegmentationMask? = null
    
    // Background state
    private var currentBackgroundColor: Int = Color.WHITE
    private var currentBackgroundDrawableId: Int? = null
    private var currentBackgroundItem: BackgroundItem = BackgroundItem.Original
    private var isEnhanced = false
    
    // Engine
    private lateinit var exportEngine: ExportEngine
    
    // Foreground state
    private var isAdjustMode = false
    private var foregroundScale = 1.0f
    private var foregroundTranslateX = 0f
    private var foregroundTranslateY = 0f
    
    // Background items - 世界旅游景点背景
    private val backgroundItems by lazy {
        val items = mutableListOf<BackgroundItem>()
        items.add(BackgroundItem.Original)
        
        // 纯色背景
        val colors = listOf(
            Color.WHITE,          // 白色
            Color.BLACK,          // 黑色
            Color.parseColor("#4A90E2")  // 证件照蓝
        )
        items.addAll(colors.map { color -> BackgroundItem.ColorItem(color) })
        
        // 新增国家 - 游客照背景 (Batch 1 & 2)
        items.add(BackgroundItem.ImageItem(R.drawable.bg_australia_sydney)) // 澳大利亚
        items.add(BackgroundItem.ImageItem(R.drawable.bg_korea_palace))    // 韩国
        items.add(BackgroundItem.ImageItem(R.drawable.bg_uk_london))        // 英国
        items.add(BackgroundItem.ImageItem(R.drawable.bg_usa_liberty))      // 美国
        items.add(BackgroundItem.ImageItem(R.drawable.bg_germany_castle))   // 德国
        items.add(BackgroundItem.ImageItem(R.drawable.bg_spain_sagrada))    // 西班牙
        items.add(BackgroundItem.ImageItem(R.drawable.bg_france_eiffel))    // 法国
        items.add(BackgroundItem.ImageItem(R.drawable.bg_italy_colosseum))  // 意大利
        items.add(BackgroundItem.ImageItem(R.drawable.bg_japan_kyoto))      // 日本京都
        
        // 马来西亚
        items.add(BackgroundItem.ImageItem(R.drawable.bg_malaysia_towers))
        items.add(BackgroundItem.ImageItem(R.drawable.bg_kl_towers))
        items.add(BackgroundItem.ImageItem(R.drawable.bg_malaysia_mosque))
        items.add(BackgroundItem.ImageItem(R.drawable.bg_malaysia_batu))
        items.add(BackgroundItem.ImageItem(R.drawable.bg_malaysia_penang))
        items.add(BackgroundItem.ImageItem(R.drawable.bg_malaysia_beach))
        
        // 日本
        items.add(BackgroundItem.ImageItem(R.drawable.bg_japan_fuji))
        items.add(BackgroundItem.ImageItem(R.drawable.bg_japan_osaka))
        items.add(BackgroundItem.ImageItem(R.drawable.bg_japan_tokyo_tower))
        
        // 中国
        items.add(BackgroundItem.ImageItem(R.drawable.bg_china_great_wall))
        items.add(BackgroundItem.ImageItem(R.drawable.bg_china_forbidden_city))
        items.add(BackgroundItem.ImageItem(R.drawable.bg_china_shanghai_bund))
        
        // 新加坡
        items.add(BackgroundItem.ImageItem(R.drawable.bg_singapore_mbs))
        items.add(BackgroundItem.ImageItem(R.drawable.bg_singapore_merlion))
        items.add(BackgroundItem.ImageItem(R.drawable.bg_singapore_gardens))
        
        Log.d(TAG, "Loaded ${items.size} backgrounds")
        items
    }
    // Crop image launcher
    private val cropImageLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val resultUri = UCrop.getOutput(result.data!!)
                if (resultUri != null) {
                    currentImageUri = resultUri
                    loadImage()
                }
            }
        }
    
    // Custom background fromgallery
    private val customBackgroundLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val uri = result.data?.data ?: return@registerForActivityResult
            
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    val backgroundBitmap = MediaStore.Images.Media.getBitmap(contentResolver, uri)
                    
                    withContext(Dispatchers.Main) {
                        // Store custom background
                        customBackgroundBitmap?.recycle()
                        customBackgroundBitmap = backgroundBitmap
                        
                        // Set special marker for custom background
                        currentBackgroundColor = -2  // Special value for custom background
                        currentBackgroundDrawableId = null // Clear preset drawable
                        currentBackgroundItem = BackgroundItem.Original // Reset selection state
                        
                        val drawable = android.graphics.drawable.BitmapDrawable(resources, backgroundBitmap)
                        binding.backgroundLayer.background = drawable
                        binding.ivForeground.visibility = View.VISIBLE
                        
                        // ** FIX: Set foreground bitmap **
                        foregroundBitmap?.let {
                            binding.ivForeground.setImageBitmap(it)
                        }
                        binding.ivForeground.bringToFront()
                        
                        // Reset RecyclerView selection
                        binding.rvBackgrounds.adapter?.notifyDataSetChanged()
                        
                        Toast.makeText(this@EditActivity, "Custom background loaded", Toast.LENGTH_SHORT).show()
                        Log.d(TAG, "Custom background loaded successfully")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to load custom background", e)
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@EditActivity, "Failed to load background", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate: EditActivity started")
        
        binding = ActivityEditBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // 调整按钮点击事件
        binding.btnAdjust.setOnClickListener {
            isAdjustMode = !isAdjustMode
            updateAdjustModeUI()
            Log.d(TAG, "Adjust mode toggled: $isAdjustMode")
        }
        
        // 初始化手势控制
        setupForegroundGestures()

        
        exportEngine = ExportEngine(this)
        
        setupUI()
        loadImage()
    }
    
    // Note: loadImage and startCrop are defined below
    
    private fun setupUI() {
        // Background color selector
        binding.rvBackgrounds.adapter = BackgroundAdapter(backgroundItems) { item ->
            currentBackgroundItem = item
            customBackgroundBitmap = null  // Clear custom background when selecting preset
            updateBackground(item)
        }
        
        // Get default background from settings
        val prefs = getSharedPreferences("AppSettings", android.content.Context.MODE_PRIVATE)
        // Default to Original if not set
        updateBackground(BackgroundItem.Original)
        Log.d(TAG, "setupUI: Applied default background")
        
        binding.btnCancel.setOnClickListener {
            finish()
        }
        
        binding.btnSave.setOnClickListener {
            saveImage()  // FIX ISSUE 5 - Will call format dialog
        }
        
        binding.btnCustomBackground.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            customBackgroundLauncher.launch(intent)
        }
        
        binding.btnCrop.setOnClickListener {
            startCrop()
        }
        
        binding.btnAutoEnhance.setOnClickListener {
            toggleAutoEnhance()
        }
        
        // 调整按钮：只能移动和缩放，禁止旋转，慢速响应
        // Initial Adjust UI state
        updateAdjustModeUI()
        
        binding.btnShare.setOnClickListener {
            shareImage()
        }
    }
    
    private fun startCrop() {
        val sourceUri = currentImageUri ?: return
        
        val destinationFileName = "cropped_${System.currentTimeMillis()}.jpg"
        val destinationUri = Uri.fromFile(File(cacheDir, destinationFileName))
        
        val options = UCrop.Options()
        options.setCompressionQuality(90)
        options.setFreeStyleCropEnabled(true)
        options.setToolbarTitle("Crop & Rotate")
        
        val uCrop = UCrop.of(sourceUri, destinationUri)
            .withOptions(options)
            
        cropImageLauncher.launch(uCrop.getIntent(this))
    }

    private fun loadImage() {
        if (currentImageUri == null) {
            val imageUriString = intent.getStringExtra("image_uri")
            if (imageUriString != null) {
                currentImageUri = Uri.parse(imageUriString)
            }
        }
        
        Log.d(TAG, "loadImage: Current URI = $currentImageUri")
        
        if (currentImageUri == null) {
            Log.e(TAG, "loadImage: Image URI is null")
            Toast.makeText(this, "Image not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        
        val imageUri = currentImageUri!!
        binding.progressBar.visibility = View.VISIBLE
        binding.tvLoading.visibility = View.VISIBLE
        
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                Log.d(TAG, "loadImage: Starting to load and compress image")
                val bitmap = compressImage(imageUri)
                
                if (bitmap == null) {
                    Log.e(TAG, "loadImage: Failed to load bitmap")
                    withContext(Dispatchers.Main) {
                        binding.progressBar.visibility = View.GONE
                        binding.tvLoading.visibility = View.GONE
                        Toast.makeText(this@EditActivity, "Failed to load image", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                    return@launch
                }
                
                Log.d(TAG, "loadImage: Bitmap loaded, size=${bitmap.width}x${bitmap.height}")
                originalBitmap = bitmap
                
                withContext(Dispatchers.Main) {
                    processImageWithMLKit(bitmap)
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "loadImage: Exception", e)
                withContext(Dispatchers.Main) {
                    binding.progressBar.visibility = View.GONE
                    binding.tvLoading.visibility = View.GONE
                    Toast.makeText(this@EditActivity, "Failed to load image: ${e.message}", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        }
    }
    
    private fun compressImage(uri: Uri): Bitmap? {
        return try {
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            
            contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, options)
            }
            
            val maxWidth = 1080
            options.inSampleSize = calculateInSampleSize(options, maxWidth)
            options.inJustDecodeBounds = false
            options.inPreferredConfig = Bitmap.Config.ARGB_8888
            
            var bitmap = contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, options)
            }
            
            // Fix EXIF orientation
            if (bitmap != null) {
                try {
                    contentResolver.openInputStream(uri)?.use { stream ->
                        val exif = androidx.exifinterface.media.ExifInterface(stream)
                        val orientation = exif.getAttributeInt(
                            androidx.exifinterface.media.ExifInterface.TAG_ORIENTATION,
                            androidx.exifinterface.media.ExifInterface.ORIENTATION_NORMAL
                        )
                        
                        val matrix = android.graphics.Matrix()
                        when (orientation) {
                            androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                            androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                            androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                        }
                        
                        if (!matrix.isIdentity) {
                            val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
                            bitmap.recycle()
                            bitmap = rotated
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "EXIF processing failed", e)
                }
            }
            
            bitmap
        } catch (e: Exception) {
            Log.e(TAG, "Failed to compress image", e)
            null
        }
    }
    
    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int): Int {
        val width = options.outWidth
        var inSampleSize = 1
        
        if (width > reqWidth) {
            val halfWidth = width / 2
            while (halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        
        return inSampleSize
    }
    
    private fun processImageWithMLKit(bitmap: Bitmap) {
        lifecycleScope.launch {
            try {
                val options = SelfieSegmenterOptions.Builder()
                    .setDetectorMode(SelfieSegmenterOptions.SINGLE_IMAGE_MODE)
                    .build()
                
                val segmenter = Segmentation.getClient(options)
                val inputImage = InputImage.fromBitmap(bitmap, 0)
                
                segmenter.process(inputImage)
                    .addOnSuccessListener { mask ->
                        Log.d(TAG, "Segmentation successful")
                        currentMask = mask // Store mask for re-use
                        try {
                            val targetBitmap = if (isEnhanced && enhancedBitmap != null) enhancedBitmap!! else bitmap
                            val foreground = createForegroundBitmap(targetBitmap, mask)
                            foregroundBitmap = foreground
                            
                            // Update UI
                            if (currentBackgroundColor == Int.MAX_VALUE) {
                                updateBackground(BackgroundItem.Original)
                            } else {
                                updateBackground(currentBackgroundItem)
                            }
                            
                            // Hide loading indicators
                            binding.progressBar.visibility = View.GONE
                            binding.tvLoading.visibility = View.GONE
                            
                        } catch (e: Exception) {
                            Log.e(TAG, "Error creating foreground", e)
                            handleMLKitFailure(bitmap)
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Segmentation failed", e)
                        handleMLKitFailure(bitmap)
                    }
                
            } catch (e: Exception) {
                Log.e(TAG, "ML Kit exception", e)
                handleMLKitFailure(bitmap)
            }
        }
    }
    
    private fun handleMLKitFailure(bitmap: Bitmap) {
        binding.progressBar.visibility = View.GONE
        binding.tvLoading.visibility = View.GONE
        
        foregroundBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        binding.ivForeground.setImageBitmap(foregroundBitmap)
        updateBackground(currentBackgroundItem)
        
        Toast.makeText(
            this,
            "AI segmentation failed. Showing original image.",
            Toast.LENGTH_LONG
        ).show()
    }
    
    // FIX ISSUE 1: Improved segmentation with mask smoothing
    private fun createForegroundBitmap(bitmap: Bitmap, mask: SegmentationMask): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val maskWidth = mask.width
        val maskHeight = mask.height
        
        Log.d(TAG, "createForegroundBitmap: Bitmap=${width}x${height}, Mask=${maskWidth}x${maskHeight}")
        
        val foreground = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val maskBuffer = mask.buffer
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        
        maskBuffer.rewind()
        
        // Extract and smooth mask for better quality
        val rawConfidences = ImageSegmentationHelper.extractConfidenceArray(maskBuffer, maskWidth, maskHeight)
        val smoothedConfidences = ImageSegmentationHelper.smoothMaskArray(rawConfidences, maskWidth, maskHeight, iterations = 2)
        
        // Process pixels
        if (width == maskWidth && height == maskHeight) {
            for (i in 0 until width * height) {
                val confidence = smoothedConfidences[i]
                val alpha = ImageSegmentationHelper.calculateAlpha(confidence)
                pixels[i] = (alpha shl 24) or (pixels[i] and 0x00FFFFFF)
            }
        } else {
            // Size mismatch - need interpolation
            val scaleX = maskWidth.toFloat() / width
            val scaleY = maskHeight.toFloat() / height
            
            for (y in 0 until height) {
                for (x in 0 until width) {
                    val maskX = (x * scaleX).toInt().coerceIn(0, maskWidth - 1)
                    val maskY = (y * scaleY).toInt().coerceIn(0, maskHeight - 1)
                    val maskIndex = maskY * maskWidth + maskX
                    
                    val confidence = smoothedConfidences[maskIndex]
                    val alpha = ImageSegmentationHelper.calculateAlpha(confidence)
                    val pixelIndex = y * width + x
                    pixels[pixelIndex] = (alpha shl 24) or (pixels[pixelIndex] and 0x00FFFFFF)
                }
            }
        }
        
        foreground.setPixels(pixels, 0, width, 0, 0, width, height)
        
        val visiblePixels = pixels.count { (it ushr 24) > 0 }
        Log.d(TAG, "createForegroundBitmap: Visible=${visiblePixels}/${pixels.size}")
        
        return foreground
    }
    
    private fun updateBackground(item: BackgroundItem) {
        
        when (item) {
            is BackgroundItem.Original -> {
                currentBackgroundColor = Int.MAX_VALUE
                currentBackgroundDrawableId = null
                customBackgroundBitmap = null
                
                binding.ivForeground.visibility = View.GONE
                binding.backgroundLayer.visibility = View.VISIBLE
                originalBitmap?.let {
                    val drawable = android.graphics.drawable.BitmapDrawable(resources, it)
                    binding.backgroundLayer.background = drawable
                }
            }
            is BackgroundItem.ColorItem -> {
                currentBackgroundColor = item.color
                currentBackgroundDrawableId = null
                customBackgroundBitmap = null
                
                binding.ivForeground.visibility = View.VISIBLE
                binding.backgroundLayer.visibility = View.VISIBLE
                binding.backgroundLayer.setBackgroundColor(item.color)
                
                // ** FIX: Set foreground bitmap **
                foregroundBitmap?.let {
                    binding.ivForeground.setImageBitmap(it)
                }
                binding.ivForeground.bringToFront()
            }
            is BackgroundItem.ImageItem -> {
                currentBackgroundColor = -3 // Special value for preset image
                currentBackgroundDrawableId = item.drawableId
                customBackgroundBitmap = null
                
                binding.ivForeground.visibility = View.VISIBLE
                binding.backgroundLayer.visibility = View.VISIBLE
                binding.backgroundLayer.setBackgroundResource(item.drawableId)
                
                // ** FIX: Set foreground bitmap **
                foregroundBitmap?.let {
                    binding.ivForeground.setImageBitmap(it)
                }
                binding.ivForeground.bringToFront()
            }
        }
    }
    
    // FIX ISSUE 5: Format selection dialog
    private fun saveImage() {
        if (originalBitmap == null) {
            Toast.makeText(this, "Image not ready", Toast.LENGTH_SHORT).show()
            return
        }
        
        Log.d(TAG, "saveImage: Preparing export")
        binding.btnSave.isEnabled = false
        
        try {
            val finalBitmap = compositeFinalImage()
            showExportFormatDialog(finalBitmap)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to prepare image", e)
            Toast.makeText(this, "Failed to prepare image: ${e.message}", Toast.LENGTH_SHORT).show()
            binding.btnSave.isEnabled = true
        }
    }
    
    private fun shareImage() {
        if (originalBitmap == null) {
            Toast.makeText(this, "Image not ready", Toast.LENGTH_SHORT).show()
            return
        }
        
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val finalBitmap = compositeFinalImage()
                val file = File(externalCacheDir, "share_image.jpg")
                val stream = java.io.FileOutputStream(file)
                finalBitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream)
                stream.close()
                
                val uri = androidx.core.content.FileProvider.getUriForFile(
                    this@EditActivity,
                    "${packageName}.fileprovider",
                    file
                )
                
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "image/jpeg"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                
                withContext(Dispatchers.Main) {
                    startActivity(Intent.createChooser(intent, "Share Image"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Share failed", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@EditActivity, "Share failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    private fun showExportFormatDialog(bitmap: Bitmap) {
        val formats = arrayOf(
            "JPEG High (95%)",
            "JPEG Standard (85%)",
            "JPEG Compressed (70%)",
            "PNG Lossless",
            "WebP High Quality"
        )
        
        AlertDialog.Builder(this)
            .setTitle("Select Export Format")
            .setItems(formats) { _, which ->
                lifecycleScope.launch(Dispatchers.IO) {
                    try {
                        val format = when (which) {
                            0 -> ExportEngine.ExportFormat.JPEG_HIGH
                            1 -> ExportEngine.ExportFormat.JPEG_STANDARD
                            2 -> ExportEngine.ExportFormat.JPEG_COMPRESSED
                            3 -> ExportEngine.ExportFormat.PNG
                            4 -> ExportEngine.ExportFormat.WEBP
                            else -> ExportEngine.ExportFormat.JPEG_HIGH
                        }
                        
                        val uri = exportEngine.exportImage(bitmap, format)
                        
                        // **  SAVE ORIGINAL FEATURE **
                        // Check if we should also save original image
                        val prefs = getSharedPreferences("AppSettings", android.content.Context.MODE_PRIVATE)
                        val saveOriginal = prefs.getBoolean("save_original", false)
                        
                        var originalSaved = false
                        if (saveOriginal && originalBitmap != null && uri != null) {
                            try {
                                val timestamp = System.currentTimeMillis()
                                val extension = when (format) {
                                    ExportEngine.ExportFormat.PNG -> "png"
                                    ExportEngine.ExportFormat.WEBP -> "webp"
                                    else -> "jpg"
                                }
                                val originalUri = exportEngine.exportImage(
                                    originalBitmap!!, 
                                    format,
                                    "IMG_${timestamp}_original.$extension"
                                )
                                originalSaved = originalUri != null
                                if (originalSaved) {
                                    Log.d(TAG, "? Original image also saved: $originalUri")
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "Failed to save original image", e)
                            }
                        }
                        
                        withContext(Dispatchers.Main) {
                            if (uri != null) {
                                val message = if (originalSaved) {
                                    getString(R.string.save_2_images)
                                } else {
                                    getString(R.string.save_success)
                                }
                                Toast.makeText(this@EditActivity, message, Toast.LENGTH_SHORT).show()
                                binding.btnSave.isEnabled = true
                                finish()
                            } else {
                                Toast.makeText(this@EditActivity, getString(R.string.save_failed), Toast.LENGTH_SHORT).show()
                                binding.btnSave.isEnabled = true
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Export failed", e)
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@EditActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                            binding.btnSave.isEnabled = true
                        }
                    }
                }
            }
            .setNegativeButton("Cancel") { _, _ ->
                binding.btnSave.isEnabled = true
            }
            .setOnCancelListener {
                binding.btnSave.isEnabled = true
            }
            .show()
    }
    
    // FIX ISSUE 2 & 4: Composite with color harmony and custom background support
    private fun compositeFinalImage(): Bitmap {
        val foreground = foregroundBitmap
        
        // FIX ISSUE 3: Handle original mode
        if (currentBackgroundColor == Int.MAX_VALUE && originalBitmap != null) {
            Log.d(TAG, "compositeFinalImage: ORIGINAL MODE")
            return originalBitmap!!.copy(Bitmap.Config.ARGB_8888, false)
        }
        
        if (foreground == null) {
            return originalBitmap?.copy(Bitmap.Config.ARGB_8888, false) 
                ?: throw IllegalStateException("No image available")
        }
        
        // Create result bitmap
        val result = Bitmap.createBitmap(
            foreground.width,
            foreground.height,
            Bitmap.Config.ARGB_8888
        )
        
        val canvas = Canvas(result)
        
        // FIX ISSUE 4: Handle custom background image
        if (currentBackgroundColor == -2 && customBackgroundBitmap != null) {
            Log.d(TAG, "compositeFinalImage: Custom background mode")
            val scaledBg = Bitmap.createScaledBitmap(
                customBackgroundBitmap!!,
                foreground.width,
                foreground.height,
                true
            )
            canvas.drawBitmap(scaledBg, 0f, 0f, null)
            scaledBg.recycle()
        } else if (currentBackgroundColor == -3 && currentBackgroundDrawableId != null) {
            // Handle preset image background
            Log.d(TAG, "compositeFinalImage: Preset image mode")
            val drawable = ContextCompat.getDrawable(this, currentBackgroundDrawableId!!)
            if (drawable != null) {
                // Convert drawable to bitmap
                val bgBitmap = Bitmap.createBitmap(foreground.width, foreground.height, Bitmap.Config.ARGB_8888)
                val bgCanvas = Canvas(bgBitmap)
                drawable.setBounds(0, 0, bgCanvas.width, bgCanvas.height)
                drawable.draw(bgCanvas)
                
                canvas.drawBitmap(bgBitmap, 0f, 0f, null)
                bgBitmap.recycle()
            } else {
                canvas.drawColor(Color.BLACK) // Fallback
            }
        } else {
            // Solid color background
            canvas.drawColor(currentBackgroundColor)
        }
        
        // FIX ISSUE 2: Apply color harmony adjustment
        val adjustedForeground = applyColorHarmony(foreground, currentBackgroundColor)

    // 应用用户调整的缩放和平移变换 (WYSIWYG - 所见即所得)
    val matrix = android.graphics.Matrix()
    
    // 计算中心点
    val centerX = adjustedForeground.width / 2f
    val centerY = adjustedForeground.height / 2f
    val canvasCenterX = result.width / 2f
    val canvasCenterY = result.height / 2f
    
    // 按顺序应用变换（以中心为基准缩放，然后应用平移）
    matrix.postTranslate(-centerX, -centerY)  // 1. 移到原点
    matrix.postScale(foregroundScale, foregroundScale)  // 2. 应用缩放
    matrix.postTranslate(canvasCenterX, canvasCenterY)  // 3. 移到画布中心
    matrix.postTranslate(foregroundTranslateX, foregroundTranslateY)  // 4. 应用用户的平移调整
    
    Log.d(TAG, "compositeFinalImage: scale=$foregroundScale, translateX=$foregroundTranslateX, translateY=$foregroundTranslateY")
    
    canvas.drawBitmap(adjustedForeground, matrix, null)
        
        if (adjustedForeground != foreground) {
            adjustedForeground.recycle()
        }
        
        return result
    }
    
    // FIX ISSUE 2: Color harmony implementation
    private fun applyColorHarmony(foreground: Bitmap, backgroundColor: Int): Bitmap {
        if (backgroundColor == -2 || backgroundColor == -3 || backgroundColor == Int.MAX_VALUE) {
            return foreground  // No adjustment for custom/preset/original mode for now
        }
        
        // Analyze background color
        val bgHSV = FloatArray(3)
        Color.colorToHSV(backgroundColor, bgHSV)
        
        val hue = bgHSV[0]
        val saturation = bgHSV[1]
        val value = bgHSV[2]
        
        // Determine warm/cool tone
        val isWarm = hue in 0f..60f || hue >= 300f
        val isCool = hue in 180f..300f
        
        // Create color adjustment matrix
        val colorMatrix = ColorMatrix()
        
        // Slight color shift based on background
        if (isWarm) {
            // Warm background -> add slight warm tone to foreground
            colorMatrix.setScale(1.02f, 0.99f, 0.97f, 1.0f)  // Boost red slightly
        } else if (isCool) {
            // Cool background -> add slight cool tone to foreground
            colorMatrix.setScale(0.97f, 0.99f, 1.02f, 1.0f)  // Boost blue slightly
        }
        
        // Brightness adjustment
        val brightnessDiff = value - 0.5f
        if (Math.abs(brightnessDiff) > 0.2f) {
            val adjustment = brightnessDiff * 0.15f  // Subtle adjustment
            colorMatrix.postConcat(ColorMatrix().apply {
                set(floatArrayOf(
                    1f, 0f, 0f, 0f, adjustment * 255,
                    0f, 1f, 0f, 0f, adjustment * 255,
                    0f, 0f, 1f, 0f, adjustment * 255,
                    0f, 0f, 0f, 1f, 0f
                ))
            })
        }
        
        // Apply color matrix to foreground
        val result = Bitmap.createBitmap(foreground.width, foreground.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        val paint = Paint().apply {
            colorFilter = ColorMatrixColorFilter(colorMatrix)
        }
        canvas.drawBitmap(foreground, 0f, 0f, paint)
        
        return result
    }
    
    private fun toggleAutoEnhance() {
        if (originalBitmap == null) return
        
        isEnhanced = !isEnhanced
        
        binding.progressBar.visibility = View.VISIBLE
        binding.tvLoading.visibility = View.VISIBLE
        binding.tvLoading.text = if (isEnhanced) "Enhancing..." else "Restoring..."
        
        lifecycleScope.launch(Dispatchers.Default) {
            // Generate enhanced bitmap if needed
            if (isEnhanced && enhancedBitmap == null) {
                enhancedBitmap = com.example.snapscenecamera.utils.ColorCorrectionHelper.autoEnhance(originalBitmap!!)
            }
            
            val targetBitmap = if (isEnhanced) enhancedBitmap!! else originalBitmap!!
            
            // Re-generate foreground if mask exists
            if (currentMask != null) {
                val foreground = createForegroundBitmap(targetBitmap, currentMask!!)
                foregroundBitmap = foreground
            }
            
            withContext(Dispatchers.Main) {
                binding.progressBar.visibility = View.GONE
                binding.tvLoading.visibility = View.GONE
                
                // Update button state visually
                binding.btnAutoEnhance.setTextColor(if (isEnhanced) Color.parseColor("#4A90E2") else Color.parseColor("#333333"))
                
                // Refresh view
                updateBackground(currentBackgroundItem)
                Toast.makeText(this@EditActivity, if (isEnhanced) "Auto Enhance: ON" else "Auto Enhance: OFF", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupSlowAdjustGesture() {
        // 慢速系数：值越小移动越慢
        val moveSpeedFactor = 0.3f  // 移动速度30%
        val scaleSpeedFactor = 0.4f // 缩放速度40%
        
        // 获取容器和人像尺寸
        val container = binding.backgroundLayer
        val foregroundView = binding.ivForeground
        
        // 缩放手势检测器
        val scaleDetector = android.view.ScaleGestureDetector(this, 
            object : android.view.ScaleGestureDetector.SimpleOnScaleGestureListener() {
                override fun onScale(detector: android.view.ScaleGestureDetector): Boolean {
                    if (!isAdjustMode) return false
                    
                    // 慢速缩放：只应用部分缩放因子
                    val scaleFactor = detector.scaleFactor
                    val slowScaleFactor = 1f + (scaleFactor - 1f) * scaleSpeedFactor
                    
                    val newScale = foregroundScale * slowScaleFactor
                    
                    // 获取容器尺寸
                    val containerWidth = container.width.toFloat()
                    val containerHeight = container.height.toFloat()
                    val foregroundWidth = foregroundView.width.toFloat()
                    val foregroundHeight = foregroundView.height.toFloat()
                    
                    if (containerWidth <= 0 || containerHeight <= 0 || 
                        foregroundWidth <= 0 || foregroundHeight <= 0) {
                        return false
                    }
                    
                    // 计算最大缩放比例：放大后人像不能超出背景
                    val maxScaleX = containerWidth / foregroundWidth
                    val maxScaleY = containerHeight / foregroundHeight
                    val maxScale = minOf(maxScaleX, maxScaleY)
                    
                    // 限制缩放范围：最小0.3倍，最大不超过背景
                    foregroundScale = newScale.coerceIn(0.3f, maxScale)
                    
                    foregroundView.scaleX = foregroundScale
                    foregroundView.scaleY = foregroundScale
                    
                    // 缩放后检查并修正位置（确保人像仍在边界内）
                    constrainPositionToBounds(containerWidth, containerHeight, 
                        foregroundWidth, foregroundHeight)
                    
                    return true
                }
            })
        
        // 触摸事件处理
        var lastX = 0f
        var lastY = 0f
        var pointerId = -1
        
        foregroundView.setOnTouchListener { view, event ->
            if (!isAdjustMode) return@setOnTouchListener false
            
            // 处理缩放手势
            scaleDetector.onTouchEvent(event)
            
            when (event.actionMasked) {
                android.view.MotionEvent.ACTION_DOWN -> {
                    pointerId = event.getPointerId(0)
                    lastX = event.x
                    lastY = event.y
                }
                android.view.MotionEvent.ACTION_MOVE -> {
                    if (event.pointerCount == 1 && pointerId != -1) {
                        // 单指：慢速移动
                        val dx = (event.x - lastX) * moveSpeedFactor
                        val dy = (event.y - lastY) * moveSpeedFactor
                        
                        // 获取容器和人像尺寸
                        val containerWidth = container.width.toFloat()
                        val containerHeight = container.height.toFloat()
                        val foregroundWidth = foregroundView.width.toFloat()
                        val foregroundHeight = foregroundView.height.toFloat()
                        
                        if (containerWidth > 0 && containerHeight > 0 &&
                            foregroundWidth > 0 && foregroundHeight > 0) {
                            
                            // 计算缩放后的人像尺寸
                            val scaledWidth = foregroundWidth * foregroundScale
                            val scaledHeight = foregroundHeight * foregroundScale
                            
                            // 计算可移动的最大距离（人像中心可移动范围）
                            // 人像必须完全在背景内，所以边界 = (容器尺寸 - 缩放后尺寸) / 2
                            val maxTranslateX = (containerWidth - scaledWidth) / 2f
                            val maxTranslateY = (containerHeight - scaledHeight) / 2f
                            
                            // 计算新位置
                            val newTranslateX = foregroundTranslateX + dx
                            val newTranslateY = foregroundTranslateY + dy
                            
                            // 应用边界限制（当缩小时，maxTranslate会变大，允许移动到更远的位置）
                            if (maxTranslateX >= 0) {
                                foregroundTranslateX = newTranslateX.coerceIn(-maxTranslateX, maxTranslateX)
                            } else {
                                // 人像比容器大，不允许移动（或限制移动范围）
                                foregroundTranslateX = 0f
                            }
                            
                            if (maxTranslateY >= 0) {
                                foregroundTranslateY = newTranslateY.coerceIn(-maxTranslateY, maxTranslateY)
                            } else {
                                // 人像比容器大，不允许移动
                                foregroundTranslateY = 0f
                            }
                            
                            foregroundView.translationX = foregroundTranslateX
                            foregroundView.translationY = foregroundTranslateY
                        }
                        
                        lastX = event.x
                        lastY = event.y
                    }
                }
                android.view.MotionEvent.ACTION_UP, android.view.MotionEvent.ACTION_CANCEL -> {
                    pointerId = -1
                }
            }
            true
        }
    }
    
    /**
     * 缩放后修正位置，确保人像在边界内
     */
    private fun constrainPositionToBounds(
        containerWidth: Float, containerHeight: Float,
        foregroundWidth: Float, foregroundHeight: Float
    ) {
        val scaledWidth = foregroundWidth * foregroundScale
        val scaledHeight = foregroundHeight * foregroundScale
        
        val maxTranslateX = (containerWidth - scaledWidth) / 2f
        val maxTranslateY = (containerHeight - scaledHeight) / 2f
        
        if (maxTranslateX >= 0) {
            val minX = -maxTranslateX
            val maxX = maxTranslateX
            foregroundTranslateX = foregroundTranslateX.coerceIn(minX, maxX)
        } else {
            foregroundTranslateX = 0f
        }
        
        if (maxTranslateY >= 0) {
            val minY = -maxTranslateY
            val maxY = maxTranslateY
            foregroundTranslateY = foregroundTranslateY.coerceIn(minY, maxY)
        } else {
            foregroundTranslateY = 0f
        }
        
        binding.ivForeground.translationX = foregroundTranslateX
        binding.ivForeground.translationY = foregroundTranslateY
    }
    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy: Cleaning up")
        
        originalBitmap?.recycle()
        foregroundBitmap?.recycle()
        customBackgroundBitmap?.recycle()
        enhancedBitmap?.recycle() // Clean up enhanced bitmap
        originalBitmap = null
        foregroundBitmap = null
        customBackgroundBitmap = null
        enhancedBitmap = null
    }
    
    companion object {
        private const val TAG = "EditActivity"
    }
    
    // ========== 手势控制功能 ==========
    
    private fun setupForegroundGestures() {
        // Default: No gestures. Adjust mode enables them.
        binding.ivForeground.setOnTouchListener(null)
    }
    private fun updateAdjustModeUI() {
        if (isAdjustMode) {
            // Enter Adjust Mode - 使用稳定的绿色背景 drawable（避免闪烁）
            binding.btnAdjust.setBackgroundResource(R.drawable.btn_adjust_bg_active)
            binding.btnAdjust.setTextColor(Color.WHITE)
            binding.adjustFrame.visibility = View.VISIBLE
            
            // Set up slow gesture listener
            setupSlowAdjustGesture()
            
            Toast.makeText(this, "单指拖动移动，双指缩放大小", Toast.LENGTH_SHORT).show()
        } else {
            // Exit Adjust Mode - 恢复半透明橙色边框
            binding.btnAdjust.setBackgroundResource(R.drawable.btn_adjust_bg)
            binding.btnAdjust.setTextColor(Color.parseColor("#FF9800"))
            binding.adjustFrame.visibility = View.GONE
            
            // Remove listener
            binding.ivForeground.setOnTouchListener(null)
            
            Toast.makeText(this, "调整完成", Toast.LENGTH_SHORT).show()
        }
    }
}






