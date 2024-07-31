package com.its.nunkkam.android

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import java.util.UUID

class MainActivity : AppCompatActivity() {

    private lateinit var guestLoginButton: Button
    private lateinit var googleLoginButton: Button
    private lateinit var logoutButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        guestLoginButton = findViewById(R.id.guest_login_button)
        googleLoginButton = findViewById(R.id.google_login_button)
        logoutButton = findViewById(R.id.logout_button)

        val deviceModel = Build.MODEL ?: "unknown_device"
        Log.d("MainActivity", "Device model: $deviceModel") // 디버그 로그로 기기 모델명 확인

        guestLoginButton.setOnClickListener {
            Log.d("MainActivity", "Guest Login button clicked")
            initializeGuestUser()
            navigateToMainFunction()
        }

        googleLoginButton.setOnClickListener {
            // 구글 로그인 처리 (현재는 구현하지 않음)
        }

        logoutButton.setOnClickListener {
            Log.d("MainActivity", "Logout button clicked")
            logoutUser()
        }

        createNotificationChannel()
        requestNotificationPermission()
    }

    private fun initializeGuestUser() {
        val sharedPreferences = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        val deviceModel = Build.MODEL ?: "unknown_device"
        val userId = "$deviceModel-${UUID.randomUUID()}"
        with(sharedPreferences.edit()) {
            if (sharedPreferences.getString("user_id", null) == null) {
                putString("user_id", userId)
                Log.d("MainActivity", "User ID initialized: $userId")
            }
            putString("user_name", "Guest User")
            putBoolean("is_first_login", true)
            apply()
        }
    }

    private fun navigateToMainFunction() {
        UserManager.initialize(this) // UserManager 초기화
        Log.d("MainActivity", "Navigating to main function")
        val intent = Intent(this, TimerActivity::class.java)
        startActivity(intent)
    }

    private fun logoutUser() {
        UserManager.clearUserData(this)
        Log.d("MainActivity", "User logged out")
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Alarm Channel"
            val descriptionText = "Channel for Alarm"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel("alarmChannel", name, importance).apply {
                description = descriptionText
            }

            val notificationManager: NotificationManager =
                getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED -> {
                    // 권한이 이미 허용됨
                }
                else -> {
                    // 권한 요청
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // 권한이 허용됨
            Log.d("MainActivity", "Notification permission granted")
        } else {
            // 권한이 거부됨
            Log.d("MainActivity", "Notification permission denied")
        }
    }
}
