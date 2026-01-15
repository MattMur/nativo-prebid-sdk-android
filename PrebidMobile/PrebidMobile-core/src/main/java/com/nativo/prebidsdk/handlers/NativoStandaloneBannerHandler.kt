package com.nativo.prebidsdk.handlers

import org.prebid.mobile.AdSize
import org.prebid.mobile.rendering.bidding.data.bid.Bid
import org.prebid.mobile.rendering.bidding.data.bid.BidResponse
import org.prebid.mobile.rendering.bidding.interfaces.BannerEventHandler
import org.prebid.mobile.rendering.bidding.listeners.BannerEventListener

class NativoStandaloneBannerHandler : BannerEventHandler {
    private var bannerEventListener: BannerEventListener? = null

    override fun getAdSizeArray(): Array<AdSize> {
        return emptyArray()
    }

    override fun setBannerEventListener(bannerViewListener: BannerEventListener) {
        bannerEventListener = bannerViewListener
    }

    override fun requestAdWithBid(bid: Bid?) {
        bannerEventListener?.onPrebidSdkWin()
    }

    override fun requestAdWithBidResponses(prebidResponse: BidResponse?, nativoResponse: BidResponse?) {
        val prebidPrice = prebidResponse?.winningBid?.price ?: 0.0
        val nativoPrice = nativoResponse?.winningBid?.price ?: 0.0
        if (nativoPrice >= prebidPrice) {
            bannerEventListener?.onNativoSdkWin()
        } else {
            bannerEventListener?.onPrebidSdkWin()
        }
    }

    override fun trackImpression() {
    }

    override fun destroy() {
    }
}
