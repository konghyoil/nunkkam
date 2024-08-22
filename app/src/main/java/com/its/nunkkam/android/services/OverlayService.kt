package com.its.nunkkam.android.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import androidx.camera.view.PreviewView
import androidx.core.app.NotificationCompat
import com.its.nunkkam.android.R

class OverlayService : Service() {
    private lateinit var windowManager: WindowManager
    private lateinit var overlayView: View
    private lateinit var cameraService: CameraService
    private var serviceBound = false

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as CameraService.LocalBinder
            cameraService = binder.getService()
            serviceBound = true
            setupOverlayCamera()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            serviceBound = false
        }
    }

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        createOverlayView()
        val intent = Intent(this, CameraService::class.java)
        bindService(intent, connection, Context.BIND_AUTO_CREATE)

        // Foreground 서비스로 시작
        startForeground(NOTIFICATION_ID, createNotification())
    }

    private fun createNotification(): Notification {
        val channelId = "OverlayServiceChannel"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Overlay Service", NotificationManager.IMPORTANCE_LOW)
            notificationManager.createNotificationChannel(channel)
        }

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("Overlay Service")
            .setContentText("Running in background")
            .setSmallIcon(R.drawable.ic_notification)
            .build()
    }

    private fun createOverlayView() {
        val params = WindowManager.LayoutParams(
            1, // 최소 너비
            1, // 최소 높이
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.TOP or Gravity.START

        overlayView = LayoutInflater.from(this).inflate(R.layout.overlay_layout, null)
        windowManager.addView(overlayView, params)
    }

    private fun setupOverlayCamera() {
        if (serviceBound) {
            val overlayPreviewView = overlayView.findViewById<PreviewView>(R.id.previewView)
            cameraService.switchToOverlay(overlayPreviewView)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (serviceBound) {
            unbindService(connection)
        }
        windowManager.removeView(overlayView)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val NOTIFICATION_ID = 1
    }
}