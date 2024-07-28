package com.its.nunkkam.android

import android.content.Context
import android.util.Log

object UserManager {
    var userId: String? = null
    var userName: String? = null

    fun initialize(context: Context) {
        val sharedPreferences = context.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        userId = sharedPreferences.getString("user_id", null)
        userName = sharedPreferences.getString("user_name", "Guest User")
        Log.d("UserManager", "User ID: $userId, User Name: $userName")
    }

    fun clearUserData(context: Context) {
        val sharedPreferences = context.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        with(sharedPreferences.edit()) {
            clear()
            apply()
        }
        userId = null
        userName = null
        Log.d("UserManager", "User data cleared")

    }
}
