package com.its.nunkkam.android                                      // 패키지 선언

import android.app.Application                                       // Android Application 클래스 임포트
import com.google.android.gms.auth.api.signin.GoogleSignIn           // Google 로그인 관련 클래스 임포트
import com.google.android.gms.auth.api.signin.GoogleSignInClient     // Google 로그인 클라이언트 클래스 임포트
import com.google.android.gms.auth.api.signin.GoogleSignInOptions    // Google 로그인 옵션 클래스 임포트
import com.google.firebase.auth.FirebaseAuth                         // Firebase 인증 클래스 임포트
import com.its.nunkkam.android.managers.UserManager

class MyApplication : Application() {                                // Application 클래스를 상속받는 MyApplication 클래스 정의

    lateinit var auth: FirebaseAuth                                  // Firebase 인증 객체 선언
    lateinit var googleSignInClient: GoogleSignInClient              // Google 로그인 클라이언트 객체 선언

    override fun onCreate() {                                        // 애플리케이션 생성 시 호출되는 메서드
        super.onCreate()                                             // 부모 클래스의 onCreate 메서드 호출

        UserManager.initialize(this)                                 // UserManager 초기화

        auth = FirebaseAuth.getInstance()                            // Firebase 인증 인스턴스 가져오기

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)  // Google 로그인 옵션 빌더 생성
            .requestIdToken(getString(R.string.default_web_client_id))  // 웹 클라이언트 ID 요청
            .requestEmail()                                          // 이메일 정보 요청
            .build()                                                 // GoogleSignInOptions 객체 생성
        googleSignInClient = GoogleSignIn.getClient(this, gso)       // GoogleSignInClient 초기화
    }
}