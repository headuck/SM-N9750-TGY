package com.android.server.wifi;

import com.android.server.wifi.NetworkSuggestionEvaluator;
import java.util.function.Function;

/* renamed from: com.android.server.wifi.-$$Lambda$NetworkSuggestionEvaluator$MatchMetaInfo$Z-5L6mK_zEDnbf0FAnqkVI8uBr0 */
/* compiled from: lambda */
public final /* synthetic */ class C0332x32e518c2 implements Function {
    public static final /* synthetic */ C0332x32e518c2 INSTANCE = new C0332x32e518c2();

    private /* synthetic */ C0332x32e518c2() {
    }

    public final Object apply(Object obj) {
        return Integer.valueOf(((NetworkSuggestionEvaluator.PerNetworkSuggestionMatchMetaInfo) obj).matchingScanDetail.getScanResult().level);
    }
}
