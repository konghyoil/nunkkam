package com.its.nunkkam.android
//
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
// import com.its.nunkkam.android.R
//
class GoogleSignInActivity : Activity() {
}
//    private lateinit var auth: FirebaseAuth
//    private lateinit var googleSignInClient: GoogleSignInClient
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_google_sign_in) // 변경: activity_google_sign_in 레이아웃 파일 사용
//
//        // Configure Google Sign In
//        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
//            .requestIdToken(getString(R.string.default_web_client_id))
//            .requestEmail()
//            .build()
//
//        googleSignInClient = GoogleSignIn.getClient(this, gso)
//
//        // Initialize Firebase Auth
//        auth = Firebase.auth
//
//        // Add sign in button click listener
//        findViewById<SignInButton>(R.id.sign_in_button).setOnClickListener {
//            signIn()
//        }
//    }
//
//    override fun onStart() {
//        super.onStart()
//        val currentUser = auth.currentUser
//        updateUI(currentUser)
//    }
//
//    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
//        super.onActivityResult(requestCode, resultCode, data)
//
//        if (requestCode == RC_SIGN_IN) {
//            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
//            try {
//                val account = task.getResult(ApiException::class.java)!!
//                Log.d(TAG, "firebaseAuthWithGoogle:" + account.id)
//                firebaseAuthWithGoogle(account.idToken!!)
//            } catch (e: ApiException) {
//                Log.w(TAG, "Google sign in failed", e)
//                updateUI(null)
//            }
//        }
//    }
//
//    private fun firebaseAuthWithGoogle(idToken: String) {
//        val credential = GoogleAuthProvider.getCredential(idToken, null)
//        auth.signInWithCredential(credential)
//            .addOnCompleteListener(this) { task ->
//                if (task.isSuccessful) {
//                    Log.d(TAG, "signInWithCredential:success")
//                    val user = auth.currentUser
//                    updateUI(user)
//                } else {
//                    Log.w(TAG, "signInWithCredential:failure", task.exception)
//                    Toast.makeText(baseContext, "Authentication Failed.", Toast.LENGTH_SHORT).show()
//                    updateUI(null)
//                }
//            }
//    }
//
//    private fun signIn() {
//        val signInIntent = googleSignInClient.signInIntent
//        startActivityForResult(signInIntent, RC_SIGN_IN)
//    }
//
//    private fun signOut() {
//        auth.signOut()
//        googleSignInClient.signOut().addOnCompleteListener(this) {
//            updateUI(null)
//        }
//    }
//
//    private fun updateUI(user: FirebaseUser?) {
//        if (user != null) {
//            // Navigate to MainActivity or update UI to show signed-in state
//            val intent = Intent(this, MainActivity::class.java)
//            startActivity(intent)
//            finish()
//        } else {
//            // Show signed-out UI state or navigate to SignInActivity
//        }
//    }
//
//    companion object {
//        private const val TAG = "GoogleSignInActivity"
//        private const val RC_SIGN_IN = 9001
//    }
//}
