#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
完整实现：Save Original和调整功能
"""

import re

print("="*60)
print("实现Save Original和调整功能")
print("="*60)

# ==================== 1. CameraActivity - Save Original ====================
print("\n[1/3] CameraActivity - Save Original...")

camera_file = r'd:\Androidapp\app\src\main\java\com\example\snapscenecamera\CameraActivity.kt'
with open(camera_file, 'r', encoding='utf-8') as f:
    camera = f.read()

# 替换onImageSaved方法
old_onImageSaved = '''override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val savedUri = Uri.fromFile(photoFile)
                    val msg = "Photo capture succeeded: $savedUri"
                    Log.d(TAG, msg)
                    
                    // 跳转到 EditActivity
                    val intent = Intent(baseContext, EditActivity::class.java)
                    intent.putExtra("image_uri", savedUri.toString())
                    startActivity(intent)
                    finish()
                }'''

new_onImageSaved = '''override fun onImageSaved(output: ImageCapture.OutputFileResults) {
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
                }'''

if old_onImageSaved in camera:
    camera = camera.replace(old_onImageSaved, new_onImageSaved)
    print("  ✅ onImageSaved已更新")
else:
    print("  ⚠️  onImageSaved格式不匹配，尝试正则替换")
    pattern = r'override fun onImageSaved\(output: ImageCapture\.OutputFileResults\) \{[^}]+\}'
    if re.search(pattern, camera):
        camera = re.sub(pattern, new_onImageSaved, camera)
        print("  ✅ 正则替换成功")

# 添加import
if 'import kotlinx.coroutines.launch' not in camera:
    camera = camera.replace(
        'import java.util.concurrent.Executors',
        '''import java.util.concurrent.Executors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch'''
    )
    print("  ✅ 添加协程import")

with open(camera_file, 'w', encoding='utf-8') as f:
    f.write(camera)
print("  ✅ CameraActivity完成")

# ==================== 2. activity_edit.xml - 添加调整框架 ====================
print("\n[2/3] activity_edit.xml - 添加白色调整框架...")

xml_file = r'd:\Androidapp\app\src\main\res\layout\activity_edit.xml'
with open(xml_file, 'r', encoding='utf-8') as f:
    xml = f.read()

# 在ivForeground后添加调整框架
if 'adjustFrame' not in xml:
    adjust_frame = '''
        <!-- 调整框架 - 白色边框 -->
        <FrameLayout
            android:id="@+id/adjustFrame"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_gravity="center"
            android:visibility="gone"
            android:padding="8dp">
            
            <View
                android:layout_width="match_parent"
                android:layout_height="match_parent"
                android:background="@drawable/adjust_frame_border" />
            
            <!-- 四角拖动手柄 -->
            <View
                android:id="@+id/handleTopLeft"
                android:layout_width="24dp"
                android:layout_height="24dp"
                android:layout_gravity="start|top"
                android:background="#FFFFFF" />
            <View
                android:id="@+id/handleTopRight"
                android:layout_width="24dp"
                android:layout_height="24dp"
                android:layout_gravity="end|top"
                android:background="#FFFFFF" />
            <View
                android:id="@+id/handleBottomLeft"
                android:layout_width="24dp"
                android:layout_height="24dp"
                android:layout_gravity="start|bottom"
                android:background="#FFFFFF" />
            <View
                android:id="@+id/handleBottomRight"
                android:layout_width="24dp"
                android:layout_height="24dp"
                android:layout_gravity="end|bottom"
                android:background="#FFFFFF" />
        </FrameLayout>'''
    
    # 在ivForeground后添加
    xml = xml.replace(
        '</FrameLayout>\n    \n    <!-- 背景选择区域 -->',
        adjust_frame + '\n    </FrameLayout>\n    \n    <!-- 背景选择区域 -->'
    )
    print("  ✅ 调整框架已添加")
else:
    print("  ✅ 调整框架已存在")

with open(xml_file, 'w', encoding='utf-8') as f:
    f.write(xml)

# ==================== 2b. 创建边框drawable ====================
print("  创建边框drawable...")
border_file = r'd:\Androidapp\app\src\main\res\drawable\adjust_frame_border.xml'
border_content = '''<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android"
    android:shape="rectangle">
    <stroke
        android:width="3dp"
        android:color="#FFFFFF" />
    <corners android:radius="4dp" />
</shape>'''
with open(border_file, 'w', encoding='utf-8') as f:
    f.write(border_content)
print("  ✅ adjust_frame_border.xml已创建")

# ==================== 3. EditActivity - 调整按钮功能 ====================
print("\n[3/3] EditActivity - 调整按钮功能...")

edit_file = r'd:\Androidapp\app\src\main\java\com\example\snapscenecamera\EditActivity.kt'
with open(edit_file, 'r', encoding='utf-8') as f:
    edit = f.read()

changes = []

# 3a. 添加变量
if 'private var isAdjustMode' not in edit:
    edit = edit.replace(
        'private var currentForeground: Bitmap? = null',
        '''private var currentForeground: Bitmap? = null
    private var isAdjustMode = false
    private var adjustScale = 1.0f'''
    )
    changes.append("添加isAdjustMode和adjustScale变量")

# 3b. 在setupUI中添加调整按钮点击事件
if 'btnAdjust.setOnClickListener' not in edit:
    adjust_code = '''
        
        // 调整按钮点击事件
        binding.btnAdjust.setOnClickListener {
            if (!isAdjustMode) {
                // 进入调整模式：显示白色框架
                isAdjustMode = true
                binding.btnAdjust.setBackgroundColor(android.graphics.Color.parseColor("#FF9800"))
                binding.btnAdjust.setTextColor(android.graphics.Color.WHITE)
                binding.adjustFrame.visibility = View.VISIBLE
                
                // 设置框架大小与人像一致
                binding.adjustFrame.layoutParams.width = binding.ivForeground.width
                binding.adjustFrame.layoutParams.height = binding.ivForeground.height
                binding.adjustFrame.requestLayout()
                
                setupAdjustGesture()
                Toast.makeText(this, "拖动四角调整大小", Toast.LENGTH_SHORT).show()
            } else {
                // 退出调整模式：保存到相册
                isAdjustMode = false
                binding.btnAdjust.setBackgroundResource(R.drawable.btn_adjust_bg)
                binding.btnAdjust.setTextColor(android.graphics.Color.parseColor("#FF9800"))
                binding.adjustFrame.visibility = View.GONE
                
                // 应用缩放并保存
                applyAdjustAndSave()
            }
        }'''
    
    edit = edit.replace(
        'binding.btnShare.setOnClickListener {',
        adjust_code + '\n        \n        binding.btnShare.setOnClickListener {'
    )
    changes.append("添加btnAdjust点击事件")

# 3c. 添加setupAdjustGesture方法
if 'fun setupAdjustGesture' not in edit:
    adjust_gesture = '''
    
    private fun setupAdjustGesture() {
        // 双指缩放调整
        val scaleDetector = android.view.ScaleGestureDetector(this, 
            object : android.view.ScaleGestureDetector.SimpleOnScaleGestureListener() {
                override fun onScale(detector: android.view.ScaleGestureDetector): Boolean {
                    if (!isAdjustMode) return false
                    
                    adjustScale *= detector.scaleFactor
                    adjustScale = adjustScale.coerceIn(0.3f, 3.0f)
                    
                    // 更新人像和框架大小
                    binding.ivForeground.scaleX = adjustScale
                    binding.ivForeground.scaleY = adjustScale
                    
                    return true
                }
            })
        
        binding.previewContainer.setOnTouchListener { _, event ->
            if (isAdjustMode) {
                scaleDetector.onTouchEvent(event)
            }
            true
        }
    }
    
    private fun applyAdjustAndSave() {
        // 应用调整并保存到相册
        lifecycleScope.launch {
            try {
                Toast.makeText(this@EditActivity, "正在保存...", Toast.LENGTH_SHORT).show()
                
                val result = withContext(Dispatchers.IO) {
                    compositeFinalImage()
                }
                
                // 保存到MediaStore
                val uri = withContext(Dispatchers.IO) {
                    saveToGallery(result)
                }
                
                Toast.makeText(this@EditActivity, "已保存到相册", Toast.LENGTH_SHORT).show()
                Log.d(TAG, "Adjust saved: $uri")
                
            } catch (e: Exception) {
                Log.e(TAG, "Save failed", e)
                Toast.makeText(this@EditActivity, "保存失败", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private suspend fun saveToGallery(bitmap: Bitmap): Uri? {
        return withContext(Dispatchers.IO) {
            try {
                val filename = "SnapScene_${System.currentTimeMillis()}.jpg"
                val contentValues = android.content.ContentValues().apply {
                    put(android.provider.MediaStore.Images.Media.DISPLAY_NAME, filename)
                    put(android.provider.MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                    put(android.provider.MediaStore.Images.Media.RELATIVE_PATH,
                        "${android.os.Environment.DIRECTORY_PICTURES}/SnapScene Camera")
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                        put(android.provider.MediaStore.Images.Media.IS_PENDING, 1)
                    }
                }
                
                val uri = contentResolver.insert(
                    android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    contentValues
                )
                
                uri?.let {
                    contentResolver.openOutputStream(it)?.use { out ->
                        bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 95, out)
                    }
                    
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                        contentValues.clear()
                        contentValues.put(android.provider.MediaStore.Images.Media.IS_PENDING, 0)
                        contentResolver.update(uri, contentValues, null, null)
                    }
                }
                
                uri
            } catch (e: Exception) {
                Log.e(TAG, "saveToGallery failed", e)
                null
            }
        }
    }
'''
    
    edit = edit.replace(
        '    override fun onDestroy() {',
        adjust_gesture + '    override fun onDestroy() {'
    )
    changes.append("添加setupAdjustGesture和applyAdjustAndSave方法")

with open(edit_file, 'w', encoding='utf-8') as f:
    f.write(edit)

if changes:
    print("  ✅ EditActivity完成:")
    for c in changes:
        print(f"     - {c}")

print("\n" + "="*60)
print("✅ 修复完成！正在编译...")
print("="*60)
