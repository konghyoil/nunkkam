package com.its.nunkkam.android                                      // 패키지 선언

import android.Manifest
import android.content.Intent                                        // 인텐트 클래스 임포트
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle                                             // Bundle 클래스 임포트
import android.util.Log                                              // 로깅을 위한 Log 클래스 임포트
import android.view.ViewGroup
import android.widget.Button                                         // Button 위젯 임포트
import android.widget.ImageButton
import androidx.activity.result.contract.ActivityResultContracts     // 액티비티 결과 계약 임포트
import androidx.appcompat.app.AppCompatActivity                      // AppCompatActivity 임포트
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.auth.api.signin.GoogleSignIn           // Google 로그인 관련 클래스 임포트
import com.google.android.gms.auth.api.signin.GoogleSignInAccount    // Google 계정 정보 클래스 임포트
import com.google.android.gms.common.api.ApiException                // API 예외 처리 클래스 임포트
import com.google.firebase.auth.FirebaseAuth                         // Firebase 인증 클래스 임포트
import com.google.firebase.auth.GoogleAuthProvider                   // Google 인증 제공자 클래스 임포트
import com.google.firebase.auth.FirebaseUser                         // Firebase 사용자 클래스 임포트
import com.google.firebase.firestore.ktx.firestore                   // Firestore 데이터베이스 임포트
import com.google.firebase.ktx.Firebase                              // Firebase 코어 기능 임포트

class MainActivity : AppCompatActivity() {                           // MainActivity 클래스 정의, AppCompatActivity 상속

    private lateinit var auth: FirebaseAuth                          // Firebase 인증 객체 선언
    private lateinit var googleLoginButton: ImageButton                   // Google 로그인 버튼 객체 선언

    override fun onCreate(savedInstanceState: Bundle?) {             // 액티비티 생성 시 호출되는 메서드
        super.onCreate(savedInstanceState)                           // 부모 클래스의 onCreate 메서드 호출
        setContentView(R.layout.activity_main)                       // 액티비티 레이아웃 설정

        val app = application as MyApplication                       // 애플리케이션 객체 가져오기
        auth = app.auth                                              // Firebase 인증 객체 초기화

        UserManager.initialize(this)                                 // UserManager 초기화
        Log.d("MainActivity", "UserManager initialized with userId: ${UserManager.userId}")

        googleLoginButton = findViewById(R.id.google_login_button)   // Google 로그인 버튼 뷰 찾기

        googleLoginButton.setOnClickListener {                       // 버튼 클릭 리스너 설정
            signInGoogle()                                           // Google 로그인 메서드 호출
        }

        checkAndRequestPermissions()                                 // 권한 확인 및 요청

        checkUserStatus() // 사용자 상태 확인 메서드 호출
        
    }

    private fun checkAndRequestPermissions() {
        val permissionsToRequest = mutableListOf<String>()

        // 카메라 권한 확인
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.CAMERA)
        }

        // 포그라운드 서비스 권한 확인
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.FOREGROUND_SERVICE) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.FOREGROUND_SERVICE)
        }

        // 알림 권한 확인 (안드로이드 13 이상에서 필요)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        // 필요한 권한 요청
        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsToRequest.toTypedArray(), REQUEST_CODE_PERMISSIONS)
        }
    }

    public override fun onStart() {                                  // 액티비티가 시작될 때 호출되는 메서드
        super.onStart()                                              // 부모 클래스의 onStart 메서드 호출
        val currentUser = auth.currentUser                           // 현재 로그인된 사용자 가져오기
        Log.d(TAG, "onStart currentUser: $currentUser")
        updateUI(currentUser)                                        // UI 업데이트 메서드 호출
    }

    private fun updateUI(currentUser: FirebaseUser?) {               // UI 업데이트 메서드
        if (currentUser != null) {                                   // 현재 사용자가 있는 경우
            UserManager.setUser(currentUser.uid, UserManager.LOGIN_TYPE_GOOGLE)  // UserManager에 사용자 정보 설정
            navigateToMainFunction()                                 // 메인 기능 화면으로 이동
        }
    }

    private val googleSignInLauncher = registerForActivityResult(    // Google 로그인 결과를 처리하기 위한 런처
        ActivityResultContracts.StartActivityForResult()             // 액티비티 결과 계약 사용
    ) { result ->                                                    // 결과 처리 람다 함수
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)  // Google 로그인 인텐트에서 계정 정보 가져오기
        try {
            val account = task.getResult(ApiException::class.java)!! // Google 계정 정보 가져오기
            firebaseAuthWithGoogle(account)                          // Firebase로 Google 인증 수행
        } catch (e: ApiException) {                                  // API 예외 발생 시
            Log.w(TAG, "Google sign in failed", e)
        }
    }

    private fun signInGoogle() {                                     // Google 로그인 시작 메서드
        val signInIntent = (application as MyApplication).googleSignInClient.signInIntent  // Google 로그인 인텐트 생성
        googleSignInLauncher.launch(signInIntent)                    // 로그인 인텐트 실행
    }

    private fun firebaseAuthWithGoogle(acct: GoogleSignInAccount) {  // Google 계정으로 Firebase 인증
        val credential = GoogleAuthProvider.getCredential(acct.idToken, null)  // Google 인증 정보 생성
        auth.signInWithCredential(credential)                        // Firebase에 인증 정보로 로그인
            .addOnCompleteListener(this) { task ->                   // 로그인 완료 리스너
                if (task.isSuccessful) {                             // 로그인 성공 시
                    val user = auth.currentUser                      // 현재 사용자 정보 가져오기
                    if (user != null) {                              // 사용자 정보가 있는 경우
                        UserManager.setUser(user.uid, UserManager.LOGIN_TYPE_GOOGLE)  // UserManager에 사용자 정보 설정
                        saveUserToFirestore(user)                    // Firestore에 사용자 정보 저장
                        navigateToMainFunction()                     // 메인 기능 화면으로 이동
                    } else {
                        Log.w(TAG, "signInWithCredential: User is null")
                    }
                } else {                                             // 로그인 실패 시
                    Log.w(TAG, "signInWithCredential:failure", task.exception)
                }
            }
    }

    private fun saveUserToFirestore(user: FirebaseUser?) {           // Firestore에 사용자 정보 저장 메서드
        val userId = user?.uid ?: return                             // 사용자 ID 가져오기, null이면 함수 종료
        val userRef = Firebase.firestore.collection("USERS").document(userId)  // Firestore 문서 참조 생성

        userRef.get().addOnSuccessListener { document ->             // Firestore에서 사용자 문서 가져오기
            if (!document.exists()) {                                // 문서가 존재하지 않는 경우 (새 사용자)
                val userData = hashMapOf(                            // 새 사용자 데이터 생성
                    "login_type" to UserManager.LOGIN_TYPE_GOOGLE,
                    "birth_date" to null,
                    "tutorial" to false
                )
                userRef.set(userData).addOnSuccessListener {         // Firestore에 새 사용자 데이터 저장
                    Log.d("MainActivity", "User data saved to Firestore")
                }.addOnFailureListener {
                    Log.e("MainActivity", "Error saving user data to Firestore", it)
                }
            } else {                                                 // 문서가 이미 존재하는 경우 (기존 사용자)
                userRef.update("login_type", UserManager.LOGIN_TYPE_GOOGLE)  // 로그인 타입만 업데이트
                    .addOnSuccessListener {
                        Log.d("MainActivity", "User login type updated in Firestore")
                    }.addOnFailureListener {
                        Log.e("MainActivity", "Error updating user login type in Firestore", it)
                    }
            }
        }.addOnFailureListener {
            Log.e("MainActivity", "Error checking user data in Firestore", it)
        }
    }

    private fun navigateToMainFunction() {                           // 메인 기능 화면으로 이동하는 메서드
        Log.d("MainActivity", "Navigating to main function")
        val intent = Intent(this, TimerActivity::class.java)         // TimerActivity로 이동하는 인텐트 생성
        startActivity(intent)                                        // TimerActivity 시작
        finish()                                                     // 현재 액티비티 종료
    }

    private fun checkUserStatus() {                                  // 사용자 상태 확인 메서드
        val currentUser = auth.currentUser                           // 현재 로그인된 사용자 가져오기
        if (currentUser != null) {                                   // 사용자가 이미 로그인된 경우
            navigateToMainFunction()                                 // 메인 기능 화면으로 바로 이동
        }
    }

    companion object {                                               // 동반 객체
        private const val TAG = "MainActivity"                       // 로깅을 위한 태그
        private const val REQUEST_CODE_PERMISSIONS = 10 // 권한 요청 코드: onRequestPermissionsResult에서 이 요청을 식별하는 데 사용

    }
}