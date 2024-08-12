package com.its.nunkkam.android

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import android.view.WindowManager

class PopupFragment : DialogFragment() {

    private lateinit var imageView: ImageView
    private lateinit var textDescription: TextView
    private lateinit var pageIndicator: TextView
    private var currentPage = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, R.style.PopupDialogTheme)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_popup, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        imageView = view.findViewById(R.id.image)
        textDescription = view.findViewById(R.id.text_description)
        pageIndicator = view.findViewById(R.id.page_indicator)

        view.findViewById<View>(R.id.previous_button).setOnClickListener { showPreviousPage() }
        view.findViewById<View>(R.id.next_button).setOnClickListener { showNextPage() }

        updatePage()
    }

    override fun onStart() {
        super.onStart()

        // 팝업 창의 크기를 6번 페이지 기준으로 고정
        val imageHeight = resources.getDimensionPixelSize(R.dimen.image_height_page_6)
        val textHeight = resources.getDimensionPixelSize(R.dimen.text_height_page_6)
        val totalHeight = imageHeight + textHeight + resources.getDimensionPixelSize(R.dimen.popup_additional_height)

        dialog?.window?.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            totalHeight
        )
    }

    private fun showPreviousPage() {
        if (currentPage > 1) {
            currentPage--
            updatePage()
        }
    }

    private fun showNextPage() {
        if (currentPage < 6) {
            currentPage++
            updatePage()
        }
    }

    private fun updatePage() {
        val (imageRes, description) = when (currentPage) {
            1 -> Pair(R.drawable.step1, "1. 눈을 뜬 상태에서 안구를 위아래로 움직인다. 5회 이상 반복한다.")
            2 -> Pair(R.drawable.step2, "2. 눈을 감았다가 뜨고, 다시 1번 과정을 반복한다.")
            3 -> Pair(R.drawable.step3, "3. 이번에는 안구를 오른쪽에서 왼쪽으로 움직인다. 5회 이상 반복한다.")
            4 -> Pair(R.drawable.step4, "4. 눈을 감았다가 뜨고, 다시 3번 과정을 반복한다.")
            5 -> Pair(R.drawable.step5, "5. 양 손바닥을 비벼서 따뜻하게 만든 다음 달걀 하나를 쥔 것처럼 양손을 오목하게 만들어 눈을 가볍게 덮는다.")
            6 -> Pair(R.drawable.step6, "6. 양손으로 눈을 덮은 상태에서 (눈을 뜨고) 눈동자를 시계 방향으로 천천히 돌린 다음 반시계 방향으로 돌린다. 시계 방향과 반시계 방향을 번갈아가며 5회 반복한다.")
            else -> Pair(R.drawable.step1, "")
        }

        imageView.setImageResource(imageRes)
        textDescription.text = description
        pageIndicator.text = "$currentPage/6"
    }

    companion object {
        fun newInstance() = PopupFragment()
    }
}
