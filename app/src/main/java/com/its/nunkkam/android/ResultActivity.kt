package com.its.nunkkam.android

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import androidx.appcompat.app.AppCompatActivity
import android.widget.FrameLayout

class ResultActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("ResultActivity", "onCreate called")
        setContentView(R.layout.activity_result)

        // 각 Activity의 레이아웃을 FrameLayout에 인플레이트하여 추가
        // addContentToFrameLayout(R.id.cardFrame, R.layout.activity_card) // 카드 관련 부분 주석 처리 또는 제거
        addContentToFrameLayout(R.id.chartFrame, R.layout.activity_chart)
        addContentToFrameLayout(R.id.calendarFrame, R.layout.activity_calendar)
    }

    private fun addContentToFrameLayout(frameLayoutId: Int, layoutResId: Int) {
        Log.d("ResultActivity", "Inflating layout $layoutResId into frame $frameLayoutId")
        val frameLayout = findViewById<FrameLayout>(frameLayoutId)
        val inflater = LayoutInflater.from(this)
        val view = inflater.inflate(layoutResId, frameLayout, false)
        frameLayout.addView(view)
        Log.d("ResultActivity", "Layout inflated: $layoutResId into FrameLayout: $frameLayoutId")
    }
}