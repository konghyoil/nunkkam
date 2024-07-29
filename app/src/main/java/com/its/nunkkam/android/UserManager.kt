package com.its.nunkkam.android

import android.content.Context
import android.content.SharedPreferences
import android.util.Log

object UserManager {
    private const val PREFS_NAME = "UserPrefs"
    private const val USER_ID_KEY = "user_id"
    private const val USER_NAME_KEY = "user_name"
    private const val FIRST_LOGIN_KEY = "is_first_login"

    var userId: String? = null
        private set

    fun initialize(context: Context) {
        val sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        userId = sharedPreferences.getString(USER_ID_KEY, null)
        Log.d("UserManager", "UserManager initialized with userId: $userId")
    }

    fun clearUserData(context: Context) {
        val sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        with(sharedPreferences.edit()) {
            remove(USER_ID_KEY)
            remove(USER_NAME_KEY)
            remove(FIRST_LOGIN_KEY)
            apply()
        }
        userId = null
        Log.d("UserManager", "User data cleared")
    }
}
