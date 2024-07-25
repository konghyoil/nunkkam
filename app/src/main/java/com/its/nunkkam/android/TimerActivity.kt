package com.its.nunkkam.android

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.widget.Button
import android.os.CountDownTimer
import android.util.Log
import android.view.View
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*

class TimerActivity : ComponentActivity() {

    private lateinit var timerTextView: TextView
    private lateinit var startButton: Button
    private lateinit var pauseButton: Button
    private lateinit var resetButton: Button
    private lateinit var resultButton: Button
    private lateinit var restartButton: Button

    private var countDownTimer: CountDownTimer? = null
    private var timeLeftInMillis: Long = 1200000 // 20 minutes
    private var timerRunning: Boolean = false

    private var startTime: Long = 0
    private var endTime: Long = 0
    private var pausedTime: Long = 0

    private val db = FirebaseFirestore.getInstance()
    private val userId = "user1234" // 실제 사용자 ID로 변경 필요

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_timer)

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

        startButton.setOnClickListener {
            startTimer()
        }

        pauseButton.setOnClickListener {
            pauseTimer()
        }

        resetButton.setOnClickListener {
            Log.e("TimerActivity", "resetButton clicked")
            // 초기화 시 DB에 저장
            saveMeasurementData()
            resetTimer()
        }

        restartButton.setOnClickListener {
            startTimer()
        }

        resultButton.setOnClickListener {
            // 결과 화면으로 이동
            goToResultScreen()
        }
    }

    private fun startTimer() {
        if (startTime == 0L) {
            startTime = System.currentTimeMillis()
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
                saveMeasurementData()
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
        pausedTime = System.currentTimeMillis()
        startButton.visibility = View.GONE
        pauseButton.visibility = View.GONE
        resetButton.visibility = View.VISIBLE
        restartButton.visibility = View.VISIBLE
    }

    private fun resetTimer() {
        countDownTimer?.cancel()
        timeLeftInMillis = 1200000 // 20 minutes
        updateTimerText()
        timerRunning = false
        startTime = 0
        endTime = 0
        pausedTime = 0
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

    private fun saveMeasurementData() {
        Log.e("TimerActivity", "saveMeasurementData1 called!")

//        if (startTime == 0L || endTime == 0L) return
        Log.e("TimerActivity", "saveMeasurementData2 called!")

        val measurementTimeInSeconds = (endTime - startTime) / 1000
        val measurementTime = Timestamp(Date(startTime))
        val measurementDate = measurementTime.toDate().toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate()

        val blinkData = hashMapOf(
            "blink_id" to UUID.randomUUID().toString(),
            "frequency" to 20, // 실제 빈도수를 계산해서 넣어야 함
            "measurement_time" to measurementTime.toDate(),
            "measurement_date" to measurementDate.toString(),
            "average_frequency_per_minute" to 20 // 실제 분당 평균 빈도수 계산해서 넣어야 함
        )

        Log.e("TimerActivity", blinkData.toString())
        println("Saving data: $blinkData")

        val userDocument = db.collection("nunkkam").document("users")

        userDocument.get().addOnSuccessListener { document ->
            if (document.exists()) {
                userDocument.update("blinks", FieldValue.arrayUnion(blinkData))
                    .addOnSuccessListener {
                        println("Data successfully written!")
                    }
                    .addOnFailureListener { e ->
                        Log.e("TimerActivity", "saveMeasurementData3 called!")
                    }
            } else {
                // 문서가 존재하지 않으면 생성
                val newUser = hashMapOf(
                    "guest_id" to userId,
                    "birth_date" to "1990-01-01T00:00:00Z", // 예시로 설정한 생년월일
                    "tutorial" to true,
                    "blinks" to listOf(blinkData)
                )
                userDocument.set(newUser)
                    .addOnSuccessListener {
                        println("New user document successfully created!")
                    }
                    .addOnFailureListener { e ->
                        Log.e("TimerActivity", "saveMeasurementData4 called!")
                    }
            }
        }
    }

    private fun goToResultScreen() {
        // 결과 화면으로 이동하는 코드 추가
        val intent = Intent(this, CalendarActivity::class.java)
        startActivity(intent)
    }
}
