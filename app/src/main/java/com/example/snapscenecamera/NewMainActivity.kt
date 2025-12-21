package com.example.snapscenecamera

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.util.Log
import android.widget.FrameLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.snapscenecamera.databinding.ActivityNewMainBinding
import java.text.SimpleDateFormat
import java.util.*

class NewMainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNewMainBinding
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var clockRunnable: Runnable
    private var currentLanguage = "zh" // zh / en
    
    // 数字人相关（SDK方案）
    private var digitalHumanWebView: DigitalHumanWebView? = null
    private var digitalHumanAnimationView: DigitalHumanAnimationView? = null
    private var isUsingWebViewSolution = true // true: SDK方案, false: 静态方案
    
    // TTS备用方案
    private var textToSpeech: TextToSpeech? = null
    private var isTTSReady = false

    companion object {
        private const val TAG = "NewMainActivity"
        private const val PREF_LANGUAGE = "app_language"
    }

    private var isDropdownVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate: NewMainActivity started")

        // 加载保存的语言设置
        loadLanguagePreference()

        binding = ActivityNewMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initTopBar()
        initDigitalHuman()
        initTextToSpeech()
        initButtons()
        startClock()
    }

    private fun loadLanguagePreference() {
        val prefs = getSharedPreferences("AppSettings", MODE_PRIVATE)
        currentLanguage = prefs.getString(PREF_LANGUAGE, "zh") ?: "zh"
        
        // 设置应用Locale
        val locale = if (currentLanguage == "zh") Locale.CHINESE else Locale.ENGLISH
        val config = Configuration(resources.configuration)
        config.setLocale(locale)
        resources.updateConfiguration(config, resources.displayMetrics)
    }

    private fun initTopBar() {
        // 学校名称已在XML中设置
        
        // 天气数据（硬编码）
        binding.tvWeather.text = "28°C | 65%"
        
        // 语言切换按钮和下拉菜单
        updateLanguageDisplay()
        
        // 点击语言区域显示/隐藏下拉菜单
        binding.languageContainer.setOnClickListener {
            toggleLanguageDropdown()
        }
        
        // 下拉菜单选项点击事件
        binding.btnLangEnglish.setOnClickListener {
            switchLanguage("en")
            hideLanguageDropdown()
        }
        
        binding.btnLangChinese.setOnClickListener {
            switchLanguage("zh")
            hideLanguageDropdown()
        }
        
        binding.btnLangMalay.setOnClickListener {
            switchLanguage("ms")
            hideLanguageDropdown()
        }
    
    }
    private fun toggleLanguageDropdown() {
        isDropdownVisible = !isDropdownVisible
        binding.languageDropdown.visibility = if (isDropdownVisible) {
            android.view.View.VISIBLE
        } else {
            android.view.View.GONE
        }
    }
    
    private fun hideLanguageDropdown() {
        isDropdownVisible = false
        binding.languageDropdown.visibility = android.view.View.GONE
    }
    
    private fun updateLanguageDisplay() {
        val languageCode = when (currentLanguage) {
            "zh" -> "ZH"
            "en" -> "EN"
            "ms" -> "MS"
            else -> "ZH"
        }
        binding.tvLanguageCode.text = languageCode
    }
    
    private fun switchLanguage(langCode: String) {
        currentLanguage = langCode
        
        // 保存语言设置
        val prefs = getSharedPreferences("AppSettings", MODE_PRIVATE)
        prefs.edit().putString(PREF_LANGUAGE, currentLanguage).apply()
        
        Log.d(TAG, "switchLanguage: Switched to $currentLanguage")
        
        // 更新TTS语言
        if (isTTSReady) {
            setTTSLanguage()
        }
        
        // 更新显示
        updateLanguageDisplay()
        
        // 重新创建Activity以应用新语言
        recreate()
    }
    
    /**
     * 初始化数字人（暂时只使用静态方案）
     */
    private fun initDigitalHuman() {
        try {
            Log.d(TAG, "初始化数字人（静态方案）...")
            isUsingWebViewSolution = false
            
            // 创建静态动画视图
            digitalHumanAnimationView = DigitalHumanAnimationView(this).apply {
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
            }
            
            // 添加到容器
            binding.digitalHumanContainer.removeAllViews()
            binding.digitalHumanContainer.addView(digitalHumanAnimationView)
            
            // 启动动画
            digitalHumanAnimationView?.startAnimations()
            
            Log.d(TAG, "静态数字人初始化成功")
            
        } catch (e: Exception) {
            Log.e(TAG, "数字人初始化失败", e)
            Toast.makeText(this, "数字人初始化失败", Toast.LENGTH_SHORT).show()
        }
    }
    
    /* 暂时注释WebView方案，先解决构建问题
    private fun initDigitalHumanWithSDK() {
        // SDK WebView代码...
    }
    
    private fun fallbackToStaticDigitalHuman() {
        // fallback代码...
    }
    */
    
    /**
     * 初始化文字转语音（TTS）
     */
    private fun initTextToSpeech() {
        textToSpeech = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                // 设置语言
                val result = setTTSLanguage()
                
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.e(TAG, "TTS language not supported: $currentLanguage")
                    Toast.makeText(this, "当前语言不支持语音播报", Toast.LENGTH_SHORT).show()
                } else {
                    isTTSReady = true
                    Log.d(TAG, "TTS initialized successfully")
                    
                    // TTS准备就绪后，延迟2秒播放欢迎语
                    handler.postDelayed({
                        speakWelcomeMessage()
                    }, 2000)
                }
            } else {
                Log.e(TAG, "TTS initialization failed")
                Toast.makeText(this, "语音功能初始化失败", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    /**
     * 设置TTS语言
     */
    private fun setTTSLanguage(): Int {
        val locale = when (currentLanguage) {
            "zh" -> Locale.CHINESE
            "en" -> Locale.ENGLISH
            "ms" -> Locale("ms", "MY")
            else -> Locale.CHINESE
        }
        return textToSpeech?.setLanguage(locale) ?: TextToSpeech.LANG_NOT_SUPPORTED
    }
    
    /**
     * 播报语音
     */
    private fun speak(text: String) {
        if (isTTSReady && textToSpeech != null) {
            textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
            Log.d(TAG, "Speaking: $text")
        } else {
            Log.w(TAG, "TTS not ready, cannot speak")
        }
    }
    
    /**
     * 播报欢迎语
     */
    private fun speakWelcomeMessage() {
        val message = when (currentLanguage) {
            "zh" -> "欢迎来到亚太科技大学！我是您的智能导览助手，很高兴为您服务。"
            "en" -> "Welcome to Asia Pacific University! I am your intelligent guide, happy to serve you."
            "ms" -> "Selamat datang ke Asia Pacific University! Saya adalah pemandu pintar anda."
            else -> "欢迎来到亚太科技大学！"
        }
        speakMessage(message)
    }
    
    /**
     * 统一的语音播报方法（当前只使用TTS）
     */
    private fun speakMessage(text: String) {
        // 暂时只使用TTS
        speak(text)
        Log.d(TAG, "使用TTS播报: $text")
    }

    private fun initButtons() {
        // AI拍照 - 跳转到SnapScene Camera过渡页（MainActivity）
        binding.btnAiCamera.setOnClickListener {
            Log.d(TAG, "btnAiCamera clicked")
            
            // 播报提示语
            val message = when (currentLanguage) {
                "zh" -> "正在为您打开智能拍照功能"
                "en" -> "Opening AI camera for you"
                "ms" -> "Membuka kamera AI untuk anda"
                else -> "正在为您打开智能拍照功能"
            }
            speakMessage(message)
            
            // 延迟跳转，让语音播放
            handler.postDelayed({
                startActivity(Intent(this, MainActivity::class.java))
            }, 2000)
        }

        // 导航学校 - 跳转到新的SchoolMapActivity
        binding.btnNavigation.setOnClickListener {
            Log.d(TAG, "btnNavigation clicked")
            
            // 播报提示语
            val message = when (currentLanguage) {
                "zh" -> "为您展示校园地图"
                "en" -> "Showing campus map for you"
                "ms" -> "Menunjukkan peta kampus"
                else -> "为您展示校园地图"
            }
            speakMessage(message)
            
            // 延迟跳转
            handler.postDelayed({
                startActivity(Intent(this, SchoolMapActivity::class.java))
            }, 2000)
        }

        // 预约参观 - 跳转到新的BookingActivity
        binding.btnBooking.setOnClickListener {
            Log.d(TAG, "btnBooking clicked")
            
            // 播报提示语
            val message = when (currentLanguage) {
                "zh" -> "正在为您打开预约系统"
                "en" -> "Opening booking system for you"
                "ms" -> "Membuka sistem tempahan"
                else -> "正在为您打开预约系统"
            }
            speakMessage(message)
            
            // 延迟跳转
            handler.postDelayed({
                startActivity(Intent(this, BookingActivity::class.java))
            }, 2000)
        }
    }

    private fun startClock() {
        clockRunnable = object : Runnable {
            override fun run() {
                updateTimeAndDate()
                handler.postDelayed(this, 1000) // 每秒更新一次
            }
        }
        handler.post(clockRunnable)
    }

    private fun updateTimeAndDate() {
        val currentTime = System.currentTimeMillis()
        
        // 时间格式：HH:mm:ss
        val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        binding.tvTime.text = timeFormat.format(currentTime)
        
        // 日期格式：完整版 "2025年12月20日 星期五"
        val dateFormat = SimpleDateFormat("yyyy年M月d日 EEEE", Locale.getDefault())
        binding.tvDate.text = dateFormat.format(currentTime)
    }

    override fun onResume() {
        super.onResume()
        // 恢复数字人动画（静态方案）
        digitalHumanAnimationView?.resumeAnimations()
        // WebView会自动恢复
    }
    
    override fun onPause() {
        super.onPause()
        // 暂停数字人动画（静态方案）
        digitalHumanAnimationView?.pauseAnimations()
        
        // 停止TTS播放
        textToSpeech?.stop()
    }

    override fun onDestroy() {
        super.onDestroy()
        
        // 停止时钟更新
        handler.removeCallbacks(clockRunnable)
        
        // 清理静态动画视图
        digitalHumanAnimationView?.stopAnimations()
        digitalHumanAnimationView = null
        
        // 释放TTS资源
        textToSpeech?.stop()
        textToSpeech?.shutdown()
        textToSpeech = null
        
        Log.d(TAG, "onDestroy: All resources cleaned up")
    }
}
