package com.its.nunkkam.android                                      // 패키지 선언

import android.content.Context                                       // Android Context 클래스 임포트
import android.content.Intent                                        // Intent 클래스 임포트
import android.graphics.Bitmap                                       // Bitmap 클래스 임포트
import android.graphics.Canvas                                       // Canvas 클래스 임포트
import android.graphics.Color                                        // Color 클래스 임포트
import android.os.Bundle                                             // Bundle 클래스 임포트
import android.util.Log                                              // Log 클래스 임포트
import android.view.LayoutInflater                                   // LayoutInflater 클래스 임포트
import android.view.View                                             // View 클래스 임포트
import android.view.ViewGroup                                        // ViewGroup 클래스 임포트
import android.widget.Button                                         // Button 위젯 임포트
import android.widget.ImageView                                      // ImageView 위젯 임포트
import android.widget.TextView                                       // TextView 위젯 임포트
import androidx.core.content.FileProvider                            // FileProvider 클래스 임포트
import androidx.fragment.app.Fragment                                // Fragment 클래스 임포트
import com.google.firebase.firestore.FirebaseFirestore              // Firestore 클래스 임포트
import com.google.firebase.firestore.ktx.firestore                   // Firestore KTX 임포트
import com.google.firebase.ktx.Firebase                              // Firebase KTX 임포트
import java.io.File                                                  // File 클래스 임포트
import java.io.FileOutputStream                                      // FileOutputStream 클래스 임포트
import java.io.IOException                                           // IOException 클래스 임포트

class CardFragment : Fragment() {                                    // Fragment를 상속받는 CardFragment 클래스 정의

    private lateinit var resultTextView: TextView                    // 결과를 표시할 TextView
    private lateinit var eyeTypeTextView: TextView                   // 눈 타입을 표시할 TextView
    private lateinit var eyeTypeImageView: ImageView                 // 눈 타입 이미지를 표시할 ImageView
    private lateinit var shareButton: Button                         // 공유 버튼
    private val db = Firebase.firestore                              // Firestore 인스턴스 가져오기

    companion object {                                               // 동반 객체 정의
        private const val ARG_RATE_PER_MINUTE = "rate_per_minute"    // 분당 깜빡임 횟수 인자 키

        fun newInstance(ratePerMinute: Int): CardFragment {          // 새 CardFragment 인스턴스 생성 메서드
            val fragment = CardFragment()
            val args = Bundle()
            args.putInt(ARG_RATE_PER_MINUTE, ratePerMinute)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_card, container, false)  // fragment_card 레이아웃 인플레이트
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {  // 뷰 생성 후 호출되는 메서드
        super.onViewCreated(view, savedInstanceState)

        resultTextView = view.findViewById(R.id.resultTextView)      // 결과 TextView 초기화
        eyeTypeTextView = view.findViewById(R.id.textView3)          // 눈 타입 TextView 초기화
        eyeTypeImageView = view.findViewById(R.id.imageView)         // 눈 타입 ImageView 초기화
        shareButton = view.findViewById(R.id.shareButton)            // 공유 버튼 초기화

        shareButton.setOnClickListener {                             // 공유 버튼 클릭 리스너 설정
            val bitmap = getBitmapFromView(view)                     // 현재 뷰를 비트맵으로 변환
            bitmap?.let {
                saveBitmapAndShare(it)                               // 비트맵 저장 및 공유
            }
        }

        val sharedPreferences = requireContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        val userId = sharedPreferences.getString("user_id", null)    // SharedPreferences에서 사용자 ID 가져오기
        val isGoogleLogin = sharedPreferences.getBoolean("is_google_login", false)  // Google 로그인 여부 확인

        if (userId != null) {
            fetchLatestRatePerMinute(userId)                         // 최신 깜빡임 빈도 데이터 가져오기
        } else {
            Log.e("CardFragment", "User ID is null")                 // 사용자 ID가 null인 경우 로그 출력
        }

        view.findViewById<TextView>(R.id.popupButton).setOnClickListener {  // 팝업 버튼 클릭 리스너 설정
            showPopup()                                              // 팝업 표시
        }
    }

    private fun getBitmapFromView(view: View): Bitmap? {             // View를 Bitmap으로 변환하는 메서드
        val returnedBitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(returnedBitmap)
        val bgDrawable = view.background
        if (bgDrawable != null) {
            bgDrawable.draw(canvas)                                  // 배경이 있으면 배경 그리기
        } else {
            canvas.drawColor(Color.WHITE)                            // 배경이 없으면 흰색으로 채우기
        }
        view.draw(canvas)                                            // 뷰를 캔버스에 그리기
        return returnedBitmap
    }

    private fun saveBitmapAndShare(bitmap: Bitmap) {                 // Bitmap을 파일로 저장하고 공유하는 메서드
        val context = requireContext()                               // 현재 Fragment의 Context 가져오기
        try {
            // 외부 저장소의 앱 전용 디렉토리에 'fragment_image.png'라는 이름으로 파일 생성
            val file = File(context.getExternalFilesDir(null), "fragment_image.png")
            val outputStream = FileOutputStream(file)                // 파일에 쓰기 위한 OutputStream 생성
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)  // 비트맵을 PNG 형식으로 파일에 저장 (품질 100%)
            outputStream.flush()                                     // 출력 스트림의 버퍼를 비움
            outputStream.close()                                     // 출력 스트림 닫기
            shareImage(file)                                         // 저장된 이미지 파일 공유 메서드 호출
        } catch (e: IOException) {                                   // 입출력 예외 처리
            e.printStackTrace()                                      // 스택 트레이스 출력
        }
    }

    private fun shareImage(file: File) {                             // 저장된 이미지 파일을 공유하는 메서드
        val context = requireContext()                               // 현재 Fragment의 Context 가져오기
        // FileProvider를 사용하여 파일의 Content URI 생성
        val uri = FileProvider.getUriForFile(context, "com.its.nunkkam.android.provider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {              // 공유 인텐트 생성
            type = "image/png"                                       // MIME 타입을 PNG 이미지로 설정
            putExtra(Intent.EXTRA_STREAM, uri)                       // 공유할 파일의 URI 추가
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)          // 수신 앱에 임시 읽기 권한 부여
        }
        // 사용자에게 공유 대상을 선택할 수 있는 선택기 표시
        startActivity(Intent.createChooser(intent, "Share Image"))   // 이미지 공유 인텐트 실행
    }

    private fun showPopup() {                                        // 팝업을 표시하는 메서드
        val popupFragment = PopupFragment.newInstance()
        popupFragment.show(childFragmentManager, "popup")            // PopupFragment를 다이얼로그로 표시
    }

    private fun fetchLatestRatePerMinute(userId: String) {           // Firestore에서 최신 눈 깜빡임 데이터를 가져오는 메서드
        db.collection("USERS").document(userId)                      // 'USERS' 컬렉션에서 userId에 해당하는 문서 참조
            .get()                                                   // 문서 가져오기
            .addOnSuccessListener { document ->                      // 문서 가져오기 성공 시 실행되는 콜백
                if (document != null && document.exists()) {         // 문서가 존재하는지 확인
                    val blinkData = document.get("blinks") as? List<Map<String, Any>>  // 'blinks' 필드에서 데이터 가져오기
                    if (blinkData != null && blinkData.isNotEmpty()) {  // 깜빡임 데이터가 존재하고 비어있지 않은지 확인
                        val latestBlink = blinkData.maxByOrNull { (it["measurement_date"] as com.google.firebase.Timestamp).seconds }
                        // 가장 최근의 깜빡임 데이터 찾기 (measurement_date 기준)
                        val rate = latestBlink?.get("average_frequency_per_minute") as? Double  // 평균 깜빡임 빈도 가져오기
                        if (rate != null) {
                            displayResult(rate.toInt())              // 결과 표시 (정수로 변환)
                        } else {
                            Log.e("CardFragment", "Rate per minute is null")  // 깜빡임 빈도가 null인 경우 로그 출력
                        }
                    } else {
                        Log.e("CardFragment", "Blink data is empty or null")  // 깜빡임 데이터가 비어있거나 null인 경우 로그 출력
                    }
                } else {
                    Log.e("CardFragment", "Document does not exist")  // 문서가 존재하지 않는 경우 로그 출력
                }
            }
            .addOnFailureListener { exception ->                     // 데이터 가져오기 실패 시 실행되는 콜백
                Log.e("CardFragment", "Error fetching data", exception)  // 오류 로그 출력
            }
    }

    private fun getEyeTypeMessageAndImage(ratePerMinute: Int): Pair<String, Int> {  // 눈 타입 메시지와 이미지 리소스를 반환하는 메서드
        return when {
            ratePerMinute >= 15 -> "당신은 '독수리'의 눈을 가졌습니다!" to R.drawable.eagle
            ratePerMinute >= 12 -> "당신은 '매'의 눈을 가졌습니다!" to R.drawable.falcon
            ratePerMinute >= 9 -> "당신은 '올빼미'의 눈을 가졌습니다!" to R.drawable.owl
            ratePerMinute >= 6 -> "당신은 '고양이'의 눈을 가졌습니다!" to R.drawable.cat
            ratePerMinute >= 3 -> "당신은 '두더지'의 눈을 가졌습니다!" to R.drawable.mole
            else -> "측정값이 유효하지 않습니다." to R.drawable.eye_closed
        }
    }

    private fun displayResult(ratePerMinute: Int) {                  // 결과를 화면에 표시하는 메서드
        val rateMessage = "분당 $ratePerMinute 회"
        val (eyeTypeMessage, eyeTypeImageRes) = getEyeTypeMessageAndImage(ratePerMinute)
        resultTextView.visibility = View.VISIBLE
        resultTextView.text = rateMessage                            // 깜빡임 빈도 표시
        eyeTypeTextView.text = eyeTypeMessage                        // 눈 타입 메시지 표시
        eyeTypeImageView.setImageResource(eyeTypeImageRes)           // 눈 타입 이미지 표시
    }
}