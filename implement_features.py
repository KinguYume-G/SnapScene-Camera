#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
实现用户要求的两个精确功能：
1. Save Original：设置开启 → 拍照自动保存到相册
2. 调整按钮：点击显示白色框 → 调整大小 → 再次点击保存到相册
"""

import re

print("="*60)
print("开始实现Save Original和调整白色框功能")
print("="*60)

# ========== 功能1: Save Original自动保存 ==========
print("\n[1/2] 实现Save Original自动保存...")

camera_file = r'd:\Androidapp\app\src\main\java\com\example\snapscenecamera\CameraActivity.kt'

with open(camera_file, 'r', encoding='utf-8') as f:
    camera_content = f.read()

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
                    Log.d(TAG, "Photo captured: ${photoFile.absolutePath}")
                    
                    // 检查Save Original设置
                    val prefs = getSharedPreferences("AppSettings", MODE_PRIVATE)
                    val saveOriginal = prefs.getBoolean("save_original", false)
                    
                    if (saveOriginal) {
                        // Save Original开启 - 保存到相册
                        lifecycleScope.launch(Dispatchers.IO) {
                            val mediaUri = saveToMediaStore(photoFile)
                            Log.d(TAG, "✅ Save Original: 已保存到相册 $mediaUri")
                            
                            withContext(Dispatchers.Main) {
                                Toast.makeText(baseContext, "照片已保存到相册", Toast.LENGTH_SHORT).show()
                                val intent = Intent(baseContext, EditActivity::class.java)
                                intent.putExtra("image_uri", savedUri.toString())
                                startActivity(intent)
                                finish()
                            }
                        }
                    } else {
                        // Save Original未开启 - 直接编辑
                        Log.d(TAG, "Save Original未开启")
                        val intent = Intent(baseContext, EditActivity::class.java)
                        intent.putExtra("image_uri", savedUri.toString())
                        startActivity(intent)
                        finish()
                    }
                }'''

camera_content = camera_content.replace(old_onImageSaved, new_onImageSaved)

# 确保有Toast import
if 'import android.widget.Toast' not in camera_content:
    camera_content = camera_content.replace(
        'import android.util.Log',
        'import android.util.Log\nimport android.widget.Toast'
    )

with open(camera_file, 'w', encoding='utf-8') as f:
    f.write(camera_content)

print("  ✅ CameraActivity修改完成")
print("     - onImageSaved现在检查save_original设置")
print("     - 开启时自动保存到MediaStore相册")

# ========== 功能2: 调整按钮显示白色框 ==========
print("\n[2/2] 实现调整按钮白色框功能...")

edit_file = r'd:\Androidapp\app\src\main\java\com\example\snapscenecamera\EditActivity.kt'

with open(edit_file, 'r', encoding='utf-8') as f:
    edit_content = f.read()

changes = []

# 添加白色框View变量
if 'private var adjustFrameView' not in edit_content:
    edit_content = edit_content.replace(
        'private var isAdjustMode = false',
        '''private var isAdjustMode = false
    private var adjustFrameView: View? = null'''
    )
    changes.append("添加adjustFrameView变量")

# 修改btnAdjust点击事件
old_btn_click = '''binding.btnAdjust.setOnClickListener {
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

new_btn_click = '''binding.btnAdjust.setOnClickListener {
            isAdjustMode = !isAdjustMode
            if (isAdjustMode) {
                // 激活调整模式 - 显示白色框
                binding.btnAdjust.setBackgroundColor(android.graphics.Color.parseColor("#FF9800"))
                binding.btnAdjust.setTextColor(android.graphics.Color.WHITE)
                showAdjustFrame(true)
                setupScaleGesture()
                Toast.makeText(this, "拖动白色框调整大小", Toast.LENGTH_SHORT).show()
            } else {
                // 完成调整 - 保存到相册
                try {
                    binding.btnAdjust.setBackgroundResource(R.drawable.btn_adjust_bg)
                } catch (e: Exception) {
                    binding.btnAdjust.setBackgroundColor(android.graphics.Color.TRANSPARENT)
                }
                binding.btnAdjust.setTextColor(android.graphics.Color.parseColor("#FF9800"))
                showAdjustFrame(false)
                
                // 保存到相册
                lifecycleScope.launch(Dispatchers.IO) {
                    val bitmap = compositeFinalImage()
                    val uri = exportEngine.exportImage(bitmap)
                    withContext(Dispatchers.Main) {
                        if (uri != null) {
                            Toast.makeText(this@EditActivity, "已保存到相册", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }'''

if old_btn_click in edit_content:
    edit_content = edit_content.replace(old_btn_click, new_btn_click)
    changes.append("修改btnAdjust点击事件（保存到相册）")

# 添加showAdjustFrame方法
if 'fun showAdjustFrame' not in edit_content:
    show_frame_method = '''
    
    private fun showAdjustFrame(show: Boolean) {
        if (show) {
            // 创建白色框
            if (adjustFrameView == null) {
                adjustFrameView = View(this).apply {
                    setBackgroundColor(android.graphics.Color.TRANSPARENT)
                    // 添加白色边框
                    background = android.graphics.drawable.GradientDrawable().apply {
                        setStroke(8, android.graphics.Color.WHITE)
                        cornerRadius = 12f
                    }
                }
                
                // 添加到布局
                val params = android.widget.FrameLayout.LayoutParams(
                    binding.ivForeground.width,
                    binding.ivForeground.height
                )
                params.gravity = android.view.Gravity.CENTER
                (binding.ivForeground.parent as android.view.ViewGroup).addView(adjustFrameView, params)
            }
            adjustFrameView?.visibility = View.VISIBLE
        } else {
            adjustFrameView?.visibility = View.GONE
        }
    }
'''
    
    edit_content = edit_content.replace(
        '    fun setupScaleGesture() {',
        show_frame_method + '    private fun setupScaleGesture() {'
    )
    changes.append("添加showAdjustFrame方法（白色边框）")

with open(edit_file, 'w', encoding='utf-8') as f:
    f.write(edit_content)

if changes:
    print("  ✅ EditActivity修改完成:")
    for change in changes:
        print(f"     - {change}")

print("\n" + "="*60)
print("✅ 所有修改完成！")
print("="*60)
print("\n功能说明:")
print("1. Save Original: 设置开启 → 拍照 → 自动保存到相册")
print("2. 调整按钮: 点击 → 白色框 → 调整大小 → 再次点击 → 保存到相册")
print("\n正在编译...")
