package com.its.nunkkam.android // 패키지 선언: 이 코드가 속한 패키지를 지정

import android.annotation.SuppressLint
import android.os.Bundle
import android.widget.CalendarView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.util.Calendar
import java.util.HashMap
import android.graphics.Color
//import android.view.Gravity;
//import android.widget.RelativeLayout;
import com.anychart.AnyChart
import com.anychart.AnyChartView
//import com.anychart.chart.common.dataentry.DataEntry
import com.anychart.chart.common.dataentry.ValueDataEntry

class CalendarActivity : AppCompatActivity() {

    private lateinit var calendarView: CalendarView
    private lateinit var textViewBlinkFrequency: TextView
    private val blinkFrequencyData: HashMap<String, Int> = HashMap()

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_calendar)

        calendarView = findViewById(R.id.calendarView)//캘린더 뷰 아이디
        textViewBlinkFrequency = findViewById(R.id.textViewBlinkFrequency)//텍스트 뷰 아이디

        generateBlinkFrequencyData() // 데이터 받아오기
        setupCurrentBlinkGraph()// 현재 눈 깜빡임 수치 그래프 설정

        calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            val selectedDate = "$year-${month + 1}-$dayOfMonth" // 날짜 가져오기
            val blinkFrequency = blinkFrequencyData[selectedDate] ?: 0 // 눈깜빡임 빈도수 가져오기

            val value = blinkFrequency // 이 값에 따라 색상을 변경

            if (value >= 15) {
                textViewBlinkFrequency.setTextColor(Color.GREEN)//수치가 좋으면 초록
            }
            else if(value in 1..14){
                textViewBlinkFrequency.setTextColor(Color.RED)//안좋으면 빨강
            }
            else{
                textViewBlinkFrequency.setTextColor(Color.BLACK)//에러시 검은색
            }

            textViewBlinkFrequency.text = "Blinks on $selectedDate: $blinkFrequency" //텍스트 표시
        }
    }

    private fun generateBlinkFrequencyData() { //랜덤 데이터 생성 및 받아오기
        val calendar = Calendar.getInstance()
        val dateFormat = java.text.SimpleDateFormat("yyyy-M-d", java.util.Locale.getDefault())

        calendar.add(Calendar.YEAR, -1)// 1년 전 날짜를 계산
        val oneYearAgo = calendar.timeInMillis
        calendarView.minDate = oneYearAgo// 최소 달력 날짜 설정

        calendar.timeInMillis = System.currentTimeMillis()// 오늘 날짜로 초기화

        while (calendar.timeInMillis >= oneYearAgo) {
            val dateStr = dateFormat.format(calendar.time)
            val blinkFrequency = (0..30).random() // 데이터 베이스 x-> 랜덤 눈 깜빡임 빈도수 생성
            blinkFrequencyData[dateStr] = blinkFrequency //깜빡임 배열 생성
            calendar.add(Calendar.DAY_OF_YEAR, -1)//날짜를 1씩 감소
        }
    }

    private fun setupCurrentBlinkGraph() { //그래프 부분 그리기
        val anyChartView: AnyChartView = findViewById(R.id.calendar_chart_view)
        val bar = AnyChart.bar()

        // 그래프 스타일 설정
        bar.background().fill("#000000")
        bar.title("현재 눈 깜빡임 수치 (*분당 평균)")
        bar.title().padding(0.0, 0.0, 10.0, 0.0)
        bar.title().fontColor("#4DB6AC")
        bar.title().fontSize(18)
        bar.title().fontWeight("bold")
        bar.title().background().fill("#00796B")

        // 데이터 설정
        val data = listOf(
            ValueDataEntry("나", 7),
            ValueDataEntry("평균", 15)
        )

        val series = bar.bar(data)
        series.color("#B2DFDB")

        // 축 설정
        bar.xAxis(0).title("").labels().fontSize(14)
        bar.yAxis(0).title("").labels().fontSize(14)

        // 레이블 설정
        bar.labels().enabled(true)
        bar.labels().position("right")
        bar.labels().fontSize(14)
        bar.labels().fontColor("#FFFFFF")

        anyChartView.setChart(bar)
    }
}