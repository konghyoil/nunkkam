package com.its.nunkkam.android

import android.content.Context
import android.content.SharedPreferences

object UserManager {

    private const val PREF_NAME = "UserPrefs"
    private const val USER_ID_KEY = "user_id"
    private lateinit var sharedPreferences: SharedPreferences

    var userId: String? = null
        private set

    fun initialize(context: Context) {
        sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        userId = sharedPreferences.getString(USER_ID_KEY, null)
    }

    fun setUser(userId: String) {
        this.userId = userId
        with(sharedPreferences.edit()) {
            putString(USER_ID_KEY, userId)
            apply()
        }
    }

    fun clearUserData(context: Context) {
        sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        with(sharedPreferences.edit()) {
            remove(USER_ID_KEY)
            apply()
        }
        userId = null
    }
}
