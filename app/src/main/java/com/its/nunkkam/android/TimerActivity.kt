package com.its.nunkkam.android // 패키지 선언: 이 코드가 속한 패키지를 지정

import android.os.Bundle
import android.widget.TextView
//import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.widget.ArrayAdapter
import android.os.CountDownTimer
import android.widget.Button
import android.widget.Switch
import android.widget.Spinner
import androidx.core.view.WindowInsetsCompat.Type.systemBars
import android.view.View
import androidx.activity.ComponentActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.Timestamp
import java.util.*

class TimerActivity : ComponentActivity() {

    private lateinit var timerTextView: TextView
    private lateinit var startButton: Button
    private lateinit var pauseButton: Button
    private lateinit var resetButton: Button
    private lateinit var resultButton: Button
    private lateinit var restartButton: Button

    private var countDownTimer: CountDownTimer? = null
    private var timeLeftInMillis: Long =1200000 // 20 minutes
    private var timerRunning: Boolean = false

    private var startTime: Long = 0
    private var endTime: Long = 0
    private var pausedTime: Long = 0


    private val db = FirebaseFirestore.getInstance()
    private val userId = "example_user_id" // 실제 사용자 ID로 변경 필요

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_timer)

        // WindowInsets 처리
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.timer)) { v, insets ->
            val systemBars = insets.getInsets(systemBars())
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

        startButton.setOnClickListener {
            startTimer()
        }

        pauseButton.setOnClickListener {
            pauseTimer()
        }

        resetButton.setOnClickListener {
            resetTimer()
        }

        restartButton.setOnClickListener {
            startTimer()
        }

        resultButton.setOnClickListener {
            // 결과 확인 로직 구현
        }
    }
//
    private fun initializeTimerLayout() { // 타이머 레이아웃 초기화 함수 -> 타이머 초기화면으로 사용
        // 타이머 레이아웃 설정
        timerTextView = findViewById(R.id.timer_text)
        pauseButton = findViewById(R.id.pause_button)
        restartButton = findViewById(R.id.restart_button)
        resetButton = findViewById(R.id.reset_button)
        resultButton = findViewById(R.id.result_button)

        pauseButton.setOnClickListener {
            pauseTimer()
            restartButton.visibility = View.VISIBLE
            pauseButton.visibility = View.GONE
            adjustConstraints()
        }

        restartButton.setOnClickListener {
            startTimer()
            restartButton.visibility = View.GONE
            pauseButton.visibility = View.VISIBLE
            adjustConstraints()
        }

        resetButton.setOnClickListener {
            resetTimer()
        }

        resultButton.setOnClickListener {
            // 결과 확인 로직 구현
        }
    }

    private fun adjustConstraints() { // Pause Button 실행 시 Reset Button 위치 조정 함수
        val constraintLayout = findViewById<ConstraintLayout>(R.id.timer)
        val constraintSet = ConstraintSet()
        constraintSet.clone(constraintLayout)

        if (restartButton.visibility == View.VISIBLE) {
            constraintSet.connect(R.id.reset_button, ConstraintSet.START, R.id.restart_button, ConstraintSet.END, 10)
        } else {
            constraintSet.connect(R.id.reset_button, ConstraintSet.START, R.id.pause_button, ConstraintSet.END, 10)
        }

        constraintSet.applyTo(constraintLayout)
    }

    private fun startTimer() {
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
            }
        }.start()

        timerRunning = true
        startButton.visibility = View.GONE
        pauseButton.visibility = View.VISIBLE
        resetButton.visibility = View.VISIBLE
        restartButton.visibility = View.GONE
    }

    private fun pauseTimer() {
        countDownTimer?.cancel()
        timerRunning = false
        startButton.visibility = View.GONE
        pauseButton.visibility = View.GONE
        resetButton.visibility = View.VISIBLE
        restartButton.visibility = View.VISIBLE
    }

    private fun resetTimer() {
        countDownTimer?.cancel()
        timeLeftInMillis = 1200000 // 10 minutes
        updateTimerText()
        timerRunning = false
        startButton.visibility = View.VISIBLE
        pauseButton.visibility = View.GONE
        resetButton.visibility = View.GONE
        restartButton.visibility = View.GONE
    }

    private fun updateTimerText() {
        val minutes = (timeLeftInMillis / 1000) / 60
        val seconds = (timeLeftInMillis / 1000) % 60
        val timeFormatted = String.format("00:%02d:%02d", minutes, seconds)
        timerTextView.text = timeFormatted
    }
}
