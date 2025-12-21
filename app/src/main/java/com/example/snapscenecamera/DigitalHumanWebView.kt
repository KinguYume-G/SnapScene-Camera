package com.example.snapscenecamera

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.webkit.*
import android.widget.Toast

/**
 * 魔珠星云数字人WebView组件（简化版）
 * 实现3D数字人显示 + TTS播放 + 口型同步
 */
class DigitalHumanWebView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : WebView(context, attrs, defStyleAttr) {

    companion object {
        private const val TAG = "DigitalHumanWebView"
        
        // 魔珠星云SDK CDN链接
        private const val SDK_URL = "https://media.xingyun3d.com/xingyun3d/general/litesdk/xmovAvatar.0.1.0-alpha.75.js"
    }

    // 事件回调
    var onReady: (() -> Unit)? = null
    var onError: ((String) -> Unit)? = null
    var onSpeakStart: (() -> Unit)? = null
    var onSpeakEnd: (() -> Unit)? = null

    init {
        setupWebView()
        loadDigitalHumanPage()
    }

    /**
     * 配置WebView设置
     */
    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        settings.apply {
            // 启用JavaScript（必须）
            javaScriptEnabled = true
            
            // 启用DOM存储
            domStorageEnabled = true
            
            // 启用数据库
            databaseEnabled = true
            
            // 允许文件访问
            allowFileAccess = true
            allowContentAccess = true
            
            // 缓存模式
            cacheMode = WebSettings.LOAD_DEFAULT
            
            // 禁用缩放
            setSupportZoom(false)
            builtInZoomControls = false
            
            // 自适应屏幕
            useWideViewPort = true
            loadWithOverviewMode = true
            
            // 硬件加速（提升3D渲染性能）
            setRenderPriority(WebSettings.RenderPriority.HIGH)
            
            // 混合内容模式（允许HTTPS加载HTTP资源）
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        }

        // WebViewClient - 处理页面加载
        webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                Log.d(TAG, "Page loaded successfully")
            }

            override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: WebResourceError?
            ) {
                super.onReceivedError(view, request, error)
                Log.e(TAG, "WebView error: ${error?.description}")
                
                // 只在主资源加载失败时通知错误
                if (request?.isForMainFrame == true) {
                    post {
                        onError?.invoke("加载失败: ${error?.description}")
                    }
                }
            }
        }

        // WebChromeClient - 处理JavaScript console和权限
        webChromeClient = object : WebChromeClient() {
            override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                Log.d(TAG, "Console [${consoleMessage?.messageLevel()}]: ${consoleMessage?.message()}")
                return true
            }
            
            override fun onPermissionRequest(request: PermissionRequest?) {
                // 自动授权音频权限
                request?.grant(request.resources)
            }
        }

        // JavaScript Bridge
        addJavascriptInterface(JavaScriptBridge(), "AndroidBridge")
        
        Log.d(TAG, "WebView configured successfully")
    }

    /**
     * 加载数字人页面
     */
    private fun loadDigitalHumanPage() {
        val htmlContent = generateHTML()
        loadDataWithBaseURL(
            "https://xingyun3d.com",
            htmlContent,
            "text/html",
            "UTF-8",
            null
        )
    }

    /**
     * 生成HTML页面
     */
    private fun generateHTML(): String {
        return """
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
    <title>数字人</title>
    <style>
        * {
            margin: 0;
            padding: 0;
            box-sizing: border-box;
        }
        
        body {
            width: 100vw;
            height: 100vh;
            overflow: hidden;
            background: transparent;
        }
        
        #container {
            width: 100%;
            height: 100%;
            position: relative;
        }
        
        #loading {
            position: fixed;
            top: 50%;
            left: 50%;
            transform: translate(-50%, -50%);
            color: rgba(255, 255, 255, 0.8);
            font-size: 16px;
            text-align: center;
            z-index: 999;
        }
        
        .spinner {
            border: 3px solid rgba(255, 255, 255, 0.3);
            border-top: 3px solid white;
            border-radius: 50%;
            width: 40px;
            height: 40px;
            animation: spin 1s linear infinite;
            margin: 0 auto 10px;
        }
        
        @keyframes spin {
            0% { transform: rotate(0deg); }
            100% { transform: rotate(360deg); }
        }
    </style>
</head>
<body>
    <!-- 加载提示 -->
    <div id="loading">
        <div class="spinner"></div>
        <div>数字人加载中...</div>
    </div>
    
    <!-- 数字人容器 -->
    <div id="container"></div>

    <!-- 引入魔珠星云SDK -->
    <script src="$SDK_URL" onerror="handleSDKLoadError()"></script>
    
    <script>
        let avatar = null;
        let isReady = false;
        
        /**
         * SDK加载失败处理
         */
        function handleSDKLoadError() {
            console.error('SDK加载失败');
            if (typeof AndroidBridge !== 'undefined') {
                AndroidBridge.onError('SDK脚本加载失败，请检查网络连接');
            }
        }
        
        /**
         * 初始化数字人
         */
        async function initAvatar() {
            try {
                console.log('开始初始化数字人...');
                
                // 检查SDK是否加载
                if (typeof window.XmovAvatar === 'undefined') {
                    throw new Error('XmovAvatar SDK未加载');
                }
                
                // 创建数字人实例
                avatar = new window.XmovAvatar({
                    container: document.getElementById('container'),
                    
                    // 基础配置
                    autoPlay: true,          // 自动播放待机动画
                    enableAudio: true,       // 启用音频
                    enableLipSync: true,     // 启用口型同步
                    
                    // 事件回调
                    onReady: function() {
                        console.log('数字人已准备就绪');
                        isReady = true;
                        hideLoading();
                        notifyAndroid('onReady', {});
                    },
                    
                    onSpeakStart: function() {
                        console.log('开始说话');
                        notifyAndroid('onSpeakStart', {});
                    },
                    
                    onSpeakEnd: function() {
                        console.log('说话结束');
                        notifyAndroid('onSpeakEnd', {});
                    },
                    
                    onError: function(error) {
                        console.error('数字人错误:', error);
                        notifyAndroid('onError', { message: error.message || '未知错误' });
                    }
                });
                
                // 初始化
                await avatar.init();
                
            } catch (error) {
                console.error('初始化失败:', error);
                hideLoading();
                if (typeof AndroidBridge !== 'undefined') {
                    AndroidBridge.onError('数字人初始化失败: ' + error.message);
                }
            }
        }
        
        /**
         * 隐藏加载提示
         */
        function hideLoading() {
            const loading = document.getElementById('loading');
            if (loading) {
                loading.style.display = 'none';
            }
        }
        
        /**
         * 播放文本（带口型同步）
         * @param {string} text - 要播放的文本
         */
        function speak(text) {
            if (!avatar) {
                console.error('数字人未初始化');
                return;
            }
            
            if (!isReady) {
                console.warn('数字人尚未准备就绪');
                return;
            }
            
            console.log('播放文本:', text);
            
            try {
                // 调用SDK的speak方法
                avatar.speak(text);
            } catch (error) {
                console.error('播放失败:', error);
                if (typeof AndroidBridge !== 'undefined') {
                    AndroidBridge.onError('播放失败: ' + error.message);
                }
            }
        }
        
        /**
         * 停止播放
         */
        function stop() {
            if (avatar && typeof avatar.stop === 'function') {
                avatar.stop();
            }
        }
        
        /**
         * 通知Android层
         * @param {string} event - 事件名称
         * @param {object} data - 事件数据
         */
        function notifyAndroid(event, data) {
            if (typeof AndroidBridge !== 'undefined') {
                try {
                    AndroidBridge.onEvent(event, JSON.stringify(data));
                } catch (error) {
                    console.error('通知Android失败:', error);
                }
            }
        }
        
        // 页面加载完成后初始化
        window.addEventListener('load', function() {
            console.log('页面加载完成，开始初始化数字人');
            setTimeout(initAvatar, 500); // 延迟500ms确保SDK加载完成
        });
        
        // 错误处理
        window.addEventListener('error', function(event) {
            console.error('全局错误:', event.error);
        });
    </script>
</body>
</html>
        """.trimIndent()
    }

    /**
     * Android调用：让数字人说话
     * @param text 要播放的文本
     */
    fun speak(text: String) {
        // 转义单引号避免JavaScript语法错误
        val escapedText = text.replace("'", "\\'")
        evaluateJavascript("speak('$escapedText')") { result ->
            Log.d(TAG, "speak() executed, result: $result")
        }
    }

    /**
     * Android调用：停止播放
     */
    fun stopSpeaking() {
        evaluateJavascript("stop()") { result ->
            Log.d(TAG, "stop() executed, result: $result")
        }
    }

    /**
     * JavaScript Bridge - 接收来自JavaScript的事件
     */
    inner class JavaScriptBridge {
        @JavascriptInterface
        fun onEvent(event: String, dataJson: String) {
            Log.d(TAG, "Received event from JS: $event, data: $dataJson")
            
            // 在主线程处理回调
            post {
                when (event) {
                    "onReady" -> {
                        onReady?.invoke()
                        Toast.makeText(context, "数字人已准备就绪", Toast.LENGTH_SHORT).show()
                    }
                    "onSpeakStart" -> {
                        onSpeakStart?.invoke()
                    }
                    "onSpeakEnd" -> {
                        onSpeakEnd?.invoke()
                    }
                    "onError" -> {
                        try {
                            // 简单解析错误信息
                            val errorMsg = dataJson.substringAfter("message\":\"").substringBefore("\"")
                            onError?.invoke(errorMsg)
                        } catch (e: Exception) {
                            onError?.invoke("未知错误")
                        }
                    }
                }
            }
        }
        
        @JavascriptInterface
        fun onReady() {
            Log.d(TAG, "onReady callback")
            post {
                onReady?.invoke()
            }
        }
        
        @JavascriptInterface
        fun onError(message: String) {
            Log.e(TAG, "onError callback: $message")
            post {
                onError?.invoke(message)
            }
        }
    }
    
    /**
     * 清理资源
     */
    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        
        // 停止播放
        stopSpeaking()
        
        // 清理WebView
        clearHistory()
        clearCache(true)
        loadUrl("about:blank")
        
        // 移除JavaScript接口
        removeJavascriptInterface("AndroidBridge")
        
        // 清理回调
        onReady = null
        onError = null
        onSpeakStart = null
        onSpeakEnd = null
        
        // 销毁WebView
        destroy()
        
        Log.d(TAG, "Resources cleaned up")
    }
}
