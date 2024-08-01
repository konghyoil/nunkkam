package com.its.nunkkam.android

import android.content.Context
import android.content.Intent
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

class MainActivity : AppCompatActivity() {

    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var auth: FirebaseAuth
    private lateinit var guestLoginButton: Button
    private lateinit var googleLoginButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val app = application as MyApplication
        auth = app.auth
        googleSignInClient = app.googleSignInClient

        UserManager.initialize(this)
        Log.d("MainActivity", "UserManager initialized with userId: $userId")

        guestLoginButton = findViewById(R.id.guest_login_button)
        googleLoginButton = findViewById(R.id.google_login_button)

        googleLoginButton.setOnClickListener {
            signInGoogle()
        }

        guestLoginButton.setOnClickListener {
            signInAnonymously()
        }

        checkUserStatus()
    }

    public override fun onStart() {
        super.onStart()
        val currentUser = auth.currentUser
        Log.d(TAG, "onStart currentUser: $currentUser")
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
            Log.w(TAG, "Google sign in failed", e)
        }
    }

    private fun signInGoogle() {
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
                        UserManager.setUser(user.uid, UserManager.LOGIN_TYPE_GOOGLE)
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

    private fun signInAnonymously() {
        val sharedPreferences = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        val storedUserId = sharedPreferences.getString("user_id", null)

        if (storedUserId != null) {
            auth.signInAnonymously().addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    UserManager.setUser(storedUserId, UserManager.LOGIN_TYPE_ANONYMOUS)
                    saveUserToFirestore(auth.currentUser)
                    navigateToMainFunction()
                } else {
                    Log.e(TAG, "Error signing in anonymously", task.exception)
                }
            }
        } else {
            auth.signInAnonymously()
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        val user = auth.currentUser
                        val userId = user?.uid ?: "${UUID.randomUUID()}"
                        UserManager.setUser(userId, UserManager.LOGIN_TYPE_ANONYMOUS)

                        with(sharedPreferences.edit()) {
                            putString("user_id", userId)
                            apply()
                        }
                        saveUserToFirestore(user)
                        navigateToMainFunction()
                    } else {
                        Log.e(TAG, "Error signing in anonymously", task.exception)
                    }
                }
        }
    }

    private fun saveUserToFirestore(user: FirebaseUser?) {
        val userId = user?.uid ?: return
        val userRef = Firebase.firestore.collection("USERS").document(userId)

        userRef.get().addOnSuccessListener { document ->
            if (!document.exists()) {
                val userData = hashMapOf(
                    "login_type" to UserManager.loginType,
                    "birth_date" to null,
                    "tutorial" to false
                )
                userRef.set(userData).addOnSuccessListener {
                    Log.d("MainActivity", "User data saved to Firestore")
                }.addOnFailureListener {
                    Log.e("MainActivity", "Error saving user data to Firestore", it)
                }
            } else {
                userRef.update("login_type", UserManager.loginType)
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
