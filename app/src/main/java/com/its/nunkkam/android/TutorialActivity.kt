package com.its.nunkkam.android

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class TutorialActivity : AppCompatActivity() {

    private lateinit var requestPermissionsButton: Button
    private val PERMISSION_REQUEST_CODE = 100
    private val requiredPermissions = mutableListOf(
        Manifest.permission.CAMERA,
        Manifest.permission.INTERNET,
        Manifest.permission.POST_NOTIFICATIONS
    ).apply {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            add(Manifest.permission.FOREGROUND_SERVICE)
        }
    }

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tutorial)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        requestPermissionsButton = findViewById(R.id.btn_request_permissions)

        requestPermissionsButton.setOnClickListener {
            if (allPermissionsGranted()) {
                completeTutorial()
            } else {
                requestNecessaryPermissions()
            }
        }

        checkTutorialStatus()
    }

    private fun checkTutorialStatus() {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            db.collection("USERS").document(userId).get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        val tutorialCompleted = document.getBoolean("tutorial") ?: false
                        if (tutorialCompleted) {
                            navigateToTimerActivity()
                        }
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Error checking tutorial status: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun requestNecessaryPermissions() {
        ActivityCompat.requestPermissions(
            this,
            requiredPermissions.toTypedArray(),
            PERMISSION_REQUEST_CODE
        )
    }

    private fun allPermissionsGranted() = requiredPermissions.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (allPermissionsGranted()) {
                completeTutorial()
            } else {
                showPermissionExplanationDialog()
            }
        }
    }

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
                finish()
            }
            .setCancelable(false)
            .show()
    }

    private fun completeTutorial() {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            db.collection("USERS").document(userId)
                .update("tutorial", true)
                .addOnSuccessListener {
                    navigateToTimerActivity()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Error updating tutorial status: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        } else {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
        }
    }

    private fun navigateToTimerActivity() {
        val intent = Intent(this, TimerActivity::class.java)
        startActivity(intent)
        finish()
    }
}