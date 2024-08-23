package com.its.nunkkam.android.activities

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.Menu
import android.view.View
import android.webkit.WebView
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.bumptech.glide.request.RequestOptions
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.its.nunkkam.android.MyApplication
import com.its.nunkkam.android.R
import com.its.nunkkam.android.fragments.AlarmFragment
import com.its.nunkkam.android.managers.UserManager

class TimerActivity : AppCompatActivity() {

    private lateinit var startButton: Button
    private lateinit var resultButton: Button
    private lateinit var minutePicker: NumberPicker
    private lateinit var secondPicker: NumberPicker
    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient
    private val db = FirebaseFirestore.getInstance()
    private lateinit var profileImageView: ImageView

    private val PERMISSION_REQUEST_CODE = 1001
    private val RC_SIGN_IN = 9001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_timer)

        Log.d(TAG, "TimerActivity created")

        initializeViews()
        setupAuth()
        setupListeners()
        addAlarmFragment()
        checkNotificationPermission()
    }

    private fun initializeViews() {
        startButton = findViewById(R.id.start_button)
        resultButton = findViewById(R.id.result_button)
        minutePicker = findViewById(R.id.minute_picker)
        secondPicker = findViewById(R.id.second_picker)
        profileImageView = findViewById(R.id.profile_image_view)

        setupTimePickers()
    }

    private fun setupAuth() {
        val app = application as MyApplication
        auth = app.auth

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        loadUserProfileImage()
    }

    private fun setupListeners() {
        profileImageView.setOnClickListener { view -> showPopupMenu(view) }
        startButton.setOnClickListener { startBlinkActivity() }
        resultButton.setOnClickListener { goToResultScreen() }
    }

    private fun setupTimePickers() {
        minutePicker.minValue = 0
        minutePicker.maxValue = 59
        secondPicker.minValue = 0
        secondPicker.maxValue = 59

        minutePicker.value = 20
        secondPicker.value = 0

        val timeChangeListener = NumberPicker.OnValueChangeListener { _, _, _ -> updateTimeDisplay() }
        minutePicker.setOnValueChangedListener(timeChangeListener)
        secondPicker.setOnValueChangedListener(timeChangeListener)

        updateTimeDisplay()
    }

    private fun updateTimeDisplay() {
        // 필요한 경우 여기에 시간 표시 업데이트 로직 추가
    }

    private fun loadUserProfileImage() {
        val account: GoogleSignInAccount? = GoogleSignIn.getLastSignedInAccount(this)
        account?.let {
            val personPhoto = it.photoUrl
            if (personPhoto != null) {
                Glide.with(this)
                    .load(personPhoto)
                    .apply(RequestOptions.bitmapTransform(CircleCrop()))
                    .into(profileImageView)
            } else {
                profileImageView.setImageResource(R.drawable.ic_account)
            }
        }
    }

    private fun showPopupMenu(view: View) {
        val popupMenu = PopupMenu(this, view)
        popupMenu.menuInflater.inflate(R.menu.menu_main, popupMenu.menu)
        popupMenu.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_logout -> {
                    logoutUser()
                    true
                }
                R.id.action_delete_account -> {
                    showDeleteAccountWarning()
                    true
                }
                R.id.action_privacy_policy -> {
                    showPrivacyPolicy()
                    true
                }
                else -> false
            }
        }
        popupMenu.show()
    }

    private fun showDeleteAccountWarning() {
        AlertDialog.Builder(this)
            .setMessage("회원 탈퇴를 하면 모든 회원 정보가 삭제됩니다.\n회원을 탈퇴하시겠습니까?")
            .setPositiveButton("예") { _, _ -> startReauthenticationForDeletion() }
            .setNegativeButton("아니요") { dialog, _ -> dialog.dismiss() }
            .create().show()
    }

    private fun startReauthenticationForDeletion() {
        val signInIntent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)
                firebaseAuthWithGoogle(account.idToken!!)
            } catch (e: ApiException) {
                Log.w(TAG, "Google sign in failed", e)
                Toast.makeText(this, "Google 로그인 실패", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.currentUser?.reauthenticate(credential)
            ?.addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "User re-authenticated.")
                    deleteUserAccount()
                } else {
                    Log.w(TAG, "Re-authentication failed.", task.exception)
                    Toast.makeText(this, "재인증 실패. 다시 시도해주세요.", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun deleteUserAccount() {
        val user = auth.currentUser
        if (user == null) {
            Toast.makeText(this, "사용자 정보가 없습니다. 다시 로그인해주세요.", Toast.LENGTH_SHORT).show()
            navigateToLoginScreen()
            return
        }

        db.runTransaction { transaction ->
            val userDoc = db.collection("USERS").document(user.uid)
            transaction.delete(userDoc)
        }.addOnSuccessListener {
            user.delete().addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    UserManager.deleteUserData(this)
                    Toast.makeText(this, "회원탈퇴가 완료되었습니다.", Toast.LENGTH_SHORT).show()
                    navigateToLoginScreen()
                } else {
                    Log.w(TAG, "Error deleting user.", task.exception)
                    Toast.makeText(this, "계정 삭제에 실패했습니다. 다시 시도해주세요.", Toast.LENGTH_SHORT).show()
                }
            }
        }.addOnFailureListener { e ->
            Log.w(TAG, "Error deleting user data from Firestore", e)
            Toast.makeText(this, "데이터 삭제 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                showNotificationPermissionDialog()
            }
        }
    }

    private fun showNotificationPermissionDialog() {
        AlertDialog.Builder(this)
            .setTitle("알림 권한 필요")
            .setMessage("이 앱의 모든 기능을 사용하기 위해서는 알림 권한이 필요합니다. 설정에서 권한을 허용해주세요.")
            .setPositiveButton("설정으로 이동") { _, _ ->
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                val uri = Uri.fromParts("package", packageName, null)
                intent.data = uri
                startActivity(intent)
            }
            .setNegativeButton("취소") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun startBlinkActivity() {
        val minutes = minutePicker.value
        val seconds = secondPicker.value
        val totalTimeInSeconds = minutes * 60 + seconds

        val intent = Intent(this, BlinkActivity::class.java)
        intent.putExtra("TIMER_DURATION", totalTimeInSeconds)
        startActivity(intent)
    }

    private fun goToResultScreen() {
        val intent = Intent(this, ResultActivity::class.java)
        startActivity(intent)
    }

    private fun logoutUser() {
        Log.i(TAG, "User requested logout")

        auth.signOut()

        googleSignInClient.signOut().addOnCompleteListener(this) {
            UserManager.clearUserData()
            Log.i(TAG, "User successfully logged out")
            navigateToLoginScreen()
        }
    }

    private fun navigateToLoginScreen() {
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
        finish()
    }

    private fun showPrivacyPolicy() {
        val webView = WebView(this)
        webView.settings.javaScriptEnabled = true
        webView.loadUrl("https://sites.google.com/view/nunkkam001?usp=sharing")

        AlertDialog.Builder(this)
            .setTitle("개인정보 처리방침")
            .setView(webView)
            .setNegativeButton("닫기") { dialog, _ -> dialog.dismiss() }
            .create()
            .show()
    }

    private fun addAlarmFragment() {
        val fragment: Fragment = AlarmFragment()
        supportFragmentManager.beginTransaction()
            .replace(R.id.alarm_fragment_container, fragment)
            .commit()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d("PermissionDebug", "Notification permission granted")
                Toast.makeText(this, "알림 권한이 승인되었습니다.", Toast.LENGTH_SHORT).show()
            } else {
                Log.d("PermissionDebug", "Notification permission denied")
                Toast.makeText(this, "알림 권한이 거부되었습니다. 일부 기능이 제한될 수 있습니다.", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "TimerActivity destroyed")
    }

    companion object {
        private const val TAG = "TimerActivity"
    }
}