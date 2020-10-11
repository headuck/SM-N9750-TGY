package com.android.server.wifi;

import com.android.server.wifi.NetworkSuggestionEvaluator;
import java.util.function.Function;

/* renamed from: com.android.server.wifi.-$$Lambda$NetworkSuggestionEvaluator$PerAppMatchMetaInfo$4zBwRelAVwggSH4KkLdQq5J6uMs */
/* compiled from: lambda */
public final /* synthetic */ class C0333x4cc006 implements Function {
    public static final /* synthetic */ C0333x4cc006 INSTANCE = new C0333x4cc006();

    private /* synthetic */ C0333x4cc006() {
    }

    public final Object apply(Object obj) {
        return Integer.valueOf(((NetworkSuggestionEvaluator.PerNetworkSuggestionMatchMetaInfo) obj).wifiNetworkSuggestion.wifiConfiguration.priority);
    }
}
