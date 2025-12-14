package com.example.snapscenecamera

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import com.example.snapscenecamera.databinding.ActivitySettingsBinding

class SettingsActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySettingsBinding
    private lateinit var prefs: SharedPreferences

    companion object {
        const val PREFS_NAME = "AppSettings"
        const val KEY_RESOLUTION = "photo_resolution"
        const val KEY_GUIDE_LINES = "guide_lines"
        const val KEY_AUTO_FOCUS = "auto_focus"
        const val KEY_DEFAULT_BG = "default_background"
        const val KEY_SAVE_ORIGINAL = "save_original"
        
        private const val TAG = "SettingsActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate: SettingsActivity started")
        
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        setupUI()
        loadSettings()
    }

    private fun setupUI() {
        // Back button
        binding.btnBack.setOnClickListener {
            finish()
        }

        // Photo Resolution Spinner
        val resolutions = arrayOf("High (1080p)", "Medium (720p)", "Low (480p)")
        val resolutionAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, resolutions)
        resolutionAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerResolution.adapter = resolutionAdapter
        binding.spinnerResolution.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val value = when (position) {
                    0 -> "high"
                    1 -> "medium"
                    2 -> "low"
                    else -> "high"
                }
                saveSetting(KEY_RESOLUTION, value)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Default Background Spinner
        val backgrounds = arrayOf("White", "Red", "Blue", "Green", "Yellow", "Purple", "Pink", "Black")
        val backgroundAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, backgrounds)
        backgroundAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerDefaultBg.adapter = backgroundAdapter
        binding.spinnerDefaultBg.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                saveSetting(KEY_DEFAULT_BG, position.toString())
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Guide Lines Switch
        binding.switchGuideLines.setOnCheckedChangeListener { _, isChecked ->
            saveSetting(KEY_GUIDE_LINES, isChecked)
        }

        // Auto Focus Switch
        binding.switchAutoFocus.setOnCheckedChangeListener { _, isChecked ->
            saveSetting(KEY_AUTO_FOCUS, isChecked)
        }

        // Save Original Switch
        binding.switchSaveOriginal.setOnCheckedChangeListener { _, isChecked ->
            saveSetting(KEY_SAVE_ORIGINAL, isChecked)
        }
    }

    private fun loadSettings() {
        Log.d(TAG, "loadSettings: Loading settings from SharedPreferences")
        
        // Load photo resolution
        val resolution = prefs.getString(KEY_RESOLUTION, "high")
        val resolutionPosition = when (resolution) {
            "high" -> 0
            "medium" -> 1
            "low" -> 2
            else -> 0
        }
        binding.spinnerResolution.setSelection(resolutionPosition)

        // Load default background
        val defaultBg = prefs.getString(KEY_DEFAULT_BG, "0")?.toIntOrNull() ?: 0
        binding.spinnerDefaultBg.setSelection(defaultBg)

        // Load guide lines setting
        val guideLines = prefs.getBoolean(KEY_GUIDE_LINES, false)
        binding.switchGuideLines.isChecked = guideLines

        // Load auto focus setting
        val autoFocus = prefs.getBoolean(KEY_AUTO_FOCUS, true)
        binding.switchAutoFocus.isChecked = autoFocus

        // Load save original setting
        val saveOriginal = prefs.getBoolean(KEY_SAVE_ORIGINAL, false)
        binding.switchSaveOriginal.isChecked = saveOriginal

        Log.d(TAG, "loadSettings: Settings loaded - resolution=$resolution, bg=$defaultBg, " +
                "guideLines=$guideLines, autoFocus=$autoFocus, saveOriginal=$saveOriginal")
    }

    private fun saveSetting(key: String, value: String) {
        prefs.edit().putString(key, value).apply()
        Log.d(TAG, "saveSetting: Saved $key = $value")
    }

    private fun saveSetting(key: String, value: Boolean) {
        prefs.edit().putBoolean(key, value).apply()
        Log.d(TAG, "saveSetting: Saved $key = $value")
    }
}
