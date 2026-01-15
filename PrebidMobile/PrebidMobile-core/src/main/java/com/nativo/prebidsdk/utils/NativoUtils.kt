package com.nativo.prebidsdk.utils

import android.os.SystemClock
import java.util.concurrent.atomic.AtomicLong

object NativoUtils {
    fun debounceAction(intervalMs: Long, action: (Any?) -> Unit): (Any?) -> Unit {
        val lastCall = AtomicLong(0L)
        return { param ->
            val now = SystemClock.elapsedRealtime()
            val previous = lastCall.get()
            if (now - previous >= intervalMs) {
                lastCall.set(now)
                action(param)
            }
        }
    }
}
