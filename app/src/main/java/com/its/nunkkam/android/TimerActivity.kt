package com.its.nunkkam.android                                      // 패키지 선언

import android.content.Context                                       // Android Context 클래스 임포트
import android.content.Intent                                        // Intent 클래스 임포트
import android.os.Bundle                                             // Bundle 클래스 임포트
import android.util.Log                                              // Log 클래스 임포트
import android.widget.Button                                         // Button 위젯 임포트
import android.widget.TextView                                       // TextView 위젯 임포트
import androidx.appcompat.app.AppCompatActivity                      // AppCompatActivity 임포트
import com.google.android.gms.auth.api.signin.GoogleSignIn           // Google 로그인 관련 클래스 임포트
import com.google.android.gms.auth.api.signin.GoogleSignInClient     // Google 로그인 클라이언트 클래스 임포트
import com.google.android.gms.auth.api.signin.GoogleSignInOptions    // Google 로그인 옵션 클래스 임포트
import com.google.firebase.auth.FirebaseAuth                         // Firebase 인증 클래스 임포트
import com.google.firebase.firestore.FirebaseFirestore              // Firestore 데이터베이스 클래스 임포트
import com.its.nunkkam.android.UserManager.userId                    // UserManager에서 userId 임포트

class TimerActivity : AppCompatActivity() {                          // AppCompatActivity를 상속받는 TimerActivity 클래스 정의

    private lateinit var startButton: Button                         // 시작 버튼 선언
    private lateinit var resultButton: Button                        // 결과 버튼 선언
    private lateinit var logoutButton: Button                        // 로그아웃 버튼 선언
    private lateinit var deleteAccountButton: Button                 // 계정 삭제 버튼 선언
    private lateinit var timerTextView: TextView                     // 타이머 텍스트뷰 선언
    private lateinit var auth: FirebaseAuth                          // Firebase 인증 객체 선언
    private lateinit var googleSignInClient: GoogleSignInClient      // Google 로그인 클라이언트 선언
    private val db = FirebaseFirestore.getInstance()                 // Firestore 인스턴스 가져오기

    override fun onCreate(savedInstanceState: Bundle?) {             // 액티비티 생성 시 호출되는 메서드
        super.onCreate(savedInstanceState)                           // 부모 클래스의 onCreate 메서드 호출
        setContentView(R.layout.activity_timer)                      // 레이아웃 설정

        val app = application as MyApplication                       // MyApplication 객체 가져오기
        auth = app.auth                                              // Firebase 인증 객체 초기화
        googleSignInClient = app.googleSignInClient                  // Google 로그인 클라이언트 초기화

        startButton = findViewById(R.id.start_button)                // 시작 버튼 초기화
        resultButton = findViewById(R.id.result_button)              // 결과 버튼 초기화
        logoutButton = findViewById(R.id.logout_button)              // 로그아웃 버튼 초기화
        deleteAccountButton = findViewById(R.id.delete_account_button)  // 계정 삭제 버튼 초기화
        timerTextView = findViewById(R.id.timer_text)                // 타이머 텍스트뷰 초기화

        startButton.setOnClickListener {                             // 시작 버튼 클릭 리스너 설정
            val intent = Intent(this, BlinkActivity::class.java)     // BlinkActivity로 이동하는 인텐트 생성
            startActivity(intent)                                    // BlinkActivity 시작
        }

        resultButton.setOnClickListener {                            // 결과 버튼 클릭 리스너 설정
            goToResultScreen()                                       // 결과 화면으로 이동
            Log.d("TimerActivity", "userId: $userId")                // 로그 출력
        }

        logoutButton.setOnClickListener {                            // 로그아웃 버튼 클릭 리스너 설정
            logoutUser()                                             // 사용자 로그아웃
            Log.d("TimerActivity", "userId: $userId")                // 로그 출력
        }

        deleteAccountButton.setOnClickListener {                     // 계정 삭제 버튼 클릭 리스너 설정
            deleteUserAccount()                                      // 사용자 계정 삭제
        }
        checkCurrentUser()                                           // 현재 사용자 확인
    }

    private fun checkCurrentUser() {                                 // 현재 사용자 확인 메서드
        val currentUser = auth.currentUser                           // 현재 로그인된 사용자 가져오기
        Log.d("TimerActivity", "Current user in TimerActivity: $currentUser")  // 로그 출력
    }

    private fun goToResultScreen() {                                 // 결과 화면으로 이동하는 메서드
        val intent = Intent(this, ResultActivity::class.java)        // ResultActivity로 이동하는 인텐트 생성
        startActivity(intent)                                        // ResultActivity 시작
        checkCurrentUser()                                           // 현재 사용자 확인
    }

    private fun logoutUser() {                                       // 사용자 로그아웃 메서드
        auth.signOut()                                               // Firebase에서 로그아웃
        Log.d("TimerActivity", "Firebase 로그아웃 성공")

        googleSignInClient.signOut().addOnCompleteListener(this) {   // Google에서 로그아웃
            if (it.isSuccessful) {
                Log.d("TimerActivity", "Google 로그아웃 성공")
            } else {
                Log.d("TimerActivity", "Google 로그아웃 실패: ${it.exception?.message}")
            }

            UserManager.clearUserData()                              // 사용자 데이터 초기화
            Log.d("TimerActivity", "UserManager 데이터 초기화 성공")

            val intent = Intent(this, MainActivity::class.java)      // MainActivity로 이동하는 인텐트 생성
            startActivity(intent)                                    // MainActivity 시작
            finish()                                                 // 현재 액티비티 종료
            Log.d("TimerActivity", "MainActivity로 이동")
        }
    }

    private fun deleteUserAccount() {                                // 사용자 계정 삭제 메서드
        val user = auth.currentUser                                  // 현재 로그인된 사용자 가져오기

        user?.let {                                                  // 사용자가 null이 아닌 경우
            val userId = it.uid                                      // 사용자 ID 가져오기

            db.collection("USERS").document(userId).delete().addOnCompleteListener { task ->  // Firestore에서 사용자 데이터 삭제
                if (task.isSuccessful) {
                    Log.d("TimerActivity", "User data deleted from Firestore")

                    user.delete().addOnCompleteListener { deleteTask ->  // Firebase Authentication에서 사용자 계정 삭제
                        if (deleteTask.isSuccessful) {
                            Log.d("TimerActivity", "User deleted from Firebase Auth")

                            UserManager.deleteUserData(this)         // UserManager에서 사용자 데이터 삭제

                            val intent = Intent(this, MainActivity::class.java)  // MainActivity로 이동하는 인텐트 생성
                            startActivity(intent)                    // MainActivity 시작
                            finish()                                 // 현재 액티비티 종료
                        } else {
                            Log.e("TimerActivity", "Error deleting user from Firebase Auth", deleteTask.exception)
                        }
                    }
                } else {
                    Log.e("TimerActivity", "Error deleting user data from Firestore", task.exception)
                }
            }
        } ?: run {
            Log.e("TimerActivity", "User is null")                   // 사용자가 null인 경우 로그 출력
        }
    }
}