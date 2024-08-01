package com.its.nunkkam.android

import android.app.Application
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth

// MyApplication 클래스: 애플리케이션 전역 상태를 관리하는 클래스
class MyApplication : Application() {

    lateinit var auth: FirebaseAuth
    lateinit var googleSignInClient: GoogleSignInClient

    // onCreate 메서드: 애플리케이션이 처음 생성될 때 호출됨
    override fun onCreate() {
        super.onCreate()

        // UserManager를 초기화
        UserManager.initialize(this)

        // Firebase Auth 초기화
        auth = FirebaseAuth.getInstance()

        // GoogleSignInClient 초기화
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id)) // 여기에 웹 클라이언트 ID를 입력합니다.
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)
    }
}
