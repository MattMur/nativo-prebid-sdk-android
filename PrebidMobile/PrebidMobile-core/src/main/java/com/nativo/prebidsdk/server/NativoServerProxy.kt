package com.nativo.prebidsdk.server

import com.nativo.prebidsdk.bid.NativoBidResponse
import com.nativo.prebidsdk.networking.NativoBidRequester
import org.prebid.mobile.LogUtil
import org.prebid.mobile.api.exceptions.AdException
import org.prebid.mobile.configuration.AdUnitConfiguration
import org.prebid.mobile.rendering.bidding.data.bid.Bid
import org.prebid.mobile.rendering.bidding.data.bid.BidResponse

typealias NativoBidResponseCallback = (bidResponse: NativoBidResponse?, shouldRenderImmediately: Boolean) -> Unit

class NativoServerProxy {

    private val nativoBidRequester = NativoBidRequester()
    var nativoBidResponse : NativoBidResponse? = null
        private set

    fun requestNativoBid(
        adUnitConfig: AdUnitConfiguration,
        callback: NativoBidResponseCallback
    ) {
        nativoBidResponse = null
        nativoBidRequester.requestBids(
            adUnitConfig
        ) { response: BidResponse?, error: AdException? ->
            nativoBidResponse = response as NativoBidResponse?

            if (error != null) {
                LogUtil.error(this::class.simpleName, error.message)
            }
            if (response != null) {
                callback(response, nativoBidRequester.shouldRenderImmediately(response))
            } else {
                callback(null, false)
            }
        }
    }

    fun decideWinner(
        prebidBidResponse: BidResponse?
    ): BidResponse? {
        if (prebidBidResponse == null) {
            return nativoBidResponse
        }
        if (nativoBidResponse == null) {
            return prebidBidResponse
        }
        val prebidPrice = getBidPrice(prebidBidResponse)
        val nativoPrice = getBidPrice(nativoBidResponse)

        if (nativoPrice >= prebidPrice) {
            return nativoBidResponse
        }
        return prebidBidResponse
    }

    fun getBidFromResponse(response: BidResponse?): Bid? {
        return response?.getWinningBid()
    }

    fun getBidPrice(response: BidResponse?): Double {
        val bid = getBidFromResponse(response)
        return bid?.price ?: 0.0
    }
}