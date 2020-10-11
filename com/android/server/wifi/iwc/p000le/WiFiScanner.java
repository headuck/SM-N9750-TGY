package com.android.server.wifi.iwc.p000le;

import android.content.Context;
import android.os.Bundle;
import java.util.List;

/* renamed from: com.android.server.wifi.iwc.le.WiFiScanner */
public interface WiFiScanner {
    void initialize(Context context, Bundle bundle);

    void requestScan(WiFiScanResultListener wiFiScanResultListener, CapInfo capInfo, List<Integer> list);
}
