package com.example.snapscenecamera

import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.animation.ValueAnimator
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.util.Log
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.appcompat.widget.AppCompatImageView

/**
 * 数字人动画视图
 * 实现呼吸动画和眨眼动画效果
 */
class DigitalHumanAnimationView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatImageView(context, attrs, defStyleAttr) {

    companion object {
        private const val TAG = "DigitalHumanAnimation"
        
        // 动画参数
        private const val BREATHING_DURATION = 4000L // 呼吸周期4秒
        private const val BREATHING_SCALE_MIN = 0.98f // 最小缩放
        private const val BREATHING_SCALE_MAX = 1.02f // 最大缩放
        
        private const val BLINK_DURATION = 200L // 眨眼持续时间
        private const val BLINK_ALPHA_MIN = 0.7f // 眨眼最小透明度
        private const val BLINK_DELAY_MIN = 2000L // 最小眨眼间隔
        private const val BLINK_DELAY_MAX = 5000L // 最大眨眼间隔
    }

    // 动画对象
    private var breathingAnimator: ObjectAnimator? = null
    private var blinkAnimator: ValueAnimator? = null
    private val blinkHandler = Handler(Looper.getMainLooper())
    private var blinkRunnable: Runnable? = null
    
    // 动画状态
    private var isAnimating = false

    init {
        // 设置缩放类型
        scaleType = ScaleType.CENTER_CROP
        
        // 设置数字人图片
        setImageResource(R.drawable.digital_human_placeholder)
        
        Log.d(TAG, "DigitalHumanAnimationView initialized")
    }

    /**
     * 启动所有动画
     */
    fun startAnimations() {
        if (isAnimating) {
            Log.w(TAG, "Animations already running")
            return
        }
        
        isAnimating = true
        startBreathingAnimation()
        startBlinkingAnimation()
        Log.d(TAG, "All animations started")
    }

    /**
     * 启动呼吸动画（缩放效果）
     */
    private fun startBreathingAnimation() {
        stopBreathingAnimation() // 先停止已有动画
        
        breathingAnimator = ObjectAnimator.ofPropertyValuesHolder(
            this,
            PropertyValuesHolder.ofFloat("scaleX", BREATHING_SCALE_MIN, BREATHING_SCALE_MAX),
            PropertyValuesHolder.ofFloat("scaleY", BREATHING_SCALE_MIN, BREATHING_SCALE_MAX)
        ).apply {
            duration = BREATHING_DURATION
            repeatMode = ObjectAnimator.REVERSE
            repeatCount = ObjectAnimator.INFINITE
            interpolator = AccelerateDecelerateInterpolator()
            start()
        }
        
        Log.d(TAG, "Breathing animation started (subtle)")
    }

    /**
     * 启动眨眼动画（透明度变化）
     */
    private fun startBlinkingAnimation() {
        stopBlinkingAnimation() // 先停止已有动画
        scheduleNextBlink()
        Log.d(TAG, "Blinking animation started (gentle)")
    }

    /**
     * 调度下一次眨眼
     */
    private fun scheduleNextBlink() {
        if (!isAnimating) return
        
        // 随机延迟3-6秒后眨眼 (降低频率)
        val delay = (BLINK_DELAY_MIN..BLINK_DELAY_MAX).random().toLong()
        
        blinkRunnable = Runnable {
            performBlink()
            scheduleNextBlink() // 递归调度下一次
        }
        
        blinkHandler.postDelayed(blinkRunnable!!, delay)
    }

    /**
     * 执行一次眨眼动画
     */
    private fun performBlink() {
        blinkAnimator = ValueAnimator.ofFloat(1.0f, BLINK_ALPHA_MIN, 1.0f).apply {
            duration = BLINK_DURATION
            addUpdateListener { animation ->
                alpha = animation.animatedValue as Float
            }
            start()
        }
        
        Log.d(TAG, "Performed blink")
    }

    /**
     * 暂停所有动画
     */
    fun pauseAnimations() {
        breathingAnimator?.pause()
        blinkHandler.removeCallbacksAndMessages(null)
        isAnimating = false
        Log.d(TAG, "Animations paused")
    }

    /**
     * 恢复所有动画
     */
    fun resumeAnimations() {
        if (!isAnimating) {
            startAnimations()
        } else {
            breathingAnimator?.resume()
            scheduleNextBlink()
        }
        Log.d(TAG, "Animations resumed")
    }

    /**
     * 停止呼吸动画
     */
    private fun stopBreathingAnimation() {
        breathingAnimator?.cancel()
        breathingAnimator = null
        
        // 重置缩放
        scaleX = 1.0f
        scaleY = 1.0f
    }

    /**
     * 停止眨眼动画
     */
    private fun stopBlinkingAnimation() {
        blinkHandler.removeCallbacksAndMessages(null)
        blinkAnimator?.cancel()
        blinkAnimator = null
        blinkRunnable = null
        
        // 重置透明度
        alpha = 1.0f
    }

    /**
     * 停止所有动画
     */
    fun stopAnimations() {
        isAnimating = false
        stopBreathingAnimation()
        stopBlinkingAnimation()
        Log.d(TAG, "All animations stopped")
    }

    /**
     * View从窗口分离时清理资源
     */
    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        stopAnimations()
        Log.d(TAG, "View detached, animations cleaned up")
    }
}
