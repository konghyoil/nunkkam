package com.its.nunkkam.android

import android.content.Context
import android.content.SharedPreferences

object UserManager {
    var userId: String = ""
        private set
    var isGoogleLogin: Boolean = false
        private set

    fun initialize(context: Context) {
        val sharedPreferences = context.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        userId = sharedPreferences.getString("user_id", "") ?: ""
        isGoogleLogin = sharedPreferences.getBoolean("is_google_login", false)
    }

    fun setUser(context: Context, userId: String, isGoogleLogin: Boolean) {
        val sharedPreferences = context.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        with(sharedPreferences.edit()) {
            putString("user_id", userId)
            putBoolean("is_google_login", isGoogleLogin)
            apply()
        }
        this.userId = userId
        this.isGoogleLogin = isGoogleLogin
    }

    fun clearUserData(context: Context) {
        val sharedPreferences = context.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        with(sharedPreferences.edit()) {
            remove("user_id")
            remove("user_name")
            remove("is_first_login")
            remove("is_google_login")
            apply()
        }
        userId = ""
        isGoogleLogin = false
    }
}
