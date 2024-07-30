package com.its.nunkkam.android

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
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.its.nunkkam.android.UserManager.userId
import java.util.UUID

class MainActivity : AppCompatActivity() {

    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var auth: FirebaseAuth
    private lateinit var guestLoginButton: Button
    private lateinit var googleLoginButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        UserManager.initialize(this) // UserManager 초기화

        guestLoginButton = findViewById(R.id.guest_login_button)
        googleLoginButton = findViewById(R.id.google_login_button)

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
            Log.d("MainActivity", "guest userId: $userId")
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
    }

    private fun firebaseAuthWithGoogle(acct: GoogleSignInAccount) {
        val credential = GoogleAuthProvider.getCredential(acct.idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    if (user != null) {
                        UserManager.setUser(user.uid)  // userId 설정
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
        UserManager.setUser(userId)  // userId 설정
        with(sharedPreferences.edit()) {
            if (sharedPreferences.getString("user_id", null) == null) {
                putString("user_id", userId)
                Log.d("MainActivity", "User ID initialized: $userId")
            }
            putString("user_name", "Guest User")
            putBoolean("is_first_login", true)
            apply()
        }
        navigateToMainFunction()
    }

    private fun navigateToMainFunction() {
        Log.d("MainActivity", "Navigating to main function")
        val intent = Intent(this, TimerActivity::class.java)
        startActivity(intent)
    }

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
