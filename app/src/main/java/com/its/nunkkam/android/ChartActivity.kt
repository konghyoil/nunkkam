package com.its.nunkkam.android

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.anychart.AnyChart
import com.anychart.AnyChartView
import com.anychart.chart.common.dataentry.DataEntry
import com.anychart.chart.common.dataentry.ValueDataEntry
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class ChartActivity : AppCompatActivity() {

    // Firestore 데이터베이스 인스턴스를 저장할 변수
    private lateinit var firestore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 차트 액티비티의 레이아웃 설정
        setContentView(R.layout.activity_chart)

        // Firestore 인스턴스 초기화
        firestore = FirebaseFirestore.getInstance()

        // 평균 눈 깜빡임 그래프 설정 함수 호출
        setupAverageBlinkGraph()
    }

    // 평균 눈 깜빡임 그래프를 설정하는 함수
    private fun setupAverageBlinkGraph() {
        // XML 레이아웃에서 차트 뷰를 찾습니다.
        val anyChartView: AnyChartView = findViewById(R.id.any_chart_view2)
        // 컬럼 차트 객체를 생성합니다.
        val column = AnyChart.column()

        // 그래프의 스타일을 설정합니다.
        column.background().fill("#000000") // 배경색 설정
        column.title("평균 눈 깜빡임 수치 (*분당 평균)") // 제목 설정
        column.title().padding(0.0, 0.0, 10.0, 0.0) // 제목 패딩 설정
        column.title().fontColor("#4DB6AC") // 제목 글자색 설정
        column.title().fontSize(18) // 제목 글자 크기 설정
        column.title().fontWeight("bold") // 제목 글자 굵기 설정
        column.title().background().fill("#00796B") // 제목 배경색 설정

        // Firestore에서 데이터를 가져옵니다.
        firestore.collection("nunkkam").document("users")
            .get()
            .addOnSuccessListener { document ->
                if (document != null) {
                    // 'blinks' 필드에서 데이터를 가져옵니다.
                    val blinks = document.get("blinks") as? List<Map<String, Any>>
                    if (blinks != null) {
                        // 주간 평균 데이터를 생성합니다.
                        val data = generateWeeklyAverageData(blinks)
                        // 차트에 데이터를 설정합니다.
                        val series = column.column(data)
                        series.color("#4DB6AC") // 막대 색상 설정

                        // x축과 y축 설정
                        column.xAxis(0).title("").labels().fontSize(12)
                        column.yAxis(0).title("").labels().fontSize(12)

                        // 레이블 설정
                        column.labels().enabled(true)
                        column.labels().position("top")
                        column.labels().fontSize(12)
                        column.labels().fontColor("#FFFFFF")

                        // 차트 뷰에 차트를 설정합니다.
                        anyChartView.setChart(column)
                    }
                }
            }
            .addOnFailureListener { exception ->
                // 데이터 가져오기 실패 시 오류를 출력합니다.
                println("문서 가져오기 오류: $exception")
            }
    }

    // 주간 평균 데이터를 생성하는 함수
    private fun generateWeeklyAverageData(blinks: List<Map<String, Any>>): List<DataEntry> {
        val weekFormat = SimpleDateFormat("MM월 W주차", Locale.getDefault())
        val data = mutableListOf<DataEntry>()

        // 주차별로 데이터를 그룹화합니다.
        val weeklyData = blinks.groupBy { blink ->
            val date = (blink["measurement_date"] as Map<String, Any>)["timestamp"] as String
            // ISO 8601 형식의 날짜 문자열을 파싱합니다.
            val parsedDate = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).parse(date)
            weekFormat.format(parsedDate)
        }

        // 각 주차별 평균을 계산합니다.
        weeklyData.forEach { (week, blinkList) ->
            val weeklyAverage = blinkList.map { it["average_frequency_per_minute"] as Double }.average()
            data.add(ValueDataEntry(week, weeklyAverage))
        }

        // 오늘의 데이터를 추가합니다. (해당 주차의 첫 번째 데이터를 사용)
        val today = Calendar.getInstance().time
        data.add(ValueDataEntry("오늘", weeklyData[weekFormat.format(today)]?.first()?.get("average_frequency_per_minute") as? Double ?: 0.0))

        // 주차 순서대로 정렬하여 반환합니다.
        // 수정된 부분: ValueDataEntry로 캐스팅하고 x 값을 String으로 캐스팅
        return data.sortedBy { (it as ValueDataEntry).getValue("x") as String }
    }
}