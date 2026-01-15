package com.nativo.prebidsdk.networking

import org.prebid.mobile.configuration.AdUnitConfiguration
import org.prebid.mobile.rendering.networking.parameters.AdRequestInput
import org.prebid.mobile.rendering.networking.parameters.ParameterBuilder

class NativoParameterBuilder(
    private val adConfiguration: AdUnitConfiguration
) : ParameterBuilder() {

    override fun appendBuilderParameters(adRequestInput: AdRequestInput) {
        val bidRequest = adRequestInput.bidRequest
        bidRequest.device.ip = FALLBACK_IP

        for (imp in bidRequest.imp) {
            imp.tagid = adConfiguration.configId
        }
    }

    companion object {
        private const val FALLBACK_IP = "108.214.18.218"
    }
}
