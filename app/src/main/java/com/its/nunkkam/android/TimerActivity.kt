package com.its.nunkkam.android // 패키지 선언: 이 코드가 속한 패키지를 지정

// 필요한 Android 및 Kotlin 라이브러리들을 import
import android.content.Intent
import android.os.Bundle // 액티비티 상태 저장 및 복원을 위한 클래스
import android.widget.TextView // 텍스트를 표시하는 UI 요소
import androidx.appcompat.app.AppCompatActivity // 앱 호환성을 위한 기본 액티비티 클래스
import androidx.core.view.ViewCompat // 뷰 호환성 지원을 위한 유틸리티 클래스
import androidx.core.view.WindowInsetsCompat // 윈도우 인셋 관련 호환성 클래스
import android.widget.Button // 버튼 UI 요소
import androidx.core.view.WindowInsetsCompat.Type.systemBars // 시스템 바 타입 정의
import android.view.View // 뷰의 기본 클래스
import androidx.activity.ComponentActivity // 컴포넌트 기반 액티비티 클래스
import androidx.constraintlayout.widget.ConstraintLayout // 제약 레이아웃
import androidx.constraintlayout.widget.ConstraintSet // 제약 레이아웃의 제약 조건 설정
import kotlinx.coroutines.CoroutineScope // 코루틴 스코프
import kotlinx.coroutines.Dispatchers // 디스패처 정의
import kotlinx.coroutines.Job // 코루틴 잡
import kotlinx.coroutines.delay // 딜레이 함수
import kotlinx.coroutines.launch // 코루틴 시작 함수

// TimerActivity 클래스 정의: 타이머 기능을 구현하는 주요 액티비티
class TimerActivity : ComponentActivity() {

    // UI 요소들을 위한 변수 선언
    private lateinit var timerTextView: TextView // 타이머 시간을 표시할 텍스트뷰
    private lateinit var startButton: Button // 타이머 시작 버튼
    private lateinit var pauseButton: Button // 타이머 일시정지 버튼
    private lateinit var resetButton: Button // 타이머 리셋 버튼
    private lateinit var resultButton: Button // 결과 확인 버튼
    private lateinit var restartButton: Button // 타이머 재시작 버튼

    // 타이머 관련 변수들
    private var timeLeftInMillis: Long = 1200000 // 남은 시간(밀리초): 초기값 20분
    private var timerRunning: Boolean = false // 타이머 실행 상태
    private var countDownTimerJob: Job? = null // 코루틴 잡 객체

    // 액티비티가 생성될 때 호출되는 메서드
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_timer) // 레이아웃 설정

        // WindowInsets 처리: 시스템 바(상태 바, 네비게이션 바 등)에 맞춰 패딩 조정
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.timer)) { v, insets ->
            val systemBars = insets.getInsets(systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // UI 요소들 초기화
        timerTextView = findViewById(R.id.timer_text) // 타이머 텍스트뷰 초기화
        startButton = findViewById(R.id.start_button) // 시작 버튼 초기화
        pauseButton = findViewById(R.id.pause_button) // 일시정지 버튼 초기화
        resetButton = findViewById(R.id.reset_button) // 리셋 버튼 초기화
        resultButton = findViewById(R.id.result_button) // 결과 버튼 초기화
        restartButton = findViewById(R.id.restart_button) // 재시작 버튼 초기화

        // 버튼 클릭 리스너 설정
        startButton.setOnClickListener {
            startTimer() // 타이머 시작 함수 호출
            startBlinkActivity()
        }

        pauseButton.setOnClickListener {
            pauseTimer() // 타이머 일시정지 함수 호출
        }

        resetButton.setOnClickListener {
            resetTimer() // 타이머 리셋 함수 호출
        }

        restartButton.setOnClickListener {
            startTimer() // 타이머 재시작(시작 함수와 동일)
        }

        resultButton.setOnClickListener {
            startResultActivity() // 결과 확인 화면 이동
        }
    }

    // BlinkActivity를 시작하는 함수
    private fun startBlinkActivity() {
        val intent = Intent(this, BlinkActivity::class.java) // BlinkActivity로 이동하기 위한 Intent 생성
        startActivity(intent) // BlinkActivity 시작
    }

    // 결과 액티비티를 시작하는 함수
    private fun startResultActivity() {
        val intent = Intent(this, ResultActivity::class.java) // ResultActivity로 이동하기 위한 Intent 생성
        startActivity(intent) // ResultActivity 시작
    }

    // 타이머 레이아웃 초기화 함수: 타이머 초기 화면 설정
    private fun initializeTimerLayout() {
        // UI 요소들 재초기화
        timerTextView = findViewById(R.id.timer_text) // 타이머 텍스트뷰 재초기화
        pauseButton = findViewById(R.id.pause_button) // 일시정지 버튼 재초기화
        restartButton = findViewById(R.id.restart_button) // 재시작 버튼 재초기화
        resetButton = findViewById(R.id.reset_button) // 리셋 버튼 재초기화
        resultButton = findViewById(R.id.result_button) // 결과 버튼 재초기화

        // 버튼 클릭 리스너 재설정
        pauseButton.setOnClickListener {
            pauseTimer() // 타이머 일시정지
            restartButton.visibility = View.VISIBLE // 재시작 버튼 표시
            pauseButton.visibility = View.GONE // 일시정지 버튼 숨김
            adjustConstraints() // 제약 조건 조정
        }

        restartButton.setOnClickListener {
            startTimer() // 타이머 시작
            restartButton.visibility = View.GONE // 재시작 버튼 숨김
            pauseButton.visibility = View.VISIBLE // 일시정지 버튼 표시
            adjustConstraints() // 제약 조건 조정
        }

        resetButton.setOnClickListener {
            resetTimer() // 타이머 리셋
        }

        resultButton.setOnClickListener {
            startResultActivity() // 결과 확인 화면 이동
        }
    }

    // Pause Button 실행 시 Reset Button 위치 조정 함수
    private fun adjustConstraints() {
        val constraintLayout = findViewById<ConstraintLayout>(R.id.timer) // 제약 레이아웃 찾기
        val constraintSet = ConstraintSet() // 새로운 제약 세트 생성
        constraintSet.clone(constraintLayout) // 현재 레이아웃의 제약 조건 복제

        if (restartButton.visibility == View.VISIBLE) {
            // 재시작 버튼이 보이면 리셋 버튼을 재시작 버튼 오른쪽에 배치
            constraintSet.connect(R.id.reset_button, ConstraintSet.START, R.id.restart_button, ConstraintSet.END, 10)
        } else {
            // 일시정지 버튼이 보이면 리셋 버튼을 일시정지 버튼 오른쪽에 배치
            constraintSet.connect(R.id.reset_button, ConstraintSet.START, R.id.pause_button, ConstraintSet.END, 10)
        }

        constraintSet.applyTo(constraintLayout) // 변경된 제약 조건 적용
    }

    // 타이머 시작 함수
    private fun startTimer() {
        countDownTimerJob?.cancel() // 기존 타이머가 있다면 취소
        countDownTimerJob = CoroutineScope(Dispatchers.Main).launch {
            timerRunning = true // 타이머 실행 중 상태로 설정
            startButton.visibility = View.GONE // 시작 버튼 숨김
            pauseButton.visibility = View.VISIBLE // 일시정지 버튼 보임
            resetButton.visibility = View.VISIBLE // 리셋 버튼 보임
            restartButton.visibility = View.GONE // 재시작 버튼 숨김

            while (timeLeftInMillis > 0 && timerRunning) {
                delay(1000) // 1초 대기
                timeLeftInMillis -= 1000 // 남은 시간 1초 감소
                updateTimerText() // 타이머 텍스트 업데이트
            }

            if (timeLeftInMillis <= 0) {
                onTimerFinish() // 타이머 종료 시 작업 호출
            }
        }
    }

    // 타이머 일시정지 함수
    private fun pauseTimer() {
        countDownTimerJob?.cancel() // 타이머 취소
        timerRunning = false // 타이머 중지 상태로 설정
        startButton.visibility = View.GONE // 시작 버튼 숨김
        pauseButton.visibility = View.GONE // 일시정지 버튼 숨김
        resetButton.visibility = View.VISIBLE // 리셋 버튼 보임
        restartButton.visibility = View.VISIBLE // 재시작 버튼 보임
    }

    // 타이머 리셋 함수
    private fun resetTimer() {
        countDownTimerJob?.cancel() // 타이머 취소
        timeLeftInMillis = 1200000 // 20분으로 시간 리셋
        updateTimerText() // 타이머 텍스트 업데이트
        timerRunning = false // 타이머 중지 상태로 설정
        startButton.visibility = View.VISIBLE // 시작 버튼 보임
        pauseButton.visibility = View.GONE // 일시정지 버튼 숨김
        resetButton.visibility = View.GONE // 리셋 버튼 숨김
        restartButton.visibility = View.GONE // 재시작 버튼 숨김
    }

    // 타이머 텍스트 업데이트 함수
    private fun updateTimerText() {
        val minutes = (timeLeftInMillis / 1000) / 60 // 분 계산
        val seconds = (timeLeftInMillis / 1000) % 60 // 초 계산
        val timeFormatted = String.format("00:%02d:%02d", minutes, seconds) // 시간 형식 지정
        timerTextView.text = timeFormatted // 타이머 텍스트뷰 업데이트
    }

    // 타이머 종료 시 호출되는 함수
    private fun onTimerFinish() {
        timerRunning = false // 타이머 중지 상태로 설정
        startButton.visibility = View.VISIBLE // 시작 버튼 보임
        pauseButton.visibility = View.GONE // 일시정지 버튼 숨김
        resetButton.visibility = View.GONE // 리셋 버튼 숨김
        restartButton.visibility = View.GONE // 재시작 버튼 숨김
    }
}
