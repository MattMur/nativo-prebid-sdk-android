package com.nativo.prebidsdk.networking

import org.prebid.mobile.rendering.networking.urlBuilder.URLPathBuilder

class NativoPathBuilder : URLPathBuilder() {
    override fun buildURLPath(domain: String): String {
        return NATIVO_ENDPOINT
    }

    companion object {
        // TODO: https?
        const val NATIVO_ENDPOINT = "http://exchange.postrelease.com/esi.json?ntv_epid=7&ntv_tm=tout"
    }
}
