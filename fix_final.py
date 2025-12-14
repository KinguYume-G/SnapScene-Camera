#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
最终修复脚本：实现调整按钮和Save Original功能
"""

import re

print("=" * 60)
print("开始修复调整按钮和Save Original功能")
print("=" * 60)

# ========== 修复1: CameraActivity - Save Original功能 ==========
print("\n[1/2] 修复CameraActivity - Save Original功能...")

camera_file = r'd:\Androidapp\app\src\main\java\com\example\snapscenecamera\CameraActivity.kt'

with open(camera_file, 'r', encoding='utf-8') as f:
    camera_content = f.read()

# 检查是否已有saveToMediaStore方法
if 'fun saveToMediaStore' not in camera_content:
    print("  ❌ saveToMediaStore方法不存在，需要添加")
    
    # 替换onImageSaved方法
    old_onImageSaved = r'''override fun onImageSaved\(output: ImageCapture\.OutputFileResults\) \{
                    val savedUri = Uri\.fromFile\(photoFile\)
                    val msg = "Photo capture succeeded: \$savedUri"
                    Log\.d\(TAG, msg\)
                    
                    // 跳转到 EditActivity
                    val intent = Intent\(baseContext, EditActivity::class\.java\)
                    intent\.putExtra\("image_uri", savedUri\.toString\(\)\)
                    startActivity\(intent\)
                    finish\(\)
                \}'''
    
    new_onImageSaved = '''override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val savedUri = Uri.fromFile(photoFile)
                    Log.d(TAG, "Photo capture succeeded: ${photoFile.absolutePath}")
                    
                    // 检查Save Original设置
                    val prefs = getSharedPreferences("AppSettings", MODE_PRIVATE)
                    val saveOriginal = prefs.getBoolean("save_original", false)
                    
                    if (saveOriginal) {
                        // Save Original开启：保存到MediaStore
                        lifecycleScope.launch(Dispatchers.IO) {
                            try {
                                val mediaStoreUri = saveToMediaStore(photoFile)
                                Log.d(TAG, "✅ Save Original: Image saved to MediaStore: $mediaStoreUri")
                                
                                withContext(Dispatchers.Main) {
                                    val intent = Intent(baseContext, EditActivity::class.java)
                                    intent.putExtra("image_uri", savedUri.toString())
                                    if (mediaStoreUri != null) {
                                        intent.putExtra("original_uri", mediaStoreUri.toString())
                                    }
                                    startActivity(intent)
                                    finish()
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "Failed to save to MediaStore", e)
                                withContext(Dispatchers.Main) {
                                    val intent = Intent(baseContext, EditActivity::class.java)
                                    intent.putExtra("image_uri", savedUri.toString())
                                    startActivity(intent)
                                    finish()
                                }
                            }
                        }
                    } else {
                        // Save Original未开启：直接跳转编辑
                        Log.d(TAG, "ℹ️ Save Original disabled - skipping MediaStore save")
                        val intent = Intent(baseContext, EditActivity::class.java)
                        intent.putExtra("image_uri", savedUri.toString())
                        startActivity(intent)
                        finish()
                    }
                }'''
    
    camera_content = re.sub(old_onImageSaved, new_onImageSaved, camera_content, flags=re.DOTALL)
    
    # 添加saveToMediaStore方法（在onDestroy之前）
    save_to_mediastore_method = '''
    
    private suspend fun saveToMediaStore(file: File): Uri? {
        return withContext(Dispatchers.IO) {
            try {
                val contentValues = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, file.name)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                    put(MediaStore.Images.Media.RELATIVE_PATH,
                        "${Environment.DIRECTORY_PICTURES}/SnapScene Camera")
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        put(MediaStore.Images.Media.IS_PENDING, 1)
                    }
                }
                
                val uri = contentResolver.insert(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    contentValues
                )
                
                uri?.let {
                    contentResolver.openOutputStream(it)?.use { outputStream ->
                        file.inputStream().use { inputStream ->
                            inputStream.copyTo(outputStream)
                        }
                    }
                    
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        contentValues.clear()
                        contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                        contentResolver.update(uri, contentValues, null, null)
                    }
                }
                
                uri
            } catch (e: Exception) {
                Log.e(TAG, "Error saving to MediaStore", e)
                null
            }
        }
    }
'''
    
    camera_content = camera_content.replace(
        '    override fun onDestroy() {',
        save_to_mediastore_method + '    override fun onDestroy() {'
    )
    
    # 添加必要的import
    if 'import kotlinx.coroutines.launch' not in camera_content:
        camera_content = camera_content.replace(
            'import java.util.concurrent.Executors',
            '''import java.util.concurrent.Executors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import android.content.ContentValues
import android.os.Environment
import android.provider.MediaStore'''
        )
    
    with open(camera_file, 'w', encoding='utf-8') as f:
        f.write(camera_content)
    
    print("  ✅ CameraActivity修复完成")
    print("     - onImageSaved已更新（检查Save Original设置）")
    print("     - saveToMediaStore方法已添加")
    print("     - 必要import已添加")
else:
    print("  ✅ saveToMediaStore方法已存在")

# ========== 修复2: EditActivity - 调整按钮功能 ==========
print("\n[2/2] 修复EditActivity - 调整按钮功能...")

edit_file = r'd:\Androidapp\app\src\main\java\com\example\snapscenecamera\EditActivity.kt'

with open(edit_file, 'r', encoding='utf-8') as f:
    edit_content = f.read()

changes = []

# 添加变量
if 'private var isAdjustMode' not in edit_content:
    # 找到foreground相关变量的位置
    if 'private var currentForeground' in edit_content:
        edit_content = edit_content.replace(
            'private var currentForeground: Bitmap? = null',
            '''private var currentForeground: Bitmap? = null
    private var isAdjustMode = false
    private var foregroundScale = 1.0f'''
        )
        changes.append("添加isAdjustMode和foregroundScale变量")

# 添加点击事件（在setupUI方法中）
if 'btnAdjust.setOnClickListener' not in edit_content:
    # 找到btnShare.setOnClickListener的位置，在后面添加
    if 'binding.btnShare.setOnClickListener' in edit_content:
        share_button_pattern = r'(binding\.btnShare\.setOnClickListener \{[^}]+\})'
        adjust_button_code = r'''\1
        
        // 调整按钮 - 启用/禁用缩放模式
        binding.btnAdjust.setOnClickListener {
            isAdjustMode = !isAdjustMode
            if (isAdjustMode) {
                binding.btnAdjust.setBackgroundColor(android.graphics.Color.parseColor("#FF9800"))
                binding.btnAdjust.setTextColor(android.graphics.Color.WHITE)
                Toast.makeText(this, "调整模式：双指缩放调整大小", Toast.LENGTH_SHORT).show()
                setupScaleGesture()
            } else {
                try {
                    binding.btnAdjust.setBackgroundResource(R.drawable.btn_adjust_bg)
                } catch (e: Exception) {
                    binding.btnAdjust.setBackgroundColor(android.graphics.Color.TRANSPARENT)
                }
                binding.btnAdjust.setTextColor(android.graphics.Color.parseColor("#FF9800"))
                Toast.makeText(this, "调整完成", Toast.LENGTH_SHORT).show()
            }
        }'''
        edit_content = re.sub(share_button_pattern, adjust_button_code, edit_content)
        changes.append("添加btnAdjust点击事件")

# 添加setupScaleGesture方法
if 'fun setupScaleGesture' not in edit_content:
    scale_gesture_method = '''
    
    private fun setupScaleGesture() {
        val scaleDetector = ScaleGestureDetector(this, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                if (!isAdjustMode) return false
                
                foregroundScale *= detector.scaleFactor
                foregroundScale = foregroundScale.coerceIn(0.5f, 3.0f)
                
                binding.ivForeground.scaleX = foregroundScale
                binding.ivForeground.scaleY = foregroundScale
                
                Log.d(TAG, "Scale adjusted: $foregroundScale")
                return true
            }
        })
        
        binding.ivForeground.setOnTouchListener { _, event ->
            if (isAdjustMode) {
                scaleDetector.onTouchEvent(event)
            }
            true
        }
    }
'''
    
    # 在特定位置插入（如onDestroy之前）
    edit_content = edit_content.replace(
        '    override fun onDestroy() {',
        scale_gesture_method + '    override fun onDestroy() {'
    )
    changes.append("添加setupScaleGesture方法")

# 添加import
if 'import android.view.ScaleGestureDetector' not in edit_content:
    edit_content = edit_content.replace(
        'import android.view.View',
        '''import android.view.View
import android.view.ScaleGestureDetector'''
    )
    changes.append("添加ScaleGestureDetector import")

with open(edit_file, 'w', encoding='utf-8') as f:
    f.write(edit_content)

if changes:
    print("  ✅ EditActivity修复完成:")
    for change in changes:
        print(f"     - {change}")
else:
    print("  ℹ️  EditActivity无需修改（代码已存在）")

print("\n" + "=" * 60)
print("✅ 所有修复完成！")
print("=" * 60)
print("\n下一步:")
print("1. 编译: ./gradlew assembleDebug")
print("2. 安装测试")
print("3. 测试Save Original: 设置开启 → 拍照 → My Creations有图")
print("4. 测试调整按钮: 点击调整 → 双指缩放人像")
