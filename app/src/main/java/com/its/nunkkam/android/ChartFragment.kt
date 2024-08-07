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
import java.util.*
import kotlin.math.roundToInt

// ChartFragment: 차트를 표시하는 Fragment
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

        // UserManager에서 user_id 가져오기
        val userId = UserManager.userId ?: "unknown_user"
        Log.d("ChartFragment", "User ID: $userId")

        // 평균 눈 깜빡임 그래프 설정
        setupAverageBlinkGraph(userId)
    }

    // 수정: 평균 눈 깜빡임 그래프를 설정하는 함수
    private fun setupAverageBlinkGraph(userId: String) {
        val anyChartView: AnyChartView = requireView().findViewById(R.id.any_chart_view2)
        val column = AnyChart.column()

        column.background().fill("#D9D9D9")
        column.title().enabled(false)

        firestore.collection("USERS").document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val blinkData = document.get("blinks") as? List<Map<String, Any>>
                    if (blinkData != null && blinkData.isNotEmpty()) {
                        val data = generateWeeklyAverageData(blinkData)
                        if (data.isNotEmpty()) {
                            val series = column.column(data)

                            // 수정: 막대 색상 및 모양 설정
                            series.fill(
                                "function() { " +
                                        "if (this.x === '최근') return '#2E2E2E'; " +
                                        "else return '#888888'; " +
                                        "}"
                            )

                            // 수정: 축 설정
                            column.xAxis(0).labels().fontColor("#888888")
                            column.yAxis(0).labels().fontColor("#888888")

                            // 수정: 레이블 설정
                            column.labels().enabled(true)
                            column.labels().position("top")
                            column.labels().fontColor(
                                "function() { " +
                                        "if (this.x === '최근') return '#2E2E2E'; " +
                                        "else return '#888888'; " +
                                        "}"
                            )

                            // 수정: x축 레이블 설정
                            column.xAxis(0).labels().fontColor(
                                "function() { " +
                                        "if (this.value === '최근') return '#2E2E2E'; " +
                                        "else return '#888888'; " +
                                        "}"
                            )

                            // 추가: y축 범위 설정
                            column.yScale().minimum(0)
                            column.yScale().maximum(60)  // 적절한 최대값 설정

                            // 추가: 애니메이션 추가
                            column.animation(true)

                            anyChartView.setChart(column)
                        } else {
                            Log.e("ChartFragment", "No data to display")
                            // 에러 메시지 표시
                        }
                    } else {
                        Log.e("ChartFragment", "Blink data is null or empty")
                        // 에러 메시지 표시
                    }
                } else {
                    Log.e("ChartFragment", "Document does not exist")
                    // 에러 메시지 표시
                }
            }
            .addOnFailureListener { exception ->
                Log.e("ChartFragment", "Error fetching document", exception)
                // 에러 메시지 표시
            }
    }

    // 수정: 주간 평균 데이터를 생성하는 함수
    private fun generateWeeklyAverageData(blinkData: List<Map<String, Any>>): List<DataEntry> {
        val data = mutableListOf<DataEntry>()
        val calendar = Calendar.getInstance()
        // 월요일을 한 주의 시작으로 설정
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

        // 최근 6주의 라벨 생성
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

        // 수정: 데이터가 없는 경우 처리
        if (data.isEmpty()) {
            // 최근 6주 동안의 빈 데이터 추가
            for (i in 5 downTo 0) {
                val weekLabel = getWeekLabel(Calendar.getInstance().apply { add(Calendar.WEEK_OF_YEAR, -i) }.time)
                data.add(ValueDataEntry(weekLabel, 0))
            }
        }

        // 수정: 최근 데이터 추가 (없으면 0으로)
        val recentAverage = blinkData.lastOrNull()?.let {
            (it["average_frequency_per_minute"] as? Number)?.toDouble()?.roundToInt() ?: 0
        } ?: 0
        data.add(ValueDataEntry("최근", recentAverage))

        return data
    }

    // 주차 라벨을 생성하는 함수
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