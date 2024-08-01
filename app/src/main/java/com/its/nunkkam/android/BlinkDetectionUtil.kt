package com.its.nunkkam.android

import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import kotlin.math.abs

class BlinkDetectionUtil {
    companion object {
        const val BLINK_THRESHOLD_CLOSE = 0.20 // 눈을 감은 것으로 판단하는 임계값
        const val BLINK_THRESHOLD_OPEN = 0.30  // 눈을 뜬 것으로 판단하는 임계값
        private const val MIN_BLINK_INTERVAL = 100 // 100ms

        // 왼쪽 눈의 랜드마크 인덱스
        private val LEFT_EYE_INDICES = listOf(159, 145, 33, 133)

        // 오른쪽 눈의 랜드마크 인덱스
        private val RIGHT_EYE_INDICES = listOf(386, 374, 263, 362)

        private var lastBlinkState = false
        private var lastBlinkTime = 0L

        fun detectBlink(landmarks: List<NormalizedLandmark>): Boolean {
            val leftEyeOpenness = calculateEyeOpenness(landmarks, true)
            val rightEyeOpenness = calculateEyeOpenness(landmarks, false)
            val averageEyeOpenness = (leftEyeOpenness + rightEyeOpenness) / 2

            val currentTime = System.currentTimeMillis()
            val timeSinceLastBlink = currentTime - lastBlinkTime

            val blinkDetected = if (lastBlinkState) {
                averageEyeOpenness > BLINK_THRESHOLD_OPEN
            } else {
                averageEyeOpenness < BLINK_THRESHOLD_CLOSE
            }

            if (blinkDetected && timeSinceLastBlink > MIN_BLINK_INTERVAL) {
                lastBlinkState = !lastBlinkState
                if (!lastBlinkState) { // 눈을 감았다가 다시 뜬 경우에만 깜빡임으로 간주
                    lastBlinkTime = currentTime
                    return true
                }
            }

            return false
        }

        fun calculateEyeOpenness(landmarks: List<NormalizedLandmark>, isLeftEye: Boolean): Float {
            val indices = if (isLeftEye) LEFT_EYE_INDICES else RIGHT_EYE_INDICES
            val (top, bottom, inner, outer) = indices.map { landmarks[it] }

            val verticalDistance = abs(top.y() - bottom.y())
            val horizontalDistance = abs(outer.x() - inner.x())
            return verticalDistance / (horizontalDistance + 1e-6f)
        }
    }
}