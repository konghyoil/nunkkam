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
        val startBlinkActivityButton: Button = findViewById(R.id.startBlinkActivityButton)
        startBlinkActivityButton.setOnClickListener {
            startBlinkActivity() // 버튼 클릭 시 BlinkActivity 시작
        }

        // TimerActivity를 시작하는 버튼 설정
        val startTimerActivityButton: Button = findViewById(R.id.startTimerActivityButton)
        startTimerActivityButton.setOnClickListener {
            startTimerActivity() // 버튼 클릭 시 TimerActivity 시작
        }

        // CalendarActivity를 시작하는 버튼 설정
        val startCalendarActivityButton: Button = findViewById(R.id.startCalendarActivityButton)
        startCalendarActivityButton.setOnClickListener {
            startCalendarActivity() // 버튼 클릭 시 CalendarActivity 시작
        }

        // ChartActivity를 시작하는 버튼 설정
        val startChartActivityButton: Button = findViewById(R.id.startChartActivityButton)
        startChartActivityButton.setOnClickListener {
            startChartActivity() // 버튼 클릭 시 ChartActivity 시작
        }

        // CardActivity를 시작하는 버튼 설정
        val startCardActivityButton: Button = findViewById(R.id.startCardActivityButton)
        startCardActivityButton.setOnClickListener {
            startCardActivity() // 버튼 클릭 시 CardActivity 시작
        }

        // ResultActivity를 시작하는 버튼 설정
        val startResultActivityButton: Button = findViewById(R.id.startResultActivityButton)
        startResultActivityButton.setOnClickListener {
            startResultActivity() // 버튼 클릭 시 ChartActivity 시작
        }

    }
    // BlinkActivity를 시작하는 함수
    private fun startBlinkActivity() {
        val intent = Intent(this, BlinkActivity::class.java) // BlinkActivity로 이동하기 위한 Intent 생성
        startActivity(intent) // BlinkActivity 시작
    }

    // TimerActivity를 시작하는 함수
    private fun startTimerActivity() {
        val intent = Intent(this, TimerActivity::class.java) // TimerActivity로 이동하기 위한 Intent 생성
        startActivity(intent) // TimerActivity 시작
    }

    // CalenderActivity를 시작하는 함수
    private fun startCalendarActivity() {
        val intent = Intent(this, CalendarActivity::class.java) // CalenderActivity로 이동하기 위한 Intent 생성
        startActivity(intent) // CalendarActivity 시작
    }

    // ChartActivity를 시작하는 함수
    private fun startChartActivity() {
        val intent = Intent(this, ChartActivity::class.java) // ChartActivity로 이동하기 위한 Intent 생성
        startActivity(intent) // ChartActivity 시작
    }

    // CardActivity를 시작하는 함수
    private fun startCardActivity() {
        val exampleRatePerMinute = 13 // 예제 분당 횟수 값 (이 값을 실제로 측정된 값으로 변경해야 합니다)
        val intent = Intent(this, CardActivity::class.java).apply {
            putExtra("RATE_PER_MINUTE", exampleRatePerMinute)
        }
        startActivity(intent) // CardActivity 시작
    }

    // ResultActivity를 시작하는 함수
    private fun startResultActivity() {
        val intent = Intent(this, ResultActivity::class.java) // ResultActivity로 이동하기 위한 Intent 생성
        startActivity(intent) // ResultActivity 시작
    }

}