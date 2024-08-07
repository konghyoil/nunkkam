package com.its.nunkkam.android

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.ImageView
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
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.its.nunkkam.android.UserManager.userId

class TimerActivity : AppCompatActivity() {

    private lateinit var startButton: Button
    private lateinit var resultButton: Button
//    private lateinit var logoutButton: Button
//    private lateinit var deleteAccountButton: Button
    private lateinit var timerTextView: TextView
    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient
    private val db = FirebaseFirestore.getInstance()
    private lateinit var profileImageView: ImageView


    // 알림 권한 요청을 위한 상수
    private val PERMISSION_REQUEST_CODE = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_timer)

        val app = application as MyApplication
        auth = app.auth
        googleSignInClient = app.googleSignInClient

        profileImageView = findViewById(R.id.profile_image_view)

        // 사용자 프로필 이미지 로드
        loadUserProfileImage()

        startButton = findViewById(R.id.start_button)
        resultButton = findViewById(R.id.result_button)
//        logoutButton = findViewById(R.id.logout_button)
//        deleteAccountButton = findViewById(R.id.delete_account_button)
        timerTextView = findViewById(R.id.timer_text)

        // 프로필 이미지 클릭 리스너 설정
        profileImageView.setOnClickListener { view ->
            showPopupMenu(view)
        }

        startButton.setOnClickListener {
            val intent = Intent(this, BlinkActivity::class.java)
            startActivity(intent)
        }

        resultButton.setOnClickListener {
            goToResultScreen()
            Log.d("TimerActivity", "userId: $userId")
        }

//        logoutButton.setOnClickListener {
//            logoutUser()
//            Log.d("TimerActivity", "userId: $userId")
//        }
//
//        deleteAccountButton.setOnClickListener {
//            deleteUserAccount()
//        }
//        checkCurrentUser()

        // AlarmFragment 추가
        addAlarmFragment()

        // 알림 권한 요청
        requestNotificationPermission()
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


    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                Log.d("PermissionDebug", "Requesting notification permission")
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), PERMISSION_REQUEST_CODE)
            } else {
                Log.d("PermissionDebug", "Notification permission already granted")
            }
        } else {
            Log.d("PermissionDebug", "Android version < 13, no need to request permission")
        }
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

    private fun checkCurrentUser() {                                 // 현재 사용자 확인 메서드
        val currentUser = auth.currentUser                           // 현재 로그인된 사용자 가져오기
        Log.d("TimerActivity", "Current user in TimerActivity: $currentUser")  // 로그 출력
    }

    private fun goToResultScreen() {                                 // 결과 화면으로 이동하는 메서드
        val intent = Intent(this, ResultActivity::class.java)        // ResultActivity로 이동하는 인텐트 생성
        startActivity(intent)                                        // ResultActivity 시작
        checkCurrentUser()                                           // 현재 사용자 확인
    }

    private fun logoutUser() {
        // Firebase에서 로그아웃
        auth.signOut()
        Log.d("TimerActivity", "Firebase 로그아웃 성공")

        // Google에서 로그아웃
        googleSignInClient.signOut().addOnCompleteListener(this) {
            if (it.isSuccessful) {
                Log.d("TimerActivity", "Google 로그아웃 성공")
            } else {
                Log.d("TimerActivity", "Google 로그아웃 실패: ${it.exception?.message}")
            }

            // 사용자 데이터 초기화 (삭제하지 않음)
            UserManager.clearUserData()
            Log.d("TimerActivity", "UserManager 데이터 초기화 성공")

            // MainActivity로 이동
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
            Log.d("TimerActivity", "MainActivity로 이동")
        }
    }

    private fun deleteUserAccount() {
        // FirebaseAuth 인스턴스를 통해 현재 사용자 정보를 가져옴
        val user = auth.currentUser

        // 현재 사용자가 존재하는 경우 아래의 작업을 수행
        user?.let {
            val userId = it.uid // 현재 사용자의 고유 ID를 가져옴

            // Firestore에서 사용자 데이터를 삭제
            db.collection("USERS").document(userId).delete().addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d("TimerActivity", "User data deleted from Firestore")

                    // Firebase Authentication에서 사용자 계정을 삭제
                    user.delete().addOnCompleteListener { deleteTask ->
                        if (deleteTask.isSuccessful) {
                            Log.d("TimerActivity", "User deleted from Firebase Auth")

                            // SharedPreferences에서 사용자 데이터 삭제
                            UserManager.deleteUserData(this)

                            // 메인 화면(MainActivity)으로 이동
                            val intent = Intent(this, MainActivity::class.java)
                            startActivity(intent)
                            finish()
                        } else {
                            // Firebase Auth에서 사용자 삭제 중 오류 발생 시 로그 출력
                            Log.e("TimerActivity", "Error deleting user from Firebase Auth", deleteTask.exception)
                        }
                    }
                } else {
                    // Firestore에서 사용자 데이터 삭제 중 오류 발생 시 로그 출력
                    Log.e("TimerActivity", "Error deleting user data from Firestore", task.exception)
                }
            }
        } ?: run {
            // 현재 사용자가 null인 경우 로그 출력
            Log.e("TimerActivity", "User is null")
        }
    }

    private fun addAlarmFragment() {
        val fragment: Fragment = AlarmFragment()
        supportFragmentManager.beginTransaction()
            .replace(R.id.alarm_fragment_container, fragment)
            .commit()
    }
}
