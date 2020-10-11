package com.android.server.wifi;

import com.android.server.wifi.WifiNetworkSuggestionsManager;
import java.util.function.Predicate;

/* renamed from: com.android.server.wifi.-$$Lambda$WifiNetworkSuggestionsManager$aCg0WttFDDZf8QB522s95VRR5qs */
/* compiled from: lambda */
public final /* synthetic */ class C0347xb17adf61 implements Predicate {
    public static final /* synthetic */ C0347xb17adf61 INSTANCE = new C0347xb17adf61();

    private /* synthetic */ C0347xb17adf61() {
    }

    public final boolean test(Object obj) {
        return ((WifiNetworkSuggestionsManager.ExtendedWifiNetworkSuggestion) obj).wns.isAppInteractionRequired;
    }
}
