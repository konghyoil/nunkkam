package com.its.nunkkam.android

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.anychart.AnyChart
import com.anychart.AnyChartView
import com.anychart.chart.common.dataentry.DataEntry
import com.anychart.chart.common.dataentry.ValueDataEntry
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

class ChartActivity : AppCompatActivity() {

    private lateinit var firestore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chart)

        firestore = FirebaseFirestore.getInstance()
        setupAverageBlinkGraph()
    }

    private fun setupAverageBlinkGraph() {
        val anyChartView: AnyChartView = findViewById(R.id.any_chart_view2)
        val column = AnyChart.column()

        column.background().fill("#000000")
        column.title("평균 눈 깜빡임 수치 (*분당 평균)")
        column.title().padding(0.0, 0.0, 10.0, 0.0)
        column.title().fontColor("#4DB6AC")
        column.title().fontSize(18)
        column.title().fontWeight("bold")
        column.title().background().fill("#00796B")

        firestore.collection("USERS").document("user1234")
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val blinkData = document.get("blinks") as? List<Map<String, Any>>
                    if (blinkData != null) {
                        val data = generateWeeklyAverageData(blinkData)
                        val series = column.column(data)
                        series.color("#4DB6AC")

                        column.xAxis(0).title("").labels().fontSize(12)
                        column.yAxis(0).title("").labels().fontSize(12)

                        column.labels().enabled(true)
                        column.labels().position("top")
                        column.labels().fontSize(12)
                        column.labels().fontColor("#FFFFFF")

                        anyChartView.setChart(column)
                    }
                }
            }
            .addOnFailureListener { exception ->
                println("문서 가져오기 오류: $exception")
            }
    }

    private fun generateWeeklyAverageData(blinkData: List<Map<String, Any>>): List<DataEntry> {
        val weekFormat = SimpleDateFormat("MM월 W주차", Locale.KOREAN)
        val data = mutableListOf<DataEntry>()

        // 현재 날짜로부터 6주 전 날짜 계산
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.WEEK_OF_YEAR, -6)
        val sixWeeksAgo = calendar.time

        // 주차별 데이터 그룹화
        val weeklyData = blinkData
            .filter { blink ->
                val measurementDate = (blink["measurement_date"] as Timestamp).toDate()
                measurementDate.after(sixWeeksAgo)
            }
            .groupBy { blink ->
                val measurementDate = (blink["measurement_date"] as Timestamp).toDate()
                weekFormat.format(measurementDate)
            }

        // 주차별 평균 계산
        weeklyData.forEach { (week, blinks) ->
            val weeklyAverage = blinks
                .map {
                    when (val value = it["average_frequency_per_minute"]) {
                        is Long -> value.toDouble()
                        is Double -> value
                        else -> 0.0
                    }
                }
                .average()
                .roundToInt()
            data.add(ValueDataEntry(week, weeklyAverage))
        }

        // 최근 데이터 추가
        val recentBlink = blinkData.maxByOrNull { (it["measurement_date"] as Timestamp).seconds }
        if (recentBlink != null) {
            val recentAverage = when (val value = recentBlink["average_frequency_per_minute"]) {
                is Long -> value.toDouble()
                is Double -> value
                else -> 0.0
            }.roundToInt()
            data.add(ValueDataEntry("최근", recentAverage))
        }

        return data.sortedBy { it.getValue("x") as String }
    }
}