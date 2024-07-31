package com.its.nunkkam.android

import android.app.Application

// MyApplication 클래스: 애플리케이션 전역 상태를 관리하는 클래스
class MyApplication : Application() {

    // onCreate 메서드: 애플리케이션이 처음 생성될 때 호출됨
    override fun onCreate() {
        super.onCreate()

        // UserManager를 초기화
        // 이를 통해 앱 전체에서 사용자 정보에 접근할 수 있게 됨
        UserManager.initialize(this)
    }
}