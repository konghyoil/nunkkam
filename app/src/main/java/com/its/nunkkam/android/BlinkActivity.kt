package com.its.nunkkam.android

// 필요한 라이브러리들을 가져오기
import android.Manifest
import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.os.IBinder
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.framework.image.MPImage
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarker
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarkerResult
import java.util.Date
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.abs

// BlinkActivity 클래스 정의: 앱의 눈 깜빡임 화면을 담당
class BlinkActivity : AppCompatActivity() {

    // 클래스 내부에서 사용할 변수들을 선언
    private lateinit var faceLandmarker: FaceLandmarker
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var viewFinder: PreviewView
    private lateinit var eyeStatusImageView: ImageView
    private lateinit var eyeStatusTextView: TextView
    private var blinkCount = 0
    private var lastEyeState = true
    private lateinit var blinkCountTextView: TextView
    private var lastBlinkTime = System.currentTimeMillis()
    private var blinkRate = 0.0
    private lateinit var blinkRateTextView: TextView
    private var frameCounter = 0
    private var lastFpsUpdateTime = System.currentTimeMillis()
    private var fps = 0f
    private lateinit var fpsTextView: TextView

    // 홍철 타이머 관련 코드 추가
    private lateinit var timerTextView: TextView
    private lateinit var pauseButton: Button
    private lateinit var restartButton: Button
    private lateinit var resetButton: Button

    private var userId: String = ""
    private var birthDate: Timestamp? = null
    private var isGoogleLogin: Boolean = false

    private var countDownTimer: CountDownTimer? = null
    private var timeLeftInMillis: Long = 1200000
    private var timerRunning: Boolean = false
    private var startTime: Long = 0
    private var endTime: Long = 0
    private var pausedStartTime: Long = 0
    private var pausedAccumulatedTime: Long = 0
    private val db = FirebaseFirestore.getInstance()

    private lateinit var cameraService: CameraService
    private var serviceBound = false

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as CameraService.LocalBinder
            cameraService = binder.getService()
            cameraService.setCallback(cameraCallback)
            if (allPermissionsGranted()) {
                cameraService.startCamera(viewFinder)
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            // 서비스 연결이 끊어졌을 때의 처리
        }
    }

    private val cameraCallback = object : CameraService.CameraCallback {
        override fun onFaceLandmarkerResult(result: FaceLandmarkerResult, image: MPImage) {
            handleFaceLandmarkerResult(result, image)
        }
    }

    private val REQUEST_CODE_POST_NOTIFICATIONS = 101 // 권한 요청 코드
    private val CAMERA_PERMISSION_REQUEST_CODE = 1001

    // 액티비티가 생성될 때 호출되는 함수
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState) // 부모 클래스의 onCreate 메서드 호출
        setContentView(R.layout.activity_blink) // [외부] 레이아웃 XML 파일을 가져와 화면에 설정

        if (!allPermissionsGranted()) {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
            )
        }

        // POST_NOTIFICATIONS 권한이 허용되지 않은 경우 요청
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), REQUEST_CODE_POST_NOTIFICATIONS)
        }

        // 서비스 시작 및 바인딩
        val intent = Intent(this, CameraService::class.java)
        ContextCompat.startForegroundService(this, intent)
        bindService(intent, connection, Context.BIND_AUTO_CREATE)

        // XML에서 정의한 뷰들을 찾아 변수에 할당
        viewFinder = findViewById(R.id.viewFinder)
        eyeStatusImageView = findViewById(R.id.eyeStatusImageView)
        eyeStatusTextView = findViewById(R.id.textViewEyeStatus)
        fpsTextView = findViewById(R.id.fpsTextView)

        // 홍철 타이머 관련 내용 추가
        blinkCountTextView = findViewById(R.id.blinkCountTextView)
        blinkRateTextView = findViewById(R.id.blinkRateTextView)

        // 타이머 관련 뷰 초기화
        timerTextView = findViewById(R.id.timer_text)
        pauseButton = findViewById(R.id.pause_button)
        restartButton = findViewById(R.id.restart_button)
        resetButton = findViewById(R.id.reset_button)

        val sharedPreferences = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        userId = sharedPreferences.getString("user_id", null) ?: ""
        Log.e("BlinkActivity", "User ID_1: $userId")

        isGoogleLogin = intent.getBooleanExtra("isGoogleLogin", false)

        if (isGoogleLogin) {
            // Google 로그인 시 생년월일을 가져오는 로직을 추가합니다.
            val auth = FirebaseAuth.getInstance()
            val currentUser = auth.currentUser
            currentUser?.let {
                birthDate = getBirthDateFromGoogleAccount(it)
                Log.e("BlinkActivity", "birthDate: $birthDate")
            }
        }

        // 깜빡임 카운트 표시 관련 TextView 초기화 및 UI 업데이트
        blinkCountTextView = findViewById(R.id.blinkCountTextView)
        blinkRateTextView = findViewById(R.id.blinkRateTextView)
        updateBlinkUI()

        // 홍철 타이머 관련 내용 추가
        // 타이머 시작
        startTimer()

        pauseButton.setOnClickListener { pauseTimer() }
        restartButton.setOnClickListener { restartTimer() }
        resetButton.setOnClickListener { resetTimer() }
    }

    // 홍철 타이머 관련 함수
    private fun startTimer() {
        if (startTime == 0L) {
            startTime = System.currentTimeMillis()
        }
        if (pausedStartTime != 0L) {
            pausedAccumulatedTime += System.currentTimeMillis() - pausedStartTime
        }
        countDownTimer?.cancel()
        countDownTimer = object : CountDownTimer(timeLeftInMillis, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                timeLeftInMillis = millisUntilFinished
                updateTimerText()
            }

            override fun onFinish() {
                timerRunning = false
                endTime = System.currentTimeMillis()
                saveMeasurementData()
            }
        }.start()

        timerRunning = true
    }

    private fun pauseTimer() {
        countDownTimer?.cancel()
        timerRunning = false
        pausedStartTime = System.currentTimeMillis()
    }

    private fun restartTimer() {
        timerRunning = true
        if (pausedStartTime != 0L) {
            pausedAccumulatedTime += System.currentTimeMillis() - pausedStartTime
            pausedStartTime = 0L
        }
        countDownTimer?.cancel()
        countDownTimer = object : CountDownTimer(timeLeftInMillis, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                timeLeftInMillis = millisUntilFinished
                updateTimerText()
            }

            override fun onFinish() {
                timerRunning = false
                startTime = System.currentTimeMillis()
                saveMeasurementData()
            }
        }.start()
    }

    private fun resetTimer() {
        endTime = System.currentTimeMillis()
        saveMeasurementData()

        countDownTimer?.cancel()
        timeLeftInMillis = 1200000
        updateTimerText()
        timerRunning = false
        startTime = 0
        endTime = 0
        pausedStartTime = 0
        pausedAccumulatedTime = 0

        // TimerFragment로 돌아가도록 설정
        finish()
    }

    @SuppressLint("DefaultLocale")
    private fun updateTimerText() {
        val minutes = (timeLeftInMillis / 1000) / 60
        val seconds = (timeLeftInMillis / 1000) % 60
        val timeFormatted = String.format("00:%02d:%02d", minutes, seconds)
        timerTextView.text = timeFormatted
    }

    private fun saveMeasurementData() {
        val measurementTimeInSeconds = (endTime - startTime - pausedAccumulatedTime) / 1000
        val measurementTimeInMinutes = measurementTimeInSeconds / 60.0
        val measurementTime = Timestamp(Date(startTime))
        val count = blinkCount

        val blinkData = hashMapOf(
            "count" to count,
            "measurement_time" to measurementTimeInMinutes,
            "measurement_date" to measurementTime,
            "average_frequency_per_minute" to count / measurementTimeInMinutes
        )

        val userDocument = db.collection("USERS").document(userId)
        Log.e("BlinkActivity", "User ID_2: $userId")
        userDocument.get().addOnSuccessListener { document ->
            if (document.exists()) {
                userDocument.update("blinks", FieldValue.arrayUnion(blinkData))
            } else {
                val newUser = hashMapOf(
                    "birth_date" to birthDate,
                    "tutorial" to true,
                    "blinks" to listOf(blinkData)
                )
                userDocument.set(newUser)
            }
        }
    }

    private fun getBirthDateFromGoogleAccount(user: FirebaseUser): Timestamp? {
        // Google 계정에서 생년월일 정보를 가져오는 로직을 여기에 추가합니다.
        // 생년월일 정보는 Google API에서 직접 가져올 수 없습니다.
        // 대안으로는 사용자가 생년월일을 입력하도록 요청하는 방법이 있습니다.
        return null
    }

    // FaceLandmarker 결과 처리 함수
    @Suppress("UNUSED_PARAMETER")
    private fun handleFaceLandmarkerResult(result: FaceLandmarkerResult, image: MPImage) {
        if (!this::viewFinder.isInitialized) {
            Log.e(TAG, "ViewFinder is not initialized")
            return
        }

        frameCounter++

        // FPS 계산
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastFpsUpdateTime >= 1000) {
            fps = frameCounter * 1000f / (currentTime - lastFpsUpdateTime)
            frameCounter = 0
            lastFpsUpdateTime = currentTime
            updateFpsUI()
        }

        if (result.faceLandmarks().isEmpty()) return

        val landmarks = result.faceLandmarks()[0]

        // 눈 랜드마크 좌표 가져오기
        val leftEyeTop = landmarks[159]
        val leftEyeBottom = landmarks[145]
        val leftEyeInner = landmarks[33]
        val leftEyeOuter = landmarks[133]
        val rightEyeTop = landmarks[386]
        val rightEyeBottom = landmarks[374]
        val rightEyeInner = landmarks[263]
        val rightEyeOuter = landmarks[362]

        // 눈 개폐 정도 계산
        val leftEyeOpenness = calculateEyeOpenness(leftEyeTop, leftEyeBottom, leftEyeInner, leftEyeOuter)
        val rightEyeOpenness = calculateEyeOpenness(rightEyeTop, rightEyeBottom, rightEyeInner, rightEyeOuter)
        val averageEyeOpenness = (leftEyeOpenness + rightEyeOpenness) / 2

        Log.d("FaceLandmarks", "Total landmarks detected: ${landmarks.size}, FPS: $fps")
        Log.d("EyeOpenness", "FPS: $fps, Left Eye: $leftEyeOpenness, Right Eye: $rightEyeOpenness, Average: $averageEyeOpenness")

        val eyesOpen = averageEyeOpenness >= BLINK_THRESHOLD

        if (eyesOpen != lastEyeState && !eyesOpen) {
            blinkCount++
            val timeDiff = (currentTime - lastBlinkTime) / 1000.0
            blinkRate = 60.0 / timeDiff
            lastBlinkTime = currentTime
            updateBlinkUI()
        }
        lastEyeState = eyesOpen

        if (!eyesOpen) {
            updateUI("Eye is closed", R.drawable.eye_closed)
        } else {
            updateUI("Eye is open", R.drawable.eye_open)
        }
    }

    private fun calculateEyeOpenness(
        top: NormalizedLandmark,
        bottom: NormalizedLandmark,
        inner: NormalizedLandmark,
        outer: NormalizedLandmark
    ): Float {
        val verticalDistance = abs(top.y() - bottom.y())
        val horizontalDistance = abs(outer.x() - inner.x())
        return verticalDistance / horizontalDistance
    }

    private fun updateBlinkCount() {
        cameraService.incrementBlinkCount()
        saveBlinkCountToPreferences()
        updateBlinkUI()
    }

    private fun saveBlinkCountToPreferences() {
        val blinkCount = cameraService.getBlinkCount()
        val sharedPref = getSharedPreferences("MyApp", Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            putInt("blink_count", blinkCount)
            apply()
        }
    }

    @SuppressLint("SetTextI18n")
    private fun updateBlinkUI() {
        runOnUiThread {
            val blinkCount = cameraService.getBlinkCount()
            blinkCountTextView.text = "Total Blinks: $blinkCount"
            blinkRateTextView.text = "Blink Rate: %.2f bpm".format(blinkRate)
        }
    }

    @SuppressLint("SetTextI18n")
    private fun updateFpsUI() {
        runOnUiThread {
            fpsTextView.text = "FPS: %.2f".format(fps)
        }
    }

    private fun updateUI(message: String, drawableResId: Int) {
        runOnUiThread {
            eyeStatusTextView.text = message
            eyeStatusImageView.setImageResource(drawableResId)
        }
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                cameraService.startCamera(viewFinder)
            } else {
                Toast.makeText(this, "Permissions not granted by the user.", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (serviceBound) {
            unbindService(connection)
            serviceBound = false
        }
    }

    override fun onPause() {
        super.onPause()
        if (serviceBound) {
            cameraService.stopCamera()
        }
    }

    override fun onResume() {
        super.onResume()
        if (!serviceBound) {
            val intent = Intent(this, CameraService::class.java)
            bindService(intent, connection, Context.BIND_AUTO_CREATE)
        } else {
            if (allPermissionsGranted()) {
                cameraService.startCamera(viewFinder)
            }
        }
    }

    companion object {
        private const val TAG = "CameraXApp"
        private const val BLINK_THRESHOLD = 0.5
        private const val REQUEST_CODE_PERMISSIONS = 10

        private val REQUIRED_PERMISSIONS = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(
                Manifest.permission.CAMERA,
                Manifest.permission.POST_NOTIFICATIONS,
                Manifest.permission.FOREGROUND_SERVICE
            )
        } else {
            arrayOf(
                Manifest.permission.CAMERA,
                Manifest.permission.FOREGROUND_SERVICE
            )
        }
    }
}
