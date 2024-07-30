package com.its.nunkkam.android

import android.app.Application

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        UserManager.initialize(this)
    }
}
