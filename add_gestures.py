#!/usr/bin/env python3
# -*- coding: utf-8 -*-

import re

file_path = r'd:\Androidapp\app\src\main\java\com\example\snapscenecamera\EditActivity.kt'

# 读取文件
with open(file_path, 'r', encoding='utf-8') as f:
    content = f.read()

# 检查是否已经添加
if 'setupForegroundGestures' in content:
    print('✅ Gesture methods already exist')
    exit(0)

# 1. 在onDestroy之前添加手势方法
gesture_methods = '''
    // ========== 手势控制功能 ==========
    
    private fun setupForegroundGestures() {
        val scaleGestureDetector = ScaleGestureDetector(this, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                foregroundScale *= detector.scaleFactor
                foregroundScale = foregroundScale.coerceIn(0.5f, 3.0f)
                applyForegroundTransformation()
                return true
            }
        })
        
        binding.ivForeground.setOnTouchListener { view, event ->
            scaleGestureDetector.onTouchEvent(event)
            
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    lastTouchX = event.x
                    lastTouchY = event.y
                    activePointerId = event.getPointerId(0)
                }
                
                MotionEvent.ACTION_MOVE -> {
                    if (event.pointerCount == 1) {
                        val dx = event.x - lastTouchX
                        val dy = event.y - lastTouchY
                        foregroundTranslateX += dx
                        foregroundTranslateY += dy
                        lastTouchX = event.x
                        lastTouchY = event.y
                        applyForegroundTransformation()
                    } else if (event.pointerCount == 2) {
                        val angle = calculateRotation(event)
                        foregroundRotation = angle
                        applyForegroundTransformation()
                    }
                }
                
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    activePointerId = -1
                }
            }
            true
        }
    }
    
    private fun applyForegroundTransformation() {
        binding.ivForeground.scaleX = foregroundScale
        binding.ivForeground.scaleY = foregroundScale
        binding.ivForeground.rotation = foregroundRotation
        binding.ivForeground.translationX = foregroundTranslateX
        binding.ivForeground.translationY = foregroundTranslateY
    }
    
    private fun calculateRotation(event: MotionEvent): Float {
        val deltaX = (event.getX(0) - event.getX(1)).toDouble()
        val deltaY = (event.getY(0) - event.getY(1)).toDouble()
        val radians = atan2(deltaY, deltaX)
        return Math.toDegrees(radians).toFloat()
    }
    
'''

# 在onDestroy之前插入
content = content.replace('    override fun onDestroy() {', gesture_methods + '    override fun onDestroy() {')

# 2. 在setupUI末尾添加调用
setup_ui_call = '''        
        // 手势控制
        setupForegroundGestures()
    }'''

# 查找setupUI的结束位置并替加
content = re.sub(r'(binding\.btnShare\.setOnClickListener \{[^}]+\})\s*\}', r'\1' + setup_ui_call, content)

# 3. 修改compositeFinalImage中的绘制逻辑
matrix_code = '''
        // 应用用户的手势变换到最终图片
        val matrix = android.graphics.Matrix()
        val foregroundCenterX = adjustedForeground.width / 2f
        val foregroundCenterY = adjustedForeground.height / 2f
        val canvasCenterX = result.width / 2f
        val canvasCenterY = result.height / 2f
        
        // 按正确顺序应用变换
        matrix.postTranslate(-foregroundCenterX, -foregroundCenterY)
        matrix.postScale(foregroundScale, foregroundScale)
        matrix.postRotate(foregroundRotation)
        matrix.postTranslate(canvasCenterX + foregroundTranslateX, canvasCenterY + foregroundTranslateY)
        
        canvas.drawBitmap(adjustedForeground, matrix, null)'''

# 替换简单的drawBitmap调用
content = re.sub(
    r'val adjustedForeground = applyColorHarmony\(foreground, currentBackgroundColor\)\s*canvas\.drawBitmap\(adjustedForeground, 0f, 0f, null\)',
    'val adjustedForeground = applyColorHarmony(foreground, currentBackgroundColor)' + matrix_code,
    content
)

# 写回文件
with open(file_path, 'w', encoding='utf-8') as f:
    f.write(content)

print('✅ Successfully added gesture control methods')
print('✅ Added setupForegroundGestures() call in setupUI()')
print('✅ Modified compositeFinalImage() to use Matrix transformation')
