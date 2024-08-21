package com.its.nunkkam.android // 패키지 선언: 이 코드가 속한 패키지를 지정

// 필요한 라이브러리들을 가져오기
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent // 지연된 인텐트를 위한 클래스
import android.content.Intent // 컴포넌트 간 통신을 위한 메시지 객체
import android.os.Binder
import android.os.IBinder // 서비스와 통신하기 위한 인터페이스
import android.util.Log // 로그 출력을 위한 클래스
import androidx.camera.core.* // 카메라 관련 핵심 클래스들
import androidx.camera.lifecycle.ProcessCameraProvider // 카메라 프로바이더 클래스
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat // 컨텍스트 호환성 관련 클래스
import androidx.lifecycle.LifecycleService
import com.google.mediapipe.framework.image.MPImage // MediaPipe 이미지 클래스
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarker // 얼굴 랜드마크 감지 클래스
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarkerResult // 얼굴 랜드마크 결과 클래스
import java.util.concurrent.Executors // 실행자 생성 유틸리티 클래스
import android.content.Context // 애플리케이션 환경에 대한 정보를 제공하는 클래스
import androidx.camera.view.PreviewView // 카메라 미리보기 뷰 클래스
import java.util.concurrent.ExecutorService // 실행자 서비스 인터페이스
import java.util.concurrent.TimeUnit
import android.view.Gravity
import android.view.LayoutInflater
import android.view.WindowManager
import android.graphics.PixelFormat
import android.provider.Settings
import android.view.View
import java.util.concurrent.ScheduledExecutorService

class CameraService : LifecycleService() {
    private lateinit var faceLandmarker: FaceLandmarker // 얼굴 랜드마크 감지기
    private lateinit var cameraExecutor: ScheduledExecutorService // 카메라 작업 실행을 위한 실행자
    private var imageAnalysis: ImageAnalysis? = null
    private val binder = LocalBinder()
    private val blinkDetectionUtil = BlinkDetectionUtil()
    private lateinit var landmarkDetectionManager: LandmarkDetectionManager

    private lateinit var windowManager: WindowManager
    private lateinit var overlayView: View

    fun setLandmarkDetectionManager(manager: LandmarkDetectionManager) {
        landmarkDetectionManager = manager
    }

    private fun setupImageAnalysis() {
        imageAnalysis = ImageAnalysis.Builder().build().also {
            it.setAnalyzer(cameraExecutor) { imageProxy ->
                val mpImage = imageProxy.toMPImage() // ImageProxy를 MPImage로 변환
                landmarkDetectionManager.detectAsync(mpImage, imageProxy.imageInfo.timestamp)
                imageProxy.close()
            }
        }
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

    private fun updateNotification() {
        val intent = Intent(this, BlinkActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("눈 깜빡임 감지 중")
            .setContentText("눈 깜빡임 결과는 앱에서 확인!!") // 고정된 텍스트만 표시
            .setSmallIcon(R.drawable.app_icon)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
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
            blinkDetectionUtil.setStartTime(System.currentTimeMillis())

            val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Camera Service")
                .setContentText("Running in the background")
                .setSmallIcon(R.drawable.eye_open)
                .setContentIntent(pendingIntent)
                .build()

            startForeground(1, notification)

            // cameraExecutor 초기화
            cameraExecutor = Executors.newSingleThreadScheduledExecutor()

            // 주기적으로 알림을 업데이트하는 스케줄러 추가 (1초마다)
            cameraExecutor.scheduleWithFixedDelay({
                Log.d(TAG, "Updating notification in background")
                updateNotification()
            }, 0, 1, TimeUnit.SECONDS)

            Log.d(TAG, "CameraService: onCreate completed successfully")
        } catch (e: Exception) {
            Log.e(TAG, "CameraService: Error in onCreate", e)
            stopSelf()
        }
    }

    private fun createNotificationChannel() {
        val serviceChannel = NotificationChannel(
            CHANNEL_ID,
            "Camera Service Channel",
            NotificationManager.IMPORTANCE_LOW // 백그라운드 알림은 중요도가 낮아야 합니다.
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(serviceChannel)
    }

    private fun onFaceLandmarkerResult(result: FaceLandmarkerResult, image: MPImage) {
        Log.d(TAG, "CameraService: Face landmark result received")
        callback?.onFaceLandmarkerResult(result, image)

        // 얼굴 랜드마크 결과를 처리한 후 즉시 알림 업데이트
        updateNotification()
    }

    fun switchToViewFinder(viewFinder: PreviewView) {
        stopCamera()
        startCamera(viewFinder, null)
    }

    fun startBackgroundProcessing() {
        stopCamera()
        startCamera(null, null)
    }

    fun switchToOverlay(overlayPreviewView: PreviewView) {
        stopCamera()
        startCamera(null, overlayPreviewView)
    }

    // 카메라 리소스 유지
    fun startCamera(viewFinder: PreviewView?, overlayPreviewView: PreviewView?) {
        Log.d(TAG, "CameraService: startCamera called")

        if (!this::cameraExecutor.isInitialized) {
            Log.d(TAG, "CameraService: Initializing cameraExecutor")
            cameraExecutor = Executors.newSingleThreadScheduledExecutor()
        }

        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            try {
                val cameraProvider = cameraProviderFuture.get()
                Log.d(TAG, "CameraService: CameraProvider obtained")

                // Preview 객체 생성
                val preview = when {
                    viewFinder != null -> Preview.Builder().build().also {
                        it.setSurfaceProvider(viewFinder.surfaceProvider)
                    }
                    overlayPreviewView != null -> Preview.Builder().build().also {
                        it.setSurfaceProvider(overlayPreviewView.surfaceProvider)
                    }
                    else -> null
                }

                // ImageAnalysis 객체 생성 및 설정
                imageAnalysis = ImageAnalysis.Builder().build().also {
                    it.setAnalyzer(cameraExecutor) { imageProxy ->
                        // viewFinder가 창에 부착되지 않은 경우 이미지를 처리하지 않음
                        if (viewFinder != null) {
                            if (!viewFinder.isAttachedToWindow) {
                                imageProxy.close()
                                return@setAnalyzer
                            }
                        }

                        // 프레임 분석 수행
                        val mpImage = imageProxy.toMPImage() // ImageProxy를 MPImage로 변환
                        faceLandmarker.detectAsync(mpImage, imageProxy.imageInfo.timestamp)
                        imageProxy.close()
                    }
                }

                // ImageAnalysis 설정
                setupImageAnalysis()

                val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

                // 모든 이전 바인딩 해제
                cameraProvider.unbindAll()

                // 카메라와 Preview, ImageAnalysis 연결
                if (preview != null) {
                    cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis)
                } else {
                    cameraProvider.bindToLifecycle(this, cameraSelector, imageAnalysis)
                }

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

    private var callback: CameraCallback? = null

    fun setCallback(callback: CameraCallback) {
        this.callback = callback
        // 콜백 설정 시 즉시 알림 업데이트
        updateNotification()
    }

    private fun createOverlayView() {
        if (Settings.canDrawOverlays(this)) {
            windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
            overlayView = LayoutInflater.from(this).inflate(R.layout.overlay_layout, null)

            val params = WindowManager.LayoutParams(
                1, // 최소 너비
                1, // 최소 높이
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
            )
            params.gravity = Gravity.TOP or Gravity.START

            windowManager.addView(overlayView, params)
        } else {
            Log.e(TAG, "Overlay permission is not granted.")
        }
    }

    private fun removeOverlayView() {
        if (::windowManager.isInitialized && ::overlayView.isInitialized) {
            windowManager.removeView(overlayView)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        removeOverlayView()
        cameraExecutor.shutdown()

        try {
            if (!cameraExecutor.awaitTermination(800, TimeUnit.MILLISECONDS)) {
                Log.e(TAG, "CameraService: Executor did not terminate in the specified time.")
            }
        } catch (e: InterruptedException) {
            Log.e(TAG, "CameraService: Executor termination interrupted", e)
        }
    }

    // 서비스가 백그라운드에서도 계속 실행되도록 설정
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        Log.d(TAG, "CameraService: onStartCommand called")

        // 초기 알림 생성
        updateNotification()

//        blinkDetectionUtil.setForeground(true)

        // 오버레이 뷰 생성 시 context 전달
//        createOverlayView(this)  // context를 전달

        return START_STICKY // 서비스가 중단되었을 때 다시 시작하도록 설정
    }

    override fun onTaskRemoved(rootIntent: Intent) {
        super.onTaskRemoved(rootIntent)
        Log.d(TAG, "Task removed, stopping service")
        stopSelf() // 서비스가 백그라운드에서 종료되면 스스로 중지
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
            .setContentText("눈깜빡임 백그라운드 상태로 감지 중...")
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