package com.android.server.wifi;

import android.content.Context;
import android.net.wifi.ITrafficStateCallback;
import android.os.Bundle;
import android.os.FileUtils;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.util.Log;
import com.android.server.wifi.util.ExternalCallbackTracker;
import com.samsung.android.os.SemDvfsManager;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.PrintWriter;

public class WifiTrafficPoller {
    private static final String TAG = "WifiTrafficPoller";
    private static final int mAffinityBoosterThreshod = Integer.parseInt("40000");
    private static final int mCstateDiableThreshold = Integer.parseInt("25000");
    private static final int mDelackThreshold = Integer.parseInt("25000");
    private static final int mL1ssDisableThreshold = Integer.parseInt("25000");
    private SemDvfsManager mCpuCstate = null;
    private int mCurrenAffinityMode = 0;
    private int mCurrentCstateModeValue = 0;
    private int mCurrentDelAckSize = 1;
    private int mCurrentL1ssModeValue = 0;
    private int mDataActivity;
    private SemDvfsManager mLpm = null;
    private final ExternalCallbackTracker<ITrafficStateCallback> mRegisteredCallbacks;
    private long mRxPkts;
    private long mTxPkts;
    private boolean mVerboseLoggingEnabled = true;
    private WifiServiceImpl mWifiService;

    WifiTrafficPoller(Context context, Looper looper) {
        this.mRegisteredCallbacks = new ExternalCallbackTracker<>(new Handler(looper));
        this.mCpuCstate = SemDvfsManager.createInstance(context, "WIFI_CSTATE", 23);
        this.mLpm = SemDvfsManager.createInstance(context, "WIFI_L1SS", 26);
    }

    public void addCallback(IBinder binder, ITrafficStateCallback callback, int callbackIdentifier) {
        if (!this.mRegisteredCallbacks.add(binder, callback, callbackIdentifier)) {
            Log.e(TAG, "Failed to add callback");
        }
    }

    public void removeCallback(int callbackIdentifier) {
        this.mRegisteredCallbacks.remove(callbackIdentifier);
    }

    public void notifyOnDataActivity(long txPkts, long rxPkts) {
        int dataActivity;
        long preTxPkts = this.mTxPkts;
        long preRxPkts = this.mRxPkts;
        int dataActivity2 = 0;
        this.mTxPkts = txPkts;
        this.mRxPkts = rxPkts;
        if (preTxPkts > 0 || preRxPkts > 0) {
            long sent = this.mTxPkts - preTxPkts;
            long received = this.mRxPkts - preRxPkts;
            if (sent > 0) {
                dataActivity2 = 0 | 2;
            }
            if (received > 0) {
                dataActivity = dataActivity2 | 1;
            } else {
                dataActivity = dataActivity2;
            }
            long j = preTxPkts;
            if (received > ((long) mDelackThreshold)) {
                if (this.mCurrentDelAckSize == 1) {
                    if (this.mVerboseLoggingEnabled) {
                        Log.i(TAG, "delack 20 Set");
                    }
                    setDelAckSize("20");
                    this.mCurrentDelAckSize = 20;
                }
            } else if (this.mCurrentDelAckSize == 20) {
                if (this.mVerboseLoggingEnabled) {
                    Log.i(TAG, "delack auto");
                }
                setDelAckSize("1");
                this.mCurrentDelAckSize = 1;
            }
            long sent2 = sent;
            if (sent + received > ((long) mCstateDiableThreshold)) {
                if (this.mCpuCstate != null) {
                    if (this.mVerboseLoggingEnabled) {
                        Log.i(TAG, "mCpuCstate acquire");
                    }
                    this.mCpuCstate.acquire(4000);
                    this.mCurrentCstateModeValue = 1;
                }
            } else if (this.mCurrentCstateModeValue == 1 && this.mCpuCstate != null) {
                if (this.mVerboseLoggingEnabled) {
                    Log.i(TAG, "mCpuCstate release.");
                }
                this.mCpuCstate.release();
                this.mCurrentCstateModeValue = 0;
            }
            int i = mL1ssDisableThreshold;
            if (sent2 > ((long) i) || received > ((long) i)) {
                if (this.mCurrentL1ssModeValue == 0 && this.mLpm != null) {
                    if (this.mVerboseLoggingEnabled) {
                        Log.i(TAG, "mL1ss acquire");
                    }
                    this.mLpm.acquire();
                    this.mCurrentL1ssModeValue = 1;
                }
            } else if (this.mCurrentL1ssModeValue == 1 && this.mLpm != null) {
                if (this.mVerboseLoggingEnabled) {
                    Log.i(TAG, "mL1ss release");
                }
                this.mLpm.release();
                this.mCurrentL1ssModeValue = 0;
            }
            int i2 = mAffinityBoosterThreshod;
            long j2 = preRxPkts;
            if (sent2 + received > ((long) (i2 * 2))) {
                if (this.mCurrenAffinityMode != 2) {
                    if (this.mVerboseLoggingEnabled) {
                        Log.i(TAG, "Affinity Set 2");
                    }
                    callSECApi(282, 1, true, 2);
                    this.mCurrenAffinityMode = 2;
                }
            } else if (sent2 + received > ((long) i2)) {
                if (this.mCurrenAffinityMode != 1) {
                    if (this.mVerboseLoggingEnabled) {
                        Log.i(TAG, "Affinity Set 1");
                    }
                    callSECApi(282, 1, true, 1);
                    this.mCurrenAffinityMode = 1;
                }
            } else if (this.mCurrenAffinityMode != 0) {
                if (this.mVerboseLoggingEnabled) {
                    Log.i(TAG, "Affinity Auto");
                }
                callSECApi(282, 1, true, 0);
                this.mCurrenAffinityMode = 0;
            }
            if (dataActivity != this.mDataActivity) {
                this.mDataActivity = dataActivity;
                for (ITrafficStateCallback callback : this.mRegisteredCallbacks.getCallbacks()) {
                    try {
                        callback.onStateChanged(this.mDataActivity);
                    } catch (RemoteException e) {
                    }
                }
            }
            int i3 = dataActivity;
            return;
        }
        long j3 = preTxPkts;
        long j4 = preRxPkts;
    }

    public void setService(WifiServiceImpl service) {
        this.mWifiService = service;
    }

    private void setDelAckSize(String delAckSize) {
        Object obj = "/sys/kernel/ipv4/tcp_delack_seg";
        try {
            int delAck = Integer.parseInt(delAckSize);
            if (delAck <= 0 || delAck > 60) {
                Log.e(TAG, " delAck size is out of range, configuring to default");
            }
            FileUtils.stringToFile("/sys/kernel/ipv4/tcp_delack_seg", delAckSize);
        } catch (IOException e) {
            Log.e(TAG, "Can't set delayed ACK size:" + e);
        }
    }

    public void callSECApi(int cmd, int arg, boolean enable, int value) {
        Message msg = new Message();
        msg.what = cmd;
        msg.arg1 = arg;
        if (enable) {
            Bundle args = new Bundle();
            args.putInt("enable", value);
            msg.obj = args;
        }
        WifiServiceImpl wifiServiceImpl = this.mWifiService;
        if (wifiServiceImpl != null) {
            wifiServiceImpl.callSECApi(msg);
        }
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        pw.println("mTxPkts " + this.mTxPkts);
        pw.println("mRxPkts " + this.mRxPkts);
        pw.println("mDataActivity " + this.mDataActivity);
        pw.println("mRegisteredCallbacks " + this.mRegisteredCallbacks.getNumCallbacks());
    }
}
