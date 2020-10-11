package com.android.server.wifi.hotspot2;

import android.net.wifi.ScanResult;
import com.android.server.wifi.hotspot2.PasspointProvisioner;
import java.util.Comparator;

/* renamed from: com.android.server.wifi.hotspot2.-$$Lambda$PasspointProvisioner$ProvisioningStateMachine$eoIJocMS8FJInAr7jQAC0mYUXi0 */
/* compiled from: lambda */
public final /* synthetic */ class C0510x41455f63 implements Comparator {
    public static final /* synthetic */ C0510x41455f63 INSTANCE = new C0510x41455f63();

    private /* synthetic */ C0510x41455f63() {
    }

    public final int compare(Object obj, Object obj2) {
        return PasspointProvisioner.ProvisioningStateMachine.lambda$getBestMatchingOsuProvider$2((ScanResult) obj, (ScanResult) obj2);
    }
}
