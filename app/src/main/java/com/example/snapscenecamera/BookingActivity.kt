package com.example.snapscenecamera

import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.snapscenecamera.databinding.ActivityBookingBinding

class BookingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBookingBinding

    companion object {
        private const val TAG = "BookingActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate: BookingActivity started")

        binding = ActivityBookingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
    }

    private fun setupUI() {
        // 返回按钮
        binding.btnBack.setOnClickListener {
            finish()
        }

        // 提交按钮
        binding.btnSubmit.setOnClickListener {
            if (validateForm()) {
                submitBooking()
            }
        }
    }

    private fun validateForm(): Boolean {
        val name = binding.etName.text.toString().trim()
        val email = binding.etEmail.text.toString().trim()
        val phone = binding.etPhone.text.toString().trim()

        return when {
            name.isBlank() -> {
                binding.etName.error = getString(R.string.error_name_required)
                binding.etName.requestFocus()
                Toast.makeText(this, getString(R.string.error_name_required), Toast.LENGTH_SHORT).show()
                Log.w(TAG, "validateForm: Name is blank")
                false
            }
            !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                binding.etEmail.error = getString(R.string.error_email_invalid)
                binding.etEmail.requestFocus()
                Toast.makeText(this, getString(R.string.error_email_invalid), Toast.LENGTH_SHORT).show()
                Log.w(TAG, "validateForm: Email is invalid")
                false
            }
            phone.length < 10 -> {
                binding.etPhone.error = getString(R.string.error_phone_invalid)
                binding.etPhone.requestFocus()
                Toast.makeText(this, getString(R.string.error_phone_invalid), Toast.LENGTH_SHORT).show()
                Log.w(TAG, "validateForm: Phone number too short")
                false
            }
            else -> {
                Log.d(TAG, "validateForm: All fields valid")
                true
            }
        }
    }

    private fun submitBooking() {
        val name = binding.etName.text.toString().trim()
        val email = binding.etEmail.text.toString().trim()
        val phone = binding.etPhone.text.toString().trim()

        Log.d(TAG, "submitBooking: Name=$name, Email=$email, Phone=$phone")

        // 显示成功提示
        Toast.makeText(
            this,
            getString(R.string.booking_success),
            Toast.LENGTH_LONG
        ).show()

        // 关闭页面
        finish()
    }
}
