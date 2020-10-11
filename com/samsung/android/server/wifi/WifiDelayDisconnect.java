package com.samsung.android.server.wifi;

import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.os.Debug;
import android.os.UserHandle;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.util.Log;
import com.android.server.wifi.WifiInjector;

public class WifiDelayDisconnect {
    private static final boolean DBG = Debug.semIsProductDev();
    private static final int MAX_INTERVAL_DISCONNECT_DELAY_MS = 500;
    private static final String TAG = "WifiDelayDisconnect";
    private Context mContext;
    private long mDeregisterationDuration = 0;
    private boolean mNeedDeregisterationFlag = false;
    private TelephonyManager mTelephonyManager;
    private WifiInjector mWifiInjector;

    public WifiDelayDisconnect(Context context, WifiInjector wifiInjector) {
        this.mContext = context;
        this.mWifiInjector = wifiInjector;
    }

    public void checkAndWait(NetworkInfo info) {
        if (needDeregisteration()) {
            Log.i(TAG, "force delayed transition state");
            waitForDeregisteration(info);
        }
    }

    public boolean setEnable(boolean enabled, long duration) {
        this.mNeedDeregisterationFlag = enabled;
        if (enabled) {
            Log.i(TAG, "request delay operation : " + this.mDeregisterationDuration);
            if (this.mDeregisterationDuration >= duration) {
                return true;
            }
            this.mDeregisterationDuration = duration;
            return true;
        }
        Log.i(TAG, "clear delay operation flag");
        this.mDeregisterationDuration = 0;
        return true;
    }

    public boolean isEnabled() {
        return this.mNeedDeregisterationFlag;
    }

    private TelephonyManager getTelephonyManager() {
        if (this.mTelephonyManager == null) {
            this.mTelephonyManager = this.mWifiInjector.makeTelephonyManager();
        }
        return this.mTelephonyManager;
    }

    private boolean needDeregisteration() {
        if (!this.mNeedDeregisterationFlag) {
            return false;
        }
        if (Settings.Global.getInt(this.mContext.getContentResolver(), "airplane_mode_on", 0) == 1) {
            return true;
        }
        int networkType = getTelephonyManager().getNetworkType();
        if (networkType == 13 || networkType == 14) {
            return false;
        }
        return true;
    }

    private void waitForDeregisteration(NetworkInfo info) {
        if (this.mDeregisterationDuration > 0) {
            if (DBG) {
                Log.i(TAG, "required duration:" + this.mDeregisterationDuration);
            }
            sendSecNetworkStateChanged(info, this.mNeedDeregisterationFlag, this.mDeregisterationDuration);
            long repeat = 1;
            long count = this.mDeregisterationDuration / 500;
            Log.i(TAG, "delay start. duration:" + this.mDeregisterationDuration);
            while (true) {
                if (repeat > count) {
                    break;
                }
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    if (DBG) {
                        Log.e(TAG, "needDeregisteration interrupt exception " + e.toString());
                    }
                }
                if (DBG) {
                    Log.i(TAG, "delay " + (repeat * 500) + "ms");
                }
                if (!needDeregisteration()) {
                    Log.i(TAG, "finish!");
                    break;
                }
                repeat++;
            }
            sendSecNetworkStateChanged(info, false, 0);
        } else if (DBG) {
            Log.e(TAG, "Deregisteration duration time is 0");
        }
    }

    private void sendSecNetworkStateChanged(NetworkInfo info, boolean delayState, long delayMilis) {
        Intent intent = new Intent("com.samsung.android.net.wifi.SEC_NETWORK_STATE_CHANGED");
        intent.addFlags(67108864);
        NetworkInfo networkInfo = new NetworkInfo(info);
        networkInfo.setExtraInfo((String) null);
        intent.putExtra("networkInfo", networkInfo);
        intent.putExtra("delayState", delayState);
        intent.putExtra("delayMaxTime", delayMilis);
        try {
            this.mContext.sendBroadcastAsUser(intent, UserHandle.CURRENT);
        } catch (IllegalStateException e) {
            if (DBG) {
                Log.e(TAG, "Send broadcast before boot - action:" + intent.getAction());
            }
        }
    }
}
