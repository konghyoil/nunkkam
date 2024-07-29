package com.its.nunkkam.android

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.anychart.AnyChart
import com.anychart.AnyChartView
import com.anychart.chart.common.dataentry.DataEntry
import com.anychart.chart.common.dataentry.ValueDataEntry
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

class ChartFragment : Fragment() {

    // Firestore 인스턴스 선언
    private lateinit var firestore: FirebaseFirestore

    // Fragment의 View를 생성하고 반환하는 메서드
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Log.d("Fragment", "ChartFragment onCreateView called")
        // fragment_chart 레이아웃을 인플레이트
        return inflater.inflate(R.layout.fragment_chart, container, false)
    }

    // View가 생성된 후 호출되는 메서드
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Firestore 인스턴스 초기화
        firestore = FirebaseFirestore.getInstance()
        // 평균 눈 깜빡임 그래프 설정
        setupAverageBlinkGraph()
    }

    // 평균 눈 깜빡임 그래프를 설정하는 함수
    private fun setupAverageBlinkGraph() {
        // AnyChartView 찾기
        val anyChartView: AnyChartView = requireView().findViewById(R.id.any_chart_view2)
        // 컬럼 차트 생성
        val column = AnyChart.column()

        // 차트 스타일 설정
        column.background().fill("#0D3D39")
        column.title("평균 눈 깜빡임 수치 (*분당 평균)")
        column.title().padding(0.0, 0.0, 10.0, 0.0)
        column.title().fontColor("#009D90")
        column.title().fontSize(16)
        column.title().background().fill("#FFFFFF")

        // Firestore에서 데이터 가져오기
        firestore.collection("USERS").document("user1234")
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val blinkData = document.get("blinks") as? List<Map<String, Any>>
                    if (blinkData != null) {
                        // 주간 평균 데이터 생성
                        val data = generateWeeklyAverageData(blinkData)
                        val series = column.column(data)
                        series.color("#009D90")

                        // 축 설정
                        column.xAxis(0).title("").labels().fontSize(12).fontColor("#009D90")
                        column.yAxis(0).title("").labels().fontSize(12).fontColor("#009D90")

                        // 레이블 설정
                        column.labels().enabled(true)
                        column.labels().position("top")
                        column.labels().fontSize(12)
                        column.labels().fontColor("#009D90")

                        // 차트 설정
                        anyChartView.setChart(column)
                    }
                }
            }
            .addOnFailureListener { exception ->
                println("문서 가져오기 오류: $exception")
            }
    }

    // 수정: 주간 평균 데이터를 생성하는 함수
    private fun generateWeeklyAverageData(blinkData: List<Map<String, Any>>): List<DataEntry> {
        val data = mutableListOf<DataEntry>()
        val calendar = Calendar.getInstance()
        // 수정: 월요일을 한 주의 시작으로 설정
        calendar.firstDayOfWeek = Calendar.MONDAY

        // 현재 날짜로부터 6주 전 날짜 계산
        calendar.add(Calendar.WEEK_OF_YEAR, -5)  // 현재 주 포함 6주
        val sixWeeksAgo = calendar.time

        // 주차별 데이터 그룹화
        val weeklyData = blinkData
            .filter { blink ->
                val measurementDate = (blink["measurement_date"] as Timestamp).toDate()
                measurementDate.after(sixWeeksAgo) || measurementDate == sixWeeksAgo
            }
            .groupBy { blink ->
                val measurementDate = (blink["measurement_date"] as Timestamp).toDate()
                getWeekLabel(measurementDate)
            }

        // 수정: 최근 6주의 라벨 생성
        val weeks = mutableListOf<String>()
        repeat(6) {
            val weekLabel = getWeekLabel(calendar.time)
            if (!weeks.contains(weekLabel)) {
                weeks.add(weekLabel)
            }
            calendar.add(Calendar.WEEK_OF_YEAR, 1)
        }

        // 각 주차별 평균 계산 및 데이터 추가
        weeks.forEach { weekLabel ->
            val weeklyAverage = weeklyData[weekLabel]?.let { blinks ->
                blinks.map {
                    when (val value = it["average_frequency_per_minute"]) {
                        is Long -> value.toDouble()
                        is Double -> value
                        else -> 0.0
                    }
                }.average().roundToInt()
            } ?: 0
            data.add(ValueDataEntry(weekLabel, weeklyAverage))
        }

        // 최근 데이터 추가
        val recentBlink = blinkData.maxByOrNull { (it["measurement_date"] as Timestamp).seconds }
        val recentAverage = recentBlink?.let {
            when (val value = it["average_frequency_per_minute"]) {
                is Long -> value.toDouble()
                is Double -> value
                else -> 0.0
            }.roundToInt()
        } ?: 0
        data.add(ValueDataEntry("최근", recentAverage))

        return data
    }

    // 수정: 주차 라벨을 생성하는 함수
    private fun getWeekLabel(date: Date): String {
        val calendar = Calendar.getInstance()
        calendar.firstDayOfWeek = Calendar.MONDAY
        calendar.time = date
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)

        val month = calendar.get(Calendar.MONTH) + 1
        val weekOfMonth = calendar.get(Calendar.WEEK_OF_MONTH)

        return "${month}월 ${weekOfMonth}주차"
    }
}