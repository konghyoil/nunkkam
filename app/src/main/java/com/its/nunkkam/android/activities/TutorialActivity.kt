package com.its.nunkkam.android.activities

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.viewpager2.widget.ViewPager2
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.its.nunkkam.android.R
import com.its.nunkkam.android.adapters.TutorialPagerAdapter

class TutorialActivity : AppCompatActivity() {

    // ViewPager2와 인디케이터 뷰들을 위한 변수 선언
    private lateinit var viewPager: ViewPager2
    private lateinit var indicatorViews: List<View>

    // 권한 요청 버튼
    private lateinit var requestPermissionsButton: Button

    // 권한 요청 코드
    private val PERMISSION_REQUEST_CODE = 100

    // 필요한 권한 목록
    private val requiredPermissions = mutableListOf(
        Manifest.permission.CAMERA,
        Manifest.permission.INTERNET,
        Manifest.permission.POST_NOTIFICATIONS
    ).apply {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            add(Manifest.permission.FOREGROUND_SERVICE)
        }
    }

    // Firebase 인증 및 Firestore 인스턴스
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.tutorial1)

        // Firebase 인스턴스 초기화
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // ViewPager2 및 인디케이터 뷰 초기화
        viewPager = findViewById(R.id.viewPager)
        indicatorViews = listOf(
            findViewById(R.id.indicator1),
            findViewById(R.id.indicator2),
            findViewById(R.id.indicator3),
            findViewById(R.id.indicator4)
        )

        // ViewPager2에 어댑터 설정
        val adapter = TutorialPagerAdapter(this)
        viewPager.adapter = adapter

        // 페이지 변경 리스너 설정
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                if (position < indicatorViews.size) {
                    updateIndicator(position) // 인디케이터 업데이트
                } else {
                    // 추가된 페이지에 도달하면 activity_tutorial.xml로 전환
                    showPermissionRequestScreen()
                }
            }
        })

        // 튜토리얼 상태 확인
        checkTutorialStatus()
    }

    // 인디케이터 업데이트 함수
    private fun updateIndicator(position: Int) {
        indicatorViews.forEachIndexed { index, view ->
            view.setBackgroundColor(
                if (index == position) ContextCompat.getColor(this, R.color.white)
                else ContextCompat.getColor(this, R.color.dark_lightbar)
            )
        }
    }

    // 튜토리얼 상태 확인 함수
    private fun checkTutorialStatus() {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            db.collection("USERS").document(userId).get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        val tutorialCompleted = document.getBoolean("tutorial") ?: false
                        if (tutorialCompleted) {
                            navigateToTimerActivity() // 튜토리얼 완료 시 타이머 화면으로 이동
                        } else {
                            // 튜토리얼을 완료하지 않았다면 ViewPager2가 보이는 상태 유지
                        }
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Error checking tutorial status: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    // 권한 요청 화면 표시 함수
    private fun showPermissionRequestScreen() {
        setContentView(R.layout.activity_tutorial) // activity_tutorial.xml로 레이아웃 전환
        requestPermissionsButton = findViewById(R.id.btn_request_permissions)
        requestPermissionsButton.setOnClickListener {
            requestNecessaryPermissions() // 권한 요청 수행
        }
    }

    // 필요한 권한 요청 함수
    private fun requestNecessaryPermissions() {
        ActivityCompat.requestPermissions(
            this,
            requiredPermissions.toTypedArray(),
            PERMISSION_REQUEST_CODE
        )
    }

    // 모든 권한이 부여되었는지 확인하는 함수
    private fun allPermissionsGranted() = requiredPermissions.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    // 권한 요청 결과 처리 함수
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (allPermissionsGranted()) {
                completeTutorial() // 모든 권한이 부여되었을 경우 튜토리얼 완료 처리
            } else {
                showPermissionExplanationDialog() // 권한 거부 시 설명 다이얼로그 표시
            }
        }
    }

    // 권한 설명 다이얼로그 표시 함수
    private fun showPermissionExplanationDialog() {
        AlertDialog.Builder(this)
            .setTitle("권한이 필요합니다")
            .setMessage("이 앱을 사용하기 위해서는 모든 권한이 필요합니다. 설정에서 권한을 허용해주세요.")
            .setPositiveButton("설정으로 이동") { _, _ ->
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                val uri = Uri.fromParts("package", packageName, null)
                intent.data = uri
                startActivity(intent)
            }
            .setNegativeButton("취소") { dialog, _ ->
                dialog.dismiss()
                finish() // 권한이 허용되지 않으면 앱 종료
            }
            .setCancelable(false)
            .show()
    }

    // 튜토리얼 완료 처리 함수
    private fun completeTutorial() {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            db.collection("USERS").document(userId)
                .update("tutorial", true)
                .addOnSuccessListener {
                    navigateToTimerActivity() // 튜토리얼 완료 시 타이머 화면으로 이동
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Error updating tutorial status: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        } else {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
        }
    }

    // TimerActivity로 이동하는 함수
    private fun navigateToTimerActivity() {
        val intent = Intent(this, TimerActivity::class.java)
        startActivity(intent)
        finish() // 현재 액티비티 종료
    }
}
