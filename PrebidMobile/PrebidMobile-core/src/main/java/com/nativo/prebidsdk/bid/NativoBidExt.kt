package com.nativo.prebidsdk.bid

import org.json.JSONObject
import org.prebid.mobile.rendering.bidding.data.bid.Bid

object NativoBidExt {
    private const val EXT_KEY = "ext"
    private const val NATIVO_KEY = "nativo"
    private const val OWNED_OPERATED_KEY = "oo"

    @JvmStatic
    fun isOwnedOperated(bid: Bid?): Boolean {
        if (bid == null) {
            return false
        }
        val json = bid.jsonString ?: return false
        return try {
            val root = JSONObject(json)
            val ext = root.optJSONObject(EXT_KEY) ?: return false
            val nativo = ext.optJSONObject(NATIVO_KEY) ?: return false
            when (val raw = nativo.opt(OWNED_OPERATED_KEY)) {
                is Boolean -> raw
                is Number -> raw.toInt() != 0
                else -> false
            }
        } catch (_: Exception) {
            false
        }
    }
}
