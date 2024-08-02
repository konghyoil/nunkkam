package com.its.nunkkam.android

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
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
    private lateinit var deleteAccountButton: Button
    private lateinit var timerTextView: TextView
    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_timer)

        val app = application as MyApplication
        auth = app.auth
        googleSignInClient = app.googleSignInClient

        startButton = findViewById(R.id.start_button)
        resultButton = findViewById(R.id.result_button)
        logoutButton = findViewById(R.id.logout_button)
        deleteAccountButton = findViewById(R.id.delete_account_button)
        timerTextView = findViewById(R.id.timer_text)

        startButton.setOnClickListener {
            val intent = Intent(this, BlinkActivity::class.java)
            startActivity(intent)
        }

        resultButton.setOnClickListener {
            goToResultScreen()
            Log.d("TimerActivity", "userId: $userId")
        }

        logoutButton.setOnClickListener {
            logoutUser()
            Log.d("TimerActivity", "userId: $userId")
        }

        deleteAccountButton.setOnClickListener {
            deleteUserAccount()
        }
        checkCurrentUser()

        // AlarmFragment 추가
        addAlarmFragment()

    }

    private fun checkCurrentUser() {
        val currentUser = auth.currentUser
        Log.d("TimerActivity", "Current user in TimerActivity: $currentUser")
    }

    private fun goToResultScreen() {
        val intent = Intent(this, ResultActivity::class.java)
        startActivity(intent)
        checkCurrentUser()
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

            // 사용자 데이터 초기화 (삭제하지 않음)
            UserManager.clearUserData()
            Log.d("TimerActivity", "UserManager 데이터 초기화 성공")

            // MainActivity로 이동
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
            Log.d("TimerActivity", "MainActivity로 이동")
        }
    }

    private fun deleteUserAccount() {
        // FirebaseAuth 인스턴스를 통해 현재 사용자 정보를 가져옴
        val user = auth.currentUser

        // 현재 사용자가 존재하는 경우 아래의 작업을 수행
        user?.let {
            val userId = it.uid // 현재 사용자의 고유 ID를 가져옴

            // Firestore에서 사용자 데이터를 삭제
            db.collection("USERS").document(userId).delete().addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d("TimerActivity", "User data deleted from Firestore")

                    // Firebase Authentication에서 사용자 계정을 삭제
                    user.delete().addOnCompleteListener { deleteTask ->
                        if (deleteTask.isSuccessful) {
                            Log.d("TimerActivity", "User deleted from Firebase Auth")

                            // SharedPreferences에서 사용자 데이터 삭제
                            UserManager.deleteUserData(this)

                            // 메인 화면(MainActivity)으로 이동
                            val intent = Intent(this, MainActivity::class.java)
                            startActivity(intent)
                            finish()
                        } else {
                            // Firebase Auth에서 사용자 삭제 중 오류 발생 시 로그 출력
                            Log.e("TimerActivity", "Error deleting user from Firebase Auth", deleteTask.exception)
                        }
                    }
                } else {
                    // Firestore에서 사용자 데이터 삭제 중 오류 발생 시 로그 출력
                    Log.e("TimerActivity", "Error deleting user data from Firestore", task.exception)
                }
            }
        } ?: run {
            // 현재 사용자가 null인 경우 로그 출력
            Log.e("TimerActivity", "User is null")
        }
    }

    private fun addAlarmFragment() {
        val fragment: Fragment = AlarmFragment()
        supportFragmentManager.beginTransaction()
            .replace(R.id.alarm_fragment_container, fragment)
            .commit()
    }
}
