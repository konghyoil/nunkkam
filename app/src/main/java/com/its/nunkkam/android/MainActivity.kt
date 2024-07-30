package com.its.nunkkam.android

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.services.people.v1.People
import com.google.api.services.people.v1.PeopleScopes
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var auth: FirebaseAuth
    private lateinit var guestLoginButton: Button
    private lateinit var googleLoginButton: Button
    private lateinit var credential: GoogleAccountCredential

    private val REQUEST_CODE_READ_PHONE_STATE = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        guestLoginButton = findViewById(R.id.guest_login_button)
        googleLoginButton = findViewById(R.id.google_login_button)

        // Firebase Auth 초기화
        auth = FirebaseAuth.getInstance()

        // GoogleSignInOptions 설정
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .requestScopes(Scope(PeopleScopes.CONTACTS_READONLY))
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        googleLoginButton.setOnClickListener {
            signIn()
        }

        guestLoginButton.setOnClickListener {
            Log.d("MainActivity", "Guest Login button clicked")
            checkAndRequestPermissions()
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
                    getBirthDateFromGoogleAccount(user) { birthDate ->
                        saveUserToFirestore(user, birthDate)
                        navigateToMainFunction()
                    }
                } else {
                    Log.w(TAG, "signInWithCredential:failure", task.exception)
                }
            }
    }

    private fun getBirthDateFromGoogleAccount(user: FirebaseUser?, callback: (String?) -> Unit) {
        if (user == null) {
            callback(null)
            return
        }

        credential = GoogleAccountCredential.usingOAuth2(this, listOf(PeopleScopes.CONTACTS_READONLY))
        credential.selectedAccount = user.email?.let { account -> account }

        val peopleService = People.Builder(GoogleNetHttpTransport.newTrustedTransport(), JacksonFactory.getDefaultInstance(), credential)
            .setApplicationName("YourAppName")
            .build()

        val request = peopleService.people().get("people/me").setPersonFields("birthdays")
        Thread {
            try {
                val response = request.execute()
                val birthDate = response.birthdays?.firstOrNull()?.date
                val birthDateString = birthDate?.let { "${it.year}-${it.month}-${it.day}" }
                callback(birthDateString)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to get birth date", e)
                callback(null)
            }
        }.start()
    }

    private fun saveUserToFirestore(user: FirebaseUser?, birthDate: String?) {
        val userRef = Firebase.firestore.collection("USERS").document(user?.uid ?: "unknown")
        val userData = mapOf(
            "uid" to (user?.uid ?: "unknown"),
            "name" to (user?.displayName ?: "Guest User"),
            "email" to (user?.email ?: ""),
            "birth_date" to birthDate,
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
            putString("user_name", "Guest User")
            putBoolean("is_first_login", true)
            apply()
        }
        navigateToMainFunction()
    }

    private fun checkAndRequestPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.READ_PHONE_STATE),
                REQUEST_CODE_READ_PHONE_STATE
            )
        } else {
            getDeviceSerialNumber()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_READ_PHONE_STATE) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                getDeviceSerialNumber()
            } else {
                // 권한이 거부된 경우 처리
            }
        }
    }

    @SuppressLint("HardwareIds")
    private fun getDeviceSerialNumber() {
        val serialNumber: String = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Build.getSerial()
        } else {
            Build.SERIAL
        }
        Log.d("SerialNumber", "Device Serial Number: $serialNumber")
        initializeGuestUser()
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
