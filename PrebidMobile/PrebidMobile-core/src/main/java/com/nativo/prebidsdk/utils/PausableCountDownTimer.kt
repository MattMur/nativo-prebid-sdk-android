package com.nativo.prebidsdk.utils

import android.os.CountDownTimer
import android.os.SystemClock

/**
 * A simple wrapper around [CountDownTimer] that supports pause and resume.
 *
 * Implementation details:
 * - We track the remaining time on every tick.
 * - Pause is achieved by cancelling the underlying timer but keeping the remaining time.
 * - Resume recreates a new [CountDownTimer] with the remaining time and the same tick interval.
 * - When the countdown finishes, [onFinish] is invoked exactly once.
 */
class PausableCountDownTimer(
    private val totalDurationMillis: Long,
    private val onFinish: () -> Unit,
) {

    private var timer: CountDownTimer? = null
    private var startTimeMillis: Long = 0L
    var timeLeftMillis: Long = totalDurationMillis
        private set
    var isRunning: Boolean = false
        private set
    var isPaused: Boolean = false
        private set
    var isFinished: Boolean = false
        private set

    /** Starts or restarts the countdown from the full duration. */
    @Synchronized
    fun start() {
        cancelInternal(resetTime = true)
        timeLeftMillis = totalDurationMillis
        createAndStartTimer(totalDurationMillis)
    }

    /** Pauses the countdown, keeping the remaining time. No-op if not running. */
    @Synchronized
    fun pause() {
        if (!isRunning) return
        // Calculate new time left
        val now = SystemClock.elapsedRealtime()
        timeLeftMillis = timeLeftMillis - (now - startTimeMillis)

        timer?.cancel()
        timer = null
        isRunning = false
        if (timeLeftMillis <= 0L) {
            finishInternal()
        } else {
            isPaused = true
        }
    }

    /** Resumes the countdown from where it was paused. No-op if not paused or already running. */
    @Synchronized
    fun resume() {
        if (!isPaused || isRunning) return
        if (timeLeftMillis <= 0L) {
            // Nothing to resume; already finished
            return
        }
        createAndStartTimer(timeLeftMillis)
        isRunning = true
        isPaused = false
    }

    /** Cancels the countdown and clears the state. */
    @Synchronized
    fun cancel() {
        cancelInternal(resetTime = false)
    }

    private fun createAndStartTimer(duration: Long) {
        // Guard against non-positive duration: finish immediately
        if (duration <= 0L) {
            finishInternal()
            return
        }
        startTimeMillis = SystemClock.elapsedRealtime()
        isRunning = true
        isPaused = false
        timer = object : CountDownTimer(duration, duration) {
            override fun onTick(millisUntilFinished: Long) {}

            override fun onFinish() {
                // Ensure we set state and fire the callback once
                finishInternal()
            }
        }.also { it.start() }
    }

    @Synchronized
    private fun finishInternal() {
        timer?.cancel()
        timer = null
        timeLeftMillis = 0L
        isRunning = false
        isPaused = false
        isFinished = true
        onFinish.invoke()
    }

    @Synchronized
    private fun cancelInternal(resetTime: Boolean) {
        timer?.cancel()
        timer = null
        isRunning = false
        isPaused = false
        startTimeMillis = 0L
        if (resetTime) {
            timeLeftMillis = totalDurationMillis
        }
    }
}