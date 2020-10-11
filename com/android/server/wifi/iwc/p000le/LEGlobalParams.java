package com.android.server.wifi.iwc.p000le;

import android.os.Bundle;

/* renamed from: com.android.server.wifi.iwc.le.LEGlobalParams */
public class LEGlobalParams {
    public static final String DEFAULT_FILE_DIR = "/data/log/wifi/iwc/";
    public static final long DEFAULT_MIN_SCAN_INTERVAL = 10000;
    public static final String[] DEFAULT_WIFI_SCAN_INTENT = {"com.samsung.android.net.wifi.WIFI_POORLINK_MESSAGE"};
    public static final String KEY_FILE_DIR = "FILE_DIR";
    public static final String KEY_MIN_SCAN_INTERVAL = "MIN_SCAN_INTERVAL";
    public static final String KEY_WIFI_SCAN_INTENT = "WIFI_SCAN_INTENT";

    public static Bundle getDefaultParams() {
        Bundle params = new Bundle();
        params.putLong(KEY_MIN_SCAN_INTERVAL, 10000);
        params.putString(KEY_FILE_DIR, DEFAULT_FILE_DIR);
        params.putStringArray(KEY_WIFI_SCAN_INTENT, DEFAULT_WIFI_SCAN_INTENT);
        return params;
    }
}
