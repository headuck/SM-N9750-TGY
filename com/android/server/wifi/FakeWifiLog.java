package com.android.server.wifi;

import com.android.server.wifi.WifiLog;

public class FakeWifiLog implements WifiLog {
    private static final DummyLogMessage sDummyLogMessage = new DummyLogMessage();

    public WifiLog.LogMessage err(String format) {
        return sDummyLogMessage;
    }

    public WifiLog.LogMessage warn(String format) {
        return sDummyLogMessage;
    }

    public WifiLog.LogMessage info(String format) {
        return sDummyLogMessage;
    }

    public WifiLog.LogMessage trace(String format) {
        return sDummyLogMessage;
    }

    public WifiLog.LogMessage trace(String format, int numFramesToIgnore) {
        return sDummyLogMessage;
    }

    public WifiLog.LogMessage dump(String format) {
        return sDummyLogMessage;
    }

    /* renamed from: eC */
    public void mo2087eC(String msg) {
    }

    /* renamed from: wC */
    public void mo2097wC(String msg) {
    }

    /* renamed from: iC */
    public void mo2090iC(String msg) {
    }

    /* renamed from: tC */
    public void mo2092tC(String msg) {
    }

    /* renamed from: e */
    public void mo2086e(String msg) {
    }

    /* renamed from: w */
    public void mo2096w(String msg) {
    }

    /* renamed from: i */
    public void mo2089i(String msg) {
    }

    /* renamed from: d */
    public void mo2084d(String msg) {
    }

    /* renamed from: v */
    public void mo2095v(String msg) {
    }
}
