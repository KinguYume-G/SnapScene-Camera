#!/usr/bin/env python3
# -*- coding: utf-8 -*-

import re

file_path = r'd:\Androidapp\app\src\main\java\com\example\snapscenecamera\EditActivity.kt'

# 读取文件
with open(file_path, 'r', encoding='utf-8') as f:
    content = f.read()

changes_made = []

# 1. 添加isAdjustMode变量（在activePointerId之后）
if 'private var isAdjustMode' not in content:
    content = content.replace(
        'private var activePointerId = -1',
        'private var activePointerId = -1\n    private var isAdjustMode = false  // 调整模式标志'
    )
    changes_made.append('Added isAdjustMode variable')

# 2. 在setupUI中添加btnAdjust点击事件（在setupForegroundGestures之前）
if 'binding.btnAdjust.setOnClickListener' not in content:
    adjust_click = '''
        
        // 调整按钮点击事件
        binding.btnAdjust.setOnClickListener {
            isAdjustMode = !isAdjustMode
            updateAdjustModeUI()
            Log.d(TAG, "Adjust mode toggled: $isAdjustMode")
        }
'''
    # 在setupForegroundGestures()之前插入
    content = content.replace(
        '// 手势控制\n        setupForegroundGestures()',
        adjust_click + '\n        // 手势控制\n        setupForegroundGestures()'
    )
    changes_made.append('Added btnAdjust click listener in setupUI()')

# 3. 添加updateAdjustModeUI方法（在calculateRotation之前）
if 'private fun updateAdjustModeUI' not in content:
    update_ui_method = '''
    
    private fun updateAdjustModeUI() {
        if (isAdjustMode) {
            // 激活状态：实心橙色背景
            binding.btnAdjust.setTextColor(android.graphics.Color.WHITE)
            binding.btnAdjust.setBackgroundColor(
                android.graphics.Color.parseColor("#FF9800")
            )
            Toast.makeText(
                this,
                "调整模式：双指缩放/旋转，单指拖动",
                Toast.LENGTH_SHORT
            ).show()
        } else {
            // 关闭状态：橙色边框
            binding.btnAdjust.setTextColor(
                android.graphics.Color.parseColor("#FF9800")
            )
            // 尝试使用drawable背景，如果不存在则用透明
            try {
                binding.btnAdjust.setBackgroundResource(R.drawable.btn_adjust_bg)
            } catch (e: Exception) {
                binding.btnAdjust.setBackgroundColor(android.graphics.Color.TRANSPARENT)
            }
            Toast.makeText(
                this,
                "调整模式已关闭",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
'''
    content = content.replace(
        '    private fun calculateRotation(event: MotionEvent): Float {',
        update_ui_method + '\n    private fun calculateRotation(event: MotionEvent): Float {'
    )
    changes_made.append('Added updateAdjustModeUI() method')

# 4. 修改setupForegroundGestures中的setOnTouchListener，添加isAdjustMode检查
if 'if (!isAdjustMode)' not in content:
    # 查找setOnTouchListener并在开头添加检查
    pattern = r'(binding\.ivForeground\.setOnTouchListener \{ view, event ->\n)'
    replacement = r'\1        // 只在调整模式下响应手势\n        if (!isAdjustMode) return@setOnTouchListener false\n        \n'
    content = re.sub(pattern, replacement, content)
    changes_made.append('Added isAdjustMode check in gesture listener')

# 写回文件
with open(file_path, 'w', encoding='utf-8') as f:
    f.write(content)

if changes_made:
    print('✅ Successfully modified EditActivity.kt:')
    for change in changes_made:
        print(f'  - {change}')
else:
    print('⚠️  No changes needed - all code already exists')
