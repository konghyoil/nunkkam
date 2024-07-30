package com.its.nunkkam.android

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.util.UUID

class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var guestLoginButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        guestLoginButton = findViewById(R.id.guest_login_button)

        // Firebase Auth 초기화
        auth = FirebaseAuth.getInstance()

        guestLoginButton.setOnClickListener {
            Log.d("MainActivity", "Guest Login button clicked")
            initializeGuestUser()
        }

        checkUserStatus()
    }

    private fun initializeGuestUser() {
        val sharedPreferences = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        var userId = sharedPreferences.getString("user_id", null)
        if (userId == null) {
            val deviceModel = Build.MODEL ?: "unknown_device"
            userId = "$deviceModel-${UUID.randomUUID()}"
            with(sharedPreferences.edit()) {
                putString("user_id", userId)
                putString("user_name", "Guest User")
                putBoolean("is_first_login", true)
                apply()
                Log.d("MainActivity", "User ID initialized: $userId")
            }
        } else {
            Log.d("MainActivity", "Using existing User ID: $userId")
        }
        UserManager.setUser(userId)
        navigateToMainFunction()
    }

    private fun navigateToMainFunction() {
        Log.d("MainActivity", "Navigating to main function")
        val intent = Intent(this, TimerActivity::class.java)
        startActivity(intent)
    }

    private fun checkUserStatus() {
        UserManager.initialize(this)
        val userId = UserManager.userId
        if (userId != null) {
            navigateToMainFunction()
        }
    }

    companion object {
        private const val TAG = "MainActivity"
    }
}
