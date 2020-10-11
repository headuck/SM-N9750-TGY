package com.android.server.wifi.iwc.p000le;

import android.content.Context;
import android.os.Bundle;

/* renamed from: com.android.server.wifi.iwc.le.WiFiStatusObserver */
public interface WiFiStatusObserver {
    void initialize(Context context, Bundle bundle);

    void startMonitoring(WiFiStatusListener wiFiStatusListener);

    void stopMonitoring();
}
