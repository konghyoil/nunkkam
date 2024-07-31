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
import com.google.firebase.firestore.FirebaseFirestore
import com.its.nunkkam.android.UserManager.userId

class TimerActivity : AppCompatActivity() {

    private lateinit var startButton: Button
    private lateinit var resultButton: Button
    private lateinit var logoutButton: Button
    private lateinit var deleteAccountButton: Button // 회원탈퇴 버튼 추가
    private lateinit var timerTextView: TextView
    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient
    private val db = FirebaseFirestore.getInstance()



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_timer)

        startButton = findViewById(R.id.start_button)
        resultButton = findViewById(R.id.result_button)
        logoutButton = findViewById(R.id.logout_button)
        deleteAccountButton = findViewById(R.id.delete_account_button) // 회원탈퇴 버튼 초기화
        timerTextView = findViewById(R.id.timer_text)

        // Firebase Auth 초기화
        auth = FirebaseAuth.getInstance()

        // GoogleSignInClient 초기화
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        val currentUser = auth.currentUser
        Log.d("TimerActivity", "Current user after initialization: $currentUser")

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
            Log.d("TimerActivity","userId: $userId")
            Log.d("TimerActivity","currentUser: $currentUser")
        }

        logoutButton.setOnClickListener {
            logoutUser()
            Log.d("TimerActivity", "userId: $userId")
        }

        deleteAccountButton.setOnClickListener {
            deleteUserAccount()
        }
        checkCurrentUser()
    }

    private fun checkCurrentUser() {
        val currentUser = auth.currentUser
        Log.d("TimerActivity", "Current user in TimerActivity: $currentUser")
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
        Log.d("TimerActivity", "userId: $userId")
    }

    private fun deleteUserAccount() {
        val user = auth.currentUser
        Log.d("TimerActivity", "Current user: $user")

        user?.let {
            val userId = it.uid
            Log.d("TimerActivity", "User ID: $userId")

            // Firestore에서 사용자 데이터 삭제
            db.collection("USERS").document(userId).delete().addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d("TimerActivity", "User data deleted from Firestore")

                    // Firebase Authentication에서 사용자 삭제
                    user.delete().addOnCompleteListener { deleteTask ->
                        if (deleteTask.isSuccessful) {
                            Log.d("TimerActivity", "User deleted from Firebase Auth")

                            // SharedPreferences에서 사용자 데이터 삭제
                            UserManager.clearUserData(this)

                            // 메인 화면으로 이동
                            val intent = Intent(this, MainActivity::class.java)
                            startActivity(intent)
                            finish()
                        } else {
                            Log.e("TimerActivity", "Error deleting user from Firebase Auth", deleteTask.exception)
                        }
                    }
                } else {
                    Log.e("TimerActivity", "Error deleting user data from Firestore", task.exception)
                }
            }
        } ?: run {
            Log.e("TimerActivity", "User is null")
        }
    }

    }




