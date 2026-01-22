package com.nativo.prebidsdk.exposure

import android.content.Context
import android.view.View
import android.view.ViewTreeObserver
import org.prebid.mobile.LogUtil
import org.prebid.mobile.rendering.models.internal.VisibilityTrackerOption
import org.prebid.mobile.rendering.models.internal.VisibilityTrackerResult
import org.prebid.mobile.rendering.utils.exposure.ViewExposure
import org.prebid.mobile.rendering.utils.exposure.ViewExposureChecker
import org.prebid.mobile.rendering.utils.helpers.VisibilityChecker
import org.prebid.mobile.rendering.views.webview.mraid.Views
import com.nativo.prebidsdk.utils.NativoUtils
import com.nativo.prebidsdk.utils.PausableCountDownTimer
import java.lang.ref.WeakReference
import java.util.Collections

class NativoCreativeVisibilityTracker(
    trackedView: View,
    visibilityTrackerOptionSet: Set<VisibilityTrackerOption>
) {

    interface VisibilityTrackerListener {
        fun onVisibilityChanged(result: VisibilityTrackerResult)
    }

    private val onScrollChangedListener: ViewTreeObserver.OnScrollChangedListener
    private val onGlobalLayoutListener: ViewTreeObserver.OnGlobalLayoutListener
    private var weakViewTreeObserver: WeakReference<ViewTreeObserver?>

    private val trackedView: WeakReference<View> = WeakReference(trackedView)
    private val visibilityCheckerList: MutableList<VisibilityChecker> = ArrayList()
    private var visibilityTrackerListener: VisibilityTrackerListener? = null
    private var proceedAfterImpTracking: Boolean = false

    private val viewabilityTimerMap: MutableMap<VisibilityChecker, PausableCountDownTimer> = mutableMapOf()

    private val viewabilityCheckDebouncer: (Any?) -> Unit

    constructor(
        trackedView: View,
        visibilityTrackerOptionSet: Set<VisibilityTrackerOption>,
        proceedAfterImpTracking: Boolean
    ) : this(trackedView, visibilityTrackerOptionSet) {
        this.proceedAfterImpTracking = proceedAfterImpTracking
    }

    constructor(
        trackedView: View,
        visibilityTrackerOption: VisibilityTrackerOption
    ) : this(trackedView, Collections.singleton(visibilityTrackerOption))

    constructor(
        trackedView: View,
        visibilityTrackerOption: VisibilityTrackerOption,
        proceedAfterImpTracking: Boolean
    ) : this(trackedView, Collections.singleton(visibilityTrackerOption), proceedAfterImpTracking)

    init {
        val viewExposureChecker = ViewExposureChecker()

        for (trackingOption in visibilityTrackerOptionSet) {
            visibilityCheckerList.add(VisibilityChecker(trackingOption, viewExposureChecker))
        }

        viewabilityCheckDebouncer = NativoUtils.debounceAction(VISIBILITY_DEBOUNCE_MILLIS) {
            runViewabilityCheck()
        }

        onScrollChangedListener = ViewTreeObserver.OnScrollChangedListener {
            viewabilityCheckDebouncer.invoke(null)
        }
        onGlobalLayoutListener = ViewTreeObserver.OnGlobalLayoutListener {
            viewabilityCheckDebouncer.invoke(null)
        }

        weakViewTreeObserver = WeakReference(null)
    }

    fun setVisibilityTrackerListener(visibilityTrackerListener: VisibilityTrackerListener?) {
        this.visibilityTrackerListener = visibilityTrackerListener
    }

    fun startVisibilityCheck(context: Context) {
        val tracked = trackedView.get()
        if (tracked == null) {
            LogUtil.error(TAG, "Couldn't start visibility check. Tasrget view is null")
            return
        }
        setViewTreeObserver(context, tracked)
        // Run initial check
        viewabilityCheckDebouncer.invoke(null)
    }

    /**
     * Used for interstitial cases, when the ad is opened in the new view hierarchy or received the new window focus.
     */
    fun restartVisibilityCheck() {
        viewabilityCheckDebouncer.invoke(null)
    }

    fun stopVisibilityCheck() {
        val viewTreeObserver = weakViewTreeObserver.get()
        if (viewTreeObserver != null && viewTreeObserver.isAlive) {
            try {
                viewTreeObserver.removeOnScrollChangedListener(onScrollChangedListener)
            } catch (e: Exception) {
                LogUtil.error(TAG, "Error removing scroll listener: ${e.message}")
            }
            try {
                viewTreeObserver.removeOnGlobalLayoutListener(onGlobalLayoutListener)
            } catch (e: Exception) {
                LogUtil.error(TAG, "Error removing global layout listener: ${e.message}")
            }
        }
        weakViewTreeObserver.clear()
    }

    private fun setViewTreeObserver(context: Context?, view: View?) {
        val originalViewTreeObserver = weakViewTreeObserver.get()
        if (originalViewTreeObserver != null && originalViewTreeObserver.isAlive) {
            LogUtil.debug(TAG, "Original ViewTreeObserver is still alive.")
            return
        }

        val rootView = Views.getTopmostView(context, view)
        if (rootView == null) {
            LogUtil.debug(TAG, "Unable to set Visibility Tracker due to no available root view.")
            return
        }

        val viewTreeObserver = rootView.viewTreeObserver
        if (!viewTreeObserver.isAlive) {
            LogUtil.debug(
                TAG,
                "Visibility Tracker was unable to track views because the root view tree observer was not alive"
            )
            return
        }

        weakViewTreeObserver = WeakReference(viewTreeObserver)
        viewTreeObserver.addOnScrollChangedListener(onScrollChangedListener)
        viewTreeObserver.addOnGlobalLayoutListener(onGlobalLayoutListener)
    }

    private fun runViewabilityCheck() {
        val trackedView = this.trackedView.get()
        if (trackedView == null) {
            stopVisibilityCheck()
            return
        }

        for (visibilityChecker in visibilityCheckerList) {
            val viewExposure: ViewExposure = visibilityChecker.checkViewExposure(trackedView)
            var shouldFireImpression = false
            val isVisible = visibilityChecker.isVisible(trackedView, viewExposure)

            // Manage viewability timer to ensure ad is visible for set duration before imp is fired
            val visibilityTrackerOption = visibilityChecker.visibilityTrackerOption
            var viewabilityTimer = viewabilityTimerMap[visibilityChecker]
            if (isVisible) {
                // Start viewability duration timer, which will call runViewabilityCheck() again when finished
                if (viewabilityTimer == null) {
                    val viewableImpDuration = visibilityTrackerOption.minimumVisibleMillis.toLong()
                    viewabilityTimer = PausableCountDownTimer(viewableImpDuration) {
                        runViewabilityCheck()
                    }
                    viewabilityTimerMap[visibilityChecker] = viewabilityTimer
                    viewabilityTimer.start()
                }

                if (viewabilityTimer.isFinished && !visibilityTrackerOption.isImpressionTracked) {
                    shouldFireImpression = true
                    visibilityTrackerOption.isImpressionTracked = true
                }
            } else {
                viewabilityTimer?.pause()
            }

            val visibilityTrackerResult = VisibilityTrackerResult(
                visibilityTrackerOption.eventType,
                viewExposure,
                isVisible,
                shouldFireImpression
            )
            notifyListener(visibilityTrackerResult)
        }

        // If all impressions are done and no further tracking is required, fully stop.
        if (allImpressionsFired() && !proceedAfterImpTracking) {
            stopVisibilityCheck()
        }
    }

    private fun notifyListener(visibilityTrackerResult: VisibilityTrackerResult) {
        visibilityTrackerListener?.onVisibilityChanged(visibilityTrackerResult)
    }

    private fun allImpressionsFired(): Boolean {
        for (visibilityChecker in visibilityCheckerList) {
            val visibilityTrackerOption = visibilityChecker.visibilityTrackerOption
            if (!visibilityTrackerOption.isImpressionTracked) {
                return false
            }
        }
        return true
    }

    companion object Companion {
        private val TAG = NativoCreativeVisibilityTracker::class.java.simpleName

        // Time interval to use for throttling visibility checks and debounce window for events.
        private const val VISIBILITY_DEBOUNCE_MILLIS = 150L
    }
}
