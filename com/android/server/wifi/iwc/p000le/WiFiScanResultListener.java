package com.android.server.wifi.iwc.p000le;

import java.util.List;
import java.util.Map;

/* renamed from: com.android.server.wifi.iwc.le.WiFiScanResultListener */
public interface WiFiScanResultListener {
    void onWiFiScanResultAcquired(CapInfo capInfo, Map<String, Double> map, List<Integer> list);
}
