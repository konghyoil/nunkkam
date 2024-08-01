package com.its.nunkkam.android

import android.content.Context
import android.content.SharedPreferences


object UserManager {
    private const val PREF_NAME = "UserPrefs"
    private const val USER_ID_KEY = "user_id"
    private const val LOGIN_TYPE_KEY = "login_type"
    const val LOGIN_TYPE_ANONYMOUS = "anonymous"
    const val LOGIN_TYPE_GOOGLE = "google"
    private lateinit var sharedPreferences: SharedPreferences

    var userId: String? = null
        private set
    var loginType: String? = null
        private set

    fun initialize(context: Context) {
        sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        userId = sharedPreferences.getString(USER_ID_KEY, null)
        loginType = sharedPreferences.getString(LOGIN_TYPE_KEY, null)
    }

    fun setUser(userId: String, loginType: String) {
        this.userId = userId
        this.loginType = loginType

        with(sharedPreferences.edit()) {
            putString(USER_ID_KEY, userId)
            putString(LOGIN_TYPE_KEY, loginType)
            apply()
        }
    }

    fun clearUserData() {
        userId = null
        loginType = null
    }

        fun deleteUserData(context: Context) {
        sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        with(sharedPreferences.edit()) {
            remove(USER_ID_KEY)
            remove(LOGIN_TYPE_KEY)
            apply()
        }
        clearUserData()
    }
}
//package com.its.nunkkam.android
//
//import android.content.Context
//import android.content.SharedPreferences
//
//object UserManager {
//    private const val PREF_NAME = "UserPrefs"
//    private const val USER_ID_KEY = "user_id"
//    private const val LOGIN_TYPE_KEY = "login_type"
//    const val LOGIN_TYPE_ANONYMOUS = "anonymous"
//    const val LOGIN_TYPE_GOOGLE = "google"
//    private lateinit var sharedPreferences: SharedPreferences
//
//    var userId: String? = null
//        private set
//    var loginType: String? = null
//        private set
//
//    fun initialize(context: Context) {
//        sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
//        userId = sharedPreferences.getString(USER_ID_KEY, null)
//        loginType = sharedPreferences.getString(LOGIN_TYPE_KEY, null)
//    }
//
//    fun setUser(userId: String, loginType: String) {
//        this.userId = userId
//        this.loginType = loginType
//
//        with(sharedPreferences.edit()) {
//            putString(USER_ID_KEY, userId)
//            putString(LOGIN_TYPE_KEY, loginType)
//            apply()
//        }
//    }
//
//    fun clearUserData() {
//        userId = null
//        loginType = null
//    }
//
//    fun deleteUserData(context: Context) {
//        sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
//        with(sharedPreferences.edit()) {
//            remove(USER_ID_KEY)
//            remove(LOGIN_TYPE_KEY)
//            apply()
//        }
//        clearUserData()
//    }
//}
