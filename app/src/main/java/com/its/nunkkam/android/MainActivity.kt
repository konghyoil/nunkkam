package com.its.nunkkam.android

// 필요한 라이브러리들을 import합니다.
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.its.nunkkam.android.UserManager.userId
import java.util.UUID

// MainActivity 클래스 정의
class MainActivity : AppCompatActivity() {

    // 필요한 변수들을 선언합니다.
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var auth: FirebaseAuth
    private lateinit var guestLoginButton: Button
    private lateinit var googleLoginButton: Button

    // onCreate 메서드: 액티비티가 생성될 때 호출됩니다.
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        UserManager.initialize(this) // UserManager를 초기화합니다.
        Log.d("MainActivity", "UserManager initialized with userId: $userId")

        // Firebase Auth를 초기화합니다.
        auth = Firebase.auth

        // 버튼을 찾아 변수에 할당합니다.
        guestLoginButton = findViewById(R.id.guest_login_button)
        googleLoginButton = findViewById(R.id.google_login_button)


        // Google 로그인 옵션을 설정합니다.
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        // Google 로그인 클라이언트를 초기화합니다.
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        // Google 로그인 버튼 클릭 리스너를 설정합니다.
        googleLoginButton.setOnClickListener {
            signInGoogle()
        }

        // 게스트 로그인 버튼 클릭 리스너를 설정합니다.
        guestLoginButton.setOnClickListener {
            signInAnonymously()
        }

        // 사용자 상태를 확인합니다.
        checkUserStatus()
    }

    // Google 로그인 결과를 처리하는 ActivityResultLauncher를 정의합니다.
    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)!!
            firebaseAuthWithGoogle(account)
        } catch (e: ApiException) {
            Log.w(TAG, "Google sign in failed", e)
        }
    }

    // Google 로그인 프로세스를 시작하는 함수입니다.
    private fun signInGoogle() {
        val signInIntent = googleSignInClient.signInIntent
        googleSignInLauncher.launch(signInIntent)
    }

    // Google 계정으로 Firebase 인증을 수행하는 함수입니다.
    private fun firebaseAuthWithGoogle(acct: GoogleSignInAccount) {
        val credential = GoogleAuthProvider.getCredential(acct.idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    if (user != null) {
                        UserManager.setUser(user.uid)  // userId를 설정합니다.
                        saveUserToFirestore(user)
                        navigateToMainFunction()
                    } else {
                        Log.w(TAG, "signInWithCredential: User is null")
                    }
                } else {
                    Log.w(TAG, "signInWithCredential:failure", task.exception)
                }
            }
    }

    // 사용자 정보를 Firestore에 저장하는 함수입니다.
    private fun saveUserToFirestore(user: FirebaseUser?) {
        val userId = user?.uid ?: return
        val userRef = Firebase.firestore.collection("USERS").document(userId)

        // 사용자 데이터가 이미 존재하는지 확인
        userRef.get().addOnSuccessListener { document ->
            if (!document.exists()) {
                // 사용자 데이터가 존재하지 않는 경우에만 저장
                val userData = mapOf(
                    "birth_date" to null,
                    "tutorial" to false
                )
                userRef.set(userData).addOnSuccessListener {
                    Log.d("MainActivity", "User data saved to Firestore for userId: $userId")
                }.addOnFailureListener {
                    Log.e("MainActivity", "Error saving user data to Firestore for userId: $userId", it)
                }
            } else {
                Log.d("MainActivity", "User data already exists for userId: $userId")
            }
        }.addOnFailureListener {
            Log.e("MainActivity", "Error checking user data in Firestore for userId: $userId", it)
        }
    }

    private fun signInAnonymously() {
        val sharedPreferences = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        val storedUserId = sharedPreferences.getString("user_id", null)

        if (storedUserId != null) {
            // 기존 userId가 있으면 이를 사용
            UserManager.setUser(storedUserId)
            Log.d(TAG, "Using stored userId: $storedUserId")
            navigateToMainFunction()
        } else {
            auth.signInAnonymously()
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        val user = auth.currentUser
                        val userId = user?.uid ?: "${UUID.randomUUID()}"
                        UserManager.setUser(userId)
                        Log.d(TAG, "Guest userId set: $userId")

                        with(sharedPreferences.edit()) {
                            putString("user_id", userId)
                            Log.d(TAG, "SharedPreferences userId initialized: $userId")
                            putBoolean("is_first_login", true)
                            apply()
                        }
                        navigateToMainFunction()
                    } else {
                        Log.e(TAG, "Error signing in anonymously", task.exception)
                    }
                }
        }
    }


    // 메인 기능 화면으로 이동하는 함수입니다.
    private fun navigateToMainFunction() {
        Log.d("MainActivity", "Navigating to main function")
        val intent = Intent(this, TimerActivity::class.java)
        startActivity(intent)
    }

    // 사용자 상태를 확인하는 함수입니다.
    private fun checkUserStatus() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            navigateToMainFunction()
        }
    }

    // 컴패니언 오브젝트: 클래스 레벨의 상수를 정의합니다.
    companion object {
        private const val TAG = "MainActivity"
    }
}