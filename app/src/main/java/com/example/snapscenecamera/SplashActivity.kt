package com.example.snapscenecamera

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity

/**
 * APU Virtual Guide 启动页
 * 显示品牌信息和欢迎动画
 */
class SplashActivity : AppCompatActivity() {

    companion object {
        private const val SPLASH_DURATION = 2500L // 2.5秒
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        // 延迟后跳转到主页
        Handler(Looper.getMainLooper()).postDelayed({
            startActivity(Intent(this, NewMainActivity::class.java))
            // 添加渐变过渡动画
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            finish()
        }, SPLASH_DURATION)
    }

    // 禁用返回键
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        // 启动页不响应返回键
    }
}
