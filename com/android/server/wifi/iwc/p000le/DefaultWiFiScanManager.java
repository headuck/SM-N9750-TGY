package com.android.server.wifi.iwc.p000le;

/* renamed from: com.android.server.wifi.iwc.le.DefaultWiFiScanManager */
public class DefaultWiFiScanManager extends AbstractWiFiScanManager {
    public DefaultWiFiScanManager(WiFiStatusObserver externalObserver) {
        this.mWiFiStatusObserver = externalObserver;
    }

    /* access modifiers changed from: protected */
    public void createComponents() {
        this.mWiFiScanner = new ProxyBasedScanner();
        this.mChannelCache = new FileChannelCache();
    }
}
