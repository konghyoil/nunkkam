package com.its.nunkkam.android

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth

class TimerActivity : AppCompatActivity() {

    private lateinit var startButton: Button
    private lateinit var resultButton: Button
    private lateinit var timerTextView: TextView
    private lateinit var logoutButton: Button
    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_timer)

        startButton = findViewById(R.id.start_button)
        resultButton = findViewById(R.id.result_button)
        logoutButton = findViewById(R.id.logout_button)
        timerTextView = findViewById(R.id.timer_text)

        // Firebase Auth 초기화
        auth = FirebaseAuth.getInstance()

        // GoogleSignInClient 초기화
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        startButton.setOnClickListener {
            val intent = Intent(this, BlinkActivity::class.java)
            startActivity(intent)
        }

        startButton.setOnClickListener {
            val intent = Intent(this, BlinkActivity::class.java)
            startActivity(intent)
        }

        resultButton.setOnClickListener {
            goToResultScreen()
        }

        logoutButton.setOnClickListener {
            logoutUser()
        }
    }

    private fun goToResultScreen() {
        val intent = Intent(this, ResultActivity::class.java)
        startActivity(intent)
    }

    private fun logoutUser() {
        // Firebase에서 로그아웃
        auth.signOut()
        Log.d("TimerActivity", "Firebase 로그아웃 성공")

        // Google에서 로그아웃
        googleSignInClient.signOut().addOnCompleteListener(this) {
            if (it.isSuccessful) {
                Log.d("TimerActivity", "Google 로그아웃 성공")
            } else {
                Log.d("TimerActivity", "Google 로그아웃 실패: ${it.exception?.message}")
            }

            // SharedPreferences에서 로그아웃 처리
            UserManager.clearUserData(this)
            Log.d("TimerActivity", "SharedPreferences 데이터 삭제 성공")

            // MainActivity로 이동
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
            Log.d("TimerActivity", "MainActivity로 이동")
        }
    }
}
