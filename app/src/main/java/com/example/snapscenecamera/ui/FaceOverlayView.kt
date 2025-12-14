package com.example.snapscenecamera.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

class FaceOverlayView(context: Context, attrs: AttributeSet?) : View(context, attrs) {

    private val paint = Paint().apply {
        color = Color.GREEN
        style = Paint.Style.STROKE
        strokeWidth = 5f
        isAntiAlias = true
    }

    private var faces: List<Rect> = emptyList()
    private var imageWidth = 0
    private var imageHeight = 0
    private var scaleX = 1f
    private var scaleY = 1f
    private var isFrontCamera = false

    fun setFaces(faceBounds: List<Rect>, imgWidth: Int, imgHeight: Int, isFront: Boolean) {
        faces = faceBounds
        imageWidth = imgWidth
        imageHeight = imgHeight
        isFrontCamera = isFront
        calculateScale()
        invalidate()
    }

    private fun calculateScale() {
        if (imageWidth > 0 && imageHeight > 0 && width > 0 && height > 0) {
            scaleX = width.toFloat() / imageWidth
            scaleY = height.toFloat() / imageHeight
        }
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        calculateScale()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        for (face in faces) {
            val rect = RectF(face)
            
            // Transform coordinates
            // 1. Scale
            rect.left *= scaleX
            rect.right *= scaleX
            rect.top *= scaleY
            rect.bottom *= scaleY

            // 2. Mirror if front camera
            if (isFrontCamera) {
                val left = rect.left
                rect.left = width - rect.right
                rect.right = width - left
            }

            canvas.drawRect(rect, paint)
        }
    }
}
