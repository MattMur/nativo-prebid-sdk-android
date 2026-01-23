package com.nativo.prebidsdk.renderer

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import org.json.JSONObject
import org.prebid.mobile.LogUtil
import org.prebid.mobile.api.data.AdFormat
import org.prebid.mobile.api.exceptions.AdException
import org.prebid.mobile.api.rendering.PrebidMobileInterstitialControllerInterface
import org.prebid.mobile.api.rendering.pluginrenderer.PluginEventListener
import org.prebid.mobile.api.rendering.pluginrenderer.PrebidMobilePluginRenderer
import org.prebid.mobile.configuration.AdUnitConfiguration
import org.prebid.mobile.rendering.bidding.data.bid.BidResponse
import org.prebid.mobile.rendering.bidding.display.InterstitialController
import org.prebid.mobile.rendering.bidding.interfaces.InterstitialControllerListener
import org.prebid.mobile.rendering.bidding.listeners.DisplayVideoListener
import org.prebid.mobile.rendering.bidding.listeners.DisplayViewListener

class NativoPrebidRenderer : PrebidMobilePluginRenderer {

    companion object {
        private const val TAG = "NativoPrebidRenderer"
        const val NAME = "NativoRenderer"
        const val VERSION = "1.0.0"
    }

    override fun getName(): String = NAME

    override fun getVersion(): String = VERSION

    override fun getData(): JSONObject? = null

    override fun createBannerAdView(
        context: Context,
        displayViewListener: DisplayViewListener,
        displayVideoListener: DisplayVideoListener?,
        adUnitConfiguration: AdUnitConfiguration,
        bidResponse: BidResponse
    ): View {
        var displayViewRef: NativoDisplayView? = null
        val forwardingListener = object : DisplayViewListener {
            override fun onAdLoaded() {
                displayViewListener.onAdLoaded()
            }

            override fun onAdDisplayed() {
                displayViewListener.onAdDisplayed()
                displayViewRef?.let {
                    setChildrenFullWidth(it)
                }
            }

            override fun onAdFailed(exception: AdException) {
                displayViewListener.onAdFailed(exception)
            }

            override fun onAdClicked() {
                displayViewListener.onAdClicked()
            }

            override fun onAdClosed() {
                displayViewListener.onAdClosed()
            }
        }

        val displayView = NativoDisplayView(
            context,
            forwardingListener,
            displayVideoListener,
            adUnitConfiguration,
            bidResponse
        )
        displayViewRef = displayView

        displayView.layoutParams = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        return displayView
    }

    override fun createInterstitialController(
        context: Context,
        interstitialControllerListener: InterstitialControllerListener,
        adUnitConfiguration: AdUnitConfiguration,
        bidResponse: BidResponse
    ): PrebidMobileInterstitialControllerInterface? {
        return try {
            InterstitialController(context, interstitialControllerListener)
        } catch (exception: AdException) {
            LogUtil.error(TAG, "message: ${exception.message}")
            null
        }
    }

    override fun isSupportRenderingFor(adUnitConfiguration: AdUnitConfiguration): Boolean {
        return adUnitConfiguration.isAdType(AdFormat.BANNER)
    }

    private fun setChildrenFullWidth(displayView: ViewGroup) {
            val child = displayView.getChildAt(0)
            if (child == null) {
                LogUtil.error(TAG, "Nativo renderer expected a child view on PrebidDisplayView, but none was found.")
                return
            }
            ensureFullWidth(child)
            if (child is ViewGroup && child.childCount > 0) {
                ensureFullWidth(child.getChildAt(0))
            }
    }

    private fun ensureFullWidth(view: View) {
        val layoutParams = view.layoutParams
        if (layoutParams != null) {
            layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT
            view.layoutParams = layoutParams
            return
        }

        view.layoutParams = when (view.parent) {
            is FrameLayout -> FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            else -> ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
    }

    override fun registerEventListener(
        pluginEventListener: PluginEventListener,
        listenerKey: String
    ) {
    }

    override fun unregisterEventListener(listenerKey: String) {
    }
}
