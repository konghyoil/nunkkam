package com.its.nunkkam.android

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
//import com.google.api.client.googleapis.auth.oauth2.GoogleCredential
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.services.people.v1.People
import com.google.api.services.people.v1.PeopleServiceScopes
import com.google.api.services.people.v1.model.Person
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.util.*

class LoginMainActivity : Activity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient
    private val db = Firebase.firestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_loginmain)

        val signInButton: Button = findViewById(R.id.btn_google_sign_in)

        // Configure Google Sign In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .requestScopes(GoogleSignInOptions.scope(PeopleServiceScopes.USER_BIRTHDAY_READ))
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        // Initialize Firebase Auth
        auth = Firebase.auth

        signInButton.setOnClickListener {
            signIn()
        }
    }

    override fun onStart() {
        super.onStart()
        val currentUser = auth.currentUser
        updateUI(currentUser)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)!!
                Log.d(TAG, "firebaseAuthWithGoogle:" + account.id)
                firebaseAuthWithGoogle(account.idToken!!, account)
            } catch (e: ApiException) {
                Log.w(TAG, "Google sign in failed", e)
                updateUI(null)
            }
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String, account: GoogleSignInAccount) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "signInWithCredential:success")
                    val user = auth.currentUser
                    user?.let {
                        getAndSaveUserBirthday(account, it)
                    }
                    updateUI(user)
                } else {
                    Log.w(TAG, "signInWithCredential:failure", task.exception)
                    Toast.makeText(baseContext, "Authentication Failed.", Toast.LENGTH_SHORT).show()
                    updateUI(null)
                }
            }
    }

    private fun getAndSaveUserBirthday(account: GoogleSignInAccount, user: FirebaseUser) {
        val credential = GoogleAccountCredential.usingOAuth2(this, Collections.singleton(PeopleServiceScopes.USER_BIRTHDAY_READ))
        credential.selectedAccount = account.account
        val peopleService = People.Builder(
            com.google.api.client.http.javanet.NetHttpTransport(),
            com.google.api.client.json.gson.GsonFactory(),
            credential
        )
            .setApplicationName("Your Application Name")
            .build()

        Thread {
            try {
                val person: Person = peopleService.people().get("people/me").setPersonFields("birthdays").execute()
                val birthday = person.birthdays?.get(0)?.date
                val birthdate = "${birthday?.year ?: ""}-${birthday?.month ?: ""}-${birthday?.day ?: ""}"
                saveUserToFirestore(user, birthdate)
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching birthday", e)
            }
        }.start()
    }

    private fun saveUserToFirestore(user: FirebaseUser, birthdate: String) {
        val userId = user.uid
        val userDoc = hashMapOf(
            "userId" to userId,
            "email" to user.email,
            "displayName" to user.displayName,
            "birthdate" to birthdate
        )

        db.collection("users").document(userId).set(userDoc)
            .addOnSuccessListener {
                Log.d(TAG, "User document successfully written!")
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "Error writing document", e)
            }
    }

    private fun signIn() {
        val signInIntent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }

    private fun signOut() {
        auth.signOut()
        googleSignInClient.signOut().addOnCompleteListener(this) {
            updateUI(null)
        }
    }

    private fun updateUI(user: FirebaseUser?) {
        if (user != null) {
            // Navigate to MainActivity or update UI to show signed-in state
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        } else {
            // Show signed-out UI state or navigate to SignInActivity
        }
    }

    companion object {
        private const val TAG = "LoginMainActivity"
        private const val RC_SIGN_IN = 9001
    }
}
