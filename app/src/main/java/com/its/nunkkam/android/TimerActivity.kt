package com.its.nunkkam.android

import android.content.Intent // 인텐트 사용을 위한 임포트
import android.os.Bundle // 번들 사용을 위한 임포트
import android.widget.TextView // 텍스트뷰 사용을 위한 임포트
import androidx.activity.ComponentActivity // 컴포넌트 액티비티 사용을 위한 임포트
import androidx.core.view.ViewCompat // 뷰 호환성을 위한 임포트
import androidx.core.view.WindowInsetsCompat // 윈도우 인셋 호환성을 위한 임포트
import android.widget.Button // 버튼 사용을 위한 임포트
import android.os.CountDownTimer // 카운트다운 타이머 사용을 위한 임포트
import android.util.Log // 로그 사용을 위한 임포트
import android.view.View // 뷰 사용을 위한 임포트
import com.google.firebase.Timestamp // 파이어베이스 타임스탬프 사용을 위한 임포트
import com.google.firebase.firestore.FieldValue // 파이어스토어 필드값 사용을 위한 임포트
import com.google.firebase.firestore.FirebaseFirestore // 파이어스토어 사용을 위한 임포트
import java.util.* // 자바 유틸 클래스 사용을 위한 임포트

class TimerActivity : ComponentActivity() {

    private lateinit var timerTextView: TextView // 타이머 표시 텍스트뷰
    private lateinit var startButton: Button // 시작 버튼
    private lateinit var pauseButton: Button // 일시정지 버튼
    private lateinit var resetButton: Button // 리셋 버튼
    private lateinit var resultButton: Button // 결과 버튼
    private lateinit var restartButton: Button // 재시작 버튼

    private var countDownTimer: CountDownTimer? = null // 카운트다운 타이머 객체
    private var timeLeftInMillis: Long = 1200000 // 남은 시간 (20분)
    private var timerRunning: Boolean = false // 타이머 실행 상태

    private var startTime: Long = 0 // 시작 시간
    private var endTime: Long = 0 // 종료 시간
    private var pausedTime: Long = 0 // 일시정지 시간

    private val db = FirebaseFirestore.getInstance() // 파이어스토어 인스턴스
    private val userId = "user1234" // 사용자 ID (실제 ID로 변경 필요)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_timer) // 레이아웃 설정

        // WindowInsets 처리
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.timer)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // 뷰 요소 초기화
        timerTextView = findViewById(R.id.timer_text)
        startButton = findViewById(R.id.start_button)
        pauseButton = findViewById(R.id.pause_button)
        resetButton = findViewById(R.id.reset_button)
        resultButton = findViewById(R.id.result_button)
        restartButton = findViewById(R.id.restart_button)

        startButton.setOnClickListener { // 시작 버튼 클릭 리스너
            startTimer()
        }

        pauseButton.setOnClickListener { // 일시정지 버튼 클릭 리스너
            pauseTimer()
        }

        resetButton.setOnClickListener { // 리셋 버튼 클릭 리스너
            Log.e("TimerActivity", "resetButton clicked")
            saveMeasurementData() // 측정 데이터 저장
            resetTimer()
        }

        restartButton.setOnClickListener { // 재시작 버튼 클릭 리스너
            startTimer()
        }

        resultButton.setOnClickListener { // 결과 버튼 클릭 리스너
            goToResultScreen()
        }
    }

    private fun startTimer() {
        if (startTime == 0L) { // 시작 시간 기록
            startTime = System.currentTimeMillis()
        }
        countDownTimer?.cancel() // 기존 타이머 취소
        countDownTimer = object : CountDownTimer(timeLeftInMillis, 1000) {
            override fun onTick(millisUntilFinished: Long) { // 매 초마다 실행
                timeLeftInMillis = millisUntilFinished
                updateTimerText()
            }

            override fun onFinish() { // 타이머 종료 시 실행
                timerRunning = false
                startButton.visibility = View.VISIBLE
                pauseButton.visibility = View.GONE
                resetButton.visibility = View.GONE
                restartButton.visibility = View.GONE
                endTime = System.currentTimeMillis()
                saveMeasurementData() // 측정 데이터 저장
            }
        }.start()

        timerRunning = true // 타이머 실행 상태 설정
        startButton.visibility = View.GONE // 버튼 가시성 조정
        pauseButton.visibility = View.VISIBLE
        resetButton.visibility = View.VISIBLE
        restartButton.visibility = View.GONE
    }

    private fun pauseTimer() {
        countDownTimer?.cancel() // 타이머 취소
        timerRunning = false // 타이머 상태 변경
        pausedTime = System.currentTimeMillis() // 일시정지 시간 기록
        startButton.visibility = View.GONE // 버튼 가시성 조정
        pauseButton.visibility = View.GONE
        resetButton.visibility = View.VISIBLE
        restartButton.visibility = View.VISIBLE
    }

    private fun resetTimer() {
        countDownTimer?.cancel() // 타이머 취소
        timeLeftInMillis = 1200000 // 시간 초기화 (20분)
        updateTimerText() // 타이머 텍스트 업데이트
        timerRunning = false // 타이머 상태 초기화
        startTime = 0 // 시간 변수 초기화
        endTime = System.currentTimeMillis()
        pausedTime = 0
        startButton.visibility = View.VISIBLE // 버튼 가시성 조정
        pauseButton.visibility = View.GONE
        resetButton.visibility = View.GONE
        restartButton.visibility = View.GONE
    }

    private fun updateTimerText() {
        val minutes = (timeLeftInMillis / 1000) / 60 // 분 계산
        val seconds = (timeLeftInMillis / 1000) % 60 // 초 계산
        val timeFormatted = String.format("00:%02d:%02d", minutes, seconds) // 시간 포맷팅
        timerTextView.text = timeFormatted // 타이머 텍스트 업데이트
    }

    private fun saveMeasurementData() {
        Log.e("TimerActivity", "saveMeasurementData1 called!") // 함수 진입 로그

        Log.e("TimerActivity", "saveMeasurementData2 called!") // 데이터 처리 시작 로그

        // 측정 시간 계산 (초 단위)
        val measurementTimeInSeconds = (endTime - startTime) / 1000
        Log.e("TimerActivity", endTime.toString())
        Log.e("TimerActivity", startTime.toString())
        // 측정 시간 계산 (분 단위)
        val measurementTimeInMinutes = measurementTimeInSeconds / 60.0

        // 측정 시작 시간을 Timestamp 객체로 변환
        val measurementTime = Timestamp(Date(startTime))
        // 측정 날짜를 LocalDate 객체로 변환 (시스템 기본 시간대 사용)
        val measurementDate = measurementTime.toDate().toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate()

        // 눈 깜빡임 데이터를 담을 HashMap 생성
        val blinkData = hashMapOf(
            "blink_id" to UUID.randomUUID().toString(), // 고유 ID 생성
            "frequency" to 200, // 눈 깜빡임 빈도 (임시값, 실제 계산 필요)
            "measurement_time" to measurementTime.toDate(), // 측정 시간
            "measurement_date" to measurementDate.toString(), // 측정 날짜 (문자열로 변환)
            "average_frequency_per_minute" to 20, // 분당 평균 빈도 (임시값, 실제 계산 필요)
            "measurement_time_minutes" to measurementTimeInMinutes // 측정 시간 (분 단위)
        )

        Log.e("TimerActivity", blinkData.toString()) // 생성된 데이터 로그 출력
        println("Saving data: $blinkData") // 저장할 데이터 콘솔 출력

        // Firestore의 'nunkkam' 컬렉션 내 'users' 문서 참조
        val userDocument = db.collection("nunkkam").document("users")

        // 문서 가져오기 시도
        userDocument.get().addOnSuccessListener { document ->
            if (document.exists()) { // 문서가 이미 존재하는 경우
                // 'blinks' 필드에 새로운 blinkData를 추가 (배열에 요소 추가)
                userDocument.update("blinks", FieldValue.arrayUnion(blinkData))
                    .addOnSuccessListener {
                        println("Data successfully written!") // 데이터 추가 성공 로그
                    }
                    .addOnFailureListener { e ->
                        Log.e("TimerActivity", "saveMeasurementData3 called!") // 데이터 추가 실패 로그
                    }
            } else { // 문서가 존재하지 않는 경우
                // 새로운 사용자 문서 생성을 위한 데이터
                val newUser = hashMapOf(
                    "guest_id" to userId, // 게스트 ID
                    "birth_date" to "1990-01-01T00:00:00Z", // 임시 생년월일
                    "tutorial" to true, // 튜토리얼 완료 여부
                    "blinks" to listOf(blinkData) // 눈 깜빡임 데이터 리스트
                )
                // 새 문서 생성 및 데이터 설정
                userDocument.set(newUser)
                    .addOnSuccessListener {
                        println("New user document successfully created!") // 새 문서 생성 성공 로그
                    }
                    .addOnFailureListener { e ->
                        Log.e("TimerActivity", "saveMeasurementData4 called!") // 새 문서 생성 실패 로그
                    }
            }
        }
    }

    private fun goToResultScreen() {
        val intent = Intent(this, CalendarActivity::class.java) // CalendarActivity로 이동하는 인텐트 생성
        startActivity(intent) // 액티비티 시작
    }
}