package com.its.nunkkam.android

import android.content.Context
import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.test.internal.runner.junit4.statement.UiThreadStatement.runOnUiThread
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.framework.image.MPImage
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.core.Delegate
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarker
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarkerResult
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.jvm.optionals.toList

class LandmarkDetectionManager(private val context: Context) {
    private lateinit var faceLandmarker: FaceLandmarker
    private var resultListener: ((FaceLandmarkerResult) -> Unit)? = null
    private val backgroundExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private var isDetectionPaused: Boolean = false

    fun initialize(onInitialized: () -> Unit) {
        backgroundExecutor.execute {
            setupFaceLandmarker()
            Handler(Looper.getMainLooper()).post {
                onInitialized()
            }
        }
    }

    fun setResultListener(listener: (FaceLandmarkerResult) -> Unit) {
        resultListener = listener
    }

    private fun setupFaceLandmarker() {
        Log.d(TAG, "LandmarkDetectionManager: Setting up Face Landmarker")

        val baseOptionBuilder = BaseOptions.builder()
        baseOptionBuilder.setDelegate(Delegate.CPU)
        baseOptionBuilder.setModelAssetPath(AppConstants.MP_FACE_LANDMARKER_TASK)

        val baseOptions = baseOptionBuilder.build()

        try {
            val optionsBuilder = FaceLandmarker.FaceLandmarkerOptions.builder()
                .setBaseOptions(baseOptions) // 기본 옵션 설정
                .setRunningMode(RunningMode.LIVE_STREAM) // 실행 모드 설정
                .setNumFaces(AppConstants.DEFAULT_NUM_FACES) // 감지할 최대 얼굴 수 설정
                .setMinFaceDetectionConfidence(AppConstants.DEFAULT_FACE_DETECTION_CONFIDENCE) // 얼굴 감지 최소 신뢰도 설정
                .setMinFacePresenceConfidence(AppConstants.DEFAULT_FACE_PRESENCE_CONFIDENCE) // 얼굴 존재 최소 신뢰도 설정
                .setMinTrackingConfidence(AppConstants.DEFAULT_FACE_TRACKING_CONFIDENCE) // 얼굴 추적 최소 신뢰도 설정
                .setResultListener { result: FaceLandmarkerResult, inputImage: MPImage -> // LIVE_STREAM 모드에서만 설정 가능
                    Log.d(TAG, "ResultListener triggered with result: $result")
                    onLandmarkerResult(result)
                }
                .setOutputFaceBlendshapes(true) // Face Blendshapes 출력 여부 설정
                .setOutputFacialTransformationMatrixes(true)

            faceLandmarker = FaceLandmarker.createFromOptions(context, optionsBuilder.build())
            Log.d(TAG, "LandmarkDetectionManager: Face Landmarker initialized successfully")

        } catch (e: IllegalStateException) {
            Log.e(TAG, "LandmarkDetectionManager: Failed to initialize Face Landmarker", e)
        } catch (e: RuntimeException) {
            Log.e(TAG, "LandmarkDetectionManager: Error loading model or initializing Face Landmarker", e)
        } catch (e: Exception) {
            Log.e(TAG, "LandmarkDetectionManager: Error setting up Face Landmarker", e)
        }
    }

    fun pauseDetection() {
        isDetectionPaused = true
    }

    fun resumeDetection() {
        isDetectionPaused = false
    }

    fun detectAsync(mpImage: MPImage, frameTime: Long) {
        if (::faceLandmarker.isInitialized) {
            faceLandmarker.detectAsync(mpImage, frameTime)
        } else {
            Log.e(TAG, "LandmarkDetectionManager: FaceLandmarker is not initialized")
        }
    }

    private fun onLandmarkerResult(result: FaceLandmarkerResult): Boolean {
//        runOnUiThread {
//            if (result.faceLandmarks().isEmpty()) {
//                Log.d(TAG, "\"LandmarkDetectionManager: No face detected")
//                return@runOnUiThread
//            }
//
//            val faceBlendshapes = result.faceBlendshapes()
//            if (faceBlendshapes == null) {
//                Log.d(TAG, "No face blendshapes detected")
//                return@runOnUiThread
//            }
//
//            // 얼굴 감지 결과가 있을 때 호출되는 메서드
//            val blendshapesList = result.faceBlendshapes().toList()
//            Log.d(TAG, "LandmarkDetectionManager: Number of faces detected: ${blendshapesList.size}")
//            Log.d(TAG, "LandmarkDetectionManager: Result details: ${result.toString()}")
//
//            blendshapesList.forEachIndexed { index, faceBlendshapes ->
//                Log.d(TAG, "LandmarkDetectionManager: Face $index faceBlendshapes: $faceBlendshapes")
//
//                // faceBlendshapes에서 카테고리 리스트를 가져옴
//                val categories = faceBlendshapes.flatMap { it -> it.map { Category(it.categoryName(), it.score()) } }
//
//                // 각 Category 객체를 문자열로 변환하여 로그에 출력
//                val categoriesString = categories.joinToString { category -> "${category.categoryName}: ${category.score}" }
//                Log.d(TAG, "LandmarkDetectionManager: Face $index categoriesString: $categoriesString")
//            }
//
//            if (blendshapesList.isNotEmpty()) {
//                // 눈 깜빡임 여부를 감지하고 UI 업데이트 updateUI(result)
//                Log.d(TAG, "LandmarkDetectionManager: face detected in the result")
//            } else {
//                Log.d(TAG, "LandmarkDetectionManager: No face detected in the result")
//            }
//        }
        val isEyeClosed = checkIfEyesClosed(result)
        resultListener?.invoke(result)  // 결과 리스너 호출
        return isEyeClosed
    }

    /**
     * FaceLandmarkerResult(얼굴 감지 결과)를 기반으로 눈이 감긴 상태인지 확인하는 함수
     * - 'eyeBlinkLeft', 'eyeBlinkRight', 'eyeSquintLeft', 'eyeSquintRight' 블렌드셰이프의 점수를 통해 판단하며,
     * - 각 점수가 EYE_CLOSED_THRESHOLD를 초과할 경우 눈이 감긴 것으로 간주
     *
     * @param result FaceLandmarkerResult - 얼굴 감지 결과 데이터
     * @return Boolean - 두 눈이 모두 감긴 상태면 true, 그렇지 않으면 false
     */
    private var lastEyeState: Boolean = false
    private var blinkDetected: Boolean = false

    fun checkIfEyesClosed(result: FaceLandmarkerResult): Boolean {
        if (result.faceLandmarks().isEmpty()) return false

        // 각 눈의 블렌드쉐이프 지수를 가져오는 로직
        /** 블렌드쉐이프 리스트 가져오기 */
        val blendshapesList = result.faceBlendshapes().toList().getOrNull(0)

        /** 첫 번째 얼굴의 블렌드셰이프 목록 */
        val firstFaceBlendshapes = blendshapesList?.firstOrNull()

        // 각 블렌드셰이프의 점수 추출 (`Category` 클래스에서 `categoryName`과 `score` 메서드 사용)
        /** 기본값: 0f | 첫 번째 얼굴 블렌드셰이프에서 'eyeBlinkLeft' 카테고리를 찾고, 왼쪽 눈이 감긴 상태 신뢰도 점수를 가져오는 변수 */
        val leftEyeBlinkScore = firstFaceBlendshapes?.firstOrNull { it.categoryName() == "eyeBlinkLeft" }?.let { Category(it.categoryName(), it.score()) }

        /** 기본값: 0f | 첫 번째 얼굴 블렌드셰이프에서 'eyeBlinkRight' 카테고리를 찾고, 오른쪽 눈이 감긴 상태 신뢰도 점수를 가져오는 변수 */
        val rightEyeBlinkScore = firstFaceBlendshapes?.firstOrNull { it.categoryName() == "eyeBlinkRight" }?.let { Category(it.categoryName(), it.score()) }

        /** 기본값: 0f | 첫 번째 얼굴 블렌드셰이프에서 'eyeSquintLeft' 카테고리를 찾고, 왼쪽 눈이 가늘게 뜨인 상태 신뢰도 점수를 가져오는 변수 */
        val leftEyeSquintScore = firstFaceBlendshapes?.firstOrNull { it.categoryName() == "eyeSquintLeft" }?.let { Category(it.categoryName(), it.score()) }

        /** 기본값: 0f | 첫 번째 얼굴 블렌드셰이프에서 'eyeSquintRight' 카테고리를 찾고, 오른쪽 눈이 가늘게 뜨인 상태 신뢰도 점수를 가져오는 변수 */
        val rightEyeSquintScore = firstFaceBlendshapes?.firstOrNull { it.categoryName() == "eyeSquintRight" }?.let { Category(it.categoryName(), it.score()) }

        val leftEyeClosed = (leftEyeBlinkScore?.score ?: 0f) > AppConstants.EYE_CLOSED_THRESHOLD || (leftEyeSquintScore?.score ?: 0f) > AppConstants.EYE_CLOSED_THRESHOLD
        val rightEyeClosed = (rightEyeBlinkScore?.score ?: 0f) > AppConstants.EYE_CLOSED_THRESHOLD || (rightEyeSquintScore?.score ?: 0f) > AppConstants.EYE_CLOSED_THRESHOLD

        /** 왼쪽 눈과 오른쪽 눈이 모두 감긴 상태인지 확인하는 변수 */
        val currentEyeState = leftEyeClosed && rightEyeClosed

        blinkDetected = lastEyeState && !currentEyeState

        lastEyeState = currentEyeState

        Log.d(TAG, "LandmarkDetectionManager: Eye state check - Left eye blink: $leftEyeBlinkScore, Right eye blink: $rightEyeBlinkScore")
        Log.d(TAG, "LandmarkDetectionManager: Eye state check - Left eye squint: $leftEyeSquintScore, Right eye squint: $rightEyeSquintScore")
        Log.d(TAG, "Eye state: ${if(currentEyeState) "Closed" else "Open"}, Blink detected: $blinkDetected")

        return currentEyeState
    }

    /** Category 데이터 클래스를 정의하여 얼굴 랜드마크와 관련된 정보를 담기 */
    data class Category(
        val categoryName: String,
        val score: Float
    )

    fun onDestroy() {
        backgroundExecutor.shutdown()
    }

    companion object {
        private const val TAG = "Blink"
    }
}