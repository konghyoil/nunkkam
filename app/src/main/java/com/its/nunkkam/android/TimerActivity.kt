package com.its.nunkkam.android // 패키지 선언: 이 코드가 속한 패키지를 지정

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

// TimerActivity 클래스 정의: 타이머 기능을 구현하는 주요 액티비티
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
    private var pausedStartTime: Long = 0 // 일시정지 시작 시간
    private var pausedAccumulatedTime: Long = 0 // 누적 일시정지 시간
    private val db = FirebaseFirestore.getInstance() // 파이어스토어 인스턴스
    private val userId = "user1234" // 사용자 ID (실제 ID로 변경 필요) //Device ID 활용 방안 연구

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
//            saveMeasurementData() // 측정 데이터 저장
            resetTimer()
        }

        restartButton.setOnClickListener { // 재시작 버튼 클릭 리스너
            restartTimer()
        }

        resultButton.setOnClickListener { // 결과 버튼 클릭 리스너
            goToResultScreen()
        }
    }

    private fun startTimer() {
        if (startTime == 0L) { // 시작 시간 기록
            startTime = System.currentTimeMillis()
        }
        if (pausedStartTime != 0L) { // 일시정지 시간이 기록된 경우
            pausedAccumulatedTime += System.currentTimeMillis() - pausedStartTime
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
                Log.e("TimerActivity", "Finish endTime: $endTime")
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
        pausedStartTime = System.currentTimeMillis() // 일시정지 시작 시간 기록
        startButton.visibility = View.GONE // 버튼 가시성 조정
        pauseButton.visibility = View.GONE
        resetButton.visibility = View.VISIBLE
        restartButton.visibility = View.VISIBLE
    }

    private fun restartTimer() {
        timerRunning = true // 타이머 상태 변경
        if (pausedStartTime != 0L) { // 일시정지 시간이 기록된 경우
            pausedAccumulatedTime += System.currentTimeMillis() - pausedStartTime
            pausedStartTime = 0L // 일시정지 시작 시간 초기화
        }
        countDownTimer?.cancel()
        countDownTimer = object : CountDownTimer(timeLeftInMillis, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                timeLeftInMillis = millisUntilFinished
                updateTimerText()
            }

            override fun onFinish() {
                timerRunning = false
                startButton.visibility = View.VISIBLE
                pauseButton.visibility = View.GONE
                resetButton.visibility = View.GONE
                restartButton.visibility = View.GONE
                endTime = System.currentTimeMillis()
                Log.e("TimerActivity", "Finish endTime: $endTime")

                saveMeasurementData()
            }
        }.start()

        startButton.visibility = View.GONE
        pauseButton.visibility = View.VISIBLE
        resetButton.visibility = View.VISIBLE
        restartButton.visibility = View.GONE
    }


    private fun resetTimer() {
        Log.e("TimerActivity", "resetTimer start startTime: $startTime")
        Log.e("TimerActivity", "resetTimer start endTime: $endTime")
        Log.e("TimerActivity", "resetTimer start pausedAccumulatedTime: $pausedAccumulatedTime")
        endTime = System.currentTimeMillis()
        saveMeasurementData()

        countDownTimer?.cancel() // 타이머 취소
        timeLeftInMillis = 1200000 // 시간 초기화 (20분)
        updateTimerText() // 타이머 텍스트 업데이트
        timerRunning = false // 타이머 상태 초기화
        startTime = 0 // 시작 시간 초기화
        endTime = 0 // 종료 시간 초기화
        pausedStartTime = 0 // 일시정지 시작 시간 초기화
        pausedAccumulatedTime = 0 // 누적 일시정지 시간 초기화
        startButton.visibility = View.VISIBLE // 버튼 가시성 조정
        pauseButton.visibility = View.GONE
        resetButton.visibility = View.GONE
        restartButton.visibility = View.GONE
        Log.e("TimerActivity", "resetTimer end startTime: $startTime")
        Log.e("TimerActivity", "resetTimer end endTime: $endTime")
        Log.e("TimerActivity", "resetTimer end pausedAccumulatedTime: $pausedAccumulatedTime")

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
        val measurementTimeInSeconds = (endTime - startTime - pausedAccumulatedTime) / 1000
        Log.e("TimerActivity", "measurement endTime: $endTime")
        Log.e("TimerActivity", "measurement startTime: $startTime")
        Log.e("TimerActivity", "measurement pausedAccumulatedTime: $pausedAccumulatedTime")
        // 측정 시간 계산 (분 단위)
        val measurementTimeInMinutes = measurementTimeInSeconds / 60.0

        // 측정 시작 시간을 Timestamp 객체로 변환
        val measurementTime = Timestamp(Date(startTime))
//        // 측정 날짜를 LocalDate 객체로 변환 (시스템 기본 시간대 사용)
//        val measurementDate = measurementTime.toDate().toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate()

        // 임의 값(나중에 실제 계산 필요)
        val count = 2
        val birthDate = Timestamp(seconds = 631173525, nanoseconds = 863000000)
        val exMeasurementTime = Timestamp(seconds = 1718071582, nanoseconds = 863000000)


        // 눈 깜빡임 데이터를 담을 HashMap 생성
        val blinkData = hashMapOf(
            "count" to count, // 눈 깜빡임 빈도 (임시값, 실제 계산 필요)
            "measurement_time" to measurementTimeInMinutes, // 측정 시간
            "measurement_date" to exMeasurementTime, // 측정 날짜 Timestamp 객체
            "average_frequency_per_minute" to count / measurementTimeInMinutes, // 분당 평균 빈도 (임시값, 실제 계산 필요)
        )

        Log.e("TimerActivity", blinkData.toString()) // 생성된 데이터 로그 출력
        println("Saving data: $blinkData") // 저장할 데이터 콘솔 출력

        // Firestore의 'nunkkam' 컬렉션 내 'users' 문서 참조
        val userDocument = db.collection("USERS").document(userId)

        // 문서 가져오기 시도
        userDocument.get().addOnSuccessListener { document ->
            if (document.exists()) { // 문서가 이미 존재하는 경우
                // 'blinks' 필드에 새로운 blinkData를 추가 (배열에 요소 추가)
                userDocument.update("blinks", FieldValue.arrayUnion(blinkData))
                    .addOnSuccessListener {
                        Log.e("TimerActivity", "Data successfully written!") // 데이터 추가 성공 로그
                    }
                    .addOnFailureListener { e ->
                        Log.e("TimerActivity", "saveMeasurementData3 called!") // 데이터 추가 실패 로그
                    }
            } else { // 문서가 존재하지 않는 경우
                // 새로운 사용자 문서 생성을 위한 데이터
                val newUser = hashMapOf(
                    "birth_date" to birthDate, // 임시 생년월일
                    "tutorial" to true, // 튜토리얼 완료 여부
                    "blinks" to listOf(blinkData) // 눈 깜빡임 데이터 리스트
                )
                // 새 문서 생성 및 데이터 설정
                userDocument.set(newUser)
                    .addOnSuccessListener {
                        Log.e(
                            "TimerActivity",
                            "New user document successfully created!"
                        ) // 새 문서 생성 성공 로그
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
