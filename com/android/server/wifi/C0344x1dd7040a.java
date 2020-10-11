package com.android.server.wifi;

import com.android.server.wifi.WifiNetworkSuggestionsManager;
import java.util.function.Predicate;

/* renamed from: com.android.server.wifi.-$$Lambda$WifiNetworkSuggestionsManager$NCSgMx5AU5TMrknU5s9bz-w5LWc */
/* compiled from: lambda */
public final /* synthetic */ class C0344x1dd7040a implements Predicate {
    public static final /* synthetic */ C0344x1dd7040a INSTANCE = new C0344x1dd7040a();

    private /* synthetic */ C0344x1dd7040a() {
    }

    public final boolean test(Object obj) {
        return ((WifiNetworkSuggestionsManager.ExtendedWifiNetworkSuggestion) obj).perAppInfo.hasUserApproved;
    }
}
