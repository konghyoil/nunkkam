package com.its.nunkkam.android

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.FirebaseException
import com.google.firebase.auth.ActionCodeSettings
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.auth.PlayGamesAuthProvider
import com.google.firebase.auth.ktx.actionCodeSettings
import com.google.firebase.auth.ktx.auth
import com.google.firebase.auth.ktx.userProfileChangeRequest
import com.google.firebase.ktx.Firebase
//import com.google.firebase.quickstart.auth.R
import java.util.concurrent.TimeUnit

abstract class LoginMainActivity : AppCompatActivity() {

    private val TAG = "LoginMainActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    private fun checkCurrentUser() {
        val user = Firebase.auth.currentUser
        if (user != null) {
            // User is signed in
        } else {
            // No user is signed in
        }
    }

    private fun getUserProfile() {
        val user = Firebase.auth.currentUser
        user?.let {
            val name = it.displayName
            val email = it.email
            val photoUrl = it.photoUrl
            val emailVerified = it.isEmailVerified
            val uid = it.uid
        }
    }

    private fun getProviderData() {
        val user = Firebase.auth.currentUser
        user?.let {
            for (profile in it.providerData) {
                val providerId = profile.providerId
                val uid = profile.uid
                val name = profile.displayName
                val email = profile.email
                val photoUrl = profile.photoUrl
            }
        }
    }

    private fun updateProfile() {
        val user = Firebase.auth.currentUser

        val profileUpdates = userProfileChangeRequest {
            displayName = "Jane Q. User"
            photoUri = Uri.parse("https://example.com/jane-q-user/profile.jpg")
        }

        user!!.updateProfile(profileUpdates)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "User profile updated.")
                }
            }
    }

    private fun updateEmail() {
        val user = Firebase.auth.currentUser

        user!!.updateEmail("user@example.com")
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "User email address updated.")
                }
            }
    }

    private fun updatePassword() {
        val user = Firebase.auth.currentUser
        val newPassword = "SOME-SECURE-PASSWORD"

        user!!.updatePassword(newPassword)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "User password updated.")
                }
            }
    }

    private fun sendEmailVerification() {
        val user = Firebase.auth.currentUser

        user!!.sendEmailVerification()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "Email sent.")
                }
            }
    }

    private fun sendEmailVerificationWithContinueUrl() {
        val auth = Firebase.auth
        val user = auth.currentUser!!

        val url = "http://www.example.com/verify?uid=" + user.uid
        val actionCodeSettings = ActionCodeSettings.newBuilder()
            .setUrl(url)
            .setIOSBundleId("com.example.ios")
            .setAndroidPackageName("com.example.android", false, null)
            .build()

        user.sendEmailVerification(actionCodeSettings)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "Email sent.")
                }
            }

        auth.setLanguageCode("fr")
    }

    private fun sendPasswordReset() {
        val emailAddress = "user@example.com"

        Firebase.auth.sendPasswordResetEmail(emailAddress)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "Email sent.")
                }
            }
    }

    private fun deleteUser() {
        val user = Firebase.auth.currentUser!!

        user.delete()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "User account deleted.")
                }
            }
    }

    private fun reauthenticate() {
        val user = Firebase.auth.currentUser!!

        val credential = EmailAuthProvider
            .getCredential("user@example.com", "password1234")

        user.reauthenticate(credential)
            .addOnCompleteListener { Log.d(TAG, "User re-authenticated.") }
    }

    private fun linkAndMerge(credential: AuthCredential) {
        val auth = Firebase.auth

        val prevUser = auth.currentUser
        auth.signInWithCredential(credential)
            .addOnSuccessListener { result ->
                val currentUser = result.user
                // Merge prevUser and currentUser accounts and data
            }
            .addOnFailureListener {
                // ...
            }
    }

    private fun unlink(providerId: String) {
        Firebase.auth.currentUser!!.unlink(providerId)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Auth provider unlinked from account
                }
            }
    }

    private fun buildActionCodeSettings() {
        val actionCodeSettings = actionCodeSettings {
            url = "https://www.example.com/finishSignUp?cartId=1234"
            handleCodeInApp = true
            setIOSBundleId("com.example.ios")
            setAndroidPackageName("com.example.android", true, "12")
        }
    }

    private fun sendSignInLink(email: String, actionCodeSettings: ActionCodeSettings) {
        Firebase.auth.sendSignInLinkToEmail(email, actionCodeSettings)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "Email sent.")
                }
            }
    }

    private fun verifySignInLink() {
        val auth = Firebase.auth
        val intent = intent
        val emailLink = intent.data.toString()

        if (auth.isSignInWithEmailLink(emailLink)) {
            val email = "someemail@domain.com"

            auth.signInWithEmailLink(email, emailLink)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Log.d(TAG, "Successfully signed in with email link!")
                        val result = task.result
                    } else {
                        Log.e(TAG, "Error signing in with email link", task.exception)
                    }
                }
        }
    }

    private fun linkWithSignInLink(email: String, emailLink: String) {
        val credential = EmailAuthProvider.getCredentialWithLink(email, emailLink)

        Firebase.auth.currentUser!!.linkWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "Successfully linked emailLink credential!")
                } else {
                    Log.e(TAG, "Error linking emailLink credential", task.exception)
                }
            }
    }

    private fun reauthWithLink(email: String, emailLink: String) {
        val credential = EmailAuthProvider.getCredentialWithLink(email, emailLink)

        Firebase.auth.currentUser!!.reauthenticateAndRetrieveData(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                } else {
                    Log.e(TAG, "Error reauthenticating", task.exception)
                }
            }
    }

    private fun differentiateLink(email: String) {
        Firebase.auth.fetchSignInMethodsForEmail(email)
            .addOnSuccessListener { result ->
                val signInMethods = result.signInMethods!!
                if (signInMethods.contains(EmailAuthProvider.EMAIL_PASSWORD_SIGN_IN_METHOD)) {
                } else if (signInMethods.contains(EmailAuthProvider.EMAIL_LINK_SIGN_IN_METHOD)) {
                }
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Error getting sign in methods for user", exception)
            }
    }

    private fun getGoogleCredentials() {
        val googleIdToken = ""
        val credential = GoogleAuthProvider.getCredential(googleIdToken, null)
    }

    private fun getEmailCredentials() {
        val email = ""
        val password = ""
        val credential = EmailAuthProvider.getCredential(email, password)
    }

    private fun signOut() {
        Firebase.auth.signOut()
    }

    private fun testPhoneVerify() {
        val phoneNum = "+16505554567"
        val testVerificationCode = "123456"

        val options = PhoneAuthOptions.newBuilder(Firebase.auth)
            .setPhoneNumber(phoneNum)
            .setTimeout(30L, TimeUnit.SECONDS)
            .setActivity(this)
            .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

                override fun onCodeSent(
                    verificationId: String,
                    forceResendingToken: PhoneAuthProvider.ForceResendingToken,
                ) {
                    this@LoginMainActivity.enableUserManuallyInputCode()
                }

                override fun onVerificationCompleted(phoneAuthCredential: PhoneAuthCredential) {
                }

                override fun onVerificationFailed(e: FirebaseException) {
                }
            })
            .build()
        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    private fun enableUserManuallyInputCode() {
        // No-op
    }

    private fun testPhoneAutoRetrieve() {
        val phoneNumber = "+16505554567"
        val smsCode = "123456"

        val firebaseAuth = Firebase.auth
        val firebaseAuthSettings = firebaseAuth.firebaseAuthSettings

        firebaseAuthSettings.setAutoRetrievedSmsCodeForPhoneNumber(phoneNumber, smsCode)

        val options = PhoneAuthOptions.newBuilder(firebaseAuth)
            .setPhoneNumber(phoneNumber)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(this)
            .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                }

                override fun onVerificationFailed(e: FirebaseException) {
                }
            })
            .build()
        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    private fun gamesMakeGoogleSignInOptions() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_GAMES_SIGN_IN)
            .requestServerAuthCode(getString(R.string.default_web_client_id))
            .build()
    }

    private fun firebaseAuthWithPlayGames(acct: GoogleSignInAccount) {
        Log.d(TAG, "firebaseAuthWithPlayGames:" + acct.id!!)

        val auth = Firebase.auth
        val credential = PlayGamesAuthProvider.getCredential(acct.serverAuthCode!!)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "signInWithCredential:success")
                    val user = auth.currentUser
                    updateUI(user)
                } else {
                    Log.w(TAG, "signInWithCredential:failure", task.exception)
                    Toast.makeText(
                        baseContext,
                        "Authentication failed.",
                        Toast.LENGTH_SHORT,
                    ).show()
                    updateUI(null)
                }
            }
    }

    private fun gamesGetUserInfo() {
        val auth = Firebase.auth

        val user = auth.currentUser
        user?.let {
            val playerName = it.displayName
            val uid = it.uid
        }
    }

    private fun updateUI(user: FirebaseUser?) {
        // No-op
    }
}
