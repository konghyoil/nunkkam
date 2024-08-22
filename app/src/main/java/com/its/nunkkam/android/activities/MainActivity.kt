package com.its.nunkkam.android.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ImageButton
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.its.nunkkam.android.MyApplication
import com.its.nunkkam.android.R
import com.its.nunkkam.android.managers.UserManager

class MainActivity : AppCompatActivity() {

    // Firebase Authentication 객체
    private lateinit var auth: FirebaseAuth
    // Google 로그인 버튼
    private lateinit var googleLoginButton: ImageButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // MyApplication에서 Firebase Authentication 객체 가져오기
        val app = application as MyApplication
        auth = app.auth

        // UserManager 초기화
        UserManager.initialize(this)

        // Google 로그인 버튼 설정
        googleLoginButton = findViewById(R.id.google_login_button)
        googleLoginButton.setOnClickListener {
            signInGoogle()
        }

        // 사용자 상태 확인
        checkUserStatus()
    }

    // 액티비티가 시작될 때 호출되는 메서드
    public override fun onStart() {
        super.onStart()
        val currentUser = auth.currentUser
        updateUI(currentUser)
    }

    // UI 업데이트 메서드
    private fun updateUI(currentUser: FirebaseUser?) {
        if (currentUser != null) {
            UserManager.setUser(currentUser.uid, UserManager.LOGIN_TYPE_GOOGLE)
            // Firestore에서 튜토리얼 수행 여부 확인
            checkTutorialStatus(currentUser)
        }
    }

    // Google 로그인 결과를 처리하는 ActivityResultLauncher
    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)!!
            firebaseAuthWithGoogle(account)
        } catch (e: ApiException) {
            // 로그인 실패 시 에러 로그 기록
            Log.e(TAG, "Google sign in failed", e)
        }
    }

    // Firestore에서 튜토리얼 상태 확인
    private fun checkTutorialStatus(user: FirebaseUser) {
        val userRef = Firebase.firestore.collection("USERS").document(user.uid)

        userRef.get().addOnSuccessListener { document ->
            val tutorialCompleted = document.getBoolean("tutorial") ?: false
            if (tutorialCompleted) {
                navigateToMainFunction()
            } else {
                // TutorialActivity로 이동
                startActivity(Intent(this, TutorialActivity::class.java))
                finish()
            }
        }.addOnFailureListener {
            // Firestore에서 사용자 데이터 확인 실패
            Log.e(TAG, "Failed to fetch tutorial status", it)
        }
    }

    // Google 로그인 시작
    private fun signInGoogle() {
        val signInIntent = (application as MyApplication).googleSignInClient.signInIntent
        googleSignInLauncher.launch(signInIntent)
    }

    // Google 계정으로 Firebase 인증
    private fun firebaseAuthWithGoogle(acct: GoogleSignInAccount) {
        val credential = GoogleAuthProvider.getCredential(acct.idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    if (user != null) {
                        UserManager.setUser(user.uid, UserManager.LOGIN_TYPE_GOOGLE)
                        saveUserToFirestore(user)

                        // 로그인 성공 시 로그 기록 (사용자 아이디는 기록하지 않음)
                        Log.i(TAG, "User successfully signed in via Google")

                        checkTutorialStatus(user)
                    }
                } else {
                    // 로그인 실패 시 에러 로그 기록
                    Log.e(TAG, "signInWithCredential:failure", task.exception)
                }
            }
    }

    // Firestore에 사용자 정보 저장
    private fun saveUserToFirestore(user: FirebaseUser?) {
        val userId = user?.uid ?: return
        val userRef = Firebase.firestore.collection("USERS").document(userId)

        userRef.get().addOnSuccessListener { document ->
            if (!document.exists()) {
                val userData = hashMapOf(
                    "login_type" to UserManager.LOGIN_TYPE_GOOGLE,
                    "birth_date" to null,
                    "tutorial" to false
                )
                userRef.set(userData).addOnSuccessListener {
                    // 사용자 데이터 Firestore에 성공적으로 저장
                    Log.d(TAG, "User data saved to Firestore")
                }.addOnFailureListener {
                    // 사용자 데이터 Firestore에 저장 실패
                    Log.e(TAG, "Error saving user data to Firestore", it)
                }
            } else {
                userRef.update("login_type", UserManager.LOGIN_TYPE_GOOGLE).addOnSuccessListener {
                    // 로그인 타입 업데이트 성공
                    Log.d(TAG, "User login type updated in Firestore")
                }.addOnFailureListener {
                    // 로그인 타입 업데이트 실패
                    Log.e(TAG, "Error updating user login type in Firestore", it)
                }
            }
        }.addOnFailureListener {
            // Firestore에서 사용자 데이터 확인 실패
            Log.e(TAG, "Error checking user data in Firestore", it)
        }
    }

    // 메인 기능 화면(TimerActivity)으로 이동
    private fun navigateToMainFunction() {
        val intent = Intent(this, TimerActivity::class.java)
        startActivity(intent)
        finish()
    }

    // 사용자 로그인 상태 확인
    private fun checkUserStatus() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            navigateToMainFunction()
        }
    }

    companion object {
        private const val TAG = "MainActivity"
    }
}