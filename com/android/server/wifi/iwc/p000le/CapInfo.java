package com.android.server.wifi.iwc.p000le;

import java.io.Serializable;

/* renamed from: com.android.server.wifi.iwc.le.CapInfo */
public class CapInfo implements Serializable {
    public String bssid;
    public int level;

    public CapInfo(String bssid2, int level2) {
        this.bssid = bssid2;
        this.level = level2;
    }

    public CapInfo(String bssid2) {
        this(bssid2, 0);
    }

    public CapInfo(CapInfo capInfo) {
        this(capInfo.bssid, capInfo.level);
    }

    public CapInfo copy() {
        return new CapInfo(this);
    }
}
