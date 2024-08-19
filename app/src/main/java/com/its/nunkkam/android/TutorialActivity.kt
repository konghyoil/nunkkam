package com.its.nunkkam.android

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class TutorialActivity : AppCompatActivity() {

    private lateinit var requestPermissionsButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tutorial)

        requestPermissionsButton = findViewById(R.id.btn_request_permissions)

        requestPermissionsButton.setOnClickListener {
            requestNecessaryPermissions()
        }
    }

    private fun requestNecessaryPermissions() {
        val permissionsToRequest = mutableListOf<String>()

        // 카메라 권한 확인
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.CAMERA)
        }

        // 포그라운드 서비스 권한 확인 (Android 9 이상)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.FOREGROUND_SERVICE) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.FOREGROUND_SERVICE)
        }

        // 알림 권한 확인 (Android 13 이상)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        // 권한이 필요하지 않으면, 바로 다음 단계로
        if (permissionsToRequest.isEmpty()) {
            Log.d("TutorialActivity", "All permissions are already granted.")
            onAllPermissionsGranted()
            return
        }

        // 권한 요청 실행
        Log.d("TutorialActivity", "Requesting permissions: $permissionsToRequest")
        permissionsLauncher.launch(permissionsToRequest.toTypedArray())
    }

    private val permissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        permissions.forEach { (permission, isGranted) ->
            Log.d("TutorialActivity", "Permission: $permission, Granted: $isGranted")
        }

        val allGranted = permissions.all { it.value }
        if (allGranted) {
            onAllPermissionsGranted()
        } else {
            Toast.makeText(this, "모든 권한이 허용되지 않았습니다.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun onAllPermissionsGranted() {
        // 모든 권한이 허용된 경우 TimerActivity로 이동
        Toast.makeText(this, "모든 권한이 허용되었습니다.", Toast.LENGTH_SHORT).show()
        val intent = Intent(this, TimerActivity::class.java)
        startActivity(intent)
        finish() // TutorialActivity 종료
    }
}
