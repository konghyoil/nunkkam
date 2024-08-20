package com.its.nunkkam.android

import android.Manifest
import android.content.Context
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
import android.widget.Button
import android.widget.ImageView
import android.widget.NumberPicker
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.bumptech.glide.request.RequestOptions
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_timer)

        Log.d("TimerActivity", "TimerActivity created")

        startButton = findViewById(R.id.start_button)
        resultButton = findViewById(R.id.result_button)
        minutePicker = findViewById(R.id.minute_picker)
        secondPicker = findViewById(R.id.second_picker)

        setupTimePickers()

        val app = application as MyApplication
        auth = app.auth
        googleSignInClient = app.googleSignInClient

        profileImageView = findViewById(R.id.profile_image_view)

        loadUserProfileImage()

        profileImageView.setOnClickListener { view ->
            showPopupMenu(view)
        }

        startButton.setOnClickListener {
            startBlinkActivity()
        }

        resultButton.setOnClickListener {
            goToResultScreen()
        }

        addAlarmFragment()
        checkNotificationPermission()
    }

    private fun setupTimePickers() {
        minutePicker.minValue = 0
        minutePicker.maxValue = 59

        secondPicker.minValue = 0
        secondPicker.maxValue = 59

        // 초기 시간 설정 (예: 20분)
        minutePicker.value = 20
        secondPicker.value = 0

        val timeChangeListener = NumberPicker.OnValueChangeListener { _, _, _ ->
            updateTimeDisplay()
        }
        minutePicker.setOnValueChangedListener(timeChangeListener)
        secondPicker.setOnValueChangedListener(timeChangeListener)

        updateTimeDisplay()
    }

    private fun updateTimeDisplay() {
        val minutes = minutePicker.value
        val seconds = secondPicker.value
        // 여기서 시간 표시를 업데이트합니다 (필요한 경우)
        // 예: timerTextView.text = String.format("%02d:%02d", minutes, seconds)
    }

    private fun startBlinkActivity() {
        val minutes = minutePicker.value
        val seconds = secondPicker.value
        val totalTimeInSeconds = minutes * 60 + seconds

        val intent = Intent(this, BlinkActivity::class.java)
        intent.putExtra("TIMER_DURATION", totalTimeInSeconds)
        startActivity(intent)
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
                // 프로필 사진이 없는 경우 기본 아이콘 사용
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
        val builder = AlertDialog.Builder(this)
        builder.setMessage("회원 탈퇴를 하면 모든 회원 정보가 삭제됩니다.\n회원을 탈퇴하시겠습니까?")
            .setPositiveButton("예") { dialog, id ->
                deleteUserAccount()
                // MainActivity로 이동
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
                finish()
            }
            .setNegativeButton("아니요") { dialog, id ->
                dialog.dismiss()
            }
        builder.create().show()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
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
            .setNegativeButton("취소") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
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

    private fun checkCurrentUser() {
        val currentUser = auth.currentUser
        // 테스트 로그 제거됨
    }

    private fun goToResultScreen() {
        val intent = Intent(this, ResultActivity::class.java)
        startActivity(intent)
        checkCurrentUser()
    }

    private fun logoutUser() {
        // 로그아웃 시 로그 기록
        Log.i("TimerActivity", "User requested logout")

        auth.signOut()

        googleSignInClient.signOut().addOnCompleteListener(this) {
            UserManager.clearUserData()

            // 로그아웃 성공 시 로그 기록
            Log.i("TimerActivity", "User successfully logged out")

            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun deleteUserAccount() {
        val user = auth.currentUser

        user?.let {
            val userId = it.uid

            // 회원탈퇴 시 로그 기록
            Log.i("TimerActivity", "User requested account deletion")

            db.collection("USERS").document(userId).delete().addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    user.delete().addOnCompleteListener { deleteTask ->
                        if (deleteTask.isSuccessful) {
                            UserManager.deleteUserData(this)

                            // 회원탈퇴 성공 시 로그 기록
                            Log.i("TimerActivity", "User account successfully deleted")

                            val intent = Intent(this, MainActivity::class.java)
                            startActivity(intent)
                            finish()
                        } else {
                            // 에러 로그 유지
                            Log.e("TimerActivity", "Error deleting user from Firebase Auth", deleteTask.exception)
                        }
                    }
                } else {
                    // 에러 로그 유지
                    Log.e("TimerActivity", "Error deleting user data from Firestore", task.exception)
                }
            }
        } ?: run {
            // 에러 로그 유지
            Log.e("TimerActivity", "User is null")
        }
    }

    private fun showPrivacyPolicy(){
        val webView = WebView(this)
        webView.settings.javaScriptEnabled = true
        webView.loadUrl("https://sites.google.com/view/nunkkam001?usp=sharing")

        val dialog = AlertDialog.Builder(this)
            .setTitle("개인정보 처리방침")
            .setView(webView)
            .setNegativeButton("닫기") { dialog, _ -> dialog.dismiss() }
            .create()

        dialog.show()
    }

    private fun addAlarmFragment() {
        val fragment: Fragment = AlarmFragment()
        supportFragmentManager.beginTransaction()
            .replace(R.id.alarm_fragment_container, fragment)
            .commit()
    }

    // 추가된 메서드: TimerActivity가 파괴될 때 로그 출력
    override fun onDestroy() {
        super.onDestroy()
        Log.d("TimerActivity", "TimerActivity destroyed")
    }
}
