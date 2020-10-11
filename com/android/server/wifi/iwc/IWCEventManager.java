package com.android.server.wifi.iwc;

public class IWCEventManager {
    public static final int ALWAYS = 0;
    public static final int ManaualSwitch_M_Timer = 1000;
    public static final int ManualSwitch_G_Timer = 5000;
    public static final long autoDisconnectThreshold = 4000;
    public static final long connectionMaintainThreshold = 30000;
    public static final int edgeTimer = 20000;
    public static final int edgeTimer_MS = 23000;
    public static final int edgeTimer_RC = 35000;
    public static final long manualSwitchTimeWin = 3000;
    public static final long reconTimeThreshold = 25000;
    public static final int wifiOFFPending_MS = 3000;
}
