package com.its.nunkkam.android

import android.os.Bundle
import android.util.Log
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import com.anychart.AnyChart
import com.anychart.AnyChartView
import com.anychart.chart.common.dataentry.DataEntry
import com.anychart.chart.common.dataentry.ValueDataEntry

class ChartActivity : AppCompatActivity() {

    private lateinit var chartView1: AnyChartView
    private lateinit var chartView2: AnyChartView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 로그 추가
        Log.d("ChartActivity", "onCreate 시작")

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(android.graphics.Color.BLACK)
        }

        chartView1 = AnyChartView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f // 비율 조정: 그래프 1의 비율을 1로 설정
            )
        }
        layout.addView(chartView1)

        chartView2 = AnyChartView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f // 비율 조정: 그래프 2의 비율을 1로 설정
            )
        }
        layout.addView(chartView2)

        setContentView(layout)

        setupCurrentBlinkGraph()
        setupAverageBlinkGraph()

        // 로그 추가
        Log.d("ChartActivity", "onCreate 종료")
    }

    private fun setupCurrentBlinkGraph() {
        Log.d("ChartActivity", "setupCurrentBlinkGraph 시작")
        val bar = AnyChart.bar()

        bar.background().fill("#000000")
        bar.title("현재 눈 깜빡임 수치")
        bar.title().padding(0.0, 0.0, 10.0, 0.0)
        bar.title().fontColor("#4DB6AC")
        bar.title().fontSize(16)
        bar.title().fontWeight("bold")
        bar.title().background().fill("#00796B")

        val data = listOf(
            ValueDataEntry("나", 12),
            ValueDataEntry("평균", 15)
        )

        val series = bar.bar(data)
        series.color("#B2DFDB")

        bar.xAxis(0).title("").labels().fontSize(14)
        bar.yAxis(0).title("").labels().fontSize(14)

        bar.labels().enabled(true)
        bar.labels().position("right")
        bar.labels().fontSize(14)
        bar.labels().fontColor("#FFFFFF")

        chartView1.setChart(bar)

        // 로그 추가
        Log.d("ChartActivity", "setupCurrentBlinkGraph 종료")
    }

    private fun setupAverageBlinkGraph() {
        Log.d("ChartActivity", "setupAverageBlinkGraph 시작")
        val column = AnyChart.column()

        column.background().fill("#000000")

        // 제목과 부제목을 HTML 태그를 사용하여 설정
        val titleHtml = "<span style=\"color:#4DB6AC; font-size:16px; font-weight:bold;\">평균 눈 깜빡임 수치</span><br/><span style=\"color:#4DB6AC; font-size:12px;\">(분당 평균)</span>"

        val mainTitle = column.title()
        mainTitle.enabled(true)
        mainTitle.useHtml(true) // HTML 사용을 활성화
        mainTitle.text(titleHtml)
        mainTitle.background().fill("#00796B")
        mainTitle.hAlign("center")

        val data = generateWeeklyAverageData()

        val series = column.column(data)
        series.color("#4DB6AC")

        column.xAxis(0).title("").labels().fontSize(12)
        column.yAxis(0).title("").labels().fontSize(12)

        column.labels().enabled(true)
        column.labels().position("top")
        column.labels().fontSize(12)
        column.labels().fontColor("#FFFFFF")

        chartView2.setChart(column)

        // 로그 추가
        Log.d("ChartActivity", "setupAverageBlinkGraph 종료")
    }

    private fun generateWeeklyAverageData(): List<DataEntry> {
        Log.d("ChartActivity", "generateWeeklyAverageData 시작")
        val data = listOf(
            ValueDataEntry("6주 전", 11),
            ValueDataEntry("5주 전", 13),
            ValueDataEntry("4주 전", 12),
            ValueDataEntry("3주 전", 14),
            ValueDataEntry("2주 전", 10),
            ValueDataEntry("1주 전", 15),
            ValueDataEntry("오늘", 12) // "이번 주"를 "오늘"로 변경
        )
        // 로그 추가
        Log.d("ChartActivity", "generateWeeklyAverageData 종료")
        return data
    }
}

