package com.its.nunkkam.android

import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*
import kotlin.math.roundToInt
class ChartFragment : Fragment() {
    private lateinit var firestore: FirebaseFirestore
    private lateinit var chartContainer: LinearLayout

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Log.d("Fragment", "ChartFragment onCreateView called")
        return inflater.inflate(R.layout.fragment_chart, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        firestore = FirebaseFirestore.getInstance()
        chartContainer = view.findViewById(R.id.chart_container)

        val userId = UserManager.userId ?: "unknown_user"
        Log.d("ChartFragment", "User ID: $userId")
        setupAverageBlinkGraph(userId)
    }

    private fun setupAverageBlinkGraph(userId: String) {
        firestore.collection("USERS").document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val blinkData = document.get("blinks") as? List<Map<String, Any>>
                    if (blinkData != null && blinkData.isNotEmpty()) {
                        val data = generateWeeklyAverageData(blinkData)
                        displayChart(data)
                    } else {
                        Log.e("ChartFragment", "Blink data is null or empty")
                    }
                } else {
                    Log.e("ChartFragment", "Document does not exist")
                }
            }
            .addOnFailureListener { exception ->
                Log.e("ChartFragment", "Error fetching document", exception)
            }
    }

    private fun generateWeeklyAverageData(blinkData: List<Map<String, Any>>): List<Pair<String, Int>> {
        val data = mutableListOf<Pair<String, Int>>()
        val calendar = Calendar.getInstance()
        calendar.firstDayOfWeek = Calendar.MONDAY

        calendar.add(Calendar.WEEK_OF_YEAR, -5)
        val sixWeeksAgo = calendar.time

        val weeklyData = blinkData
            .filter { blink ->
                val measurementDate = (blink["measurement_date"] as Timestamp).toDate()
                measurementDate.after(sixWeeksAgo) || measurementDate == sixWeeksAgo
            }
            .groupBy { blink ->
                val measurementDate = (blink["measurement_date"] as Timestamp).toDate()
                getWeekLabel(measurementDate)
            }

        val weeks = mutableListOf<String>()
        repeat(6) {
            val weekLabel = getWeekLabel(calendar.time)
            if (!weeks.contains(weekLabel)) {
                weeks.add(weekLabel)
            }
            calendar.add(Calendar.WEEK_OF_YEAR, 1)
        }

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
            data.add(Pair(weekLabel, weeklyAverage))
        }

        if (data.isEmpty()) {
            for (i in 5 downTo 0) {
                val weekLabel = getWeekLabel(Calendar.getInstance().apply { add(Calendar.WEEK_OF_YEAR, -i) }.time)
                data.add(Pair(weekLabel, 0))
            }
        }

        val recentAverage = blinkData.lastOrNull()?.let {
            (it["average_frequency_per_minute"] as? Number)?.toDouble()?.roundToInt() ?: 0
        } ?: 0
        data.add(Pair("최근", recentAverage))

        return data
    }

    private fun getWeekLabel(date: Date): String {
        val calendar = Calendar.getInstance()
        calendar.firstDayOfWeek = Calendar.MONDAY
        calendar.time = date
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)

        val month = calendar.get(Calendar.MONTH) + 1
        val weekOfMonth = calendar.get(Calendar.WEEK_OF_MONTH)

        return "${month}월 ${weekOfMonth}주차"
    }

    private fun displayChart(data: List<Pair<String, Int>>) {
        chartContainer.removeAllViews()

        val maxValue = data.maxOfOrNull { it.second } ?: 60
        val barWidth = resources.getDimensionPixelSize(R.dimen.bar_width)
        val barSpacing = resources.getDimensionPixelSize(R.dimen.bar_spacing)
        val maxBarHeight = resources.getDimensionPixelSize(R.dimen.max_bar_height)

        // 전체 차트의 너비를 계산 (7개의 막대와 간격, 여백 추가)
        val totalChartWidth = (barWidth + barSpacing) * 7 - barSpacing + 60 // 60dp 여백 추가

        val chartLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                totalChartWidth,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            gravity = Gravity.CENTER
        }

        val barsLayout = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.BOTTOM
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        data.forEachIndexed { index, (label, value) ->
            val barAndLabelLayout = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(barWidth + barSpacing, LinearLayout.LayoutParams.WRAP_CONTENT)
            }

            // 수치 레이블 생성
            val valueLabel = TextView(context).apply {
                text = value.toString()
                textSize = 10f
                setTextColor(ContextCompat.getColor(requireContext(), R.color.dark_lightbar))
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }
            barAndLabelLayout.addView(valueLabel)

            // 막대 생성
            val barView = View(context).apply {
                val barHeight = (value.toFloat() / maxValue * maxBarHeight).toInt().coerceAtMost(maxBarHeight)
                layoutParams = LinearLayout.LayoutParams(barWidth, barHeight)
                val isRecent = index == data.size - 1
                val barColor = if (isRecent) R.color.dark_darkbar else R.color.dark_lightbar
                setBackgroundColor(ContextCompat.getColor(requireContext(), barColor))
            }
            barAndLabelLayout.addView(barView)

            // 레이블 생성
            val labelView = TextView(context).apply {
                text = label
                textSize = 10f
                setTextColor(ContextCompat.getColor(requireContext(), R.color.dark_lightbar))
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                maxLines = 1
                if (index % 2 == 0 || index == data.size - 1) {
                    visibility = View.VISIBLE
                } else {
                    visibility = View.INVISIBLE
                    text = "" // 빈 텍스트로 설정하여 공간 차지하지 않도록 함
                }
            }
            barAndLabelLayout.addView(labelView)

            barsLayout.addView(barAndLabelLayout)
        }

        chartLayout.addView(barsLayout)

        // 차트를 컨테이너의 중앙에 배치
        val wrapperLayout = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            gravity = Gravity.CENTER
        }
        wrapperLayout.addView(chartLayout)

        chartContainer.addView(wrapperLayout)
    }
}