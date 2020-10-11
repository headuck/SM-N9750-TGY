package com.android.server.wifi.aware;

import android.net.NetworkRequest;
import java.util.function.Function;

/* renamed from: com.android.server.wifi.aware.-$$Lambda$WifiAwareDataPathStateManager$NetworkInterfaceWrapper$5dfGOhPStI7PI7XT9E1QFwOyQdc */
/* compiled from: lambda */
public final /* synthetic */ class C0490x6231c9a9 implements Function {
    public static final /* synthetic */ C0490x6231c9a9 INSTANCE = new C0490x6231c9a9();

    private /* synthetic */ C0490x6231c9a9() {
    }

    public final Object apply(Object obj) {
        return ((NetworkRequest) obj).networkCapabilities.getNetworkSpecifier();
    }
}
