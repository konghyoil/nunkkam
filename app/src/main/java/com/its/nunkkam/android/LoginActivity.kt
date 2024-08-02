package com.its.nunkkam.android                                      // 패키지 선언

import android.content.Intent                                        // Intent 클래스 임포트
import android.os.Bundle                                             // Bundle 클래스 임포트
import android.view.View                                             // View 클래스 임포트
import android.widget.Button                                         // Button 위젯 임포트
import androidx.appcompat.app.AppCompatActivity                      // AppCompatActivity 클래스 임포트

class LoginActivity : AppCompatActivity() {                          // AppCompatActivity를 상속받는 LoginActivity 클래스 정의
    private lateinit var guestLoginButton: Button                    // 게스트 로그인 버튼 선언
    private lateinit var googleLoginButton: Button                   // Google 로그인 버튼 선언

    override fun onCreate(savedInstanceState: Bundle?) {             // 액티비티 생성 시 호출되는 메서드
        super.onCreate(savedInstanceState)                           // 부모 클래스의 onCreate 메서드 호출
        setContentView(R.layout.activity_login)                      // 레이아웃 설정

        guestLoginButton = findViewById(R.id.guest_login_button)     // 게스트 로그인 버튼 초기화
        googleLoginButton = findViewById(R.id.google_login_button)   // Google 로그인 버튼 초기화

        // 게스트 로그인 버튼 클릭 리스너 설정
        guestLoginButton.setOnClickListener {
            // 게스트 로그인 처리
            val intent = Intent(this, MainActivity::class.java)      // MainActivity로 이동하는 인텐트 생성
            startActivity(intent)                                    // MainActivity 시작
        }

        // Google 로그인 버튼 클릭 리스너 설정
        googleLoginButton.setOnClickListener {
            // Google 로그인 처리
            // 여기에 Google Sign-In 통합 코드를 추가해야 함
        }
    }
}