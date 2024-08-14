package com.its.nunkkam.android

import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment

class ResultActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_result)

        try {
            // 툴바 설정
            val toolbar: androidx.appcompat.widget.Toolbar = findViewById(R.id.toolbar)
            setSupportActionBar(toolbar)

            // 수정: 앱 타이틀 제거
            supportActionBar?.setDisplayShowTitleEnabled(false)

            // tips 버튼 클릭 리스너 설정
            val tipsButton: TextView = findViewById(R.id.tipsButton)
            tipsButton.setOnClickListener {
                showPopup()
            }

            val ratePerMinute = intent.getIntExtra("RATE_PER_MINUTE", 0)

            // 각 Fragment를 생성하고 순서대로 추가합니다.
            val cardFragment = CardFragment.newInstance(ratePerMinute)
            val chartFragment = ChartFragment()
            val calendarFragment = CalendarFragment()

            // FragmentTransaction을 통해 Fragment들을 추가
            supportFragmentManager.beginTransaction().apply {
                replace(R.id.cardContainer, cardFragment) // CardFragment를 cardContainer에 추가
                replace(R.id.chartContainer, chartFragment) // ChartFragment를 chartContainer에 추가
                replace(R.id.calendarContainer, calendarFragment) // CalendarFragment를 calendarContainer에 추가
            }.commit()
        } catch (e: Exception) {
            Log.e("ResultActivity", "Error setting up activity", e)
            // 에러 처리 (예: 사용자에게 오류 메시지 표시)
        }
    }

    // 수정: showPopup 메서드 추가
    private fun showPopup() {
        try {
            val popupFragment = PopupFragment.newInstance()
            popupFragment.show(supportFragmentManager, "popup")
        } catch (e: Exception) {
            Log.e("ResultActivity", "Error showing popup", e)
        }
    }
}