#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
修复compositeFinalImage - 清理重复代码并正确应用缩放变换
"""

import re

print("修复compositeFinalImage方法...")

edit_file = r'd:\Androidapp\app\src\main\java\com\example\snapscenecamera\EditActivity.kt'

with open(edit_file, 'r', encoding='utf-8') as f:
    content = f.read()

# 找到compositeFinalImage方法中重复的matrix代码部分
# 需要删除不正确的部分，只保留简单的缩放变换

# 查找并替换整个混乱的matrix部分
old_matrix_section = r'''// 应用用户的手势变换到最终图片
val matrix = android\.graphics\.Matrix\(\)
val foregroundCenterX = adjustedForeground\.width / 2f
val foregroundCenterY = adjustedForeground\.height / 2f
val canvasCenterX = result\.width / 2f
val canvasCenterY = result\.width / 2f

// 按正确顺序应用变换（顺序很重要！）
matrix\.postTranslate\(-foregroundCenterX, -foregroundCenterY\)  // 1\. 移到原点
matrix\.postScale\(foregroundScale, foregroundScale\)            // 2\. 缩放
matrix\.postRotate\(foregroundRotation\)                         // 3\. 旋转
matrix\.postTranslate\(canvasCenterX \+ foregroundTranslateX, canvasCenterY \+ foregroundTranslateY\)  // 4\. 移到目标位置


    // 应用用户调整的缩放变换
    val matrix = android\.graphics\.Matrix\(\)
    
    // 计算中心点
    val centerX = adjustedForeground\.width / 2f
    val centerY = adjustedForeground\.height / 2f
    val canvasCenterX = result\.width / 2f
    val canvasCenterY = result\.height / 2f
    
    // 按顺序应用变换
    matrix\.postTranslate\(-centerX, -centerY\)  // 移到原点
    matrix\.postScale\(foregroundScale, foregroundScale\)  // 应用用户调整的缩放
    matrix\.postTranslate\(canvasCenterX, canvasCenterY\)  // 移到画布中心
    
    canvas\.drawBitmap\(adjustedForeground, matrix, null\)'''

new_matrix_section = '''    // 应用用户调整的缩放变换
    val matrix = android.graphics.Matrix()
    
    // 计算中心点
    val centerX = adjustedForeground.width / 2f
    val centerY = adjustedForeground.height / 2f
    val canvasCenterX = result.width / 2f
    val canvasCenterY = result.height / 2f
    
    // 按顺序应用变换（以中心为基准缩放）
    matrix.postTranslate(-centerX, -centerY)  // 1. 移到原点
    matrix.postScale(foregroundScale, foregroundScale)  // 2. 应用用户调整的缩放
    matrix.postTranslate(canvasCenterX, canvasCenterY)  // 3. 移到画布中心
    
    canvas.drawBitmap(adjustedForeground, matrix, null)'''

# 使用正则替换
content = re.sub(old_matrix_section, new_matrix_section, content, flags=re.DOTALL)

# 如果上面的替换失败，尝试查找简单的部分
if 'foregroundRotation' in content and 'compositeFinalImage' in content:
    # 说明还有旧的错误代码，需要手动清理
    print("⚠️  检测到foregroundRotation等未定义变量，正在清理...")
    
    # 找到applyColorHarmony之后，canvas.drawBitmap之前的所有matrix代码
    # 替换为干净的缩放变换
    pattern = r'(val adjustedForeground = applyColorHarmony\(foreground, currentBackgroundColor\))[\s\S]*?(canvas\.drawBitmap\(adjustedForeground, matrix, null\))'
    
    replacement = r'''\1

    // 应用用户调整的缩放变换
    val matrix = android.graphics.Matrix()
    
    // 计算中心点
    val centerX = adjustedForeground.width / 2f
    val centerY = adjustedForeground.height / 2f
    val canvasCenterX = result.width / 2f
    val canvasCenterY = result.height / 2f
    
    // 按顺序应用变换（以中心为基准缩放）
    matrix.postTranslate(-centerX, -centerY)  // 1. 移到原点
    matrix.postScale(foregroundScale, foregroundScale)  // 2. 应用缩放
    matrix.postTranslate(canvasCenterX, canvasCenterY)  // 3. 移到画布中心
    
    \2'''
    
    content = re.sub(pattern, replacement, content, flags=re.DOTALL)

with open(edit_file, 'w', encoding='utf-8') as f:
    f.write(content)

print("✅ compositeFinalImage方法已修复")
print("   - 删除重复的matrix代码")
print("   - 删除未定义变量（foregroundRotation等）")
print("   - 保留正确的缩放变换")
print("   - Matrix应用顺序：移到原点 → 缩放 → 移到中心")
