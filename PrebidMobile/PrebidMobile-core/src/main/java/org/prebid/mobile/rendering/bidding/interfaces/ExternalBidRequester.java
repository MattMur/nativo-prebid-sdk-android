package org.prebid.mobile.rendering.bidding.interfaces;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.prebid.mobile.configuration.AdUnitConfiguration;
import org.prebid.mobile.rendering.bidding.data.bid.BidResponse;

public interface ExternalBidRequester {
    void requestBids(
            @NonNull AdUnitConfiguration adUnitConfiguration,
            @NonNull ExternalBidRequesterListener listener
    );

    default boolean shouldRenderImmediately(@Nullable BidResponse response) {
        return false;
    }
}
