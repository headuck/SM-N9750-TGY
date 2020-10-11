package com.android.server.wifi.iwc.p000le;

/* renamed from: com.android.server.wifi.iwc.le.WiFiStatusListener */
public interface WiFiStatusListener {
    public static final int LINK_STATUS_GOOD = 2;
    public static final int LINK_STATUS_POOR = 1;

    void onCapStatusUpdated(CapInfo capInfo);

    void onStatusUpdated(int i);
}
