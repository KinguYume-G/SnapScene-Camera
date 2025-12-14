package com.example.snapscenecamera

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View

class GuideLineView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint().apply {
        color = 0x80FFFFFF.toInt() // 半透明白色
        strokeWidth = 2f
        style = Paint.Style.STROKE
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        val width = width.toFloat()
        val height = height.toFloat()
        
        // 绘制垂直线（1/3 和 2/3 处）
        canvas.drawLine(width / 3, 0f, width / 3, height, paint)
        canvas.drawLine(width * 2 / 3, 0f, width * 2 / 3, height, paint)
        
        // 绘制水平线（1/3 和 2/3 处）
        canvas.drawLine(0f, height / 3, width, height / 3, paint)
        canvas.drawLine(0f, height * 2 / 3, width, height * 2 / 3, paint)
    }
}
