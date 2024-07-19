package com.its.nunkkam.android // 패키지 선언: 이 코드가 속한 패키지를 지정

// 필요한 라이브러리들을 가져오기
import android.content.Intent // 다른 액티비티를 시작하기 위한 클래스
import android.os.Bundle // 액티비티 생명주기 관련 클래스
import androidx.appcompat.app.AppCompatActivity // 앱 호환성을 위한 기본 액티비티 클래스
import android.widget.Button // 버튼 위젯을 사용하기 위한 클래스

// 주석 규칙 | [외부]: 외부 데이터에서 가져오는 부분을 구분하기 위한 주석

// MainActivity 클래스 정의: 앱의 메인 화면을 담당
class MainActivity : AppCompatActivity() {

    // 액티비티가 생성될 때 호출되는 함수
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState) // 부모 클래스의 onCreate 메서드 호출
        setContentView(R.layout.activity_main) // [외부] 레이아웃 XML 파일을 가져와 화면에 설정

        // BlinkActivity를 시작하는 버튼 설정
//        val startBlinkActivityButton: Button = findViewById(R.id.startBlinkActivityButton)
//        startBlinkActivityButton.setOnClickListener {
//            startBlinkActivity() // 버튼 클릭 시 BlinkActivity 시작
//        }
    }

    // BlinkActivity를 시작하는 함수
//    private fun startBlinkActivity() {
//        val intent = Intent(this, BlinkActivity::class.java) // BlinkActivity로 이동하기 위한 Intent 생성
//        startActivity(intent) // BlinkActivity 시작
//    }
}