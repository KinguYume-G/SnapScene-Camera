package com.example.snapscenecamera

import android.Manifest
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.example.snapscenecamera.databinding.ActivityCameraBinding
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import android.annotation.SuppressLint
import androidx.camera.core.Camera
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.SurfaceOrientedMeteringPointFactory
import java.util.concurrent.TimeUnit
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class CameraActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCameraBinding
    private var imageCapture: ImageCapture? = null
    private lateinit var cameraExecutor: ExecutorService
    private var lensFacing = CameraSelector.LENS_FACING_BACK
    private var flashMode = ImageCapture.FLASH_MODE_OFF
    
    // Face Detection
    private var faceDetector: FaceDetector? = null
    private var camera: Camera? = null
    private var lastFocusTime = 0L

    // 权限请求
    private val activityResultLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            var permissionGranted = true
            permissions.entries.forEach {
                if (it.key in REQUIRED_PERMISSIONS && !it.value)
                    permissionGranted = false
            }
            if (!permissionGranted) {
                Toast.makeText(baseContext, "需要相机权限才能拍照", Toast.LENGTH_SHORT).show()
            } else {
                startCamera()
            }
        }
    
    // 相册选择
    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let {
            val intent = Intent(this, EditActivity::class.java)
            intent.putExtra("image_uri", it.toString())
            startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCameraBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 读取设置
        val prefs = getSharedPreferences("AppSettings", android.content.Context.MODE_PRIVATE)
        
        // 应用辅助线设置
        val showGuideLines = prefs.getBoolean("guide_lines", false)
        binding.guideLines.visibility = if (showGuideLines) View.VISIBLE else View.GONE
        Log.d(TAG, "onCreate: Guide lines visibility = $showGuideLines")
        
        // 检查权限并启动相机
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            requestPermissions()
        }
        
        // 初始化线程池
        cameraExecutor = Executors.newSingleThreadExecutor()
        
        // 设置手电筒初始图标
        binding.btnFlash.setImageResource(R.drawable.ic_flashlight_off)
        
        // 拍照按钮
        binding.btnCapture.setOnClickListener {
            takePhoto()
        }
        
        // 切换摄像头
        binding.btnSwitchCamera.setOnClickListener {
            lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
                CameraSelector.LENS_FACING_FRONT
            } else {
                CameraSelector.LENS_FACING_BACK
            }
            startCamera()
        }

        // 返回按钮
        binding.btnBack.setOnClickListener {
            finish()
        }

        // 闪光灯
        binding.btnFlash.setOnClickListener {
            flashMode = when (flashMode) {
                ImageCapture.FLASH_MODE_OFF -> {
                    binding.btnFlash.setImageResource(R.drawable.ic_flashlight_on)
                    Toast.makeText(this, "Flash: On", Toast.LENGTH_SHORT).show()
                    Log.d(TAG, "Flash mode: ON")
                    ImageCapture.FLASH_MODE_ON
                }
                ImageCapture.FLASH_MODE_ON -> {
                    binding.btnFlash.setImageResource(R.drawable.ic_flashlight_auto)
                    Toast.makeText(this, "Flash: Auto", Toast.LENGTH_SHORT).show()
                    Log.d(TAG, "Flash mode: AUTO")
                    ImageCapture.FLASH_MODE_AUTO
                }
                else -> {
                    binding.btnFlash.setImageResource(R.drawable.ic_flashlight_off)
                    Toast.makeText(this, "Flash: Off", Toast.LENGTH_SHORT).show()
                    Log.d(TAG, "Flash mode: OFF")
                    ImageCapture.FLASH_MODE_OFF
                }
            }
            // 重新启动相机应用新的闪光灯设置
            startCamera()
        }
        
        // 相册按钮 - 从系统相册选择照片
        binding.btnGallery.setOnClickListener {
            pickImageLauncher.launch(
                androidx.activity.result.PickVisualMediaRequest(
                    androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageOnly
                )
            )
        }
        // Initialize Face Detector
        val options = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .setContourMode(FaceDetectorOptions.CONTOUR_MODE_NONE)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
            .build()
        faceDetector = FaceDetection.getClient(options)
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            // 绑定生命周期
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // 预览 UseCase
            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(binding.viewFinder.surfaceProvider)
                }

            // 拍照 UseCase
            // 从设置读取照片分辨率
            val prefs = getSharedPreferences("AppSettings", android.content.Context.MODE_PRIVATE)
            val resolution = prefs.getString("photo_resolution", "high") ?: "high"
            
            val captureMode = when(resolution) {
                "high" -> ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY
                "medium", "low" -> ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY
                else -> ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY
            }
            
            Log.d(TAG, "startCamera: Using resolution=$resolution, captureMode=$captureMode")
            
            imageCapture = ImageCapture.Builder()
                .setCaptureMode(captureMode)
                .setFlashMode(flashMode)
                .build()

            // Image Analysis UseCase (Face Detection)
            val imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor) { imageProxy ->
                        faceDetector?.let { detector ->
                            processImageProxy(detector, imageProxy)
                        } ?: imageProxy.close()
                    }
                }

            // 选择摄像头
            val cameraSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()

            try {
                // 解绑所有用例
                cameraProvider.unbindAll()

                // 绑定用例
                camera = cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture, imageAnalysis
                )

            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(this))
    }

    @SuppressLint("UnsafeOptInUsageError")
    private fun processImageProxy(detector: FaceDetector, imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            detector.process(image)
                .addOnSuccessListener { faces ->
                    // Update overlay
                    binding.faceOverlay.setFaces(
                        faces.map { it.boundingBox }, 
                        imageProxy.width, 
                        imageProxy.height, 
                        lensFacing == CameraSelector.LENS_FACING_FRONT
                    )
                    
                    // Update People Count
                    val count = faces.size
                    binding.tvPeopleCount.text = "People: $count"
                    binding.tvPeopleCount.visibility = if (count > 0) View.VISIBLE else View.GONE
                    
                    // Autofocus logic
                    checkAutoFocus(faces, imageProxy.width, imageProxy.height)
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Face detection failed", e)
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        } else {
            imageProxy.close()
        }
    }

    private fun checkAutoFocus(faces: List<Face>, width: Int, height: Int) {
        if (faces.isEmpty()) return
        
        val now = System.currentTimeMillis()
        if (now - lastFocusTime < 2000) return // Debounce 2s

        // Focus on the largest face
        val face = faces.maxByOrNull { it.boundingBox.width() * it.boundingBox.height() } ?: return
        val centerX = face.boundingBox.centerX().toFloat()
        val centerY = face.boundingBox.centerY().toFloat()

        // Convert coordinates if needed (simplified here, assuming full sensor usage)
        // Note: SurfaceOrientedMeteringPointFactory handles the conversion from sensor coordinates to 0..1 range
        // But we need to be careful about rotation. For now, let's try direct mapping.
        
        try {
            val factory = SurfaceOrientedMeteringPointFactory(width.toFloat(), height.toFloat())
            val point = factory.createPoint(centerX, centerY)
            val action = FocusMeteringAction.Builder(point, FocusMeteringAction.FLAG_AF)
                .setAutoCancelDuration(3, TimeUnit.SECONDS)
                .build()

            camera?.cameraControl?.startFocusAndMetering(action)
            lastFocusTime = now
            Log.d(TAG, "Autofocus triggered on face at $centerX, $centerY")
        } catch (e: Exception) {
            Log.e(TAG, "Autofocus failed", e)
        }
    }

    private fun takePhoto() {
        Log.d(TAG, "Taking photo with flash mode: $flashMode")
        // 获取稳定的 imageCapture 引用
        val imageCapture = imageCapture ?: return

        // 创建输出文件
        val photoFile = File(
            externalMediaDirs.firstOrNull(),
            SimpleDateFormat(FILENAME_FORMAT, Locale.CHINA)
                .format(System.currentTimeMillis()) + ".jpg"
        )

        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        // 显示拍照白闪动画
        binding.root.postDelayed({
            binding.root.setBackgroundColor(android.graphics.Color.WHITE)
            binding.root.postDelayed({
                binding.root.setBackgroundColor(android.graphics.Color.TRANSPARENT)
            }, 50)
        }, 0)

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
                    Toast.makeText(baseContext, "Capture failed: ${exc.message}", Toast.LENGTH_SHORT).show()
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val savedUri = Uri.fromFile(photoFile)
                    Log.d(TAG, "Photo saved: ${photoFile.absolutePath}")
                    
                    // 检查Save Original设置
                    val prefs = getSharedPreferences("AppSettings", MODE_PRIVATE)
                    val saveOriginal = prefs.getBoolean("save_original", false)
                    
                    if (saveOriginal) {
                        // 开启Save Original：自动保存到相册
                        lifecycleScope.launch(Dispatchers.IO) {
                            val mediaUri = saveToMediaStore(photoFile)
                            Log.d(TAG, "Save Original: saved to $mediaUri")
                            
                            withContext(Dispatchers.Main) {
                                Toast.makeText(baseContext, "原图已保存到相册", Toast.LENGTH_SHORT).show()
                                val intent = Intent(baseContext, EditActivity::class.java)
                                intent.putExtra("image_uri", savedUri.toString())
                                startActivity(intent)
                                finish()
                            }
                        }
                    } else {
                        // 未开启：直接跳转
                        val intent = Intent(baseContext, EditActivity::class.java)
                        intent.putExtra("image_uri", savedUri.toString())
                        startActivity(intent)
                        finish()
                    }
                }
            }
        )
    }

    private fun requestPermissions() {
        activityResultLauncher.launch(REQUIRED_PERMISSIONS)
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            baseContext, it
        ) == PackageManager.PERMISSION_GRANTED
    }


    
    private suspend fun saveToMediaStore(file: File): Uri? {
        return withContext(Dispatchers.IO) {
            try {
                // 准备元数据
                val contentValues = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, file.name)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                    put(MediaStore.Images.Media.RELATIVE_PATH,
                        "${Environment.DIRECTORY_PICTURES}/SnapScene Camera")
                    // Android 10+ 使用IS_PENDING机制
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        put(MediaStore.Images.Media.IS_PENDING, 1)
                    }
                }
                
                // 插入到MediaStore
                val uri = contentResolver.insert(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    contentValues
                )
                
                if (uri == null) {
                    Log.e(TAG, "Failed to create MediaStore entry")
                    return@withContext null
                }
                
                // 写入文件内容
                contentResolver.openOutputStream(uri)?.use { outputStream ->
                    file.inputStream().use { inputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
                
                // 标记为完成
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    contentValues.clear()
                    contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                    contentResolver.update(uri, contentValues, null, null)
                }
                
                Log.d(TAG, "Successfully saved to MediaStore: $uri")
                uri
                
            } catch (e: Exception) {
                Log.e(TAG, "Error saving to MediaStore", e)
                null
            }
        }
    }
    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    companion object {
        private const val TAG = "CameraActivity"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private val REQUIRED_PERMISSIONS = arrayOf(
            Manifest.permission.CAMERA
        )
    }
}