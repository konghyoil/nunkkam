package com.its.nunkkam.android

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class LoginActivity : AppCompatActivity() {
    private lateinit var guestLoginButton: Button
    private lateinit var googleLoginButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login) // XML 레이아웃 파일 설정

        guestLoginButton = findViewById(R.id.guest_login_button)
        googleLoginButton = findViewById(R.id.google_login_button)

        // 게스트 로그인 버튼 클릭 시 이벤트 처리
        guestLoginButton.setOnClickListener {
            // 게스트 로그인 처리
            // 예를 들어, 새로운 액티비티 시작
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }

        // 구글 로그인 버튼 클릭 시 이벤트 처리
        googleLoginButton.setOnClickListener {
            // 구글 로그인 처리
            // 여기에서 Google Sign-In 통합
        }
    }
}