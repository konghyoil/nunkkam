package com.its.nunkkam.android

import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import kotlin.math.abs


class BlinkDetectionUtil {// blinkDetectionUtil
private var blinkCount = 0 // 눈 깜빡임 총 횟수를 저장하는 변수
    private var lastBlinkTime = System.currentTimeMillis() // 마지막 눈 깜빡임이 감지된 시간
    private var blinkRate = 0.0 // 분당 눈 깜빡임 횟수 (blinks per minute)
    private var frameCounter = 0 // 프레임 카운터
    private var lastFpsUpdateTime = System.currentTimeMillis() // 마지막 FPS 업데이트 시간
    private var fps = 0f // 현재 FPS

    // 타이머
    private var timeLeftInMillis: Long = 1200000 // 남은 시간 (밀리초)
    private var timerRunning: Boolean = false // 타이머 실행 중 여부
    private var startTime: Long = 0 // 시작 시간
    private var endTime: Long = 0 // 종료 시간
    private var pausedStartTime: Long = 0 // 일시정지 시작 시간
    private var pausedAccumulatedTime: Long = 0 // 누적된 일시정지 시간

    private val currentTime = System.currentTimeMillis() // 현재 시간 기록

    private var previousEyeState = EyeState.OPEN
    private var currentEyeState = EyeState.OPEN
    private var isTimerRunning = false

    private var isForeground = false // 포그라운드 여부를 나타내는 변수

    fun getLastBlinkTime(): Long = lastBlinkTime
    fun setLastBlinkTime(time: Long) { lastBlinkTime = time }

    fun getBlinkRate(): Double = blinkRate
    fun setBlinkRate(rate: Double) { blinkRate = rate }

    fun getFrameCounter(): Int = frameCounter
    fun incrementFrameCounter() { frameCounter++ }
    fun resetFrameCounter() { frameCounter = 0 }

    fun getLastFpsUpdateTime(): Long = lastFpsUpdateTime
    fun setLastFpsUpdateTime(time: Long) { lastFpsUpdateTime = time }

    fun getFps(): Float = fps
    fun setFps(newFps: Float) { fps = newFps }

    fun getCurrentTime(): Long = currentTime

    fun isTimerRunning(): Boolean = timerRunning
    fun setTimerRunning(running: Boolean) { timerRunning = running }

    fun getStartTime(): Long = startTime
    fun setStartTime(time: Long) { startTime = time }

    fun getEndTime(): Long = endTime
    fun setEndTime(time: Long) { endTime = time }

    fun getPausedStartTime(): Long = pausedStartTime
    fun setPausedStartTime(time: Long) { pausedStartTime = time }

    fun getPausedAccumulatedTime(): Long = pausedAccumulatedTime
    fun setPausedAccumulatedTime(time: Long) { pausedAccumulatedTime = time }
    fun addPausedAccumulatedTime(time: Long) { pausedAccumulatedTime += time }

    enum class EyeState {
        OPEN, CLOSED
    }

    fun getTimeLeftInMillis(): Long { return timeLeftInMillis }
    fun setTimeLeftInMillis(time: Long) { timeLeftInMillis = time }

    /** 현재까지 감지된 눈 깜빡임 수를 반환 */
    fun getBlinkCount(): Int {
        return blinkCount
    }

    fun incrementBlinkCount() {
        blinkCount++
    }

    /** 눈 깜빡임 카운트를 초기화 */
    fun resetBlinkCount() {
        blinkCount = 0
        previousEyeState = EyeState.OPEN
        currentEyeState = EyeState.OPEN
    }

    fun setForeground(foreground: Boolean) {
        isForeground = foreground
    }

    fun isForeground(): Boolean {
        return isForeground
    }

    /**
     * 눈 상태를 업데이트하고, 눈 깜빡임이 감지되면 카운트를 증가
     * @param isEyeClosed 현재 눈이 감겨있는지 여부.
     */
    fun updateBlinkCount(isEyeClosed: Boolean) {
        currentEyeState = if (isEyeClosed) EyeState.CLOSED else EyeState.OPEN

        if (previousEyeState == EyeState.OPEN && currentEyeState == EyeState.CLOSED) {
            blinkCount++
        }

        previousEyeState = currentEyeState
    }

    /** 타이머 시작 */
    fun startTimer(totalTimeInSeconds: Int) {
//        remainingTimeInSeconds = totalTimeInSeconds
        isTimerRunning = true
    }

    /** 타이머 일시정지 */
    fun pauseTimer() {
        isTimerRunning = false
    }

    /** 타이머 재개 */
    fun resumeTimer() {
        isTimerRunning = true
    }

    /** 타이머 초기화 */
    fun resetTimer() {
        timeLeftInMillis = 1200000
        isTimerRunning = false
    }

    /** 타이머 업데이트 (1초마다 호출) */
    fun updateTimer(elapsedMillis: Long): Boolean {
        if (isTimerRunning && timeLeftInMillis > 0) {
            timeLeftInMillis -= elapsedMillis
            if (timeLeftInMillis < 0) timeLeftInMillis = 0
            return true
        }
        return false
    }

    /** 남은 시간 반환 */
    fun getRemainingTime(): String {
        return getFormattedTime()
    }

    /** 시간을 "MM:SS" 형식의 문자열로 반환 */
    fun getFormattedTime(): String {
        val minutes = (timeLeftInMillis / 1000) / 60
        val seconds = (timeLeftInMillis / 1000) % 60
        return String.format("%02d:%02d", minutes, seconds)//00:%02d:%02d
    }

    /** Blink Rate 업데이트 함수 */
    fun updateBlinkRate() {
        val currentTime = System.currentTimeMillis()
        val timeDiff = (currentTime - lastBlinkTime) / 1000.0 // 마지막 깜빡임과의 시간 차이를 초 단위로 계산
        blinkRate = if (timeDiff > 0) 60.0 / timeDiff else 0.0 // 분당 깜빡임 횟수 계산 (60초 / 깜빡임 간격)
    }

    /** FPS 업데이트 함수 */
    fun updateFps() {
        frameCounter++
        val currentTime = System.currentTimeMillis()
        val elapsedMillis = currentTime - lastFpsUpdateTime
        if (elapsedMillis >= 1000) { // 1초마다 FPS 계산
            fps = frameCounter / (elapsedMillis / 1000f)
            lastFpsUpdateTime = currentTime
            frameCounter = 0
        }
    }

    companion object {
        // 깜빡임 임계값 정의
        private val BLINK_THRESHOLD = 0.2

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