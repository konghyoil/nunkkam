package com.its.nunkkam.android                                      // 패키지 선언

import android.os.Bundle                                             // Bundle 클래스 임포트
import android.view.LayoutInflater                                   // LayoutInflater 클래스 임포트
import android.view.View                                             // View 클래스 임포트
import android.view.ViewGroup                                        // ViewGroup 클래스 임포트
import android.widget.ImageView                                      // ImageView 위젯 임포트
import android.widget.TextView                                       // TextView 위젯 임포트
import androidx.fragment.app.DialogFragment                          // DialogFragment 클래스 임포트

class PopupFragment : DialogFragment() {                             // DialogFragment를 상속받는 PopupFragment 클래스 정의

    private lateinit var imageView: ImageView                        // 이미지를 표시할 ImageView 선언
    private lateinit var textDescription: TextView                   // 설명을 표시할 TextView 선언
    private lateinit var pageIndicator: TextView                     // 페이지 번호를 표시할 TextView 선언
    private var currentPage = 1                                      // 현재 페이지 번호

    override fun onCreate(savedInstanceState: Bundle?) {             // Fragment 생성 시 호출되는 메서드
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, R.style.PopupDialogTheme)           // 다이얼로그 스타일 설정
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_popup, container, false)  // 레이아웃 인플레이트
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {  // 뷰 생성 후 호출되는 메서드
        super.onViewCreated(view, savedInstanceState)

        imageView = view.findViewById(R.id.image)                    // ImageView 초기화
        textDescription = view.findViewById(R.id.text_description)   // 설명 TextView 초기화
        pageIndicator = view.findViewById(R.id.page_indicator)       // 페이지 인디케이터 TextView 초기화

        view.findViewById<View>(R.id.previous_button).setOnClickListener { showPreviousPage() }  // 이전 버튼 클릭 리스너 설정
        view.findViewById<View>(R.id.next_button).setOnClickListener { showNextPage() }          // 다음 버튼 클릭 리스너 설정
        view.findViewById<View>(R.id.x_button).setOnClickListener { dismiss() }                  // 닫기 버튼 클릭 리스너 설정

        updatePage()                                                 // 초기 페이지 업데이트
    }

    private fun showPreviousPage() {                                 // 이전 페이지 표시 메서드
        if (currentPage > 1) {
            currentPage--
            updatePage()
        }
    }

    private fun showNextPage() {                                     // 다음 페이지 표시 메서드
        if (currentPage < 6) {
            currentPage++
            updatePage()
        }
    }

    private fun updatePage() {                                       // 페이지 내용 업데이트 메서드
        val (imageRes, description) = when (currentPage) {           // 현재 페이지에 따른 이미지와 설명 선택
            1 -> Pair(R.drawable.step1, "1. 눈을 뜬 상태에서 안구를 위아래로 움직인다. 5회 이상 반복한다.")
            2 -> Pair(R.drawable.step2, "2. 눈을 감았다가 뜨고, 다시 1번 과정을 반복한다.")
            3 -> Pair(R.drawable.step3, "3. 이번에는 안구를 오른쪽에서 왼쪽으로 움직인다. 5회 이상 반복한다.")
            4 -> Pair(R.drawable.step4, "4. 눈을 감았다가 뜨고, 다시 3번 과정을 반복한다.")
            5 -> Pair(R.drawable.step5, "5. 양 손바닥을 비벼서 따뜻하게 만든 다음 달걀 하나를 쥔 것처럼 양손을 오목하게 만들어 눈을 가볍게 덮는다.")
            6 -> Pair(R.drawable.step6, "6. 양손으로 눈을 덮은 상태에서 (눈을 뜨고) 눈동자를 시계 방향으로 천천히 돌린 다음 반시계 방향으로 돌린다. 시계 방향과 반시계 방향을 번갈아가며 5회 반복한다.")
            else -> Pair(R.drawable.step1, "")
        }

        imageView.setImageResource(imageRes)                         // 선택된 이미지 설정
        textDescription.text = description                           // 선택된 설명 텍스트 설정
        pageIndicator.text = "$currentPage/6"                        // 페이지 인디케이터 텍스트 업데이트
    }

    companion object {
        fun newInstance() = PopupFragment()                          // PopupFragment 인스턴스 생성 메서드
    }
}