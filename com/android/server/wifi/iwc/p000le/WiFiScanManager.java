package com.android.server.wifi.iwc.p000le;

import android.content.Context;
import android.os.Bundle;

/* renamed from: com.android.server.wifi.iwc.le.WiFiScanManager */
public interface WiFiScanManager {
    void finalize();

    void initialize(Context context, Bundle bundle);

    boolean isMonitoring();

    void startMonitoring(WiFiScanResultListener wiFiScanResultListener);

    void stopMonitoring();
}
