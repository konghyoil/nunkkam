package com.its.nunkkam.android // 패키지 선언: 이 코드가 속한 패키지를 지정

import android.annotation.SuppressLint
import android.os.Bundle
import android.widget.CalendarView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.util.Calendar
import java.util.HashMap

class CalendarActivity : AppCompatActivity() {

    private lateinit var calendarView: CalendarView
    private lateinit var textViewBlinkFrequency: TextView
    private val blinkFrequencyData: HashMap<String, Int> = HashMap()

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_calendar)

        calendarView = findViewById(R.id.calendarView)
        textViewBlinkFrequency = findViewById(R.id.textViewBlinkFrequency)

        generateBlinkFrequencyData() // 데이터 받아오기

        calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            val selectedDate = "$year-${month + 1}-$dayOfMonth" // 날짜 가져오기
            val blinkFrequency = blinkFrequencyData[selectedDate] ?: 0 // 눈깜빡임 빈도수 가져오기
            textViewBlinkFrequency.text = "Blinks on $selectedDate: $blinkFrequency" //텍스트 표시
        }
    }

    private fun generateBlinkFrequencyData() {
        val calendar = Calendar.getInstance()
        val dateFormat = java.text.SimpleDateFormat("yyyy-M-d", java.util.Locale.getDefault())

        calendar.add(Calendar.YEAR, -1)// 1년 전 날짜를 계산
        val oneYearAgo = calendar.timeInMillis
        calendarView.minDate = oneYearAgo// 최소 달력 날짜 설정

        calendar.timeInMillis = System.currentTimeMillis()// 오늘 날짜로 초기화

        while (calendar.timeInMillis >= oneYearAgo) {
            val dateStr = dateFormat.format(calendar.time)
            val blinkFrequency = (0..30).random() // 데이터 베이스 x-> 랜덤 눈 깜빡임 빈도수 생성
            blinkFrequencyData[dateStr] = blinkFrequency //
            calendar.add(Calendar.DAY_OF_YEAR, -1)
        }
    }
}