package com.nativo.prebidsdk.utils

import android.os.CountDownTimer
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode
import org.robolectric.Shadows
import org.robolectric.shadows.ShadowCountDownTimer
import java.util.concurrent.atomic.AtomicInteger

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [19])
@LooperMode(LooperMode.Mode.LEGACY)
class PausableCountDownTimerTest {

    private fun currentInnerTimer(p: PausableCountDownTimer): CountDownTimer? {
        val field = PausableCountDownTimer::class.java.getDeclaredField("timer")
        field.isAccessible = true
        return field.get(p) as? CountDownTimer
    }

    private fun invokeFinishOnCurrentTimer(p: PausableCountDownTimer) {
        val t = currentInnerTimer(p) ?: error("Expected inner CountDownTimer to be non-null")
        val shadow = Shadows.shadowOf(t) as ShadowCountDownTimer
        shadow.invokeFinish()
    }

    @Test
    fun start_thenFinish_invokesOnFinishOnce() {
        val calls = AtomicInteger(0)
        val pausable = PausableCountDownTimer(200) { calls.incrementAndGet() }

        pausable.start()

        // In Robolectric, CountDownTimer callbacks are typically driven via ShadowCountDownTimer.
        invokeFinishOnCurrentTimer(pausable)

        assertEquals("onFinish should be called exactly once", 1, calls.get())
        assertFalse(pausable.isRunning)
        assertFalse(pausable.isPaused)
        assertEquals(0L, pausable.timeLeftMillis)
    }

    @Test
    fun pause_cancelsCurrentTimer_andResume_createsNewTimer_thenFinishFiresOnce() {
        val calls = AtomicInteger(0)
        val pausable = PausableCountDownTimer(200) { calls.incrementAndGet() }

        pausable.start()
        val t1 = currentInnerTimer(pausable)
        assertNotNull("Timer should be created on start()", t1)

        pausable.pause()
        assertTrue(pausable.isPaused)
        assertFalse(pausable.isRunning)
        assertNull("Timer should be cleared on pause()", currentInnerTimer(pausable))
        assertEquals(0, calls.get())

        pausable.resume()
        assertTrue(pausable.isRunning)
        assertFalse(pausable.isPaused)

        val t2 = currentInnerTimer(pausable)
        assertNotNull("Timer should be recreated on resume()", t2)
        assertNotSame("Resume should create a new CountDownTimer instance", t1, t2)

        invokeFinishOnCurrentTimer(pausable)

        assertEquals(1, calls.get())
        assertFalse(pausable.isRunning)
        assertFalse(pausable.isPaused)
        assertEquals(0L, pausable.timeLeftMillis)
    }

    @Test
    fun pause_captures_remaining_time() {
        val pausable = PausableCountDownTimer(200) {}

        pausable.start()

        // Simulate time passing (with your time-aware version, you’d advance elapsedRealtime / looper)
        // For Robolectric, you can advance the system clock used by elapsedRealtime:
        org.robolectric.shadows.ShadowSystemClock.advanceBy(java.time.Duration.ofMillis(50))

        pausable.pause()

        val left = pausable.timeLeftMillis
        assertTrue("Expected remaining time to be < 200ms, but was $left", left in 1L..199L)
    }

    @Test
    fun cancel_clearsState_andDoesNotInvokeFinish() {
        val calls = AtomicInteger(0)
        val pausable = PausableCountDownTimer(200) { calls.incrementAndGet() }

        pausable.start()
        pausable.cancel()

        assertEquals(0, calls.get())
        assertFalse(pausable.isRunning)
        assertFalse(pausable.isPaused)
        assertNull(currentInnerTimer(pausable))
    }
}
