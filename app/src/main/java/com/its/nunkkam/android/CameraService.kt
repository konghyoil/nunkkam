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
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import kotlin.math.abs

class CameraService : LifecycleService() {
    private lateinit var cameraExecutor: ScheduledExecutorService
    private lateinit var faceLandmarker: FaceLandmarker
    private var imageAnalysis: ImageAnalysis? = null
    private val binder = LocalBinder()
    private var timeLeftInMillis: Long = 1200000

    private var blinkCount = 0
    private var frameSkipCount = 0
    private val FRAME_SKIP_THRESHOLD = 30

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
        return binder
    }

    private fun updateNotification(blinkCount: Int, timeLeft: Long) {
        var timeFormatted = getTimeLeftInMillis()
        Log.d(TAG, "Updating notification: Blinks = $blinkCount, Time Left = $timeFormatted") // 로그 추가

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
        val cameraExecutor = Executors.newSingleThreadScheduledExecutor()
        setupFaceLandmarker()

        // 주기적으로 알림을 업데이트하는 스케줄러 추가
        cameraExecutor.scheduleWithFixedDelay({
            Log.d(TAG, "Scheduled task running") // 로그 추가
            updateNotification(blinkCount, timeLeftInMillis)
        }, 0, 1, TimeUnit.SECONDS) // 알림 업데이트 주기를 1분으로 설정
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
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            // Preview 객체 생성
            val preview = Preview.Builder().build()

            imageAnalysis = ImageAnalysis.Builder().build().also {
                it.setAnalyzer(cameraExecutor) { imageProxy ->
                    if (!viewFinder.isAttachedToWindow) {
                        imageProxy.close()
                        return@setAnalyzer
                    }

                    frameSkipCount++
                    if (frameSkipCount % FRAME_SKIP_THRESHOLD == 0) {
                        val bitmap = imageProxy.toBufferBitmap()
                        val mpImage = BitmapImageBuilder(bitmap).build()
                        faceLandmarker.detectAsync(mpImage, imageProxy.imageInfo.timestamp)
                    }
                    imageProxy.close()
                }
            }

            // PreviewView 연결
            preview.setSurfaceProvider(viewFinder.surfaceProvider)

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this, CameraSelector.DEFAULT_FRONT_CAMERA, preview, imageAnalysis
                )
            } catch (exc: Exception) {
                Log.e(TAG, "카메라 바인딩 실패", exc)
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
        return averageEyeOpenness < BLINK_THRESHOLD
    }

    // 눈 개폐 정도 계산 메서드
    private fun calculateEyeOpenness(top: NormalizedLandmark, bottom: NormalizedLandmark, inner: NormalizedLandmark, outer: NormalizedLandmark): Float {
        val verticalDistance = abs(top.y() - bottom.y())
        val horizontalDistance = abs(outer.x() - inner.x())
        return verticalDistance / horizontalDistance
    }

    // 눈 깜빡임 감지 결과 처리 메서드
    private fun handleFaceLandmarkerResult(result: FaceLandmarkerResult, image: MPImage) {
        callback?.onFaceLandmarkerResult(result, image)

        if (result.faceLandmarks().isEmpty()) return

        if (detectBlink(result)) {
            incrementBlinkCount()
            updateNotification(blinkCount, timeLeftInMillis)
        }
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
        if (!isForeground) {
            startForeground(NOTIFICATION_ID, createNotification())
            isForeground = true
        }
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
        private const val TAG = "CameraService"
    }
}
