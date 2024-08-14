package com.its.nunkkam.android

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var googleLoginButton: ImageButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val app = application as MyApplication
        auth = app.auth

        UserManager.initialize(this)

        googleLoginButton = findViewById(R.id.google_login_button)

        googleLoginButton.setOnClickListener {
            signInGoogle()
        }

        checkAndRequestPermissions()

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

    public override fun onStart() {
        super.onStart()
        val currentUser = auth.currentUser
        updateUI(currentUser)
    }

    private fun updateUI(currentUser: FirebaseUser?) {
        if (currentUser != null) {
            UserManager.setUser(currentUser.uid, UserManager.LOGIN_TYPE_GOOGLE)
            navigateToMainFunction()
        }
    }

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

    private fun signInGoogle() {
        val signInIntent = (application as MyApplication).googleSignInClient.signInIntent
        googleSignInLauncher.launch(signInIntent)
    }

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

                        navigateToMainFunction()
                    }
                } else {
                    // 로그인 실패 시 에러 로그 기록
                    Log.e(TAG, "signInWithCredential:failure", task.exception)
                }
            }
    }

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
                }.addOnFailureListener {
                    // 사용자 데이터 Firestore에 저장 실패
                }
            } else {
                userRef.update("login_type", UserManager.LOGIN_TYPE_GOOGLE).addOnSuccessListener {
                    // 로그인 타입 업데이트 성공
                }.addOnFailureListener {
                    // 로그인 타입 업데이트 실패
                }
            }
        }.addOnFailureListener {
            // Firestore에서 사용자 데이터 확인 실패
        }
    }

    private fun navigateToMainFunction() {
        val intent = Intent(this, TimerActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun checkUserStatus() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            navigateToMainFunction()
        }
    }

    companion object {
        private const val TAG = "MainActivity"
        private const val REQUEST_CODE_PERMISSIONS = 10
    }
}
