package com.its.nunkkam.android

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

    private var isBlinkDetectionPaused: Boolean = false
    private var isFpsCalculationPaused: Boolean = false

    fun pauseBlinkDetection() {
        isBlinkDetectionPaused = true
    }

    fun resumeBlinkDetection() {
        isBlinkDetectionPaused = false
    }

    fun pauseFpsCalculation() {
        isFpsCalculationPaused = true
    }

    fun resumeFpsCalculation() {
        isFpsCalculationPaused = false
    }

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

    private fun setPreviousEyeState(state: EyeState) {  previousEyeState = state }
    private fun setCurrentEyeState(state: EyeState) {  previousEyeState = state }

    /** 눈 깜빡임 카운트를 초기화 */
    fun resetBlinkCount() {
        blinkCount = 0
        setPreviousEyeState(EyeState.OPEN)
        setCurrentEyeState(EyeState.OPEN)
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
        if (isBlinkDetectionPaused) return  // 눈 깜빡임 감지가 일시정지된 경우 함수 종료

        currentEyeState = if (isEyeClosed) EyeState.CLOSED else EyeState.OPEN

        if (previousEyeState == EyeState.OPEN && currentEyeState == EyeState.CLOSED) {
            incrementBlinkCount()
            updateBlinkRate()
            setLastBlinkTime(System.currentTimeMillis())
        }

        setPreviousEyeState(currentEyeState)
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
        if (isBlinkDetectionPaused) return  // 눈 깜빡임 감지가 일시정지된 경우 함수 종료

        val timeDiff = (System.currentTimeMillis() - lastBlinkTime) / 1000.0 // 마지막 깜빡임과의 시간 차이를 초 단위로 계산
        blinkRate = if (timeDiff > 0) 60.0 / timeDiff else 0.0 // 분당 깜빡임 횟수 계산 (60초 / 깜빡임 간격)
    }

    /** FPS 업데이트 함수 */
    fun updateFps() {
        if (isFpsCalculationPaused) return  // FPS 계산이 일시정지된 경우 함수 종료

        frameCounter++
        val currentTime = System.currentTimeMillis()
        val elapsedMillis = currentTime - lastFpsUpdateTime
        if (elapsedMillis >= 1000) { // 1초마다 FPS 계산
            fps = frameCounter / (elapsedMillis / 1000f)
            //            fps = (frameCounter * 1000f) / elapsedMillis
            lastFpsUpdateTime = currentTime
            frameCounter = 0
        }
    }

}