package com.its.nunkkam.android

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment

class CardFragment : Fragment() {

    private var ratePerMinute: Int = 0

    companion object {
        private const val ARG_RATE_PER_MINUTE = "rate_per_minute"

        fun newInstance(ratePerMinute: Int): CardFragment {
            val fragment = CardFragment()
            val args = Bundle()
            args.putInt(ARG_RATE_PER_MINUTE, ratePerMinute)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            ratePerMinute = it.getInt(ARG_RATE_PER_MINUTE, 0)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_card, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
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
        val resultTextView: TextView = requireView().findViewById(R.id.resultTextView)
        val message = "분당 $ratePerMinute 회\n${getEyeTypeMessage(ratePerMinute)}"
        resultTextView.text = message
    }
}