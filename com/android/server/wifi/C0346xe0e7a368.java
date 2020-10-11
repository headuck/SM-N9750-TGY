package com.android.server.wifi;

import com.android.server.wifi.WifiNetworkSuggestionsManager;
import java.util.function.Predicate;

/* renamed from: com.android.server.wifi.-$$Lambda$WifiNetworkSuggestionsManager$VZi4a9MMz0x_1KiQWZ0-XwDSoj4 */
/* compiled from: lambda */
public final /* synthetic */ class C0346xe0e7a368 implements Predicate {
    public static final /* synthetic */ C0346xe0e7a368 INSTANCE = new C0346xe0e7a368();

    private /* synthetic */ C0346xe0e7a368() {
    }

    public final boolean test(Object obj) {
        return ((WifiNetworkSuggestionsManager.ExtendedWifiNetworkSuggestion) obj).perAppInfo.hasUserApproved;
    }
}
