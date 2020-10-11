package com.android.server.wifi.aware;

import android.net.NetworkRequest;
import java.util.function.Function;

/* renamed from: com.android.server.wifi.aware.-$$Lambda$WifiAwareDataPathStateManager$AwareNetworkRequestInformation$39ENKv5hDa6RLtoJkAXWF8pVxAs */
/* compiled from: lambda */
public final /* synthetic */ class C0489xf449489d implements Function {
    public static final /* synthetic */ C0489xf449489d INSTANCE = new C0489xf449489d();

    private /* synthetic */ C0489xf449489d() {
    }

    public final Object apply(Object obj) {
        return ((NetworkRequest) obj).networkCapabilities.getNetworkSpecifier();
    }
}
