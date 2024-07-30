package com.its.nunkkam.android

import android.content.Context
import android.content.SharedPreferences

object UserManager {
    private lateinit var preferences: SharedPreferences
    var userId: String? = null
        private set

    fun initialize(context: Context) {
        preferences = context.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        userId = preferences.getString("user_id", null)
    }

    fun setUserId(id: String) {
        userId = id
        with(preferences.edit()) {
            putString("user_id", id)
            apply()
        }
    }

    fun clearUserData() {
        userId = null
        with(preferences.edit()) {
            clear()
            apply()
        }
    }
}
