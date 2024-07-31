package com.its.nunkkam.android // 패키지 선언: 이 코드가 속한 패키지를 지정

// 필요한 라이브러리들을 가져오기
import android.Manifest // 안드로이드 권한 관련 클래스
import android.annotation.SuppressLint // 특정 lint 경고를 억제하기 위한 어노테이션
import android.app.AlarmManager // 알람 관리자 클래스
import android.app.PendingIntent // 지연된 인텐트를 위한 클래스
import android.content.ComponentName // 컴포넌트 이름을 나타내는 클래스
import android.content.Context // 애플리케이션 환경에 대한 정보를 제공하는 클래스
import android.content.Intent // 컴포넌트 간 통신을 위한 메시지 객체
import android.content.ServiceConnection // 서비스 연결을 위한 인터페이스
import android.content.pm.PackageManager // 패키지 관리자 클래스
import android.os.Build // 빌드 버전 정보를 제공하는 클래스
import android.os.Bundle // 키-값 쌍의 데이터를 저장하는 클래스
import android.os.CountDownTimer // 카운트다운 타이머 클래스
import android.os.Handler // 메시지와 Runnable 객체를 처리하는 클래스
import android.os.IBinder // 서비스와 통신하기 위한 인터페이스
import android.os.Looper // 메시지 루프를 관리하는 클래스
import android.util.Log // 로그 출력을 위한 클래스
import android.widget.Button // 버튼 위젯
import android.widget.ImageView // 이미지 뷰 위젯
import android.widget.TextView // 텍스트 뷰 위젯
import android.widget.Toast // 짧은 메시지를 화면에 표시하는 클래스
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
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark // 정규화된 랜드마크 클래스
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarkerResult // 얼굴 랜드마크 결과 클래스
import java.util.Date // 날짜 클래스
import kotlin.math.abs // 절대값을 계산하는 수학 함수

// 주석 규칙 | [외부]: 외부 데이터에서 가저오는 부분을 구분하기 위한 주석

// BlinkActivity 클래스 정의: 앱의 눈 깜빡임 화면을 담당
class BlinkActivity : AppCompatActivity() {

    // 클래스 내부에서 사용할 변수들을 선언
    private lateinit var viewFinder: PreviewView // 카메라 미리보기 뷰
    private lateinit var eyeStatusImageView: ImageView // 눈 상태를 표시할 이미지 뷰
    private lateinit var eyeStatusTextView: TextView // 눈 상태를 표시할 텍스트 뷰
    private var lastEyeState = true // 마지막으로 감지된 눈 상태 (true: 눈 뜸, false: 눈 감음)
    private lateinit var blinkCountTextView: TextView // 눈 깜빡임 횟수를 표시할 TextView
    private var lastBlinkTime = System.currentTimeMillis() // 마지막 눈 깜빡임이 감지된 시간
    private var blinkRate = 0.0 // 분당 눈 깜빡임 횟수 (blinks per minute)
    private lateinit var blinkRateTextView: TextView // 분당 눈 깜빡임 횟수를 표시할 TextView
    private var frameCounter = 0 // 프레임 카운터
    private var lastFpsUpdateTime = System.currentTimeMillis() // 마지막 FPS 업데이트 시간
    private var fps = 0f // 현재 FPS
    private lateinit var fpsTextView: TextView // FPS를 표시할 TextView

    // 타이머
    private lateinit var timerTextView: TextView // 타이머 텍스트 뷰
    private lateinit var pauseButton: Button // 일시정지 버튼
    private lateinit var restartButton: Button // 재시작 버튼
    private lateinit var resetButton: Button // 리셋 버튼

    private var userId: String = "" // 사용자 ID
    private var birthDate: Timestamp? = null // 사용자 생년월일
    private var isGoogleLogin: Boolean = false // Google 로그인 여부

    private var countDownTimer: CountDownTimer? = null // 카운트다운 타이머
    private var timeLeftInMillis: Long = 1200000 // 남은 시간 (밀리초)
    private var timerRunning: Boolean = false // 타이머 실행 중 여부
    private var startTime: Long = 0 // 시작 시간
    private var endTime: Long = 0 // 종료 시간
    private var pausedStartTime: Long = 0 // 일시정지 시작 시간
    private var pausedAccumulatedTime: Long = 0 // 누적된 일시정지 시간
    private val db = FirebaseFirestore.getInstance() // Firestore 데이터베이스 인스턴스

    // 카메라 및 포그라운드/백그라운드 구분
    private lateinit var cameraService: CameraService // 카메라 서비스
    private var serviceBound = false // 서비스 바인딩 여부

    // 알람
    private lateinit var alarmManager: AlarmManager // 알람 관리자
    private lateinit var alarmIntent: PendingIntent // 알람 인텐트

    // ServiceConnection 객체: 서비스와의 연결을 관리
    private val connection = object : ServiceConnection {
        // 서비스 연결 시 호출되는 메서드
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            Log.d(TAG, "Service connected") // 서비스 연결 로그
            val binder = service as CameraService.LocalBinder // 서비스 바인더 획득
            cameraService = binder.getService() // 서비스 인스턴스 획득
            cameraService.setCallback(cameraCallback) // 콜백 설정
            serviceBound = true // 서비스 바인딩 상태 설정

            Log.d(TAG, "Starting camera from onServiceConnected") // 카메라 시작 로그
            cameraService.startCamera(viewFinder) // 카메라 시작
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
            handleFaceLandmarkerResult(result, image) // 결과 처리
        }
    }

    // 초기 UI 설정 메서드
    private fun initViews() {
        // XML에서 정의한 뷰들을 찾아 변수에 할당
        viewFinder = findViewById(R.id.viewFinder) // 카메라 미리보기 뷰
        eyeStatusImageView = findViewById(R.id.eyeStatusImageView) // 눈 상태 이미지 뷰
        eyeStatusTextView = findViewById(R.id.textViewEyeStatus) // 눈 상태 텍스트 뷰
        fpsTextView = findViewById(R.id.fpsTextView) // FPS 표시 텍스트 뷰

        // 깜빡임 카운트 표시 관련 TextView 초기화 및 UI 업데이트
        blinkCountTextView = findViewById(R.id.blinkCountTextView) // 깜빡임 횟수 텍스트 뷰
        blinkRateTextView = findViewById(R.id.blinkRateTextView) // 깜빡임 빈도 텍스트 뷰

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

        // 알람
        alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager // 알람 매니저 초기화
    }

    // 액티비티가 생성될 때 호출되는 함수
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState) // 부모 클래스의 onCreate 메서드 호출
        setContentView(R.layout.activity_blink) // 레이아웃 XML 파일을 가져와 화면에 설정

        Log.d(TAG, "BlinkActivity onCreate called") // 액티비티 생성 로그

        initViews() // 초기 UI 및 뷰 설정

        // 권한 확인 및 요청
        if (!allPermissionsGranted()) {
            Log.d(TAG, "Requesting permissions") // 권한 요청 로그
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS) // 권한 요청
        } else {
            Log.d(TAG, "All permissions granted") // 권한 허용 로그
            startCameraService() // 카메라 서비스 시작
        }

        updateBlinkUI() // UI 업데이트

        startTimer() // 타이머 시작

        pauseButton.setOnClickListener { pauseTimer() } // 일시정지 버튼 클릭 리스너
        restartButton.setOnClickListener { restartTimer() } // 재시작 버튼 클릭 리스너
        resetButton.setOnClickListener {
            resetTimer() // 기존 타이머 초기화 함수 호출
            resetBlinkCount() // 눈 깜빡임 카운트 초기화 함수 호출
        } // 리셋 버튼 클릭 리스너

        setupAlarm() // 알람 설정
    }

    // 서비스 시작 및 바인딩
    private fun startCameraService() {
        Log.d(TAG, "Starting CameraService") // 서비스 시작 로그
        val intent = Intent(this, CameraService::class.java) // 서비스 인텐트 생성
        startService(intent) // 서비스 시작
        Log.d(TAG, "CameraService started, waiting before binding") // 바인딩 대기 로그
        Handler(Looper.getMainLooper()).postDelayed({
            bindToService(intent) // 서비스 바인딩
        }, 1000) // 1초 대기 후 바인딩
    }

    private var bindingAttempts = 0
    private val MAX_BINDING_ATTEMPTS = 3

    private fun bindToService(intent: Intent) {
        if (bindingAttempts < MAX_BINDING_ATTEMPTS) {
            Log.d(TAG, "Binding to CameraService, attempt ${bindingAttempts + 1}")
            try {
                val bound = bindService(intent, connection, Context.BIND_AUTO_CREATE)
                if (!bound) {
                    Log.e(TAG, "Failed to bind to CameraService, bindService returned false")
                    bindingAttempts++
                    Handler(Looper.getMainLooper()).postDelayed({
                        bindToService(intent)
                    }, 2000)
                } else {
                    Log.d(TAG, "Successfully bound to CameraService")
                    bindingAttempts = 0
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception while binding to CameraService", e)
                bindingAttempts++
                Handler(Looper.getMainLooper()).postDelayed({
                    bindToService(intent)
                }, 2000)
            }
        } else {
            Log.e(TAG, "Max binding attempts reached")
            Toast.makeText(this, "Failed to start camera service", Toast.LENGTH_LONG).show()
        }
    }

    private fun resetBlinkCount() {
        if (::cameraService.isInitialized) {
            cameraService.resetBlinkCount() // CameraService에서 눈 깜빡임 카운트를 초기화
            updateBlinkUI() // UI 업데이트
        } else {
            Log.e(TAG, "CameraService is not initialized")
        }
    }

    //홍철 타이머 관련 함수
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
        if (!::cameraService.isInitialized) {
            Log.e(TAG, "CameraService is not initialized")
            return
        }

        val measurementTimeInSeconds = (endTime - startTime - pausedAccumulatedTime) / 1000
        val measurementTimeInMinutes = measurementTimeInSeconds / 60.0
        val measurementTime = Timestamp(Date(startTime))
        val count = cameraService.getBlinkCount()

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
    private fun handleFaceLandmarkerResult(result: FaceLandmarkerResult, image: MPImage) {
        if (result.faceLandmarks().isEmpty()) {
            Log.d(TAG, "No face landmarks detected")
            return
        }

        Log.d(TAG, "BlinkActivity: handleFaceLandmarkerResult called")

        // viewFinder가 초기화되지 않았을 경우 로그 출력 후 종료
        if (!this::viewFinder.isInitialized) {
            Log.e(TAG, "ViewFinder is not initialized")
            return
        }

        frameCounter++ // 프레임 카운터 증가

        // FPS 계산
        val currentTime = System.currentTimeMillis() // 현재 시간 기록
        if (currentTime - lastFpsUpdateTime >= 1000) { // 1초마다 FPS 업데이트
            fps = frameCounter * 1000f / (currentTime - lastFpsUpdateTime) // FPS 계산
            frameCounter = 0 // 프레임 카운터 초기화
            lastFpsUpdateTime = currentTime // 마지막 FPS 업데이트 시간 갱신
            updateFpsUI() // FPS UI 업데이트
        }

        if (result.faceLandmarks().isEmpty()) return // 감지된 얼굴이 없으면 함수 종료

        val landmarks = result.faceLandmarks()[0] // 첫 번째 감지된 얼굴의 랜드마크

        // 눈 랜드마크 좌표 가져오기
        val leftEyeTop = landmarks[159]     // 159: 왼쪽 눈 위쪽 점
        val leftEyeBottom = landmarks[145]  // 145: 왼쪽 눈 아래쪽 점
        val leftEyeInner = landmarks[33]    // 33: 왼쪽 눈 안쪽 점
        val leftEyeOuter = landmarks[133]   // 133: 왼쪽 눈 바깥쪽 점
        val rightEyeTop = landmarks[386]    // 386: 오른쪽 눈 위쪽 점
        val rightEyeBottom = landmarks[374] // 374: 오른쪽 눈 아래쪽 점
        val rightEyeInner = landmarks[263]  // 263: 오른쪽 눈 안쪽 점
        val rightEyeOuter = landmarks[362]  // 362: 오른쪽 눈 바깥쪽 점

        // 눈 랜드마크 좌표 로깅 추가
        Log.d("[8]Landmark" +
                "q  ", "Landmark coordinates: " +
                "Left Eye Top (159): $leftEyeTop, " +
                "Left Eye Bottom (145): $leftEyeBottom, " +
                "Left Eye Inner (33): $leftEyeInner, " +
                "Left Eye Outer (133): $leftEyeOuter, " +
                "Right Eye Top (386): $rightEyeTop, " +
                "Right Eye Bottom (374): $rightEyeBottom, " +
                "Right Eye Inner (263): $rightEyeInner, " +
                "Right Eye Outer (362): $rightEyeOuter")

        // 눈 개폐 정도 계산
        fun calculateEyeOpenness(top: NormalizedLandmark,
                                 bottom: NormalizedLandmark,
                                 inner: NormalizedLandmark,
                                 outer: NormalizedLandmark
        ): Float {
            val verticalDistance = abs(top.y() - bottom.y()) // 눈의 세로 길이 계산
            val horizontalDistance = abs(outer.x() - inner.x()) // 눈의 가로 길이 계산
            return verticalDistance / (horizontalDistance + 1e-6f) // 세로/가로 비율 반환 (눈 개폐 정도) | 0으로 나누는 것을 방지
        }

        // 왼쪽, 오른쪽 눈의 개폐 정도 계산
        val leftEyeOpenness = calculateEyeOpenness(leftEyeTop, leftEyeBottom, leftEyeInner, leftEyeOuter)
        val rightEyeOpenness = calculateEyeOpenness(rightEyeTop, rightEyeBottom, rightEyeInner, rightEyeOuter)
        val averageEyeOpenness = (leftEyeOpenness + rightEyeOpenness) / 2 // 양쪽 눈의 평균 개폐 정도

        Log.d("FaceLandmarks", "Total landmarks detected: ${landmarks.size}, FPS: $fps")
        Log.d("EyeOpenness", "FPS: $fps, Left Eye: $leftEyeOpenness, Right Eye: $rightEyeOpenness, Average: $averageEyeOpenness")

        val eyesOpen = averageEyeOpenness >= BLINK_THRESHOLD // 눈이 열려있는지 여부 판단
        Log.d(TAG, "Eyes open: $eyesOpen, Average openness: $averageEyeOpenness")

        // 눈 깜빡임 감지 및 UI 업데이트 로직
        if (eyesOpen != lastEyeState && !eyesOpen) {
            Log.d(TAG, "Blink detected")
            cameraService.incrementBlinkCount() // 눈 깜빡임 횟수 증가
            val blinkCount = cameraService.getBlinkCount()
            Log.d(TAG, "Total blinks: $blinkCount")

            val timeDiff = (currentTime - lastBlinkTime) / 1000.0 // 마지막 깜빡임과의 시간 차이를 초 단위로 계산
            blinkRate = 60.0 / timeDiff // 분당 깜빡임 횟수 계산 (60초 / 깜빡임 간격)
            lastBlinkTime = currentTime // 마지막 깜빡임 시간 업데이트
            updateBlinkUI() // UI 업데이트 함수 호출
        }
        lastEyeState = eyesOpen // 현재 눈 상태를 마지막 상태로 저장

        // 눈 상태에 따라 UI 업데이트
        if (!eyesOpen) {
            updateUI("Eye is closed", R.drawable.eye_closed)    // [외부] drawable/eye_closed.png 이미지 리소스 가져오기
        } else {
            updateUI("Eye is open", R.drawable.eye_open)        // [외부] drawable/eye_open.png 이미지 리소스 가져오기
        }
    }

    // 눈 깜빡임 카운트 증가 및 저장 함수 -> TimerFragment로 보내기 위함
    private fun updateBlinkCount() {
        cameraService.incrementBlinkCount() // 눈 깜빡임 횟수 증가
        saveBlinkCountToPreferences()
        updateBlinkUI()
    }

    private fun saveBlinkCountToPreferences() {
        if (this::cameraService.isInitialized) {
            val blinkCount = cameraService.getBlinkCount()
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
            if (::cameraService.isInitialized) {
                val blinkCount = cameraService.getBlinkCount()
                blinkCountTextView.text = "$blinkCount" // 총 깜빡임 횟수 표시
                blinkRateTextView.text = "%.2f bpm".format(blinkRate) // 분당 깜빡임 횟수 표시
            } else {
                Log.e(TAG, "CameraService is not initialized")
            }
        }
    }

    // FPS UI 업데이트 함수
    @SuppressLint("SetTextI18n")
    private fun updateFpsUI() {
        runOnUiThread {
            fpsTextView.text = "%.2f".format(fps) // FPS 표시 업데이트
        }
    }

    // UI 업데이트 함수
    private fun updateUI(message: String, drawableResId: Int) {
        runOnUiThread {
            Log.d(TAG, "Updating UI: message=$message, drawableResId=$drawableResId")
            eyeStatusTextView.text = message
            eyeStatusImageView.setImageResource(drawableResId)
            val blinkCount = cameraService.getBlinkCount()
            blinkCountTextView.text = "$blinkCount"
            Log.d(TAG, "UI updated, Blink count: $blinkCount")
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

    private fun setupAlarm() {
        alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, AlarmReceiver::class.java)
        alarmIntent = PendingIntent.getBroadcast(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 분마다 알람 설정
        val intervalMillis = 1 * 60 * 1000L // 1분
        val triggerTime = System.currentTimeMillis() + intervalMillis

        if (alarmManager.canScheduleExactAlarms()) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerTime,
                alarmIntent
            )
        } else {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerTime,
                alarmIntent
            )
        }
    }

    // 액티비티가 종료될 때 호출되는 함수
    override fun onDestroy() {
        super.onDestroy()
        // 앱이 종료될 때 알람 취소
        alarmManager.cancel(alarmIntent)
        if (serviceBound) {
            unbindService(connection)
            serviceBound = false
        }
    }

    override fun onPause() {
        super.onPause()
        if (serviceBound) {
            // 서비스를 언바인딩 하지 않음
            cameraService.stopCamera()  // 필요시 카메라 세션 종료
        }
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume called")
        if (!serviceBound) {
            Log.d(TAG, "Service not bound in onResume, starting CameraService")
            startCameraService()
        } else if (::cameraService.isInitialized) {
            Log.d(TAG, "Starting camera from onResume")
            Handler(Looper.getMainLooper()).post {
                cameraService.startCamera(viewFinder)
            }
        } else {
            Log.e(TAG, "CameraService not initialized in onResume")
        }
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
    }
}