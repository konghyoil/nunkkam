package com.its.nunkkam.android.fragments

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.its.nunkkam.android.R
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class CardFragment : Fragment() {

    private val db = Firebase.firestore  // Firestore 인스턴스 가져오기

    companion object {
        private const val ARG_RATE_PER_MINUTE = "rate_per_minute"

        // 새 CardFragment 인스턴스 생성 메서드
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
        // 이 메서드에서는 프래그먼트의 초기 상태를 설정합니다.
        // 여기서는 특별한 초기화 작업이 필요하지 않아 비어있습니다.
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // fragment_card 레이아웃 인플레이트
        return inflater.inflate(R.layout.fragment_card, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val ratePerMinute = arguments?.getInt(ARG_RATE_PER_MINUTE, 0) ?: 0
        // 데이터를 가져오고 UI를 생성하는 메서드 호출
        fetchDataAndCreateUI(ratePerMinute)
    }

    // 데이터를 가져오고 UI를 생성하는 메서드
    private fun fetchDataAndCreateUI(initialRatePerMinute: Int) {
        val sharedPreferences = requireContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        val userId = sharedPreferences.getString("user_id", null)  // SharedPreferences에서 사용자 ID 가져오기

        if (userId != null) {
            db.collection("USERS").document(userId)
                .get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        val blinkData = document.get("blinks") as? List<Map<String, Any>>
                        if (blinkData != null && blinkData.isNotEmpty()) {
                            val latestBlink = blinkData.maxByOrNull { (it["measurement_date"] as com.google.firebase.Timestamp).seconds }
                            val rate = latestBlink?.get("average_frequency_per_minute") as? Double
                            if (rate != null) {
                                createAndDisplayCardUI(rate.toInt())
                            } else {
                                createAndDisplayCardUI(initialRatePerMinute)
                            }
                        } else {
                            createAndDisplayCardUI(initialRatePerMinute)
                        }
                    } else {
                        createAndDisplayCardUI(initialRatePerMinute)
                    }
                }
                .addOnFailureListener { exception ->
                    Log.e("CardFragment", "Error fetching data", exception)  // 오류 로그 출력
                    createAndDisplayCardUI(initialRatePerMinute)
                }
        } else {
            Log.e("CardFragment", "User ID is null")  // 사용자 ID가 null인 경우 로그 출력
            createAndDisplayCardUI(initialRatePerMinute)
        }
    }

    // UI를 생성하고 표시하는 메서드
    private fun createAndDisplayCardUI(ratePerMinute: Int) {
        val cardView = LayoutInflater.from(requireContext()).inflate(R.layout.card_content, null)

        val resultTextView: TextView = cardView.findViewById(R.id.resultTextView)
        val eyeTypeTextView: TextView = cardView.findViewById(R.id.textView3)
        val eyeTypeImageView: ImageView = cardView.findViewById(R.id.imageView)
        val shareButton: Button = cardView.findViewById(R.id.shareButton)

        val rateMessage = "분당 $ratePerMinute 회"
        val (eyeTypeMessage, eyeTypeImageRes) = getEyeTypeMessageAndImage(ratePerMinute)

        resultTextView.text = rateMessage  // 깜빡임 빈도 표시
        eyeTypeTextView.text = eyeTypeMessage  // 눈 타입 메시지 표시
        eyeTypeImageView.setImageResource(eyeTypeImageRes)  // 눈 타입 이미지 표시

        shareButton.setOnClickListener {
            shareButton.visibility = View.GONE
            val bitmap = getBitmapFromView(cardView)
            bitmap?.let {
                saveBitmapAndShare(it)
            }
            shareButton.visibility = View.VISIBLE
        }

        view?.findViewById<FrameLayout>(R.id.card_container)?.addView(cardView)
    }

    // 눈 타입 메시지와 이미지 리소스를 반환하는 메서드
    private fun getEyeTypeMessageAndImage(ratePerMinute: Int): Pair<String, Int> {
        return when {
            ratePerMinute >= 25 -> "당신은 '독수리'의 눈을 \n가졌습니다!" to R.drawable.eagle
            ratePerMinute >= 20 -> "당신은 '매'의 눈을 \n가졌습니다!" to R.drawable.falcon
            ratePerMinute >= 15 -> "당신은 '올빼미'의 눈을 \n가졌습니다!" to R.drawable.owl
            ratePerMinute >= 10 -> "당신은 '고양이'의 눈을 \n가졌습니다!" to R.drawable.cat
            ratePerMinute >= 5 -> "당신은 '두더지'의 눈을 \n가졌습니다!" to R.drawable.mole
            else -> "측정값이 유효하지 \n않습니다." to R.drawable.eye_closed
        }
    }

    // View를 Bitmap으로 변환하는 메서드
    private fun getBitmapFromView(view: View): Bitmap? {
        val returnedBitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(returnedBitmap)
        val bgDrawable = view.background
        if (bgDrawable != null) {
            bgDrawable.draw(canvas)  // 배경이 있으면 배경 그리기
        } else {
            canvas.drawColor(Color.WHITE)  // 배경이 없으면 흰색으로 채우기
        }
        view.draw(canvas)  // 뷰를 캔버스에 그리기
        return returnedBitmap
    }

    // Bitmap을 파일로 저장하고 공유하는 메서드
    private fun saveBitmapAndShare(bitmap: Bitmap) {
        val context = requireContext()  // 현재 Fragment의 Context 가져오기
        try {
            // 외부 저장소의 앱 전용 디렉토리에 'fragment_image.png'라는 이름으로 파일 생성
            val file = File(context.getExternalFilesDir(null), "fragment_image.png")
            val outputStream = FileOutputStream(file)  // 파일에 쓰기 위한 OutputStream 생성
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)  // 비트맵을 PNG 형식으로 파일에 저장 (품질 100%)
            outputStream.flush()  // 출력 스트림의 버퍼를 비움
            outputStream.close()  // 출력 스트림 닫기
            shareImage(file)  // 저장된 이미지 파일 공유 메서드 호출
        } catch (e: IOException) {  // 입출력 예외 처리
            e.printStackTrace()  // 스택 트레이스 출력
        }
    }

    // 저장된 이미지 파일을 공유하는 메서드
    private fun shareImage(file: File) {
        val context = requireContext()  // 현재 Fragment의 Context 가져오기
        // FileProvider를 사용하여 파일의 Content URI 생성
        val uri = FileProvider.getUriForFile(context, "com.its.nunkkam.android.provider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {  // 공유 인텐트 생성
            type = "image/png"  // MIME 타입을 PNG 이미지로 설정
            putExtra(Intent.EXTRA_STREAM, uri)  // 공유할 파일의 URI 추가
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)  // 수신 앱에 임시 읽기 권한 부여
        }
        // 사용자에게 공유 대상을 선택할 수 있는 선택기 표시
        startActivity(Intent.createChooser(intent, "Share Image"))  // 이미지 공유 인텐트 실행
    }
}