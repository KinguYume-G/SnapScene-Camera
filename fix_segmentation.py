#!/usr/bin/env python3
# -*- coding: utf-8 -*-

import re

file_path = r'd:\Androidapp\app\src\main\java\com\example\snapscenecamera\EditActivity.kt'

with open(file_path, 'r', encoding='utf-8') as f:
    content = f.read()

changes_made = []

# 1. 添加ImageSegmentationHelper import
if 'import com.example.snapscenecamera.utils.ImageSegmentationHelper' not in content:
    # 在其他import之后添加
    content = content.replace(
        'import com.example.snapscenecamera.databinding.ActivityEditBinding',
        'import com.example.snapscenecamera.databinding.ActivityEditBinding\nimport com.example.snapscenecamera.utils.ImageSegmentationHelper'
    )
    changes_made.append('Added ImageSegmentationHelper import')

# 2. 完全替换createForegroundBitmap方法
old_method_pattern = r'private fun createForegroundBitmap\(bitmap: Bitmap, mask: SegmentationMask\): Bitmap \{[^}]*?(\n    \}(?:\n|$)|\n\})'
    
new_method = '''private fun createForegroundBitmap(bitmap: Bitmap, mask: SegmentationMask): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val maskWidth = mask.width
        val maskHeight = mask.height
        
        Log.d(TAG, "createForegroundBitmap: Bitmap=${width}x${height}, Mask=${maskWidth}x${maskHeight}")
        
        // 创建前景位图
        val foreground = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        
        // ===== 使用 ImageSegmentationHelper 优化 =====
        
        // 1. 提取原始confidence数组
        val confidences = ImageSegmentationHelper.extractConfidenceArray(
            mask.buffer, 
            maskWidth, 
            maskHeight
        )
        
        // 2. 平滑处理（4次迭代，提高质量）
        val smoothedConfidences = ImageSegmentationHelper.smoothMaskArray(
            confidences, 
            maskWidth, 
            maskHeight, 
            iterations = 4
        )
        
        // 3. 形态学清洁去噪
        val cleanedConfidences = ImageSegmentationHelper.morphologicalClean(
            smoothedConfidences, 
            maskWidth, 
            maskHeight
        )
        
        // 4. 应用到像素（带缩放适配）
        for (y in 0 until height) {
            for (x in 0 until width) {
                // 计算对应的mask坐标（处理尺寸不匹配）
                val maskX = (x.toFloat() / width * maskWidth).toInt()
                    .coerceIn(0, maskWidth - 1)
                val maskY = (y.toFloat() / height * maskHeight).toInt()
                    .coerceIn(0, maskHeight - 1)
                val maskIndex = maskY * maskWidth + maskX
                
                // 获取清洁后的confidence
                val confidence = cleanedConfidences[maskIndex]
                
                // 使用优化的Alpha计算
                val alpha = ImageSegmentationHelper.calculateAlpha(confidence)
                
                // 应用Alpha通道
                val pixelIndex = y * width + x
                pixels[pixelIndex] = (alpha shl 24) or (pixels[pixelIndex] and 0x00FFFFFF)
            }
        }
        
        // 设置处理后的像素
        foreground.setPixels(pixels, 0, width, 0, 0, width, height)
        
        // 日志：统计可见像素
        val visiblePixels = pixels.count { (it ushr 24) > 0 }
        Log.d(TAG, "createForegroundBitmap: Visible pixels=${visiblePixels}/${pixels.size}")
        
        return foreground
    }'''

# 使用更灵活的匹配
if 'ImageSegmentationHelper.extractConfidenceArray' not in content:
    # 找到方法开始和结束
    start_marker = 'private fun createForegroundBitmap(bitmap: Bitmap, mask: SegmentationMask): Bitmap {'
    if start_marker in content:
        start_pos = content.find(start_marker)
        # 找到匹配的结束大括号
        brace_count = 0
        pos = start_pos + len(start_marker)
        while pos < len(content):
            if content[pos] == '{':
                brace_count += 1
            elif content[pos] == '}':
                if brace_count == 0:
                    # 找到方法结束
                    content = content[:start_pos] + new_method + content[pos+1:]
                    changes_made.append('Replaced createForegroundBitmap() with optimized version')
                    break
                brace_count -= 1
            pos += 1

with open(file_path, 'w', encoding='utf-8') as f:
    f.write(content)

if changes_made:
    print('✅ Successfully modified EditActivity.kt for Phase 3:')
    for change in changes_made:
        print(f'  - {change}')
else:
    print('⚠️  No changes needed - ImageSegmentationHelper already integrated')
