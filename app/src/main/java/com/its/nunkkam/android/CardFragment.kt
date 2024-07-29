package com.its.nunkkam.android

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase


class CardFragment : Fragment() {

    private lateinit var resultTextView: TextView
    private lateinit var eyeTypeTextView: TextView
    private lateinit var eyeTypeImageView: ImageView
    private val db = Firebase.firestore

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

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_card, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        resultTextView = view.findViewById(R.id.resultTextView)
        eyeTypeTextView = view.findViewById(R.id.textView3)
        eyeTypeImageView = view.findViewById(R.id.imageView)

        val sharedPreferences = requireContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        val userId = sharedPreferences.getString("user_id", null)
        val isGoogleLogin = sharedPreferences.getBoolean("is_google_login", false)

        if (userId != null) {
            UserManager.initialize(requireContext(), userId, isGoogleLogin)
            fetchLatestRatePerMinute(userId)
        } else {
            Log.e("CardFragment", "User ID is null")
        }
    }


    // Firestore에서 최신 데이터를 가져오는 함수
    private fun fetchLatestRatePerMinute(userId: String) {
        db.collection("USERS").document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val blinkData = document.get("blinks") as? List<Map<String, Any>>
                    if (blinkData != null && blinkData.isNotEmpty()) {
                        val latestBlink = blinkData.maxByOrNull { (it["measurement_date"] as com.google.firebase.Timestamp).seconds }
                        val rate = latestBlink?.get("average_frequency_per_minute") as? Double
                        if (rate != null) {
                            displayResult(rate.toInt())
                        } else {
                            Log.e("CardFragment", "Rate per minute is null")
                        }
                    } else {
                        Log.e("CardFragment", "Blink data is empty or null")
                    }
                } else {
                    Log.e("CardFragment", "Document does not exist")
                }
            }
            .addOnFailureListener { exception ->
                Log.e("CardFragment", "Error fetching data", exception)
            }
    }

    // 특정 분당 횟수에 따른 메시지와 이미지 리소스를 반환하는 함수
    private fun getEyeTypeMessageAndImage(ratePerMinute: Int): Pair<String, Int> {
        return when {
            ratePerMinute >= 15 -> "당신은 '독수리'의 눈을 가졌습니다!" to R.drawable.eagle
            ratePerMinute >= 12 -> "당신은 '매'의 눈을 가졌습니다!" to R.drawable.falcon
            ratePerMinute >= 9 -> "당신은 '올빼미'의 눈을 가졌습니다!" to R.drawable.owl
            ratePerMinute >= 6 -> "당신은 '고양이'의 눈을 가졌습니다!" to R.drawable.cat
            ratePerMinute >= 3 -> "당신은 '두더지'의 눈을 가졌습니다!" to R.drawable.mole
            else -> "측정값이 유효하지 않습니다." to R.drawable.eye_closed
        }
    }

    // 특정 분당 횟수와 메시지를 화면에 표시하는 함수
    private fun displayResult(ratePerMinute: Int) {
        val rateMessage = "분당 $ratePerMinute 회"
        val (eyeTypeMessage, eyeTypeImageRes) = getEyeTypeMessageAndImage(ratePerMinute)
        resultTextView.visibility = View.VISIBLE
        resultTextView.text = rateMessage
        eyeTypeTextView.text = eyeTypeMessage
        eyeTypeImageView.setImageResource(eyeTypeImageRes)
    }
}
