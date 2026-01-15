package org.prebid.mobile.rendering.bidding.interfaces;

import androidx.annotation.Nullable;

import org.prebid.mobile.api.exceptions.AdException;
import org.prebid.mobile.rendering.bidding.data.bid.BidResponse;

public interface ExternalBidRequesterListener {
    void onComplete(@Nullable BidResponse response, @Nullable AdException error);
}
