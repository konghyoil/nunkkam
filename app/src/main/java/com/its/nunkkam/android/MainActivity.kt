package com.its.nunkkam.android // 패키지 선언: 이 코드가 속한 패키지를 지정

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity

// 주석 규칙 | [외부]: 외부 데이터에서 가져오는 부분을 구분하기 위한 주석

// MainActivity 클래스 정의: 앱의 메인 화면을 담당
class MainActivity : AppCompatActivity() {

    // 액티비티가 생성될 때 호출되는 함수
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        val startChartActivityButton: Button = findViewById(R.id.startChartActivityButton)
        startChartActivityButton.setOnClickListener {
            startChartActivity()
        }
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
            gravity = Gravity.CENTER
            setBackgroundColor(Color.WHITE)
        }
    }

    val buttonData = listOf(
        Triple("startBlinkActivityButton", "START BLINK", BlinkActivity::class.java),
        Triple("startTimerActivityButton", "START TIMER", TimerActivity::class.java),
        Triple("startChartActivityButton", "START CHART", ChartActivity::class.java),
            Triple("startCalendarActivityButton", "CALENDAR", CalendarActivity::class.java)
        )

    private fun startBlinkActivity() {
        val intent = Intent(this, BlinkActivity::class.java)
        startActivity(intent)

        buttonData.forEach { (idName, text, activityClass) ->
            val button = Button(this).apply {
                id = resources.getIdentifier(idName, "id", packageName)
                this.text = text
                setBackgroundColor(Color.BLUE)
                setTextColor(Color.WHITE)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0, 0, 0, 50) // 버튼 사이 간격
                }
                setOnClickListener {
                    startActivity(Intent(this@MainActivity, activityClass))
                }
            }
            layout.addView(button)
        }
    }

    private fun startChartActivity() {
        val intent = Intent(this, ChartActivity::class.java)
        startActivity(intent)
        setContentView(layout)
    }
}