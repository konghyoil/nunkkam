package com.its.nunkkam.android

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.Button
// 수정: Firebase 관련 import 추가
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
// 수정: Gson 관련 import 추가
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.IOException

class MainActivity : AppCompatActivity() {

    // Firestore 데이터베이스 인스턴스를 저장할 변수
    // 수정: firestore 변수 선언
    private lateinit var firestore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 메인 액티비티의 레이아웃 설정
        setContentView(R.layout.activity_main)

        // 수정: Firebase 초기화 추가
        FirebaseApp.initializeApp(this)
        firestore = FirebaseFirestore.getInstance()

        // JSON 데이터를 Firestore에 저장하는 함수 호출
        // 주의: 이 함수는 앱을 실행할 때마다 데이터를 덮어씁니다.
        // 실제 사용 시에는 한 번만 실행되도록 로직을 추가해야 할 수 있습니다.
        saveJsonDataToFirestore()

        // 각 기능을 시작하는 버튼들을 설정합니다.
        setupButtons()
    }

    // 각 기능을 시작하는 버튼들을 설정하는 함수
    private fun setupButtons() {
        // BlinkActivity를 시작하는 버튼 설정
        val startBlinkActivityButton: Button = findViewById(R.id.startBlinkActivityButton)
        startBlinkActivityButton.setOnClickListener {
            startBlinkActivity()
        }

        // TimerActivity를 시작하는 버튼 설정
        val startTimerActivityButton: Button = findViewById(R.id.startTimerActivityButton)
        startTimerActivityButton.setOnClickListener {
            startTimerActivity()
        }

        // CalendarActivity를 시작하는 버튼 설정
        val startCalendarActivityButton: Button = findViewById(R.id.startCalendarActivityButton)
        startCalendarActivityButton.setOnClickListener {
            startCalendarActivity()
        }

        // ChartActivity를 시작하는 버튼 설정
        val startChartActivityButton: Button = findViewById(R.id.startChartActivityButton)
        startChartActivityButton.setOnClickListener {
            startChartActivity()
        }
    }

    // JSON 데이터를 Firestore에 저장하는 함수
    // 수정: Firestore를 사용하도록 함수 수정
    private fun saveJsonDataToFirestore() {
        try {
            // assets 폴더에서 JSON 파일을 읽어옵니다.
            val jsonString = assets.open("blink_data.json").bufferedReader().use { it.readText() }

            // Gson 라이브러리를 사용하여 JSON 문자열을 Map 객체로 변환합니다.
            val gson = Gson()
            val type = object : TypeToken<Map<String, Any>>() {}.type
            val data: Map<String, Any> = gson.fromJson(jsonString, type)

            // Firestore의 'nunkkam' 컬렉션의 'users' 문서에 데이터를 저장합니다.
            firestore.collection("nunkkam").document("users")
                .set(data)
                .addOnSuccessListener {
                    println("JSON 데이터가 성공적으로 Firestore에 저장되었습니다!")
                }
                .addOnFailureListener { e ->
                    println("Firestore에 JSON 데이터 저장 중 오류 발생: $e")
                }
        } catch (e: IOException) {
            println("JSON 파일 읽기 오류: $e")
        }
    }

    // BlinkActivity를 시작하는 함수
    private fun startBlinkActivity() {
        val intent = Intent(this, BlinkActivity::class.java)
        startActivity(intent)
    }

    // TimerActivity를 시작하는 함수
    private fun startTimerActivity() {
        val intent = Intent(this, TimerActivity::class.java)
        startActivity(intent)
    }

    // CalendarActivity를 시작하는 함수
    private fun startCalendarActivity() {
        val intent = Intent(this, CalendarActivity::class.java)
        startActivity(intent)
    }

    // ChartActivity를 시작하는 함수
    private fun startChartActivity() {
        val intent = Intent(this, ChartActivity::class.java)
        startActivity(intent)
    }
}