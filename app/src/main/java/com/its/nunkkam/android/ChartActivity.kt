package com.its.nunkkam.android

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.anychart.AnyChart
import com.anychart.AnyChartView
import com.anychart.chart.common.dataentry.DataEntry
import com.anychart.chart.common.dataentry.ValueDataEntry
import java.text.SimpleDateFormat
import java.util.*

class ChartActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chart) // 새로 생성한 레이아웃 파일 사용

//        setupCurrentBlinkGraph()
        setupAverageBlinkGraph()
    }

    // 현재 눈 깜빡임 수치 그래프 설정
//    private fun setupCurrentBlinkGraph() {
//        val anyChartView: AnyChartView = findViewById(R.id.any_chart_view)
//        val bar = AnyChart.bar()
//
//        // 그래프 스타일 설정
//        bar.background().fill("#000000")
//        bar.title("현재 눈 깜빡임 수치 (*분당 평균)")
//        bar.title().padding(0.0, 0.0, 10.0, 0.0)
//        bar.title().fontColor("#4DB6AC")
//        bar.title().fontSize(18)
//        bar.title().fontWeight("bold")
//        bar.title().background().fill("#00796B")
//
//        // 데이터 설정
//        val data = listOf(
//            ValueDataEntry("나", 7),
//            ValueDataEntry("평균", 15)
//        )
//
//        Log.d("ChartActivity", "Current Blink Data: $data")
//
//        val series = bar.bar(data)
//        series.color("#B2DFDB")
//
//        // 축 설정
//        bar.xAxis(0).title("").labels().fontSize(14)
//        bar.yAxis(0).title("").labels().fontSize(14)
//
//        // 레이블 설정
//        bar.labels().enabled(true)
//        bar.labels().position("right")
//        bar.labels().fontSize(14)
//        bar.labels().fontColor("#FFFFFF")
//
//        anyChartView.setChart(bar)
//    }

    // 평균 눈 깜빡임 수치 그래프 설정
    private fun setupAverageBlinkGraph() {
        val anyChartView: AnyChartView = findViewById(R.id.any_chart_view2)
        val column = AnyChart.column()

        // 그래프 스타일 설정
        column.background().fill("#000000")
        column.title("평균 눈 깜빡임 수치 (*분당 평균)")
        column.title().padding(0.0, 0.0, 10.0, 0.0)
        column.title().fontColor("#4DB6AC")
        column.title().fontSize(18)
        column.title().fontWeight("bold")
        column.title().background().fill("#00796B")

        val data = generateWeeklyAverageData()

        Log.d("ChartActivity", "Weekly Average Data: $data")

        val series = column.column(data)
        series.color("#4DB6AC")

        // 축 설정
        column.xAxis(0).title("").labels().fontSize(12)
        column.yAxis(0).title("").labels().fontSize(12)

        // 레이블 설정
        column.labels().enabled(true)
        column.labels().position("top")
        column.labels().fontSize(12)
        column.labels().fontColor("#FFFFFF")

        anyChartView.setChart(column)
    }

    // 주간 평균 데이터 생성
    private fun generateWeeklyAverageData(): List<DataEntry> {
        val calendar = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val weekFormat = SimpleDateFormat("MM월 W주차", Locale.getDefault())

        val data = mutableListOf<DataEntry>()
        val random = Random()

        // 오늘 데이터 추가
        data.add(ValueDataEntry("오늘", random.nextInt(6) + 10))

        // 이전 6주 데이터 생성
        for (i in 1..6) {
            calendar.add(Calendar.WEEK_OF_YEAR, -1)
            val weekStart = calendar.time
            val weekLabel = weekFormat.format(weekStart)

            var weeklySum = 0
            var count = 0
            for (j in 0..6) {
                weeklySum += random.nextInt(6) + 8
                count++
            }
            val weeklyAverage = weeklySum / count
            data.add(ValueDataEntry(weekLabel, weeklyAverage))
        }

        return data.reversed()
    }
}