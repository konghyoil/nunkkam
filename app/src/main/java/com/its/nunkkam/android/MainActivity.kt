package com.its.nunkkam.android

import android.content.ContentValues.TAG
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
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.firestore
import java.util.UUID

class MainActivity : AppCompatActivity() {

    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var auth: FirebaseAuth
    private lateinit var guestLoginButton: Button
    private lateinit var googleLoginButton: Button
    private lateinit var logoutButton: Button


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        guestLoginButton = findViewById(R.id.guest_login_button)
        googleLoginButton = findViewById(R.id.google_login_button)
        logoutButton = findViewById(R.id.logout_button)

        val deviceModel = Build.MODEL ?: "unknown_device"
        Log.d("MainActivity", "Device model: $deviceModel") // 디버그 로그로 기기 모델명 확인


        // Firebase Auth 초기화
        auth = FirebaseAuth.getInstance()

        // GoogleSignInOptions 설정
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        googleLoginButton.setOnClickListener {
            signIn()
        }

        guestLoginButton.setOnClickListener {
            Log.d("MainActivity", "Guest Login button clicked")
            initializeGuestUser()
            navigateToMainFunction()
        }

        logoutButton.setOnClickListener {
            Log.d("MainActivity", "Logout button clicked")
            logoutUser()
        }

        checkUserStatus()
    }


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

    private fun signIn() {
        val signInIntent = googleSignInClient.signInIntent
        googleSignInLauncher.launch(signInIntent)
        Log.d("MainActivity", "Google sign-in")
    }

    private fun firebaseAuthWithGoogle(acct: GoogleSignInAccount) {
        val credential = GoogleAuthProvider.getCredential(acct.idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    saveUserToFirestore(user)
                    user?.let {
                        UserManager.setUser(this, it.uid, true)
                    }
                    navigateToMainFunction()
                } else {
                    Log.w(TAG, "signInWithCredential:failure", task.exception)
                }
            }
    }

    // 여기 수정할 것
    private fun saveUserToFirestore(user: FirebaseUser?) {
        val userRef = Firebase.firestore.collection("USERS").document(user?.uid ?: "unknown")
        val userData = mapOf(
            "uid" to (user?.uid ?: "unknown"),
            "name" to (user?.displayName ?: "Guest User"),
            "email" to (user?.email ?: ""),
            "birth_date" to null,
            "tutorial" to true
        )
        userRef.set(userData)
    }



    private fun initializeGuestUser() {
        val sharedPreferences = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        val deviceModel = Build.MODEL ?: "unknown_device"
        val userId = "$deviceModel-${UUID.randomUUID()}"
        with(sharedPreferences.edit()) {
            if (sharedPreferences.getString("user_id", null) == null) {
                putString("user_id", userId)
                Log.d("MainActivity", "User ID initialized: $userId")
            }
            putBoolean("is_google_login", false)
            apply()
        }
        UserManager.setUser(this, userId, false)
    }

    private fun navigateToMainFunction() {
        Log.d("MainActivity", "Navigating to main function")
        val intent = Intent(this, TimerActivity::class.java)
        startActivity(intent)
    }

    private fun logoutUser() {
        UserManager.clearUserData(this)
        Log.d("MainActivity", "User logged out")
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun checkUserStatus() {
        UserManager.initialize(this)
        val currentUser = auth.currentUser
        if (UserManager.userId.isNotEmpty() || currentUser != null) {
            navigateToMainFunction()
        }
    }

    companion object {
        private const val TAG = "MainActivity"
    }

}
