package com.its.nunkkam.android

import android.app.Application

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // UserManager를 초기화할 때 userId와 isGoogleLogin은 null과 false로 초기화
        UserManager.initialize(this)
    }
}
