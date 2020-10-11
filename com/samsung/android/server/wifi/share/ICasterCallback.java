package com.samsung.android.server.wifi.share;

import com.samsung.android.server.wifi.share.McfDataUtil;

interface ICasterCallback {
    void onPasswordRequested(McfDataUtil.McfData mcfData, String str);

    void onSessionClosed(String str);
}
