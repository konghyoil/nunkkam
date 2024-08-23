package com.its.nunkkam.android.activities // 패키지 선언: 이 코드가 속한 패키지를 지정

// 필요한 라이브러리들을 가져오기
import android.Manifest // 안드로이드 권한 관련 클래스
import android.annotation.SuppressLint // 특정 lint 경고를 억제하기 위한 어노테이션
import android.content.ComponentName // 컴포넌트 이름을 나타내는 클래스
import android.content.Context // 애플리케이션 환경에 대한 정보를 제공하는 클래스
import android.content.Intent // 컴포넌트 간 통신을 위한 메시지 객체
import android.content.ServiceConnection // 서비스 연결을 위한 인터페이스
import android.content.pm.PackageManager // 패키지 관리자 클래스
import android.net.Uri
import android.os.Build // 빌드 버전 정보를 제공하는 클래스
import android.os.Bundle // 키-값 쌍의 데이터를 저장하는 클래스
import android.os.CountDownTimer // 카운트다운 타이머 클래스
import android.os.IBinder // 서비스와 통신하기 위한 인터페이스
import android.provider.Settings
import android.util.Log // 로그 출력을 위한 클래스
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.Button // 버튼 위젯
import android.widget.ImageView // 이미지 뷰 위젯
import android.widget.TextView // 텍스트 뷰 위젯
import android.widget.Toast // 짧은 메시지를 화면에 표시하는 클래스
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity // 앱 호환성을 위한 기본 액티비티 클래스
import androidx.camera.view.PreviewView // 카메라 미리보기 뷰 클래스
import androidx.core.app.ActivityCompat // 액티비티 호환성 관련 클래스
import androidx.core.content.ContextCompat // 컨텍스트 호환성 관련 클래스
import com.google.firebase.Timestamp // Firebase 타임스탬프 클래스
import com.google.firebase.auth.FirebaseAuth // Firebase 인증 클래스
import com.google.firebase.auth.FirebaseUser // Firebase 사용자 클래스
import com.google.firebase.firestore.FieldValue // Firestore 필드 값 조작 클래스
import com.google.firebase.firestore.FirebaseFirestore // Firestore 데이터베이스 클래스
import com.google.mediapipe.framework.image.MPImage // MediaPipe 이미지 클래스
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarkerResult // 얼굴 랜드마크 결과 클래스
import com.its.nunkkam.android.R
import com.its.nunkkam.android.managers.LandmarkDetectionManager
import com.its.nunkkam.android.services.CameraService
import com.its.nunkkam.android.utils.BlinkDetectionUtil
import java.util.Date // 날짜 클래스

// 주석 규칙 | [외부]: 외부 데이터에서 가저오는 부분을 구분하기 위한 주석

// BlinkActivity 클래스 정의: 앱의 눈 깜빡임 화면을 담당
class BlinkActivity : AppCompatActivity() {
    private lateinit var blinkDetectionUtil: BlinkDetectionUtil
    private lateinit var landmarkDetectionManager: LandmarkDetectionManager

    // 클래스 내부에서 사용할 변수들을 선언
    private lateinit var viewFinder: PreviewView // 카메라 미리보기 뷰
    private lateinit var eyeStatusImageView: ImageView // 눈 상태를 표시할 이미지 뷰
    private lateinit var eyeStatusTextView: TextView // 눈 상태를 표시할 텍스트 뷰
    private lateinit var blinkCountTextView: TextView // 눈 깜빡임 횟수를 표시할 TextView
    private lateinit var fpsTextView: TextView // FPS를 표시할 TextView
    private lateinit var blinkRateTextView: TextView // 분당 눈 깜빡임 횟수를 표시할 TextView
    private var timeLeftInMillis: Long = 0

    // 타이머
    private lateinit var timerTextView: TextView // 타이머 텍스트 뷰
    private lateinit var pauseButton: Button // 일시정지 버튼
    private lateinit var restartButton: Button // 재시작 버튼
    private lateinit var resetButton: Button // 리셋 버튼

    private var userId: String = "" // 사용자 ID
    private var birthDate: Timestamp? = null // 사용자 생년월일
    private var isGoogleLogin: Boolean = false // Google 로그인 여부

    private var countDownTimer: CountDownTimer? = null // 카운트다운 타이머
    private val db = FirebaseFirestore.getInstance() // Firestore 데이터베이스 인스턴스

    // 카메라 및 포그라운드/백그라운드 구분
    private lateinit var cameraService: CameraService // 카메라 서비스
    private var serviceBound = false // 서비스 바인딩 여부

    private lateinit var overlayPermissionLauncher: ActivityResultLauncher<Intent>
    private lateinit var overlayView: View

    private var isViewFinderVisible = true // viewFinder 표시 상태 추적 플래그
    private var overlayViewAttached = false // 오버레이 뷰 추적 플래그

    // ServiceConnection 객체: 서비스와의 연결을 관리
    private val connection = object : ServiceConnection {
        // 서비스 연결 시 호출되는 메서드
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            Log.d(TAG, "Service connected") // 서비스 연결 로그
            val binder = service as CameraService.LocalBinder // 서비스 바인더 획득
            cameraService = binder.getService() // 서비스 인스턴스 획득
            cameraService.setCallback(cameraCallback) // 콜백 설정
            cameraService.setLandmarkDetectionManager(landmarkDetectionManager)
            serviceBound = true // 서비스 바인딩 상태 설정

            Log.d(TAG, "Starting camera from onServiceConnected") // 카메라 시작 로그
//            cameraService.startCamera(viewFinder) // 카메라 시작 | viewFinder 전달
            // 모든 권한이 부여된 경우에만 카메라를 시작
            if (allPermissionsGranted()) {
                val overlayPreviewView = overlayView.findViewById<PreviewView>(R.id.previewView)
                cameraService.startCamera(viewFinder, null)
            }
        }

        // 서비스 연결 해제 시 호출되는 메서드
        override fun onServiceDisconnected(name: ComponentName?) {
            Log.d(TAG, "Service disconnected") // 서비스 연결 해제 로그
            serviceBound = false // 서비스 바인딩 상태 해제
        }
    }

    // 카메라 콜백 객체: 얼굴 랜드마크 결과 처리
    private val cameraCallback = object : CameraService.CameraCallback {
        // 얼굴 랜드마크 결과 수신 시 호출되는 메서드
        override fun onFaceLandmarkerResult(result: FaceLandmarkerResult, image: MPImage) {
            Log.d(TAG, "Face landmark result received in BlinkActivity") // 결과 수신 로그
//            handleFaceLandmarkerResult(result, image) // 결과 처리
            handleFaceLandmarkerResult(result)
        }
    }

    // 초기 UI 설정 메서드
    private fun initViews() {
        // XML에서 정의한 뷰들을 찾아 변수에 할당
        viewFinder = findViewById(R.id.viewFinder) // 카메라 미리보기 뷰
        if (!::viewFinder.isInitialized) {
            Log.e(TAG, "viewFinder가 제대로 초기화되지 않았습니다.")
            return  // 초기화 실패 시 더 이상 진행하지 않음
        } else {
            Log.d(TAG, "viewFinder 초기화 완료")
        }

        eyeStatusImageView = findViewById(R.id.eyeStatusImageView) // 눈 상태 이미지 뷰
        eyeStatusTextView = findViewById(R.id.textViewEyeStatus) // 눈 상태 텍스트 뷰
        fpsTextView = findViewById(R.id.fpsTextView) // FPS 표시 텍스트 뷰

        // 깜빡임 카운트 표시 관련 TextView 초기화 및 UI 업데이트
        blinkCountTextView = findViewById(R.id.blinkCountTextView) // 깜빡임 횟수 텍스트 뷰
        blinkRateTextView = findViewById(R.id.blinkRateTextView) // 깜빡임 빈도 텍스트 뷰

        // 오버레이 뷰 초기화
        val windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val overlayRoot = LayoutInflater.from(this).inflate(R.layout.overlay_layout, null)
        overlayView = overlayRoot.findViewById(R.id.previewView) // 오버레이 프리뷰 뷰 초기화

        // 타이머 뷰
        timerTextView = findViewById(R.id.timer_text) // 타이머 텍스트 뷰
        pauseButton = findViewById(R.id.pause_button) // 일시정지 버튼
        restartButton = findViewById(R.id.restart_button) // 재시작 버튼
        resetButton = findViewById(R.id.reset_button) // 리셋 버튼

        val sharedPreferences = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE) // SharedPreferences 객체 생성
        userId = sharedPreferences.getString("user_id", null) ?: "" // 사용자 ID 가져오기
        Log.e("BlinkActivity", "User ID_1: $userId") // 사용자 ID 로그

        isGoogleLogin = intent.getBooleanExtra("isGoogleLogin", false) // Google 로그인 여부 확인

        if (isGoogleLogin) {
            // Google 로그인 시 생년월일을 가져오는 로직
            val auth = FirebaseAuth.getInstance() // Firebase 인증 인스턴스 획득
            val currentUser = auth.currentUser // 현재 사용자 정보 획득
            currentUser?.let {
                birthDate = getBirthDateFromGoogleAccount(it) // Google 계정에서 생년월일 가져오기
                Log.e("BlinkActivity", "birthDate: $birthDate") // 생년월일 로그
            }
        }
    }

    // 액티비티가 생성될 때 호출되는 함수
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState) // 부모 클래스의 onCreate 메서드 호출
        setContentView(R.layout.activity_blink) // 레이아웃 XML 파일을 가져와 화면에 설정

        blinkDetectionUtil = BlinkDetectionUtil()
        timerTextView = findViewById(R.id.timer_text)

        initViews() // 초기 UI 및 뷰 설정
        blinkDetectionUtil.setStartTime(System.currentTimeMillis())
        landmarkDetectionManager = LandmarkDetectionManager(this)

        // 오버레이 권한 요청 처리 초기화
        overlayPermissionLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (Settings.canDrawOverlays(this)) {
                Log.d(TAG, "Overlay permission granted.")
            } else {
                Toast.makeText(this, "Overlay permission is required for this feature.", Toast.LENGTH_LONG).show()
            }
        }

        landmarkDetectionManager.initialize {
            requestPermissions() // 권한 요청
        }

        landmarkDetectionManager.setResultListener { result ->
            handleFaceLandmarkerResult(result)
        }

        val durationInSeconds = intent.getIntExtra("TIMER_DURATION", 1200) // 기본값 20분
        timeLeftInMillis = durationInSeconds * 1000L
        blinkDetectionUtil.setTimeLeftInMillis(timeLeftInMillis)

        updateBlinkUI() // UI 업데이트

        startTimer() // 타이머 시작

        pauseButton.setOnClickListener {
            pauseTimer()
            blinkDetectionUtil.pauseBlinkDetection()  // 눈 깜빡임 감지 일시정지
            blinkDetectionUtil.pauseFpsCalculation()  // FPS 계산 일시정지
        } // 일시정지 버튼 클릭 리스너
        restartButton.setOnClickListener {
            restartTimer()
            blinkDetectionUtil.resumeBlinkDetection()  // 눈 깜빡임 감지 재개
            blinkDetectionUtil.resumeFpsCalculation()  // FPS 계산 재개
        } // 재시작 버튼 클릭 리스너
        resetButton.setOnClickListener {
            resetTimer() // 기존 타이머 초기화 함수 호출
            resetBlinkCount() // 눈 깜빡임 카운트 초기화 함수 호출
        } // 리셋 버튼 클릭 리스너

        // 초기 상태: 앱이 처음 시작될 때는 오버레이 뷰를 표시하지 않음
        isViewFinderVisible = true
        overlayViewAttached = false
    }

    private fun resetBlinkCount() {
        if (::cameraService.isInitialized) {
            blinkDetectionUtil.resetBlinkCount() // blinkDetectionUtil에서 눈 깜빡임 카운트를 초기화
            updateBlinkUI() // UI 업데이트
        } else {
            Log.e(TAG, "CameraService is not initialized")
        }
    }

    // 타이머 관련 함수
    private fun startTimer() {
        if (blinkDetectionUtil.getStartTime() == 0L) {
            blinkDetectionUtil.setStartTime(System.currentTimeMillis())
        }

        if (blinkDetectionUtil.getPausedStartTime() != 0L) {
            blinkDetectionUtil.addPausedAccumulatedTime(System.currentTimeMillis() - blinkDetectionUtil.getPausedStartTime())
        }

        countDownTimer?.cancel()
        countDownTimer = object : CountDownTimer(timeLeftInMillis, 1000) { // blinkDetectionUtil.getTimeLeftInMillis() 대신 timeLeftInMillis 사용
            override fun onTick(millisUntilFinished: Long) {
                timeLeftInMillis = millisUntilFinished
                blinkDetectionUtil.setTimeLeftInMillis(millisUntilFinished)
                updateTimerText()
            }

            override fun onFinish() {
                blinkDetectionUtil.setTimerRunning(false)
                blinkDetectionUtil.setEndTime(System.currentTimeMillis())
                saveMeasurementData()

                // 타이머가 끝나면 TimerActivity로 이동(수정)
                val resultIntent = Intent(this@BlinkActivity, TimerActivity::class.java)
                resultIntent.putExtra("blinkCount", blinkDetectionUtil.getBlinkCount())
                resultIntent.putExtra("blinkRate", blinkDetectionUtil.getBlinkRate())
                resultIntent.putExtra("measurementTime", blinkDetectionUtil.getEndTime() - blinkDetectionUtil.getStartTime())
                startActivity(resultIntent)
                finish() // BlinkActivity 종료
            }
        }.start()

        blinkDetectionUtil.setTimerRunning(true)
    }

    private fun pauseTimer() {
        countDownTimer?.cancel() // 타이머를 취소
        blinkDetectionUtil.setTimerRunning(false) // 타이머 상태를 false로 설정
        blinkDetectionUtil.setPausedStartTime(System.currentTimeMillis()) // 일시정지 시간 설정
        landmarkDetectionManager.pauseDetection() // 얼굴 인식 일시정지
        blinkDetectionUtil.pauseBlinkDetection() // 깜빡임 감지 일시정지
        blinkDetectionUtil.pauseFpsCalculation() // FPS 계산 일시정지
    }

    private fun restartTimer() {
        blinkDetectionUtil.setTimerRunning(true) // 타이머 상태를 true로 설정
        if (blinkDetectionUtil.getPausedStartTime() != 0L) {
            blinkDetectionUtil.addPausedAccumulatedTime(System.currentTimeMillis() - blinkDetectionUtil.getPausedStartTime())
            blinkDetectionUtil.setPausedStartTime(0L) // 일시정지 시간 초기화
        }
        countDownTimer?.cancel() // 이전 타이머 취소
        countDownTimer = object : CountDownTimer(blinkDetectionUtil.getTimeLeftInMillis(), 1000) {
            override fun onTick(millisUntilFinished: Long) {
                blinkDetectionUtil.setTimeLeftInMillis(millisUntilFinished) // 남은 시간 설정
                updateTimerText() // 타이머 텍스트 업데이트
            }

            override fun onFinish() {
                blinkDetectionUtil.setTimerRunning(false)
                blinkDetectionUtil.setStartTime(System.currentTimeMillis()) // 종료 시간 설정
                saveMeasurementData() // 측정 데이터 저장
            }
        }.start()
        landmarkDetectionManager.resumeDetection() // 얼굴 인식 재개
        blinkDetectionUtil.resumeBlinkDetection() // 깜빡임 감지 재개
        blinkDetectionUtil.resumeFpsCalculation() // FPS 계산 재개
    }

    private fun resetTimer() {
        blinkDetectionUtil.setEndTime(System.currentTimeMillis())
        saveMeasurementData()

        countDownTimer?.cancel()
        blinkDetectionUtil.setTimeLeftInMillis(1200000)
        updateTimerText()
        blinkDetectionUtil.setTimerRunning(false)
        blinkDetectionUtil.setStartTime(0)
        blinkDetectionUtil.setEndTime(0)
        blinkDetectionUtil.setPausedStartTime(0)
        blinkDetectionUtil.setPausedAccumulatedTime(0)

        // TimerFragment로 돌아가도록 설정
        finish()
    }

    @SuppressLint("DefaultLocale")
    private fun updateTimerText() {
        val minutes = (blinkDetectionUtil.getTimeLeftInMillis() / 1000) / 60
        val seconds = (blinkDetectionUtil.getTimeLeftInMillis() / 1000) % 60
        timerTextView.text = String.format("%02d:%02d", minutes, seconds)
    }

    private fun saveMeasurementData() {
        if (!::cameraService.isInitialized) {
            Log.e(TAG, "CameraService is not initialized")
            return
        }

        val measurementTimeInSeconds = (blinkDetectionUtil.getEndTime() - blinkDetectionUtil.getStartTime() - blinkDetectionUtil.getPausedAccumulatedTime()) / 1000
        val measurementTimeInMinutes = measurementTimeInSeconds / 60.0
        val measurementTime = Timestamp(Date(blinkDetectionUtil.getStartTime()))
        val count = blinkDetectionUtil.getBlinkCount()

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
                    "birth_date" to birthDate, // 예시 생년월일
                    "tutorial" to false,
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
    @Suppress("UNUSED_PARAMETER") // image 파라미터를 현재 사용하지 않음을 컴파일러에 알림
    private fun handleFaceLandmarkerResult(result: FaceLandmarkerResult) { //, image: MPImage) {
        val isEyeClosed = landmarkDetectionManager.checkIfEyesClosed(result)
        blinkDetectionUtil.updateBlinkCount(isEyeClosed)
        blinkDetectionUtil.updateBlinkRate()
        updateBlinkUI()
        updateEyeStateUI(isEyeClosed)

        Log.d(TAG, "BlinkActivity: handleFaceLandmarkerResult called")
        if (result.faceLandmarks().isEmpty()) { // 감지된 얼굴이 없으면 함수 종료
            Log.d(TAG, "No face landmarks detected")
            return
        }

        // viewFinder가 초기화되지 않았을 경우 로그 출력 후 종료
        if (!this::viewFinder.isInitialized) {
            Log.e(TAG, "ViewFinder is not initialized")
            return
        }

        blinkDetectionUtil.updateFps() // FPS 업데이트

        updateFpsUI() // FPS UI 업데이트

        Log.d(TAG, "Current state - Blink count: ${blinkDetectionUtil.getBlinkCount()}, Blink rate: ${blinkDetectionUtil.getBlinkRate()}")
    }

    // 눈 깜빡임 카운트 증가 및 저장 함수 -> TimerFragment로 보내기 위함
    private fun updateBlinkCount() {
        blinkDetectionUtil.incrementBlinkCount() // 눈 깜빡임 횟수 증가
        saveBlinkCountToPreferences()
        updateBlinkUI()
    }

    private fun saveBlinkCountToPreferences() {
        if (this::cameraService.isInitialized) {
            val blinkCount = blinkDetectionUtil.getBlinkCount()
            val sharedPref = getSharedPreferences("MyApp", Context.MODE_PRIVATE)
            with(sharedPref.edit()) {
                putInt("blink_count", blinkCount)
                apply()
            }
        } else {
            Log.e("BlinkActivity", "CameraService is not initialized")
        }
    }

    // 깜빡임 카운트 UI 업데이트 함수 | 메인 UI 스레드에서 실행되어야 하므로 runOnUiThread를 사용
    @SuppressLint("SetTextI18n")
    private fun updateBlinkUI() {
        runOnUiThread {
            val blinkCount = blinkDetectionUtil.getBlinkCount()
            val blinkRate = blinkDetectionUtil.getBlinkRate()
            blinkCountTextView.text = "$blinkCount" // 총 깜빡임 횟수 표시
            blinkRateTextView.text = "%.2f bpm".format(blinkRate) // 분당 깜빡임 횟수 표시
//            blinkRateTextView.text = String.format("%.2f bpm", blinkRate)
            Log.d(TAG, "UI updated - Blink count: $blinkCount, Blink rate: $blinkRate")
        }
    }

    // FPS UI 업데이트 함수
    @SuppressLint("SetTextI18n")
    private fun updateFpsUI() {
        runOnUiThread {
            fpsTextView.text = "%.2f".format(blinkDetectionUtil.getFps()) // FPS 표시 업데이트
        }
    }

    // UI 업데이트 함수
    private fun updateUI(message: String, drawableResId: Int) {
        runOnUiThread {
            Log.d(TAG, "Updating UI: message=$message, drawableResId=$drawableResId")
            eyeStatusTextView.text = message
            eyeStatusImageView.setImageResource(drawableResId)
            val blinkCount = blinkDetectionUtil.getBlinkCount()
            blinkCountTextView.text = "$blinkCount"
            Log.d(TAG, "UI updated, Blink count: $blinkCount")
        }
    }

    private fun updateEyeStateUI(isEyeClosed: Boolean) {
        runOnUiThread {
            val eyeState = if (isEyeClosed) "Eye is closed" else "Eye is open"
            val drawableResId = if (isEyeClosed) R.drawable.eye_closed else R.drawable.eye_open
            eyeStatusTextView.text = eyeState
            eyeStatusImageView.setImageResource(drawableResId)
        }
    }

    // 모든 필요한 권한이 허용되었는지 확인하는 함수
    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        // REQUIRED_PERMISSIONS 배열의 모든 권한에 대해 확인
        // 모든 권한이 허용되었을 때만 true를 반환
        val isGranted = ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
        Log.d(TAG, "Permission $it is granted: $isGranted")
        isGranted
    }

    // 권한 요청 결과 처리 함수
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                Log.d(TAG, "All permissions granted in onRequestPermissionsResult")
                startCameraService()
            } else {
                Log.e(TAG, "Permissions not granted in onRequestPermissionsResult")
                Toast.makeText(this, "Permissions not granted by the user.", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }



    // 오버레이 권한 요청 함수
    private fun requestOverlayPermission() {
        if (!Settings.canDrawOverlays(this)) {
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
            overlayPermissionLauncher.launch(intent)
        } else {
            // 이미 권한이 있거나, API 23 이하에서는 권한이 필요하지 않음
            Log.d(TAG, "Overlay permission is already granted or not required.")
        }
    }



    private fun requestPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                CAMERA_PERMISSION_REQUEST_CODE
            )
        } else {
            startCameraService()
        }

        if (!allPermissionsGranted()) {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        } else {
            startCameraService()
            // 오버레이 권한 요청
            requestOverlayPermission()
        }
    }

    override fun onPause() {
        super.onPause()

        if (!isFinishing) {
            hideViewFinder() // viewFinder 숨기기
//            createOverlayView() // 오버레이 카메라 시작
            if (::cameraService.isInitialized) {
                cameraService.startBackgroundProcessing()
            }
        }
    }

    // 액티비티가 종료될 때 호출되는 함수
    override fun onDestroy() {
        super.onDestroy()

//        landmarkDetectionManager.onDestroy()

        if (serviceBound) {
            cameraService.stopCamera()  // 기존 카메라 세션 종료
            unbindService(connection)
            serviceBound = false
        }
    }

    // onResume에서 카메라 서비스가 바인딩되지 않았으면 바인딩을 시작하고, 바인딩 후 카메라 시작
    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume called")

        // viewFinder 표시
        showViewFinder()

//        if (serviceBound) {
        // 카메라 서비스가 초기화되었는지 확인
        if (::cameraService.isInitialized) {
            cameraService.switchToViewFinder(viewFinder)
        } else {
            startCameraService()
        }
    }

    // viewFinder 숨기기 및 활성화 상태 관리
    fun hideViewFinder() {
        runOnUiThread {
            viewFinder.visibility = View.GONE
        }
    }

    fun showViewFinder() {
        runOnUiThread {
            viewFinder.visibility = View.VISIBLE
        }
    }

    private fun startBackgroundCamera() {
        if (::cameraService.isInitialized) {
            cameraService.startBackgroundProcessing()
        }
    }

    // 서비스 시작 및 바인딩
    private fun startCameraService() {
        // 서비스 인텐트 생성
        val intent = Intent(this, CameraService::class.java)
        startForegroundService(intent)
        bindService(intent, connection, Context.BIND_AUTO_CREATE)
    }

    // 클래스 내부에서 사용할 상수들을 정의
    companion object {
        private const val TAG = "CameraXApp" // 로그 태그: 로그 메시지를 필터링하거나 식별하는 데 사용
        private const val BLINK_THRESHOLD = 0.25 // 눈 깜빡임 감지 임계값: 이 값보다 작으면 눈을 감은 것으로 판단
        private const val REQUEST_CODE_PERMISSIONS = 10 // 권한 요청 코드: onRequestPermissionsResult에서 이 요청을 식별하는 데 사용
        private const val CAMERA_PERMISSION_REQUEST_CODE = 1001
        private val REQUEST_CODE_POST_NOTIFICATIONS = 101 // 권한 요청 코드
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
        private const val REQUEST_CODE_OVERLAY_PERMISSION = 11 // 오버레이 권한 요청 코드 추가
    }
}