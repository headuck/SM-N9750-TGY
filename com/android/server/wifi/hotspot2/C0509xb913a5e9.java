package com.android.server.wifi.hotspot2;

import android.net.wifi.ScanResult;
import java.util.function.Predicate;

/* renamed from: com.android.server.wifi.hotspot2.-$$Lambda$PasspointProvisioner$ProvisioningStateMachine$BJK4Yjr06CQ7LkDBjJfiArECw5Y */
/* compiled from: lambda */
public final /* synthetic */ class C0509xb913a5e9 implements Predicate {
    public static final /* synthetic */ C0509xb913a5e9 INSTANCE = new C0509xb913a5e9();

    private /* synthetic */ C0509xb913a5e9() {
    }

    public final boolean test(Object obj) {
        return ((ScanResult) obj).isPasspointNetwork();
    }
}
