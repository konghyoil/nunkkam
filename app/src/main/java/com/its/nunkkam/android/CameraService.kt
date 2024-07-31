package com.its.nunkkam.android

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log
import androidx.camera.core.* // 카메라 관련 핵심 클래스들
import androidx.camera.lifecycle.ProcessCameraProvider // 카메라 프로바이더 클래스
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import com.google.mediapipe.framework.image.BitmapImageBuilder // MediaPipe 이미지 빌더 클래스
import com.google.mediapipe.framework.image.MPImage
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarker // 얼굴 랜드마크 감지 클래스
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarkerResult
import java.util.concurrent.Executors
import android.content.Context
import android.os.Build
import androidx.camera.view.PreviewView // 카메라 미리보기 뷰 클래스
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import java.util.concurrent.ExecutorService
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import kotlin.math.abs

class CameraService : LifecycleService() {
    private lateinit var faceLandmarker: FaceLandmarker // 얼굴 랜드마크 감지기
    private lateinit var cameraExecutor: ExecutorService // 카메라 작업 실행을 위한 실행자
    private var imageAnalysis: ImageAnalysis? = null
    private val binder = LocalBinder()
    private var timeLeftInMillis: Long = 1200000

    private var blinkCount = 0 // 눈 깜빡임 총 횟수를 저장하는 변수

    private var isForeground = false // 포그라운드 여부를 나타내는 변수

    // 깜빡임 임계값 정의
    private val BLINK_THRESHOLD = 0.2

    fun getBlinkCount(): Int {
        return blinkCount
    }

    fun getTimeLeftInMillis(): Long {
        return timeLeftInMillis
    }

    fun incrementBlinkCount() {
        blinkCount++
    }

    // 콜백 인터페이스 정의
    interface CameraCallback {
        fun onFaceLandmarkerResult(result: FaceLandmarkerResult, image: MPImage)
    }

    inner class LocalBinder : Binder() {
        fun getService(): CameraService = this@CameraService
    }

    override fun onBind(intent: Intent): IBinder {
        super.onBind(intent)
        Log.d(TAG, "CameraService: onBind called")
        return binder
    }

    private fun updateNotification(blinkCount: Int, timeLeft: Long) {
        var timeFormatted = getTimeLeftInMillis()
        Log.d(TAG, "CameraService: Updating notification: Blinks = $blinkCount, Time Left = $timeFormatted") // 로그 추가

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Camera Service")
            .setContentText("Blinks: $blinkCount, Time Left: $timeFormatted")
            .setSmallIcon(R.drawable.eye_open) // 적절한 아이콘으로 변경 필요
            .setSound(null) // 알림을 무음으로 설정
            .setOngoing(true) // 알림을 지속적으로 표시
            .setPriority(NotificationCompat.PRIORITY_HIGH) // 알림 우선순위 설정
            .setCategory(NotificationCompat.CATEGORY_SERVICE) // 알림 카테고리 설정
            .build()

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    override fun onCreate() {
        super.onCreate()
        try {
            Log.d(TAG, "CameraService: CameraService onCreate called")

            createNotificationChannel()
            val notificationIntent = Intent(this, BlinkActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE)

            val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Camera Service")
                .setContentText("Running in the background")
                .setSmallIcon(R.drawable.eye_open)
                .setContentIntent(pendingIntent)
                .build()

            startForeground(1, notification)

            // 카메라 작업을 위한 단일 스레드 스케줄링 실행자 생성
            val localExecutor = Executors.newSingleThreadScheduledExecutor()
            cameraExecutor = localExecutor
            setupFaceLandmarker()

            // 주기적으로 알림을 업데이트하는 스케줄러 추가
            localExecutor.scheduleWithFixedDelay({
                Log.d(TAG, "CameraService: Scheduled task running")
                updateNotification(blinkCount, timeLeftInMillis)
            }, 0, 1, TimeUnit.MINUTES)

            Log.d(TAG, "CameraService: onCreate completed successfully")
        } catch (e: Exception) {
            Log.e(TAG, "CameraService: Error in onCreate", e)
            // 여기서 서비스를 중지하거나 다른 오류 처리 로직을 추가할 수 있습니다.
            stopSelf()
        }
    }
    private fun createNotificationChannel() {
        val serviceChannel = NotificationChannel(
            CHANNEL_ID,
            "Camera Service Channel",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(serviceChannel)
    }

    private fun setupFaceLandmarker() {
        // FaceLandmarker 설정
        val baseOptions = BaseOptions.builder() // [외부] 얼굴 랜드마크 모델 파일(face_landmarker.task) 가져오기
            .setModelAssetPath("face_landmarker.task") // 모델 파일 경로 설정
            .build() // BaseOptions 객체 생성 및 반환
        val options = FaceLandmarker.FaceLandmarkerOptions.builder()
            .setBaseOptions(baseOptions) // 기본 옵션 설정
            .setRunningMode(RunningMode.LIVE_STREAM) // 실시간 스트림 모드로 설정
            .setNumFaces(1) // 감지할 얼굴 수 설정
            .setResultListener(this::handleFaceLandmarkerResult) // 결과 처리 리스너 설정
            .build() // FaceLandmarkerOptions 객체 생성 및 반환
        faceLandmarker = FaceLandmarker.createFromOptions(this, options) // FaceLandmarker 객체 생성
    }

    // 카메라 리소스 유지
    fun startCamera(viewFinder: PreviewView) {
        Log.d(TAG, "CameraService: startCamera called")

        if (!this::cameraExecutor.isInitialized) {
            Log.d(TAG, "CameraService: Initializing cameraExecutor")
            cameraExecutor = Executors.newSingleThreadExecutor()
        }

        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            try {
                val cameraProvider = cameraProviderFuture.get()
                Log.d(TAG, "CameraService: CameraProvider obtained")

                // Preview 객체 생성
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(viewFinder.surfaceProvider)
                }
                Log.d(TAG, "CameraService: Preview built and surface provider set")

                // ImageAnalysis 객체 생성 및 설정
                imageAnalysis = ImageAnalysis.Builder().build().also {
                    it.setAnalyzer(cameraExecutor) { imageProxy ->
                        // viewFinder가 창에 부착되지 않은 경우 이미지를 처리하지 않음
                        if (!viewFinder.isAttachedToWindow) {
                            imageProxy.close()
                            return@setAnalyzer
                        }

                        // 프레임 분석 수행
                        val bitmap = imageProxy.toBufferBitmap()
                        val mpImage = BitmapImageBuilder(bitmap).build()
                        faceLandmarker.detectAsync(mpImage, imageProxy.imageInfo.timestamp)
                        imageProxy.close()
                    }
                }
                Log.d(TAG, "CameraService: ImageAnalysis configured")

                val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA
                Log.d(TAG, "CameraService: CameraSelector set to front camera")

                // 모든 이전 바인딩 해제
                cameraProvider.unbindAll()
                Log.d(TAG, "CameraService: Unbound all use cases")

                // 카메라와 Preview, ImageAnalysis 연결
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageAnalysis
                )
                Log.d(TAG, "CameraService: Camera bound to lifecycle with preview and image analysis")
            } catch (exc: Exception) {
                Log.e(TAG, "CameraService: Use case binding failed", exc)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    fun stopCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            cameraProvider.unbindAll()
        }, ContextCompat.getMainExecutor(this))
    }

    private var lastBlinkTime = 0L
    private val MIN_BLINK_INTERVAL = 100 // 100ms

    // 눈 깜빡임 감지 메서드
    private fun detectBlink(result: FaceLandmarkerResult): Boolean {
        val landmarks = result.faceLandmarks()[0]

        // 눈 랜드마크 좌표 가져오기
        val leftEyeTop = landmarks[159]     // 159: 왼쪽 눈 위쪽 점
        val leftEyeBottom = landmarks[145]  // 145: 왼쪽 눈 아래쪽 점
        val leftEyeInner = landmarks[33]    // 33: 왼쪽 눈 안쪽 점
        val leftEyeOuter = landmarks[133]   // 133: 왼쪽 눈 바깥쪽 점
        val rightEyeTop = landmarks[386]    // 386: 오른쪽 눈 위쪽 점
        val rightEyeBottom = landmarks[374] // 374: 오른쪽 눈 아래쪽 점
        val rightEyeInner = landmarks[263]  // 263: 오른쪽 눈 안쪽 점
        val rightEyeOuter = landmarks[362]  // 362: 오른쪽 눈 바깥쪽 점

        // 눈 개폐 정도 계산
        val leftEyeOpenness = calculateEyeOpenness(leftEyeTop, leftEyeBottom, leftEyeInner, leftEyeOuter)
        val rightEyeOpenness = calculateEyeOpenness(rightEyeTop, rightEyeBottom, rightEyeInner, rightEyeOuter)
        val averageEyeOpenness = (leftEyeOpenness + rightEyeOpenness) / 2

        val currentTime = System.currentTimeMillis()
        val isBlink = averageEyeOpenness < BLINK_THRESHOLD

        Log.d(TAG, "CameraService: Average eye openness: $averageEyeOpenness, Threshold: $BLINK_THRESHOLD")

        if (isBlink && (currentTime - lastBlinkTime) > MIN_BLINK_INTERVAL) {
            lastBlinkTime = currentTime
            return true
        }

        return false
    }

    // 눈 개폐 정도 계산 메서드
    private fun calculateEyeOpenness(top: NormalizedLandmark, bottom: NormalizedLandmark, inner: NormalizedLandmark, outer: NormalizedLandmark): Float {
        val verticalDistance = abs(top.y() - bottom.y())
        val horizontalDistance = abs(outer.x() - inner.x())
        return verticalDistance / horizontalDistance
    }

    // 눈 깜빡임 감지 결과 처리 메서드
    private fun handleFaceLandmarkerResult(result: FaceLandmarkerResult, image: MPImage) {
        Log.d(TAG, "CameraService: Face landmark result received")
        if (result.faceLandmarks().isEmpty()) {
            Log.d(TAG, "CameraService: No face landmarks detected")
            return
        }

        val blinked = detectBlink(result)
        Log.d(TAG, "CameraService: Blink detected: $blinked")
        if (blinked) {
            incrementBlinkCount()
            Log.d(TAG, "CameraService: Blink count incremented: $blinkCount")
            updateNotification(blinkCount, timeLeftInMillis)
        }

        callback?.onFaceLandmarkerResult(result, image)
    }

    private var callback: CameraCallback? = null

    fun setCallback(callback: CameraCallback) {
        this.callback = callback
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    // 서비스가 백그라운드에서도 계속 실행되도록 설정
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        Log.d(TAG, "CameraService: onStartCommand called")

        // Foreground 서비스로 설정

        try {
            // 알림 생성
            val notification: Notification = createNotification()
            startForeground(NOTIFICATION_ID, notification)
            setupFaceLandmarker() // MediaPipe FaceLandmarker 설정
            isForeground = true
        } catch (e: Exception) {
            Log.e(TAG, "CameraService: Error in onStartCommand", e)
        }
//        return super.onStartCommand(intent, flags, startId)
        return START_STICKY // 서비스가 중단되었을 때 다시 시작하도록 설정
    }

    override fun onTaskRemoved(rootIntent: Intent) {
        super.onTaskRemoved(rootIntent)
        stopSelf()
    }

    private fun createNotification(): Notification {
        // 알림 채널 생성
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Camera Service",
            NotificationManager.IMPORTANCE_LOW // 또는 NotificationManager.IMPORTANCE_DEFAULT
        )
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)

        // Notification 객체 생성 및 반환
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Camera Service")
            .setContentText("카메라 서비스가 실행 중입니다.")
            .setSmallIcon(R.drawable.eye_open) // 적절한 아이콘으로 변경 필요
            .setOngoing(true) // 사용자가 직접 알림을 제거할 수 없도록 설정
            .build()
    }

    companion object {
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "CameraServiceChannel"
        private const val TAG = "CameraXApp"
    }
}