package com.its.nunkkam.android

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageView
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

            // 앱 타이틀 제거
            supportActionBar?.setDisplayShowTitleEnabled(false)

            // tips 버튼 클릭 리스너 설정
            val tipsButton: TextView = findViewById(R.id.tipsButton)
            tipsButton.setOnClickListener {
                showPopup()
            }

            // 홈 버튼 클릭 리스너 설정
            val homeButton: ImageView = findViewById(R.id.homeButton)
            homeButton.setOnClickListener {
                // TimerActivity로 이동하는 Intent 생성
                val intent = Intent(this, TimerActivity::class.java)
                // 이전 액티비티를 스택에서 제거하고 새로 시작
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
                // 현재 액티비티 종료
                finish()
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