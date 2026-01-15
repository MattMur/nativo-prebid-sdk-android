package com.nativo.prebidsdk.bid

import com.nativo.prebidsdk.renderer.NativoPrebidRenderer
import org.prebid.mobile.rendering.bidding.data.bid.Bid
import org.prebid.mobile.rendering.bidding.data.bid.BidResponse
import org.prebid.mobile.rendering.bidding.data.bid.Seatbid
import org.prebid.mobile.configuration.AdUnitConfiguration

class NativoBidResponse(
    json: String,
    adUnitConfiguration: AdUnitConfiguration
) : BidResponse(json, adUnitConfiguration) {

    private var nativoWinningBid: Bid? = null

    init {
        nativoWinningBid = selectWinningBid()
        nativoWinningBid?.let { bid ->
            applyTargeting(bid)
            applyRendererMeta(bid)
        }
    }

    override fun getWinningBid(): Bid? {
        return nativoWinningBid ?: super.getWinningBid()
    }

    private fun selectWinningBid(): Bid? {
        var winningBid: Bid? = null
        var winningPrice = Double.NEGATIVE_INFINITY
        for (seatbid: Seatbid in seatbids) {
            for (bid in seatbid.bids) {
                if (bid.price > winningPrice) {
                    winningBid = bid
                    winningPrice = bid.price
                }
            }
        }
        return winningBid
    }

    private fun applyTargeting(bid: Bid) {
        val width = bid.width
        val height = bid.height
        val size = "${width}x${height}"
        val price = bid.price.toString()

        val targeting = bid.prebid.targeting
        targeting["hb_env"] = "mobile-app"
        targeting["hb_env_nativo"] = "mobile-app"
        targeting["hb_size"] = size
        targeting["hb_size_nativo"] = size
        targeting["hb_bidder"] = "nativo"
        targeting["hb_bidder_nativo"] = "nativo"
        targeting["hb_pb"] = price
        targeting["hb_pb_nativo"] = price
    }

    private fun applyRendererMeta(bid: Bid) {
        val meta = bid.prebid.meta
        meta[KEY_RENDERER_NAME] = NativoPrebidRenderer.NAME
        meta[KEY_RENDERER_VERSION] = NativoPrebidRenderer.VERSION
    }
}
