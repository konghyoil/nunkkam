package com.its.nunkkam.android.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*
import kotlin.math.roundToInt
import android.util.Log // 수정: Log import 추가
import com.its.nunkkam.android.R
import com.its.nunkkam.android.managers.UserManager

class ChartFragment : Fragment() {
    private lateinit var firestore: FirebaseFirestore
    private lateinit var chartContainer: LinearLayout
    private lateinit var noDataContainer: LinearLayout
    private lateinit var noDataMessageTextView: TextView
    private lateinit var noDataImageView: ImageView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_chart, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        firestore = FirebaseFirestore.getInstance()
        chartContainer = view.findViewById(R.id.chart_container)
        noDataContainer = view.findViewById(R.id.no_data_container)
        noDataMessageTextView = view.findViewById(R.id.noDataMessageTextView)
        noDataImageView = view.findViewById(R.id.noDataImageView)

        val userId = UserManager.userId ?: "unknown_user"
        setupAverageBlinkGraph(userId)
    }

    // 수정: setupAverageBlinkGraph 함수에 예외 처리 추가
    private fun setupAverageBlinkGraph(userId: String) {
        firestore.collection("USERS").document(userId)
            .get()
            .addOnSuccessListener { document ->
                try {
                    if (document != null && document.exists()) {
                        val blinkData = document.get("blinks")
                        if (blinkData is List<*>) {
                            val castedBlinkData = blinkData.filterIsInstance<Map<String, Any>>()
                            if (castedBlinkData.isNotEmpty()) {
                                val data = generateWeeklyAverageData(castedBlinkData)
                                if (data.all { it.second == 0 }) {
                                    displayNoDataMessage()
                                } else {
                                    displayChart(data)
                                }
                            } else {
                                displayNoDataMessage()
                            }
                        } else {
                            displayNoDataMessage()
                        }
                    } else {
                        displayNoDataMessage()
                    }
                } catch (e: Exception) {
                    // 수정: 예외 발생 시 로그 출력
                    Log.e("ChartFragment", "Error processing blink data", e)
                    displayNoDataMessage()
                }
            }
            .addOnFailureListener { e ->
                // 수정: 실패 시 로그 출력
                Log.e("ChartFragment", "Error fetching user data", e)
                displayNoDataMessage()
            }
    }

    private fun displayNoDataMessage() {
        chartContainer.visibility = View.GONE
        noDataContainer.visibility = View.VISIBLE
        noDataMessageTextView.text = "측정값이 유효하지 않습니다."
        noDataImageView.setImageResource(R.drawable.eye_closed)
    }

    // 수정: generateWeeklyAverageData 함수에 예외 처리 추가
    private fun generateWeeklyAverageData(blinkData: List<Map<String, Any>>): List<Pair<String, Int>> {
        return try {
            val data = mutableListOf<Pair<String, Int>>()
            val calendar = Calendar.getInstance()
            calendar.firstDayOfWeek = Calendar.MONDAY

            calendar.add(Calendar.WEEK_OF_YEAR, -5)
            val sixWeeksAgo = calendar.time

            val weeklyData = blinkData
                .filter { blink ->
                    val measurementDate = (blink["measurement_date"] as? Timestamp)?.toDate()
                    measurementDate?.after(sixWeeksAgo) ?: false || measurementDate == sixWeeksAgo
                }
                .groupBy { blink ->
                    val measurementDate = (blink["measurement_date"] as? Timestamp)?.toDate()
                    getWeekLabel(measurementDate ?: Date())
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
                            is Number -> value.toDouble()
                            else -> 0.0
                        }
                    }.average().takeIf { !it.isNaN() }?.roundToInt() ?: 0
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
                (it["average_frequency_per_minute"] as? Number)?.toDouble()?.takeIf { !it.isNaN() }?.roundToInt() ?: 0
            } ?: 0
            data.add(Pair("최근", recentAverage))

            data
        } catch (e: Exception) {
            // 수정: 예외 발생 시 로그 출력
            Log.e("ChartFragment", "Error generating weekly average data", e)
            emptyList()
        }
    }

    private fun getWeekLabel(date: Date): String {
        val calendar = Calendar.getInstance()
        calendar.firstDayOfWeek = Calendar.MONDAY
        calendar.time = date
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)

        val month = calendar.get(Calendar.MONTH) + 1
        val weekOfMonth = calendar.get(Calendar.WEEK_OF_MONTH)

        return "${month}월 ${weekOfMonth}주"
    }

    // 수정: displayChart 함수에 예외 처리 추가
    private fun displayChart(data: List<Pair<String, Int>>) {
        try {
            chartContainer.visibility = View.VISIBLE
            noDataContainer.visibility = View.GONE
            chartContainer.removeAllViews()

            val maxValue = data.maxOfOrNull { it.second } ?: 60
            val barWidth = resources.getDimensionPixelSize(R.dimen.bar_width)
            val barSpacing = resources.getDimensionPixelSize(R.dimen.bar_spacing)
            val maxBarHeight = resources.getDimensionPixelSize(R.dimen.max_bar_height)

            val totalChartWidth = (barWidth + barSpacing) * data.size - barSpacing + 60

            val chartLayout = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    totalChartWidth,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                gravity = Gravity.CENTER_HORIZONTAL
            }

            val barsLayout = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.BOTTOM
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }

            data.forEachIndexed { index, (_, value) ->
                val barAndLabelLayout = LinearLayout(context).apply {
                    orientation = LinearLayout.VERTICAL
                    gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
                    layoutParams = LinearLayout.LayoutParams(barWidth + barSpacing, LinearLayout.LayoutParams.WRAP_CONTENT)
                }

                val isRecent = index == data.size - 1

                val valueLabel = TextView(context).apply {
                    text = value.toString()
                    textSize = 12f
                    setTextColor(ContextCompat.getColor(requireContext(),
                        if (isRecent) R.color.dark_darkbar else R.color.dark_lightbar
                    ))
                    gravity = Gravity.CENTER
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                    typeface = ResourcesCompat.getFont(requireContext(), R.font.roboto_medium)
                }
                barAndLabelLayout.addView(valueLabel)

                val barView = View(context).apply {
                    val barHeight = (value.toFloat() / maxValue * maxBarHeight).toInt().coerceAtMost(maxBarHeight)
                    layoutParams = LinearLayout.LayoutParams(barWidth, barHeight)
                    setBackgroundColor(ContextCompat.getColor(requireContext(),
                        if (isRecent) R.color.dark_darkbar else R.color.dark_lightbar
                    ))
                }
                barAndLabelLayout.addView(barView)

                barsLayout.addView(barAndLabelLayout)
            }

            chartLayout.addView(barsLayout)

            val labelsLayout = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = resources.getDimensionPixelSize(R.dimen.label_top_margin)
                    bottomMargin = resources.getDimensionPixelSize(R.dimen.label_bottom_margin)
                }
            }

            data.forEachIndexed { index, (label, _) ->
                val isRecent = index == data.size - 1
                val labelView = TextView(context).apply {
                    text = if (index % 2 == 0 || isRecent) label else ""
                    textSize = 11f
                    setTextColor(ContextCompat.getColor(requireContext(),
                        if (isRecent) R.color.dark_darkbar else R.color.dark_lightbar
                    ))
                    gravity = Gravity.CENTER
                    layoutParams = LinearLayout.LayoutParams(barWidth + barSpacing, LinearLayout.LayoutParams.WRAP_CONTENT)
                    maxLines = 1
                    if (index % 2 == 0 || isRecent) {
                        typeface = ResourcesCompat.getFont(requireContext(), R.font.roboto_medium)
                    }
                }
                labelsLayout.addView(labelView)
            }

            chartLayout.addView(labelsLayout)

            val wrapperLayout = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                gravity = Gravity.CENTER_HORIZONTAL
            }
            wrapperLayout.addView(chartLayout)

            chartContainer.addView(wrapperLayout)
        } catch (e: IllegalArgumentException) {
            // 수정: 특정 예외 처리 추가
            Log.e("ChartFragment", "IllegalArgumentException in displayChart", e)
            displayNoDataMessage()
        } catch (e: NullPointerException) {
            // 수정: Null 포인터 예외 처리 추가
            Log.e("ChartFragment", "NullPointerException in displayChart", e)
            displayNoDataMessage()
        } catch (e: Exception) {
            // 수정: 일반적인 예외 처리 추가
            Log.e("ChartFragment", "Unexpected error in displayChart", e)
            displayNoDataMessage()
        }
    }
}