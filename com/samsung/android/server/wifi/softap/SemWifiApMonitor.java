package com.samsung.android.server.wifi.softap;

import android.os.Handler;
import android.os.Message;
import android.util.ArraySet;
import android.util.Log;
import android.util.SparseArray;
import com.android.internal.annotations.VisibleForTesting;
import com.android.server.wifi.WifiInjector;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;

public class SemWifiApMonitor {
    public static final int AP_CHANGED_CHANNEL_EVENT = 548875;
    public static final int AP_CSA_FINISHED_EVENT = 548874;
    public static final int AP_STA_ASSOCIATION_EVENT = 548866;
    public static final int AP_STA_CONNECTED_EVENT = 548883;
    public static final int AP_STA_DEAUTH_EVENT = 548881;
    public static final int AP_STA_DISASSOCIATION_EVENT = 548867;
    public static final int AP_STA_DISCONNECTED_EVENT = 548865;
    public static final int AP_STA_JOIN_EVENT = 548876;
    public static final int AP_STA_NEW_EVENT = 548877;
    public static final int AP_STA_NOTALLOW_EVENT = 548878;
    public static final int AP_STA_NOTIFY_DISASSOCIATION_EVENT = 548879;
    public static final int AP_STA_POSSIBLE_PSK_MISMATCH_EVENT = 548872;
    public static final int AP_STA_REMOVE_EVENT = 548880;
    private static final int BASE = 548864;
    public static final int CMD_AP_STA_DISCONNECT = 548884;
    public static final int CMD_AP_STA_RECONNECT = 548885;
    public static final int CTRL_EVENT_DRIVER_STATE_EVENT = 548873;
    private static final String TAG = "SemWifiApMonitor";
    public static final int WPS_FAIL_EVENT = 548869;
    public static final int WPS_OVERLAP_DETECTED = 548882;
    public static final int WPS_PIN_NEEDED_EVENT = 548871;
    public static final int WPS_SUCCESS_EVENT = 548868;
    public static final int WPS_TIMEOUT_EVENT = 548870;
    private boolean mConnected = false;
    private final Map<String, SparseArray<Set<Handler>>> mHandlerMap = new HashMap();
    private final Map<String, Boolean> mMonitoringMap = new HashMap();
    private boolean mVerboseLoggingEnabled = false;
    private final WifiInjector mWifiInjector;

    public SemWifiApMonitor(WifiInjector wifiInjector) {
        this.mWifiInjector = wifiInjector;
    }

    public void enableVerboseLogging(int verbose) {
        if (verbose > 0) {
            this.mVerboseLoggingEnabled = true;
        } else {
            this.mVerboseLoggingEnabled = false;
        }
    }

    public synchronized void registerHandler(String iface, int what, Handler handler) {
        SparseArray<Set<Handler>> ifaceHandlers = this.mHandlerMap.get(iface);
        if (ifaceHandlers == null) {
            ifaceHandlers = new SparseArray<>();
            this.mHandlerMap.put(iface, ifaceHandlers);
        }
        Set<Handler> ifaceWhatHandlers = ifaceHandlers.get(what);
        if (ifaceWhatHandlers == null) {
            ifaceWhatHandlers = new ArraySet<>();
            ifaceHandlers.put(what, ifaceWhatHandlers);
        }
        ifaceWhatHandlers.add(handler);
    }

    public synchronized void unRegisterHandler() {
        this.mHandlerMap.clear();
    }

    private boolean isMonitoring(String iface) {
        Boolean val = this.mMonitoringMap.get(iface);
        if (val == null) {
            return false;
        }
        return val.booleanValue();
    }

    @VisibleForTesting
    public void setMonitoring(String iface, boolean enabled) {
        this.mMonitoringMap.put(iface, Boolean.valueOf(enabled));
    }

    private void setMonitoringNone() {
        for (String iface : this.mMonitoringMap.keySet()) {
            setMonitoring(iface, false);
        }
    }

    public synchronized void startMonitoring(String iface) {
        if (this.mVerboseLoggingEnabled) {
            Log.d(TAG, "startMonitoring(" + iface + ")");
        }
        setMonitoring(iface, true);
    }

    public synchronized void stopMonitoring(String iface) {
        if (this.mVerboseLoggingEnabled) {
            Log.d(TAG, "stopMonitoring(" + iface + ")");
        }
        setMonitoring(iface, false);
    }

    public synchronized void stopAllMonitoring() {
        this.mConnected = false;
        setMonitoringNone();
    }

    private void sendMessage(String iface, int what) {
        sendMessage(iface, Message.obtain((Handler) null, what));
    }

    private void sendMessage(String iface, int what, Object obj) {
        sendMessage(iface, Message.obtain((Handler) null, what, obj));
    }

    private void sendMessage(String iface, int what, int arg1) {
        sendMessage(iface, Message.obtain((Handler) null, what, arg1, 0));
    }

    private void sendMessage(String iface, int what, int arg1, int arg2) {
        sendMessage(iface, Message.obtain((Handler) null, what, arg1, arg2));
    }

    private void sendMessage(String iface, int what, int arg1, int arg2, Object obj) {
        sendMessage(iface, Message.obtain((Handler) null, what, arg1, arg2, obj));
    }

    private void sendMessage(String iface, Message message) {
        Set<Handler> ifaceWhatHandlers;
        SparseArray<Set<Handler>> ifaceHandlers = this.mHandlerMap.get(iface);
        if (iface == null || ifaceHandlers == null) {
            if (this.mVerboseLoggingEnabled) {
                Log.d(TAG, "Sending to all monitors because there's no matching iface");
            }
            for (Map.Entry<String, SparseArray<Set<Handler>>> entry : this.mHandlerMap.entrySet()) {
                if (isMonitoring(entry.getKey()) && (ifaceWhatHandlers = (Set) entry.getValue().get(message.what)) != null) {
                    for (Handler handler : ifaceWhatHandlers) {
                        if (handler != null) {
                            sendMessage(handler, Message.obtain(message));
                        }
                    }
                }
            }
        } else if (isMonitoring(iface)) {
            Set<Handler> ifaceWhatHandlers2 = ifaceHandlers.get(message.what);
            if (ifaceWhatHandlers2 != null) {
                for (Handler handler2 : ifaceWhatHandlers2) {
                    if (handler2 != null) {
                        sendMessage(handler2, Message.obtain(message));
                    }
                }
            }
        } else if (this.mVerboseLoggingEnabled) {
            Log.d(TAG, "Dropping event because (" + iface + ") is stopped");
        }
        message.recycle();
    }

    private void sendMessage(Handler handler, Message message) {
        message.setTarget(handler);
        message.sendToTarget();
    }

    public void hostapdCallbackEvent(String iface, String tstr) {
        Log.d(TAG, "hostapdCallbackEvent,tstr=" + tstr);
        String[] mstr = tstr.split(" ");
        StringJoiner sj = new StringJoiner(" ");
        for (int i = 1; i < mstr.length; i++) {
            sj.add(mstr[i]);
        }
        String str = sj.toString();
        if (mstr[0].equals("WPS-OVERLAP-DETECTED")) {
            sendMessage(iface, (int) WPS_OVERLAP_DETECTED, (Object) str);
        }
        if (mstr[0].equals("AP_STA_CONNECTED")) {
            sendMessage(iface, (int) AP_STA_ASSOCIATION_EVENT, (Object) str);
        } else if (mstr[0].equals("AP-STA-REMOVE")) {
            sendMessage(iface, (int) AP_STA_REMOVE_EVENT, (Object) str);
        } else if (mstr[0].equals("AP-STA-DEAUTH")) {
            sendMessage(iface, (int) AP_STA_DEAUTH_EVENT, (Object) str);
        } else if (mstr[0].equals("AP-STA-DISASSOC")) {
            sendMessage(iface, (int) AP_STA_DISASSOCIATION_EVENT, (Object) str);
        } else if (mstr[0].equals("CTRL-EVENT-DRIVER-STATE")) {
            sendMessage(iface, (int) CTRL_EVENT_DRIVER_STATE_EVENT, (Object) str);
        } else if (mstr[0].equals("WPS-SUCCESS")) {
            sendMessage(iface, (int) WPS_SUCCESS_EVENT, (Object) str);
        } else if (mstr[0].equals("AP-STA-POSSIBLE-PSK-MISMATCH")) {
            sendMessage(iface, (int) AP_STA_POSSIBLE_PSK_MISMATCH_EVENT, (Object) str);
        } else if (mstr[0].equals("WPS_EVENT_FAIL")) {
            sendMessage(iface, (int) WPS_FAIL_EVENT, (Object) str);
        } else if (mstr[0].equals("WPS-TIMEOUT")) {
            sendMessage(iface, (int) WPS_TIMEOUT_EVENT, (Object) str);
        } else if (mstr[0].equals("WPS-PIN-NEEDED")) {
            sendMessage(iface, (int) WPS_PIN_NEEDED_EVENT, (Object) str);
        } else if (mstr[0].equals("AP-CSA-FINISHED")) {
            sendMessage(iface, (int) AP_CSA_FINISHED_EVENT, (Object) str);
        } else if (mstr[0].equals("AP-STA-NOTAllOW")) {
            sendMessage(iface, (int) AP_STA_NOTALLOW_EVENT, (Object) str);
        } else if (mstr[0].equals("AP-STA-NOTIF-DISASSOC")) {
            sendMessage(iface, (int) AP_STA_DISCONNECTED_EVENT, (Object) str);
        } else if (mstr[0].equals("AP-STA-NEW")) {
            sendMessage(iface, (int) AP_STA_NEW_EVENT, (Object) str);
        }
    }
}
