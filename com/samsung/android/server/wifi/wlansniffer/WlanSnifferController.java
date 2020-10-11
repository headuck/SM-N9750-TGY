package com.samsung.android.server.wifi.wlansniffer;

import android.content.Context;
import android.os.Debug;
import android.util.Log;

public class WlanSnifferController {
    private static final boolean DBG = Debug.semIsProductDev();
    private static final String TAG = "WlanSnifferController";
    private final int CMDTYPE_MON_LOAD_FW = 37;
    private final int CMDTYPE_MON_START_160 = 33;
    private final int CMDTYPE_MON_START_20 = 30;
    private final int CMDTYPE_MON_START_40 = 31;
    private final int CMDTYPE_MON_START_80 = 32;
    private final int CMDTYPE_MON_STATUS = 35;
    private final int CMDTYPE_MON_STOP = 36;
    private final int CMDTYPE_MON_TCPDUMP = 38;
    private Context mContext;
    private WlanDutServiceCaller mWlanDutServiceCaller;

    public WlanSnifferController(Context context) {
        this.mContext = context;
        this.mWlanDutServiceCaller = new WlanDutServiceCaller();
    }

    public String semLoadMonitorModeFirmware(boolean enable) {
        if (DBG) {
            Log.d(TAG, "semLoadMonitorModeFirmware : enable = " + enable);
        }
        if (enable) {
            return this.mWlanDutServiceCaller.semWlanDutServiceCommand(37, "1");
        }
        return this.mWlanDutServiceCaller.semWlanDutServiceCommand(37, "2");
    }

    public String semCheckMonitorMode() {
        if (DBG) {
            Log.d(TAG, "semCheckMonitorMode");
        }
        return this.mWlanDutServiceCaller.semWlanDutServiceCommand(35, "");
    }

    public String semStartMonitorMode(int ch, int bw) {
        if (DBG) {
            Log.d(TAG, "semStartMonitorMode : ch = " + ch + " : bw = " + bw);
        }
        if (bw == 20) {
            WlanDutServiceCaller wlanDutServiceCaller = this.mWlanDutServiceCaller;
            return wlanDutServiceCaller.semWlanDutServiceCommand(30, "" + ch);
        } else if (bw == 40) {
            WlanDutServiceCaller wlanDutServiceCaller2 = this.mWlanDutServiceCaller;
            return wlanDutServiceCaller2.semWlanDutServiceCommand(31, "" + ch);
        } else if (bw == 80) {
            WlanDutServiceCaller wlanDutServiceCaller3 = this.mWlanDutServiceCaller;
            return wlanDutServiceCaller3.semWlanDutServiceCommand(32, "" + ch);
        } else if (bw != 160) {
            return "NG";
        } else {
            WlanDutServiceCaller wlanDutServiceCaller4 = this.mWlanDutServiceCaller;
            return wlanDutServiceCaller4.semWlanDutServiceCommand(33, "" + ch);
        }
    }

    public String semStartAirlogs(boolean enable, boolean compressiveMode) {
        if (DBG) {
            Log.d(TAG, "semStartAirlogs : enable = " + enable + " : compressiveMode = " + compressiveMode);
        }
        if (!enable) {
            return this.mWlanDutServiceCaller.semWlanDutServiceCommand(38, "2");
        }
        if (compressiveMode) {
            return this.mWlanDutServiceCaller.semWlanDutServiceCommand(38, "3");
        }
        return this.mWlanDutServiceCaller.semWlanDutServiceCommand(38, "1");
    }

    public String semStopMonitorMode() {
        if (DBG) {
            Log.d(TAG, "semStopMonitorMode");
        }
        if (this.mWlanDutServiceCaller.semWlanDutServiceCommand(36, "").equals("OK")) {
            return this.mWlanDutServiceCaller.stopWlanDutService();
        }
        return "NG";
    }
}
