package com.its.nunkkam.android

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
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

//        guestLoginButton.setOnClickListener {
//            Log.d("MainActivity", "Guest Login button clicked")
//            initializeGuestUser()
//            UserManager.initialize(this) // UserManager 초기화
//            Log.d("MainActivity", "User ID: ${UserManager.userId}, User Name: ${UserManager.userName}")
//            navigateToMainFunction()
//        }

        googleLoginButton.setOnClickListener {
            // 구글 로그인 처리 (현재는 구현하지 않음)
        }
        logoutButton.setOnClickListener {
            Log.d("MainActivity", "Logout button clicked")
            logoutUser()
        }

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
}
