package com.android.server.wifi.util;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import com.android.internal.annotations.VisibleForTesting;
import com.android.server.wifi.WifiInjector;
import com.android.server.wifi.WifiLog;

public class WifiHandler extends Handler {
    private static final String LOG_TAG = "WifiHandler";
    private WifiLog mLog;
    private String mTag;

    public WifiHandler(String tag, Looper looper) {
        super(looper);
        this.mTag = "WifiHandler." + tag;
    }

    private WifiLog getOrInitLog() {
        if (this.mLog == null) {
            this.mLog = WifiInjector.getInstance().makeLog(this.mTag);
        }
        return this.mLog;
    }

    public void handleMessage(Message msg) {
        getOrInitLog().trace("Received message=% sendingUid=%").mo2069c((long) msg.what).mo2069c((long) msg.sendingUid).flush();
    }

    @VisibleForTesting
    public void setWifiLog(WifiLog wifiLog) {
        this.mLog = wifiLog;
    }
}
