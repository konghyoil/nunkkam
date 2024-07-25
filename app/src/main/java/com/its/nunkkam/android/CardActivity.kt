package com.its.nunkkam.android

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class CardActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_card)

        // Intent로부터 분당 횟수를 받아옴
        val ratePerMinute = intent.getIntExtra("RATE_PER_MINUTE", 0)

        // 결과를 화면에 표시
        displayResult(ratePerMinute)
    }

    // 특정 분당 횟수에 따른 메시지를 반환하는 함수
    private fun getEyeTypeMessage(ratePerMinute: Int): String {
        return when {
            ratePerMinute >= 15 -> "당신은 '독수리'의 눈을 가졌습니다!"
            ratePerMinute >= 12 -> "당신은 '매'의 눈을 가졌습니다!"
            ratePerMinute >= 9 -> "당신은 '올빼미'의 눈을 가졌습니다!"
            ratePerMinute >= 6 -> "당신은 '고양이'의 눈을 가졌습니다!"
            ratePerMinute >= 3 -> "당신은 '두더지'의 눈을 가졌습니다!"
            else -> "측정값이 유효하지 않습니다."
        }
    }

    // 특정 분당 횟수와 메시지를 화면에 표시하는 함수
    private fun displayResult(ratePerMinute: Int) {
        val resultTextView: TextView = findViewById(R.id.resultTextView)
        val message = "분당 $ratePerMinute 회\n${getEyeTypeMessage(ratePerMinute)}"
        resultTextView.text = message
    }
}