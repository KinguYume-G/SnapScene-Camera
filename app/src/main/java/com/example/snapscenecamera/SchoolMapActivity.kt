package com.example.snapscenecamera

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.snapscenecamera.databinding.ActivitySchoolMapBinding

class SchoolMapActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySchoolMapBinding

    companion object {
        private const val TAG = "SchoolMapActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate: SchoolMapActivity started")

        binding = ActivitySchoolMapBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
    }

    private fun setupUI() {
        // 返回按钮
        binding.btnBack.setOnClickListener {
            finish()
        }

        // 设置APU校园地图（使用PhotoView实现缩放和拖动）
        try {
            binding.photoView.apply {
                setImageResource(R.drawable.apu_campus_map)
                minimumScale = 1.0f
                mediumScale = 2.5f
                maximumScale = 5.0f
            }
            
            Toast.makeText(
                this,
                getString(R.string.map_placeholder_hint),
                Toast.LENGTH_LONG
            ).show()
            
            Log.d(TAG, "setupUI: APU campus map loaded successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error loading map", e)
            Toast.makeText(this, "地图加载失败", Toast.LENGTH_SHORT).show()
        }
    }
}
