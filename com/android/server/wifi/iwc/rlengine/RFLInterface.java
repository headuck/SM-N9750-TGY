package com.android.server.wifi.iwc.rlengine;

import android.content.Context;
import com.android.server.wifi.iwc.IWCBDTracking;

public class RFLInterface {
    public boolean aggSnsFlag;
    public int capRSSI;
    public long connectionMaintainedTime;
    public int curAction;
    public int curState;
    public String currentApBssid_IN;
    public boolean edgeFlag;
    public int lastAction;
    public int lastState;
    public IWCBDTracking mBdTracking = new IWCBDTracking();
    public Context mContext;
    public boolean snsFlag;
    public boolean snsOptionChanged;
    public boolean switchFlag;
}
